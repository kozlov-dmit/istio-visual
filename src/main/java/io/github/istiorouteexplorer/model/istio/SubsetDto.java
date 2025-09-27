package io.github.istiorouteexplorer.model.istio;
import lombok.NoArgsConstructor;

/**
 * DTO describing a subset definition referenced by Istio DestinationRules.
 */
@NoArgsConstructor(force = true)
public class SubsetDto {

    public SubsetDto() {
    }

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
