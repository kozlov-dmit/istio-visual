package io.github.istiorouteexplorer.model.kubernetes;

import io.fabric8.kubernetes.api.model.ServicePort;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO describing the specification block of a Kubernetes Service.
 */
@Data
@NoArgsConstructor(force = true)
public class ServiceSpecDto {

    private Boolean allocateLoadBalancerNodePorts;
    private String clusterIP;
    private List<String> clusterIPs;
    private List<String> externalIPs;
    private String externalName;
    private String externalTrafficPolicy;
    private Integer healthCheckNodePort;
    private String internalTrafficPolicy;
    private List<String> ipFamilies;
    private String ipFamilyPolicy;
    private String loadBalancerClass;
    private String loadBalancerIP;
    private List<String> loadBalancerSourceRanges;
    private List<ServicePort> ports;
    private Boolean publishNotReadyAddresses;
    private Map<String, String> selector;
    private String sessionAffinity;
    private String trafficDistribution;
    private String type;

}
