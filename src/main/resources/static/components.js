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

    function NodeBadge({ node }) {
        if (node.type === "externalService") {
            return React.createElement("span", { className: "badge external" }, "External");
        }
        if (node.type === "sidecarContainer") {
            return React.createElement("span", { className: "badge sidecar" }, "Sidecar");
        }
        return React.createElement("span", { className: "badge app" }, "App");
    }

    function describeNode(node) {
        if (!node) {
            return [];
        }
        const props = node.properties || {};
        const entries = [];
        if (props.pod) {
            entries.push(["Pod", `${props.pod} (${props.namespace})`]);
        }
        if (props.container) {
            entries.push(["Container", props.container]);
        }
        if (props.containerType) {
            entries.push(["Role", props.containerType === "sidecar" ? "Sidecar" : "Application"]);
        }
        if (props.image) {
            entries.push(["Image", props.image]);
        }
        if (props.host) {
            entries.push(["Host", props.host]);
        }
        if (props.labels && Object.keys(props.labels).length) {
            entries.push(["Labels", Object.entries(props.labels).map(([k, v]) => `${k}=${v}`).join(", ")]);
        }
        return entries;
    }

    function describeEdge(edge) {
        if (!edge) {
            return [];
        }
        const props = edge.properties || {};
        const entries = [];
        if (props.destinationHost) {
            entries.push(["Destination", props.destinationHost]);
        }
        if (props.destinationNamespace) {
            entries.push(["Namespace", props.destinationNamespace]);
        }
        if (props.route?.timeout) {
            entries.push(["Timeout", props.route.timeout]);
        }
        if (props.route?.retryPolicy) {
            entries.push(["Retry policy", JSON.stringify(props.route.retryPolicy)]);
        }
        if (props.trafficPolicy) {
            entries.push(["Traffic policy", JSON.stringify(props.trafficPolicy)]);
        }
        if (!entries.length) {
            entries.push(["Info", "No additional metadata"]);
        }
        return entries;
    }

    function DetailsPanel({ selected }) {
        if (!selected) {
            return React.createElement("pre", { className: "details" }, "Click a node or edge to inspect");
        }
        if (selected.type === "node") {
            const entries = describeNode(selected.data);
            return React.createElement(
                "div",
                { className: "details" },
                React.createElement("h3", null, selected.data.properties?.displayName ?? selected.data.id),
                React.createElement("div", { className: "details-grid" },
                    entries.map(([label, value]) =>
                        React.createElement("div", { key: label, className: "details-row" },
                            React.createElement("span", { className: "details-label" }, label),
                            React.createElement("span", { className: "details-value" }, value)
                        )
                    )
                )
            );
        }
        const entries = describeEdge(selected.data);
        return React.createElement(
            "div",
            { className: "details" },
            React.createElement("h3", null, "Traffic Edge"),
            React.createElement("div", { className: "details-grid" },
                entries.map(([label, value]) =>
                    React.createElement("div", { key: label, className: "details-row" },
                        React.createElement("span", { className: "details-label" }, label),
                        React.createElement("span", { className: "details-value" }, value)
                    )
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
                React.createElement(NodeBadge, { node }),
                React.createElement("text", {
                    x: node.x + node.radius + 10,
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
[... omitted 83 of 495 lines ...]
