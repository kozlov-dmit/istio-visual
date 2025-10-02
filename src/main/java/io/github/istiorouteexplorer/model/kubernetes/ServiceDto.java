package io.github.istiorouteexplorer.model.kubernetes;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Kubernetes Service including metadata and specification.
 */
@Data
@NoArgsConstructor(force = true)
public class ServiceDto {

    private ObjectMetadataDto metadata;
    private ServiceSpecDto spec;

}
