package io.github.istiorouteexplorer.model.kubernetes;
import java.util.Objects;

/**
 * Service is a named abstraction of software service (for example, mysql)
 * consisting of local port (for example 3306) that the proxy listens on,
 * and the selector that determines which pods will answer requests sent through the proxy
 *
 * @param metadata metadata
 * @param spec spec
 */

public final class ServiceDto {

    private final ObjectMetadataDto metadata;
    private final ServiceSpecDto spec;

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
