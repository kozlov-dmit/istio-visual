package io.github.istiorouteexplorer.model.istio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO capturing the target host, port, and subset information for an Istio route destination.
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class DestinationDto {

    private String host;
    private Long port;
    private String subset;

}
