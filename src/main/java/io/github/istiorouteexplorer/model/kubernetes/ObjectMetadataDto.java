package io.github.istiorouteexplorer.model.kubernetes;

import java.util.Map;

/**
 * Metadata of a kubernetes object
 *
 * @param name      the name of the object
 * @param namespace the namespace of the object
 * @param labels    the labels of the object
 */
public record ObjectMetadataDto(
        String name,
        String namespace,
        Map<String, String> labels,
        Map<String, String> annotations
) {
}
