package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an Istio WorkloadEntry resource.
 */
@Data
@NoArgsConstructor(force = true)
public class WorkloadEntryDto {

    private ObjectMetadataDto metadata;
    private WorkloadEntrySpecDto spec;

}
