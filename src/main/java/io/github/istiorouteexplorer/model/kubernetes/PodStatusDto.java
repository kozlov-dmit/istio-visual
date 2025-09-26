package io.github.istiorouteexplorer.model.kubernetes;

/**
 * Pod status
 * @param phase Phase of the pod
 */
public record PodStatusDto(
        String phase
) {
}
