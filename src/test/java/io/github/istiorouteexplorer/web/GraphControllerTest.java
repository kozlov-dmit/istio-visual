package io.github.istiorouteexplorer.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.istiorouteexplorer.model.GraphEdge;
import io.github.istiorouteexplorer.model.GraphNode;
import io.github.istiorouteexplorer.model.GraphResponse;
import io.github.istiorouteexplorer.service.RouteExplorerException;
import io.github.istiorouteexplorer.service.RouteExplorerService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GraphController.class)
class GraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RouteExplorerService routeExplorerService;

    @Test
    void returnsGraphResponse() throws Exception {
        GraphResponse response = sampleResponse("orders");
        when(routeExplorerService.buildGraph("orders")).thenReturn(response);

        mockMvc.perform(get("/api/graph").param("namespace", "orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.namespace").value("orders"))
                .andExpect(jsonPath("$.summary.nodes").value(1))
                .andExpect(jsonPath("$.nodes[0].id").value("node-1"));

        verify(routeExplorerService).buildGraph("orders");
    }

    @Test
    void returnsBadGatewayWhenServiceFails() throws Exception {
        when(routeExplorerService.buildGraph(null)).thenThrow(new RouteExplorerException("boom", new IOException("fail")));

        mockMvc.perform(get("/api/graph"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("boom"));

        verify(routeExplorerService).buildGraph(null);
    }

    private GraphResponse sampleResponse(String namespace) {
        return new GraphResponse(
                namespace,
                Instant.parse("2025-09-25T10:15:00Z"),
                Map.of("nodes", 1),
                List.of(new GraphNode("node-1", "service", Map.of("service", "reviews"))),
                List.of(new GraphEdge("edge-1", "traffic", "src", "dest", Map.of("protocol", "http"))),
                List.of("Warning")
        );
    }
}
