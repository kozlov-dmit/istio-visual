package io.github.istiorouteexplorer.model;

import io.github.istiorouteexplorer.model.istio.TrafficPolicyDto;
import io.github.istiorouteexplorer.model.istio.WorkLoadSelectorDto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Route for Istio traffic
 */
@Slf4j
@Data
@NoArgsConstructor
public class Route {

    private String destinationHost;
    private List<Long> destinationPorts;
    // nodes in istio namespace
    private Map<String, RouteNode> nodes = new HashMap<>();
    // links between nodes
    private List<RouteLink> links = new LinkedList<>();

    public Route(String destinationHost, List<Long> destinationPorts) {
        this.destinationHost = destinationHost;
        this.destinationPorts = destinationPorts;
    }

    public void addNode(RouteNode node) {
        if (!nodes.isEmpty()) {
            log.warn("Route already contains nodes, it might be able only to add links");
        }
        nodes.put(node.getId(), node);
    }

    public void addLink(String sourceId, RouteNode destination, String protocol, Long port, List<MatchCondition> matchConditions) {
        RouteNode source = nodes.get(sourceId);
        if (source == null) {
            log.error("Source node {} not found, route for destination {} was not created", sourceId, destination.getId());
        }
        nodes.put(destination.getId(), destination);
        links.add(new RouteLink(sourceId, destination.getId(), protocol, port, matchConditions));
    }
}
