package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO describing a TLS routing entry in an Istio VirtualService.
 */
@Data
@NoArgsConstructor(force = true)
public class TlsRouteDto implements IstioRoute {

    private List<TlsMatchRequestDto> match;
    private List<TcpRouteDestinationDto> route;

}
