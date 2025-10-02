package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO describing port configuration shared across Istio networking resources.
 */
@Data
@NoArgsConstructor(force = true)
public class PortDto {

    private String name;
    private Long number;
    private String protocol;
    private Long targetPort;

}
