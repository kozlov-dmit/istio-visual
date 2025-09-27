package io.github.istiorouteexplorer.model;

import java.util.Map;

public record GraphEdge(String id, String kind, String source, String target, Map<String, Object> properties) {
}
