package io.github.istiorouteexplorer.model;

import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class TopologyGraph {
    private final Map<String, RouteNode> nodes = new HashMap<>();
    private final List<RouteEdge> edges = new ArrayList<>();
    // nodeId -> subsetName -> endpoints
    private final Map<String, Map<String, List<String>>> subsetMapping = new HashMap<>();

    public void addNode(RouteNode node) {
        nodes.putIfAbsent(node.getId(), node);
    }

    public void ensureNode(String id) {
        nodes.putIfAbsent(id, new RouteNode(id, RouteNode.Type.EXTERNAL, Map.of()));
    }

    public  void addEdge(RouteEdge edge) {
        edges.add(edge);
    }

    public void addSubsetMapping(String nodeId, String subsetName, List<String> endpoints) {
        subsetMapping.computeIfAbsent(nodeId, k -> new HashMap<>()).put(subsetName, endpoints);
    }
}
