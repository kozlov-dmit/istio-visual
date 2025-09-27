package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO holding the specification details of an Istio ServiceEntry.
 */
@NoArgsConstructor(force = true)
public class ServiceEntrySpecDto {

    private final List<String> addresses;
    private final List<WorkloadEntrySpecDto> endpoints;
    private final List<String> exportTo;
    private final List<String> hosts;
    private final List<PortDto> ports;
    private final ServiceEntryResolution resolution;
    private final List<String> subjectAltNames;
    private final WorkLoadSelectorDto workloadSelector;

    public ServiceEntrySpecDto(List<String> addresses, List<WorkloadEntrySpecDto> endpoints, List<String> exportTo, List<String> hosts, List<PortDto> ports, ServiceEntryResolution resolution, List<String> subjectAltNames, WorkLoadSelectorDto workloadSelector) {
        this.addresses = addresses;
        this.endpoints = endpoints;
        this.exportTo = exportTo;
        this.hosts = hosts;
        this.ports = ports;
        this.resolution = resolution;
        this.subjectAltNames = subjectAltNames;
        this.workloadSelector = workloadSelector;
    }

    public List<String> addresses() {
        return addresses;
    }

    public List<WorkloadEntrySpecDto> endpoints() {
        return endpoints;
    }

    public List<String> exportTo() {
        return exportTo;
    }

    public List<String> hosts() {
        return hosts;
    }

    public List<PortDto> ports() {
        return ports;
    }

    public ServiceEntryResolution resolution() {
        return resolution;
    }

    public List<String> subjectAltNames() {
        return subjectAltNames;
    }

    public WorkLoadSelectorDto workloadSelector() {
        return workloadSelector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceEntrySpecDto that)) {
            return false;
        }
        return Objects.equals(addresses, that.addresses) &&
            Objects.equals(endpoints, that.endpoints) &&
            Objects.equals(exportTo, that.exportTo) &&
            Objects.equals(hosts, that.hosts) &&
            Objects.equals(ports, that.ports) &&
            Objects.equals(resolution, that.resolution) &&
            Objects.equals(subjectAltNames, that.subjectAltNames) &&
            Objects.equals(workloadSelector, that.workloadSelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addresses, endpoints, exportTo, hosts, ports, resolution, subjectAltNames, workloadSelector);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ServiceEntrySpecDto{");
        builder.append("addresses=").append(addresses);
        builder.append(", endpoints=").append(endpoints);
        builder.append(", exportTo=").append(exportTo);
        builder.append(", hosts=").append(hosts);
        builder.append(", ports=").append(ports);
        builder.append(", resolution=").append(resolution);
        builder.append(", subjectAltNames=").append(subjectAltNames);
        builder.append(", workloadSelector=").append(workloadSelector);
        builder.append('}');
        return builder.toString();
    }
}
