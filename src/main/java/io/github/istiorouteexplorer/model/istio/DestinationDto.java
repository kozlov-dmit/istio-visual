package io.github.istiorouteexplorer.model.istio;

import io.fabric8.istio.api.api.networking.v1alpha3.PortSelector;

/**
 * Istio Destination
 * @param host host
 * @param port port
 * @param subset subset
 */
public record DestinationDto(
        String host,
        Long port,
        String subset
) {

}
