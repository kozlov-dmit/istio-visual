package io.github.istiorouteexplorer.model;

import io.github.istiorouteexplorer.model.istio.DestinationRuleDto;
import io.github.istiorouteexplorer.model.istio.GatewayDto;
import io.github.istiorouteexplorer.model.istio.ServiceEntryDto;
import io.github.istiorouteexplorer.model.istio.VirtualServiceDto;
import io.github.istiorouteexplorer.model.istio.WorkloadEntryDto;
import io.github.istiorouteexplorer.model.kubernetes.PodDto;
import io.github.istiorouteexplorer.model.kubernetes.ServiceDto;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * Aggregates all Kubernetes and Istio resources discovered within a namespace so the graph builder
 * can construct traffic topology and metadata.
 */
@NoArgsConstructor(force = true)
public class NamespaceResources {

    private final String namespace;
    private final List<VirtualServiceDto> virtualServices;
    private final List<DestinationRuleDto> destinationRules;
    private final List<GatewayDto> gateways;
    private final List<ServiceEntryDto> serviceEntries;
    private final List<WorkloadEntryDto> workloadEntries;
    private final List<ServiceDto> services;
    private final List<PodDto> pods;

    public NamespaceResources(
            String namespace,
            List<VirtualServiceDto> virtualServices,
            List<DestinationRuleDto> destinationRules,
            List<GatewayDto> gateways,
            List<ServiceEntryDto> serviceEntries,
            List<WorkloadEntryDto> workloadEntries,
            List<ServiceDto> services,
            List<PodDto> pods
    ) {
        this.namespace = namespace;
        this.virtualServices = List.copyOf(virtualServices);
        this.destinationRules = List.copyOf(destinationRules);
        this.gateways = List.copyOf(gateways);
        this.serviceEntries = List.copyOf(serviceEntries);
        this.workloadEntries = List.copyOf(workloadEntries);
        this.services = List.copyOf(services);
        this.pods = List.copyOf(pods);
    }

    public String namespace() {
        return namespace;
    }

    public List<VirtualServiceDto> virtualServices() {
        return virtualServices;
    }

    public List<DestinationRuleDto> destinationRules() {
        return destinationRules;
    }

    public List<GatewayDto> gateways() {
        return gateways;
    }

    public List<ServiceEntryDto> serviceEntries() {
        return serviceEntries;
    }

    public List<WorkloadEntryDto> workloadEntries() {
        return workloadEntries;
    }

    public List<ServiceDto> services() {
        return services;
    }

    public List<PodDto> pods() {
        return pods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NamespaceResources that)) {
            return false;
        }
        return Objects.equals(namespace, that.namespace)
                && Objects.equals(virtualServices, that.virtualServices)
                && Objects.equals(destinationRules, that.destinationRules)
                && Objects.equals(gateways, that.gateways)
                && Objects.equals(serviceEntries, that.serviceEntries)
                && Objects.equals(workloadEntries, that.workloadEntries)
                && Objects.equals(services, that.services)
                && Objects.equals(pods, that.pods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, virtualServices, destinationRules, gateways,
                serviceEntries, workloadEntries, services, pods);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("NamespaceResources{");
        builder.append("namespace='").append(namespace).append('\'');
        builder.append(", virtualServices=").append(virtualServices);
        builder.append(", destinationRules=").append(destinationRules);
        builder.append(", gateways=").append(gateways);
        builder.append(", serviceEntries=").append(serviceEntries);
        builder.append(", workloadEntries=").append(workloadEntries);
        builder.append(", services=").append(services);
        builder.append(", pods=").append(pods);
        builder.append('}');
        return builder.toString();
    }
}
