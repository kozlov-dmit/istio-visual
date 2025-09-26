package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;

/**
 * WorkloadEntry enables specifying the properties of a single non-Kubernetes workload such a VM or a bare metal services that can be referred to by service entries.
 *
 * @param metadata metadata
 * @param spec spec
 */
public record WorkloadEntryDto(
        ObjectMetadataDto metadata,
        WorkloadEntrySpecDto spec
) {
}
