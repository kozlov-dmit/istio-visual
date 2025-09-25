package io.github.istiorouteexplorer.model;

import java.util.Map;

public record GraphNode(String id, String type, Map<String, Object> properties) {
}
