package io.github.istiorouteexplorer.web;

import io.github.istiorouteexplorer.graph.TopologyBuilder;
import io.github.istiorouteexplorer.model.TopologyGraph;
import io.github.istiorouteexplorer.service.RouteExplorerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TopologyController {

    private final RouteExplorerService routeExplorerService;

    @GetMapping("/topology")
    public Map<String, Object> topology(@RequestParam(defaultValue = "fort-isito") String namespace) {
        TopologyGraph topologyGraph = routeExplorerService.buildTopologyGraph(namespace);
        List<Map<String, Object>> nodes = topologyGraph.getNodes().values().stream()
                .map(n -> Map.of("id", n.getId(), "type", n.getType().name(), "meta", n.getMetadata()))
                .toList();

        List<Map<String, Object>> edges = topologyGraph.getEdges().stream()
                .map(e -> Map.<String, Object>of(
                        "fromId", e.getFromId(),
                        "toId", e.getToId(),
                        "protocol", e.getProtocol(),
                        "port", e.getPort(),
                        "matches", e.getMatches(),
                        "notes", e.getNotes(),
                        "weights", e.getWeights()
                ))
                .toList();

        return Map.of("nodes", nodes, "edges", edges);
    }

}
