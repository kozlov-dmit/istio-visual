package io.github.istiorouteexplorer.model.istio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO holding the specification portion of an Istio DestinationRule.
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class DestinationRuleSpecDto {

    private List<SubsetDto> subsets;
    private List<String> exportTo;
    private String host;
    private TrafficPolicyDto trafficPolicy;
    private WorkLoadSelectorDto workloadSelector;

}
