package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;

/**
 * VirtualService
 * @param metadata metadata
 * @param spec spec
 */
public record VirtualServiceDto(
    ObjectMetadataDto metadata,
    VirtualServiceSpecDto spec
) {
}
