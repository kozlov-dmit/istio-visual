package io.github.istiorouteexplorer.model;

import io.github.istiorouteexplorer.model.istio.*;
import io.github.istiorouteexplorer.model.kubernetes.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregates all Kubernetes and Istio resources discovered within a namespace so the graph builder
 * can construct traffic topology and metadata.
 */
@Data
@NoArgsConstructor(force = true)
public class NamespaceResources {

    private String namespace;
    private List<VirtualServiceDto> virtualServices;
    private List<DestinationRuleDto> destinationRules;
    private List<GatewayDto> gateways;
    private List<ServiceEntryDto> serviceEntries;
    private List<EnvoyFilterDto> envoyFilters;
    private List<WorkloadEntryDto> workloadEntries;
    private List<ServiceDto> services;
    private List<EndpointDto> endpoints;
    private List<DeploymentDto> deployments;
    private List<ReplicaSetDto> replicaSets;
    private List<PodDto> pods;

    public NamespaceResources(
            String namespace,
            List<VirtualServiceDto> virtualServices,
            List<DestinationRuleDto> destinationRules,
            List<GatewayDto> gateways,
            List<ServiceEntryDto> serviceEntries,
            List<EnvoyFilterDto> envoyFilters,
            List<WorkloadEntryDto> workloadEntries,
            List<ServiceDto> services,
            List<EndpointDto> endpoints,
            List<DeploymentDto> deployments,
            List<ReplicaSetDto> replicaSets,
            List<PodDto> pods
    ) {
        this.namespace = namespace;
        this.virtualServices = List.copyOf(virtualServices);
        this.destinationRules = List.copyOf(destinationRules);
        this.gateways = List.copyOf(gateways);
        this.serviceEntries = List.copyOf(serviceEntries);
        this.envoyFilters = List.copyOf(envoyFilters);
        this.workloadEntries = List.copyOf(workloadEntries);
        this.services = List.copyOf(services);
        this.endpoints = List.copyOf(endpoints);
        this.deployments = List.copyOf(deployments);
        this.replicaSets = List.copyOf(replicaSets);
        this.pods = List.copyOf(pods);
    }

}
