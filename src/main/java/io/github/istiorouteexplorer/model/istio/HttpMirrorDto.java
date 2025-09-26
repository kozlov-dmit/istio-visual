package io.github.istiorouteexplorer.model.istio;

/**
 * Mirror to specify the destination to mirror http traffic
 * @param destinationDto the destination to mirror
 * @param percentage the percentage of traffic to mirror
 */
public record HttpMirrorDto(
        DestinationDto destinationDto,
        Double percentage
) {

}
