package io.github.istiorouteexplorer.model.istio;

/**
 * Route destination for HTTP traffic
 * @param destination destination
// * @param headers headers
 * @param weight weight
 */
public record HttpRouteDestinationDto(
        DestinationDto destination,
//        Headers headers,
        Integer weight
) implements RouteDestinationDto {
}
