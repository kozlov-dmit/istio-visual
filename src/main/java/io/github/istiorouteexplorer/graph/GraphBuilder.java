package io.github.istiorouteexplorer.graph;

import io.github.istiorouteexplorer.model.GraphEdge;
import io.github.istiorouteexplorer.model.GraphNode;
import io.github.istiorouteexplorer.model.GraphResponse;
import io.github.istiorouteexplorer.model.NamespaceResources;
import io.github.istiorouteexplorer.model.ResourceCollection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class GraphBuilder {

    public GraphResponse build(ResourceCollection resources) {
        GraphAccumulator accumulator = new GraphAccumulator(resources);
        accumulator.process();
        return accumulator.toResponse();
    }

    private static final class GraphAccumulator {
        private final ResourceCollection collection;
        private final Map<String, NodeBuilder> nodes = new LinkedHashMap<>();
        private final List<GraphEdge> edges = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final ServiceIndex serviceIndex;
        private final DestinationRuleIndex destinationRuleIndex;

        private GraphAccumulator(ResourceCollection collection) {
            this.collection = collection;
            this.serviceIndex = new ServiceIndex(collection);
            this.destinationRuleIndex = new DestinationRuleIndex(collection);
        }

        private void process() {
            NamespaceResources primary = collection.primary();
            primary.serviceEntries().forEach(this::processServiceEntry);
            primary.workloadEntries().forEach(this::processWorkloadEntry);
            primary.virtualServices().forEach(vs -> processVirtualService(vs, primary.namespace()));
        }

        private GraphResponse toResponse() {
            List<GraphNode> graphNodes = nodes.values().stream().map(NodeBuilder::build).toList();
            Map<String, Object> summary = Map.of(
                    "nodes", graphNodes.size(),
                    "edges", edges.size(),
                    "virtualServices", collection.primary().virtualServices().size(),
                    "destinationRules", collection.primary().destinationRules().size(),
                    "serviceEntries", collection.primary().serviceEntries().size()
            );
            return new GraphResponse(collection.primary().namespace(), Instant.now(), summary, graphNodes, edges, warnings);
        }

        private void processServiceEntry(Map<String, Object> serviceEntry) {
            Map<String, Object> metadata = mapValue(serviceEntry, "metadata");
            Map<String, Object> spec = mapValue(serviceEntry, "spec");
            if (spec == null) {
                warnings.add("ServiceEntry missing spec: " + metadata);
                return;
            }
            String namespace = stringValue(metadata, "namespace", collection.primary().namespace());
            String name = stringValue(metadata, "name", "service-entry");
            String nodeId = "serviceEntry:%s/%s".formatted(namespace, name);
            NodeBuilder node = nodes.computeIfAbsent(nodeId, id -> new NodeBuilder(id, "serviceEntry"));
            node.put("namespace", namespace);
            node.put("name", name);
            node.put("resource", Map.of(
                    "kind", "ServiceEntry",
                    "metadata", metadata,
                    "spec", spec
            ));
            List<String> hosts = listString(spec.get("hosts"));
            node.put("hosts", hosts);
            for (String host : hosts) {
                NodeBuilder hostNode = ensureHostNode(host, namespace, null);
                hostNode.addResource(Map.of(
                        "kind", "ServiceEntry",
                        "name", name,
                        "namespace", namespace,
                        "spec", spec
                ));
                String edgeId = "%s->%s".formatted(nodeId, hostNode.id);
                Map<String, Object> props = Map.of(
                        "kind", "serviceEntryHost",
                        "serviceEntry", Map.of("name", name, "namespace", namespace)
                );
                edges.add(new GraphEdge(edgeId, "serviceEntryHost", nodeId, hostNode.id, props));
            }
        }

        private void processWorkloadEntry(Map<String, Object> workloadEntry) {
            Map<String, Object> metadata = mapValue(workloadEntry, "metadata");
            Map<String, Object> spec = mapValue(workloadEntry, "spec");
            String namespace = stringValue(metadata, "namespace", collection.primary().namespace());
            String name = stringValue(metadata, "name", "workload-entry");
            String nodeId = "workloadEntry:%s/%s".formatted(namespace, name);
            NodeBuilder node = nodes.computeIfAbsent(nodeId, id -> new NodeBuilder(id, "workloadEntry"));
            node.put("namespace", namespace);
            node.put("name", name);
            node.put("resource", Map.of(
                    "kind", "WorkloadEntry",
                    "metadata", metadata,
                    "spec", spec
            ));
        }

        private void processVirtualService(Map<String, Object> virtualService, String defaultNamespace) {
            Map<String, Object> metadata = mapValue(virtualService, "metadata");
            Map<String, Object> spec = mapValue(virtualService, "spec");
            if (spec == null) {
                warnings.add("VirtualService missing spec: " + metadata);
                return;
            }
            String namespace = stringValue(metadata, "namespace", defaultNamespace);
            String name = stringValue(metadata, "name", "virtual-service");
            List<String> hosts = listString(spec.get("hosts"));
            if (hosts.isEmpty()) {
                warnings.add("VirtualService %s/%s has no hosts".formatted(namespace, name));
            }
            List<NodeBuilder> sourceNodes = hosts.stream()
                    .map(host -> ensureHostNode(host, namespace, virtualService))
                    .toList();
            Map<String, Object> vsInfo = Map.of(
                    "kind", "VirtualService",
                    "name", name,
                    "namespace", namespace,
                    "spec", spec
            );
            sourceNodes.forEach(node -> node.addResource(vsInfo));
            processRouteList("http", listOfMaps(spec.get("http")), namespace, name, sourceNodes, vsInfo);
            processRouteList("tcp", listOfMaps(spec.get("tcp")), namespace, name, sourceNodes, vsInfo);
            processRouteList("tls", listOfMaps(spec.get("tls")), namespace, name, sourceNodes, vsInfo);
        }

        private void processRouteList(
                String protocol,
                List<Map<String, Object>> routes,
                String namespace,
                String virtualServiceName,
                List<NodeBuilder> sources,
                Map<String, Object> vsInfo
        ) {
            for (int routeIndex = 0; routeIndex < routes.size(); routeIndex++) {
                Map<String, Object> route = routes.get(routeIndex);
                List<Map<String, Object>> destinations = extractDestinations(route);
                if (destinations.isEmpty()) {
                    warnings.add("VirtualService %s/%s %s route #%d has no destinations".formatted(namespace, virtualServiceName, protocol, routeIndex));
                    continue;
                }
                for (int destIndex = 0; destIndex < destinations.size(); destIndex++) {
                    Map<String, Object> destination = destinations.get(destIndex);
                    Map<String, Object> destSpec = mapValue(destination, "destination");
                    if (destSpec == null || !destSpec.containsKey("host")) {
                        warnings.add("VirtualService %s/%s %s route #%d missing host".formatted(namespace, virtualServiceName, protocol, routeIndex));
                        continue;
                    }
                    String host = destSpec.get("host").toString();
                    String destNamespace = destSpec.getOrDefault("subsetNamespace", namespace).toString();
                    NodeBuilder target = ensureHostNode(host, destNamespace, null);
                    target.addResource(Map.of(
                            "kind", "Destination",
                            "viaVirtualService", virtualServiceName,
                            "namespace", destNamespace,
                            "config", destSpec
                    ));
                    Map<String, Object> edgeProps = new LinkedHashMap<>();
                    edgeProps.put("protocol", protocol);
                    edgeProps.put("virtualService", vsInfo);
                    edgeProps.put("route", route);
                    edgeProps.put("destination", destination);
                    String subset = destSpec.containsKey("subset") ? destSpec.get("subset").toString() : null;
                    destinationRuleIndex.findPolicy(host, destNamespace, subset).ifPresent(policy -> edgeProps.put("trafficPolicy", policy));
                    String edgeSuffix = hash(protocol + virtualServiceName + routeIndex + destIndex + host + destNamespace);
                    for (NodeBuilder source : sources) {
                        String edgeId = "%s->%s:%s".formatted(source.id, target.id, edgeSuffix);
                        edges.add(new GraphEdge(edgeId, "traffic", source.id, target.id, edgeProps));
                    }
                }
            }
        }

        private NodeBuilder ensureHostNode(String rawHost, String namespace, Map<String, Object> owningResource) {
            String canonical = canonicalHost(rawHost, namespace);
            String nodeId = "host:" + canonical;
            ServiceIndex.ServiceRecord service = serviceIndex.lookup(canonical);
            NodeBuilder node = nodes.computeIfAbsent(nodeId, id -> new NodeBuilder(id, service != null ? "service" : "host"));
            node.put("host", canonical);
            node.put("namespace", service != null ? service.namespace() : namespace);
            if (service != null) {
                node.put("service", service.name());
                node.put("selectors", service.selectors());
                node.addResource(Map.of(
                        "kind", "Service",
                        "name", service.name(),
                        "namespace", service.namespace(),
                        "spec", service.spec()
                ));
            }
            if (owningResource != null) {
                node.addResource(Map.of(
                        "kind", owningResource.getOrDefault("kind", "Resource"),
                        "resource", owningResource
                ));
            }
            return node;
        }

        private Map<String, Object> mapValue(Map<String, Object> map, String key) {
            Object value = map.get(key);
            if (value instanceof Map<?, ?> subMap) {
                return castMap(subMap);
            }
            return null;
        }

        private List<Map<String, Object>> listOfMaps(Object value) {
            if (value instanceof List<?> list) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        result.add(castMap(map));
                    }
                }
                return result;
            }
            return List.of();
        }

        private List<String> listString(Object value) {
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

        private List<Map<String, Object>> extractDestinations(Map<String, Object> route) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (String key : List.of("route", "tcp", "tls")) {
                Object value = route.get(key);
                if (value instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            result.add(castMap(map));
                        }
                    }
                }
            }
            Object mirror = route.get("mirror");
            if (mirror instanceof Map<?, ?> map) {
                result.add(Map.of("destination", castMap(map)));
            }
            return result;
        }

        private String stringValue(Map<String, Object> map, String key, String fallback) {
            Object value = map.get(key);
            return value != null ? value.toString() : fallback;
        }

        private Map<String, Object> castMap(Map<?, ?> map) {
            return map.entrySet().stream().collect(Collectors.toMap(e -> Objects.toString(e.getKey()), Map.Entry::getValue));
        }

        private String canonicalHost(String rawHost, String namespace) {
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

        private String hash(String input) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] bytes = digest.digest(input.getBytes());
                return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 10);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-1 algorithm not available", e);
            }
        }

        private static final class NodeBuilder {
            private final String id;
            private final String type;
            private final Map<String, Object> properties = new LinkedHashMap<>();
            private final List<Map<String, Object>> resources = new ArrayList<>();

            private NodeBuilder(String id, String type) {
                this.id = id;
                this.type = type;
            }

            private void put(String key, Object value) {
                if (value != null) {
                    properties.put(key, value);
                }
            }

            private void addResource(Map<String, Object> resource) {
                if (resource != null && !resource.isEmpty()) {
                    resources.add(resource);
                }
            }

            private GraphNode build() {
                Map<String, Object> props = new LinkedHashMap<>(properties);
                if (!resources.isEmpty()) {
                    props.put("resources", resources);
                }
                return new GraphNode(id, type, props);
            }
        }

        private static final class ServiceIndex {
            private final Map<String, ServiceRecord> records = new LinkedHashMap<>();

            private ServiceIndex(ResourceCollection collection) {
                List<NamespaceResources> all = new ArrayList<>();
                all.add(collection.primary());
                all.addAll(collection.extras().values());
                for (NamespaceResources resources : all) {
                    for (Map<String, Object> svc : resources.services()) {
                        Map<String, Object> metadata = castMapSafe(svc.get("metadata"));
                        Map<String, Object> spec = castMapSafe(svc.get("spec"));
                        String name = Objects.toString(metadata.get("name"), null);
                        if (name == null) {
                            continue;
                        }
                        String namespace = Objects.toString(metadata.getOrDefault("namespace", resources.namespace()), resources.namespace());
                        Map<String, Object> selectors = castMapSafe(spec.get("selector"));
                        ServiceRecord record = new ServiceRecord(name, namespace, selectors, spec);
                        Set<String> variants = new LinkedHashSet<>();
                        variants.add(name);
                        variants.add("%s.%s".formatted(name, namespace));
                        variants.add("%s.%s.svc".formatted(name, namespace));
                        variants.add("%s.%s.svc.cluster.local".formatted(name, namespace));
                        for (String variant : variants) {
                            records.put(canonicalStatic(variant, namespace), record);
                        }
                    }
                }
            }

            private ServiceRecord lookup(String canonicalHost) {
                return records.get(canonicalHost);
            }

            private static Map<String, Object> castMapSafe(Object value) {
                if (value instanceof Map<?, ?> map) {
                    return map.entrySet().stream().collect(Collectors.toMap(e -> Objects.toString(e.getKey()), Map.Entry::getValue));
                }
                return Map.of();
            }

            private static String canonicalStatic(String host, String namespace) {
                if (host == null) {
                    return null;
                }
                String normalised = host.trim().toLowerCase(Locale.ROOT);
                if (normalised.equals("mesh")) {
                    return normalised;
                }
                if (normalised.endsWith(".svc.cluster.local") || normalised.endsWith(".cluster.local")) {
                    return normalised;
                }
                if (normalised.endsWith(".svc")) {
                    return normalised + ".cluster.local";
                }
                if (normalised.contains(".svc.")) {
                    return normalised;
                }
                String[] parts = normalised.split("\\.");
                if (parts.length == 1) {
                    return "%s.%s.svc.cluster.local".formatted(normalised, namespace);
                }
                if (parts.length == 2 && parts[1].equals(namespace)) {
                    return "%s.%s.svc.cluster.local".formatted(parts[0], parts[1]);
                }
                if (parts.length == 3 && parts[1].equals(namespace) && parts[2].equals("svc")) {
                    return "%s.%s.svc.cluster.local".formatted(parts[0], parts[1]);
                }
                return normalised;
            }

            private record ServiceRecord(String name, String namespace, Map<String, Object> selectors, Map<String, Object> spec) {
            }
        }

        private static final class DestinationRuleIndex {
            private final Map<String, List<Map<String, Object>>> rules = new LinkedHashMap<>();

            private DestinationRuleIndex(ResourceCollection collection) {
                List<NamespaceResources> all = new ArrayList<>();
                all.add(collection.primary());
                all.addAll(collection.extras().values());
                for (NamespaceResources resources : all) {
                    for (Map<String, Object> rule : resources.destinationRules()) {
                        Map<String, Object> spec = castMapSafe(rule.get("spec"));
                        Object host = spec.get("host");
                        if (host == null) {
                            continue;
                        }
                        String canonical = ServiceIndex.canonicalStatic(host.toString(), resources.namespace());
                        rules.computeIfAbsent(canonical, key -> new ArrayList<>()).add(rule);
                    }
                }
            }

            private Optional<Map<String, Object>> findPolicy(String host, String namespace, String subset) {
                String canonical = ServiceIndex.canonicalStatic(host, namespace);
                List<Map<String, Object>> byHost = rules.getOrDefault(canonical, List.of());
                for (Map<String, Object> rule : byHost) {
                    Map<String, Object> spec = castMapSafe(rule.get("spec"));
                    if (subset != null) {
                        List<Map<String, Object>> subsets = listOfMaps(spec.get("subsets"));
                        for (Map<String, Object> candidate : subsets) {
                            if (subset.equals(candidate.get("name"))) {
                                Map<String, Object> policy = castMapSafe(candidate.get("trafficPolicy"));
                                if (!policy.isEmpty()) {
                                    return Optional.of(policy);
                                }
                            }
                        }
                    }
                    Map<String, Object> policy = castMapSafe(spec.get("trafficPolicy"));
                    if (!policy.isEmpty()) {
                        return Optional.of(policy);
                    }
                }
                return Optional.empty();
            }

            private static Map<String, Object> castMapSafe(Object value) {
                if (value instanceof Map<?, ?> map) {
                    return map.entrySet().stream().collect(Collectors.toMap(e -> Objects.toString(e.getKey()), Map.Entry::getValue));
                }
                return Map.of();
            }

            private static List<Map<String, Object>> listOfMaps(Object value) {
                if (value instanceof List<?> list) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            result.add(castMapSafe(map));
                        }
                    }
                    return result;
                }
                return List.of();
            }
        }
    }
}
