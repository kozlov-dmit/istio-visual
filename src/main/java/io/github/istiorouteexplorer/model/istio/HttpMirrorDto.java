package io.github.istiorouteexplorer.model.istio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO encapsulating HTTP mirroring settings declared on a VirtualService route.
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class HttpMirrorDto {

    private DestinationDto destinationDto;
    private Double percentage;

}
