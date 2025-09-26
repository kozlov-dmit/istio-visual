package io.github.istiorouteexplorer.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.istiorouteexplorer.model.GraphEdge;
import io.github.istiorouteexplorer.model.GraphNode;
import io.github.istiorouteexplorer.model.GraphResponse;
import io.github.istiorouteexplorer.model.NamespaceResources;
import io.github.istiorouteexplorer.model.ResourceCollection;
import io.github.istiorouteexplorer.model.istio.DestinationDto;
import io.github.istiorouteexplorer.model.istio.HttpRouteDestinationDto;
import io.github.istiorouteexplorer.model.istio.HttpRouteDto;
import io.github.istiorouteexplorer.model.istio.VirtualServiceDto;
import io.github.istiorouteexplorer.model.istio.VirtualServiceSpecDto;
import io.github.istiorouteexplorer.model.kubernetes.ContainerDto;
import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import io.github.istiorouteexplorer.model.kubernetes.PodDto;
import io.github.istiorouteexplorer.model.kubernetes.PodSpecDto;
import io.github.istiorouteexplorer.model.kubernetes.PodStatusDto;
import io.github.istiorouteexplorer.model.kubernetes.ServiceDto;
import io.github.istiorouteexplorer.model.kubernetes.ServiceSpecDto;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class GraphBuilderTest {

    private final GraphBuilder graphBuilder = new GraphBuilder(new ObjectMapper());

    @Test
    void buildsTrafficGraphFromIstioResources() {
        NamespaceResources resources = primaryNamespace();
        ResourceCollection collection = new ResourceCollection(resources, Map.of());

        GraphResponse response = graphBuilder.build(collection);

        assertEquals("demo", response.namespace());
        assertEquals(4, response.nodes().size(), "Expected one app + one sidecar per pod but got: " + response.nodes());
        assertEquals(3, response.edges().size(), "Pod links plus a single traffic edge");
        assertTrue(response.warnings().isEmpty(), "No warnings expected for well-formed resources");

        assertSummaryEntry(response, "nodes", 4);
        assertSummaryEntry(response, "edges", 3);
        assertSummaryEntry(response, "pods", 2);
        assertSummaryEntry(response, "virtualServices", 1);

        Set<String> nodeIds = response.nodes().stream().map(GraphNode::id).collect(Collectors.toSet());
        assertTrue(nodeIds.contains("container:demo/frontend-pod/frontend-app"));
        assertTrue(nodeIds.contains("container:demo/frontend-pod/istio-proxy"));
        assertTrue(nodeIds.contains("container:demo/backend-pod/backend-app"));
        assertTrue(nodeIds.contains("container:demo/backend-pod/istio-proxy"));

        GraphEdge trafficEdge = response.edges().stream()
                .filter(edge -> edge.kind().equals("traffic"))
                .findFirst()
                .orElseThrow();
        assertEquals("container:demo/frontend-pod/istio-proxy", trafficEdge.source());
        assertEquals("container:demo/backend-pod/istio-proxy", trafficEdge.target());
        assertEquals("backend", trafficEdge.properties().get("destinationHost"));

        long podLinks = response.edges().stream().filter(edge -> edge.kind().equals("podLink")).count();
        assertEquals(2, podLinks, "Each pod should contribute a pod link edge");
    }

    private void assertSummaryEntry(GraphResponse response, String key, int expected) {
        Object value = response.summary().get(key);
        assertTrue(value instanceof Number, () -> "Summary entry '" + key + "' is not numeric");
        assertEquals(expected, ((Number) value).intValue(), "Unexpected value for summary entry '" + key + "'");
    }


    private NamespaceResources primaryNamespace() {
        String namespace = "demo";
        List<VirtualServiceDto> virtualServices = List.of(virtualService(namespace, "frontend-vs", "frontend", "backend"));
        List<ServiceDto> services = List.of(
                service(namespace, "frontend", "frontend"),
                service(namespace, "backend", "backend")
        );
        List<PodDto> pods = List.of(
                pod(namespace, "frontend-pod", "frontend"),
                pod(namespace, "backend-pod", "backend")
        );
        return new NamespaceResources(
                namespace,
                virtualServices,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                services,
                pods
        );
    }

    private PodDto pod(String namespace, String podName, String appLabel) {
        ObjectMetadataDto metadata = new ObjectMetadataDto(podName, namespace, Map.of("app", appLabel), Map.of());
        PodSpecDto spec = new PodSpecDto(List.of(
                new ContainerDto(appLabel + "-app", appLabel + "-app:1"),
                new ContainerDto("istio-proxy", "istio/proxyv2:1")
        ));
        PodStatusDto status = new PodStatusDto("Running");
        return new PodDto(metadata, spec, status);
    }

    private ServiceDto service(String namespace, String name, String appLabel) {
        ObjectMetadataDto metadata = new ObjectMetadataDto(name, namespace, Map.of(), Map.of());
        ServiceSpecDto spec = new ServiceSpecDto(
                null,
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                Map.of("app", appLabel),
                null,
                null,
                "ClusterIP"
        );
        return new ServiceDto(metadata, spec);
    }

    private VirtualServiceDto virtualService(String namespace, String name, String host, String destinationHost) {
        ObjectMetadataDto metadata = new ObjectMetadataDto(name, namespace, Map.of(), Map.of());
        DestinationDto destination = new DestinationDto(destinationHost, null, null);
        HttpRouteDestinationDto primaryRoute = new HttpRouteDestinationDto(destination, 100);
        HttpRouteDto httpRoute = new HttpRouteDto(null, List.of(primaryRoute));
        VirtualServiceSpecDto spec = new VirtualServiceSpecDto(
                List.of(host),
                List.of(httpRoute),
                List.of(),
                List.of()
        );
        return new VirtualServiceDto(metadata, spec);
    }

}
