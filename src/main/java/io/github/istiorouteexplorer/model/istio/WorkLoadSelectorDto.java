package io.github.istiorouteexplorer.model.istio;

import java.util.Map;

/**
 * Selector for workload
 * @param matchLabels labels to match
 */
public record WorkLoadSelectorDto(
        Map<String, String> matchLabels
) {
}
