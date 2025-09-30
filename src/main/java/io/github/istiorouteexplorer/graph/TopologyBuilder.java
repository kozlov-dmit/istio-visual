package io.github.istiorouteexplorer.graph;

import io.github.istiorouteexplorer.model.*;
import io.github.istiorouteexplorer.model.istio.*;
import io.github.istiorouteexplorer.model.kubernetes.EndpointDto;
import io.github.istiorouteexplorer.model.kubernetes.ServiceDto;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TopologyBuilder {


    public TopologyGraph build(ResourceCollection resources) {
        TopologyGraph graph = new TopologyGraph();

        String namespace = resources.primary().namespace();

        Map<String, ServiceDto> servicesByName = resources.primary().services().stream()
                .collect(Collectors.toMap(s -> s.metadata().name(), s -> s));

        // index ServiceEntry hosts
        Set<String> serviceEntryHosts = resources.primary().serviceEntries().stream()
                .flatMap(se -> se.spec().hosts() != null ? se.spec().hosts().stream() : Stream.empty())
                .collect(Collectors.toSet());

        // add k8s service nodes
        for (ServiceDto service : resources.primary().services()) {
            String id = fqdn(service.metadata().name(), namespace);
            graph.addNode(new RouteNode(
                    id,
                    RouteNode.Type.K8S_SERVICE,
                    Map.of(
                            "service", service.metadata().name(),
                            "namespace", namespace
                    )
            ));
        }

        // process VirtualServices
        for (VirtualServiceDto virtualService : resources.primary().virtualServices()) {
            Objects.requireNonNull(virtualService.metadata(), "VirtualService must have metadata");
            Objects.requireNonNull(virtualService.spec(), "VirtualService must have specification");
            if (virtualService.spec() == null) {
                continue;
            }
            String vsName = virtualService.metadata() != null ? virtualService.metadata().name() : null;
            List<String> gateways = virtualService.spec().gateways();

            // handle HTTP routes
            if (virtualService.spec().http() != null) {
                virtualService.spec().http().forEach(httpRoute -> {
                    List<HttpRouteDestinationDto> destinations = Optional.ofNullable(httpRoute.route()).orElse(Collections.emptyList());
                    if (destinations.isEmpty()) {
                        return;
                    }
                    List<MatchCondition> matchConditions = httpRoute.match().stream()
                            .map(MatchCondition::fromHttp)
                            .flatMap(List::stream)
                            .toList();

                    for (HttpRouteDestinationDto destination : destinations) {
                        if (destination == null || destination.destination() == null) {
                            continue;
                        }
                        String host = normalizeHost(destination.destination().host(), namespace);
                        String toId = resolveHostToNodeId(host, namespace, servicesByName.keySet(), serviceEntryHosts);
                        graph.ensureNode(toId);

                        String fromId = determineSourceNodeId(namespace, gateways);
                        graph.ensureNode(fromId);

                        RouteEdge edge = new RouteEdge(
                                fromId,
                                toId,
                                "HTTP",
                                destination.destination().port(),
                                matchConditions
                        );

                        // weight
                        if (destination.weight() != null) {
                            edge.getWeights().put("default", destination.weight());
                        } else {
                            edge.getWeights().put("default", 100);
                        }

                        edge.getNotes().add("VirtualService: " + vsName);
                        if (!gateways.isEmpty()) {
                            edge.getNotes().add("gateways=" + gateways);
                        }

                        runEdgeDiagnostics(edge, serviceEntryHosts, resources.primary().endpoints());

                        graph.addEdge(edge);
                    }

                });
            }

            // handle TCP routes
            if (virtualService.spec().tcp() != null) {
                virtualService.spec().tcp().forEach(tcpRoute -> {
                    if (tcpRoute.route() != null) {
                        for (TcpRouteDestinationDto destination : tcpRoute.route()) {
                            String host = normalizeHost(destination.destination().host(), namespace);
                            String toId = resolveHostToNodeId(host, namespace, servicesByName.keySet(), serviceEntryHosts);
                            graph.ensureNode(toId);
                            String fromId = determineSourceNodeId(namespace, gateways);
                            graph.ensureNode(fromId);

                            List<MatchCondition> matchConditions = tcpRoute.match().stream()
                                    .map(MatchCondition::fromTcp)
                                    .flatMap(List::stream)
                                    .toList();

                            RouteEdge edge = new RouteEdge(
                                    fromId,
                                    toId,
                                    "TCP",
                                    destination.destination().port(),
                                    matchConditions
                            );
                            edge.getNotes().add("VirtualService: " + vsName);

                            runEdgeDiagnostics(edge, serviceEntryHosts, resources.primary().endpoints());

                            graph.addEdge(edge);
                        }
                    }
                });
            }

            if (virtualService.spec().tls() != null) {
                virtualService.spec().tls().forEach(tlsRoute -> {
                    if (tlsRoute.route() != null) {
                        for (TcpRouteDestinationDto destination : tlsRoute.route()) {
                            String host = normalizeHost(destination.destination().host(), namespace);
                            String toId = resolveHostToNodeId(host, namespace, servicesByName.keySet(), serviceEntryHosts);
                            graph.ensureNode(toId);
                            String fromId = determineSourceNodeId(namespace, gateways);
                            graph.ensureNode(fromId);

                            List<MatchCondition> matchConditions = tlsRoute.match().stream()
                                    .map(MatchCondition::fromTls)
                                    .flatMap(List::stream)
                                    .toList();

                            RouteEdge edge = new RouteEdge(
                                    fromId,
                                    toId,
                                    "TLS",
                                    destination.destination().port(),
                                    matchConditions
                            );
                            edge.getNotes().add("VirtualService: " + vsName);

                            runEdgeDiagnostics(edge, serviceEntryHosts, resources.primary().endpoints());

                            graph.addEdge(edge);
                        }
                    }
                });
            }
        }

        // process DestinationRules -> subsets -> map to endpoints
        for (DestinationRuleDto destinationRule : resources.primary().destinationRules()) {
            String drHost = destinationRule.metadata().name();
            if (drHost == null) {
                continue;
            }
            String host = normalizeHost(destinationRule.spec().host(), namespace);
            if (destinationRule.spec().subsets() != null) {
                destinationRule.spec().subsets().forEach(subset -> {
                    Map<String, String> labels = subset.getLabels();
                    List<String> matchedEndpointsId = findEndpointsByLabels(labels, resources.primary().endpoints());
                    String nodeId = resolveHostToNodeId(host, namespace, servicesByName.keySet(), serviceEntryHosts);
                    graph.ensureNode(nodeId);
                    graph.addSubsetMapping(nodeId, subset.getName(), matchedEndpointsId);
                });
            }
        }

        markGatewayEdges(graph, resources.primary().gateways());
        markEnvoyFiltersImpactedEdges(graph, resources.primary().envoyFilters());

        return graph;
    }

    private void runEdgeDiagnostics(RouteEdge edge, Set<String> serviceEntryHosts, List<EndpointDto> endpoints) {
        // MISSING_SERVICEENTRY for external hosts
        if (edge.getToId().startsWith("external:")) {
            String host = edge.getToId().substring("external:".length());
            if (!serviceEntryHosts.contains(host)) {
                edge.getDiagnostics().add(new Diagnostic(
                        Diagnostic.Severity.WARNING,
                        "MISSING_SERVICEENTRY",
                        "No ServiceEntry found for external host " + host,
                        "Create a ServiceEntry for '" + host + "' with proper port"
                ));
            }
            // if protocol == TLS and external -> possibly mtls mismatch
            if ("TLS".equals(edge.getProtocol())) {
                edge.getDiagnostics().add(new Diagnostic(
                                Diagnostic.Severity.INFO,
                                "POSSIBLE_MTLS_MISMATCH",
                                "Edge usesTLS to external host - verify that destination support mTLS",
                                "If the external service does not support mTLS, configure DestinationRule for TLS origination or change PeerAuthentication"
                        )
                );
            }
        }
        // PROTOCOL_MISMATCH if port present and name suggests http but use as TCP
        Long port = edge.getPort();
        if (port != null) {
            if (Arrays.asList(80L, 8080L, 8000L).contains(port) && "TCP".equals(edge.getProtocol())) {
                edge.getDiagnostics().add(new Diagnostic(
                        Diagnostic.Severity.INFO,
                        "PROTOCOL_MISMATCH",
                        "Edge uses TCP to port " + port + " which is HTTP/HTTPS port",
                        "Ensure port naming and protocol are correct in ServiceEntry/DestinationRule"
                        )
                );
            }
        }
    }

    private String normalizeHost(String host, String namespace) {
        if (host == null) {
            return "";
        }
        host = host.trim();
        if (host.contains(".")) {
            // if host is fqdn
            return host;
        }
        return fqdn(host, namespace);
    }

    private String fqdn(String svcName, String namespace) {
        return svcName + "." + namespace + ".svc.cluster.local";
    }

    private String resolveHostToNodeId(String host, String namespace, Set<String> svcNames, Set<String> serviceEntryHosts) {
        // get normalized short name of host (without .svc.cluster.local)
        String normalizedShort = host;
        if (host.endsWith(".svc.cluster.local")) {
            String[] parts = host.split("\\.");
            if (parts.length >= 3) normalizedShort = parts[0];
        }
        if (svcNames.contains(normalizedShort)) {
            return fqdn(normalizedShort, namespace);
        }
        // check if it's a service entry host
        if (serviceEntryHosts.contains(host) || serviceEntryHosts.contains(normalizedShort)) {
            return "serviceentry:" + host;
        }
        // otherwise it's external host
        return "external:" + host;
    }

    private String determineSourceNodeId(String namespace, List<String> gateways) {
        // if vs bound to mesh only -> traffic originated from mesh
        if (gateways == null || gateways.isEmpty() || (gateways.size() == 1 && gateways.get(0).equals("mesh"))) {
            return "mesh:internal-traffic:" + namespace;
        }
        // if gateways point to gateway - use gateway id
        return gateways.stream()
                .findFirst()
                .map(gw -> {
                    if (gw.contains("."))
                        return "gateway:" + gw;
                    return "gateway:" + namespace + "/" + gw;
                })
                .orElse("mesh:internal-trafic:" + namespace);
    }

    private List<String> findEndpointsByLabels(Map<String, String> labels, List<EndpointDto> endpoints) {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> matched = new ArrayList<>();
        for (EndpointDto endpoint : endpoints) {
            if (endpoint.getMetadata() == null) {
                continue;
            }
            Map<String, String> metadataLabels = endpoint.getMetadata().labels();
            if (labels.entrySet().stream().allMatch(e -> e.getValue().equals(metadataLabels.get(e.getKey())))) {
                matched.add(endpoint.getMetadata().name());
            }
        }
        return matched;
    }

    private void markGatewayEdges(TopologyGraph graph, List<GatewayDto> gateways) {
        // if a gateway exist, mark edges which mention gateway in notes as external-ingress
        for (RouteEdge edge : graph.getEdges()) {
            boolean mentionsGateway = edge.getNotes().stream().anyMatch(note -> note.startsWith("gateways="));
            if (mentionsGateway) {
                edge.getNotes().add("via-gateway");
            }
        }
    }

    private void markEnvoyFiltersImpactedEdges(TopologyGraph graph, List<EnvoyFilterDto> envoyFilters) {
        if (envoyFilters == null || envoyFilters.isEmpty()) {
            return;
        }
        for (EnvoyFilterDto envoyFilter : envoyFilters) {
            String name = envoyFilter.getMetadata().name();
            graph.getEdges().forEach(edge -> edge.getNotes().add("envoyfilter:" + name));
        }
    }

}
