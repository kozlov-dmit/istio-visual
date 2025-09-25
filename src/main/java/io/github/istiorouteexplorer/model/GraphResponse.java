package io.github.istiorouteexplorer.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record GraphResponse(
        String namespace,
        Instant generatedAt,
        Map<String, Object> summary,
        List<GraphNode> nodes,
        List<GraphEdge> edges,
        List<String> warnings
) {
}
