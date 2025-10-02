package io.github.istiorouteexplorer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Links between istio nodes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteLink {

    private String fromId;
    private String toId;
    private String protocol; // HTTP, YCP, TLS
    private Long port;
    private List<MatchCondition> matches;

}
