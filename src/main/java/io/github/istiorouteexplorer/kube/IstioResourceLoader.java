package io.github.istiorouteexplorer.kube;

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
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.github.istiorouteexplorer.model.NamespaceResources;
import io.github.istiorouteexplorer.model.ResourceCollection;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IstioResourceLoader {

    private static final Logger log = LoggerFactory.getLogger(IstioResourceLoader.class);

    private final KubernetesClient kubernetesClient;
    private final IstioClient istioClient;

    public IstioResourceLoader(KubernetesClient kubernetesClient, IstioClient istioClient) {
        this.kubernetesClient = kubernetesClient;
        this.istioClient = istioClient;
    }

    public ResourceCollection load(String namespace, List<String> extraNamespaces) throws IOException {
        NamespaceResources primary = loadNamespace(namespace);
        Map<String, NamespaceResources> extras = new HashMap<>();
        for (String extra : extraNamespaces) {
            if (extra == null || extra.isBlank() || extra.equals(namespace)) {
                continue;
            }
            extras.put(extra, loadNamespace(extra));
        }
        return new ResourceCollection(primary, extras);
    }

    private NamespaceResources loadNamespace(String namespace) throws IOException {
        try {
            log.debug("Loading Istio resources for namespace {}", namespace);
            return new NamespaceResources(
                    namespace,
                    listOrEmpty(istioClient.v1beta1().virtualServices().inNamespace(namespace).list().getItems()),
                    listOrEmpty(istioClient.v1beta1().destinationRules().inNamespace(namespace).list().getItems()),
                    listOrEmpty(istioClient.v1beta1().gateways().inNamespace(namespace).list().getItems()),
                    listOrEmpty(istioClient.v1beta1().serviceEntries().inNamespace(namespace).list().getItems()),
                    listOrEmpty(istioClient.v1beta1().sidecars().inNamespace(namespace).list().getItems()),
                    listOrEmpty(istioClient.v1alpha3().envoyFilters().inNamespace(namespace).list().getItems()),
                    listOrEmpty(istioClient.v1beta1().workloadEntries().inNamespace(namespace).list().getItems()),
                    listOrEmpty(istioClient.v1beta1().authorizationPolicies().inNamespace(namespace).list().getItems()),
                    listOrEmpty(istioClient.v1beta1().peerAuthentications().inNamespace(namespace).list().getItems()),
                    listOrEmpty(istioClient.v1beta1().requestAuthentications().inNamespace(namespace).list().getItems()),
                    listOrEmpty(kubernetesClient.services().inNamespace(namespace).list().getItems()),
                    listOrEmpty(kubernetesClient.pods().inNamespace(namespace).list().getItems())
            );
        } catch (KubernetesClientException e) {
            throw new IOException("Failed to load resources for namespace " + namespace + ": " + e.getMessage(), e);
        }
    }

    private static <T> List<T> listOrEmpty(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }
}
