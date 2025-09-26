package io.github.istiorouteexplorer.model.istio;

import java.util.List;

/**
 * Spec for DestinationRule
 * @param exportTo A list of namespaces to which this destination rule is exported
 * @param host The host name for the destination rule
 * @param trafficPolicy The traffic policy for the destination rule
 * @param workloadSelector The workload selector for the destination rule
 */
public record DestinationRuleSpecDto(
        List<String> exportTo,
        String host,
        TrafficPolicyDto trafficPolicy,
        WorkLoadSelectorDto workloadSelector
) {
}
