package io.github.istiorouteexplorer.model.istio;

import java.util.List;

/**
 * Dto for matching tcp traffic
 * @param destinationSubnets list of subnets
 * @param gateways list of gateways
 * @param port port
 */
public record TcpMatchDto(
        List<String> destinationSubnets,
        List<String> gateways,
        Long port
) {
}
