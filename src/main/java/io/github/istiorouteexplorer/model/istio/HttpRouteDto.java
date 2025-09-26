package io.github.istiorouteexplorer.model.istio;

import java.util.List;

/**
 * Istio route for http traffic
 * @param mirror mirror
 * @param route list of routes
 */
public record HttpRouteDto(
        HttpRouteDestinationDto mirror,
        List<HttpRouteDestinationDto> route
) implements IstioRoute {

}
