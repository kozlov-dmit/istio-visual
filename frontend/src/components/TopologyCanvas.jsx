// src/components/TopologyCanvasImproved.jsx
import React, { useEffect, useRef, useState } from 'react';

/**
 * TopologyCanvasImproved
 * - zoom + pan controls
 * - better layout spacing to avoid overlaps
 * - mesh node shows services and pods (from /api/topology/pods)
 * - edges to egress broken down by port and colored
 * - safe fetch with AbortController + isMounted guard
 *
 * Expects backend endpoints:
 *  GET /api/topology/diagnose?namespace=...
 *  GET /api/topology/details?namespace=...&nodeId=...
 *  GET /api/topology/pods?namespace=...
 */

const COLORS = [
  '#ef4444', '#f97316', '#f59e0b', '#eab308', '#84cc16',
  '#10b981', '#06b6d4', '#3b82f6', '#6366f1', '#8b5cf6',
  '#ec4899', '#f43f5e'
];

function colorForPort(port, map) {
  if (map.has(String(port))) return map.get(String(port));
  const idx = map.size % COLORS.length;
  const c = COLORS[idx];
  map.set(String(port), c);
  return c;
}

function short(id) {
  if (!id) return '';
  return String(id).replace(/^external:/, '').replace(/^serviceentry:/, '').replace(/\.svc\.cluster\.local$/, '');
}

export default function TopologyCanvas({ initialNamespace = 'default' }) {
  const [namespace, setNamespace] = useState(initialNamespace);
  const [topology, setTopology] = useState(null);
  const [podsInfo, setPodsInfo] = useState(null); // { podsByService: { svcName: [...] }, services: [...] }
  const [details, setDetails] = useState(null);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState(null);

  // transform state
  const [scale, setScale] = useState(1.0);
  const [tx, setTx] = useState(0);
  const [ty, setTy] = useState(0);

  const controllerRef = useRef(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    loadAll(namespace);
    return () => {
      mountedRef.current = false;
      if (controllerRef.current && typeof controllerRef.current.abort === 'function') {
        try { controllerRef.current.abort(); } catch (e) {}
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadAll(ns) {
    if (controllerRef.current && typeof controllerRef.current.abort === 'function') {
      try { controllerRef.current.abort(); } catch (e) {}
    }
    const ctrl = new AbortController();
    controllerRef.current = ctrl;
    setError(null);
    setTopology(null);
    setPodsInfo(null);
    setDetails(null);
    setSelected(null);

    try {
      const [tRes, pRes] = await Promise.allSettled([
        fetch(`/api/topology/diagnose?namespace=${encodeURIComponent(ns)}`, { signal: ctrl.signal }),
        fetch(`/api/topology/pods?namespace=${encodeURIComponent(ns)}`, { signal: ctrl.signal })
      ]);

      if (!mountedRef.current) return;

      if (tRes.status === 'rejected') throw tRes.reason;
      if (!tRes.value.ok) {
        const txt = await tRes.value.text().catch(()=>'');
        throw new Error(`Topology API error: ${tRes.value.status} ${txt}`);
      }
      const topoJson = await tRes.value.json();
      setTopology(topoJson);

      if (pRes.status === 'fulfilled' && pRes.value.ok) {
        const podsJson = await pRes.value.json();
        setPodsInfo(podsJson);
      } else {
        // pods optional - continue gracefully
        setPodsInfo({ podsByService: {}, services: [] });
      }
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.error('loadAll error', err);
      if (mountedRef.current) setError(String(err));
    }
  }

  async function loadDetails(nodeId) {
    if (!nodeId) return;
    setDetails(null);
    try {
      const res = await fetch(`/api/topology/details?namespace=${encodeURIComponent(namespace)}&nodeId=${encodeURIComponent(nodeId)}`);
      if (!res.ok) {
        const txt = await res.text().catch(()=>'');
        throw new Error(`details ${res.status} ${txt}`);
      }
      const body = await res.json();
      if (!mountedRef.current) return;
      setDetails(body);
    } catch (err) {
      console.error('loadDetails error', err);
      if (mountedRef.current) setDetails({ error: String(err) });
    }
  }

  function handleNodeClick(nodeId) {
    setSelected(nodeId);
    loadDetails(nodeId);
  }

  function zoomIn() { setScale(s => Math.min(3, +(s + 0.2).toFixed(2))); }
  function zoomOut(){ setScale(s => Math.max(0.3, +(s - 0.2).toFixed(2))); }
  function resetView(){ setScale(1); setTx(0); setTy(0); }

  // layout parameters
  const width = 1200, height = 700;
  const leftX = 160, centerX = 600, rightX = 1040;

  // defensive
  const nodes = Array.isArray(topology?.nodes) ? topology.nodes : [];
  const edges = Array.isArray(topology?.edges) ? topology.edges : [];
  const meshNodes = nodes.filter(n => n?.type === 'K8S_SERVICE');
  const seNodes = nodes.filter(n => n?.type === 'SERVICEENTRY' || (n?.id && String(n.id).startsWith('serviceentry:')));
  const externalNodes = nodes.filter(n => n?.id && String(n.id).startsWith('external:'));

  // avoid overlap: compute dynamic spacing (min spacing grows if many nodes)
  function yFor(i, total, marginTop=80, marginBottom=100) {
    const avail = height - marginTop - marginBottom;
    const spacing = Math.max(40, Math.floor(avail / Math.max(1, total - 1)));
    return marginTop + i * spacing;
  }

  // group edges mesh->egress and split by port
  const meshToRightEdges = edges.filter(e => {
    // consider edges where from is mesh (k8s svc) and to is external or serviceentry or egress
    const fromIsMesh = meshNodes.some(n => n.id === e.fromId);
    const toIsRight = externalNodes.some(n => n.id === e.toId) || seNodes.some(n => n.id === e.toId);
    return fromIsMesh && toIsRight;
  });

  // create mapping of port -> color
  const portColorMap = new Map();
  meshToRightEdges.forEach(e => { colorForPort(e.port || 'default', portColorMap); });

  // group edges by (fromId,toId,port)
  const grouped = {};
  meshToRightEdges.forEach(e => {
    const key = `${e.fromId}:::${e.toId}:::${e.port || ''}`;
    if (!grouped[key]) grouped[key] = [];
    grouped[key].push(e);
  });
  const groupedList = Object.entries(grouped).map(([k, arr]) => {
    const parts = k.split(':::');
    return { fromId: parts[0], toId: parts[1], port: parts[2] || null, edges: arr };
  });

  // helper to draw bezier path with offset to separate multiple ports
  function bezierPath(x1, y1, x2, y2, offset=0) {
    // control points offset horizontally and vertically
    const mx = (x1 + x2) / 2;
    const my = (y1 + y2) / 2 + offset;
    return `M ${x1} ${y1} Q ${mx} ${my} ${x2} ${y2}`;
  }

  // improved legend content: list ports + colors + counts
  const legendItems = Array.from(portColorMap.keys()).map(port => ({
    port,
    color: portColorMap.get(port),
    count: groupedList.filter(g => String(g.port) === String(port)).length
  }));

  return (
    <div style={{ padding: 12 }}>
      <div style={{ display: 'flex', gap: 12, marginBottom: 8 }}>
        <input value={namespace} onChange={e => setNamespace(e.target.value)} style={{ padding: 6, borderRadius: 6 }} />
        <button onClick={() => loadAll(namespace)} style={{ padding: '6px 10px', background: '#2563eb', color: '#fff', borderRadius: 6 }}>Reload</button>

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 8, alignItems: 'center' }}>
          <button onClick={zoomOut} title="Zoom out" style={btnStyle}>−</button>
          <div style={{ width: 64, textAlign: 'center' }}>{Math.round(scale * 100)}%</div>
          <button onClick={zoomIn} title="Zoom in" style={btnStyle}>+</button>
          <button onClick={resetView} title="Reset view" style={btnStyle}>Reset</button>
        </div>
      </div>

      {error ? (
        <div style={{ padding: 12, background: '#fff5f5', borderRadius: 6 }}>{String(error)}</div>
      ) : (
        <div style={{ display: 'flex', gap: 12 }}>
          <div style={{ flex: 1, background: '#fff', borderRadius: 8, padding: 12 }}>
            <svg viewBox={`0 0 ${width} ${height}`} width="100%" height="650" style={{ background: '#f8fafc', overflow: 'visible' }}>
              <g transform={`translate(${tx},${ty}) scale(${scale})`}>
                <text x={leftX} y={28} fontSize={16} fontWeight={600}>mesh</text>
                <text x={centerX} y={28} fontSize={16} fontWeight={600}>egress</text>
                <text x={rightX} y={28} fontSize={16} fontWeight={600}>external / ServiceEntries</text>

                {/* Mesh column with services and pods */}
                {meshNodes.map((svc, i) => {
                  const y = yFor(i, meshNodes.length);
                  // display as a rectangle with service name and small pod badges underneath (if any)
                  const pods = podsInfo?.podsByService?.[String(svc?.id || svc?.meta?.svcName)] || podsInfo?.podsByService?.[svc?.meta?.svcName] || [];
                  const rectW = 220;
                  const rectH = Math.max(28, 18 + (pods.length > 0 ? Math.min(6, pods.length) * 14 : 0));
                  return (
                    <g key={svc.id} transform={`translate(${leftX - rectW / 2}, ${y - rectH / 2})`} style={{ cursor: 'pointer' }} onClick={() => handleNodeClick(svc.id)}>
                      <rect width={rectW} height={rectH} rx={8} fill="#e0f2fe" stroke="#0369a1" />
                      <text x={12} y={16} fontSize={12} fill="#075985" style={{ fontWeight: 600 }}>{ short(svc.id) }</text>
                      {/* pods list */}
                      <g transform={`translate(12, 28)`}>
                        {pods.slice(0, 6).map((p, idx) => (
                          <g key={p.name} transform={`translate(${(idx % 3) * 70}, ${Math.floor(idx/3) * 14})`}>
                            <rect width={66} height={12} rx={4} fill="#ffffff" stroke="#c7d2fe" />
                            <text x={6} y={9} fontSize={10} fill="#374151">{p.name.replace(/^(.{12}).*$/, '$1')}</text>
                          </g>
                        ))}
                        {pods.length > 6 && <text x={0} y={34} fontSize={10} fill="#6b7280">+{pods.length - 6} more</text>}
                      </g>
                    </g>
                  );
                })}

                {/* Egress visual (box) */}
                <g transform={`translate(${centerX - 80}, ${height/2 - 40})`} style={{ cursor: 'pointer' }} onClick={() => handleNodeClick('egress-gateway')}>
                  <rect width={160} height={80} rx={12} fill="#d1fae5" stroke="#065f46" />
                  <text x={80} y={44} fontSize={12} fill="#064e3b" textAnchor="middle">egress-gateway</text>
                </g>

                {/* ServiceEntries and externals on right */}
                {seNodes.map((n, i) => {
                  const y = yFor(i, Math.max(1, seNodes.length));
                  const rectW = 180;
                  return (
                    <g key={n.id} transform={`translate(${rightX - rectW/2}, ${y - 18})`} style={{ cursor: 'pointer' }} onClick={() => handleNodeClick(n.id)}>
                      <rect x={-rectW/2} width={rectW} height={36} rx={8} fill="#fff7ed" stroke="#f97316" />
                      <text x={0} y={22} fontSize={11} fill="#92400e" textAnchor="middle">{short(n.id)}</text>
                    </g>
                  );
                })}

                {externalNodes.map((n, i) => {
                  const y = yFor(i + seNodes.length, Math.max(1, externalNodes.length + seNodes.length));
                  return (
                    <g key={n.id} transform={`translate(${rightX}, ${y})`} style={{ cursor: 'pointer' }} onClick={() => handleNodeClick(n.id)}>
                      <circle cx={0} cy={0} r={16} fill="#fff1f2" stroke="#b91c1c" />
                      <text x={0} y={4} fontSize={10} fill="#7f1d1d" textAnchor="middle">{ short(n.id) }</text>
                    </g>
                  );
                })}

                {/* Draw grouped bezier paths for different ports */}
                {groupedList.map((g, idx) => {
                  // find positions
                  const fromIdx = meshNodes.findIndex(n => n.id === g.fromId);
                  const toSIdx = seNodes.findIndex(n => n.id === g.toId);
                  const toEIdx = externalNodes.findIndex(n => n.id === g.toId);
                  const fromY = fromIdx >= 0 ? yFor(fromIdx, meshNodes.length) : height/2;
                  const toY = toEIdx >= 0 ? yFor(toEIdx + seNodes.length, Math.max(1, externalNodes.length + seNodes.length)) : (toSIdx >= 0 ? yFor(toSIdx, Math.max(1, seNodes.length)) : height/2);
                  const fromX = leftX + 110; // right side of service rect
                  const toX = rightX - 110;  // left of external/serviceentry rect
                  // compute index among edges with same (from,to) to offset lines
                  const peers = groupedList.filter(x => x.fromId === g.fromId && x.toId === g.toId);
                  const indexInPeers = peers.findIndex(x => x.port === g.port);
                  const offset = (indexInPeers - (peers.length - 1)/2) * 12; // vertical offset for separation

                  const color = colorForPort(g.port || 'default', portColorMap);
                  const path = bezierPath(fromX, fromY, toX, toY, offset);

                  return (
                    <g key={`${g.fromId}--${g.toId}--${g.port}`}>
                      <path d={path} fill="none" stroke={color} strokeWidth={2.0} strokeOpacity={0.95} />
                      {/* small label on mid point with port */}
                      <text x={(fromX + toX)/2} y={(fromY + toY)/2 + offset - 6} fontSize={10} textAnchor="middle" fill={color}>{g.port || 'default'}</text>
                    </g>
                  );
                })}

              </g>
            </svg>
          </div>

            <TopologyDetailsPanel namespace={currentNamespace} nodeId={selectedNodeId} onClose={() => setSelected(null)} />
          {/* <div style={{ width: 380 }}>
            <div style={{ background: '#fff', borderRadius: 8, padding: 12 }}>
              <h3 style={{ margin: 0 }}>Details</h3>
              <hr style={{ margin: '8px 0' }} />
              {selected ? (
                <div>
                  <div style={{ fontWeight: 700, marginBottom: 8 }}>{short(selected)}</div>
                  <div style={{ fontSize: 13, color: '#444' }}>Detail info (click nodes to load)</div>
                  <div style={{ marginTop: 8 }}>
                    {details ? (
                      <pre style={{ whiteSpace: 'pre-wrap', maxHeight: 380, overflow: 'auto', background: '#fbfbfb', padding: 8 }}>{JSON.stringify(details, null, 2)}</pre>
                    ) : (
                      <div style={{ color: '#6b7280' }}>Loading details…</div>
                    )}
                  </div>
                </div>
              ) : (
                <div style={{ color: '#6b7280' }}>Click a service / external node to see TLS, Sidecar, VS/DR/SE and recommendations.</div>
              )}

              <div style={{ marginTop: 12 }}>
                <h4 style={{ margin: 0, fontSize: 13 }}>Legend — ports & colors</h4>
                <div style={{ marginTop: 8, maxHeight: 220, overflow: 'auto' }}>
                  {legendItems.length === 0 ? <div className="text-xs text-gray-500">No mesh->egress ports found</div> : legendItems.map(li => (
                    <div key={li.port} style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 6 }}>
                      <div style={{ width: 18, height: 10, background: li.color, borderRadius: 3 }} />
                      <div style={{ fontSize: 13, color: '#111' }}>{li.port || '(default)'}</div>
                      <div style={{ marginLeft: 'auto', fontSize: 12, color: '#6b7280' }}>{li.count} path(s)</div>
                    </div>
                  ))}
                </div>
                <hr style={{ margin: '12px 0' }} />
                <div style={{ fontSize: 12, color: '#6b7280' }}>
                  Mesh node shows services and a few pods. Lines to the right are split by port (labelled). Use zoom to focus.
                </div>
              </div>
            </div>
          </div> */}

        </div>
      )}
    </div>
  );
}

const btnStyle = { padding: '6px 8px', background: '#f3f4f6', border: '1px solid #e5e7eb', borderRadius: 6, cursor: 'pointer' };
