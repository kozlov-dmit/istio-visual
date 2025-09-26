package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;

public record GatewayDto(
        ObjectMetadataDto metadata,
        GatewaySpecDto spec
) {
}
