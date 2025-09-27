package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO representing an Istio Gateway resource including metadata and specification.
 */
@NoArgsConstructor(force = true)
public class GatewayDto {

    private ObjectMetadataDto metadata;
    private GatewaySpecDto spec;

    public GatewayDto(ObjectMetadataDto metadata, GatewaySpecDto spec) {
        this.metadata = metadata;
        this.spec = spec;
    }

    public ObjectMetadataDto metadata() {
        return metadata;
    }

    public GatewaySpecDto spec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GatewayDto that)) {
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
        StringBuilder builder = new StringBuilder("GatewayDto{");
        builder.append("metadata=").append(metadata);
        builder.append(", spec=").append(spec);
        builder.append('}');
        return builder.toString();
    }
}
