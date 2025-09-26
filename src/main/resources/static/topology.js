const POD_HORIZONTAL_GAP = 18;
const POD_VERTICAL_GAP = 20;
const LAYER_MARGIN_X = 80;
const LAYER_MARGIN_Y = 60;
const WIDTH_PER_LAYER = 180;
const HEIGHT_PER_GROUP = 140;
const MIN_CANVAS = 320;

const NODE_RADIUS_APP = 14;
const NODE_RADIUS_SIDECAR = 12;
const NODE_RADIUS_EXTERNAL = 16;

export function computeLayout(graph) {
    const nodes = (graph?.nodes ?? []).map(normalizeNode);
    const nodeMap = new Map(nodes.map((node) => [node.id, node]));
    const groupsMap = new Map();
    nodes.forEach((node) => {
        const key = node.groupKey;
        if (!groupsMap.has(key)) {
            groupsMap.set(key, {
                key,
                type: node.groupType,
                label: node.groupLabel,
                namespace: node.namespace,
                nodes: []
            });
        }
        groupsMap.get(key).nodes.push(node);
    });

    const edges = (graph?.edges ?? []).map((edge) => normalizeEdge(edge, nodeMap));
    const adjacency = new Map();
    const indegree = new Map();
    groupsMap.forEach((_, key) => {
        adjacency.set(key, new Set());
        indegree.set(key, 0);
    });

    edges.forEach((edge) => {
        if (edge.kind !== 'traffic') {
            return;
        }
        const sourceGroup = nodeMap.get(edge.sourceId)?.groupKey;
        const targetGroup = nodeMap.get(edge.targetId)?.groupKey;
        if (!sourceGroup || !targetGroup || sourceGroup === targetGroup) {
            return;
        }
        const neighbours = adjacency.get(sourceGroup);
        if (!neighbours.has(targetGroup)) {
            neighbours.add(targetGroup);
            indegree.set(targetGroup, indegree.get(targetGroup) + 1);
        }
    });

    const queue = [];
    indegree.forEach((value, key) => {
        if (value === 0) {
            queue.push(key);
        }
    });

    const layerMap = new Map();
    while (queue.length) {
        const key = queue.shift();
        const currentLayer = layerMap.get(key) ?? 0;
        layerMap.set(key, currentLayer);
        adjacency.get(key).forEach((next) => {
            const proposedLayer = currentLayer + 1;
            const existing = layerMap.get(next);
            if (existing === undefined || proposedLayer > existing) {
                layerMap.set(next, proposedLayer);
            }
            const remaining = indegree.get(next) - 1;
            indegree.set(next, remaining - 1);
            if (remaining - 1 === 0) {
                queue.push(next);
            }
        });
    }

    let maxLayer = 0;
    groupsMap.forEach((_, key) => {
        if (!layerMap.has(key)) {
            layerMap.set(key, 0);
        }
        maxLayer = Math.max(layerMap.get(key), maxLayer);
    });

    const layers = new Map();
    layerMap.forEach((layer, key) => {
        if (!layers.has(layer)) {
            layers.set(layer, []);
        }
        layers.get(layer).push(groupsMap.get(key));
    });

    const layerCount = maxLayer + 1;
    const width = Math.max(MIN_CANVAS, LAYER_MARGIN_X * 2 + Math.max(layerCount - 1, 0) * WIDTH_PER_LAYER);
    let maxGroups = 0;
    layers.forEach((groupList) => {
        groupList.sort((a, b) => a.label.localeCompare(b.label));
        maxGroups = Math.max(maxGroups, groupList.length);
    });
    const height = Math.max(MIN_CANVAS, LAYER_MARGIN_Y * 2 + Math.max(maxGroups - 1, 0) * HEIGHT_PER_GROUP);

    layers.forEach((groupList, layer) => {
        const x = LAYER_MARGIN_X + layer * WIDTH_PER_LAYER;
        const count = groupList.length;
        const stepY = count <= 1 ? 0 : (height - 2 * LAYER_MARGIN_Y) / Math.max(1, count - 1);
        groupList.forEach((group, index) => {
            const y = count <= 1 ? height / 2 : (LAYER_MARGIN_Y + stepY * index);
            positionGroup(group, x, y);
        });
    });

    const bounds = computeBounds(nodes, width, height);
    return {
        bounds,
        groups: Array.from(groupsMap.values()),
        nodes,
        edges,
        summary: graph?.summary ?? {},
        warnings: graph?.warnings ?? []
    };
}

function normalizeNode(node) {
    const props = node?.properties ?? {};
    const namespace = props.namespace ?? '';
    const pod = props.pod ?? null;
    const containerType = props.containerType ?? (node.type === 'sidecarContainer' ? 'sidecar' : 'app');
    const displayName = props.displayName ?? props.container ?? props.host ?? node.id;

    let groupKey;
    let groupType;
    let groupLabel;
    if (node.type === 'externalService') {
        const host = props.host ?? node.id;
        groupKey = `external:${host}`;
        groupType = 'external';
        groupLabel = host;
    } else if (pod) {
        groupKey = `pod:${namespace}/${pod}`;
        groupType = 'pod';
        groupLabel = pod;
    } else {
        groupKey = `misc:${node.id}`;
        groupType = 'misc';
        groupLabel = displayName;
    }

    const radius = node.type === 'sidecarContainer'
        ? NODE_RADIUS_SIDECAR
        : node.type === 'externalService'
            ? NODE_RADIUS_EXTERNAL
            : NODE_RADIUS_APP;

    return {
        id: node.id,
        type: node.type,
        properties: props,
        data: node,
        namespace,
        pod,
        containerType,
        displayName,
        groupKey,
        groupType,
        groupLabel,
        x: 0,
        y: 0,
        radius,
        color: node.type === 'sidecarContainer'
            ? 'var(--sidecar)'
            : node.type === 'externalService'
                ? 'var(--external)'
                : 'var(--app)'
    };
}

function normalizeEdge(edge, nodeMap) {
    const source = nodeMap.get(edge.source);
    const target = nodeMap.get(edge.target);
    return {
        id: edge.id,
        kind: edge.kind,
        properties: edge.properties ?? {},
        sourceId: source?.id ?? edge.source,
        targetId: target?.id ?? edge.target,
        source,
        target,
        data: edge
    };
}

function positionGroup(group, centerX, centerY) {
    if (!group.nodes.length) {
        return;
    }
    const appNodes = group.nodes.filter((node) => node.containerType !== 'sidecar');
    const sidecarNodes = group.nodes.filter((node) => node.containerType === 'sidecar');
    const otherNodes = group.nodes.filter((node) => node.containerType !== 'sidecar' && node.type === 'externalService');

    const placeColumn = (nodes, columnX) => {
        if (!nodes.length) {
            return;
        }
        const startY = centerY - ((nodes.length - 1) * POD_VERTICAL_GAP) / 2;
        nodes.forEach((node, idx) => {
            node.x = columnX;
            node.y = startY + idx * POD_VERTICAL_GAP;
        });
    };

    if (group.type === 'pod') {
        placeColumn(appNodes, centerX - POD_HORIZONTAL_GAP);
        placeColumn(sidecarNodes, centerX + POD_HORIZONTAL_GAP);
        placeColumn(otherNodes, centerX);
    } else if (group.type === 'external') {
        placeColumn(group.nodes, centerX + POD_HORIZONTAL_GAP * 1.5);
    } else {
        placeColumn(group.nodes, centerX);
    }

    const xs = group.nodes.map((n) => n.x);
    const ys = group.nodes.map((n) => n.y);
    const minX = Math.min(...xs);
    const maxX = Math.max(...xs);
    const minY = Math.min(...ys);
    const maxY = Math.max(...ys);
    group.box = {
        x: minX - 20,
        y: minY - 24,
        width: (maxX - minX) + 40,
        height: (maxY - minY) + 48
    };
}

function computeBounds(nodes, width, height) {
    if (!nodes.length) {
        return { minX: 0, minY: 0, maxX: width, maxY: height };
    }
    let minX = Infinity;
    let maxX = -Infinity;
    let minY = Infinity;
    let maxY = -Infinity;
    const padding = 40;
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

