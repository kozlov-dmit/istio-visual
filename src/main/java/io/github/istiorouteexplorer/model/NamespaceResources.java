package io.github.istiorouteexplorer.model;

import io.fabric8.istio.api.networking.v1alpha3.EnvoyFilter;
import io.fabric8.istio.api.networking.v1beta1.DestinationRule;
import io.fabric8.istio.api.networking.v1beta1.Gateway;
import io.fabric8.istio.api.networking.v1beta1.ServiceEntry;
import io.fabric8.istio.api.networking.v1beta1.Sidecar;
import io.fabric8.istio.api.networking.v1beta1.VirtualService;
import io.fabric8.istio.api.networking.v1beta1.WorkloadEntry;
import io.fabric8.istio.api.security.v1beta1.AuthorizationPolicy;
import io.fabric8.istio.api.security.v1beta1.PeerAuthentication;
import io.fabric8.istio.api.security.v1beta1.RequestAuthentication;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import java.util.List;

public record NamespaceResources(
        String namespace,
        List<VirtualService> virtualServices,
        List<DestinationRule> destinationRules,
        List<Gateway> gateways,
        List<ServiceEntry> serviceEntries,
        List<Sidecar> sidecars,
        List<EnvoyFilter> envoyFilters,
        List<WorkloadEntry> workloadEntries,
        List<AuthorizationPolicy> authorizationPolicies,
        List<PeerAuthentication> peerAuthentications,
        List<RequestAuthentication> requestAuthentications,
        List<Service> services,
        List<Pod> pods
) {
}
