package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO representing the HTTP route configuration section of a VirtualService.
 */
@NoArgsConstructor(force = true)
public class HttpRouteDto implements IstioRoute {

    private final HttpRouteDestinationDto mirror;
    private final List<HttpRouteDestinationDto> route;

    public HttpRouteDto(HttpRouteDestinationDto mirror, List<HttpRouteDestinationDto> route) {
        this.mirror = mirror;
        this.route = route;
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
        StringBuilder builder = new StringBuilder("HttpRouteDto{");
        builder.append("mirror=").append(mirror);
        builder.append(", route=").append(route);
        builder.append('}');
        return builder.toString();
    }
}
