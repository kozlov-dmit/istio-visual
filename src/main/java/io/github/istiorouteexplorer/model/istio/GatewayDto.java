package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an Istio Gateway resource including metadata and specification.
 */
@Data
@NoArgsConstructor(force = true)
public class GatewayDto {

    private ObjectMetadataDto metadata;
    private GatewaySpecDto spec;

}
