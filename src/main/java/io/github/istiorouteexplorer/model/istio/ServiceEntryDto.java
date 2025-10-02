package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an Istio ServiceEntry resource together with its specification.
 */
@Data
@NoArgsConstructor(force = true)
public class ServiceEntryDto {

    private ObjectMetadataDto metadata;
    private ServiceEntrySpecDto spec;

}
