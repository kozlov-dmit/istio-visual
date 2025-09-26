package io.github.istiorouteexplorer.model.istio;

import java.util.List;

/**
 * ServiceEntry enables adding additional entries into Istio's internal service registry.
 *
 * @param addresses list of IP addresses
 * @param endpoints list of endpoints
 * @param exportTo list of namespaces
 * @param hosts list of hosts
 */
public record ServiceEntrySpecDto(
        List<String> addresses,
        List<WorkloadEntrySpecDto> endpoints,
        List<String> exportTo,
        List<String> hosts,
        List<PortDto> ports,
        ServiceEntryResolution resolution,
        List<String> subjectAltNames,
        WorkLoadSelectorDto workloadSelector
) {
}
