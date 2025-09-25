import { computeLayout } from "./topology.js";

export function createApp(React, options = {}) {
    const { useEffect, useMemo, useState } = React;
    const fetchImpl = options.fetchImpl ?? (typeof fetch !== "undefined" ? fetch.bind(window) : null);
    const defaultNamespace = options.defaultNamespace ?? "default";

    function Legend() {
        return React.createElement(
            "div",
            { className: "legend" },
            React.createElement("div", null,
                React.createElement("span", { className: "legend-dot app" }),
                "Application container"
            ),
            React.createElement("div", null,
                React.createElement("span", { className: "legend-dot sidecar" }),
                "Sidecar container"
            ),
            React.createElement("div", null,
                React.createElement("span", { className: "legend-dot external" }),
                "External service"
            )
        );
    }

    function Warnings({ warnings }) {
        if (!warnings?.length) {
            return React.createElement("ul", { className: "warnings" },
                React.createElement("li", null, "No warnings")
            );
        }
        return React.createElement("ul", { className: "warnings" }, warnings.map((w, idx) =>
            React.createElement("li", { key: idx }, w)
        ));
    }

    function Summary({ summary }) {
        if (!summary) {
            return React.createElement("div", { className: "summary" }, "n/a");
        }
        const entries = Object.entries(summary);
        return React.createElement(
            "div",
            { className: "summary" },
            entries.map(([key, value]) =>
                React.createElement(
                    "div",
                    { key },
                    React.createElement("span", null, key),
                    React.createElement("strong", null, String(value))
                )
            )
        );
    }

    function TopologyDiagram({ layout, selectedNodeId, selectedEdgeId, onSelect }) {
        if (!layout || !layout.nodes.length) {
            return React.createElement("div", { className: "graph-panel" },
                React.createElement("div", { className: "empty-state" }, "No topology data")
            );
        }

        const bounds = layout.bounds;
        const width = Math.max(200, bounds.maxX - bounds.minX);
        const height = Math.max(200, bounds.maxY - bounds.minY);
        const viewBox = `${bounds.minX} ${bounds.minY} ${width} ${height}`;

        const groupBoxes = layout.groups
            .filter((group) => group.type === "pod" && group.box)
            .map((group) =>
                React.createElement(
                    "g",
                    { key: group.key },
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
                        fill: "#94a3b8",
                        fontSize: 12
                    }, group.label)
                )
            );

        const edges = layout.edges.map((edge) => {
            const isSelected = edge.id === selectedEdgeId;
            return React.createElement("line", {
                key: edge.id,
                x1: edge.source.x,
                y1: edge.source.y,
                x2: edge.target.x,
                y2: edge.target.y,
                stroke: isSelected ? "#fbbf24" : edge.kind === "podLink" ? "rgba(148,163,184,0.35)" : "rgba(148,163,184,0.6)",
                strokeWidth: edge.kind === "podLink" ? 1.2 : isSelected ? 2.6 : 1.8,
                onClick: () => onSelect({ type: "edge", data: edge.data }),
                role: "button",
                "aria-label": `Edge ${edge.id}`,
                tabIndex: 0,
                style: { cursor: "pointer" }
            });
        });

        const nodes = layout.nodes.map((node) => {
            const isSelected = node.id === selectedNodeId;
            return React.createElement(
                "g",
                { key: node.id },
                React.createElement("circle", {
                    cx: node.x,
                    cy: node.y,
                    r: node.radius,
                    fill: node.color,
                    stroke: isSelected ? "#fbbf24" : "#0f172a",
                    strokeWidth: 2,
                    onClick: () => onSelect({ type: "node", data: node.data }),
                    role: "button",
                    tabIndex: 0,
                    "aria-label": node.displayName,
                    style: { cursor: "pointer" },
                    "data-node-id": node.id
                }),
                React.createElement("text", {
                    x: node.x + node.radius + 8,
                    y: node.y,
                    fill: "#cbd5f5",
                    fontSize: 11,
                    dominantBaseline: "middle"
                }, node.displayName)
            );
        });

        return React.createElement(
            "div",
            { className: "graph-panel" },
            React.createElement(
                "svg",
                { className: "graph-svg", viewBox },
                groupBoxes,
                edges,
                nodes
            ),
            React.createElement(Legend, null)
        );
    }

    function App() {
        const [inputNamespace, setInputNamespace] = useState(defaultNamespace);
        const [namespace, setNamespace] = useState(defaultNamespace);
        const [data, setData] = useState(null);
        const [error, setError] = useState(null);
        const [status, setStatus] = useState("idle");
        const [selected, setSelected] = useState(null);

        useEffect(() => {
            let cancelled = false;
            if (!fetchImpl || !namespace) {
                return;
            }
            async function load() {
                try {
                    setStatus("loading");
                    setError(null);
                    const params = new URLSearchParams();
                    params.set("namespace", namespace);
                    const response = await fetchImpl(`/api/graph?${params.toString()}`);
                    if (!response.ok) {
                        const message = await response.text();
                        throw new Error(message || `Request failed with status ${response.status}`);
                    }
                    const json = await response.json();
                    if (!cancelled) {
                        setData(json);
                        setStatus("success");
                        if (typeof history !== "undefined") {
                            history.replaceState({}, "", `?namespace=${encodeURIComponent(namespace)}`);
                        }
                    }
                } catch (err) {
                    if (!cancelled) {
                        setError(err.message ?? String(err));
                        setStatus("error");
                    }
                }
            }
            load();
            return () => {
                cancelled = true;
            };
        }, [namespace, fetchImpl]);

        const layout = useMemo(() => (data ? enrichLayout(computeLayout(data)) : null), [data]);

        const statusText = status === "loading"
            ? "Loading..."
            : status === "error"
                ? `Error: ${error}`
                : status === "success"
                    ? `Updated ${new Date(data?.generatedAt ?? Date.now()).toLocaleTimeString()}`
                    : 'Enter namespace to begin';

        return React.createElement(
            "div",
            { className: "app-root" },
            React.createElement(
                "header",
                { className: "top-bar" },
                React.createElement("h1", null, "Istio Route Explorer"),
                React.createElement(
                    "div",
                    { className: "controls" },
                    React.createElement("label", { htmlFor: "namespace" }, "Namespace"),
                    React.createElement("input", {
                        id: "namespace",
                        name: "namespace",
                        value: inputNamespace,
                        onChange: (event) => setInputNamespace(event.target.value),
                        placeholder: defaultNamespace
                    }),
                    React.createElement("button", {
                        onClick: () => { if (inputNamespace.trim()) { setNamespace(inputNamespace.trim()); setSelected(null); } }
                    }, "Load"),
                    React.createElement("button", {
                        onClick: () => { if (namespace) { setNamespace(namespace); } }
                    }, "Refresh"),
                    React.createElement("span", { className: "status", "data-state": status }, statusText)
                )
            ),
            React.createElement(
                "main",
                { className: "layout" },
                React.createElement(TopologyDiagram, {
                    layout,
                    selectedNodeId: selected?.type === "node" ? selected.data.id : null,
                    selectedEdgeId: selected?.type === "edge" ? selected.data.id : null,
                    onSelect: setSelected
                }),
                React.createElement(
                    "aside",
                    { className: "details-panel" },
                    React.createElement(
                        "section",
                        null,
                        React.createElement("h2", null, "Selection"),
                        React.createElement("pre", { className: "details" }, formatSelection(selected))
                    ),
                    React.createElement(
                        "section",
                        null,
                        React.createElement("h2", null, "Summary"),
                        React.createElement(Summary, { summary: data?.summary })
                    ),
                    React.createElement(
                        "section",
                        null,
                        React.createElement("h2", null, "Warnings"),
                        React.createElement(Warnings, { warnings: data?.warnings })
                    )
                )
            )
        );
    }

    return App;
}

function formatSelection(selected) {
    if (!selected) {
        return 'Click a node or edge to inspect';
    }
    const printable = clean(selected.data);
    if (selected.type === 'node') {
        return `Node ${selected.data.id}\nType: ${selected.data.type}\n\n${JSON.stringify(printable, null, 2)}`;
    }
    return `Edge ${selected.data.id}\nType: ${selected.data.kind}\n\n${JSON.stringify(printable, null, 2)}`;
}

function clean(value) {
    if (Array.isArray(value)) {
        return value.map(clean);
    }
    if (value && typeof value === 'object') {
        const result = {};
        Object.entries(value).forEach(([key, val]) => {
            if (val === null || val === undefined || val === '') {
                return;
            }
            result[key] = clean(val);
        });
        return result;
    }
    return value;
}

function enrichLayout(layout) {
    const colors = {
        sidecarContainer: 'var(--sidecar)',
        externalService: 'var(--external)',
        default: 'var(--app)'
    };
    layout.nodes.forEach((node) => {
        if (node.type === 'sidecarContainer') {
            node.color = colors.sidecarContainer;
        } else if (node.type === 'externalService') {
            node.color = colors.externalService;
        } else {
            node.color = colors.default;
        }
    });
    return layout;
}
