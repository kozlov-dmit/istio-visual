package io.github.istiorouteexplorer.model.kubernetes;

import io.fabric8.kubernetes.api.model.ServicePort;

import java.util.List;
import java.util.Map;

/**
 * ServiceSpec describes the attributes that a user creates on a service.
 *
 * @param allocateLoadBalancerNodePorts
 * @param clusterIP
 * @param clusterIPs
 * @param externalIPs
 * @param externalName
 * @param externalTrafficPolicy
 * @param healthCheckNodePort
 * @param internalTrafficPolicy
 * @param ipFamilies
 * @param ipFamilyPolicy
 * @param loadBalancerClass
 * @param loadBalancerIP
 * @param loadBalancerSourceRanges
 * @param ports
 * @param publishNotReadyAddresses
 * @param selector
 * @param sessionAffinity
 * @param trafficDistribution
 * @param type
 */
public record ServiceSpecDto(
        Boolean allocateLoadBalancerNodePorts,
        String clusterIP,
        List<String> clusterIPs,
        List<String> externalIPs,
        String externalName,
        String externalTrafficPolicy,
        Integer healthCheckNodePort,
        String internalTrafficPolicy,
        List<String> ipFamilies,
        String ipFamilyPolicy,
        String loadBalancerClass,
        String loadBalancerIP,
        List<String> loadBalancerSourceRanges,
        List<ServicePort> ports,
        Boolean publishNotReadyAddresses,
        Map<String, String> selector,
        String sessionAffinity,
        String trafficDistribution,
        String type
) {
}
