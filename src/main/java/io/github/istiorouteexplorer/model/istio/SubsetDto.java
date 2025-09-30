package io.github.istiorouteexplorer.model.istio;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.istio.api.api.networking.v1alpha3.TrafficPolicy;
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
    private TrafficPolicy trafficPolicy;

    @Override
    public boolean equals(Object o) {
        return o instanceof SubsetDto;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SubsetDto{}";
    }
}
