package io.github.istiorouteexplorer.model.istio;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO describing an HTTP route destination and its weighting within a VirtualService.
 */
@NoArgsConstructor(force = true)
public class HttpRouteDestinationDto implements RouteDestinationDto {

    private DestinationDto destination;
    private Integer weight;

    public HttpRouteDestinationDto(DestinationDto destination, Integer weight) {
        this.destination = destination;
        this.weight = weight;
    }

    public DestinationDto destination() {
        return destination;
    }

    public Integer weight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HttpRouteDestinationDto that)) {
            return false;
        }
        return Objects.equals(destination, that.destination) &&
            Objects.equals(weight, that.weight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination, weight);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("HttpRouteDestinationDto{");
        builder.append("destination=").append(destination);
        builder.append(", weight=").append(weight);
        builder.append('}');
        return builder.toString();
    }
}
