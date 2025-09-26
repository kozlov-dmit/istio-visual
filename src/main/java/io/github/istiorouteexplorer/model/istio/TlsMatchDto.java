package io.github.istiorouteexplorer.model.istio;

import java.util.List;

/**
 * Dto for matching tls traffic
 * @param destinationSubnets destination subnets
 * @param gateways gateways
 * @param port port
 * @param sniHosts sni hosts
 */
public record TlsMatchDto(
        List<String> destinationSubnets,
        List<String> gateways,
        Long port,
        List<String> sniHosts
) {
}
