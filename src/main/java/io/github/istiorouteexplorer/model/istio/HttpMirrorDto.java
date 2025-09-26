package io.github.istiorouteexplorer.model.istio;
import java.util.Objects;

/**
 * Mirror to specify the destination to mirror http traffic
 * @param destinationDto the destination to mirror
 * @param percentage the percentage of traffic to mirror
 */

public final class HttpMirrorDto {

    private final DestinationDto destinationDto;
    private final Double percentage;

    public HttpMirrorDto(DestinationDto destinationDto, Double percentage) {
        this.destinationDto = destinationDto;
        this.percentage = percentage;
    }

    public DestinationDto destinationDto() {
        return destinationDto;
    }

    public Double percentage() {
        return percentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HttpMirrorDto that)) {
            return false;
        }
        return Objects.equals(destinationDto, that.destinationDto) &&
            Objects.equals(percentage, that.percentage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationDto, percentage);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("HttpMirrorDto{");
        builder.append("destinationDto=").append(destinationDto);
        builder.append(", percentage=").append(percentage);
        builder.append('}');
        return builder.toString();
    }
}
