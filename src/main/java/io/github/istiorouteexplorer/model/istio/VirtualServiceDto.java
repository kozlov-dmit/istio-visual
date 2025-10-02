package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an Istio VirtualService resource with metadata and specification.
 */
@Data
@NoArgsConstructor(force = true)
public class VirtualServiceDto {

    private ObjectMetadataDto metadata;
    private VirtualServiceSpecDto spec;

}
