import React, { useEffect, useRef, useState } from 'react';
import TopologyDetailsPanel from './TopologyDetailsPanel';

/**
 * TopologyCanvasImproved — enhanced with tooltip, highlight, and filters
 */
const COLORS = [
  '#ef4444', '#f97316', '#f59e0b', '#eab308', '#84cc16', '#10b981', '#06b6d4', '#3b82f6', '#6366f1'
];
function colorForPort(port, map) {
  if (map.has(String(port))) return map.get(String(port));
  const idx = map.size % COLORS.length;
  const c = COLORS[idx];
  map.set(String(port), c);
  return c;
}

export default function TopologyCanvasImproved({ initialNamespace = 'default' }) {
  const [namespace, setNamespace] = useState(initialNamespace);
  const [graph, setGraph] = useState({ nodes: [], edges: [] });
  const [selectedNode, setSelectedNode] = useState(null);
  const [showComposite, setShowComposite] = useState(true);
  const [showOnlyProblems, setShowOnlyProblems] = useState(false);
  const [protocolFilter, setProtocolFilter] = useState('ALL');
  const [search, setSearch] = useState('');

  const [hoverInfo, setHoverInfo] = useState(null); // {type:'edge'|'node', data, x,y}
  const [highlightedEdgeId, setHighlightedEdgeId] = useState(null);
  const [selectedEdgeId, setSelectedEdgeId] = useState(null);

  const containerRef = useRef(null);
  const mounted = useRef(true);

  useEffect(() => { mounted.current = true; load(namespace); return () => { mounted.current = false; }; }, []);

  async function load(ns) {
    try {
      const res = await fetch(`/api/topology/diagnose-enhanced?namespace=${encodeURIComponent(ns)}`);
      if (!res.ok) throw new Error(await res.text().catch(()=>res.statusText));
      const body = await res.json();
      if (!mounted.current) return;
      const nodesArr = Array.isArray(body.nodes) ? body.nodes : (body.nodes ? Array.from(body.nodes) : []);
      setGraph({ nodes: nodesArr, edges: Array.isArray(body.edges) ? body.edges : (body.edges || []) });
    } catch (e) {
      console.error('load error', e);
    }
  }

  function nodeById(id){ return (graph.nodes || []).find(n => n.id === id) || null; }
  function short(id){ if(!id) return ''; return String(id).replace(/^external:/,'').replace(/^serviceentry:/,'').replace(/\.svc\.cluster\.local$/,''); }

  // layout
  const width = 1200, height = 700;
  const leftX = 160, centerX = 600, rightX = 1040;
  function yFor(i, total){ const avail = height - 160; const spacing = Math.max(40, Math.floor(avail / Math.max(1, total - 1))); return 80 + i * spacing; }

  const nodes = graph.nodes || [];
  const edges = graph.edges || [];
  const meshNodes = nodes.filter(n => n.type === 'K8S_SERVICE');
  const seNodes = nodes.filter(n => n.type === 'SERVICEENTRY');
  const externalNodes = nodes.filter(n => n.type === 'EXTERNAL');

  // build port color map
  const portColorMap = new Map();
  edges.forEach(e => {
    if (e.protocol === 'COMPOSITE') {
      const pm = e.meta && e.meta.portMap; if (pm) { if (pm.fromPort) colorForPort(pm.fromPort, portColorMap); if (pm.toPort) colorForPort(pm.toPort, portColorMap); }
    } else if (e.port) { colorForPort(e.port, portColorMap); }
  });

  // filters
  function edgeMatchesFilters(e) {
    if (!e) return false;
    if (!showComposite && e.protocol === 'COMPOSITE') return false;
    if (protocolFilter !== 'ALL' && e.protocol !== protocolFilter) return false;
    if (showOnlyProblems) {
      const hasIssue = (e.diagnostics && e.diagnostics.length>0) || (e.notes && e.notes.some(n => /error|warn|fail/i.test(n)));
      if (!hasIssue) return false;
    }
    if (search && search.trim()) {
      const q = search.toLowerCase();
      const from = String(e.fromId || '').toLowerCase(); const to = String(e.toId || '').toLowerCase();
      if (!from.includes(q) && !to.includes(q)) return false;
    }
    return true;
  }

  const normalEdges = edges.filter(e => e.protocol !== 'COMPOSITE' && edgeMatchesFilters(e));
  const compositeEdges = edges.filter(e => e.protocol === 'COMPOSITE' && edgeMatchesFilters(e));

  function edgeId(e, idx){ return `${e.fromId}-->${e.toId}::${e.protocol}::${idx}`; }

  // hover/tooltip
  function onEdgeMouseMove(edge, idx, ev) {
    const rect = containerRef.current && containerRef.current.getBoundingClientRect();
    const x = ev.clientX - (rect ? rect.left : 0) + 10;
    const y = ev.clientY - (rect ? rect.top : 0) + 10;
    const pm = edge.meta && edge.meta.portMap ? edge.meta.portMap : null;
    const content = edge.protocol === 'COMPOSITE' ? `${short(edge.fromId)} → ${short(edge.toId)} (via ${edge.meta?.via || 'egress'}) ${pm ? ` ${pm.fromPort||'-'}→${pm.toPort||'-'}` : ''}` : `${short(edge.fromId)} → ${short(edge.toId)} ${edge.port ? ':'+edge.port : ''}`;
    setHoverInfo({ type:'edge', edge, idx, x, y, content });
    setHighlightedEdgeId(edgeId(edge, idx));
  }
  function onEdgeMouseLeave(){ setHoverInfo(null); setHighlightedEdgeId(null); }

  function onNodeMouseMove(node, ev) {
    const rect = containerRef.current && containerRef.current.getBoundingClientRect();
    const x = ev.clientX - (rect ? rect.left : 0) + 10;
    const y = ev.clientY - (rect ? rect.top : 0) + 10;
    const content = `${short(node.id)} (${node.type})`;
    setHoverInfo({ type:'node', node, x, y, content });
  }
  function onNodeMouseLeave(){ setHoverInfo(null); }

  function bezierD(x1,y1,x2,y2,offset=0){ const mx = (x1+x2)/2; const my = (y1+y2)/2 + offset; return `M ${x1} ${y1} Q ${mx} ${my} ${x2} ${y2}`; }

  return (
    <div style={{ padding: 12 }} ref={containerRef}>
      <div style={{ display: 'flex', gap: 12, marginBottom: 8 }}>
        <input value={namespace} onChange={e=>setNamespace(e.target.value)} style={{ padding: 6, borderRadius: 6 }} />
        <button onClick={() => load(namespace)} style={btnStyle}>Reload</button>

        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginLeft: 12 }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 6 }}><input type="checkbox" checked={showComposite} onChange={e=>setShowComposite(e.target.checked)} />Composite</label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 6 }}><input type="checkbox" checked={showOnlyProblems} onChange={e=>setShowOnlyProblems(e.target.checked)} />Problems only</label>
          <select value={protocolFilter} onChange={e=>setProtocolFilter(e.target.value)} style={{ padding: 6 }}>
            <option value="ALL">All protocols</option>
            <option value="HTTP">HTTP</option>
            <option value="TCP">TCP</option>
            <option value="TLS">TLS</option>
            <option value="COMPOSITE">COMPOSITE</option>
          </select>
          <input placeholder="search node" value={search} onChange={e=>setSearch(e.target.value)} style={{ padding: 6, marginLeft: 6 }} />
        </div>

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
          <div style={{ fontSize: 13, color: '#6b7280', alignSelf: 'center' }}>Nodes: {nodes.length} • Edges: {edges.length}</div>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 12 }}>
        <div style={{ flex: 1, background: '#fff', borderRadius: 8, padding: 12 }}>
          <svg viewBox={`0 0 ${width} ${height}`} width="100%" height="650" style={{ background: '#f8fafc' }}>

            <text x={leftX} y={28} fontSize={16} fontWeight={600}>mesh</text>
            <text x={centerX} y={28} fontSize={16} fontWeight={600}>egress</text>
            <text x={rightX} y={28} fontSize={16} fontWeight={600}>external / SE</text>

            {/* mesh nodes */}
            {meshNodes.map((svc,i) => {
              const y = yFor(i, meshNodes.length);
              return (
                <g key={svc.id} transform={`translate(${leftX - 120}, ${y - 24})`} style={{ cursor: 'pointer' }}
                   onMouseMove={(ev)=>onNodeMouseMove(svc, ev)} onMouseLeave={onNodeMouseLeave} onClick={()=>setSelectedNode(svc.id)}>
                  <rect width={240} height={48} rx={10} fill="#e0f2fe" stroke="#0369a1" />
                  <text x={12} y={22} fontSize={12} fill="#075985">{short(svc.id)}</text>
                </g>
              );
            })}

            {/* egress node */}
            <g transform={`translate(${centerX - 80}, ${height/2 - 40})`} style={{ cursor: 'pointer' }}
               onMouseMove={(ev)=>onNodeMouseMove({ id: 'egress:istio-egressgateway.istio-system.svc.cluster.local', type: 'EGRESS' }, ev)} onMouseLeave={onNodeMouseLeave} onClick={()=>setSelectedNode('egress:istio-egressgateway.istio-system.svc.cluster.local')}>
              <rect width={160} height={80} rx={12} fill="#d1fae5" stroke="#065f46" />
              <text x={80} y={44} fontSize={12} fill="#064e3b" textAnchor="middle">egress-gateway</text>
            </g>

            {/* service entries */}
            {seNodes.map((n,i) => {
              const y = yFor(i, Math.max(1, seNodes.length));
              return (
                <g key={n.id} transform={`translate(${rightX - 90}, ${y - 18})`} style={{ cursor: 'pointer' }}
                   onMouseMove={(ev)=>onNodeMouseMove(n, ev)} onMouseLeave={onNodeMouseLeave} onClick={()=>setSelectedNode(n.id)}>
                  <rect x={-80} width={160} height={36} rx={8} fill="#fff7ed" stroke="#f97316" />
                  <text x={0} y={22} fontSize={11} fill="#92400e" textAnchor="middle">{short(n.id)}</text>
                </g>
              );
            })}

            {/* externals */}
            {externalNodes.map((n,i) => {
              const y = yFor(i + seNodes.length, Math.max(1, externalNodes.length + seNodes.length));
              return (
                <g key={n.id} transform={`translate(${rightX}, ${y})`} style={{ cursor: 'pointer' }}
                   onMouseMove={(ev)=>onNodeMouseMove(n, ev)} onMouseLeave={onNodeMouseLeave} onClick={()=>setSelectedNode(n.id)}>
                  <circle cx={0} cy={0} r={16} fill="#fff1f2" stroke="#b91c1c" />
                  <text x={0} y={4} fontSize={10} fill="#7f1d1d" textAnchor="middle">{short(n.id)}</text>
                </g>
              );
            })}

            {/* non-composite edges (solid) */}
            {normalEdges.map((e, idx) => {
              try {
                const from = nodeById(e.fromId); const to = nodeById(e.toId);
                const fromX = from && from.type === 'K8S_SERVICE' ? leftX + 120 : (from && from.type === 'EGRESS' ? centerX : centerX);
                const toX = to && to.type === 'EXTERNAL' ? rightX - 40 : (to && to.type === 'SERVICEENTRY' ? rightX - 90 : centerX + 80);
                const fromIdx = meshNodes.findIndex(n => n.id === e.fromId);
                const toIdx = externalNodes.findIndex(n => n.id === e.toId);
                const seIdx = seNodes.findIndex(n => n.id === e.toId);
                const fromY = fromIdx >= 0 ? yFor(fromIdx, meshNodes.length) : height/2;
                const toY = toIdx >= 0 ? yFor(toIdx + seNodes.length, Math.max(1, externalNodes.length + seNodes.length)) : (seIdx>=0 ? yFor(seIdx, Math.max(1, seNodes.length)) : height/2);
                const color = e.port ? (portColorMap.get(String(e.port)) || '#374151') : '#374151';
                const id = edgeId(e, idx);
                const isHighlighted = id === highlightedEdgeId || id === selectedEdgeId;
                return (
                  <g key={id} onMouseMove={(ev)=>onEdgeMouseMove(e, idx, ev)} onMouseLeave={onEdgeMouseLeave} onClick={()=>setSelectedEdgeId(id)}>
                    <line x1={fromX} y1={fromY} x2={toX} y2={toY} stroke={color} strokeWidth={isHighlighted?3:1.5} strokeOpacity={isHighlighted?1:0.9} />
                  </g>
                );
              } catch (err) { return null; }
            })}

            {/* composite edges (dashed) */}
            {compositeEdges.map((e, idx) => {
              try {
                const fromIdx = meshNodes.findIndex(n => n.id === e.fromId);
                const toIdx = externalNodes.findIndex(n => n.id === e.toId);
                const seIdx = seNodes.findIndex(n => n.id === e.toId);
                const fromY = fromIdx >= 0 ? yFor(fromIdx, meshNodes.length) : height/2;
                const toY = toIdx >= 0 ? yFor(toIdx + seNodes.length, Math.max(1, externalNodes.length + seNodes.length)) : (seIdx>=0 ? yFor(seIdx, Math.max(1, seNodes.length)) : height/2);
                const fromX = leftX + 120; const toX = rightX - 110;
                const midX = (fromX + toX) / 2;
                const offset = (idx % 3) * 18 - 18;
                const d = `M ${fromX} ${fromY} Q ${midX} ${Math.max(60, (fromY + toY) / 2 + offset)} ${toX} ${toY}`;
                const pm = e.meta && e.meta.portMap ? e.meta.portMap : {};
                const label = pm.fromPort || pm.toPort ? `${pm.fromPort||'-'} → ${pm.toPort||'-'}` : 'via egress';
                const color = (pm.fromPort && portColorMap.get(String(pm.fromPort))) || '#6b7280';
                const id = edgeId(e, idx);
                const isHighlighted = id === highlightedEdgeId || id === selectedEdgeId;
                return (
                  <g key={id} onMouseMove={(ev)=>onEdgeMouseMove(e, idx, ev)} onMouseLeave={onEdgeMouseLeave} onClick={()=>setSelectedEdgeId(id)}>
                    <path d={d} fill="none" stroke={color} strokeWidth={isHighlighted?3:2} strokeDasharray="6,4" strokeOpacity={isHighlighted?1:0.95} />
                    <text x={midX} y={(fromY + toY) / 2 + offset - 6} fontSize={11} textAnchor="middle" fill={color}>{label}</text>
                  </g>
                );
              } catch (err) { return null; }
            })}

          </svg>

          {/* Tooltip */}
          {hoverInfo && (
            <div style={{ position: 'absolute', left: hoverInfo.x, top: hoverInfo.y, background: '#111827', color: '#fff', padding: '6px 8px', borderRadius: 6, fontSize: 12, pointerEvents: 'none', zIndex: 9999 }}>
              {hoverInfo.content}
            </div>
          )}

        </div>

        <div style={{ width: 420 }}>
          <div style={{ background: '#fff', borderRadius: 8, padding: 12 }}>
            <TopologyDetailsPanel namespace={namespace} nodeId={selectedNode} onClose={()=>setSelectedNode(null)} />

            <div style={{ marginTop: 12 }}>
              <h4 style={{ margin: 0, fontSize: 13 }}>Selected Edge</h4>
              <div style={{ fontSize: 12, color: '#374151', marginTop: 6 }}>
                {selectedEdgeId ? <div style={{ wordBreak: 'break-word' }}>Edge: {selectedEdgeId}</div> : <div className="text-xs" style={{ color: '#9ca3af' }}>No edge selected (click edge to select)</div>}
              </div>
              {selectedEdgeId && (
                <div style={{ marginTop: 6 }}><button style={btnStyle} onClick={() => { setSelectedEdgeId(null); setHighlightedEdgeId(null); }}>Clear selection</button></div>
              )}
            </div>

          </div>
        </div>

      </div>
    </div>
  );
}

const btnStyle = { padding: '6px 10px', borderRadius: 6, background: '#2563eb', color: '#fff', cursor: 'pointer' };
