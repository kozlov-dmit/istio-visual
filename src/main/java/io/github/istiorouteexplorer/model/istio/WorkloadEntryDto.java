package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO representing an Istio WorkloadEntry resource.
 */
@NoArgsConstructor(force = true)
public class WorkloadEntryDto {

    private ObjectMetadataDto metadata;
    private WorkloadEntrySpecDto spec;

    public WorkloadEntryDto(ObjectMetadataDto metadata, WorkloadEntrySpecDto spec) {
        this.metadata = metadata;
        this.spec = spec;
    }

    public ObjectMetadataDto metadata() {
        return metadata;
    }

    public WorkloadEntrySpecDto spec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkloadEntryDto that)) {
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
        StringBuilder builder = new StringBuilder("WorkloadEntryDto{");
        builder.append("metadata=").append(metadata);
        builder.append(", spec=").append(spec);
        builder.append('}');
        return builder.toString();
    }
}
