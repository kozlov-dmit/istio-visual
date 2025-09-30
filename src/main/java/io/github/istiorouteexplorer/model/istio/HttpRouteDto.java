package io.github.istiorouteexplorer.model.istio;

import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * DTO representing the HTTP route configuration section of a VirtualService.
 */
@NoArgsConstructor(force = true)
public class HttpRouteDto implements IstioRoute {

    private List<HttpMatchRequestDto> match;
    private HttpRouteDestinationDto mirror;
    private List<HttpRouteDestinationDto> route;

    public HttpRouteDto(List<HttpMatchRequestDto> match, HttpRouteDestinationDto mirror, List<HttpRouteDestinationDto> route) {
        this.match = match;
        this.mirror = mirror;
        this.route = route;
    }

    public List<HttpMatchRequestDto> match() {
        if (match == null) {
            return List.of();
        } else {
            return match;
        }
    }

    public HttpRouteDestinationDto mirror() {
        return mirror;
    }

    public List<HttpRouteDestinationDto> route() {
        return route;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HttpRouteDto that)) {
            return false;
        }
        return Objects.equals(mirror, that.mirror) &&
            Objects.equals(route, that.route);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mirror, route);
    }

    @Override
    public String toString() {
        return "HttpRouteDto{" + "match=" + match +
                "mirror=" + mirror +
                ", route=" + route +
                '}';
    }
}
