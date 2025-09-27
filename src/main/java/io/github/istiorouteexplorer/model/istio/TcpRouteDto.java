package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO representing the TCP route block inside an Istio VirtualService.
 */
@NoArgsConstructor(force = true)
public class TcpRouteDto implements IstioRoute {

    private List<TcpMatchDto> match;
    private List<TcpRouteDestinationDto> route;

    public TcpRouteDto(List<TcpMatchDto> match, List<TcpRouteDestinationDto> route) {
        this.match = match;
        this.route = route;
    }

    public List<TcpMatchDto> match() {
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
        if (!(o instanceof TcpRouteDto that)) {
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
        StringBuilder builder = new StringBuilder("TcpRouteDto{");
        builder.append("match=").append(match);
        builder.append(", route=").append(route);
        builder.append('}');
        return builder.toString();
    }
}
