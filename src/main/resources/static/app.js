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
    activeGraph = new ForceGraph(canvas, data, updateSelection);
}

class ForceGraph {
    constructor(canvas, data, onSelect) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.onSelect = onSelect;
        this.data = data;
        this.nodes = [];
        this.edges = [];
        this.selected = null;
        this.dragging = null;
        this.width = canvas.width;
        this.height = canvas.height;
        this._podCenters = new Map();
        this._initGraph();
        this._bindEvents();
        this._run();
    }

    setSize(width, height) {
        this.width = width;
        this.height = height;
    }

    teardown() {
        cancelAnimationFrame(this._raf);
        this.canvas.removeEventListener('mousedown', this._onMouseDown);
        window.removeEventListener('mousemove', this._onMouseMove);
        window.removeEventListener('mouseup', this._onMouseUp);
    }

    _initGraph() {
        const colors = this._resolveColors();
        const nodeMap = new Map();
        for (const node of this.data.nodes || []) {
            const properties = node.properties || {};
            const podKey = properties.pod ? `${properties.namespace || ''}:${properties.pod}` : null;
            const containerType = properties.containerType;
            const radius = containerType === 'sidecar' ? 10 : 12;
            const color = node.type === 'sidecarContainer' ? colors.sidecar : node.type === 'externalService' ? colors.external : colors.app;
            const base = this._computeInitialPosition(podKey, containerType);
            const item = {
                id: node.id,
                data: node,
                x: base.x,
                y: base.y,
                vx: 0,
                vy: 0,
                radius,
                color
            };
            this.nodes.push(item);
            nodeMap.set(node.id, item);
        }
        for (const edge of this.data.edges || []) {
            const source = nodeMap.get(edge.source);
            const target = nodeMap.get(edge.target);
            if (!source || !target) {
                continue;
            }
            const color = edge.kind === 'podLink' ? 'rgba(148,163,184,0.25)' : 'rgba(148,163,184,0.45)';
            this.edges.push({
                id: edge.id,
                data: edge,
                source,
                target,
                color
            });
        }
    }

    _computeInitialPosition(podKey, containerType) {
        if (!podKey) {
            const angle = Math.random() * Math.PI * 2;
            const distance = Math.random() * Math.min(this.width, this.height) * 0.4;
            return {
                x: this.width / 2 + Math.cos(angle) * distance,
                y: this.height / 2 + Math.sin(angle) * distance
            };
        }
        let pod = this._podCenters.get(podKey);
        if (!pod) {
            const angle = Math.random() * Math.PI * 2;
            const distance = Math.random() * Math.min(this.width, this.height) * 0.3;
            pod = { baseX: this.width / 2 + Math.cos(angle) * distance, baseY: this.height / 2 + Math.sin(angle) * distance, count: 0 };
            this._podCenters.set(podKey, pod);
        }
        const index = pod.count++;
        const offsetAngle = index * (Math.PI / 3);
        const offsetRadius = containerType === 'sidecar' ? 22 : 30;
        return {
            x: pod.baseX + Math.cos(offsetAngle) * offsetRadius,
            y: pod.baseY + Math.sin(offsetAngle) * offsetRadius
        };
    }

    _resolveColors() {
        const styles = getComputedStyle(document.documentElement);
        return {
            app: styles.getPropertyValue('--app').trim() || '#38bdf8',
            sidecar: styles.getPropertyValue('--sidecar').trim() || '#c084fc',
            external: styles.getPropertyValue('--external').trim() || '#f97316'
        };
    }

    _bindEvents() {
        this._onMouseDown = (event) => {
            const point = this._eventPoint(event);
            const node = this._hitNode(point.x, point.y);
            if (node) {
                this.dragging = { node, offsetX: node.x - point.x, offsetY: node.y - point.y };
                this._select({ type: 'node', data: node.data });
            } else {
                const edge = this._hitEdge(point.x, point.y);
                if (edge) {
                    this._select({ type: 'edge', data: edge.data });
                } else {
                    this._select(null);
                }
            }
        };
        this._onMouseMove = (event) => {
            if (!this.dragging) {
                return;
            }
            const point = this._eventPoint(event);
            const { node, offsetX, offsetY } = this.dragging;
            node.x = point.x + offsetX;
            node.y = point.y + offsetY;
            node.vx = 0;
            node.vy = 0;
        };
        this._onMouseUp = () => {
            this.dragging = null;
        };
        this.canvas.addEventListener('mousedown', this._onMouseDown);
        window.addEventListener('mousemove', this._onMouseMove);
        window.addEventListener('mouseup', this._onMouseUp);
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
    }

    _run() {
        const step = () => {
            this._tick();
            this._draw();
            this._raf = requestAnimationFrame(step);
        };
        step();
    }

    _tick() {
        const repulsion = 5000;
        const spring = 0.012;
        const idealLength = 140;
        for (let i = 0; i < this.nodes.length; i++) {
            const nodeA = this.nodes[i];
            for (let j = i + 1; j < this.nodes.length; j++) {
                const nodeB = this.nodes[j];
                let dx = nodeB.x - nodeA.x;
                let dy = nodeB.y - nodeA.y;
                let distSq = dx * dx + dy * dy;
                if (distSq === 0) {
                    distSq = 0.01;
                }
                const force = repulsion / distSq;
                const distance = Math.sqrt(distSq);
                dx /= distance;
                dy /= distance;
                nodeA.vx -= force * dx;
                nodeA.vy -= force * dy;
                nodeB.vx += force * dx;
                nodeB.vy += force * dy;
            }
        }
        for (const edge of this.edges) {
            if (edge.data.kind === 'podLink') {
                continue;
            }
            const { source, target } = edge;
            let dx = target.x - source.x;
            let dy = target.y - source.y;
            const distance = Math.sqrt(dx * dx + dy * dy) || 0.01;
            const displacement = distance - idealLength;
            const force = spring * displacement;
            dx /= distance;
            dy /= distance;
            source.vx += force * dx;
            source.vy += force * dy;
            target.vx -= force * dx;
            target.vy -= force * dy;
        }
        for (const node of this.nodes) {
            if (this.dragging && this.dragging.node === node) {
                continue;
            }
            node.vx *= 0.85;
            node.vy *= 0.85;
            node.x += node.vx;
            node.y += node.vy;
            const margin = node.radius + 24;
            if (node.x < margin) {
                node.x = margin;
                node.vx *= -0.5;
            } else if (node.x > this.width - margin) {
                node.x = this.width - margin;
                node.vx *= -0.5;
            }
            if (node.y < margin) {
                node.y = margin;
                node.vy *= -0.5;
            } else if (node.y > this.height - margin) {
                node.y = this.height - margin;
                node.vy *= -0.5;
            }
        }
    }

    _draw() {
        const ctx = this.ctx;
        ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        ctx.lineWidth = 1.5;
        for (const edge of this.edges) {
            ctx.globalAlpha = edge.data.kind === 'podLink' ? 0.4 : 0.9;
            ctx.strokeStyle = this.selected?.type === 'edge' && this.selected.data.id === edge.id ? '#fbbf24' : edge.color;
            ctx.beginPath();
            ctx.moveTo(edge.source.x, edge.source.y);
            ctx.lineTo(edge.target.x, edge.target.y);
            ctx.stroke();
        }
        ctx.globalAlpha = 1;
        ctx.font = '11px "Segoe UI", sans-serif';
        ctx.textBaseline = 'middle';
        for (const node of this.nodes) {
            const isSelected = this.selected?.type === 'node' && this.selected.data.id === node.id;
            ctx.beginPath();
            ctx.fillStyle = node.color;
            ctx.strokeStyle = isSelected ? '#fbbf24' : '#0f172a';
            ctx.lineWidth = 2;
            ctx.arc(node.x, node.y, node.radius, 0, Math.PI * 2);
            ctx.fill();
            ctx.stroke();
            const displayName = node.data.properties?.displayName || node.data.properties?.container || node.data.id;
            ctx.fillStyle = '#cbd5f5';
            ctx.fillText(displayName, node.x + node.radius + 6, node.y);
        }
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
