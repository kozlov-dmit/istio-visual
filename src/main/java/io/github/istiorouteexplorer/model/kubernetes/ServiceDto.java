package io.github.istiorouteexplorer.model.kubernetes;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Kubernetes Service including metadata and specification.
 */
@NoArgsConstructor(force = true)
public class ServiceDto {

    private ObjectMetadataDto metadata;
    private ServiceSpecDto spec;

    public ServiceDto(ObjectMetadataDto metadata, ServiceSpecDto spec) {
        this.metadata = metadata;
        this.spec = spec;
    }

    public ObjectMetadataDto metadata() {
        return metadata;
    }

    public ServiceSpecDto spec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceDto that)) {
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
        StringBuilder builder = new StringBuilder("ServiceDto{");
        builder.append("metadata=").append(metadata);
        builder.append(", spec=").append(spec);
        builder.append('}');
        return builder.toString();
    }
}
