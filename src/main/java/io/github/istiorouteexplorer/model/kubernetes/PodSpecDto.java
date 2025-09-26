package io.github.istiorouteexplorer.model.kubernetes;

import java.util.List;
import java.util.Objects;

/**
 * Kubernetes pod specification
 * @param containers List of pod containers
 */

public final class PodSpecDto {

    private final List<ContainerDto> containers;

    public PodSpecDto(List<ContainerDto> containers) {
        this.containers = containers;
    }

    public List<ContainerDto> containers() {
        return containers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PodSpecDto that)) {
            return false;
        }
        return Objects.equals(containers, that.containers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containers);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PodSpecDto{");
        builder.append("containers=").append(containers);
        builder.append('}');
        return builder.toString();
    }
}
