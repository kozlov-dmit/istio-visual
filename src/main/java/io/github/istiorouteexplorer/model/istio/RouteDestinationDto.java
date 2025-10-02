package io.github.istiorouteexplorer.model.istio;

public interface RouteDestinationDto {
    String getHost();
    Long getPort();
    Integer getWeight();
}
