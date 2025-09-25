const POD_HORIZONTAL_GAP = 34;
const POD_VERTICAL_GAP = 26;
const LAYER_MARGIN_X = 140;
const LAYER_MARGIN_Y = 100;
const MIN_CANVAS = 200;

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
            indegree.set(next, remaining);
            if (remaining === 0) {
                queue.push(next);
            }
        });
    }

    let maxLayer = 0;
    groupsMap.forEach((_, key) => {
        if (!layerMap.has(key)) {
            layerMap.set(key, 0);
        }
        maxLayer = Math.max(maxLayer, layerMap.get(key));
    });

    const layers = new Map();
    layerMap.forEach((layer, key) => {
        if (!layers.has(layer)) {
            layers.set(layer, []);
        }
        layers.get(layer).push(groupsMap.get(key));
    });
    layers.forEach((list) => list.sort((a, b) => a.label.localeCompare(b.label)));

    const layerCount = maxLayer + 1;
    const width = Math.max(MIN_CANVAS, (layerCount - 1) * 220 + LAYER_MARGIN_X * 2);
    const height = Math.max(MIN_CANVAS, Array.from(layers.values()).reduce((max, groupList) => Math.max(max, groupList.length), 1) * 200);
    const usableWidth = Math.max(width - LAYER_MARGIN_X * 2, 50);
    const usableHeight = Math.max(height - LAYER_MARGIN_Y * 2, 50);
    const layerSpacing = layerCount <= 1 ? 0 : usableWidth / (layerCount - 1);

    layers.forEach((groupList, layer) => {
        const x = LAYER_MARGIN_X + layerSpacing * layer;
        const count = groupList.length;
        const stepY = count <= 1 ? 0 : usableHeight / (count - 1);
        groupList.forEach((group, index) => {
            const y = count <= 1 ? LAYER_MARGIN_Y + usableHeight / 2 : LAYER_MARGIN_Y + stepY * index;
            positionGroup(group, x, y);
        });
    });

    const positionedGroups = Array.from(groupsMap.values());
    const positionedNodes = nodes;

    const bounds = computeBounds(positionedNodes);

    return {
        bounds,
        groups: positionedGroups,
        nodes: positionedNodes,
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
    const displayName = props.displayName ?? props.container ?? node.id;

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
        groupKey = `node:${node.id}`;
        groupType = 'misc';
        groupLabel = displayName;
    }

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
        radius: node.type === 'sidecarContainer' ? 10 : 12
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
    const otherNodes = group.nodes.filter((node) => node.containerType === 'sidecar' ? false : node.type === 'externalService');

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
        x: minX - 32,
        y: minY - 36,
        width: (maxX - minX) + 64,
        height: (maxY - minY) + 72
    };
}

function computeBounds(nodes) {
    if (!nodes.length) {
        return { minX: 0, minY: 0, maxX: MIN_CANVAS, maxY: MIN_CANVAS };
    }
    let minX = Infinity;
    let maxX = -Infinity;
    let minY = Infinity;
    let maxY = -Infinity;
    nodes.forEach((node) => {
        minX = Math.min(minX, node.x - node.radius - 60);
        maxX = Math.max(maxX, node.x + node.radius + 60);
        minY = Math.min(minY, node.y - node.radius - 60);
        maxY = Math.max(maxY, node.y + node.radius + 60);
    });
    return { minX, minY, maxX, maxY };
}
