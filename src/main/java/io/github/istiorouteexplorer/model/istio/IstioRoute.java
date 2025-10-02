package io.github.istiorouteexplorer.model.istio;

import java.util.List;

public interface IstioRoute {
    List<? extends RouteDestinationDto> getRoute();
    List<? extends IstioMatchRequestDto> getMatch();
}
