import React, { useEffect, useRef, useState } from 'react';
import ForceGraph2D from 'react-force-graph-2d';

// Simple Tailwind-styled React component to display TopologyGraph
// Expects backend API: GET /api/topology?namespace=<ns>
// Response JSON shape:
// {
//   nodes: [{ id: string, type: string, meta: {...} }],
//   edges: [{ fromId: string, toId: string, protocol: string, port: number|null, matches: [], notes: [], weights: { ... } }]
// }

export default function TopologyViewer({ initialNamespace = 'default' }) {
  const [namespace, setNamespace] = useState(initialNamespace);
  const [graph, setGraph] = useState({ nodes: [], links: [] });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);
  const fgRef = useRef();

  console.log("init topology")
  useEffect(() => {
      console.log("init topology effect")
    fetchTopology(namespace);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function fetchTopology(ns) {
    console.log("fetch topology...")
    setLoading(true);
    setError(null);
    setSelected(null);
    try {
      const res = await fetch(`/api/topology?namespace=${encodeURIComponent(ns)}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      console.log("get response: " + data)
      // Normalize to force-graph format
      const nodes = (data.nodes || []).map(n => ({ id: n.id, label: n.id, type: n.type, meta: n.meta || {} }));
      const links = (data.edges || []).map(e => ({
        source: e.fromId,
        target: e.toId,
        protocol: e.protocol,
        port: e.port,
        notes: e.notes || [],
        weights: e.weights || {}
      }));
      setGraph({ nodes, links });
      // center camera after load
      setTimeout(() => fgRef.current && fgRef.current.zoomToFit(400, 50), 200);
    } catch (err) {
      setError(err.message);
      setGraph({ nodes: [], links: [] });
    } finally {
      setLoading(false);
    }
  }

  const handleFetch = () => fetchTopology(namespace);

  const nodeCanvasObject = (node, ctx, globalScale) => {
      console.log("nodeCanvasObject: " + node.id)
    const label = node.id;
    const fontSize = 12 / globalScale;
    ctx.font = `${fontSize}px Sans-Serif`;
    const textWidth = ctx.measureText(label).width;
    const bBoxPadding = 6 / globalScale;

    // color by type
    const color = node.type === 'K8S_SERVICE' ? '#0ea5e9' : node.type === 'EXTERNAL' ? '#f97316' : '#a78bfa';

    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.arc(node.x, node.y, 6 / globalScale, 0, 2 * Math.PI, false);
    ctx.fill();

    // draw label background
    ctx.fillStyle = 'rgba(255,255,255,0.85)';
    ctx.fillRect(node.x - textWidth / 2 - bBoxPadding, node.y + 8 / globalScale, textWidth + bBoxPadding * 2, fontSize + bBoxPadding);

    // draw text
    ctx.fillStyle = '#111827';
    ctx.textAlign = 'left';
    ctx.textBaseline = 'top';
    ctx.fillText(label, node.x - textWidth / 2, node.y + 8 / globalScale + 0.5 / globalScale);
  };

  const linkWidth = link => 1 + (Object.values(link.weights || {}).reduce((s, v) => s + (v || 0), 0) / 100);

    console.log("render topology")
  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-6xl mx-auto bg-white shadow-md rounded-lg overflow-hidden">
        <div className="p-4 border-b flex items-center gap-4">
          <div className="flex items-center gap-2">
            <label className="text-sm text-gray-600">Namespace</label>
            <input
              className="border px-2 py-1 rounded-md text-sm"
              value={namespace}
              onChange={(e) => setNamespace(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleFetch()}
            />
            <button className="ml-2 px-3 py-1 rounded bg-indigo-600 text-white text-sm" onClick={handleFetch}>
              {loading ? 'Loading...' : 'Load'}
            </button>
          </div>

          <div className="ml-auto text-sm text-gray-500">Nodes: {graph.nodes.length} • Edges: {graph.links.length}</div>
        </div>

        <div className="grid grid-cols-4 gap-4 p-4">
          <div className="col-span-3 h-[600px] border rounded-md relative">
            {graph.nodes.length === 0 && !loading ? (
              <div className="absolute inset-0 flex items-center justify-center text-gray-400">No data — load a namespace</div>
            ) : (
              <ForceGraph2D
                ref={fgRef}
                graphData={graph}
                nodeLabel={n => `${n.id} (${n.type})`}
                nodeCanvasObject={nodeCanvasObject}
                nodePointerAreaPaint={(node, color, ctx) => {
                  ctx.fillStyle = color; ctx.beginPath(); ctx.arc(node.x, node.y, 8, 0, 2 * Math.PI, false); ctx.fill();
                }}
                linkWidth={link => linkWidth(link)}
                linkDirectionalArrowLength={3}
                linkDirectionalArrowRelPos={1}
                onNodeClick={node => setSelected({ type: 'node', data: node })}
                onLinkClick={link => setSelected({ type: 'link', data: link })}
              />
            )}
          </div>

          <div className="col-span-1">
            <div className="sticky top-4 p-3 bg-white border rounded-md h-[560px] overflow-auto">
              <h3 className="font-semibold">Details</h3>
              <hr className="my-2" />
              {selected ? (
                <div>
                  <div className="text-xs text-gray-500 mb-2">Selected: {selected.type}</div>
                  <pre className="text-xs bg-gray-100 p-2 rounded text-gray-700">{JSON.stringify(selected.data, null, 2)}</pre>
                </div>
              ) : (
                <div className="text-sm text-gray-500">Click a node or a link to see details.</div>
              )}

              <div className="mt-4">
                <h4 className="text-sm font-medium">Legend</h4>
                <ul className="text-sm mt-2 space-y-1">
                  <li>• <span className="font-medium">K8S_SERVICE</span> — cluster service</li>
                  <li>• <span className="font-medium">EXTERNAL</span> — external host / ServiceEntry</li>
                  <li>• <span className="font-medium">Other</span> — workloads / gateways</li>
                </ul>
              </div>

              <div className="mt-4 text-xs text-gray-500">
                API: <code className="bg-gray-100 px-1 py-0.5 rounded">GET /api/topology?namespace=...</code>
{/*                 <div className="mt-2">Expected JSON: <code className="block bg-gray-100 p-1 rounded">{{"nodes":[{"id":"svc.ns.svc.cluster.local","type":"K8S_SERVICE","meta":{}}],"edges":[{"fromId":"svc.ns.svc.cluster.local","toId":"external:api.example.com","protocol":"HTTP","port":80}]}}</code></div> */}
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
