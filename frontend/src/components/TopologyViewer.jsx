import React, { useEffect, useRef, useState } from 'react';
import ForceGraph2D from 'react-force-graph-2d';

export default function TopologyViewer({ initialNamespace = 'default' }) {
  const [namespace, setNamespace] = useState(initialNamespace);
  const [graph, setGraph] = useState({ nodes: [], links: [] });
  const [diagnosticsReport, setDiagnosticsReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);
  const [probeResult, setProbeResult] = useState(null);
  const fgRef = useRef();

  useEffect(() => {
    fetchTopology(namespace);
  }, []);

  async function fetchTopology(ns) {
    setLoading(true); setError(null); setSelected(null);
    try {
      const res = await fetch(`/api/topology?namespace=${encodeURIComponent(ns)}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      const nodes = (data.nodes || []).map(n => ({ id: n.id, label: n.id, type: n.type, meta: n.meta || {} }));
      const links = (data.edges || []).map(e => ({
        source: e.fromId, target: e.toId, protocol: e.protocol, port: e.port, notes: e.notes || [], weights: e.weights || {}, diagnostics: e.diagnostics || []
      }));
      setGraph({ nodes, links });
      setTimeout(() => fgRef.current && fgRef.current.zoomToFit(400, 50), 200);
    } catch (err) {
      setError(err.message); setGraph({ nodes: [], links: [] });
    } finally { setLoading(false); }
  }

  async function runDiagnose() {
    setLoading(true); setDiagnosticsReport(null);
    try {
      const res = await fetch(`/api/topology/diagnose?namespace=${encodeURIComponent(namespace)}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setDiagnosticsReport(data);
      // merge diagnostics to graph links for coloring
      const diagMap = new Map();
      (data.edges || []).forEach(e => {
        const key = `${e.fromId}>>>${e.toId}`;
        diagMap.set(key, e.diagnostics || []);
      });
      setGraph(g => ({ ...g, links: g.links.map(l => ({ ...l, diagnostics: diagMap.get(`${l.source}>>>${l.target}`) || [] })) }));
    } catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }

  async function runProbe(fromPod, host, port) {
    setProbeResult(null);
    try {
      const res = await fetch('/api/probe', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ namespace, fromPod, host, port })
      });
      const data = await res.json();
      setProbeResult(data);
    } catch (err) { setProbeResult({ error: err.message }); }
  }

  const nodeCanvasObject = (node, ctx, globalScale) => {
    const label = node.id;
    const fontSize = 12 / globalScale;
    ctx.font = `${fontSize}px Sans-Serif`;
    const textWidth = ctx.measureText(label).width;
    const bBoxPadding = 6 / globalScale;
    const color = node.type === 'K8S_SERVICE' ? '#0ea5e9' : node.type === 'EXTERNAL' ? '#f97316' : '#a78bfa';

    ctx.fillStyle = color; ctx.beginPath(); ctx.arc(node.x, node.y, 6 / globalScale, 0, 2 * Math.PI, false); ctx.fill();
    ctx.fillStyle = 'rgba(255,255,255,0.85)';
    ctx.fillRect(node.x - textWidth / 2 - bBoxPadding, node.y + 8 / globalScale, textWidth + bBoxPadding * 2, fontSize + bBoxPadding);
    ctx.fillStyle = '#111827'; ctx.textAlign = 'left'; ctx.textBaseline = 'top'; ctx.fillText(label, node.x - textWidth / 2, node.y + 8 / globalScale + 0.5 / globalScale);
  };

  const linkWidth = link => 1 + (Object.values(link.weights || {}).reduce((s, v) => s + (v || 0), 0) / 100);
  const linkColor = link => {
    const diags = link.diagnostics || [];
    if (diags.some(d => d.severity === 'ERROR')) return 'rgba(220,38,38,0.9)';
    if (diags.some(d => d.severity === 'WARN')) return 'rgba(234,88,12,0.9)';
    return 'rgba(107,114,128,0.6)';
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-6xl mx-auto bg-white shadow-md rounded-lg overflow-hidden">
        <div className="p-4 border-b flex items-center gap-4">
          <div className="flex items-center gap-2">
            <label className="text-sm text-gray-600">Namespace</label>
            <input className="border px-2 py-1 rounded-md text-sm" value={namespace} onChange={(e) => setNamespace(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && fetchTopology(namespace)} />
            <button className="ml-2 px-3 py-1 rounded bg-indigo-600 text-white text-sm" onClick={() => fetchTopology(namespace)}>{loading ? 'Loading...' : 'Load'}</button>
            <button className="ml-2 px-3 py-1 rounded bg-amber-600 text-white text-sm" onClick={runDiagnose}>Run Diagnose</button>
          </div>

          <div className="ml-auto text-sm text-gray-500">Nodes: {graph.nodes.length} • Edges: {graph.links.length}</div>
        </div>

        <div className="grid grid-cols-4 gap-4 p-4">
          <div className="col-span-3 h-[640px] border rounded-md relative">
            {graph.nodes.length === 0 && !loading ? (<div className="absolute inset-0 flex items-center justify-center text-gray-400">No data — load a namespace</div>) : (
              <ForceGraph2D ref={fgRef} graphData={graph} nodeLabel={n => `${n.id} (${n.type})`} nodeCanvasObject={nodeCanvasObject} nodePointerAreaPaint={(node, color, ctx) => { ctx.fillStyle = color; ctx.beginPath(); ctx.arc(node.x, node.y, 8, 0, 2 * Math.PI, false); ctx.fill(); }} linkWidth={link => linkWidth(link)} linkDirectionalArrowLength={3} linkDirectionalArrowRelPos={1} linkColor={linkColor} onNodeClick={node => setSelected({ type: 'node', data: node })} onLinkClick={link => setSelected({ type: 'link', data: link })} />
            )}
          </div>

          <div className="col-span-1">
            <div className="sticky top-4 p-3 bg-white border rounded-md h-[600px] overflow-auto">
              <h3 className="font-semibold">Details</h3>
              <hr className="my-2" />
              {selected ? (
                <div>
                  <div className="text-xs text-gray-500 mb-2">Selected: {selected.type}</div>
                  <pre className="text-xs bg-gray-100 p-2 rounded text-gray-700">{JSON.stringify(selected.data, null, 2)}</pre>

                  {selected.type === 'link' && (selected.data.diagnostics || []).length > 0 && (
                    <div className="mt-2">
                      <h4 className="font-medium">Diagnostics</h4>
                      <ul className="space-y-2 mt-2 text-sm">
                        {selected.data.diagnostics.map((d,i) => (
                          <li key={i} className="p-2 rounded border" style={{ borderLeft: d.severity === 'ERROR' ? '4px solid #dc2626' : d.severity === 'WARN' ? '4px solid #f97316' : '4px solid #06b6d4' }}>
                            <div className="font-semibold">{d.code} — {d.severity}</div>
                            <div className="text-xs text-gray-600">{d.message}</div>
                            <pre className="mt-2 text-xs bg-gray-50 p-2 rounded">{d.suggestion}</pre>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}

                </div>
              ) : (
                <div className="text-sm text-gray-500">Click a node or a link to see details. Run Diagnose to populate diagnostics.</div>
              )}

              <div className="mt-4">
                <h4 className="text-sm font-medium">Diagnostics Report</h4>
                <pre className="text-xs bg-gray-100 p-2 rounded mt-2">{diagnosticsReport ? JSON.stringify(diagnosticsReport.summary, null, 2) : 'No report yet'}</pre>
              </div>

              <div className="mt-4">
                <h4 className="text-sm font-medium">Run probe (from pod)</h4>
                <form onSubmit={(e) => { e.preventDefault(); const fd = new FormData(e.target); runProbe(fd.get('fromPod'), fd.get('host'), fd.get('port')); }}>
                  <input name="fromPod" placeholder="fromPod (optional)" className="w-full border px-2 py-1 rounded text-sm my-1" />
                  <input name="host" placeholder="host (e.g. kafka.example.com)" className="w-full border px-2 py-1 rounded text-sm my-1" />
                  <input name="port" placeholder="port" className="w-full border px-2 py-1 rounded text-sm my-1" />
                  <button type="submit" className="w-full mt-2 px-3 py-1 rounded bg-emerald-600 text-white text-sm">Run Probe</button>
                </form>
                {probeResult && (<pre className="text-xs bg-gray-50 p-2 rounded mt-2">{JSON.stringify(probeResult, null, 2)}</pre>)}
              </div>

            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
