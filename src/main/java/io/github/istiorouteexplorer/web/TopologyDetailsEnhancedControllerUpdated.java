package io.github.istiorouteexplorer.web;

import io.fabric8.istio.client.DefaultIstioClient;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.github.istiorouteexplorer.graph.IstioTopologyBuilderEnhanced;
import io.github.istiorouteexplorer.model.TopologyGraph;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/topology")
@CrossOrigin(origins = "*")
public class TopologyDetailsEnhancedControllerUpdated {
    private final IstioClient istio;
    private final KubernetesClient k8s;
    private final IstioTopologyBuilderEnhanced builder;

    public TopologyDetailsEnhancedControllerUpdated() {
        this.istio = new DefaultIstioClient();
        this.k8s = new DefaultKubernetesClient();
        this.builder = new IstioTopologyBuilderEnhanced(istio, k8s);
    }

    @GetMapping("/diagnose-enhanced")
    public Map<String,Object> diagnose(@RequestParam String namespace) {
        TopologyGraph graph = builder.buildTopology(namespace);
        Map<String,Object> out = new HashMap<>();
        out.put("nodes", graph.getNodes().values());
        out.put("edges", graph.getEdges());
        out.put("diagnosticsBySeverity", Map.of());
        return out;
    }

    @GetMapping("/details-enhanced-v2")
    public Map<String,Object> details(@RequestParam String namespace, @RequestParam String nodeId) {
        TopologyGraph graph = builder.buildTopology(namespace);
        Map<String,Object> out = new HashMap<>();
        out.put("nodeId", nodeId);
        var node = graph.getNodes().get(nodeId);
        out.put("node", node==null? Map.of("id", nodeId): Map.of("id", node.getId(), "type", node.getType().name(), "meta", node.getMetadata()));

        List<Map<String,Object>> incoming = new ArrayList<>();
        for (var e : graph.getEdges()) if (e.getToId().equals(nodeId)) incoming.add(Map.of("from", e.getFromId(), "protocol", e.getProtocol(), "port", e.getPort(), "notes", e.getNotes(), "meta", e.getMeta()));
        List<Map<String,Object>> outgoing = new ArrayList<>();
        for (var e : graph.getEdges()) if (e.getFromId().equals(nodeId)) outgoing.add(Map.of("to", e.getToId(), "protocol", e.getProtocol(), "port", e.getPort(), "notes", e.getNotes(), "meta", e.getMeta()));

        out.put("incoming", incoming);
        out.put("outgoing", outgoing);

        // convenience: list composite edges where this node is origin or target
        List<Map<String,Object>> composite = new ArrayList<>();
        for (var e : graph.getEdges()) if ("COMPOSITE".equals(e.getProtocol()) && (e.getFromId().equals(nodeId) || e.getToId().equals(nodeId))) {
            composite.add(Map.of("from", e.getFromId(), "to", e.getToId(), "meta", e.getMeta(), "notes", e.getNotes()));
        }
        out.put("composite", composite);

        return out;
    }
}

