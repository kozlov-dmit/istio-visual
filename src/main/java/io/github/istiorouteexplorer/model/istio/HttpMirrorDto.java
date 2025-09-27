package io.github.istiorouteexplorer.model.istio;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO encapsulating HTTP mirroring settings declared on a VirtualService route.
 */
@NoArgsConstructor(force = true)
public class HttpMirrorDto {

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
