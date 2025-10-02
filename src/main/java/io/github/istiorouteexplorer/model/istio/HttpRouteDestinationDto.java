package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO describing an HTTP route destination and its weighting within a VirtualService.
 */
@Data
@NoArgsConstructor(force = true)
public class HttpRouteDestinationDto implements RouteDestinationDto {

    private DestinationDto destination;
    private Integer weight;

    @Override
    public String getHost() {
        return destination == null ? null : destination.getHost();
    }

    @Override
    public Long getPort() {
        return destination == null ? null : destination.getPort();
    }
}
