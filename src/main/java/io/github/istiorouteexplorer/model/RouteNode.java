package io.github.istiorouteexplorer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class RouteNode {
    public enum Type {
        K8S_SERVICE,
        WORKLOAD,
        GATEWAY,
        EXTERNAL,
        SERVICEENTRY,
        EGRESS,
        VIRTUALSERVICE,
        WILDCARD
    }
    private String id;
    private Type type;
    private Map<String, Object> metadata;

    public RouteNode(String id, Type type) {
        this.id = id;
        this.type = type;
        this.metadata = new HashMap<>();
    }
}
