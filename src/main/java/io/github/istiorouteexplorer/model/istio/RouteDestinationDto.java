package io.github.istiorouteexplorer.model.istio;

public interface RouteDestinationDto {
    default DestinationDto destination() {
        return null;
    }
}
