package io.github.istiorouteexplorer.service;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.istiorouteexplorer.config.AppProperties;
import io.github.istiorouteexplorer.graph.GraphBuilder;
import io.github.istiorouteexplorer.kube.IstioResourceLoader;
import io.github.istiorouteexplorer.model.GraphEdge;
import io.github.istiorouteexplorer.model.GraphNode;
import io.github.istiorouteexplorer.model.GraphResponse;
import io.github.istiorouteexplorer.model.NamespaceResources;
import io.github.istiorouteexplorer.model.ResourceCollection;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RouteExplorerServiceTest {

    private AppProperties properties;
    private IstioResourceLoader loader;
    private GraphBuilder graphBuilder;
    private RouteExplorerService service;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.setNamespace("default");
        properties.setCacheTtl(Duration.ofSeconds(60));
        properties.setExtraNamespaces(List.of("shared"));
        loader = mock(IstioResourceLoader.class);
        graphBuilder = mock(GraphBuilder.class);
        service = new RouteExplorerService(properties, loader, graphBuilder);
    }

    @Test
    void returnsCachedGraphWhenCacheEnabled() throws Exception {
        ResourceCollection collection = emptyCollection("payments");
        GraphResponse response = sampleResponse("payments");

        when(loader.load(eq("payments"), eq(properties.getExtraNamespaces()))).thenReturn(collection);
        when(graphBuilder.build(collection)).thenReturn(response);

        GraphResponse first = service.buildGraph("payments");
        GraphResponse second = service.buildGraph("payments");

        assertSame(response, first);
        assertSame(response, second);
        verify(loader, times(1)).load(eq("payments"), eq(properties.getExtraNamespaces()));
        verify(graphBuilder, times(1)).build(collection);
    }

    @Test
    void reloadsGraphWhenCachingDisabled() throws Exception {
        properties.setCacheTtl(Duration.ZERO);
        ResourceCollection collection = emptyCollection("default");
        GraphResponse firstResponse = sampleResponse("default");
        GraphResponse secondResponse = new GraphResponse(
                "default",
                Instant.parse("2025-09-25T11:05:00Z"),
                Map.of("nodes", 2),
                List.of(new GraphNode("node-2", "host", Map.of())),
                List.of(new GraphEdge("edge-1", "traffic", "src", "dest", Map.of())),
                List.of()
        );

        when(loader.load(eq("default"), eq(properties.getExtraNamespaces()))).thenReturn(collection);
        when(graphBuilder.build(collection)).thenReturn(firstResponse, secondResponse);

        GraphResponse first = service.buildGraph(null);
        GraphResponse second = service.buildGraph(" ");

        assertNotSame(first, second);
        verify(loader, times(2)).load(eq("default"), eq(properties.getExtraNamespaces()));
        verify(graphBuilder, times(2)).build(collection);
    }

    @Test
    void wrapsIOExceptionFromLoader() throws Exception {
        when(loader.load(eq("default"), eq(properties.getExtraNamespaces()))).thenThrow(new IOException("boom"));

        RouteExplorerException ex = assertThrows(RouteExplorerException.class, () -> service.buildGraph(null));
        verify(loader, times(1)).load(eq("default"), eq(properties.getExtraNamespaces()));
        verify(graphBuilder, times(0)).build(org.mockito.Mockito.any());
        org.assertj.core.api.Assertions.assertThat(ex.getMessage()).contains("default");
    }

    private ResourceCollection emptyCollection(String namespace) {
        NamespaceResources resources = new NamespaceResources(
                namespace,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        return new ResourceCollection(resources, Map.of());
    }

    private GraphResponse sampleResponse(String namespace) {
        return new GraphResponse(
                namespace,
                Instant.parse("2025-09-25T10:00:00Z"),
                Map.of("nodes", 1),
                List.of(new GraphNode("node-1", "service", Map.of("service", "reviews"))),
                List.of(),
                List.of()
        );
    }
}



