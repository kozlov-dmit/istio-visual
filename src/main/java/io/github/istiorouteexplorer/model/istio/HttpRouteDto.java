package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the HTTP route configuration section of a VirtualService.
 */
@Data
@NoArgsConstructor(force = true)
public class HttpRouteDto implements IstioRoute {

    private List<HttpMatchRequestDto> match;
    private HttpRouteDestinationDto mirror;
    private List<HttpRouteDestinationDto> route;

}
