package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO describing the destination target used by an Istio TCP route.
 */
@Data
@NoArgsConstructor(force = true)
public class TcpRouteDestinationDto implements RouteDestinationDto {

    private DestinationDto destination;
    private Integer weight;

    @Override
    public String getHost() {
        return destination != null ? destination.getHost() : null;
    }

    @Override
    public Long getPort() {
        return destination != null ? destination.getPort() : null;
    }
}
