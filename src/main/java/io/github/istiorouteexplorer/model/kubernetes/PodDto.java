package io.github.istiorouteexplorer.model.kubernetes;

/**
 * Kubernetes pod
 * @param metadata - pod metadata
 * @param spec - pod specification
 * @param status - pod status
 */
public record PodDto (
        ObjectMetadataDto metadata,
        PodSpecDto spec,
        PodStatusDto status
) {
}
