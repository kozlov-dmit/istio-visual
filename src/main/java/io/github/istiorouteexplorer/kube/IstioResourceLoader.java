package io.github.istiorouteexplorer.kube;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.istiorouteexplorer.model.NamespaceResources;
import io.github.istiorouteexplorer.model.ResourceCollection;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import java.io.IOException;
import java.util.ArrayList;
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

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {
    };

    private final CoreV1Api coreV1Api;
    private final CustomObjectsApi customObjectsApi;
    private final ObjectMapper mapper;

    public IstioResourceLoader(CoreV1Api coreV1Api, CustomObjectsApi customObjectsApi, ObjectMapper mapper) {
        this.coreV1Api = coreV1Api;
        this.customObjectsApi = customObjectsApi;
        this.mapper = mapper;
    }

    public ResourceCollection load(String namespace, List<String> extraNamespaces) throws IOException {
        NamespaceResources primary = loadSingle(namespace);
        Map<String, NamespaceResources> extras = new HashMap<>();
        for (String extra : extraNamespaces) {
            if (extra == null || extra.isBlank() || extra.equals(namespace)) {
                continue;
            }
            extras.put(extra, loadSingle(extra));
        }
        return new ResourceCollection(primary, extras);
    }

    private NamespaceResources loadSingle(String namespace) throws IOException {
        try {
            return new NamespaceResources(
                    namespace,
                    listNamespacedCustomObject("networking.istio.io", "v1beta1", namespace, "virtualservices"),
                    listNamespacedCustomObject("networking.istio.io", "v1beta1", namespace, "destinationrules"),
                    listNamespacedCustomObject("networking.istio.io", "v1beta1", namespace, "gateways"),
                    listNamespacedCustomObject("networking.istio.io", "v1beta1", namespace, "serviceentries"),
                    listNamespacedCustomObject("networking.istio.io", "v1beta1", namespace, "sidecars"),
                    listNamespacedCustomObject("networking.istio.io", "v1alpha3", namespace, "envoyfilters"),
                    listNamespacedCustomObject("networking.istio.io", "v1alpha3", namespace, "workloadentries"),
                    listNamespacedCustomObject("security.istio.io", "v1beta1", namespace, "authorizationpolicies"),
                    listNamespacedCustomObject("security.istio.io", "v1beta1", namespace, "peerauthentications"),
                    listNamespacedCustomObject("security.istio.io", "v1beta1", namespace, "requestauthentications"),
                    listServices(namespace)
            );
        } catch (ApiException ex) {
            throw new IOException("Failed to load resources: " + ex.getResponseBody(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listNamespacedCustomObject(
            String group,
            String version,
            String namespace,
            String plural
    ) throws ApiException {
        Object response = customObjectsApi.listNamespacedCustomObject(
                group,
                version,
                namespace,
                plural,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
        Map<String, Object> asMap = mapper.convertValue(response, MAP_TYPE);
        Object items = asMap.get("items");
        if (items == null) {
            return Collections.emptyList();
        }
        return mapper.convertValue(items, LIST_OF_MAPS);
    }

    private List<Map<String, Object>> listServices(String namespace) throws ApiException {
        var response = coreV1Api.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null, false);
        List<Map<String, Object>> result = new ArrayList<>();
        response.getItems().forEach(item -> result.add(mapper.convertValue(item, MAP_TYPE)));
        return result;
    }
}

