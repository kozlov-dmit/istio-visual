package io.github.istiorouteexplorer.web;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.github.istiorouteexplorer.model.TopologyGraph;
import io.github.istiorouteexplorer.service.RouteExplorerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TopologyDiagnosticController {

    private final RouteExplorerService routeExplorerService;
    private final KubernetesClient k8s;

    @GetMapping("/topology/diagnose")
    public Map<String, Object> diagnose(@RequestParam(defaultValue = "default") String namespace) {
        TopologyGraph graph = routeExplorerService.buildTopology(namespace);

        
        List<Map<String, Object>> nodes = graph.getNodes().values().stream()
                .map(n -> Map.<String, Object>of(
                        "id", n.getId(),
                        "type", n.getType().name(),
                        "meta", n.getMetadata(),
                        "nodeDiagnostics", graph.getNodeDiagnostics().getOrDefault(n.getId(), Collections.emptyList())
                        ))
                .collect(Collectors.toList());

        List<Map<String, Object>> edges = graph.getEdges().stream().map(e -> {
            List<Map<String, Object>> diags = e.getDiagnostics().stream()
                    .map(d -> Map.<String, Object>of(
                            "severity", d.getSeverity().name(),
                            "code", d.getCode(),
                            "message", d.getMessage(),
                            "suggestion", d.getSuggestion()))
                    .collect(Collectors.toList());

            return Map.<String, Object>of(
                    "fromId", e.getFromId(),
                    "toId", e.getToId(),
                    "protocol", e.getProtocol(),
                    "port", e.getPort(),
                    "notes", e.getNotes(),
                    "weights", e.getWeights(),
                    "diagnostics", diags);
        }).collect(Collectors.toList());

        Map<String, Long> counts = graph.getEdges().stream()
                .flatMap(e -> e.getDiagnostics().stream())
                .collect(Collectors.groupingBy(d -> d.getSeverity().name(), Collectors.counting()));

        return Map.of(
                "nodes", nodes,
                "edges", edges,
                "summary", Map.of(
                        "totalNodes", nodes.size(),
                        "totalEdges", edges.size(),
                        "diagnosticsBySeverity", counts));
    }

    @PostMapping("/probe")
    public Map<String, Object> probe(@RequestBody Map<String, String> req) {
        // Lightweight synchronous probe helper — best-effort and safe. This
        // implementation does not create Jobs.
        // Expected request: { "namespace":"default", "fromPod":"mypod",
        // "host":"kafka.example.com", "port":"9092" }
        String namespace = req.getOrDefault("namespace", "default");
        String fromPod = req.get("fromPod");
        String host = req.getOrDefault("host", "");
        String port = req.getOrDefault("port", "");

        // If fromPod isn't provided, return a suggested set of commands instead of
        // executing anything.
        if (fromPod == null || fromPod.isEmpty()) {
            return Map.of(
                    "status", "not_executed",
                    "message",
                    "No fromPod provided — cannot exec inside cluster. Use 'fromPod' to run probes from a workload pod.",
                    "suggestedCommands", List.of(
                            String.format("kubectl exec -n %s -it <pod> -- nslookup %s", namespace, host),
                            String.format(
                                    "kubectl exec -n %s -it <pod> -- bash -c \"timeout 5 bash -c '</dev/tcp/%s/%s' && echo ok || echo fail\"",
                                    namespace, host, port),
                            String.format("kubectl exec -n %s -it <pod> -- curl -v telnet://%s:%s", namespace, host,
                                    port)));
        }

        // If user provided a pod name, we can attempt a best-effort exec via fabric8
        // (if environment allows).
        try {
            var pod = k8s.pods().inNamespace(namespace).withName(fromPod);
            if (pod.get() == null) {
                return Map.of("status", "error", "message", "Pod not found: " + fromPod);
            }
            // Build a set of small probes and attempt to exec sequentially. This is
            // synchronous and may block; keep simple.
            List<Map<String, Object>> results = new ArrayList<>();

            // 1) DNS
            try (var in = pod.redirectingInput().writingOutput(System.out).writingError(System.err).exec("nslookup",
                    host)) {
                // fabric8 exec streams are used here just as attempt; reading results properly
                // requires handling IO streams.
                results.add(Map.of("probe", "dns", "status", "executed"));
            } catch (Exception ex) {
                results.add(Map.of("probe", "dns", "status", "failed", "error", ex.getMessage()));
            }

            // 2) TCP connect (uses bash)
            String tcpCmd = "bash";
            String tcpArg = "-c";
            String tcpExpr = String.format("timeout 5 bash -c '</dev/tcp/%s/%s' && echo ok || echo fail", host, port);
            try (ExecWatch in = pod.exec(tcpCmd, tcpArg, tcpExpr)) {
                results.add(Map.of("probe", "tcp-connect", "status", "executed"));
            } catch (Exception ex) {
                results.add(Map.of("probe", "tcp-connect", "status", "failed", "error", ex.getMessage()));
            }

            return Map.of("status", "executed", "results", results);
        } catch (Exception ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }
}
