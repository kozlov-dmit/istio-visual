package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import java.util.Objects;

/**
 * ServiceEntry enables adding additional entries into Istio's internal service registry.
 *
 * @param metadata metadata
 * @param spec spec of ServiceEntry
 */

public final class ServiceEntryDto {

    private final ObjectMetadataDto metadata;
    private final ServiceEntrySpecDto spec;

    public ServiceEntryDto(ObjectMetadataDto metadata, ServiceEntrySpecDto spec) {
        this.metadata = metadata;
        this.spec = spec;
    }

    public ObjectMetadataDto metadata() {
        return metadata;
    }

    public ServiceEntrySpecDto spec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceEntryDto that)) {
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
        StringBuilder builder = new StringBuilder("ServiceEntryDto{");
        builder.append("metadata=").append(metadata);
        builder.append(", spec=").append(spec);
        builder.append('}');
        return builder.toString();
    }
}
