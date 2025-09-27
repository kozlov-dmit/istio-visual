package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO holding the specification portion of an Istio DestinationRule.
 */
@NoArgsConstructor(force = true)
public class DestinationRuleSpecDto {

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
