package io.github.istiorouteexplorer.model.kubernetes;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Kubernetes Pod with its specification and status fragments.
 */
@Data
@NoArgsConstructor(force = true)
public class PodDto {

    private ObjectMetadataDto metadata;
    private PodSpecDto spec;
    private PodStatusDto status;

}
