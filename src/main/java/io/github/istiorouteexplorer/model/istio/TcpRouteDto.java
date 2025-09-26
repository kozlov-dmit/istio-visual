package io.github.istiorouteexplorer.model.istio;

import java.util.List;

/**
 * Istio route for tcp traffic
 * @param match match criteria
 * @param route destination
 */
public record TcpRouteDto(
        List<TcpMatchDto> match,
        List<TcpRouteDestinationDto> route
) implements IstioRoute {
}
