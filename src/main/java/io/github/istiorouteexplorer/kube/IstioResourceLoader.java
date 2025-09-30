package io.github.istiorouteexplorer.kube;

import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.github.istiorouteexplorer.model.NamespaceResources;
import io.github.istiorouteexplorer.model.ResourceCollection;
import io.github.istiorouteexplorer.model.istio.*;
import io.github.istiorouteexplorer.model.kubernetes.EndpointDto;
import io.github.istiorouteexplorer.model.kubernetes.PodDto;
import io.github.istiorouteexplorer.model.kubernetes.ServiceDto;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IstioResourceLoader {

    private static final Logger log = LoggerFactory.getLogger(IstioResourceLoader.class);

    private final KubernetesClient kubernetesClient;
    private final IstioClient istioClient;
    private final ModelMapper modelMapper;

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
                    listOrEmpty(istioClient.v1beta1().virtualServices().inNamespace(namespace).list().getItems(), VirtualServiceDto.class),
                    listOrEmpty(istioClient.v1beta1().destinationRules().inNamespace(namespace).list().getItems(), DestinationRuleDto.class),
                    listOrEmpty(istioClient.v1beta1().gateways().inNamespace(namespace).list().getItems(), GatewayDto.class),
                    listOrEmpty(istioClient.v1beta1().serviceEntries().inNamespace(namespace).list().getItems(), ServiceEntryDto.class),
                    listOrEmpty(istioClient.v1alpha3().envoyFilters().inNamespace(namespace).list().getItems(), EnvoyFilterDto.class),
                    listOrEmpty(istioClient.v1beta1().workloadEntries().inNamespace(namespace).list().getItems(), WorkloadEntryDto.class),
                    listOrEmpty(kubernetesClient.services().inNamespace(namespace).list().getItems(), ServiceDto.class),
                    listOrEmpty(kubernetesClient.endpoints().inNamespace(namespace).list().getItems(), EndpointDto.class),
                    listOrEmpty(kubernetesClient.pods().inNamespace(namespace).list().getItems(), PodDto.class)
            );
        } catch (KubernetesClientException e) {
            throw new IOException("Failed to load resources for namespace " + namespace + ": " + e.getMessage(), e);
        }
    }

    private <T,R> List<R> listOrEmpty(List<T> items, Class<R> targetClass) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> modelMapper.map(item, targetClass))
                .toList();
    }

}
