package io.github.istiorouteexplorer.model.istio;

/**
 * Route destination for TCP traffic.
 * @param destination the destination
 * @param weight the weight
 */
public record TcpRouteDestinationDto(
        DestinationDto destination,
        Integer weight
) implements RouteDestinationDto {
}
