package io.github.istiorouteexplorer.model;

import io.github.istiorouteexplorer.model.istio.TrafficPolicyDto;
import io.github.istiorouteexplorer.model.istio.WorkLoadSelectorDto;
import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodes in istio namespace
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteNode {
    public enum Type {
        SERVICE_ENTRY,
        POD,
        DEPLOYMENT,
        MESH,
        UNKNOWN
    }
    // use host as id
    private String id;
    private String name;
    private Type type;
    private ObjectMetadataDto metadata;
    private TrafficPolicyDto trafficPolicy;
    private WorkLoadSelectorDto workloadSelector;
    private List<String> comments = new ArrayList<>();

    public RouteNode(String id, String name, Type type, ObjectMetadataDto metadata) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.metadata = metadata;
    }

    public void addTrafficPolicy(TrafficPolicyDto trafficPolicy) {
        if (this.trafficPolicy == null) {
            this.trafficPolicy = trafficPolicy;
        } else {
            comments.addAll(trafficPolicy.merge(trafficPolicy));
        }
    }

}
