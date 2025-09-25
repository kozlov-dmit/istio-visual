const canvas = document.getElementById('graph-canvas');
const namespaceInput = document.getElementById('namespace');
const loadBtn = document.getElementById('load-btn');
const refreshBtn = document.getElementById('refresh-btn');
const statusEl = document.getElementById('status');
const selectionEl = document.getElementById('selection');
const summaryEl = document.getElementById('summary');
const warningsEl = document.getElementById('warnings');

let activeGraph = null;
let activeData = null;
let currentNamespace = null;

function resizeCanvas() {
    const rect = canvas.parentElement.getBoundingClientRect();
    canvas.width = rect.width;
    canvas.height = rect.height;
    if (activeGraph) {
        activeGraph.setSize(canvas.width, canvas.height);
    }
}

window.addEventListener('resize', resizeCanvas);

function formatJSON(value) {
    return JSON.stringify(value, null, 2);
}

function setStatus(text, type = 'info') {
    statusEl.textContent = text;
    statusEl.dataset.state = type;
}

function updateSummary(data) {
    if (!data || !data.summary) {
        summaryEl.textContent = 'n/a';
        return;
    }
    const items = Object.entries(data.summary)
        .map(([key, value]) => `<div><span>${key}</span><strong>${value}</strong></div>`)
        .join('');
    summaryEl.innerHTML = items;
}

function updateWarnings(data) {
    warningsEl.innerHTML = '';
    if (!data?.warnings?.length) {
        const li = document.createElement('li');
        li.textContent = 'No warnings';
        warningsEl.appendChild(li);
        return;
    }
    for (const warning of data.warnings) {
        const li = document.createElement('li');
        li.textContent = warning;
        warningsEl.appendChild(li);
    }
}

function cleanResource(resource) {
    if (Array.isArray(resource)) {
        return resource.map(cleanResource);
    }
    if (resource && typeof resource === 'object') {
        const result = {};
        for (const [key, value] of Object.entries(resource)) {
            if (value === null || value === undefined || value === '') {
                continue;
            }
            result[key] = cleanResource(value);
        }
        return result;
    }
    return resource;
}

function updateSelection(info) {
    if (!info) {
        selectionEl.textContent = 'Click a node or edge to inspect';
        return;
    }
    if (info.type === 'node') {
        const node = info.data;
        const printable = cleanResource(node.properties || {});
        selectionEl.textContent = `Node ${node.id}\nType: ${node.type}\n\n` + formatJSON(printable);
    } else if (info.type === 'edge') {
        const edge = info.data;
        const printable = cleanResource(edge);
        selectionEl.textContent = `Edge ${edge.id}\nType: ${edge.kind}\n\n` + formatJSON(printable);
    }
}

async function fetchGraph(namespace) {
    const params = new URLSearchParams({ namespace });
    const response = await fetch(`/api/graph?${params.toString()}`);
    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Request failed with status ${response.status}`);
    }
    return await response.json();
}

async function loadGraph(namespace, silent = false) {
    try {
        if (!silent) {
            setStatus('Loading...', 'pending');
        }
        const data = await fetchGraph(namespace);
        activeData = data;
        currentNamespace = namespace;
        renderGraph(data);
        const updated = new Date(data.generatedAt || Date.now());
        setStatus(`Updated ${updated.toLocaleTimeString()}`, 'ok');
        updateSummary(data);
        updateWarnings(data);
        history.replaceState({}, '', `?namespace=${encodeURIComponent(namespace)}`);
    } catch (error) {
        console.error(error);
        setStatus(`Error: ${error.message}`, 'error');
    }
}

function renderGraph(data) {
    resizeCanvas();
    if (activeGraph) {
        activeGraph.teardown();
    }
    activeGraph = new TopologyGraph(canvas, data, updateSelection);
}

class TopologyGraph {
    constructor(canvas, data, onSelect) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.onSelect = onSelect;
        this.width = canvas.width;
        this.height = canvas.height;
        this.nodes = [];
        this.edges = [];
        this.nodeMap = new Map();
        this.nodeGroups = new Map();
        this.groups = new Map();
        this.selected = null;
        this._initGraph(data);
        this._bindEvents();
        this._layout();
        this._draw();
    }

    setSize(width, height) {
        this.width = width;
        this.height = height;
        this.canvas.width = width;
        this.canvas.height = height;
        this._layout();
        this._draw();
    }

    teardown() {
        if (this._onClick) {
            this.canvas.removeEventListener('click', this._onClick);
        }
    }

    _colorFor(node) {
        const styles = getComputedStyle(document.documentElement);
        switch (node.type) {
            case 'sidecarContainer':
                return styles.getPropertyValue('--sidecar').trim() || '#c084fc';
            case 'externalService':
                return styles.getPropertyValue('--external').trim() || '#f97316';
            default:
                return styles.getPropertyValue('--app').trim() || '#38bdf8';
        }
    }

    _initGraph(data) {
        for (const node of data.nodes || []) {
            const item = {
                id: node.id,
                data: node,
                x: 0,
                y: 0,
                radius: node.type === 'sidecarContainer' ? 10 : 12,
                color: this._colorFor(node)
            };
            this.nodes.push(item);
            this.nodeMap.set(node.id, item);
        }
        for (const edge of data.edges || []) {
            const source = this.nodeMap.get(edge.source);
            const target = this.nodeMap.get(edge.target);
            if (!source || !target) {
                continue;
            }
            const color = edge.kind === 'podLink'
                ? 'rgba(148, 163, 184, 0.25)'
                : 'rgba(148, 163, 184, 0.6)';
            this.edges.push({ id: edge.id, data: edge, source, target, color });
        }
    }

    _bindEvents() {
        this._onClick = (event) => {
            const point = this._eventPoint(event);
            const node = this._hitNode(point.x, point.y);
            if (node) {
                this._select({ type: 'node', data: node.data });
                return;
            }
            const edge = this._hitEdge(point.x, point.y);
            if (edge) {
                this._select({ type: 'edge', data: edge.data });
                return;
            }
            this._select(null);
        };
        this.canvas.addEventListener('click', this._onClick);
    }

    _layout() {
        if (!this.nodes.length) {
            return;
        }
        const groups = new Map();
        const nodeGroups = new Map();

        for (const node of this.nodes) {
            const props = node.data.properties || {};
            let key;
            let type;
            let label;
            if (node.data.type === 'externalService') {
                const host = props.host || node.data.id;
                key = `external:${host}`;
                type = 'external';
                label = host;
            } else if (props.pod) {
                const ns = props.namespace || '';
                key = `pod:${ns}/${props.pod}`;
                type = 'pod';
                label = props.pod;
            } else {
                key = `node:${node.id}`;
                type = 'misc';
                label = node.data.id;
            }
            if (!groups.has(key)) {
                groups.set(key, {
                    key,
                    type,
                    label,
                    namespace: props.namespace || '',
                    nodes: []
                });
            }
            const group = groups.get(key);
            group.nodes.push(node);
            nodeGroups.set(node.id, group);
        }

        const adjacency = new Map();
        const indegree = new Map();
        groups.forEach((group, key) => {
            adjacency.set(key, new Set());
            indegree.set(key, 0);
        });

        for (const edge of this.edges) {
            if (edge.data.kind !== 'traffic') {
                continue;
            }
            const sourceGroup = nodeGroups.get(edge.source.id);
            const targetGroup = nodeGroups.get(edge.target.id);
            if (!sourceGroup || !targetGroup) {
                continue;
            }
            if (sourceGroup.key === targetGroup.key) {
                continue;
            }
            const neighbours = adjacency.get(sourceGroup.key);
            if (!neighbours.has(targetGroup.key)) {
                neighbours.add(targetGroup.key);
                indegree.set(targetGroup.key, indegree.get(targetGroup.key) + 1);
            }
        }

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
            const nextKeys = adjacency.get(key) || new Set();
            if (!layerMap.has(key)) {
                layerMap.set(key, currentLayer);
            }
            nextKeys.forEach((next) => {
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
        groups.forEach((group, key) => {
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
            layers.get(layer).push(groups.get(key));
        });
        layers.forEach((list) => list.sort((a, b) => a.label.localeCompare(b.label)));

        const layerCount = maxLayer + 1;
        const marginX = 140;
        const marginY = 100;
        const usableWidth = Math.max(this.width - marginX * 2, 200);
        const usableHeight = Math.max(this.height - marginY * 2, 200);
        const layerSpacing = layerCount === 1 ? 0 : usableWidth / (layerCount - 1);

        layers.forEach((groupList, layer) => {
            const x = marginX + layerSpacing * layer;
            const count = groupList.length;
            const stepY = count === 1 ? 0 : usableHeight / (count - 1);
            groupList.forEach((group, index) => {
                let y;
                if (count === 1) {
                    y = marginY + usableHeight / 2;
                } else {
                    y = marginY + stepY * index;
                }
                this._positionGroup(group, x, y);
            });
        });

        this.groups = groups;
        this.nodeGroups = nodeGroups;
    }

    _positionGroup(group, centerX, centerY) {
        const verticalSpacing = 26;
        const appNodes = group.nodes.filter((node) => node.data.type === 'appContainer');
        const sidecarNodes = group.nodes.filter((node) => node.data.type === 'sidecarContainer');
        const otherNodes = group.nodes.filter((node) => node.data.type !== 'appContainer' && node.data.type !== 'sidecarContainer');

        const placeColumn = (nodes, columnX) => {
            if (!nodes.length) {
                return;
            }
            const startY = centerY - ((nodes.length - 1) * verticalSpacing) / 2;
            nodes.forEach((node, idx) => {
                node.x = columnX;
                node.y = startY + idx * verticalSpacing;
            });
        };

        if (group.type === 'pod') {
            placeColumn(appNodes, centerX - 34);
            placeColumn(sidecarNodes, centerX + 34);
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
            x: minX - 28,
            y: minY - 34,
            width: (maxX - minX) + 56,
            height: (maxY - minY) + 68
        };
    }

    _draw() {
        const ctx = this.ctx;
        ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        ctx.save();
        ctx.font = '12px "Segoe UI", sans-serif';
        ctx.textBaseline = 'top';
        this.groups.forEach((group) => {
            if (group.type !== 'pod' || !group.box) {
                return;
            }
            ctx.save();
            ctx.fillStyle = 'rgba(30, 64, 175, 0.08)';
            ctx.strokeStyle = 'rgba(59, 130, 246, 0.35)';
            ctx.lineWidth = 1.2;
            ctx.setLineDash([6, 6]);
            ctx.fillRect(group.box.x, group.box.y, group.box.width, group.box.height);
            ctx.strokeRect(group.box.x, group.box.y, group.box.width, group.box.height);
            ctx.setLineDash([]);
            ctx.fillStyle = '#94a3b8';
            ctx.fillText(group.label, group.box.x + 4, group.box.y - 16);
            ctx.restore();
        });
        ctx.restore();

        const selectedNodeId = this.selected?.type === 'node' ? this.selected.data.id : null;
        const selectedEdgeId = this.selected?.type === 'edge' ? this.selected.data.id : null;

        for (const edge of this.edges) {
            ctx.beginPath();
            const isSelected = edge.id === selectedEdgeId;
            ctx.globalAlpha = edge.data.kind === 'podLink' ? 0.35 : 0.9;
            ctx.strokeStyle = isSelected ? '#fbbf24' : edge.color;
            ctx.lineWidth = isSelected ? 2.6 : (edge.data.kind === 'podLink' ? 1 : 1.8);
            ctx.moveTo(edge.source.x, edge.source.y);
            ctx.lineTo(edge.target.x, edge.target.y);
            ctx.stroke();
        }

        ctx.globalAlpha = 1;
        ctx.font = '11px "Segoe UI", sans-serif';
        ctx.textBaseline = 'middle';
        for (const node of this.nodes) {
            const isSelected = node.id === selectedNodeId;
            ctx.beginPath();
            ctx.fillStyle = node.color;
            ctx.strokeStyle = isSelected ? '#fbbf24' : '#0f172a';
            ctx.lineWidth = 2;
            ctx.arc(node.x, node.y, node.radius, 0, Math.PI * 2);
            ctx.fill();
            ctx.stroke();
            const displayName = node.data.properties?.displayName || node.data.properties?.container || node.data.id;
            ctx.fillStyle = '#cbd5f5';
            ctx.fillText(displayName, node.x + node.radius + 8, node.y);
        }
    }

    _eventPoint(event) {
        const rect = this.canvas.getBoundingClientRect();
        return {
            x: (event.clientX - rect.left) * (this.canvas.width / rect.width),
            y: (event.clientY - rect.top) * (this.canvas.height / rect.height)
        };
    }

    _hitNode(x, y) {
        for (const node of this.nodes) {
            const dx = node.x - x;
            const dy = node.y - y;
            if (Math.hypot(dx, dy) <= node.radius + 4) {
                return node;
            }
        }
        return null;
    }

    _hitEdge(x, y) {
        const threshold = 6;
        for (const edge of this.edges) {
            const { source, target } = edge;
            const dx = target.x - source.x;
            const dy = target.y - source.y;
            const lengthSq = dx * dx + dy * dy;
            if (lengthSq === 0) {
                continue;
            }
            const t = ((x - source.x) * dx + (y - source.y) * dy) / lengthSq;
            if (t < 0 || t > 1) {
                continue;
            }
            const px = source.x + t * dx;
            const py = source.y + t * dy;
            if (Math.hypot(px - x, py - y) < threshold) {
                return edge;
            }
        }
        return null;
    }

    _select(item) {
        this.selected = item;
        if (this.onSelect) {
            this.onSelect(item);
        }
        this._draw();
    }
}

function setupNamespaceField() {
    const params = new URLSearchParams(window.location.search);
    const initial = params.get('namespace');
    if (initial) {
        namespaceInput.value = initial;
    }
    loadBtn.addEventListener('click', () => {
        const ns = namespaceInput.value.trim();
        if (!ns) {
            setStatus('Namespace is required', 'error');
            return;
        }
        loadGraph(ns);
    });
    refreshBtn.addEventListener('click', () => {
        if (currentNamespace) {
            loadGraph(currentNamespace, true);
        } else if (namespaceInput.value.trim()) {
            loadGraph(namespaceInput.value.trim());
        }
    });
}

setupNamespaceField();
resizeCanvas();

const autoNamespace = namespaceInput.value.trim() || namespaceInput.placeholder || '';
if (autoNamespace) {
    loadGraph(autoNamespace, true);
} else {
    setStatus('Enter namespace to begin', 'info');
}
