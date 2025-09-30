package io.github.istiorouteexplorer.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RouteEdge {
    private String fromId;
    private String toId;
    private String protocol; // HTTP, YCP, TLS
    private Long port;
    private List<MatchCondition> matches;
    private List<String> notes = new ArrayList<>();
    private Map<String, Integer> weights = new HashMap<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    public RouteEdge(
            String fromId,
            String toId,
            String protocol,
            Long port,
            List<MatchCondition> matches
    ) {
        this.fromId = fromId;
        this.toId = toId;
        this.protocol = protocol;
        this.port = port;
        this.matches = matches;
    }
}
