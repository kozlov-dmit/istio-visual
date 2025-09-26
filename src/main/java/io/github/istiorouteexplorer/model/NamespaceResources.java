package io.github.istiorouteexplorer.model;

import io.github.istiorouteexplorer.model.istio.*;
import io.github.istiorouteexplorer.model.kubernetes.PodDto;
import io.github.istiorouteexplorer.model.kubernetes.ServiceDto;

import java.util.List;

public record NamespaceResources(
        String namespace,
        List<VirtualServiceDto> virtualServices,
        List<DestinationRuleDto> destinationRules,
        List<GatewayDto> gateways,
        List<ServiceEntryDto> serviceEntries,
//        List<EnvoyFilter> envoyFilters,
        List<WorkloadEntryDto> workloadEntries,
        List<ServiceDto> services,
        List<PodDto> pods
) {
}
