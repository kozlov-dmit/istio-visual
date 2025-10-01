import React, { useEffect, useState } from 'react';

// Simple SVG canvas layout modeled on istio-visual.pdf
// Nodes: mesh services (left column), egress gateway (center), external hosts (right column)

export default function TopologyCanvas({ initialNamespace = 'default' }) {
  const [namespace, setNamespace] = useState(initialNamespace);
  const [topology, setTopology] = useState(null);
  const [selected, setSelected] = useState(null);
  const [details, setDetails] = useState(null);
  const width = 1100, height = 600;

  useEffect(() => load(namespace), []);

  async function load(ns) {
    const res = await fetch(`/api/topology/diagnose?namespace=${encodeURIComponent(ns)}`);
    const body = await res.json();
    setTopology(body);
  }

  async function loadDetails(nodeId) {
    setDetails(null);
    try {
      const res = await fetch(`/api/topology/details?namespace=${encodeURIComponent(namespace)}&nodeId=${encodeURIComponent(nodeId)}`);
      const body = await res.json();
      setDetails(body);
    } catch (e) {
      setDetails({ error: e.message });
    }
  }

  if (!topology) return <div className="p-6">Loading topology...</div>;

  // heuristics: select some nodes to show in mesh/egress/external columns
  const nodes = topology.nodes || [];
  const edges = topology.edges || [];

  const meshNodes = nodes.filter(n => n.type === 'K8S_SERVICE');
  const seNodes = nodes.filter(n => n.type === 'SERVICEENTRY' || n.id.startsWith('serviceentry:'));
  const externalNodes = nodes.filter(n => n.id.startsWith('external:'));

  // layout positions
  const leftX = 140, centerX = 520, rightX = 920;
  function yFor(i, total) { return 80 + (i * (height - 160) / Math.max(1, total - 1)); }

  return (
    <div className="p-4">
      <div className="flex gap-4">
        <div className="flex-1 bg-white rounded shadow p-3">
          <div className="flex items-center gap-2 mb-2">
            <input value={namespace} onChange={e=>setNamespace(e.target.value)} className="border px-2 py-1 rounded" />
            <button className="px-3 py-1 bg-indigo-600 text-white rounded" onClick={()=>load(namespace)}>Load</button>
            <div className="ml-auto text-sm text-gray-500">Nodes: {nodes.length} • Edges: {edges.length}</div>
          </div>

          <svg viewBox={`0 0 ${width} ${height}`} width="100%" height="480" style={{ background: '#f9fafb' }}>
            {/* Titles */}
            <text x={leftX} y={30} fontSize={16} fontWeight={600}>mesh</text>
            <text x={centerX} y={30} fontSize={16} fontWeight={600}>egress</text>
            <text x={rightX} y={30} fontSize={16} fontWeight={600}>external / ServiceEntries</text>

            {/* Mesh nodes */}
            {meshNodes.map((n, i) => {
              const y = yFor(i, meshNodes.length);
              return (
                <g key={n.id} onClick={() => { setSelected(n.id); loadDetails(n.id); }} style={{ cursor: 'pointer' }}>
                  <circle cx={leftX} cy={y} r={28} fill="#0ea5e9" />
                  <text x={leftX} y={y} textAnchor="middle" dy={5} fontSize={12} fill="#fff">{short(n.id)}</text>
                  <text x={leftX} y={y+22} textAnchor="middle" dy={0} fontSize={10} fill="#0f172a">{n.type}</text>
                </g>
              );
            })}

            {/* Egress gateway (single visual) */}
            <g onClick={() => { setSelected('egress-gateway'); setDetails({ nodeId: 'egress-gateway', info: 'Egress gateway (visual)' }); }} style={{ cursor: 'pointer' }}>
              <rect x={centerX-60} y={height/2-40} width={120} height={80} rx={10} fill="#c7f9cc" stroke="#059669" />
              <text x={centerX} y={height/2} textAnchor="middle" fontSize={12} fill="#064e3b">egress-gateway</text>
            </g>

            {/* External / ServiceEntry nodes */}
            {seNodes.map((n,i) => {
              const y = yFor(i, Math.max(1, seNodes.length));
              return (
                <g key={n.id} onClick={() => { setSelected(n.id); loadDetails(n.id); }} style={{ cursor: 'pointer' }}>
                  <rect x={rightX-80} y={y-18} width={160} height={36} rx={6} fill="#fff7ed" stroke="#f97316" />
                  <text x={rightX} y={y} textAnchor="middle" fontSize={11} fill="#9a3412">{short(n.id)}</text>
                </g>
              );
            })}

            {externalNodes.map((n,i) => {
              const y = yFor(i + (seNodes.length), Math.max(1, externalNodes.length + seNodes.length));
              return (
                <g key={n.id} onClick={() => { setSelected(n.id); loadDetails(n.id); }} style={{ cursor: 'pointer' }}>
                  <circle cx={rightX} cy={y} r={18} fill="#fff1f2" stroke="#ef4444" />
                  <text x={rightX} y={y} textAnchor="middle" dy={4} fontSize={10} fill="#991b1b">{short(n.id)}</text>
                </g>
              );
            })}

            {/* Simple links (mesh -> egress -> external) - draw straight lines for routes present in edges */}
            {edges.map((e, idx) => {
              const fromNode = nodes.find(n => n.id === e.fromId);
              const toNode = nodes.find(n => n.id === e.toId);
              const fromX = fromNode ? (fromNode.type === 'K8S_SERVICE' ? leftX : centerX) : centerX;
              const toX = toNode ? (toNode.type === 'K8S_SERVICE' ? leftX : (toNode.id.startsWith('external:') ? rightX : rightX - 40)) : rightX;
              // find y positions
              const fromIdx = meshNodes.findIndex(n => n.id === e.fromId);
              const fromY = fromIdx >= 0 ? yFor(fromIdx, meshNodes.length) : height/2;
              const toIdx = externalNodes.findIndex(n => n.id === e.toId);
              const seIdx = seNodes.findIndex(n => n.id === e.toId);
              const toY = toIdx >= 0 ? yFor(toIdx + seNodes.length, Math.max(1, externalNodes.length + seNodes.length)) : (seIdx>=0 ? yFor(seIdx, Math.max(1, seNodes.length)) : height/2);
              const color = (e.diagnostics || []).some(d => d.severity === 'ERROR') ? '#dc2626' : ( (e.diagnostics || []).some(d=>d.severity==='WARN') ? '#f97316' : '#374151' );
              return <line key={idx} x1={fromX+40} y1={fromY} x2={toX-40} y2={toY} stroke={color} strokeWidth={1.5} strokeOpacity={0.9} />;
            })}

          </svg>

        </div>

        <div style={{ width: 380 }}>
          <div className="bg-white rounded shadow p-3 sticky top-6">
            <h3 className="font-semibold">Details panel</h3>
            <hr className="my-2" />
            {selected ? (
              <div>
                <div className="text-sm font-medium mb-2">Selected: {selected}</div>
                {details ? (
                  <pre className="text-xs bg-gray-50 p-2 rounded max-h-96 overflow-auto">{JSON.stringify(details, null, 2)}</pre>
                ) : (
                  <div>Loading details…</div>
                )}
              </div>
            ) : (
              <div className="text-sm text-gray-500">Click an object on diagram to see details (TLS, Sidecar, VS/DR/SE, EnvoyFilters).</div>
            )}

            <div className="mt-3">
              <h5 className="text-xs text-gray-600">Legend</h5>
              <div className="text-xs mt-1">
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
}

function short(id){
  if(!id) return '';
  return String(id).replace(/^external:/,'').replace(/^serviceentry:/,'').replace(/\.svc\.cluster\.local$/,'');
}