const GRID_COLUMN_WIDTH = 240;
const GRID_ROW_HEIGHT = 180;
const HORIZONTAL_MARGIN = 120;
const VERTICAL_MARGIN = 100;
const MIN_WIDTH = 960;
const MIN_HEIGHT = 640;
const SERVICE_RADIUS = 26;
const EXTERNAL_RADIUS = 24;
const EDGE_CURVE_FACTOR = 0.35;

export function computeLayout(graph) {
    const context = buildServiceGraph(graph ?? {});
    const { width, height } = assignPositions(context.nodes);
    const bounds = computeBounds(context.nodes, width, height);
    return {
        nodes: context.nodes,
        edges: context.edges,
        width,
        height,
        bounds,
        summary: context.summary,
        warnings: Array.isArray(graph?.warnings) ? graph.warnings : []
    };
}

function buildServiceGraph(graph) {
    const baseNodes = Array.isArray(graph.nodes) ? graph.nodes : [];
    const baseEdges = Array.isArray(graph.edges) ? graph.edges : [];
    const defaultNamespace = typeof graph.namespace === "string" ? graph.namespace : null;

    const nodeById = new Map();
    baseNodes.forEach((node) => {
        if (node && node.id) {
            nodeById.set(node.id, node);
        }
    });

    const services = new Map();
    const edgeAggregates = new Map();

    baseEdges.filter((edge) => edge && edge.kind === "traffic").forEach((edge) => {
        const sourceId = edge.sourceId ?? edge.source;
        const targetId = edge.targetId ?? edge.target;
        const sourceNode = nodeById.get(sourceId);
        const targetNode = nodeById.get(targetId);
        const props = edge.properties ?? {};

        const destinationHost = extractDestinationHost(props) ?? extractNodeHost(targetNode);
        const destinationNamespace = props.destinationNamespace ?? getNamespace(targetNode) ?? defaultNamespace;
        const destInfo = parseServiceReference(destinationHost, destinationNamespace, defaultNamespace);
        const destService = ensureService(services, destInfo);
        captureAssociation(destService, targetNode);

        const sourceHosts = extractSourceHosts(props, sourceNode);
        const sourceNamespace = getNamespace(sourceNode) ?? defaultNamespace;
        const effectiveHosts = sourceHosts.length ? sourceHosts : [sourceId ?? destInfo.name];

        effectiveHosts.forEach((host) => {
            const srcInfo = parseServiceReference(host, sourceNamespace, defaultNamespace);
            const sourceService = ensureService(services, srcInfo);
            captureAssociation(sourceService, sourceNode);

            sourceService.outbound.add(destService.id);
            destService.inbound.add(sourceService.id);

            const edgeKey = `${sourceService.id}->${destService.id}`;
            let aggregate = edgeAggregates.get(edgeKey);
            if (!aggregate) {
                aggregate = createEdgeAggregate(sourceService.id, destService.id);
                edgeAggregates.set(edgeKey, aggregate);
            }
            aggregate.count += 1;
            const vsLabel = formatVirtualService(props.virtualService);
            if (vsLabel) {
                aggregate.virtualServices.add(vsLabel);
            }
            const snapshot = extractRouteSnapshot(props);
            aggregate.snapshots.set(snapshot.key, snapshot.summary);
        });
    });

    const nodes = Array.from(services.values()).map((service) => {
        const inbound = Array.from(service.inbound).sort();
        const outbound = Array.from(service.outbound).sort();
        const pods = Array.from(service.pods).sort();
        const containerIds = Array.from(service.containers).sort();
        return {
            id: service.id,
            type: service.kind === "external" ? "externalService" : service.kind,
            label: service.label,
            name: service.name,
            namespace: service.namespace,
            host: service.host,
            external: service.external,
            pods,
            containerIds,
            inbound,
            outbound,
            metrics: {
                inbound: inbound.length,
                outbound: outbound.length,
                podCount: pods.length
            },
            radius: service.external ? EXTERNAL_RADIUS : SERVICE_RADIUS,
            color: service.external ? "var(--external)" : (service.kind === "mesh" ? "var(--mesh-node, #22d3ee)" : "var(--service-node, #38bdf8)")
        };
    }).sort((a, b) => {
        if (a.external !== b.external) {
            return a.external ? 1 : -1;
        }
        if ((a.namespace ?? "") !== (b.namespace ?? "")) {
            return (a.namespace ?? "").localeCompare(b.namespace ?? "");
        }
        return a.label.localeCompare(b.label);
    });

    const edges = Array.from(edgeAggregates.values()).map((aggregate, index) => ({
        id: aggregate.id || `edge:${index}`,
        source: aggregate.source,
        target: aggregate.target,
        kind: "traffic",
        properties: {
            connectionCount: aggregate.count,
            virtualServices: Array.from(aggregate.virtualServices).sort(),
            routeSummaries: Array.from(aggregate.snapshots.values()).slice(0, 8)
        }
    }));

    const nodeIndex = new Map(nodes.map((node) => [node.id, node]));
    edges.forEach((edge) => {
        const source = nodeIndex.get(edge.source);
        const target = nodeIndex.get(edge.target);
        if (source) {
            edge.sourceLabel = source.label;
            edge.sourceNamespace = source.namespace;
        }
        if (target) {
            edge.targetLabel = target.label;
            edge.targetNamespace = target.namespace;
        }
    });

    const summary = {
        ...(graph.summary ?? {}),
        services: nodes.filter((node) => !node.external).length,
        externalServices: nodes.filter((node) => node.external).length
    };

    return { nodes, edges, summary };
}

function assignPositions(nodes) {
    if (!nodes.length) {
        return { width: MIN_WIDTH, height: MIN_HEIGHT };
    }
    const columns = Math.max(1, Math.ceil(Math.sqrt(nodes.length)));
    const rows = Math.max(1, Math.ceil(nodes.length / columns));
    const width = Math.max(MIN_WIDTH, HORIZONTAL_MARGIN * 2 + (columns - 1) * GRID_COLUMN_WIDTH);
    const height = Math.max(MIN_HEIGHT, VERTICAL_MARGIN * 2 + (rows - 1) * GRID_ROW_HEIGHT);

    nodes.forEach((node, index) => {
        const row = Math.floor(index / columns);
        const column = index % columns;
        node.x = HORIZONTAL_MARGIN + column * GRID_COLUMN_WIDTH;
        node.y = VERTICAL_MARGIN + row * GRID_ROW_HEIGHT;
    });

    return { width, height };
}

function computeBounds(nodes, width, height) {
    if (!nodes.length) {
        return { minX: 0, minY: 0, maxX: width, maxY: height };
    }
    let minX = Infinity;
    let maxX = -Infinity;
    let minY = Infinity;
    let maxY = -Infinity;
    const padding = 70;
    nodes.forEach((node) => {
        minX = Math.min(minX, node.x - node.radius - padding);
        maxX = Math.max(maxX, node.x + node.radius + padding);
        minY = Math.min(minY, node.y - node.radius - padding);
        maxY = Math.max(maxY, node.y + node.radius + padding);
    });
    return {
        minX: Math.min(minX, 0),
        minY: Math.min(minY, 0),
        maxX: Math.max(maxX, width),
        maxY: Math.max(maxY, height)
    };
}

function extractDestinationHost(props) {
    if (!props) {
        return null;
    }
    if (typeof props.destinationHost === "string" && props.destinationHost.trim()) {
        return props.destinationHost.trim();
    }
    const destination = props.destination ?? props.dest;
    if (destination && typeof destination.host === "string") {
        return destination.host.trim();
    }
    return null;
}

function extractNodeHost(node) {
    if (!node) {
        return null;
    }
    const props = node.properties ?? {};
    if (typeof props.host === "string") {
        return props.host.trim();
    }
    return null;
}

function getNamespace(node) {
    if (!node) {
        return null;
    }
    if (typeof node.namespace === "string" && node.namespace.trim()) {
        return node.namespace.trim();
    }
    const props = node.properties ?? {};
    if (typeof props.namespace === "string" && props.namespace.trim()) {
        return props.namespace.trim();
    }
    const metadata = props.metadata ?? {};
    if (typeof metadata.namespace === "string" && metadata.namespace.trim()) {
        return metadata.namespace.trim();
    }
    return null;
}

function extractSourceHosts(props, sourceNode) {
    const result = new Set();
    if (props) {
        const metaHosts = props.sourceHosts;
        if (Array.isArray(metaHosts)) {
            metaHosts.forEach((value) => {
                if (typeof value === "string" && value.trim()) {
                    result.add(value.trim());
                }
            });
        }
        const vs = props.virtualService;
        if (vs) {
            if (Array.isArray(vs.hosts)) {
                vs.hosts.forEach((value) => {
                    if (typeof value === "string" && value.trim()) {
                        result.add(value.trim());
                    }
                });
            }
            if (typeof vs.host === "string" && vs.host.trim()) {
                result.add(vs.host.trim());
            }
            if (typeof vs.name === "string" && vs.name.trim()) {
                result.add(vs.name.trim());
            }
        }
    }
    if (!result.size && sourceNode) {
        const nodeProps = sourceNode.properties ?? {};
        if (typeof nodeProps.service === "string" && nodeProps.service.trim()) {
            result.add(nodeProps.service.trim());
        }
        if (typeof nodeProps.app === "string" && nodeProps.app.trim()) {
            result.add(nodeProps.app.trim());
        }
        if (typeof nodeProps.pod === "string" && nodeProps.pod.trim()) {
            result.add(nodeProps.pod.trim());
        }
        if (typeof nodeProps.displayName === "string" && nodeProps.displayName.trim()) {
            result.add(nodeProps.displayName.trim());
        }
    }
    return Array.from(result);
}

function parseServiceReference(rawHost, explicitNamespace, defaultNamespace) {
    const trimmed = typeof rawHost === "string" ? rawHost.trim() : "";
    const normalizedNamespace = typeof explicitNamespace === "string" ? explicitNamespace : null;
    const fallbackNamespace = normalizedNamespace && normalizedNamespace !== "external"
        ? normalizedNamespace
        : (defaultNamespace ?? "");

    if (!trimmed) {
        if (fallbackNamespace) {
            const unknownName = "(unknown)";
            return {
                id: `service:${fallbackNamespace}/${unknownName}`,
                name: unknownName,
                label: "Unknown",
                namespace: fallbackNamespace,
                host: "",
                kind: "service",
                external: false
            };
        }
        return {
            id: "external:unknown",
            name: "Unknown",
            label: "Unknown",
            namespace: "",
            host: "",
            kind: "external",
            external: true
        };
    }

    const lower = trimmed.toLowerCase();
    if (lower === "mesh") {
        return {
            id: "mesh-gateway",
            name: "mesh",
            label: "mesh gateway",
            namespace: "",
            host: trimmed,
            kind: "mesh",
            external: false
        };
    }

    const externalHint = (normalizedNamespace ?? "").toLowerCase() === "external";
    const internalPattern = lower.endsWith(".svc.cluster.local") || lower.endsWith(".svc") || lower.includes(".svc.");
    const containsDot = trimmed.includes(".");
    const treatAsExternal = externalHint || (containsDot && !internalPattern);

    if (!treatAsExternal) {
        const resolved = resolveInternalService(trimmed, fallbackNamespace);
        if (resolved.namespace) {
            const id = `service:${resolved.namespace}/${resolved.name}`;
            return {
                id,
                name: resolved.name,
                label: resolved.displayName,
                namespace: resolved.namespace,
                host: resolved.host,
                kind: "service",
                external: false
            };
        }
    }

    const externalId = `external:${lower}`;
    return {
        id: externalId,
        name: trimmed,
        label: trimmed,
        namespace: "",
        host: trimmed,
        kind: "external",
        external: true
    };
}

function resolveInternalService(host, fallbackNamespace) {
    const lower = host.toLowerCase();
    const svcCluster = ".svc.cluster.local";
    let name;
    let namespace;
    if (lower.endsWith(svcCluster)) {
        const prefix = lower.slice(0, -svcCluster.length);
        const parts = prefix.split(".");
        name = parts[0];
        namespace = parts[1] ?? fallbackNamespace ?? "";
    } else if (lower.endsWith(".svc")) {
        const prefix = lower.slice(0, -4);
        const parts = prefix.split(".");
        name = parts[0];
        namespace = parts[1] ?? fallbackNamespace ?? "";
    } else if (lower.includes(".svc.")) {
        const idx = lower.indexOf(".svc.");
        const prefix = lower.substring(0, idx);
        const parts = prefix.split(".");
        name = parts[0];
        namespace = parts[1] ?? fallbackNamespace ?? "";
    } else {
        const segments = lower.split(".");
        if (segments.length === 2) {
            name = segments[0];
            namespace = segments[1];
        } else {
            name = lower;
            namespace = fallbackNamespace ?? "";
        }
    }
    if (!namespace) {
        return { name, namespace: "", host: host, displayName: host };
    }
    const displayName = name;
    const canonicalHost = `${name}.${namespace}.svc.cluster.local`;
    return { name, namespace, host: canonicalHost, displayName };
}

function ensureService(map, info) {
    let service = map.get(info.id);
    if (!service) {
        service = {
            id: info.id,
            name: info.name,
            label: info.label,
            namespace: info.namespace,
            host: info.host,
            kind: info.kind,
            external: info.external,
            pods: new Set(),
            containers: new Set(),
            inbound: new Set(),
            outbound: new Set()
        };
        map.set(info.id, service);
    }
    return service;
}

function captureAssociation(service, node) {
    if (!service || !node) {
        return;
    }
    const props = node.properties ?? {};
    if (typeof node.id === "string" && node.id) {
        service.containers.add(node.id);
    }
    if (typeof props.pod === "string" && props.pod.trim()) {
        const podNamespace = props.namespace ?? service.namespace;
        const label = podNamespace ? `${props.pod.trim()}.${podNamespace}` : props.pod.trim();
        service.pods.add(label);
    }
}

function createEdgeAggregate(sourceId, targetId) {
    return {
        id: `edge:${sourceId}->${targetId}`,
        source: sourceId,
        target: targetId,
        count: 0,
        virtualServices: new Set(),
        snapshots: new Map()
    };
}

function formatVirtualService(virtualService) {
    if (!virtualService || typeof virtualService !== "object") {
        return null;
    }
    const name = typeof virtualService.name === "string" ? virtualService.name.trim() : "";
    const namespace = typeof virtualService.namespace === "string" ? virtualService.namespace.trim() : "";
    if (!name) {
        return null;
    }
    return namespace ? `${namespace}/${name}` : name;
}

function extractRouteSnapshot(props) {
    const snapshot = {};
    if (typeof props.subset === "string" && props.subset.trim()) {
        snapshot.subset = props.subset.trim();
    }
    if (typeof props.requestTimeout === "string" && props.requestTimeout.trim()) {
        snapshot.requestTimeout = props.requestTimeout.trim();
    }
    if (props.trafficPolicy) {
        snapshot.trafficPolicy = props.trafficPolicy;
    }
    const route = props.route ?? {};
    if (typeof route.timeout === "string" && route.timeout.trim()) {
        snapshot.timeout = route.timeout.trim();
    }
    if (typeof route.weight === "number") {
        snapshot.weight = route.weight;
    }
    if (route.retries) {
        snapshot.retries = route.retries;
    }
    if (route.match) {
        snapshot.match = route.match;
    }
    if (route.fault) {
        snapshot.fault = route.fault;
    }
    if (route.corsPolicy) {
        snapshot.cors = route.corsPolicy;
    }
    if (route.headers) {
        snapshot.headers = route.headers;
    }
    if (route.mirror) {
        snapshot.mirror = route.mirror;
    }
    const key = JSON.stringify(snapshot);
    return { key, summary: snapshot };
}

export function edgePath(source, target) {
    const dx = target.x - source.x;
    const dy = target.y - source.y;
    const control1X = source.x + dx * EDGE_CURVE_FACTOR;
    const control1Y = source.y;
    const control2X = target.x - dx * EDGE_CURVE_FACTOR;
    const control2Y = target.y;
    return `M ${source.x} ${source.y} C ${control1X} ${control1Y}, ${control2X} ${control2Y}, ${target.x} ${target.y}`;
}
 