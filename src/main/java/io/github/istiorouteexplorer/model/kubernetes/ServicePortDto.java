package io.github.istiorouteexplorer.model.kubernetes;

import io.fabric8.kubernetes.api.model.IntOrString;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO detailing port configuration for a Kubernetes Service.
 */
@Data
@NoArgsConstructor(force = true)
public class ServicePortDto {

    private String appProtocol;
    private String name;
    private Integer nodePort;
    private Integer port;
    private String protocol;
    private IntOrString targetPort;

}
