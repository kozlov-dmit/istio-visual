package io.github.istiorouteexplorer.model.kubernetes;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Kubernetes Pod with its specification and status fragments.
 */
@NoArgsConstructor(force = true)
public class PodDto {

    private final ObjectMetadataDto metadata;
    private final PodSpecDto spec;
    private final PodStatusDto status;

    public PodDto(ObjectMetadataDto metadata, PodSpecDto spec, PodStatusDto status) {
        this.metadata = metadata;
        this.spec = spec;
        this.status = status;
    }

    public ObjectMetadataDto metadata() {
        return metadata;
    }

    public PodSpecDto spec() {
        return spec;
    }

    public PodStatusDto status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PodDto that)) {
            return false;
        }
        return Objects.equals(metadata, that.metadata) &&
            Objects.equals(spec, that.spec) &&
            Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, spec, status);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PodDto{");
        builder.append("metadata=").append(metadata);
        builder.append(", spec=").append(spec);
        builder.append(", status=").append(status);
        builder.append('}');
        return builder.toString();
    }
}
