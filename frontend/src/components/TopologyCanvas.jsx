import React, { useEffect, useRef, useState } from 'react';

/**
 * Safe Topology Canvas:
 * - uses AbortController to cancel fetches on unmount
 * - guards setState after unmount
 * - try/catch around async loads and rendering data defensively
 */

export default function TopologyCanvasSafe({ initialNamespace = 'default' }) {
  const [namespace, setNamespace] = useState(initialNamespace);
  const [topology, setTopology] = useState(null);
  const [selected, setSelected] = useState(null);
  const [details, setDetails] = useState(null);
  const [error, setError] = useState(null);
  const isMountedRef = useRef(true);
  const controllerRef = useRef(null);

  useEffect(() => {
    isMountedRef.current = true;
    // initial load
    loadTopology(namespace);

    return () => {
      // cleanup: mark unmounted and abort fetches
      isMountedRef.current = false;
      if (controllerRef.current && typeof controllerRef.current.abort === 'function') {
        try { controllerRef.current.abort(); } catch (e) { /* ignore */ }
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadTopology(ns) {
    // abort previous if any
    if (controllerRef.current && typeof controllerRef.current.abort === 'function') {
      try { controllerRef.current.abort(); } catch (e) { /* ignore */ }
    }
    const controller = new AbortController();
    controllerRef.current = controller;

    setError(null);
    setTopology(null);
    setSelected(null);
    setDetails(null);

    try {
      const res = await fetch(`/api/topology/diagnose?namespace=${encodeURIComponent(ns)}`, { signal: controller.signal });
      if (!res.ok) {
        const txt = await res.text().catch(() => '');
        throw new Error(`HTTP ${res.status} ${txt}`);
      }
      const body = await res.json();
      if (!isMountedRef.current) return;
      setTopology(body);
    } catch (err) {
      if (err.name === 'AbortError') {
        // expected on unmount or explicit abort; ignore
        return;
      }
      console.error('loadTopology error', err);
      if (isMountedRef.current) setError(String(err));
    }
  }

  async function loadDetails(nodeId) {
    if (!nodeId) return;
    // abort previous details fetch if any
    const controller = new AbortController();
    // keep only for this request; we don't reuse ref here to avoid cancelling topology fetch
    let aborted = false;
    try {
      const res = await fetch(`/api/topology/details?namespace=${encodeURIComponent(namespace)}&nodeId=${encodeURIComponent(nodeId)}`, { signal: controller.signal });
      if (!res.ok) {
        const txt = await res.text().catch(() => '');
        throw new Error(`HTTP ${res.status} ${txt}`);
      }
      const body = await res.json();
      if (!isMountedRef.current) return;
      setDetails(body);
    } catch (err) {
      if (err.name === 'AbortError') { aborted = true; }
      console.error('loadDetails error', err);
      if (!aborted && isMountedRef.current) setDetails({ error: String(err) });
    }
  }

  function handleNodeClick(nodeId) {
    if (!nodeId) return;
    setSelected(nodeId);
    setDetails(null);
    // schedule details load (safe)
    loadDetails(nodeId);
  }

  // defensive helpers
  const nodes = Array.isArray(topology?.nodes) ? topology.nodes : [];
  const edges = Array.isArray(topology?.edges) ? topology.edges : [];

  // simple layout params
  const width = 1100, height = 600;
  const leftX = 140, centerX = 520, rightX = 920;
  function yFor(i, total) { return 80 + (i * (height - 160) / Math.max(1, total - 1)); }

  // render guard
  if (error) {
    return (
      <div className="p-6">
        <div style={{ padding: 12, background: '#fff5f5', borderRadius: 6 }}>
          <h3>Ошибка загрузки данных</h3>
          <pre style={{ whiteSpace: 'pre-wrap' }}>{String(error)}</pre>
          <button onClick={() => loadTopology(namespace)} style={{ marginTop: 8 }}>Повторить</button>
        </div>
      </div>
    );
  }

  // Render main canvas; wrap risky parts in try/catch to avoid unhandled exceptions in Safari
  try {
    const meshNodes = nodes.filter(n => n?.type === 'K8S_SERVICE');
    const seNodes = nodes.filter(n => (n?.type === 'SERVICEENTRY' || (n?.id && String(n.id).startsWith('serviceentry:'))));
    const externalNodes = nodes.filter(n => n?.id && String(n.id).startsWith('external:'));

    return (
      <div style={{ padding: 16 }}>
        <div style={{ display: 'flex', gap: 12 }}>
          <div style={{ flex: 1, background: '#fff', borderRadius: 8, padding: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
              <input value={namespace} onChange={e => setNamespace(e.target.value)} style={{ padding: 6, borderRadius: 6 }} />
              <button onClick={() => loadTopology(namespace)} style={{ padding: '6px 10px', borderRadius: 6, background: '#2563eb', color: '#fff' }}>Load</button>
              <div style={{ marginLeft: 'auto', color: '#6b7280' }}>Nodes: {nodes.length} • Edges: {edges.length}</div>
            </div>

            <svg viewBox={`0 0 ${width} ${height}`} width="100%" height="480" style={{ background: '#f8fafc' }}>
              <text x={leftX} y={30} fontSize={16} fontWeight={600}>mesh</text>
              <text x={centerX} y={30} fontSize={16} fontWeight={600}>egress</text>
              <text x={rightX} y={30} fontSize={16} fontWeight={600}>external / ServiceEntries</text>

              {meshNodes.map((n, i) => {
                const y = yFor(i, meshNodes.length);
                return (
                  <g key={n.id} onClick={() => handleNodeClick(n.id)} style={{ cursor: 'pointer' }}>
                    <circle cx={leftX} cy={y} r={28} fill="#0ea5e9" />
                    <text x={leftX} y={y} textAnchor="middle" dy={5} fontSize={12} fill="#fff">{short(n.id)}</text>
                    <text x={leftX} y={y + 22} textAnchor="middle" dy={0} fontSize={10} fill="#0f172a">{n.type}</text>
                  </g>
                );
              })}

              <g onClick={() => { handleNodeClick('egress-gateway'); }} style={{ cursor: 'pointer' }}>
                <rect x={centerX - 60} y={height / 2 - 40} width={120} height={80} rx={10} fill="#c7f9cc" stroke="#059669" />
                <text x={centerX} y={height / 2} textAnchor="middle" fontSize={12} fill="#064e3b">egress-gateway</text>
              </g>

              {seNodes.map((n, i) => {
                const y = yFor(i, Math.max(1, seNodes.length));
                return (
                  <g key={n.id} onClick={() => handleNodeClick(n.id)} style={{ cursor: 'pointer' }}>
                    <rect x={rightX - 80} y={y - 18} width={160} height={36} rx={6} fill="#fff7ed" stroke="#f97316" />
                    <text x={rightX} y={y} textAnchor="middle" fontSize={11} fill="#9a3412">{short(n.id)}</text>
                  </g>
                );
              })}

              {externalNodes.map((n, i) => {
                const y = yFor(i + (seNodes.length), Math.max(1, externalNodes.length + seNodes.length));
                return (
                  <g key={n.id} onClick={() => handleNodeClick(n.id)} style={{ cursor: 'pointer' }}>
                    <circle cx={rightX} cy={y} r={18} fill="#fff1f2" stroke="#ef4444" />
                    <text x={rightX} y={y} textAnchor="middle" dy={4} fontSize={10} fill="#991b1b">{short(n.id)}</text>
                  </g>
                );
              })}

              {edges.map((e, idx) => {
                try {
                  const fromNode = nodes.find(n => n.id === e.fromId);
                  const toNode = nodes.find(n => n.id === e.toId);
                  const fromX = fromNode ? (fromNode.type === 'K8S_SERVICE' ? leftX : centerX) : centerX;
                  const toX = toNode ? (toNode.type === 'K8S_SERVICE' ? leftX : (toNode.id && String(toNode.id).startsWith('external:') ? rightX : rightX - 40)) : rightX;
                  const fromIdx = meshNodes.findIndex(n => n.id === e.fromId);
                  const fromY = fromIdx >= 0 ? yFor(fromIdx, meshNodes.length) : height / 2;
                  const toIdx = externalNodes.findIndex(n => n.id === e.toId);
                  const seIdx = seNodes.findIndex(n => n.id === e.toId);
                  const toY = toIdx >= 0 ? yFor(toIdx + seNodes.length, Math.max(1, externalNodes.length + seNodes.length)) : (seIdx >= 0 ? yFor(seIdx, Math.max(1, seNodes.length)) : height / 2);
                  const color = (e.diagnostics || []).some(d => d.severity === 'ERROR') ? '#dc2626' : ((e.diagnostics || []).some(d => d.severity === 'WARN') ? '#f97316' : '#374151');

                  return <line key={idx} x1={fromX + 40} y1={fromY} x2={toX - 40} y2={toY} stroke={color} strokeWidth={1.5} strokeOpacity={0.9} />;
                } catch (innerErr) {
                  // swallow per-edge errors to avoid crashing entire render
                  console.warn('edge render error', innerErr);
                  return null;
                }
              })}
            </svg>

          </div>

          <div style={{ width: 380 }}>
            <div style={{ background: '#fff', borderRadius: 8, padding: 12 }}>
              <h3 style={{ margin: 0 }}>Details panel</h3>
              <hr style={{ margin: '8px 0' }} />
              {selected ? (
                <div>
                  <div style={{ marginBottom: 8, fontWeight: 600 }}>Selected: {selected}</div>
                  {details ? (
                    <pre style={{ whiteSpace: 'pre-wrap', maxHeight: 360, overflow: 'auto', background: '#fbfbfb', padding: 8 }}>{JSON.stringify(details, null, 2)}</pre>
                  ) : (
                    <div>Loading details…</div>
                  )}
                </div>
              ) : (
                <div style={{ color: '#6b7280' }}>Click an object on diagram to see details (TLS, Sidecar, VS/DR/SE, EnvoyFilters).</div>
              )}

              <div style={{ marginTop: 12 }}>
                <h5 style={{ margin: 0, fontSize: 12, color: '#374151' }}>Legend</h5>
                <div style={{ fontSize: 12, color: '#6b7280', marginTop: 6 }}>
                  <div>● mesh service</div>
                  <div>■ egress gateway</div>
                  <div>▭ serviceentry</div>
                  <div>● external host</div>
                </div>
              </div>

            </div>
          </div>
        </div>
      </div>
    );
  } catch (renderErr) {
    console.error('TopologyCanvasSafe render error', renderErr);
    return (
      <div style={{ padding: 16 }}>
        <h3>Ошибка рендера схемы</h3>
        <pre style={{ whiteSpace: 'pre-wrap' }}>{String(renderErr)}</pre>
      </div>
    );
  }
}

function short(id) {
  if (!id) return '';
  return String(id).replace(/^external:/, '').replace(/^serviceentry:/, '').replace(/\.svc\.cluster\.local$/, '');
}
