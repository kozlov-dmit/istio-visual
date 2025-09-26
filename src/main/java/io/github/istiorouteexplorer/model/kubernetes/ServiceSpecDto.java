package io.github.istiorouteexplorer.model.kubernetes;

import io.fabric8.kubernetes.api.model.ServicePort;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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

public final class ServiceSpecDto {

    private final Boolean allocateLoadBalancerNodePorts;
    private final String clusterIP;
    private final List<String> clusterIPs;
    private final List<String> externalIPs;
    private final String externalName;
    private final String externalTrafficPolicy;
    private final Integer healthCheckNodePort;
    private final String internalTrafficPolicy;
    private final List<String> ipFamilies;
    private final String ipFamilyPolicy;
    private final String loadBalancerClass;
    private final String loadBalancerIP;
    private final List<String> loadBalancerSourceRanges;
    private final List<ServicePort> ports;
    private final Boolean publishNotReadyAddresses;
    private final Map<String, String> selector;
    private final String sessionAffinity;
    private final String trafficDistribution;
    private final String type;

    public ServiceSpecDto(Boolean allocateLoadBalancerNodePorts, String clusterIP, List<String> clusterIPs, List<String> externalIPs, String externalName, String externalTrafficPolicy, Integer healthCheckNodePort, String internalTrafficPolicy, List<String> ipFamilies, String ipFamilyPolicy, String loadBalancerClass, String loadBalancerIP, List<String> loadBalancerSourceRanges, List<ServicePort> ports, Boolean publishNotReadyAddresses, Map<String, String> selector, String sessionAffinity, String trafficDistribution, String type) {
        this.allocateLoadBalancerNodePorts = allocateLoadBalancerNodePorts;
        this.clusterIP = clusterIP;
        this.clusterIPs = clusterIPs;
        this.externalIPs = externalIPs;
        this.externalName = externalName;
        this.externalTrafficPolicy = externalTrafficPolicy;
        this.healthCheckNodePort = healthCheckNodePort;
        this.internalTrafficPolicy = internalTrafficPolicy;
        this.ipFamilies = ipFamilies;
        this.ipFamilyPolicy = ipFamilyPolicy;
        this.loadBalancerClass = loadBalancerClass;
        this.loadBalancerIP = loadBalancerIP;
        this.loadBalancerSourceRanges = loadBalancerSourceRanges;
        this.ports = ports;
        this.publishNotReadyAddresses = publishNotReadyAddresses;
        this.selector = selector;
        this.sessionAffinity = sessionAffinity;
        this.trafficDistribution = trafficDistribution;
        this.type = type;
    }

    public Boolean allocateLoadBalancerNodePorts() {
        return allocateLoadBalancerNodePorts;
    }

    public String clusterIP() {
        return clusterIP;
    }

    public List<String> clusterIPs() {
        return clusterIPs;
    }

    public List<String> externalIPs() {
        return externalIPs;
    }

    public String externalName() {
        return externalName;
    }

    public String externalTrafficPolicy() {
        return externalTrafficPolicy;
    }

    public Integer healthCheckNodePort() {
        return healthCheckNodePort;
    }

    public String internalTrafficPolicy() {
        return internalTrafficPolicy;
    }

    public List<String> ipFamilies() {
        return ipFamilies;
    }

    public String ipFamilyPolicy() {
        return ipFamilyPolicy;
    }

    public String loadBalancerClass() {
        return loadBalancerClass;
    }

    public String loadBalancerIP() {
        return loadBalancerIP;
    }

    public List<String> loadBalancerSourceRanges() {
        return loadBalancerSourceRanges;
    }

    public List<ServicePort> ports() {
        return ports;
    }

    public Boolean publishNotReadyAddresses() {
        return publishNotReadyAddresses;
    }

    public Map<String, String> selector() {
        return selector;
    }

    public String sessionAffinity() {
        return sessionAffinity;
    }

    public String trafficDistribution() {
        return trafficDistribution;
    }

    public String type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceSpecDto that)) {
            return false;
        }
        return Objects.equals(allocateLoadBalancerNodePorts, that.allocateLoadBalancerNodePorts) &&
            Objects.equals(clusterIP, that.clusterIP) &&
            Objects.equals(clusterIPs, that.clusterIPs) &&
            Objects.equals(externalIPs, that.externalIPs) &&
            Objects.equals(externalName, that.externalName) &&
            Objects.equals(externalTrafficPolicy, that.externalTrafficPolicy) &&
            Objects.equals(healthCheckNodePort, that.healthCheckNodePort) &&
            Objects.equals(internalTrafficPolicy, that.internalTrafficPolicy) &&
            Objects.equals(ipFamilies, that.ipFamilies) &&
            Objects.equals(ipFamilyPolicy, that.ipFamilyPolicy) &&
            Objects.equals(loadBalancerClass, that.loadBalancerClass) &&
            Objects.equals(loadBalancerIP, that.loadBalancerIP) &&
            Objects.equals(loadBalancerSourceRanges, that.loadBalancerSourceRanges) &&
            Objects.equals(ports, that.ports) &&
            Objects.equals(publishNotReadyAddresses, that.publishNotReadyAddresses) &&
            Objects.equals(selector, that.selector) &&
            Objects.equals(sessionAffinity, that.sessionAffinity) &&
            Objects.equals(trafficDistribution, that.trafficDistribution) &&
            Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allocateLoadBalancerNodePorts, clusterIP, clusterIPs, externalIPs, externalName, externalTrafficPolicy, healthCheckNodePort, internalTrafficPolicy, ipFamilies, ipFamilyPolicy, loadBalancerClass, loadBalancerIP, loadBalancerSourceRanges, ports, publishNotReadyAddresses, selector, sessionAffinity, trafficDistribution, type);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ServiceSpecDto{");
        builder.append("allocateLoadBalancerNodePorts=").append(allocateLoadBalancerNodePorts);
        builder.append(", clusterIP=").append(clusterIP);
        builder.append(", clusterIPs=").append(clusterIPs);
        builder.append(", externalIPs=").append(externalIPs);
        builder.append(", externalName=").append(externalName);
        builder.append(", externalTrafficPolicy=").append(externalTrafficPolicy);
        builder.append(", healthCheckNodePort=").append(healthCheckNodePort);
        builder.append(", internalTrafficPolicy=").append(internalTrafficPolicy);
        builder.append(", ipFamilies=").append(ipFamilies);
        builder.append(", ipFamilyPolicy=").append(ipFamilyPolicy);
        builder.append(", loadBalancerClass=").append(loadBalancerClass);
        builder.append(", loadBalancerIP=").append(loadBalancerIP);
        builder.append(", loadBalancerSourceRanges=").append(loadBalancerSourceRanges);
        builder.append(", ports=").append(ports);
        builder.append(", publishNotReadyAddresses=").append(publishNotReadyAddresses);
        builder.append(", selector=").append(selector);
        builder.append(", sessionAffinity=").append(sessionAffinity);
        builder.append(", trafficDistribution=").append(trafficDistribution);
        builder.append(", type=").append(type);
        builder.append('}');
        return builder.toString();
    }
}
