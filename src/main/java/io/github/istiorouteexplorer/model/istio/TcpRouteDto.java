package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the TCP route block inside an Istio VirtualService.
 */
@Data
@NoArgsConstructor(force = true)
public class TcpRouteDto implements IstioRoute {

    private List<TcpMatchRequestDto> match;
    private List<TcpRouteDestinationDto> route;

}
