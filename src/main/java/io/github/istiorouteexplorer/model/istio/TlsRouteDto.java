package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO describing a TLS routing entry in an Istio VirtualService.
 */
@NoArgsConstructor(force = true)
public class TlsRouteDto implements IstioRoute {

    private List<TlsMatchRequestDto> match;
    private List<TcpRouteDestinationDto> route;

    public TlsRouteDto(List<TlsMatchRequestDto> match, List<TcpRouteDestinationDto> route) {
        this.match = match;
        this.route = route;
    }

    public List<TlsMatchRequestDto> match() {
        return match;
    }

    public List<TcpRouteDestinationDto> route() {
        return route;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TlsRouteDto that)) {
            return false;
        }
        return Objects.equals(match, that.match) &&
            Objects.equals(route, that.route);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match, route);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("TlsRouteDto{");
        builder.append("match=").append(match);
        builder.append(", route=").append(route);
        builder.append('}');
        return builder.toString();
    }
}
