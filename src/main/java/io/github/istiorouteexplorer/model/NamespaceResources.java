package io.github.istiorouteexplorer.model;

import java.util.List;
import java.util.Map;

public record NamespaceResources(
        String namespace,
        List<Map<String, Object>> virtualServices,
        List<Map<String, Object>> destinationRules,
        List<Map<String, Object>> gateways,
        List<Map<String, Object>> serviceEntries,
        List<Map<String, Object>> sidecars,
        List<Map<String, Object>> envoyFilters,
        List<Map<String, Object>> workloadEntries,
        List<Map<String, Object>> authorizationPolicies,
        List<Map<String, Object>> peerAuthentications,
        List<Map<String, Object>> requestAuthentications,
        List<Map<String, Object>> services,
        List<Map<String, Object>> pods
) {
}
