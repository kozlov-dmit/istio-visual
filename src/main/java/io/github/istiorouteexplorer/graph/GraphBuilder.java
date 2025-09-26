package io.github.istiorouteexplorer.graph;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.istiorouteexplorer.model.GraphEdge;
import io.github.istiorouteexplorer.model.GraphNode;
import io.github.istiorouteexplorer.model.GraphResponse;
import io.github.istiorouteexplorer.model.NamespaceResources;
import io.github.istiorouteexplorer.model.ResourceCollection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import io.github.istiorouteexplorer.model.istio.*;
import io.github.istiorouteexplorer.model.kubernetes.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GraphBuilder {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    public final ObjectMapper objectMapper;
    public final ModelMapper modelMapper;

    public GraphResponse build(ResourceCollection collection) {
        return new Context(collection, objectMapper, modelMapper).build();
    }

    private static final class Context {
        private final ResourceCollection collection;
        private final Map<String, GraphNode> nodeMap = new LinkedHashMap<>();
        private final List<GraphEdge> edges = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final ServiceIndex serviceIndex;
        private final ExternalServiceIndex externalServiceIndex;
        private final Map<String, PodRecord> podRecords = new LinkedHashMap<>();
        private ServicePodsIndex servicePodsIndex;
        private final ObjectMapper objectMapper;
        private final ModelMapper modelMapper;

        Context(ResourceCollection collection, ObjectMapper objectMapper, ModelMapper modelMapper) {
            this.collection = collection;
            this.serviceIndex = new ServiceIndex(collection);
            this.externalServiceIndex = new ExternalServiceIndex(collection.primary());
            this.objectMapper = objectMapper;
            this.modelMapper = modelMapper;
        }

        GraphResponse build() {
            registerPods();
            this.servicePodsIndex = new ServicePodsIndex(serviceIndex, podRecords);
            linkContainerPairs();
            processVirtualServices();
            List<GraphNode> nodes = new ArrayList<>(nodeMap.values());
            Map<String, Object> summary = Map.of(
                    "nodes", nodes.size(),
                    "edges", edges.size(),
                    "pods", podRecords.size(),
                    "virtualServices", collection.primary().virtualServices().size()
            );
            return new GraphResponse(collection.primary().namespace(), Instant.now(), summary, nodes, edges, warnings);
        }

        private void registerPods() {
            for (var pod : collection.primary().pods()) {
                PodDto podDto = modelMapper.map(pod, PodDto.class);
                ObjectMetadataDto metadata = podDto.metadata();
                PodSpecDto spec = podDto.spec();
                PodStatusDto status = podDto.status();
                String namespace = metadata.namespace();
                String podName = metadata.name();
                if (podName == null) {
                    continue;
                }
                Map<String, String> labels = metadata.labels();
                List<ContainerDto> containersSpec = spec.containers();
                List<ContainerRecord> containers = new ArrayList<>();
                for (ContainerDto container : containersSpec) {
                    String containerName = Objects.toString(container.name(), "container");
                    String image = Objects.toString(container.image(), "");
                    boolean sidecar = isSidecarContainer(podDto, containerName, image);
                    String nodeId = "container:%s/%s/%s".formatted(namespace, podName, containerName);
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("pod", podName);
                    properties.put("namespace", namespace);
                    properties.put("container", containerName);
                    properties.put("containerType", sidecar ? "sidecar" : "app");
                    properties.put("image", image);
                    properties.put("displayName", containerName + "@" + podName);
                    properties.put("labels", labels);
                    if (status != null) {
                        properties.put("status", status);
                    }
                    GraphNode node = new GraphNode(nodeId, sidecar ? "sidecarContainer" : "appContainer", properties);
                    nodeMap.put(nodeId, node);
                    containers.add(new ContainerRecord(nodeId, namespace, podName, containerName, sidecar));
                }
                podRecords.put(namespace + ":" + podName, new PodRecord(namespace, podName, labels, containers));
            }
        }

        private void linkContainerPairs() {
            for (PodRecord pod : podRecords.values()) {
                List<ContainerRecord> apps = pod.containers().stream().filter(c -> !c.sidecar()).toList();
                List<ContainerRecord> sidecars = pod.containers().stream().filter(ContainerRecord::sidecar).toList();
                if (apps.isEmpty() || sidecars.isEmpty()) {
                    continue;
                }
                for (ContainerRecord app : apps) {
                    for (ContainerRecord sidecar : sidecars) {
                        String edgeId = "%s->%s:%s".formatted(app.nodeId(), sidecar.nodeId(), hash(app.nodeId() + sidecar.nodeId()));
                        Map<String, Object> metadata = Map.of(
                                "kind", "podLink",
                                "pod", pod.podName(),
                                "namespace", pod.namespace()
                        );
                        edges.add(new GraphEdge(edgeId, "podLink", app.nodeId(), sidecar.nodeId(), metadata));
                    }
                }
            }
        }

        private void processVirtualServices() {
            for (var vs : collection.primary().virtualServices()) {
                VirtualServiceDto vsDto = modelMapper.map(vs, VirtualServiceDto.class);
                ObjectMetadataDto metadata = vsDto.metadata();
                VirtualServiceSpecDto spec = vsDto.spec();
                if (spec == null) {
                    warnings.add("VirtualService missing spec: " + metadata);
                    continue;
                }
                String namespace = metadata.namespace();
                String name = Objects.toString(metadata.name(), "virtualService");
                List<String> hosts = spec.hosts();
                List<ContainerRecord> sourceContainers = containersForHosts(hosts, namespace, true);
                if (sourceContainers.isEmpty()) {
                    sourceContainers = containersForHosts(hosts, namespace, false);
                }
                if (sourceContainers.isEmpty()) {
                    warnings.add("VirtualService %s/%s has no matching source pods".formatted(namespace, name));
                }
                List<IstioRoute> routes = new ArrayList<>();
                routes.addAll(spec.http());
                routes.addAll(spec.tcp());
                routes.addAll(spec.tls());
                if (routes.isEmpty()) {
                    warnings.add("VirtualService %s/%s defines no routes".formatted(namespace, name));
                    continue;
                }
                for (IstioRoute route : routes) {
                    handleRoute(namespace, name, route, sourceContainers);
                }
            }
        }

        private void handleRoute(String namespace, String virtualServiceName, IstioRoute route, List<ContainerRecord> sources) {
            List<RouteDestinationDto> destinations = extractDestinations(route);
            if (destinations.isEmpty()) {
                warnings.add("VirtualService %s/%s route has no destinations".formatted(namespace, virtualServiceName));
                return;
            }
            for (RouteDestinationDto destination : destinations) {
                DestinationDto destSpec = destination.destination();
                if (destSpec.host() == null) {
                    warnings.add("VirtualService %s/%s destination missing host".formatted(namespace, virtualServiceName));
                    continue;
                }
                String host = destSpec.host();
                String destNamespace = namespace;
                String subset = destSpec.subset();
                List<ContainerRecord> targets = containersForHost(host, destNamespace, true);
                if (targets.isEmpty()) {
                    targets = containersForHost(host, destNamespace, false);
                }
                if (targets.isEmpty()) {
                    GraphNode external = ensureExternalNode(host, destNamespace);
                    connectToExternal(sources, external, route, virtualServiceName, namespace, host);
                } else {
                    connectContainers(sources, targets, route, virtualServiceName, namespace, host, destNamespace, subset);
                }
            }
        }

        private void connectContainers(
                List<ContainerRecord> sources,
                List<ContainerRecord> targets,
                IstioRoute route,
                String virtualServiceName,
                String vsNamespace,
                String destHost,
                String destNamespace,
                String subset
        ) {
            if (sources.isEmpty() || targets.isEmpty()) {
                return;
            }
            for (ContainerRecord source : sources) {
                for (ContainerRecord target : targets) {
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("virtualService", Map.of("name", virtualServiceName, "namespace", vsNamespace));
                    metadata.put("route", route);
                    metadata.put("destinationHost", destHost);
                    metadata.put("destinationNamespace", destNamespace);
                    if (subset != null) {
                        metadata.put("subset", subset);
                    }
                    serviceIndex.findTrafficPolicy(destHost, destNamespace, subset).ifPresent(policy -> metadata.put("trafficPolicy", policy));
                    String edgeId = "%s->%s:%s".formatted(source.nodeId(), target.nodeId(), hash(source.nodeId() + target.nodeId() + destHost));
                    edges.add(new GraphEdge(edgeId, "traffic", source.nodeId(), target.nodeId(), metadata));
                }
            }
        }

        private void connectToExternal(
                List<ContainerRecord> sources,
                GraphNode external,
                IstioRoute route,
                String virtualServiceName,
                String namespace,
                String destHost
        ) {
            if (sources.isEmpty()) {
                return;
            }
            for (ContainerRecord source : sources) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("virtualService", Map.of("name", virtualServiceName, "namespace", namespace));
                metadata.put("route", route);
                metadata.put("destinationHost", destHost);
                metadata.put("external", external.properties());
                String edgeId = "%s->%s:%s".formatted(source.nodeId(), external.id(), hash(source.nodeId() + external.id() + destHost));
                edges.add(new GraphEdge(edgeId, "traffic", source.nodeId(), external.id(), metadata));
            }
        }

        private List<ContainerRecord> containersForHosts(List<String> hosts, String namespace, boolean preferSidecars) {
            Set<ContainerRecord> result = new LinkedHashSet<>();
            for (String host : hosts) {
                result.addAll(containersForHost(host, namespace, preferSidecars));
            }
            return new ArrayList<>(result);
        }

        private List<ContainerRecord> containersForHost(String host, String namespace, boolean preferSidecars) {
            if (servicePodsIndex == null) {
                return List.of();
            }
            String canonical = canonicalHost(host, namespace);
            List<PodRecord> pods = servicePodsIndex.podsForHost(canonical);
            if (pods.isEmpty()) {
                return List.of();
            }
            return pods.stream()
                    .flatMap(p -> p.containers().stream())
                    .filter(preferSidecars ? ContainerRecord::sidecar : c -> true)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private GraphNode ensureExternalNode(String host, String namespace) {
            String canonical = canonicalHost(host, namespace);
            String nodeId = "external:" + canonical;
            GraphNode existing = nodeMap.get(nodeId);
            if (existing != null) {
                return existing;
            }
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("host", canonical);
            props.put("namespace", namespace);
            externalServiceIndex.find(host, namespace).ifPresent(entry -> props.put("serviceEntry", entry));
            GraphNode node = new GraphNode(nodeId, "externalService", props);
            nodeMap.put(nodeId, node);
            return node;
        }

        private List<RouteDestinationDto> extractDestinations(IstioRoute route) {
            List<RouteDestinationDto> result = new ArrayList<>();
            switch (route) {
                case HttpRouteDto httpRoute -> {
                    result.addAll(httpRoute.route());
                    if (httpRoute.mirror() != null) {
                        result.add(httpRoute.mirror());
                    }
                }
                case TcpRouteDto tcpRoute -> result.addAll(tcpRoute.route());
                case TlsRouteDto tlsRoute -> result.addAll(tlsRoute.route());
                default -> throw new IllegalStateException("Unexpected route type: " + route);
            }

            return result;
        }

        private static boolean isSidecarContainer(PodDto pod, String containerName, String image) {
            if (containerName != null && containerName.toLowerCase(Locale.ROOT).contains("istio-proxy")) {
                return true;
            }
            if (image != null && image.contains("istio/proxy")) {
                return true;
            }
            Map<String, String> annotations = pod.metadata().annotations();
            return annotations.containsKey("sidecar.istio.io/status");
        }
    }

    private record PodRecord(String namespace, String podName, Map<String, String> labels, List<ContainerRecord> containers) {
    }

    private record ContainerRecord(String nodeId, String namespace, String podName, String containerName, boolean sidecar) {
    }

    private static final class ServiceIndex {
        private final Map<String, ServiceRecord> services = new LinkedHashMap<>();

        ServiceIndex(ResourceCollection collection) {
            List<NamespaceResources> all = new ArrayList<>();
            all.add(collection.primary());
            all.addAll(collection.extras().values());
            for (NamespaceResources resources : all) {
                for (var serviceResource : resources.services()) {
                    Map<String, Object> svc = asMap(serviceResource);
                    Map<String, Object> metadata = asMap(svc.get("metadata"));
                    Map<String, Object> spec = asMap(svc.get("spec"));
                    String name = Objects.toString(metadata.get("name"), null);
                    if (name == null) {
                        continue;
                    }
                    String namespace = Objects.toString(metadata.getOrDefault("namespace", resources.namespace()), resources.namespace());
                    Map<String, String> selector = asStringMap(spec.get("selector"));
                    ServiceRecord record = new ServiceRecord(name, namespace, selector, spec);
                    for (String variant : canonicalVariants(name, namespace)) {
                        services.put(variant, record);
                    }
                }
            }
        }

        Optional<Map<String, Object>> findTrafficPolicy(String host, String namespace, String subset) {
            ServiceRecord record = services.get(canonicalHost(host, namespace));
            if (record == null) {
                return Optional.empty();
            }
            Map<String, Object> spec = record.spec();
            Map<String, Object> trafficPolicy = asMap(spec.get("trafficPolicy"));
            if (subset == null && !trafficPolicy.isEmpty()) {
                return Optional.of(trafficPolicy);
            }
            if (subset != null) {
                for (Map<String, Object> subsetSpec : listOfMaps(spec.get("subsets"))) {
                    if (subset.equals(subsetSpec.get("name"))) {
                        Map<String, Object> subsetPolicy = asMap(subsetSpec.get("trafficPolicy"));
                        if (!subsetPolicy.isEmpty()) {
                            return Optional.of(subsetPolicy);
                        }
                    }
                }
            }
            return Optional.ofNullable(trafficPolicy.isEmpty() ? null : trafficPolicy);
        }

        Optional<ServiceInfo> serviceInfo(String canonicalHost) {
            ServiceRecord record = services.get(canonicalHost);
            if (record == null) {
                return Optional.empty();
            }
            return Optional.of(new ServiceInfo(record.name(), record.namespace(), record.selector(), record.spec()));
        }

        Set<String> hosts() {
            return services.keySet();
        }

        private record ServiceRecord(String name, String namespace, Map<String, String> selector, Map<String, Object> spec) {
        }

        private record ServiceInfo(String name, String namespace, Map<String, String> selector, Map<String, Object> spec) {
        }
    }

    private static final class ServicePodsIndex {
        private final Map<String, List<PodRecord>> podsByHost = new LinkedHashMap<>();

        ServicePodsIndex(ServiceIndex serviceIndex, Map<String, PodRecord> pods) {
            for (String host : serviceIndex.hosts()) {
                serviceIndex.serviceInfo(host).ifPresent(info -> {
                    List<PodRecord> matched = pods.values().stream()
                            .filter(p -> p.namespace().equals(info.namespace()))
                            .filter(p -> matchesSelector(p.labels(), info.selector()))
                            .toList();
                    if (!matched.isEmpty()) {
                        podsByHost.put(host, matched);
                    }
                });
            }
        }

        List<PodRecord> podsForHost(String canonicalHost) {
            return podsByHost.getOrDefault(canonicalHost, List.of());
        }

        private static boolean matchesSelector(Map<String, String> labels, Map<String, String> selector) {
            if (selector == null || selector.isEmpty()) {
                return false;
            }
            for (Map.Entry<String, String> entry : selector.entrySet()) {
                if (!Objects.equals(labels.get(entry.getKey()), entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class ExternalServiceIndex {
        private final Map<String, Map<String, Object>> entries = new LinkedHashMap<>();

        ExternalServiceIndex(NamespaceResources resources) {
            for (var serviceEntryResource : resources.serviceEntries()) {
                Map<String, Object> serviceEntry = asMap(serviceEntryResource);
                Map<String, Object> spec = asMap(serviceEntry.get("spec"));
                Map<String, Object> metadata = asMap(serviceEntry.get("metadata"));
                for (String host : listOfStrings(spec.get("hosts"))) {
                    entries.put(canonicalHost(host, resources.namespace()), Map.of(
                            "kind", "ServiceEntry",
                            "metadata", metadata,
                            "spec", spec
                    ));
                }
            }
        }

        Optional<Map<String, Object>> find(String host, String namespace) {
            return Optional.ofNullable(entries.get(canonicalHost(host, namespace)));
        }
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .collect(
                            HashMap::new,
                            (m, v) -> m.put(String.valueOf(v.getKey()), v.getValue()),
                            HashMap::putAll
                    );
        }
        return Map.of();
    }

    private static Map<String, String> asStringMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .collect(Collectors.toMap(e -> Objects.toString(e.getKey()), e -> Objects.toString(e.getValue())));
        }
        return Map.of();
    }

    private static List<Map<String, Object>> listOfMaps(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add(asMap(map));
                }
            }
            return result;
        }
        return List.of();
    }

    private static List<String> listOfStrings(Object value) {
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return List.of();
    }

    private static Set<String> canonicalVariants(String serviceName, String namespace) {
        Set<String> variants = new LinkedHashSet<>();
        variants.add(serviceName);
        variants.add("%s.%s".formatted(serviceName, namespace));
        variants.add("%s.%s.svc".formatted(serviceName, namespace));
        variants.add("%s.%s.svc.cluster.local".formatted(serviceName, namespace));
        return variants.stream().map(host -> canonicalHost(host, namespace)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String canonicalHost(String rawHost, String namespace) {
        if (rawHost == null || rawHost.isBlank()) {
            return rawHost;
        }
        String host = rawHost.trim().toLowerCase(Locale.ROOT);
        if (host.equals("mesh")) {
            return host;
        }
        if (host.endsWith(".svc.cluster.local") || host.endsWith(".cluster.local")) {
            return host;
        }
        if (host.endsWith(".svc")) {
            return host + ".cluster.local";
        }
        if (host.contains(".svc.")) {
            return host;
        }
        String[] parts = host.split("\\.");
        if (parts.length == 1) {
            return "%s.%s.svc.cluster.local".formatted(host, namespace);
        }
        if (parts.length == 2 && parts[1].equals(namespace)) {
            return "%s.%s.svc.cluster.local".formatted(parts[0], parts[1]);
        }
        if (parts.length == 3 && parts[1].equals(namespace) && parts[2].equals("svc")) {
            return "%s.%s.svc.cluster.local".formatted(parts[0], parts[1]);
        }
        return host;
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(value.getBytes())).substring(0, 10);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm not available", e);
        }
    }
}
