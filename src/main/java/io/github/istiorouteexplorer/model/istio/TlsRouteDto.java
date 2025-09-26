package io.github.istiorouteexplorer.model.istio;

import java.util.List;

/**
 * Istio Route for tls traffic
 */
public record TlsRouteDto(
        List<TlsMatchDto> match,
        List<TcpRouteDestinationDto> route
) implements IstioRoute {
}
