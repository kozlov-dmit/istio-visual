package io.github.istiorouteexplorer.service;

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
import okhttp3.Response;
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

    private record ExecResult(String stdout, String stderr) {
    }

    private record SectionDefinition(String id, String title, String path) {
    }

    private static final List<SectionDefinition> SECTION_DEFINITIONS = List.of(
            new SectionDefinition("configDump", "Config Dump", "/config_dump"),
            new SectionDefinition("listeners", "Listeners", "/listeners?format=json"),
            new SectionDefinition("routes", "Routes", "/routes?format=json"),
            new SectionDefinition("clusters", "Clusters", "/clusters?format=json")
    );

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

        for (SectionDefinition definition : SECTION_DEFINITIONS) {
            try {
                ExecResult execResult = execInIstioProxy(ns, podName, definition.path());
                String payload = execResult.stdout().trim();
                String stderr = execResult.stderr().trim();
                if (payload.isEmpty()) {
                    warnings.add("Received empty payload for " + definition.title() + " from pod " + podName);
                }
                if (!stderr.isEmpty()) {
                    warnings.add("stderr for " + definition.title() + ": " + stderr);
                }
                sections.add(new EnvoyConfigSection(definition.id(), definition.title(), payload, stderr));
            } catch (IOException e) {
                log.warn("Failed to read {} from envoy in pod {}: {}", definition.title(), podName, e.getMessage());
                warnings.add("Failed to load " + definition.title() + ": " + e.getMessage());
            }
        }

        return new EnvoyConfigResponse(toSummary(pod), sections, warnings);
    }

    private ExecResult execInIstioProxy(String namespace, String podName, String path) throws IOException {
        String url = "http://127.0.0.1:15000" + path;
        String[] command = {"curl", "-sS", "-f", "-H", "Accept: application/json", url};
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
        String serviceAccount = Optional.ofNullable(pod.getSpec()).map(spec -> spec.getServiceAccountName()).orElse(null);

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
                containerStatuses
        );
    }

    private EnvoyContainerStatus toContainerStatus(ContainerStatus status) {
        if (status == null) {
            return new EnvoyContainerStatus("<unknown>", false, 0, null);
        }
        return new EnvoyContainerStatus(
                status.getName(),
                Boolean.TRUE.equals(status.getReady()),
                status.getRestartCount(),
                status.getImage()
        );
    }

    private String resolveNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return properties.getNamespace();
        }
        return namespace;
    }
}