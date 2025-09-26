package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;

/**
 * Istio Route for tls traffic
 */

public final class TlsRouteDto implements IstioRoute {

    private final List<TlsMatchDto> match;
    private final List<TcpRouteDestinationDto> route;

    public TlsRouteDto(List<TlsMatchDto> match, List<TcpRouteDestinationDto> route) {
        this.match = match;
        this.route = route;
    }

    public List<TlsMatchDto> match() {
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
