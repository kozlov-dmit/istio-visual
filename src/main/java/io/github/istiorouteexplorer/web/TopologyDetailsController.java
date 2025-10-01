package io.github.istiorouteexplorer.web;

import io.fabric8.istio.api.networking.v1beta1.DestinationRule;
import io.fabric8.istio.api.networking.v1beta1.ServiceEntry;
import io.fabric8.istio.api.networking.v1beta1.VirtualService;
import io.fabric8.istio.client.DefaultIstioClient;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.github.istiorouteexplorer.graph.TopologyBuilder;
import io.github.istiorouteexplorer.model.TopologyGraph;
import io.github.istiorouteexplorer.service.RouteExplorerService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/topology")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TopologyDetailsController {

    private final IstioClient istio;
    private final KubernetesClient k8s;
    private final RouteExplorerService explorerService;

    /**
     * Возвращает расширённую информацию по узлу (nodeId). nodeId — тот же идентификатор, что возвращает TopologyGraph, например:
     * - k8s svc: "svc-a.default.svc.cluster.local"
     * - external: "external:kafka.example.com"
     * - serviceentry: "serviceentry:kafka.example.com"
     */
    @GetMapping("/details")
    public Map<String,Object> details(@RequestParam String namespace, @RequestParam String nodeId) {
        TopologyGraph graph = explorerService.buildTopology(namespace);

        Map<String,Object> out = new HashMap<>();
        out.put("nodeId", nodeId);

        // basic diagnostics and node info available from graph
        var node = graph.getNodes().get(nodeId);
        out.put("node", node == null ? Map.of("id", nodeId) : Map.of("id", node.getId(), "type", node.getType().name(), "meta", node.getMetadata()));
        out.put("nodeDiagnostics", graph.getNodeDiagnostics().getOrDefault(nodeId, Collections.emptyList()));

        // edges in/out
        List<Map<String,Object>> incoming = graph.getEdges().stream()
                .filter(e -> e.getToId().equals(nodeId))
                .map(e -> Map.of("from", e.getFromId(), "protocol", e.getProtocol(), "port", e.getPort(), "notes", e.getNotes(), "diagnostics", e.getDiagnostics()))
                .collect(Collectors.toList());
        List<Map<String,Object>> outgoing = graph.getEdges().stream()
                .filter(e -> e.getFromId().equals(nodeId))
                .map(e -> Map.of("to", e.getToId(), "protocol", e.getProtocol(), "port", e.getPort(), "notes", e.getNotes(), "diagnostics", e.getDiagnostics()))
                .collect(Collectors.toList());
        out.put("incoming", incoming);
        out.put("outgoing", outgoing);

        // Try to find VirtualServices that route to this node
        List<Map<String,Object>> vss = new ArrayList<>();
        try {
            List<VirtualService> vslist = istio.v1beta1().virtualServices().inNamespace(namespace).list().getItems();
            for (VirtualService vs : vslist) {
                String vsName = Optional.ofNullable(vs.getMetadata()).map(m->m.getName()).orElse("<vs>");
                boolean references = false;
                if (vs.getSpec() != null) {
                    if (vs.getSpec().getHttp() != null) {
                        for (var http : vs.getSpec().getHttp()) {
                            if (http.getRoute() != null) {
                                for (var r : http.getRoute()) {
                                    if (r != null && r.getDestination() != null && normalize(r.getDestination().getHost(), namespace).equals(nodeId)) references = true;
                                }
                            }
                        }
                    }
                    if (vs.getSpec().getTcp() != null) {
                        for (var tcp: vs.getSpec().getTcp()) {
                            if (tcp.getRoute()!=null) for (var r: tcp.getRoute()) if (r.getDestination()!=null && normalize(r.getDestination().getHost(), namespace).equals(nodeId)) references = true;
                        }
                    }
                    if (vs.getSpec().getTls() != null) {
                        for (var tls: vs.getSpec().getTls()) {
                            if (tls.getRoute()!=null) for (var r: tls.getRoute()) if (r.getDestination()!=null && normalize(r.getDestination().getHost(), namespace).equals(nodeId)) references = true;
                        }
                    }
                }
                if (references) vss.add(Map.of("name", vsName, "gateways", Optional.ofNullable(vs.getSpec()).map(s->s.getGateways()).orElse(Collections.emptyList())));
            }
        } catch (Exception ex) { /* best-effort */ }
        out.put("virtualServices", vss);

        // DestinationRules matching this host
        List<Map<String,Object>> drs = new ArrayList<>();
        try {
            List<DestinationRule> drlist = istio.v1beta1().destinationRules().inNamespace(namespace).list().getItems();
            for (DestinationRule dr: drlist) {
                String host = Optional.ofNullable(dr.getSpec()).map(s->s.getHost()).orElse("");
                if (normalize(host, namespace).equals(nodeId)) {
                    drs.add(Map.of("name", Optional.ofNullable(dr.getMetadata()).map(m->m.getName()).orElse("<dr>"), "subsets", Optional.ofNullable(dr.getSpec()).map(s->s.getSubsets()).orElse(Collections.emptyList())));
                }
            }
        } catch (Exception ex) { }
        out.put("destinationRules", drs);

        // ServiceEntry if external
        List<Map<String,Object>> ses = new ArrayList<>();
        try {
            List<ServiceEntry> selist = istio.v1beta1().serviceEntries().inNamespace(namespace).list().getItems();
            for (ServiceEntry se: selist) {
                var spec = se.getSpec();
                if (spec!=null && spec.getHosts()!=null) {
                    for (String h: spec.getHosts()) {
                        if (normalize(h, namespace).equals(nodeId) || (nodeId.startsWith("external:") && nodeId.equals("external:"+h))) {
                            ses.add(Map.of("name", Optional.ofNullable(se.getMetadata()).map(m->m.getName()).orElse("<se>"), "hosts", spec.getHosts(), "ports", spec.getPorts()));
                        }
                    }
                }
            }
        } catch (Exception ex) { }
        out.put("serviceEntries", ses);

        // Sidecars that might restrict egress
        List<Map<String,Object>> scs = new ArrayList<>();
        try {
            var scList = istio.v1beta1().sidecars().inNamespace(namespace).list().getItems();
            for (var sc: scList) {
                var spec = sc.getSpec();
                if (spec!=null && spec.getEgress()!=null) {
                    var hosts = new ArrayList<String>();
                    for (var l: spec.getEgress()) if (l.getHosts()!=null) hosts.addAll(l.getHosts());
                    // if any host pattern matches nodeId, include
                    boolean matches = hosts.stream().anyMatch(p->matchPattern(nodeId, p, namespace));
                    scs.add(Map.of("name", Optional.ofNullable(sc.getMetadata()).map(m->m.getName()).orElse("<sc>"), "hosts", hosts, "matches", matches));
                }
            }
        } catch (Exception ex) { }
        out.put("sidecars", scs);

        // minimal TLS hints: if any DestinationRule has TLS settings for this host
        List<Map<String,Object>> tlsHints = new ArrayList<>();
        try {
            for (DestinationRule dr: istio.v1beta1().destinationRules().inNamespace(namespace).list().getItems()) {
                if (normalize(Optional.ofNullable(dr.getSpec()).map(s->s.getHost()).orElse(""), namespace).equals(nodeId)) {
                    var drSpec = dr.getSpec();
                    if (drSpec != null && drSpec.getTrafficPolicy()!=null && drSpec.getTrafficPolicy().getTls()!=null) {
                        tlsHints.add(Map.of("dr", Optional.ofNullable(dr.getMetadata()).map(m->m.getName()).orElse("<dr>"), "tls", drSpec.getTrafficPolicy().getTls()));
                    }
                }
            }
        } catch (Exception ex) { }
        out.put("tlsHints", tlsHints);

        // Gateway usage (best-effort): list Gateways referencing this host
        // For simplicity we just return VirtualService gateways already included above.

        // EnvoyFilters that exist in ns (names only) — may modify behavior
        try {
            var efs = istio.v1alpha3().envoyFilters().inNamespace(namespace).list().getItems();
            out.put("envoyFilters", efs.stream().map(ef->Optional.ofNullable(ef.getMetadata()).map(m->m.getName()).orElse("<ef>")).collect(Collectors.toList()));
        } catch (Exception ex) { out.put("envoyFilters", Collections.emptyList()); }

        out.put("notes", List.of("Best-effort diagnostics; for deeper probing use /api/topology/diagnose and /api/probe"));

        return out;
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