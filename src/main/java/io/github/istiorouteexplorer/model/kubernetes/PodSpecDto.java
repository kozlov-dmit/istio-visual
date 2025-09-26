package io.github.istiorouteexplorer.model.kubernetes;

import java.util.List;

/**
 * Kubernetes pod specification
 * @param containers List of pod containers
 */
public record PodSpecDto (
        List<ContainerDto> containers
) {
}
