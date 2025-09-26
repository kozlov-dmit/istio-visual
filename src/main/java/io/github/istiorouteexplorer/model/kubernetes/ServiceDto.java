package io.github.istiorouteexplorer.model.kubernetes;

/**
 * Service is a named abstraction of software service (for example, mysql)
 * consisting of local port (for example 3306) that the proxy listens on,
 * and the selector that determines which pods will answer requests sent through the proxy
 *
 * @param metadata metadata
 * @param spec spec
 */
public record ServiceDto(
        ObjectMetadataDto metadata,
        ServiceSpecDto spec
) {
}
