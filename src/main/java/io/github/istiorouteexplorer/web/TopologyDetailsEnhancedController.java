package io.github.istiorouteexplorer.web;

import io.fabric8.istio.api.networking.v1beta1.ServiceEntry;
import io.fabric8.istio.api.networking.v1beta1.VirtualService;
import io.fabric8.istio.api.networking.v1beta1.DestinationRule;
import io.fabric8.istio.api.networking.v1beta1.Sidecar;
import io.fabric8.istio.client.DefaultIstioClient;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.github.istiorouteexplorer.model.Diagnostic;
import io.github.istiorouteexplorer.model.RouteEdge;
import io.github.istiorouteexplorer.model.RouteNode;
import io.github.istiorouteexplorer.model.TopologyGraph;
import io.github.istiorouteexplorer.service.RouteExplorerService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced details controller: returns structured, human-readable data for a topology node.
 * Endpoint: GET /api/topology/details-enhanced?namespace=...&nodeId=...
 */
@RestController
@RequestMapping("/api/topology")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TopologyDetailsEnhancedController {

    private final IstioClient istio;
    private final KubernetesClient k8s;
    private final RouteExplorerService routeExplorerService;

    @GetMapping("/details-enhanced")
    public Map<String,Object> details(@RequestParam String namespace, @RequestParam String nodeId) {
        TopologyGraph graph = routeExplorerService.buildTopology(namespace);

        Map<String,Object> out = new LinkedHashMap<>();
        out.put("nodeId", nodeId);

        // Basic node info
        RouteNode node = graph.getNodes().get(nodeId);
        out.put("basic", Map.of(
                "id", nodeId,
                "type", node == null ? "UNKNOWN" : node.getType().name(),
                "meta", node == null ? Collections.emptyMap() : node.getMetadata()
        ));

        // incoming/outgoing (human-friendly)
        List<RouteEdge> incoming = graph.getEdges().stream().filter(e -> e.getToId().equals(nodeId)).collect(Collectors.toList());
        List<RouteEdge> outgoing = graph.getEdges().stream().filter(e -> e.getFromId().equals(nodeId)).collect(Collectors.toList());

        out.put("incomingRoutes", incoming.stream().map(this::edgeSummary).collect(Collectors.toList()));
        out.put("outgoingRoutes", outgoing.stream().map(this::edgeSummary).collect(Collectors.toList()));

        // VirtualServices that reference this host (search across namespace)
        List<Map<String,Object>> virtualServices = new ArrayList<>();
        try {
            var vsList = istio.v1beta1().virtualServices().inNamespace(namespace).list().getItems();
            for (VirtualService vs : vsList) {
                var name = Optional.ofNullable(vs.getMetadata()).map(m->m.getName()).orElse("<vs>");
                var referenced = false;
                var routes = new ArrayList<Map<String,Object>>();
                if (vs.getSpec() != null) {
                    if (vs.getSpec().getHttp() != null) {
                        for (var http : vs.getSpec().getHttp()) {
                            if (http.getRoute()!=null) for (var r : http.getRoute()) {
                                var host = r.getDestination() == null ? null : normalize(r.getDestination().getHost(), namespace);
                                if (host != null) {
                                    referenced = referenced || host.equals(nodeId);
                                    routes.add(Map.of("type","HTTP","host", host, "weight", r.getWeight()));
                                }
                            }
                        }
                    }
                }
                if (referenced) virtualServices.add(Map.of("name", name, "routes", routes));
            }
        } catch (Exception ex) {
            // ignore
        }
        out.put("virtualServices", virtualServices);

        // DestinationRules
        List<Map<String,Object>> drs = new ArrayList<>();
        try {
            var list = istio.v1beta1().destinationRules().inNamespace(namespace).list().getItems();
            for (DestinationRule dr : list) {
                var host = Optional.ofNullable(dr.getSpec()).map(s->s.getHost()).orElse("");
                if (normalize(host, namespace).equals(nodeId)) {
                    var name = Optional.ofNullable(dr.getMetadata()).map(m->m.getName()).orElse("<dr>");
                    var subsets = Optional.ofNullable(dr.getSpec()).map(s->s.getSubsets()).orElse(Collections.emptyList());
                    var subsetsInfo = subsets.stream().map(ss -> Map.of("name", ss.getName(), "labels", Optional.ofNullable(ss.getLabels()).orElse(Collections.emptyMap()))).collect(Collectors.toList());
                    drs.add(Map.of("name", name, "subsets", subsetsInfo, "hasTrafficPolicy", Optional.ofNullable(dr.getSpec()).map(s->s.getTrafficPolicy()!=null).orElse(false)));
                }
            }
        } catch (Exception ex) { }
        out.put("destinationRules", drs);

        // ServiceEntry info
        List<Map<String,Object>> ses = new ArrayList<>();
        try {
            var list = istio.v1beta1().serviceEntries().inNamespace(namespace).list().getItems();
            for (ServiceEntry se : list) {
                var spec = se.getSpec();
                if (spec != null && spec.getHosts() != null) {
                    for (String h: spec.getHosts()) {
                        if (normalize(h, namespace).equals(nodeId) || nodeId.equals("external:"+h)) {
                            var ports = Optional.ofNullable(spec.getPorts()).orElse(Collections.emptyList()).stream()
                                    .map(p -> Map.of("name", p.getName(), "number", p.getNumber(), "protocol", p.getProtocol()))
                                    .collect(Collectors.toList());
                            ses.add(Map.of("name", Optional.ofNullable(se.getMetadata()).map(m->m.getName()).orElse("<se>"), "hosts", spec.getHosts(), "ports", ports, "resolution", spec.getResolution()));
                        }
                    }
                }
            }
        } catch (Exception ex) { }
        out.put("serviceEntries", ses);

        // Sidecars that may restrict access
        List<Map<String,Object>> sidecars = new ArrayList<>();
        try {
            var list = istio.v1beta1().sidecars().inNamespace(namespace).list().getItems();
            for (Sidecar sc : list) {
                var name = Optional.ofNullable(sc.getMetadata()).map(m->m.getName()).orElse("<sc>");
                var hosts = Optional.ofNullable(sc.getSpec()).map(s -> Optional.ofNullable(s.getEgress()).orElse(Collections.emptyList()).stream().flatMap(l->Optional.ofNullable(l.getHosts()).orElse(Collections.emptyList()).stream()).collect(Collectors.toList())).orElse(Collections.emptyList());
                boolean matches = hosts.stream().anyMatch(p -> matchPattern(nodeId, p, namespace));
                sidecars.add(Map.of("name", name, "hosts", hosts, "matches", matches));
            }
        } catch (Exception ex) { }
        out.put("sidecars", sidecars);

        // EnvoyFilters present
        try {
            var efs = istio.v1alpha3().envoyFilters().inNamespace(namespace).list().getItems();
            out.put("envoyFilters", efs.stream().map(ef -> Optional.ofNullable(ef.getMetadata()).map(m->m.getName()).orElse("<ef>")).collect(Collectors.toList()));
        } catch (Exception ex) { out.put("envoyFilters", Collections.emptyList()); }

        // TLS hints - simple: find DR trafficPolicy.tls or DestinationRule subsets with tls
        List<Map<String,Object>> tlsHints = new ArrayList<>();
        try {
            var list = istio.v1beta1().destinationRules().inNamespace(namespace).list().getItems();
            for (DestinationRule dr : list) {
                var host = Optional.ofNullable(dr.getSpec()).map(s->s.getHost()).orElse("");
                if (normalize(host, namespace).equals(nodeId)) {
                    if (dr.getSpec()!=null && dr.getSpec().getTrafficPolicy()!=null && dr.getSpec().getTrafficPolicy().getTls()!=null) {
                        tlsHints.add(Map.of("dr", Optional.ofNullable(dr.getMetadata()).map(m->m.getName()).orElse("<dr>"), "tls", dr.getSpec().getTrafficPolicy().getTls()));
                    }
                }
            }
        } catch (Exception ex) { }
        out.put("tlsHints", tlsHints);

        // diagnostics aggregated
        List<Diagnostic> nodeDiags = graph.getNodeDiagnostics().getOrDefault(nodeId, Collections.emptyList());
        out.put("nodeDiagnostics", nodeDiags.stream().map(d->Map.of("severity", d.getSeverity().name(), "code", d.getCode(), "message", d.getMessage(), "suggestion", d.getSuggestion())).collect(Collectors.toList()));

        // boolean: is there any route from mesh -> this node?
        boolean hasMeshToNode = graph.getEdges().stream().anyMatch(e -> {
            // from is mesh-service and to equals nodeId
            boolean fromIsMesh = graph.getNodes().get(e.getFromId()) != null && graph.getNodes().get(e.getFromId()).getType() == RouteNode.Type.K8S_SERVICE;
            return fromIsMesh && e.getToId().equals(nodeId);
        });
        out.put("meshReachable", hasMeshToNode);

        // If not reachable, provide likely causes & suggested commands
        if (!hasMeshToNode) {
            List<String> causes = new ArrayList<>();
            causes.add("Нет видимых маршрутов из mesh к этому хосту в namespace — это может быть нормально, если маршрутизация сделана в другом namespace или через egress-gateway с VS в другом неймспейсе.");
            causes.add("Нет ServiceEntry для внешнего хоста (ServiceEntry отсутствует или находится в другом namespace).");
            causes.add("Sidecar с egress-ограничениями блокирует исходящие хосты из этого namespace.");
            causes.add("VirtualService, который направляет трафик, находится не в этом namespace или не ссылается на этот host в ожидаемом виде (без FQDN).");
            causes.add("mTLS / DestinationRule mismatch — клиент пытается mTLS к внешнему серверу, но тот его не поддерживает.");

            List<String> suggest = new ArrayList<>();
            suggest.add("Проверьте ServiceEntry: kubectl get serviceentry -n " + namespace + " -o yaml");
            suggest.add("Проверьте VirtualServices: kubectl get virtualservice -n " + namespace + " -o yaml");
            suggest.add("Проверьте Sidecar: kubectl get sidecar -n " + namespace + " -o yaml");
            suggest.add("Проверьте DestinationRules: kubectl get destinationrule -n " + namespace + " -o yaml");
            suggest.add("Проверьте, не направлен ли трафик через egress-gateway в другом namespace (проверьте gateways в VirtualService)");
            suggest.add("В pod с sidecar: istioctl proxy-config cluster <pod> -n " + namespace + " и istioctl proxy-config routes <pod> -n " + namespace);

            out.put("likelyCauses", causes);
            out.put("suggestedCommands", suggest);
        }

        return out;
    }

    private Map<String,Object> edgeSummary(RouteEdge e) {
        return Map.of(
                "from", e.getFromId(),
                "to", e.getToId(),
                "protocol", e.getProtocol(),
                "port", e.getPort(),
                "notes", e.getNotes(),
                "diagnostics", e.getDiagnostics().stream().map(d -> Map.of("severity", d.getSeverity().name(), "code", d.getCode(), "message", d.getMessage(), "suggestion", d.getSuggestion())).collect(Collectors.toList())
        );
    }

    private String normalize(String host, String namespace) {
        if (host == null) return "";
        host = host.trim();
        if (host.contains(".")) return host;
        return host + "." + namespace + ".svc.cluster.local";
    }

    private boolean matchPattern(String nodeId, String pattern, String namespace) {
        if (pattern == null) return false;
        if (pattern.equals("*")) return true;
        String cleaned = pattern;
        if (!pattern.contains(".")) cleaned = pattern + "." + namespace + ".svc.cluster.local";
        return nodeId.endsWith(cleaned) || nodeId.contains(cleaned) || nodeId.equals("external:"+pattern);
    }
}

