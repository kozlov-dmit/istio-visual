package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;

/**
 * Rule for destinations
 * @param metadata metadata
 * @param spec rule spec
 */
public record DestinationRuleDto(
        ObjectMetadataDto metadata,
        DestinationRuleSpecDto spec
) {
}
