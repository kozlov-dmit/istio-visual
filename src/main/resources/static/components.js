import { computeLayout, edgePath } from "./topology.js";

export function createApp(React, options = {}) {
    const { useEffect, useMemo, useState, useRef, Fragment } = React;
    const fetchImpl = options.fetchImpl ?? (typeof fetch !== "undefined" ? fetch.bind(window) : null);
    const defaultNamespace = options.defaultNamespace ?? "default";
    const initialParams = typeof window !== "undefined" ? new URLSearchParams(window.location.search) : new URLSearchParams();
    const initialViewParam = options.view ?? initialParams.get("view") ?? "topology";
    const initialView = initialViewParam === "routes" ? "routes" : "topology";

    function Legend() {
        return React.createElement(
            "div",
            { className: "legend" },
            React.createElement("div", null,
                React.createElement("span", { className: "legend-dot service" }),
                "Service"
            ),
            React.createElement("div", null,
                React.createElement("span", { className: "legend-dot external" }),
                "External service"
            ),
            React.createElement("div", null,
                React.createElement("span", { className: "legend-dot mesh" }),
                "Mesh / gateway"
            )
        );
    }

    function Summary({ summary }) {
        if (!summary || Object.keys(summary).length === 0) {
            return React.createElement("div", { className: "summary" }, "No summary data");
        }
        return React.createElement(
            "div",
            { className: "summary" },
            Object.entries(summary).map(([key, value]) =>
                React.createElement("div", { key },
                    React.createElement("span", null, key),
                    React.createElement("strong", null, String(value))
                )
            )
        );
    }

    function Warnings({ warnings }) {
        if (!warnings?.length) {
            return React.createElement("ul", { className: "warnings" },
                React.createElement("li", { key: "none" }, "No warnings")
            );
        }
        return React.createElement(
            "ul",
            { className: "warnings" },
            warnings.map((warning, idx) =>
                React.createElement("li", { key: idx }, warning)
            )
        );
    }

    function describeNodeDetails(node) {
        if (!node) {
            return { rows: [], pods: [] };
        }
        const rows = [];
        const pods = Array.isArray(node.pods) ? node.pods : [];
        const typeLabel = node.external ? "External service" : (node.type === "mesh" ? "Mesh gateway" : "Service");
        rows.push(["Type", typeLabel]);
        rows.push(["Name", node.label ?? node.name ?? node.id]);
        if (node.namespace) {
            rows.push(["Namespace", node.namespace]);
        }
        if (node.host) {
            rows.push(["Host", node.host]);
        }
        if (node.metrics) {
            if (node.metrics.outbound != null) {
                rows.push(["Outbound links", String(node.metrics.outbound)]);
            }
            if (node.metrics.inbound != null) {
                rows.push(["Inbound links", String(node.metrics.inbound)]);
            }
            if (node.metrics.podCount != null) {
                rows.push(["Pod count", String(node.metrics.podCount)]);
            }
        }
        if (Array.isArray(node.containerIds) && node.containerIds.length) {
            rows.push(["Containers", node.containerIds.join(", ")]);
        }
        return { rows, pods };
    }

    function describeEdge(edge) {
        if (!edge) {
            return { rows: [], routes: [] };
        }
        const rows = [];
        rows.push(["Source", edge.sourceLabel ?? edge.source ?? "unknown"]);
        rows.push(["Target", edge.targetLabel ?? edge.target ?? "unknown"]);
        const props = edge.properties ?? {};
        if (props.connectionCount != null) {
            rows.push(["Connections", String(props.connectionCount)]);
        }
        const virtualServices = Array.isArray(props.virtualServices) ? props.virtualServices : [];
        if (virtualServices.length) {
            rows.push(["Virtual services", virtualServices.join(", ")]);
        }
        if (props.destinationHost) {
            rows.push(["Destination host", props.destinationHost]);
        }
        if (props.destinationNamespace) {
            rows.push(["Destination namespace", props.destinationNamespace]);
        }
        if (props.requestTimeout) {
            rows.push(["Request timeout", props.requestTimeout]);
        }
        const routeSummaries = Array.isArray(props.routeSummaries) ? props.routeSummaries : [];
        return { rows, routes: routeSummaries };
    }

    function DetailsPanel({ selected }) {
        if (!selected) {
            return React.createElement("pre", { className: "details" }, "Select a node or edge to inspect");
        }
        if (selected.type === "node") {
            const detail = describeNodeDetails(selected.data);
            const title = selected.data.label ?? selected.data.name ?? selected.data.id;
            return React.createElement(
                "div",
                { className: "details" },
                React.createElement("h3", null, title),
                React.createElement("div", { className: "details-grid" },
                    detail.rows.map(([label, value], idx) => React.createElement(React.Fragment, { key: `${label}-${idx}` },
                        React.createElement("span", { className: "details-label" }, label),
                        React.createElement("span", { className: "details-value" }, String(value))
                    ))
                ),
                detail.pods.length ? React.createElement("div", { className: "details-section" },
                    React.createElement("h4", null, "Pods"),
                    React.createElement("ul", null,
                        detail.pods.map((pod) => React.createElement("li", { key: pod }, pod))
                    )
                ) : null
            );
        }
        const detail = describeEdge(selected.data);
        const sourceLabel = detail.rows.find(([label]) => label === "Source")?.[1] ?? "Source";
        const targetLabel = detail.rows.find(([label]) => label === "Target")?.[1] ?? "Target";
        return React.createElement(
            "div",
            { className: "details" },
            React.createElement("h3", null, `Traffic ${sourceLabel} -> ${targetLabel}`),
            React.createElement("div", { className: "details-grid" },
                detail.rows.map(([label, value], idx) => React.createElement(React.Fragment, { key: `${label}-${idx}` },
                    React.createElement("span", { className: "details-label" }, label),
                    React.createElement("span", { className: "details-value" }, String(value))
                ))
            ),
            detail.routes.length ? React.createElement("div", { className: "details-section" },
                React.createElement("h4", null, "Route settings"),
                React.createElement("ul", null,
                    detail.routes.map((route, idx) => React.createElement("li", { key: idx },
                        React.createElement("pre", null, JSON.stringify(route, null, 2))
                    ))
                )
            ) : null
        );
    }

    function TopologyDiagram({ layout, selected, focusNodeId, onSelect }) {
        if (!layout || !layout.nodes.length) {
            return React.createElement("div", { className: "graph-panel" },
                React.createElement("div", { className: "empty-state" }, "No topology data")
            );
        }

        const bounds = layout.bounds ?? { minX: 0, minY: 0, maxX: layout.width ?? 0, maxY: layout.height ?? 0 };
        const baseViewBox = useMemo(() => ({
            minX: bounds.minX,
            minY: bounds.minY,
            width: Math.max(200, bounds.maxX - bounds.minX),
            height: Math.max(200, bounds.maxY - bounds.minY)
        }), [bounds.minX, bounds.minY, bounds.maxX, bounds.maxY]);

        const [viewBoxState, setViewBoxState] = useState({ ...baseViewBox });
        const [isPanning, setIsPanning] = useState(false);
        const svgRef = useRef(null);
        const isPanningRef = useRef(false);
        const lastPointRef = useRef({ x: 0, y: 0 });

        useEffect(() => {
            setViewBoxState({ ...baseViewBox });
        }, [baseViewBox]);

        useEffect(() => {
            if (!focusNodeId) {
                return;
            }
            const node = layout.nodes.find((item) => item.id === focusNodeId);
            if (!node) {
                return;
            }
            setViewBoxState((prev) => ({
                minX: node.x - prev.width / 2,
                minY: node.y - prev.height / 2,
                width: prev.width,
                height: prev.height
            }));
        }, [focusNodeId, layout.nodes]);

        const minScale = 0.12;
        const maxScale = 4;

        const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

        const getSvgPoint = (event) => {
            const svg = svgRef.current;
            if (!svg) {
                return { x: 0, y: 0 };
            }
            const rect = svg.getBoundingClientRect();
            const xRatio = viewBoxState.width / rect.width;
            const yRatio = viewBoxState.height / rect.height;
            return {
                x: viewBoxState.minX + (event.clientX - rect.left) * xRatio,
                y: viewBoxState.minY + (event.clientY - rect.top) * yRatio
            };
        };

        const applyZoom = (factor, focusPoint) => {
            setViewBoxState((prev) => {
                const currentScale = prev.width / baseViewBox.width;
                const targetScale = clamp(currentScale * factor, minScale, maxScale);
                const width = baseViewBox.width * targetScale;
                const height = baseViewBox.height * targetScale;
                const focusX = focusPoint?.x ?? prev.minX + prev.width / 2;
                const focusY = focusPoint?.y ?? prev.minY + prev.height / 2;
                const offsetX = (focusX - prev.minX) / prev.width;
                const offsetY = (focusY - prev.minY) / prev.height;
                return {
                    minX: focusX - width * offsetX,
                    minY: focusY - height * offsetY,
                    width,
                    height
                };
            });
        };

        const handleWheel = (event) => {
            event.preventDefault();
            const focusPoint = getSvgPoint(event);
            const factor = event.deltaY > 0 ? 1.1 : 0.9;
            applyZoom(factor, focusPoint);
        };

        const handleZoomIn = () => applyZoom(0.8);
        const handleZoomOut = () => applyZoom(1.25);
        const handleReset = () => setViewBoxState({ ...baseViewBox });

        const handlePointerDown = (event) => {
            if (event.button !== 0) {
                return;
            }
            if (event.target?.dataset?.interactive === "true") {
                return;
            }
            const svg = svgRef.current;
            if (!svg) {
                return;
            }
            event.preventDefault();
            try {
                svg.setPointerCapture(event.pointerId);
            } catch (error) {
                /* no-op */
            }
            isPanningRef.current = true;
            setIsPanning(true);
            lastPointRef.current = getSvgPoint(event);
        };

        const handlePointerMove = (event) => {
            if (!isPanningRef.current) {
                return;
            }
            event.preventDefault();
            const currentPoint = getSvgPoint(event);
            const lastPoint = lastPointRef.current;
            const dx = currentPoint.x - lastPoint.x;
            const dy = currentPoint.y - lastPoint.y;
            if (!Number.isFinite(dx) || !Number.isFinite(dy)) {
                return;
            }
            setViewBoxState((prev) => ({
                minX: prev.minX - dx,
                minY: prev.minY - dy,
                width: prev.width,
                height: prev.height
            }));
            lastPointRef.current = currentPoint;
        };

        const endPan = (event) => {
            if (!isPanningRef.current) {
                return;
            }
            const svg = svgRef.current;
            if (svg && event?.pointerId !== undefined) {
                try {
                    svg.releasePointerCapture(event.pointerId);
                } catch (error) {
                    /* no-op */
                }
            }
            isPanningRef.current = false;
            setIsPanning(false);
        };

        const viewBoxValue = `${viewBoxState.minX} ${viewBoxState.minY} ${viewBoxState.width} ${viewBoxState.height}`;
        const nodeIndex = useMemo(() => new Map(layout.nodes.map((node) => [node.id, node])), [layout.nodes]);
        const selectedNodeId = selected?.type === "node" ? selected.data?.id : null;
        const selectedEdgeId = selected?.type === "edge" ? selected.data?.id : null;

        const markerDefs = React.createElement("defs", null,
            React.createElement("marker", {
                id: "edge-arrow",
                viewBox: "0 0 12 12",
                refX: "12",
                refY: "6",
                markerWidth: "8",
                markerHeight: "8",
                orient: "auto",
                style: { color: "inherit" }
            },
                React.createElement("path", {
                    d: "M 0 0 L 12 6 L 0 12 z",
                    fill: "currentColor"
                })
            )
        );

        const edgeElements = layout.edges.map((edge) => {
            const source = nodeIndex.get(edge.source);
            const target = nodeIndex.get(edge.target);
            if (!source || !target) {
                return null;
            }
            const weight = edge.properties?.connectionCount ?? 1;
            const strokeWidth = Math.min(5, 1.2 + Math.log10(weight + 1));
            const isSelected = selectedEdgeId === edge.id;
            const stroke = isSelected ? "#fbbf24" : "rgba(148,163,184,0.55)";
            return React.createElement("path", {
                key: edge.id,
                d: edgePath(source, target),
                fill: "none",
                stroke,
                strokeWidth,
                markerEnd: "url(#edge-arrow)",
                strokeLinecap: "round",
                strokeLinejoin: "round",
                onClick: (event) => {
                    event.stopPropagation();
                    onSelect({ type: "edge", data: edge });
                },
                role: "button",
                tabIndex: 0,
                style: { cursor: "pointer", color: stroke },
                "data-edge-id": edge.id,
                "data-interactive": "true"
            });
        }).filter(Boolean);

        const nodeElements = layout.nodes.map((node) => {
            const isSelected = selectedNodeId === node.id;
            return React.createElement("g", { key: node.id, className: "service-node" },
                React.createElement("circle", {
                    cx: node.x,
                    cy: node.y,
                    r: node.radius,
                    fill: node.color,
                    stroke: isSelected ? "#fbbf24" : "#0f172a",
                    strokeWidth: isSelected ? 3 : 2,
                    onClick: (event) => {
                        event.stopPropagation();
                        onSelect({ type: "node", data: node });
                    },
                    role: "button",
                    tabIndex: 0,
                    "aria-label": node.label,
                    style: { cursor: "pointer" },
                    "data-node-id": node.id,
                    "data-interactive": "true"
                }),
                React.createElement("text", {
                    x: node.x,
                    y: node.y - node.radius - 10,
                    textAnchor: "middle",
                    fill: "#e2e8f0",
                    fontSize: 13,
                    className: "node-label"
                }, node.label),
                node.namespace ? React.createElement("text", {
                    x: node.x,
                    y: node.y + node.radius + 18,
                    textAnchor: "middle",
                    fill: "#94a3b8",
                    fontSize: 11
                }, node.namespace) : null
            );
        });

        return React.createElement("div", { className: `graph-panel${isPanning ? " panning" : ""}` },
            React.createElement("div", { className: "zoom-controls" },
                React.createElement("button", {
                    type: "button",
                    onClick: handleZoomIn,
                    "aria-label": "Zoom in"
                }, "+"),
                React.createElement("button", {
                    type: "button",
                    onClick: handleZoomOut,
                    "aria-label": "Zoom out"
                }, "-"),
                React.createElement("button", {
                    type: "button",
                    onClick: handleReset,
                    "aria-label": "Reset view"
                }, "Reset")
            ),
            React.createElement("svg", {
                ref: svgRef,
                className: "graph-svg",
                viewBox: viewBoxValue,
                onWheel: handleWheel,
                onPointerDown: handlePointerDown,
                onPointerMove: handlePointerMove,
                onPointerUp: endPan,
                onPointerLeave: endPan,
                onPointerCancel: endPan,
                style: { touchAction: "none" }
            },
                markerDefs,
                edgeElements,
                nodeElements
            ),
            React.createElement(Legend, null)
        );
    }


    function ViewToggle({ view, onChange }) {
        const buttons = [
            { id: "topology", label: "Topology" },
            { id: "routes", label: "Routes" }
        ];
        return React.createElement(
            "div",
            { className: "view-toggle" },
            buttons.map((item) => React.createElement("button", {
                key: item.id,
                type: "button",
                className: item.id === view ? "active" : "",
                "aria-pressed": item.id === view,
                onClick: () => onChange(item.id)
            }, item.label))
        );
    }

    function StatusIndicator({ status, error, updatedAt }) {
        const state = status === "error" ? "error" : status === "loading" ? "pending" : "ok";
        let text = "Idle";
        if (status === "loading") {
            text = "Loading topology...";
        } else if (status === "error") {
            text = error ?? "Failed to load";
        } else if (status === "ready" && updatedAt) {
            text = `Updated ${new Date(updatedAt).toLocaleString()}`;
        } else if (status === "ready") {
            text = "Loaded";
        }
        return React.createElement("span", { className: "status", "data-state": state }, text);
    }

    function RouteCard({ group }) {
        const header = React.createElement("div", { className: "route-card-header" },
            React.createElement("h3", null, group.label),
            React.createElement("span", { className: "route-card-namespace" }, group.namespace)
        );
        const edges = group.edges.map((edge) => React.createElement("div", { key: edge.id, className: "route-edge" },
            React.createElement("div", { className: "route-edge-main" },
                React.createElement("div", { className: "route-node" },
                    React.createElement("span", { className: `route-node-label ${edge.source.type}` }, edge.source.label),
                    edge.source.subtitle ? React.createElement("small", { className: "route-node-subtitle" }, edge.source.subtitle) : null
                ),
                React.createElement("div", { className: "route-arrow", "aria-hidden": "true" }, ">"),
                React.createElement("div", { className: "route-node" },
                    React.createElement("span", { className: `route-node-label ${edge.target.type}` }, edge.target.label),
                    edge.target.subtitle ? React.createElement("small", { className: "route-node-subtitle" }, edge.target.subtitle) : null
                )
            ),
            React.createElement("ul", { className: "route-rules" },
                edge.rules.map((rule, idx) => React.createElement("li", { key: idx }, rule))
            )
        ));
        return React.createElement("section", { className: "route-card" }, header, edges);
    }

    function RoutesLayout({ status, error, routeGroups, warnings }) {
        if (status === "loading" || status === "idle") {
            return React.createElement("main", { className: "routes-layout" },
                React.createElement("div", { className: "routes-state" }, status === "loading" ? "Loading routes..." : "No namespace selected"));
        }
        if (status === "error") {
            return React.createElement("main", { className: "routes-layout" },
                React.createElement("div", { className: "routes-error" }, error ?? "Failed to load routes"));
        }
        if (!routeGroups.length) {
            return React.createElement("main", { className: "routes-layout" },
                React.createElement("div", { className: "routes-state" }, "No routed traffic detected"));
        }
        return React.createElement(Fragment, null,
            React.createElement("main", { className: "routes-layout" },
                React.createElement("div", { className: "routes-grid" },
                    routeGroups.map((group) => React.createElement(RouteCard, { key: group.id, group }))
                )
            ),
            warnings?.length ? React.createElement("section", { className: "routes-warnings" },
                React.createElement("h2", null, "Warnings"),
                React.createElement(Warnings, { warnings })
            ) : null
        );
    }

    function formatNodeSummary(node, fallbackId, meta) {
        if (node) {
            const props = node.properties ?? {};
            const label = props.displayName ?? props.container ?? props.pod ?? props.host ?? node.id;
            let subtitle = "";
            if (node.type === "externalService") {
                subtitle = props.host ?? subtitle;
            } else if (props.pod && props.namespace) {
                subtitle = `${props.pod}.${props.namespace}`;
            } else if (props.namespace) {
                subtitle = props.namespace;
            }
            return {
                label,
                subtitle,
                type: node.type ?? "unknown"
            };
        }
        if (meta?.external) {
            const ext = meta.external;
            const label = ext.displayName ?? ext.host ?? fallbackId;
            const subtitle = ext.host && ext.host !== label ? ext.host : "External";
            return {
                label,
                subtitle,
                type: "externalService"
            };
        }
        return { label: fallbackId, subtitle: "", type: "unknown" };
    }

    function extractRouteRules(meta) {
        const rules = [];
        const route = meta.route ?? {};
        const trafficPolicy = meta.trafficPolicy ?? {};
        if (meta.destinationHost) {
            rules.push(`Destination host: ${meta.destinationHost}`);
        }
        if (meta.destinationNamespace && meta.destinationNamespace !== (meta.virtualService?.namespace ?? "")) {
            rules.push(`Target namespace: ${meta.destinationNamespace}`);
        }
        const tlsMode = trafficPolicy.tls?.mode ?? route.tls?.mode;
        if (tlsMode) {
            rules.push(`TLS mode: ${tlsMode}`);
        }
        if (route.timeout) {
            rules.push(`Timeout: ${route.timeout}`);
        }
        const retries = route.retries ?? route.retryPolicy;
        if (retries?.attempts) {
            const retryOn = Array.isArray(retries.retryOn) ? retries.retryOn.join(", ") : retries.retryOn;
            rules.push(`Retries: ${retries.attempts}${retryOn ? ` (${retryOn})` : ""}`);
        }
        if (trafficPolicy.loadBalancer?.simple) {
            rules.push(`Load balancer: ${trafficPolicy.loadBalancer.simple}`);
        }
        if (trafficPolicy.connectionPool?.tcp?.maxConnections) {
            rules.push(`Max TCP connections: ${trafficPolicy.connectionPool.tcp.maxConnections}`);
        }
        if (trafficPolicy.outlierDetection) {
            rules.push("Outlier detection enabled");
        }
        if (route.corsPolicy) {
            rules.push("CORS policy applied");
        }
        if (route.fault) {
            rules.push("Fault injection active");
        }
        if (route.mirror) {
            rules.push("Mirroring traffic");
        }
        if (route.headers?.request || route.headers?.response) {
            rules.push("Header rewrite rules");
        }
        if (Array.isArray(route.match) && route.match.length) {
            rules.push("Match conditions defined");
        }
        if (route.weight) {
            rules.push(`Weight: ${route.weight}%`);
        }
        if (route.percent) {
            rules.push(`Percent: ${route.percent}%`);
        }
        if (!rules.length) {
            rules.push("No additional rules");
        }
        return rules;
    }

    function buildRouteGroups(graph, nodeIndex) {
        if (!graph) {
            return [];
        }
        const trafficEdges = (graph.edges ?? []).filter((edge) => edge.kind === "traffic");
        if (!trafficEdges.length) {
            return [];
        }
        const groups = new Map();
        trafficEdges.forEach((edge) => {
            const meta = edge.properties ?? {};
            const vs = meta.virtualService ?? {};
            const vsNamespace = vs.namespace ?? graph.namespace ?? "default";
            let groupKey;
            let label;
            if (vs.name) {
                groupKey = `${vsNamespace}/${vs.name}`;
                label = vs.name;
            } else {
                const hostDescriptor = meta.destinationHost ?? edge.target ?? "direct";
                groupKey = `direct:${vsNamespace}:${hostDescriptor}`;
                label = meta.destinationHost ? `Direct to ${meta.destinationHost}` : "Direct traffic";
            }
            const existing = groups.get(groupKey);
            const sourceNode = nodeIndex.get(edge.source);
            const targetNode = nodeIndex.get(edge.target);
            const edgeEntry = {
                id: edge.id,
                source: formatNodeSummary(sourceNode, edge.source, meta),
                target: formatNodeSummary(targetNode, edge.target, meta),
                rules: extractRouteRules(meta)
            };
            if (existing) {
                existing.edges.push(edgeEntry);
            } else {
                groups.set(groupKey, {
                    id: groupKey,
                    label,
                    namespace: vsNamespace,
                    edges: [edgeEntry]
                });
            }
        });
        return Array.from(groups.values()).map((group) => ({
            ...group,
            edges: group.edges.sort((a, b) => a.source.label.localeCompare(b.source.label) || a.target.label.localeCompare(b.target.label))
        })).sort((a, b) => a.label.localeCompare(b.label));
    }

    function App() {
        const [inputNamespace, setInputNamespace] = useState(defaultNamespace);
        const [namespace, setNamespace] = useState(defaultNamespace);
        const [view, setView] = useState(initialView);
        const [data, setData] = useState(null);
        const [status, setStatus] = useState(fetchImpl ? "idle" : "error");
        const [error, setError] = useState(fetchImpl ? null : "Fetch API is not available");
        const [selected, setSelected] = useState(null);
        const [searchTerm, setSearchTerm] = useState("");
        const [searchError, setSearchError] = useState("");
        const [focusNodeId, setFocusNodeId] = useState(null);
        const searchListId = "node-search-options";

        useEffect(() => {
            if (!fetchImpl || !namespace) {
                return undefined;
            }
            let cancelled = false;
            async function load() {
                try {
                    setStatus("loading");
                    setError(null);
                    setSelected(null);
                    const params = new URLSearchParams();
                    params.set("namespace", namespace);
                    const response = await fetchImpl(`/api/graph?${params.toString()}`);
                    if (!response.ok) {
                        const message = await response.text();
                        throw new Error(message || `Request failed with status ${response.status}`);
                    }
                    const payload = await response.json();
                    if (!cancelled) {
                        setData(payload);
                        setStatus("ready");
                        setFocusNodeId(null);
                        setSearchError("");
                    }
                } catch (err) {
                    if (!cancelled) {
                        setStatus("error");
                        setError(err?.message ?? String(err));
                    }
                }
            }
            load();
            return () => {
                cancelled = true;
            };
        }, [namespace, fetchImpl]);

        useEffect(() => {
            if (view !== "topology") {
                setSelected(null);
                setFocusNodeId(null);
                setSearchError("");
            }
        }, [view]);

        const applyView = (nextView) => {
            const resolved = nextView === "routes" ? "routes" : "topology";
            setView(resolved);
            if (typeof window !== "undefined") {
                const params = new URLSearchParams(window.location.search);
                if (resolved === "topology") {
                    params.delete("view");
                } else {
                    params.set("view", resolved);
                }
                const query = params.toString();
                const newUrl = query ? `${window.location.pathname}?${query}` : window.location.pathname;
                window.history.replaceState(null, "", newUrl);
            }
        };

        const layout = useMemo(() => view === "topology" && data ? computeLayout(data) : null, [data, view]);
        const searchOptions = useMemo(() => {
            if (!layout?.nodes?.length) {
                return [];
            }
            const mapped = layout.nodes.map((node) => ({
                id: node.id,
                label: node.label ?? node.name ?? node.id
            }));
            return mapped.sort((a, b) => a.label.localeCompare(b.label));
        }, [layout]);
        const nodeIndex = useMemo(() => {
            const index = new Map();
            if (data?.nodes) {
                data.nodes.forEach((node) => index.set(node.id, node));
            }
            return index;
        }, [data]);
        const routeGroups = useMemo(() => buildRouteGroups(data, nodeIndex), [data, nodeIndex]);

        const handleNamespaceSubmit = (event) => {
            event.preventDefault();
            const trimmed = (inputNamespace ?? "").trim();
            if (trimmed && trimmed !== namespace) {
                setNamespace(trimmed);
            }
        };

        const handleSelect = (item) => {
            setSelected(item);
            if (item?.type === "node" && item.data?.id) {
                setFocusNodeId(item.data.id);
            } else if (item) {
                setFocusNodeId(null);
            }
        };

        const handleSearchChange = (event) => {
            setSearchTerm(event.target.value);
            if (searchError) {
                setSearchError("");
            }
        };

        const handleSearchSubmit = (event) => {
            event.preventDefault();
            const term = (searchTerm ?? "").trim();
            if (!term || !layout?.nodes?.length) {
                return;
            }
            const lowerTerm = term.toLowerCase();
            const match = layout.nodes.find((node) => {
                const display = (node.label ?? node.name ?? node.id ?? "").toLowerCase();
                const identifier = (node.id ?? "").toLowerCase();
                return display.includes(lowerTerm) || identifier.includes(lowerTerm);
            });
            if (!match) {
                setSearchError("Node not found");
                return;
            }
            setSearchError("");
            const label = match.label ?? match.name ?? match.id;
            setSearchTerm(label);
            handleSelect({ type: "node", data: match });
        };

        const searchDisabled = view !== "topology" || status !== "ready" || !layout?.nodes?.length;

        const content = view === "routes"
            ? React.createElement(RoutesLayout, {
                status,
                error,
                routeGroups,
                warnings: data?.warnings ?? []
            })
            : React.createElement("main", { className: "layout" },
                React.createElement(TopologyDiagram, {
                    layout,
                    selected,
                    focusNodeId,
                    onSelect: handleSelect
                }),
                React.createElement("aside", { className: "details-panel" },
                    React.createElement("section", null,
                        React.createElement("h2", null, "Summary"),
                        React.createElement(Summary, { summary: layout?.summary })
                    ),
                    React.createElement("section", null,
                        React.createElement("h2", null, "Warnings"),
                        React.createElement(Warnings, { warnings: layout?.warnings })
                    ),
                    React.createElement("section", null,
                        React.createElement("h2", null, "Details"),
                        React.createElement(DetailsPanel, { selected })
                    )
                )
            );

        return React.createElement(Fragment, null,
            React.createElement("header", { className: "top-bar" },
                React.createElement("div", { className: "top-bar-left" },
                    React.createElement("h1", null, "Istio Route Explorer"),
                    React.createElement(ViewToggle, { view, onChange: applyView })
                ),
                React.createElement("div", { className: "controls" },
                    React.createElement("form", { className: "namespace-form", onSubmit: handleNamespaceSubmit },
                        React.createElement("input", {
                            type: "text",
                            value: inputNamespace,
                            onChange: (event) => setInputNamespace(event.target.value),
                            placeholder: "Namespace"
                        }),
                        React.createElement("button", { type: "submit" }, "Load")
                    ),
                    React.createElement("form", { className: "search-form", onSubmit: handleSearchSubmit },
                        React.createElement("div", { className: "search-controls" },
                            React.createElement("input", {
                                type: "search",
                                value: searchTerm,
                                onChange: handleSearchChange,
                                placeholder: "Search node",
                                list: searchListId,
                                disabled: searchDisabled
                            }),
                            React.createElement("button", {
                                type: "submit",
                                disabled: searchDisabled
                            }, "Find")
                        ),
                        searchError ? React.createElement("span", { className: "search-feedback" }, searchError) : null,
                        layout?.nodes?.length ? React.createElement("datalist", { id: searchListId },
                            searchOptions.map((option) => React.createElement("option", { key: option.id, value: option.label }))
                        ) : null
                    ),
                    React.createElement(StatusIndicator, { status, error, updatedAt: data?.generatedAt })
                )
            ),
            content
        );
    }

    return App;
}

























