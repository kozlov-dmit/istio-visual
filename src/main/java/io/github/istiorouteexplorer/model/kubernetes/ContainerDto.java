package io.github.istiorouteexplorer.model.kubernetes;

/**
 * Container
 * @param name container name
 * @param image image
 */
public record ContainerDto (
        String name,
        String image
) {
}
