package io.github.istiorouteexplorer.model.istio;

import java.util.Map;

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
public record WorkloadEntrySpecDto(
        String address,
        Map<String, String> labels,
        String locality,
        String network,
        Map<String, Long> ports,
        String serviceAccount,
        Long weight
) {
}
