package io.github.istiorouteexplorer.model;

import java.util.Map;

public record ResourceCollection(NamespaceResources primary, Map<String, NamespaceResources> extras) {
}
