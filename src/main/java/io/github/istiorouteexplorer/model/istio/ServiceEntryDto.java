package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;

/**
 * ServiceEntry enables adding additional entries into Istio's internal service registry.
 *
 * @param metadata metadata
 * @param spec spec of ServiceEntry
 */
public record ServiceEntryDto(
        ObjectMetadataDto metadata,
        ServiceEntrySpecDto spec
) {
}
