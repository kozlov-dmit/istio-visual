package io.github.istiorouteexplorer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

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
        WILDCARD
    }
    private String id;
    private Type type;
    private Map<String, Object> metadata;
}
