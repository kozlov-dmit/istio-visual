import { computeLayout } from "./topology.js";

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
                React.createElement("span", { className: "legend-dot app" }),
                "Application"
            ),
            React.createElement("div", null,
                React.createElement("span", { className: "legend-dot sidecar" }),
                "Sidecar"
            ),
            React.createElement("div", null,
                React.createElement("span", { className: "legend-dot external" }),
                "External"
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
            return [];
        }
        const props = node.properties ?? {};
        const rows = [];
        rows.push(["Type", node.type]);
        if (props.displayName && props.displayName !== node.id) {
            rows.push(["Display name", props.displayName]);
        }
        if (props.namespace) {
            rows.push(["Namespace", props.namespace]);
        }
        if (props.pod) {
            rows.push(["Pod", props.pod]);
        }
        if (props.container) {
            rows.push(["Container", props.container]);
        }
        if (props.containerType) {
            rows.push(["Role", props.containerType === "sidecar" ? "Sidecar" : "Application"]);
        }
        if (props.image) {
            rows.push(["Image", props.image]);
        }
        if (props.host) {
            rows.push(["Host", props.host]);
        }
        if (props.labels && Object.keys(props.labels).length > 0) {
            rows.push(["Labels", Object.entries(props.labels).map(([k, v]) => `${k}=${v}`).join(", ")]);
        }
        if (props.serviceAccount) {
            rows.push(["Service account", props.serviceAccount]);
        }
        return rows;
    }

    function describeEdge(edge) {
        if (!edge) {
            return [];
        }
        const props = edge.properties ?? {};
        const rows = [];
        rows.push(["Type", edge.kind]);
        rows.push(["Source", edge.sourceId ?? edge.source?.id ?? "unknown"]);
        rows.push(["Target", edge.targetId ?? edge.target?.id ?? "unknown"]);
        if (props.destinationHost) {
            rows.push(["Destination host", props.destinationHost]);
        }
        if (props.destinationNamespace) {
            rows.push(["Destination namespace", props.destinationNamespace]);
        }
        if (props.route?.timeout) {
            rows.push(["Timeout", props.route.timeout]);
        }
        const retries = props.route?.retries ?? props.route?.retryPolicy;
        if (retries) {
            rows.push(["Retries", JSON.stringify(retries)]);
        }
        if (props.trafficPolicy) {
            rows.push(["Traffic policy", JSON.stringify(props.trafficPolicy)]);
        }
        if (props.requestTimeout) {
            rows.push(["Request timeout", props.requestTimeout]);
        }
        if (!props.destinationHost && !props.destinationNamespace && !props.route && !props.trafficPolicy && !props.requestTimeout) {
            rows.push(["Info", "No additional metadata"]);
        }
        return rows;
    }

    function DetailsPanel({ selected }) {
        if (!selected) {
            return React.createElement("pre", { className: "details" }, "Select a node or edge to inspect");
        }
        if (selected.type === "node") {
            const rows = describeNodeDetails(selected.data);
            return React.createElement(
                "div",
                { className: "details" },
                React.createElement("h3", null, selected.data.properties?.displayName ?? selected.data.id),
                React.createElement("div", { className: "details-grid" },
                    rows.map(([label, value], idx) =>
                        React.createElement(React.Fragment, { key: `${label}-${idx}` },
                            React.createElement("span", { className: "details-label" }, label),
                            React.createElement("span", { className: "details-value" }, String(value))
                        )
                    )
                )
            );
        }
        const rows = describeEdge(selected.data);
        return React.createElement(
            "div",
            { className: "details" },
            React.createElement("h3", null, "Traffic Edge"),
            React.createElement("div", { className: "details-grid" },
                rows.map(([label, value], idx) =>
                    React.createElement(React.Fragment, { key: `${label}-${idx}` },
                        React.createElement("span", { className: "details-label" }, label),
                        React.createElement("span", { className: "details-value" }, String(value))
                    )
                )
            )
        );
    }

    function TopologyDiagram({ layout, selected, focusNodeId, onSelect }) {
        if (!layout || !layout.nodes.length) {
            return React.createElement("div", { className: "graph-panel" },
                React.createElement("div", { className: "empty-state" }, "No topology data")
            );
        }

        const bounds = layout.bounds;
        const { minX, minY, maxX, maxY } = bounds;
        const baseWidth = Math.max(200, maxX - minX);
        const baseHeight = Math.max(200, maxY - minY);
        const baseViewBox = useMemo(() => ({
            minX,
            minY,
            width: baseWidth,
            height: baseHeight
        }), [minX, minY, baseWidth, baseHeight]);

        const [viewBoxState, setViewBoxState] = useState({ ...baseViewBox });
        const [isPanning, setIsPanning] = useState(false);
        const svgRef = useRef(null);
        const isPanningRef = useRef(false);
        const lastPointRef = useRef({ x: 0, y: 0 });

        useEffect(() => {
            setViewBoxState({ ...baseViewBox });
        }, [baseViewBox]);

        useEffect(() => {
            if (!focusNodeId || !layout?.nodes?.length) {
                return;
            }
            const node = layout.nodes.find((item) => item.id === focusNodeId);
            if (!node) {
                return;
            }
            setViewBoxState((prev) => {
                const scale = prev.width / baseViewBox.width;
                const width = baseViewBox.width * scale;
                const height = baseViewBox.height * scale;
                return {
                    minX: node.x - width / 2,
                    minY: node.y - height / 2,
                    width,
                    height
                };
            });
        }, [focusNodeId, layout, baseViewBox]);

        const minScale = 0.08;
        const maxScale = 6.5;

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

        const podBoxes = layout.groups
            .filter((group) => group.type === "pod" && group.box)
            .map((group) => React.createElement("g", { key: group.key },
                React.createElement("rect", {
                    x: group.box.x,
                    y: group.box.y,
                    width: group.box.width,
                    height: group.box.height,
                    fill: "rgba(30, 64, 175, 0.08)",
                    stroke: "rgba(59, 130, 246, 0.35)",
                    strokeDasharray: "6,6",
                    strokeWidth: 1.2
                }),
                React.createElement("text", {
                    x: group.box.x + 4,
                    y: group.box.y - 12,
                    fontSize: 12,
                    fill: "#94a3b8"
                }, group.label)
            ));

        const edgeElements = layout.edges.map((edge) => React.createElement("line", {
            key: edge.id,
            x1: edge.source.x,
            y1: edge.source.y,
            x2: edge.target.x,
            y2: edge.target.y,
            stroke: edge.kind === "podLink" ? "rgba(148,163,184,0.25)" : "rgba(148,163,184,0.45)",
            strokeWidth: edge.kind === "podLink" ? 0.35 : 0.75,
            onClick: () => onSelect({ type: "edge", data: edge.data }),
            role: "button",
            tabIndex: 0,
            "aria-label": `Edge ${edge.id}`,
            style: { cursor: "pointer" },
            "data-interactive": "true"
        }));

        const nodeElements = layout.nodes.map((node) => React.createElement("g", { key: node.id },
            React.createElement("circle", {
                cx: node.x,
                cy: node.y,
                r: node.radius,
                fill: node.color,
                stroke: selected?.data?.id === node.id ? "#fbbf24" : "#0f172a",
                strokeWidth: 2,
                onClick: () => onSelect({ type: "node", data: node.data }),
                role: "button",
                tabIndex: 0,
                "aria-label": node.displayName,
                style: { cursor: "pointer" },
                "data-node-id": node.id,
                "data-interactive": "true"
            }),
            React.createElement("text", {
                x: node.x + node.radius + 10,
                y: node.y,
                fill: "#e2e8f0",
                fontSize: 11,
                dominantBaseline: "middle"
            }, node.displayName)
        ));

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
                podBoxes,
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
            const mapped = layout.nodes.map((node) => {
                const label = node.displayName ?? node.data?.id ?? node.id;
                return {
                    id: node.id,
                    label
                };
            });
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
                const display = (node.displayName ?? node.data?.id ?? node.id ?? "").toLowerCase();
                const identifier = (node.id ?? "").toLowerCase();
                return display.includes(lowerTerm) || identifier.includes(lowerTerm);
            });
            if (!match) {
                setSearchError("Node not found");
                return;
            }
            setSearchError("");
            const label = match.displayName ?? match.data?.id ?? match.id;
            setSearchTerm(label);
            handleSelect({ type: "node", data: match.data });
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

























