package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO describing the workload selector labels applied to Istio configuration.
 */
@Data
@NoArgsConstructor(force = true)
public class WorkLoadSelectorDto {

    private Map<String, String> matchLabels;

}
