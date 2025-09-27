package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO representing an Istio DestinationRule resource with associated metadata and spec.
 */
@NoArgsConstructor(force = true)
public class DestinationRuleDto {

    private final ObjectMetadataDto metadata;
    private final DestinationRuleSpecDto spec;

    public DestinationRuleDto(ObjectMetadataDto metadata, DestinationRuleSpecDto spec) {
        this.metadata = metadata;
        this.spec = spec;
    }

    public ObjectMetadataDto metadata() {
        return metadata;
    }

    public DestinationRuleSpecDto spec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DestinationRuleDto that)) {
            return false;
        }
        return Objects.equals(metadata, that.metadata) &&
            Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, spec);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("DestinationRuleDto{");
        builder.append("metadata=").append(metadata);
        builder.append(", spec=").append(spec);
        builder.append('}');
        return builder.toString();
    }
}
