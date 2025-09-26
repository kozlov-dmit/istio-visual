package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;

/**
 * Spec for DestinationRule
 * @param exportTo A list of namespaces to which this destination rule is exported
 * @param host The host name for the destination rule
 * @param trafficPolicy The traffic policy for the destination rule
 * @param workloadSelector The workload selector for the destination rule
 */

public final class DestinationRuleSpecDto {

    private final List<String> exportTo;
    private final String host;
    private final TrafficPolicyDto trafficPolicy;
    private final WorkLoadSelectorDto workloadSelector;

    public DestinationRuleSpecDto(List<String> exportTo, String host, TrafficPolicyDto trafficPolicy, WorkLoadSelectorDto workloadSelector) {
        this.exportTo = exportTo;
        this.host = host;
        this.trafficPolicy = trafficPolicy;
        this.workloadSelector = workloadSelector;
    }

    public List<String> exportTo() {
        return exportTo;
    }

    public String host() {
        return host;
    }

    public TrafficPolicyDto trafficPolicy() {
        return trafficPolicy;
    }

    public WorkLoadSelectorDto workloadSelector() {
        return workloadSelector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DestinationRuleSpecDto that)) {
            return false;
        }
        return Objects.equals(exportTo, that.exportTo) &&
            Objects.equals(host, that.host) &&
            Objects.equals(trafficPolicy, that.trafficPolicy) &&
            Objects.equals(workloadSelector, that.workloadSelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exportTo, host, trafficPolicy, workloadSelector);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("DestinationRuleSpecDto{");
        builder.append("exportTo=").append(exportTo);
        builder.append(", host=").append(host);
        builder.append(", trafficPolicy=").append(trafficPolicy);
        builder.append(", workloadSelector=").append(workloadSelector);
        builder.append('}');
        return builder.toString();
    }
}
