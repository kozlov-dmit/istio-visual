import React, { useEffect, useRef, useState } from 'react';
import TopologyDetailsPanel from './TopologyDetailsPanel';

// TopologyCanvasReadable.jsx
// Improved UI for large graphs:
// - pan & zoom
// - node grouping / clustering with expand/collapse
// - aggregated edges to reduce clutter
// - layout that keeps nodes inside mesh bounds and wraps
// - controls: zoom, reset, toggle grouping, search

export default function TopologyCanvasReadable({ initialNamespace = 'default' }) {
  const [namespace, setNamespace] = useState(initialNamespace);
  const [graph, setGraph] = useState({ nodes: [], edges: [] });
  const [selectedNode, setSelectedNode] = useState(null);
  const [groupingEnabled, setGroupingEnabled] = useState(true);
  const [expandedGroups, setExpandedGroups] = useState(new Set());
  const [search, setSearch] = useState('');

  // pan & zoom
  const [scale, setScale] = useState(1);
  const [tx, setTx] = useState(0);
  const [ty, setTy] = useState(0);
  const panState = useRef({ dragging:false, startX:0, startY:0, startTx:0, startTy:0 });

  const svgRef = useRef(null);

  useEffect(() => { load(namespace); }, [namespace]);

  async function load(ns) {
    try {
      const res = await fetch(`/api/topology/diagnose-enhanced?namespace=${encodeURIComponent(ns)}`);
      if (!res.ok) throw new Error(await res.text().catch(()=>res.statusText));
      const body = await res.json();
      const nodesArr = Array.isArray(body.nodes) ? body.nodes : (body.nodes ? Array.from(body.nodes) : []);
      const edgesArr = Array.isArray(body.edges) ? body.edges : (body.edges || []);
      setGraph({ nodes: nodesArr, edges: edgesArr });
      setSelectedNode(null);
      setExpandedGroups(new Set());
      // reset view
      setScale(1); setTx(0); setTy(0);
    } catch (e) { console.error('load error', e); }
  }

  // helpers
  function nodeById(id){ return (graph.nodes || []).find(n => n.id === id) || null; }
  function short(id){ if(!id) return ''; return String(id).replace(/^external:/,'').replace(/^serviceentry:/,'').replace(/\.svc\.cluster\.local$/,''); }

  // simple aggregation of edges: map (from,to,protocol) -> aggregated
  function aggregateEdges(edges) {
    const map = new Map();
    edges.forEach(e => {
      const key = `${e.fromId}||${e.toId}||${e.protocol||''}`;
      const cur = map.get(key) || { fromId: e.fromId, toId: e.toId, protocol: e.protocol, ports: new Set(), count:0, samples: [] };
      if (e.port) cur.ports.add(e.port);
      cur.count++;
      if (e.notes) cur.samples.push(...(e.notes||[]));
      map.set(key, cur);
    });
    return Array.from(map.values()).map(x => ({ ...x, ports: Array.from(x.ports) }));
  }

  // grouping nodes inside mesh by label 'app' or first label key; collapse groups larger than threshold
  function buildDrawableNodes(nodes) {
    const meshNodes = nodes.filter(n => n.type === 'K8S_SERVICE' || n.type === 'VIRTUALSERVICE' || n.type === 'UNKNOWN' || n.type === 'VIRTUALSERVICE');
    const externals = nodes.filter(n => n.type === 'EXTERNAL' || n.type === 'SERVICEENTRY');

    const grouped = new Map();
    const threshold = 18; // if group bigger than threshold, collapse

    meshNodes.forEach(n => {
      // try to detect app label or fallback to type
      const app = (n.meta && n.meta.labels && (n.meta.labels.app || n.meta.labels['app.kubernetes.io/name'])) || null;
      const key = app || (n.meta && n.meta.labels ? Object.entries(n.meta.labels).map(([k,v])=>`${k}:${v}`).join(',') : 'ungrouped');
      if (!grouped.has(key)) grouped.set(key, []);
      grouped.get(key).push(n);
    });

    const drawable = [];
    // create group nodes or singletons
    grouped.forEach((members, key) => {
      if (groupingEnabled && members.length > threshold) {
        const id = `group:${key}`;
        drawable.push({ id, label: key, members, group:true, count: members.length });
      } else {
        members.forEach(m => drawable.push({ id: m.id, label: m.id, original: m }));
      }
    });

    // add externals after
    externals.forEach(e => drawable.push({ id: e.id, label: short(e.id), external:true }));

    return drawable;
  }

  // compute positions within mesh bounds and on right side for externals
  function layout(drawable) {
    const meshBox = { x: 40, y: 60, width: 420, height: 560 };
    const rightX = 640;
    // place group/singleton nodes inside mesh in grid
    const inside = drawable.filter(d => !d.external);
    const cols = 3;
    const colWidth = Math.floor((meshBox.width - 40) / cols);
    const positions = new Map();
    inside.forEach((n, idx) => {
      const col = idx % cols; const row = Math.floor(idx / cols);
      const x = meshBox.x + 20 + col * colWidth;
      const y = meshBox.y + 40 + row * 64;
      positions.set(n.id, { x, y, width: Math.min(160, colWidth - 12), height: 40 });
    });

    // externals down the right side
    const externals = drawable.filter(d => d.external);
    externals.forEach((e, idx) => {
      const x = rightX + 100; const y = meshBox.y + 40 + idx*64;
      positions.set(e.id, { x, y, width: 160, height: 40 });
    });

    return { meshBox, rightX, positions };
  }

  // expand/collapse group
  function toggleGroup(id) {
    const next = new Set(expandedGroups);
    if (next.has(id)) next.delete(id); else next.add(id);
    setExpandedGroups(next);
  }

  // draw aggregated edges but map group ids to members when expanded
  function buildDrawableEdges(aggEdges, positions, drawableMap) {
    const out = [];
    aggEdges.forEach(e => {
      const fromId = mapDrawableId(e.fromId, drawableMap);
      const toId = mapDrawableId(e.toId, drawableMap);
      if (!positions.has(fromId) || !positions.has(toId)) return;
      out.push({ fromId, toId, protocol: e.protocol, count: e.count, ports: e.ports });
    });
    return out;
  }

  function mapDrawableId(originalId, drawableMap) {
    // if original maps to a group id in drawableMap, use group id; if group expanded, return first member (to reduce overlap)
    const entry = drawableMap.get(originalId);
    if (!entry) return originalId;
    if (entry.group) {
      const gid = entry.id;
      if (expandedGroups.has(gid)) {
        // pick a member's id (first) - in future could lay out members around group center
        return entry.members[0].id;
      }
      return gid;
    }
    return entry.id;
  }

  // ---------------- render ----------------
  const drawable = buildDrawableNodes(graph.nodes || []);
  const drawableMap = new Map();
  drawable.forEach(d => drawableMap.set(d.id, d));
  // also map original nodes to group id if grouped
  drawable.forEach(d => { if (d.group) d.members.forEach(m=>drawableMap.set(m.id, d)); else if (d.original) drawableMap.set(d.original.id, d); });

  const aggEdges = aggregateEdges(graph.edges || []);
  const { meshBox, rightX, positions } = layout(drawable);
  const drawableEdges = buildDrawableEdges(aggEdges, positions, drawableMap);

  // search highlight
  const searchQ = (search||'').toLowerCase();

  // pan/zoom handlers
  function onWheel(ev) {
    ev.preventDefault();
    const delta = -ev.deltaY;
    const factor = delta > 0 ? 1.1 : 0.9;
    const newScale = Math.min(3, Math.max(0.3, scale * factor));
    setScale(newScale);
  }
  function onMouseDown(ev) {
    if (ev.button !== 0) return; // left only
    panState.current.dragging = true;
    panState.current.startX = ev.clientX; panState.current.startY = ev.clientY;
    panState.current.startTx = tx; panState.current.startTy = ty;
  }
  function onMouseMove(ev) {
    if (!panState.current.dragging) return;
    const dx = ev.clientX - panState.current.startX; const dy = ev.clientY - panState.current.startY;
    setTx(panState.current.startTx + dx);
    setTy(panState.current.startTy + dy);
  }
  function onMouseUp() { panState.current.dragging = false; }

  function resetView(){ setScale(1); setTx(0); setTy(0); }

  return (
    <div style={{ display: 'flex', gap: 12, padding: 12 }}>
      <div style={{ flex: 1 }}>
        <div style={{ display: 'flex', gap: 8, marginBottom: 8, alignItems: 'center' }}>
          <input value={namespace} onChange={e=>setNamespace(e.target.value)} style={{ padding: 6 }} />
          <button onClick={()=>load(namespace)} style={controlBtn}>Reload</button>
          <button onClick={()=>setGroupingEnabled(!groupingEnabled)} style={controlBtn}>{groupingEnabled?'Disable grouping':'Enable grouping'}</button>
          <button onClick={resetView} style={controlBtn}>Reset view</button>
          <div style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
            <input placeholder="search" value={search} onChange={e=>setSearch(e.target.value)} style={{ padding: 6 }} />
          </div>
        </div>

        <div style={{ border: '1px solid #e5e7eb', borderRadius: 8, overflow: 'hidden', background: '#fff' }}>
          <svg ref={svgRef} width="100%" height="700"
               onWheel={onWheel} onMouseDown={onMouseDown} onMouseMove={onMouseMove} onMouseUp={onMouseUp}
               style={{ cursor: panState.current.dragging ? 'grabbing' : 'grab', touchAction:'none' }}>
            <g transform={`translate(${tx}, ${ty}) scale(${scale})`}>

              {/* mesh box */}
              <rect x={meshBox.x} y={meshBox.y} width={meshBox.width} height={meshBox.height} rx={12} fill="#ffffff" stroke="#c7d2fe" />
              <text x={meshBox.x + 12} y={meshBox.y + 24} fontSize={14} fontWeight={700}>mesh (pods with sidecar)</text>

              {/* drawable nodes */}
              {drawable.map(d => {
                if (d.group && expandedGroups.has(d.id)) {
                  // if expanded, render members in a small cluster around group position
                  // find position of group cell (first member)
                  const idx = drawable.findIndex(x=>x.id===d.id);
                  const pos = positions.get(d.id) || { x: meshBox.x + 40, y: meshBox.y + 60 };
                  const members = d.members || [];
                  return members.map((m, mi) => {
                    const mx = pos.x + (mi%3)*60; const my = pos.y + Math.floor(mi/3)*44;
                    const highlight = searchQ && (m.id||'').toLowerCase().includes(searchQ);
                    return (
                      <g key={m.id} onClick={()=>setSelectedNode(m.id)} style={{ cursor: 'pointer' }}>
                        <rect x={mx} y={my} rx={6} width={120} height={32} fill={highlight? '#fff7ed' : '#e6f6ff'} stroke={highlight? '#f97316': '#0369a1'} />
                        <text x={mx+8} y={my+20} fontSize={11} fill={highlight? '#92400e':'#075985'}>{m.id}</text>
                      </g>
                    );
                  });
                }

                const pos = positions.get(d.id);
                if (!pos) return null;
                if (d.external) {
                  const highlight = searchQ && d.id.toLowerCase().includes(searchQ);
                  return (
                    <g key={d.id} onClick={()=>setSelectedNode(d.id)} style={{ cursor: 'pointer' }}>
                      <circle cx={pos.x} cy={pos.y} r={18} fill={highlight? '#fff1f2' : '#fff'} stroke="#b91c1c" />
                      <text x={pos.x} y={pos.y+4} fontSize={11} fill="#7f1d1d" textAnchor="middle">{short(d.id)}</text>
                    </g>
                  );
                }

                if (d.group) {
                  const highlight = searchQ && (d.label||'').toLowerCase().includes(searchQ);
                  return (
                    <g key={d.id} onClick={()=>toggleGroup(d.id)} style={{ cursor: 'pointer' }}>
                      <rect x={pos.x} y={pos.y} rx={8} width={pos.width} height={pos.height} fill={highlight? '#fff7ed' : '#f8fafc'} stroke="#9ca3af" />
                      <text x={pos.x+8} y={pos.y+22} fontSize={12} fill="#374151">{d.label} ({d.count})</text>
                    </g>
                  );
                }

                const highlight = searchQ && (d.label||'').toLowerCase().includes(searchQ);
                return (
                  <g key={d.id} onClick={()=>setSelectedNode(d.id)} style={{ cursor: 'pointer' }}>
                    <rect x={pos.x} y={pos.y} rx={8} width={pos.width} height={pos.height} fill={highlight? '#fff7ed' : '#e6f6ff'} stroke={highlight? '#f97316' : '#0369a1'} />
                    <text x={pos.x+8} y={pos.y+22} fontSize={12} fill={highlight? '#92400e':'#075985'}>{d.label}</text>
                  </g>
                );
              })}

              {/* draw aggregated edges (simple bezier) */}
              {drawableEdges.map((e, idx) => {
                const pFrom = positions.get(e.fromId); const pTo = positions.get(e.toId);
                if (!pFrom || !pTo) return null;
                const x1 = pFrom.x + (pFrom.width || 120);
                const y1 = pFrom.y + (pFrom.height || 36)/2;
                const x2 = pTo.x;
                const y2 = pTo.y + (pTo.height || 36)/2;
                const mx = (x1 + x2)/2; const my = (y1 + y2)/2 - 36 - (idx%3)*8;
                const d = `M ${x1} ${y1} Q ${mx} ${my} ${x2} ${y2}`;
                const label = e.ports && e.ports.length>0 ? e.ports.join(',') : e.count>1 ? `${e.count} routes` : '';
                return (
                  <g key={`agg-${idx}`}> 
                    <path d={d} fill="none" stroke="#374151" strokeWidth={Math.min(3, 1 + Math.log(e.count))} strokeOpacity={0.9} />
                    {label && <text x={mx} y={my - 6} fontSize={11} textAnchor="middle" fill="#374151">{label}</text>}
                  </g>
                );
              })}

            </g>
          </svg>
        </div>
      </div>

      <div style={{ width: 380 }}>
        <div style={{ background: '#fff', borderRadius: 8, padding: 12 }}>
          <h3 style={{ marginTop:0 }}>Details</h3>
          <TopologyDetailsPanel namespace={namespace} nodeId={selectedNode} onClose={()=>setSelectedNode(null)} />
          <div style={{ marginTop: 12 }}>
            <h4 style={{ marginBottom: 6 }}>Legend & Controls</h4>
            <div style={{ fontSize: 13, color: '#374151' }}>• Click a group to expand/collapse. Click a node to see details.</div>
            <div style={{ marginTop: 8, fontSize: 13 }}>• Use mouse drag to pan and mouse wheel to zoom. Reset view resets pan & zoom.</div>
          </div>
        </div>
      </div>
    </div>
  );
}
