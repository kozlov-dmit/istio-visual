package io.github.istiorouteexplorer.graph;

import io.fabric8.istio.api.networking.v1beta1.DestinationRule;
import io.fabric8.istio.api.networking.v1beta1.ServiceEntry;
import io.fabric8.istio.api.networking.v1beta1.VirtualService;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.github.istiorouteexplorer.model.RouteEdge;
import io.github.istiorouteexplorer.model.RouteNode;
import io.github.istiorouteexplorer.model.TopologyGraph;

import java.util.*;

/**
 * Enhanced topology builder which follows egress-gateway routes and creates composite edges
 * (svc -> external via egress) with port mapping when possible.
 *
 * This is a best-effort implementation: it scans VirtualServices in the target namespace
 * and in istio-system to find routing from services -> egress -> external.
 */
public class IstioTopologyBuilderEnhanced {

    private final IstioClient istio;
    private final KubernetesClient k8s;

    public IstioTopologyBuilderEnhanced(IstioClient istio, KubernetesClient k8s) {
        this.istio = istio;
        this.k8s = k8s;
    }

    public TopologyGraph buildTopology(String namespace) {
        TopologyGraph graph = new TopologyGraph();

        // load resources
        List<VirtualService> vsLocal = Optional.ofNullable(istio.v1beta1().virtualServices().inNamespace(namespace).list()).map(l->l.getItems()).orElse(Collections.emptyList());
        List<VirtualService> vsIstioSystem = Optional.ofNullable(istio.v1beta1().virtualServices().inNamespace("istio-system").list()).map(l->l.getItems()).orElse(Collections.emptyList());
        List<ServiceEntry> seLocal = Optional.ofNullable(istio.v1beta1().serviceEntries().inNamespace(namespace).list()).map(l->l.getItems()).orElse(Collections.emptyList());
        List<DestinationRule> drLocal = Optional.ofNullable(istio.v1beta1().destinationRules().inNamespace(namespace).list()).map(l->l.getItems()).orElse(Collections.emptyList());

        // helper: record nodes for all serviceentries and externals discovered
        for (ServiceEntry se : seLocal) {
            if (se.getSpec() != null && se.getSpec().getHosts() != null) {
                for (String h : se.getSpec().getHosts()) {
                    String nodeId = normalizeHost(h, namespace);
                    graph.getNodes().putIfAbsent(nodeId, new RouteNode(nodeId, RouteNode.Type.SERVICEENTRY));
                }
            }
        }

        // add an egress pseudo-node for istio-egressgateway
        final String EGRESS_NODE_ID = "egress:istio-egressgateway.istio-system.svc.cluster.local";
        graph.getNodes().putIfAbsent(EGRESS_NODE_ID, new RouteNode(EGRESS_NODE_ID, RouteNode.Type.EGRESS));

        // scan local VS for routes that point to egress
        for (VirtualService vs : vsLocal) {
            String vsName = Optional.ofNullable(vs.getMetadata()).map(m->m.getName()).orElse("<vs>");
            // make a synthetic "from" id representing this VS (so UI can attach it)
            String fromNodeId = "virtualservice:" + vsName + "." + namespace;
            graph.getNodes().putIfAbsent(fromNodeId, new RouteNode(fromNodeId, RouteNode.Type.VIRTUALSERVICE));

            // inspect http/tcp/tls routes (best-effort)
            if (vs.getSpec() != null) {
                var spec = vs.getSpec();
                // HTTP
                if (spec.getHttp() != null) {
                    spec.getHttp().forEach(http -> {
                        if (http.getRoute() != null) {
                            http.getRoute().forEach(r -> {
                                if (r.getDestination() != null) {
                                    String destHostRaw = Optional.ofNullable(r.getDestination().getHost()).orElse("");
                                    String destHost = normalizeHost(destHostRaw, namespace);
                                    Long destPort = r.getDestination().getPort() != null ? r.getDestination().getPort().getNumber() : null;

                                    if (isEgressGatewayHost(destHost)) {
                                        // found a route svc -> egress
                                        RouteEdge e = new RouteEdge(fromNodeId, EGRESS_NODE_ID, "HTTP", destPort);
                                        e.getNotes().add("via-egress-destination-host:" + destHostRaw);
                                        graph.getEdges().add(e);

                                        // try to follow from egress to external via istio-system VS/SE
                                        followEgressToExternal(graph, EGRESS_NODE_ID, destHostRaw, destPort, vsIstioSystem, seLocal, namespace, fromNodeId);
                                    }
                                }
                            });
                        }
                    });
                }

                // TCP
                if (spec.getTcp() != null) {
                    spec.getTcp().forEach(tcp -> {
                        if (tcp.getRoute() != null) {
                            tcp.getRoute().forEach(r -> {
                                if (r.getDestination() != null) {
                                    String destHostRaw = Optional.ofNullable(r.getDestination().getHost()).orElse("");
                                    String destHost = normalizeHost(destHostRaw, namespace);
                                    Long destPort = r.getDestination().getPort() != null ? r.getDestination().getPort().getNumber() : null;
                                    if (isEgressGatewayHost(destHost)) {
                                        RouteEdge e = new RouteEdge(fromNodeId, EGRESS_NODE_ID, "TCP", destPort);
                                        e.getNotes().add("via-egress-destination-host:" + destHostRaw);
                                        graph.getEdges().add(e);
                                        followEgressToExternal(graph, EGRESS_NODE_ID, destHostRaw, destPort, vsIstioSystem, seLocal, namespace, fromNodeId);
                                    }
                                }
                            });
                        }
                    });
                }

                // TLS
                if (spec.getTls() != null) {
                    spec.getTls().forEach(tls -> {
                        if (tls.getRoute() != null) {
                            tls.getRoute().forEach(r -> {
                                if (r.getDestination() != null) {
                                    String destHostRaw = Optional.ofNullable(r.getDestination().getHost()).orElse("");
                                    String destHost = normalizeHost(destHostRaw, namespace);
                                    Long destPort = r.getDestination().getPort() != null ? r.getDestination().getPort().getNumber() : null;
                                    if (isEgressGatewayHost(destHost)) {
                                        RouteEdge e = new RouteEdge(fromNodeId, EGRESS_NODE_ID, "TLS", destPort);
                                        e.getNotes().add("via-egress-destination-host:" + destHostRaw);
                                        graph.getEdges().add(e);
                                        followEgressToExternal(graph, EGRESS_NODE_ID, destHostRaw, destPort, vsIstioSystem, seLocal, namespace, fromNodeId);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        }

        // return graph; other existing edges may also be built elsewhere in the app
        return graph;
    }

    private void followEgressToExternal(TopologyGraph graph, String egressNodeId, String egressDestHostRaw, Long egressPort, List<VirtualService> vsIstioSystem, List<ServiceEntry> seLocal, String namespace, String originFromId) {
        // Look for VirtualServices in istio-system that reference egress gateway and route from it to external hosts
        for (VirtualService vs : vsIstioSystem) {
            if (vs.getSpec() == null) continue;
            var spec = vs.getSpec();

            if (spec.getHttp() != null) {
                for (var http : spec.getHttp()) {
                    if (http.getRoute() == null) continue;
                    for (var r : http.getRoute()) {
                        if (r.getDestination() == null) continue;
                        String host = Optional.ofNullable(r.getDestination().getHost()).orElse("");
                        Long port = r.getDestination().getPort() != null ? r.getDestination().getPort().getNumber() : null;
                        String normalized = normalizeHost(host, namespace);
                        // check if this VS likely maps traffic from egress to external host
                        // heuristic: gateway VS often have hosts equal to external FQDNs or ServiceEntry hosts
                        if (isExternalHostCandidate(host)) {
                            // create external node
                            String externalNodeId = host.startsWith("external:") ? host : "external:" + host;
                            graph.getNodes().putIfAbsent(externalNodeId, new RouteNode(externalNodeId, RouteNode.Type.EXTERNAL));

                            // edge egress -> external
                            RouteEdge e2 = new RouteEdge(egressNodeId, externalNodeId, "HTTP", port);
                            e2.getNotes().add("egress-virtualservice:" + Optional.ofNullable(vs.getMetadata()).map(m->m.getName()).orElse("<vs>") );
                            graph.getEdges().add(e2);

                            // composite edge from origin -> external with port map
                            RouteEdge composite = new RouteEdge(originFromId, externalNodeId, "COMPOSITE", null);
                            composite.getNotes().add("via-egress");
                            Map<String,Object> portMap = new HashMap<>();
                            if (egressPort != null) portMap.put("fromPort", egressPort);
                            if (port != null) portMap.put("toPort", port);
                            composite.getMeta().put("portMap", portMap);
                            composite.getMeta().put("via", egressNodeId);
                            graph.getEdges().add(composite);
                        }
                    }
                }
            }
        }

        // fallback: check ServiceEntry hosts for matching host
        for (ServiceEntry se : seLocal) {
            if (se.getSpec() == null || se.getSpec().getHosts() == null) continue;
            for (String h : se.getSpec().getHosts()) {
                if (h.equalsIgnoreCase(egressDestHostRaw) || normalizeHost(h, namespace).equals(normalizeHost(egressDestHostRaw, namespace))) {
                    String externalNodeId = "external:" + h;
                    graph.getNodes().putIfAbsent(externalNodeId, new RouteNode(externalNodeId, RouteNode.Type.EXTERNAL));
                    RouteEdge e2 = new RouteEdge(egressNodeId, externalNodeId, "TCP", null);
                    e2.getNotes().add("serviceentry:" + Optional.ofNullable(se.getMetadata()).map(m->m.getName()).orElse("<se>"));
                    graph.getEdges().add(e2);

                    // composite
                    RouteEdge composite = new RouteEdge(originFromId, externalNodeId, "COMPOSITE", null);
                    composite.getNotes().add("via-egress");
                    Map<String,Object> portMap = new HashMap<>(); if (egressPort != null) portMap.put("fromPort", egressPort);
                    composite.getMeta().put("portMap", portMap);
                    composite.getMeta().put("via", egressNodeId);
                    graph.getEdges().add(composite);
                }
            }
        }
    }

    private boolean isEgressGatewayHost(String host) {
        if (host == null) return false;
        // heuristic: egress gateway common names
        return host.contains("egressgateway") || host.contains("egress-gateway") || host.contains("istio-egressgateway");
    }

    private boolean isExternalHostCandidate(String host) {
        if (host == null) return false;
        // heuristic: contains dot or looks like a fqdn
        return host.contains(".") || host.startsWith("external:");
    }

    private String normalizeHost(String host, String namespace) {
        if (host == null) return "";
        if (host.startsWith("external:")) return host;
        if (host.contains(".")) return host;
        return host + "." + namespace + ".svc.cluster.local";
    }

}
