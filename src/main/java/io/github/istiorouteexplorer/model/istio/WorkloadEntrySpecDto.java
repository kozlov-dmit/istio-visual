package io.github.istiorouteexplorer.model.istio;

import java.util.Map;
import java.util.Objects;

/**
 * WorkloadEntry enables specifying the properties of a single non-Kubernetes workload such a VM or a bare metal services that can be referred to by service entries
 *
 * @param address address
 * @param labels labels
 * @param locality locality
 * @param network network
 * @param ports ports
 * @param serviceAccount serviceAccount
 * @param weight weight
 */

public final class WorkloadEntrySpecDto {

    private final String address;
    private final Map<String, String> labels;
    private final String locality;
    private final String network;
    private final Map<String, Long> ports;
    private final String serviceAccount;
    private final Long weight;

    public WorkloadEntrySpecDto(String address, Map<String, String> labels, String locality, String network, Map<String, Long> ports, String serviceAccount, Long weight) {
        this.address = address;
        this.labels = labels;
        this.locality = locality;
        this.network = network;
        this.ports = ports;
        this.serviceAccount = serviceAccount;
        this.weight = weight;
    }

    public String address() {
        return address;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public String locality() {
        return locality;
    }

    public String network() {
        return network;
    }

    public Map<String, Long> ports() {
        return ports;
    }

    public String serviceAccount() {
        return serviceAccount;
    }

    public Long weight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkloadEntrySpecDto that)) {
            return false;
        }
        return Objects.equals(address, that.address) &&
            Objects.equals(labels, that.labels) &&
            Objects.equals(locality, that.locality) &&
            Objects.equals(network, that.network) &&
            Objects.equals(ports, that.ports) &&
            Objects.equals(serviceAccount, that.serviceAccount) &&
            Objects.equals(weight, that.weight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, labels, locality, network, ports, serviceAccount, weight);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("WorkloadEntrySpecDto{");
        builder.append("address=").append(address);
        builder.append(", labels=").append(labels);
        builder.append(", locality=").append(locality);
        builder.append(", network=").append(network);
        builder.append(", ports=").append(ports);
        builder.append(", serviceAccount=").append(serviceAccount);
        builder.append(", weight=").append(weight);
        builder.append('}');
        return builder.toString();
    }
}
