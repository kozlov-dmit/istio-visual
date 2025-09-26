package io.github.istiorouteexplorer.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import io.github.istiorouteexplorer.model.GraphEdge;
import io.github.istiorouteexplorer.model.GraphNode;
import io.github.istiorouteexplorer.model.GraphResponse;
import io.github.istiorouteexplorer.service.RouteExplorerService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class TopologyUiE2ETest {

    @LocalServerPort
    private int port;

    @MockBean
    private RouteExplorerService routeExplorerService;

    private GraphResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = buildSampleGraph();
        when(routeExplorerService.buildGraph(any())).thenReturn(sampleResponse);
    }

    @Test
    void rendersTopologyAndRoutesScreenshots() throws IOException {
        Path screenshotDir = Path.of("target", "ui-snapshots");
        Files.createDirectories(screenshotDir);
        Path topologyScreenshot = screenshotDir.resolve("topology.png");
        Path routesScreenshot = screenshotDir.resolve("routes.png");

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(options);
             Page page = browser.newPage()) {

            page.addInitScript("window.__errors = []; window.addEventListener('error', e => window.__errors.push({message: e.message, filename: e.filename, lineno: e.lineno, colno: e.colno}));");

            page.navigate("http://localhost:" + port + "/?namespace=default",
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            page.waitForSelector("svg.graph-svg",
                    new Page.WaitForSelectorOptions()
                            .setTimeout(20_000)
                            .setState(WaitForSelectorState.VISIBLE));
            page.waitForSelector("circle[data-node-id='container:default/checkout/app']",
                    new Page.WaitForSelectorOptions().setTimeout(20_000));
            page.click("circle[data-node-id='container:default/checkout/app']");
            page.waitForSelector(".details h3",
                    new Page.WaitForSelectorOptions().setTimeout(10_000));

            page.screenshot(new Page.ScreenshotOptions().setPath(topologyScreenshot).setFullPage(true));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> topologyErrors = (List<Map<String, Object>>) page.evaluate("() => window.__errors");
            assertTrue(topologyErrors == null || topologyErrors.isEmpty(), "Topology view console errors detected: " + topologyErrors);
            assertTrue(Files.exists(topologyScreenshot), "Topology screenshot was not created");

            page.evaluate("window.__errors = [];");

            page.navigate("http://localhost:" + port + "/?namespace=default&view=routes",
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            page.waitForSelector(".routes-grid .route-card",
                    new Page.WaitForSelectorOptions()
                            .setTimeout(20_000)
                            .setState(WaitForSelectorState.VISIBLE));
            page.waitForSelector(".route-rules li",
                    new Page.WaitForSelectorOptions().setTimeout(20_000));

            page.screenshot(new Page.ScreenshotOptions().setPath(routesScreenshot).setFullPage(true));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> routeErrors = (List<Map<String, Object>>) page.evaluate("() => window.__errors");
            assertTrue(routeErrors == null || routeErrors.isEmpty(), "Routes view console errors detected: " + routeErrors);
            assertTrue(Files.exists(routesScreenshot), "Routes screenshot was not created");
        }
    }

    private GraphResponse buildSampleGraph() {
        Map<String, GraphNode> nodeIndex = new LinkedHashMap<>();
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        String[] podNames = {"checkout", "payments", "inventory", "catalog", "orders", "users", "shipping", "analytics", "billing", "support"};

        for (String podName : podNames) {
            GraphNode appNode = new GraphNode(
                    "container:default/" + podName + "/app",
                    "appContainer",
                    Map.of(
                            "namespace", "default",
                            "pod", podName,
                            "container", "app",
                            "displayName", podName,
                            "containerType", "app",
                            "image", "registry.local/" + podName + ":1.0"
                    )
            );
            addNode(nodeIndex, nodes, appNode);

            GraphNode sidecarNode = new GraphNode(
                    "container:default/" + podName + "/istio-proxy",
                    "sidecarContainer",
                    Map.of(
                            "namespace", "default",
                            "pod", podName,
                            "container", "istio-proxy",
                            "displayName", podName + "-proxy",
                            "containerType", "sidecar",
                            "image", "istio/proxyv2:1.24"
                    )
            );
            addNode(nodeIndex, nodes, sidecarNode);

            edges.add(new GraphEdge(
                    "edge:pod-link:" + podName,
                    "podLink",
                    appNode.id(),
                    sidecarNode.id(),
                    Map.of("relation", "podLink")
            ));
        }

        GraphNode egressApp = new GraphNode(
                "container:istio-system/istio-egressgateway/app",
                "appContainer",
                Map.of(
                        "namespace", "istio-system",
                        "pod", "istio-egressgateway-1",
                        "container", "egress-app",
                        "displayName", "egress-gateway",
                        "containerType", "app",
                        "image", "istio/egress:1.24"
                )
        );
        addNode(nodeIndex, nodes, egressApp);

        GraphNode egressSidecar = new GraphNode(
                "container:istio-system/istio-egressgateway/istio-proxy",
                "sidecarContainer",
                Map.of(
                        "namespace", "istio-system",
                        "pod", "istio-egressgateway-1",
                        "container", "istio-proxy",
                        "displayName", "egress-proxy",
                        "containerType", "sidecar",
                        "image", "istio/proxyv2:1.24"
                )
        );
        addNode(nodeIndex, nodes, egressSidecar);

        edges.add(new GraphEdge(
                "edge:pod-link:egress",
                "podLink",
                egressApp.id(),
                egressSidecar.id(),
                Map.of("relation", "podLink")
        ));

        Map<String, GraphNode> externalIndex = new LinkedHashMap<>();
        int routeCount = 40;
        for (int i = 0; i < routeCount; i++) {
            String podName = podNames[i % podNames.length];
            String sidecarId = "container:default/" + podName + "/istio-proxy";
            if (!nodeIndex.containsKey(sidecarId)) {
                continue;
            }
            String vsName = "vs-" + podName + "-" + (i + 1);

            Map<String, Object> edgeMetaToEgress = new LinkedHashMap<>();
            edgeMetaToEgress.put("virtualService", Map.of("name", vsName, "namespace", "default"));
            edgeMetaToEgress.put("destinationHost", "istio-egressgateway.istio-system.svc.cluster.local");
            edgeMetaToEgress.put("destinationNamespace", "istio-system");
            Map<String, Object> routeSettings = new LinkedHashMap<>();
            routeSettings.put("timeout", (2 + (i % 4)) + "s");
            routeSettings.put("retries", Map.of(
                    "attempts", 1 + (i % 3),
                    "retryOn", List.of("5xx", "gateway-error")));
            if (i % 7 == 0) {
                routeSettings.put("corsPolicy", Map.of("allowOrigins", List.of("*")));
            }
            edgeMetaToEgress.put("route", routeSettings);

            edges.add(new GraphEdge(
                    "edge:traffic:" + podName + ":to-egress-" + i,
                    "traffic",
                    sidecarId,
                    egressSidecar.id(),
                    edgeMetaToEgress
            ));

            String host = "svc" + i + ".external.local";
            String externalId = "external:service-entry-" + i;
            GraphNode externalNode = externalIndex.get(externalId);
            if (externalNode == null) {
                String serviceEntryType = switch (i % 4) {
                    case 0 -> "HTTP";
                    case 1 -> "TLS";
                    case 2 -> "TCP";
                    default -> "Mongo";
                };
                externalNode = new GraphNode(
                        externalId,
                        "externalService",
                        Map.of(
                                "host", host,
                                "displayName", "ServiceEntry " + (i + 1),
                                "serviceEntryType", serviceEntryType
                        )
                );
                externalIndex.put(externalId, externalNode);
                addNode(nodeIndex, nodes, externalNode);
            }

            Map<String, Object> edgeMetaToExternal = new LinkedHashMap<>();
            edgeMetaToExternal.put("virtualService", Map.of("name", vsName, "namespace", "default"));
            edgeMetaToExternal.put("destinationHost", host);
            edgeMetaToExternal.put("destinationNamespace", "external");
            if (i % 2 == 0) {
                edgeMetaToExternal.put("trafficPolicy", Map.of("tls", Map.of("mode", "ISTIO_MUTUAL")));
            }
            Map<String, Object> outboundRoute = new LinkedHashMap<>();
            outboundRoute.put("timeout", (3 + (i % 5)) + "s");
            outboundRoute.put("weight", 100);
            outboundRoute.put("match", List.of(Map.of("port", Map.of("number", 443 + (i % 2)))));
            if (i % 3 == 0) {
                outboundRoute.put("fault", Map.of(
                        "delay", Map.of(
                                "percentage", Map.of("value", 5),
                                "fixedDelay", "2s")));
            }
            if (i % 4 == 0) {
                outboundRoute.put("headers", Map.of(
                        "request", Map.of(
                                "add", Map.of("x-trace-id", "trace-" + i))));
            }
            if (i % 5 == 0) {
                outboundRoute.put("mirror", Map.of("host", "mirror.svc.cluster.local"));
            }
            if (i % 6 == 0) {
                outboundRoute.put("retries", Map.of(
                        "attempts", 2 + (i % 3),
                        "retryOn", List.of("5xx", "gateway-error", "connect-failure")));
                edgeMetaToExternal.put("requestTimeout", (i % 4 + 4) + "s");
            }
            edgeMetaToExternal.put("route", outboundRoute);
            edgeMetaToExternal.put("external", Map.of(
                    "displayName", "ServiceEntry " + (i + 1),
                    "host", host,
                    "type", (i % 2 == 0) ? "TLS" : "HTTP"
            ));

            edges.add(new GraphEdge(
                    "edge:traffic:egress-to-external-" + i,
                    "traffic",
                    egressSidecar.id(),
                    externalId,
                    edgeMetaToExternal
            ));
        }

        List<String> warnings = List.of(
                "Sample warning: External traffic requires SNI",
                "Sample warning: Some routes enforce mirrored traffic"
        );
        Map<String, Object> summary = Map.of(
                "nodes", nodes.size(),
                "edges", edges.size(),
                "virtualServices", routeCount,
                "serviceEntries", routeCount
        );
        return new GraphResponse(
                "default",
                Instant.parse("2025-09-25T10:00:00Z"),
                summary,
                nodes,
                edges,
                warnings
        );
    }

    private void addNode(Map<String, GraphNode> index, List<GraphNode> nodes, GraphNode node) {
        if (!index.containsKey(node.id())) {
            index.put(node.id(), node);
            nodes.add(node);
        }
    }
}

