package io.github.istiorouteexplorer.model.istio;

import java.util.Map;
import java.util.Objects;

/**
 * Selector for workload
 * @param matchLabels labels to match
 */

public final class WorkLoadSelectorDto {

    private final Map<String, String> matchLabels;

    public WorkLoadSelectorDto(Map<String, String> matchLabels) {
        this.matchLabels = matchLabels;
    }

    public Map<String, String> matchLabels() {
        return matchLabels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkLoadSelectorDto that)) {
            return false;
        }
        return Objects.equals(matchLabels, that.matchLabels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchLabels);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("WorkLoadSelectorDto{");
        builder.append("matchLabels=").append(matchLabels);
        builder.append('}');
        return builder.toString();
    }
}
