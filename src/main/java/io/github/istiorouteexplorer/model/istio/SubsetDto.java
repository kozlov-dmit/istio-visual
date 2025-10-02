package io.github.istiorouteexplorer.model.istio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO describing a subset definition referenced by Istio DestinationRules.
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class SubsetDto {

    private Map<String, String> labels = new LinkedHashMap<>();
    private String name;
    private TrafficPolicyDto trafficPolicy;

}
