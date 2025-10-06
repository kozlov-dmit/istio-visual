package io.github.istiorouteexplorer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.github.istiorouteexplorer.config.AppProperties;
import io.github.istiorouteexplorer.model.envoy.EnvoyConfigResponse;
import io.github.istiorouteexplorer.model.envoy.EnvoyConfigSection;
import io.github.istiorouteexplorer.model.envoy.EnvoyPodSummary;
import io.github.istiorouteexplorer.model.envoy.EnvoyPodSummary.EnvoyContainerStatus;
import io.github.istiorouteexplorer.model.envoy.EnvoyPodsResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class EnvoyInspectorService {

    private static final Logger log = LoggerFactory.getLogger(EnvoyInspectorService.class);
    private static final String ISTIO_PROXY_CONTAINER = "istio-proxy";
    private static final Duration DEFAULT_EXEC_TIMEOUT = Duration.ofSeconds(15);

    private final KubernetesClient kubernetesClient;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    private record ExecResult(String stdout, String stderr) {
    }

    public EnvoyPodsResponse listEnvoyPods(String namespace) throws IOException {
        String ns = resolveNamespace(namespace);
        PodList podList;
        try {
            podList = kubernetesClient.pods().inNamespace(ns).list();
        } catch (KubernetesClientException e) {
            throw new IOException("Failed to list pods in namespace " + ns + ": " + e.getMessage(), e);
        }
        if (podList == null || podList.getItems() == null) {
            return new EnvoyPodsResponse(Collections.emptyList());
        }
        List<EnvoyPodSummary> pods = podList.getItems().stream()
                .filter(this::hasIstioProxyContainer)
                .map(this::toSummary)
                .sorted(Comparator.comparing(EnvoyPodSummary::name))
                .toList();
        return new EnvoyPodsResponse(pods);
    }

    public EnvoyConfigResponse fetchEnvoyConfig(String namespace, String podName) throws IOException {
        String ns = resolveNamespace(namespace);
        if (podName == null || podName.isBlank()) {
            throw new IllegalArgumentException("Pod name must be provided");
        }
        Pod pod;
        try {
            pod = kubernetesClient.pods().inNamespace(ns).withName(podName).get();
        } catch (KubernetesClientException e) {
            throw new IOException("Failed to load pod " + podName + ": " + e.getMessage(), e);
        }
        if (pod == null) {
            throw new IOException("Pod " + podName + " not found in namespace " + ns);
        }
        if (!hasIstioProxyContainer(pod)) {
            throw new IOException("Pod " + podName + " does not contain an istio-proxy container");
        }

        List<EnvoyConfigSection> sections = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String configDumpPayload = null;

        try {
            ExecResult execResult = execInIstioProxy(ns, podName, "/config_dump");
            String payload = execResult.stdout().trim();
            String stderr = execResult.stderr().trim();
            configDumpPayload = payload;
            if (payload.isEmpty()) {
                warnings.add("Received empty payload for Config Dump from pod " + podName);
            }
            if (!stderr.isEmpty()) {
                warnings.add("stderr for Config Dump: " + stderr);
            }
            sections.add(new EnvoyConfigSection("configDump", "Config Dump", payload, stderr));
        } catch (IOException e) {
            log.warn("Failed to read Config Dump from envoy in pod {}: {}", podName, e.getMessage());
            warnings.add("Failed to load Config Dump: " + e.getMessage());
        }

        if (configDumpPayload == null || configDumpPayload.isBlank()) {
            warnings.add("Config dump payload is empty; unable to extract listeners/routes for pod " + podName);
        } else {
            try {
                Optional<String> listenersFromDump = extractListenersFromConfigDump(configDumpPayload);
                if (listenersFromDump.isPresent()) {
                    sections.add(new EnvoyConfigSection(
                            "listenersFromConfigDump",
                            "Listeners (config_dump)",
                            listenersFromDump.get(),
                            ""));
                } else {
                    warnings.add("ListenersConfigDump section not found inside config_dump for pod " + podName);
                }
            } catch (IOException e) {
                warnings.add("Failed to parse ListenersConfigDump for pod " + podName + ": " + e.getMessage());
            }

            try {
                Optional<String> clustersFromDump = extractClustersFromConfigDump(configDumpPayload);
                if (clustersFromDump.isPresent()) {
                    sections.add(new EnvoyConfigSection(
                            "clustersFromConfigDump",
                            "Clusters (config_dump)",
                            clustersFromDump.get(),
                            ""));
                } else {
                    warnings.add("ClustersConfigDump section not found inside config_dump for pod " + podName);
                }
            } catch (IOException e) {
                warnings.add("Failed to parse ClustersConfigDump for pod " + podName + ": " + e.getMessage());
            }

            try {
                Optional<String> routesFromDump = extractRoutesFromConfigDump(configDumpPayload);
                if (routesFromDump.isPresent()) {
                    sections.add(new EnvoyConfigSection(
                            "routesFromConfigDump",
                            "Routes (config_dump)",
                            routesFromDump.get(),
                            ""));
                } else {
                    warnings.add("RoutesConfigDump section not found inside config_dump for pod " + podName);
                }
            } catch (IOException e) {
                warnings.add("Failed to parse RoutesConfigDump for pod " + podName + ": " + e.getMessage());
            }

        }

        try {
            ExecResult statsResult = execInIstioProxy(ns, podName, "/stats?format=json");
            String statsPayload = statsResult.stdout().trim();
            String statsStderr = statsResult.stderr().trim();
            if (statsPayload.isEmpty()) {
                warnings.add("Received empty payload for Envoy stats from pod " + podName);
            }
            if (!statsStderr.isEmpty()) {
                warnings.add("stderr for stats: " + statsStderr);
            }
            sections.add(new EnvoyConfigSection(
                    "stats",
                    "Runtime stats",
                    statsPayload,
                    statsStderr));
        } catch (IOException e) {
            log.warn("Failed to read stats from envoy in pod {}: {}", podName, e.getMessage());
            warnings.add("Failed to load stats: " + e.getMessage());
        }
        return new EnvoyConfigResponse(toSummary(pod), sections, warnings);
    }

    private ExecResult execInIstioProxy(String namespace, String podName, String path) throws IOException {
        String url = "http://127.0.0.1:15000" + path;
        String[] command = { "curl", "-sS", "-f", "-H", "Accept: application/json", url };
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failureRef = new AtomicReference<>();
        Duration timeout = Optional.ofNullable(properties.getRequestTimeout()).orElse(DEFAULT_EXEC_TIMEOUT);

        try (ExecWatch execWatch = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer(ISTIO_PROXY_CONTAINER)
                .writingOutput(stdout)
                .writingError(stderr)
                .usingListener(new ExecListener() {
                    @Override
                    public void onFailure(Throwable t, Response failureResponse) {
                        failureRef.set(t);
                        latch.countDown();
                    }

                    @Override
                    public void onClose(int code, String reason) {
                        latch.countDown();
                    }
                })
                .exec(command)) {
            boolean completed = latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                throw new IOException("Timed out executing curl inside pod " + podName);
            }
            Throwable failure = failureRef.get();
            if (failure != null) {
                throw new IOException("Curl execution failed: " + failure.getMessage(), failure);
            }
            return new ExecResult(stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command execution interrupted", e);
        } catch (KubernetesClientException e) {
            throw new IOException("Failed to execute curl in pod " + podName + ": " + e.getMessage(), e);
        }
    }

    private Optional<String> extractListenersFromConfigDump(String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        ArrayNode configs;
        if (root.isArray()) {
            configs = (ArrayNode) root;
        } else if (root.path("configs").isArray()) {
            configs = (ArrayNode) root.path("configs");
        } else {
            return Optional.empty();
        }

        for (JsonNode config : configs) {
            String typeUrl = config.path("@type").asText();
            if (typeUrl.contains("ListenersConfigDump")) {
                return Optional.of(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config));
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractClustersFromConfigDump(String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        ArrayNode configs;
        if (root.isArray()) {
            configs = (ArrayNode) root;
        } else if (root.path("configs").isArray()) {
            configs = (ArrayNode) root.path("configs");
        } else {
            return Optional.empty();
        }

        for (JsonNode config : configs) {
            String typeUrl = config.path("@type").asText();
            if (typeUrl.contains("ClustersConfigDump")) {
                return Optional.of(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config));
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractRoutesFromConfigDump(String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        ArrayNode configs;
        if (root.isArray()) {
            configs = (ArrayNode) root;
        } else if (root.path("configs").isArray()) {
            configs = (ArrayNode) root.path("configs");
        } else {
            return Optional.empty();
        }

        for (JsonNode config : configs) {
            String typeUrl = config.path("@type").asText();
            if (typeUrl.contains(".RoutesConfigDump")) {
                return Optional.of(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config));
            }
        }
        return Optional.empty();
    }

    private boolean hasIstioProxyContainer(Pod pod) {
        if (pod.getSpec() == null || pod.getSpec().getContainers() == null) {
            return false;
        }
        return pod.getSpec().getContainers().stream()
                .anyMatch(container -> ISTIO_PROXY_CONTAINER.equals(container.getName()));
    }

    private EnvoyPodSummary toSummary(Pod pod) {
        String name = Optional.ofNullable(pod.getMetadata()).map(meta -> meta.getName()).orElse("<unknown>");
        String namespace = Optional.ofNullable(pod.getMetadata()).map(meta -> meta.getNamespace()).orElse("<unknown>");
        String phase = Optional.ofNullable(pod.getStatus()).map(status -> status.getPhase()).orElse("UNKNOWN");
        String podIp = Optional.ofNullable(pod.getStatus()).map(status -> status.getPodIP()).orElse(null);
        String hostIp = Optional.ofNullable(pod.getStatus()).map(status -> status.getHostIP()).orElse(null);
        String nodeName = Optional.ofNullable(pod.getSpec()).map(spec -> spec.getNodeName()).orElse(null);
        String serviceAccount = Optional.ofNullable(pod.getSpec()).map(spec -> spec.getServiceAccountName())
                .orElse(null);

        String creationTimestampRaw = Optional.ofNullable(pod.getMetadata())
                .map(meta -> meta.getCreationTimestamp())
                .orElse(null);
        Instant creationTimestamp = null;
        if (creationTimestampRaw != null && !creationTimestampRaw.isBlank()) {
            creationTimestamp = Instant.parse(creationTimestampRaw);
        }

        Map<String, String> labels = Optional.ofNullable(pod.getMetadata())
                .map(meta -> meta.getLabels())
                .map(HashMap::new)
                .orElseGet(HashMap::new);
        Map<String, String> annotations = Optional.ofNullable(pod.getMetadata())
                .map(meta -> meta.getAnnotations())
                .map(HashMap::new)
                .orElseGet(HashMap::new);

        List<EnvoyContainerStatus> containerStatuses = Optional.ofNullable(pod.getStatus())
                .map(status -> status.getContainerStatuses())
                .orElse(List.of())
                .stream()
                .map(this::toContainerStatus)
                .toList();

        return new EnvoyPodSummary(
                name,
                namespace,
                phase,
                podIp,
                hostIp,
                nodeName,
                serviceAccount,
                creationTimestamp,
                labels,
                annotations,
                containerStatuses);
    }

    private EnvoyContainerStatus toContainerStatus(ContainerStatus status) {
        if (status == null) {
            return new EnvoyContainerStatus("<unknown>", false, 0, null);
        }
        return new EnvoyContainerStatus(
                status.getName(),
                Boolean.TRUE.equals(status.getReady()),
                status.getRestartCount(),
                status.getImage());
    }

    private String resolveNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return properties.getNamespace();
        }
        return namespace;
    }
}
