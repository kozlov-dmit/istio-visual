import React, { useEffect, useMemo, useRef, useState } from 'react';
import ForceGraph2D from 'react-force-graph-2d';

// TopologyExplorer.jsx
// Combines: Graph view + Table view (inspector) + advanced Path Explorer (full routing BFS)
// Features added compared to previous components:
// - toggle between Graph and Table
// - pick start node(s) and optional target node, compute ALL simple paths up to depth limit
// - rank paths by risk (sum of link risk / max severity) and show weakest link per path
// - highlight selected path(s) in the graph
// - controls: depth limit, max paths, path filtering
//
// Expects backend endpoints:
// GET  /api/topology/diagnose?namespace=...  (nodes, edges with diagnostics)
// POST /api/probe  (optional)

const SEVERITY_WEIGHT = { 'ERROR': 100, 'WARN': 10, 'INFO': 1, 'OK': 0 };
const SEV_ORDER = { 'ERROR': 3, 'WARN': 2, 'INFO': 1, 'OK': 0 };

export default function TopologyExplorer({ initialNamespace = 'default' }) {
  const [namespace, setNamespace] = useState(initialNamespace);
  const [mode, setMode] = useState('graph'); // 'graph' or 'table'
  const [data, setData] = useState({ nodes: [], edges: [], summary: {} });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // selection / path explorer state
  const [selectedNode, setSelectedNode] = useState(null);
  const [targetNode, setTargetNode] = useState(null);
  const [depthLimit, setDepthLimit] = useState(6);
  const [maxPaths, setMaxPaths] = useState(200);
  const [paths, setPaths] = useState([]);
  const [highlightedLinks, setHighlightedLinks] = useState(new Set());
  const [pathComputeLoading, setPathComputeLoading] = useState(false);
  const fgRef = useRef();

  useEffect(() => {
    load(namespace);
  }, []);

  async function load(ns) {
    setLoading(true); setError(null); setSelectedNode(null); setTargetNode(null); setPaths([]);
    try {
      const res = await fetch(`/api/topology/diagnose?namespace=${encodeURIComponent(ns)}`);
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const body = await res.json();
      // Normalize edges
      const edges = (body.edges || []).map(e => ({
        source: e.fromId,
        target: e.toId,
        protocol: e.protocol,
        port: e.port,
        notes: e.notes || [],
        diagnostics: e.diagnostics || [],
        riskScore: computeRiskScore(e.diagnostics),
        severity: computeEdgeSeverity(e.diagnostics)
      }));
      const nodes = (body.nodes || []).map(n => ({ id: n.id, label: n.id, type: n.type, meta: n.meta || {} }));
      setData({ nodes, edges, summary: body.summary || {} });
      setTimeout(() => fgRef.current && fgRef.current.zoomToFit(400, 50), 200);
    } catch (err) {
      setError(err.message);
    } finally { setLoading(false); }
  }

  function computeEdgeSeverity(diagnostics) {
    if (!diagnostics || diagnostics.length === 0) return 'OK';
    if (diagnostics.some(d => d.severity === 'ERROR')) return 'ERROR';
    if (diagnostics.some(d => d.severity === 'WARN')) return 'WARN';
    return 'INFO';
  }
  function computeRiskScore(diagnostics) {
    if (!diagnostics) return 0;
    return diagnostics.reduce((sum, d) => sum + (SEVERITY_WEIGHT[d.severity] || 0), 0);
  }

  const nodesById = useMemo(() => {
    const m = new Map();
    data.nodes.forEach(n => m.set(n.id, n));
    return m;
  }, [data.nodes]);

  const adjacency = useMemo(() => {
    const m = new Map();
    data.nodes.forEach(n => m.set(n.id, []));
    data.edges.forEach(e => {
      if (!m.has(e.source)) m.set(e.source, []);
      m.get(e.source).push(e);
    });
    return m;
  }, [data.edges, data.nodes]);

  // BFS / DFS for simple paths limited by depth and maxPaths
  function computeAllPaths(startIds, endId, depthLimitLocal = depthLimit, maxPathsLocal = maxPaths) {
    setPathComputeLoading(true);
    try {
      const results = [];
      const visitedGlobal = new Set();

      // We'll perform DFS from each start
      for (const start of startIds) {
        const stack = [{ node: start, pathNodes: [start], pathEdges: [] }];
        while (stack.length > 0 && results.length < maxPathsLocal) {
          const frame = stack.pop();
          const cur = frame.node;
          if (frame.pathNodes.length - 1 > depthLimitLocal) continue;

          // if we've reached end (if endId specified) or this is external node (when no end specified)
          const isExternal = cur.startsWith('external:');
          const reached = endId ? cur === endId : isExternal;
          if (reached && frame.pathEdges.length > 0) {
            // compute path metrics
            const path = { nodes: [...frame.pathNodes], edges: [...frame.pathEdges] };
            path.totalRisk = path.edges.reduce((s, ed) => s + (ed.riskScore || 0), 0);
            path.maxSeverity = path.edges.reduce((mx, ed) => Math.max(mx, SEV_ORDER[ed.severity] || 0), 0);
            path.weakest = path.edges.reduce((worst, ed) => {
              // choose edge with highest severity then highest risk
              const curSeverity = SEV_ORDER[ed.severity] || 0;
              if (!worst) return ed;
              const worstSeverity = SEV_ORDER[worst.severity] || 0;
              if (curSeverity > worstSeverity) return ed;
              if (curSeverity === worstSeverity && (ed.riskScore || 0) > (worst.riskScore || 0)) return ed;
              return worst;
            }, null);
            results.push(path);
          }

          // continue exploring if depth allows
          if (frame.pathNodes.length - 1 >= depthLimitLocal) continue;

          const outgoing = adjacency.get(cur) || [];
          for (const e of outgoing) {
            const next = e.target;
            if (frame.pathNodes.includes(next)) continue; // avoid cycles
            // push new frame
            stack.push({ node: next, pathNodes: [...frame.pathNodes, next], pathEdges: [...frame.pathEdges, e] });
          }
        }
        if (results.length >= maxPathsLocal) break;
      }

      // sort paths by severity (maxSeverity desc) then totalRisk desc then length asc
      results.sort((a, b) => {
        if (b.maxSeverity !== a.maxSeverity) return b.maxSeverity - a.maxSeverity;
        if (b.totalRisk !== a.totalRisk) return b.totalRisk - a.totalRisk;
        return a.nodes.length - b.nodes.length;
      });

      setPaths(results.slice(0, maxPathsLocal));
      // set highlighted links as union of edges from top paths
      const linkSet = new Set();
      results.slice(0, maxPathsLocal).forEach(p => p.edges.forEach(ed => linkSet.add(edgeKey(ed))));
      setHighlightedLinks(linkSet);

      return results;
    } finally {
      setPathComputeLoading(false);
    }
  }

  function edgeKey(e) { return `${e.source}>>>${e.target}:::${e.protocol}::${e.port || ''}`; }

  function nodePickerOptions() {
    // return useful nodes: K8s services + mesh pseudo node + workload nodes
    return data.nodes.map(n => ({ id: n.id, label: n.id, type: n.type }));
  }

  function onNodeClick(node) {
    setSelectedNode(node.id);
    // focus in graph
    if (fgRef.current) fgRef.current.centerAt(node.x || 0, node.y || 0, 400);
  }

  function highlightColor(link) {
    const key = edgeKey(link);
    if (highlightedLinks.has(key)) {
      // color by severity if part of highlighted set, otherwise muted
      if (link.severity === 'ERROR') return 'rgba(220,38,38,0.95)';
      if (link.severity === 'WARN') return 'rgba(234,88,12,0.95)';
      return 'rgba(34,197,94,0.95)';
    }
    // default
    return 'rgba(107,114,128,0.2)';
  }

  // UI handlers
  function handleComputePaths() {
    const starts = selectedNode ? [selectedNode] : data.nodes.filter(n => n.type === 'K8S_SERVICE').map(n => n.id);
    computeAllPaths(starts, targetNode, depthLimit, maxPaths);
  }

  function clearHighlights() { setHighlightedLinks(new Set()); setPaths([]); }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto bg-white shadow-md rounded-lg overflow-hidden">
        <div className="p-4 border-b flex items-center gap-4">
          <div className="flex items-center gap-2">
            <label className="text-sm text-gray-600">Namespace</label>
            <input value={namespace} onChange={(e) => setNamespace(e.target.value)} className="border px-2 py-1 rounded" />
            <button className="px-3 py-1 rounded bg-indigo-600 text-white" onClick={() => load(namespace)}>{loading ? 'Loading...' : 'Load'}</button>
          </div>

          <div className="ml-4 flex items-center gap-2">
            <button className={`px-3 py-1 rounded ${mode==='graph'?'bg-sky-600 text-white':'bg-gray-100'}`} onClick={()=>setMode('graph')}>Graph</button>
            <button className={`px-3 py-1 rounded ${mode==='table'?'bg-sky-600 text-white':'bg-gray-100'}`} onClick={()=>setMode('table')}>Table</button>
          </div>

          <div className="ml-auto text-sm text-gray-500">Nodes: {data.nodes.length} • Edges: {data.edges.length} • Issues: {Object.values(data.summary?.diagnosticsBySeverity || {}).reduce((a,b)=>a+(b||0),0)}</div>
        </div>

        <div className="p-4">
          <div className="grid grid-cols-4 gap-4">
            <div className="col-span-3">
              {mode === 'graph' ? (
                <div className="h-[680px] border rounded">
                  <ForceGraph2D
                    ref={fgRef}
                    graphData={{ nodes: data.nodes.map(n => ({ id: n.id, type: n.type })), links: data.edges.map(e => ({ source: e.source, target: e.target, severity: e.severity, riskScore: e.riskScore, protocol: e.protocol, port: e.port })) }}
                    nodeLabel={n => `${n.id} (${n.type})`}
                    nodeAutoColorBy={n => n.type}
                    nodeCanvasObject={(node, ctx, globalScale) => {
                      const label = node.id.replace(/\.svc\.cluster\.local$/, '');
                      const fontSize = 12 / globalScale;
                      ctx.font = `${fontSize}px Sans-Serif`;
                      ctx.fillStyle = '#111827'; ctx.beginPath(); ctx.arc(node.x, node.y, 6 / globalScale, 0, 2 * Math.PI, false); ctx.fill();
                      ctx.fillStyle = 'rgba(255,255,255,0.9)'; const textWidth = ctx.measureText(label).width; ctx.fillRect(node.x - textWidth/2 - 4, node.y + 8/globalScale, textWidth + 8, fontSize + 6);
                      ctx.fillStyle = '#111827'; ctx.textAlign='left'; ctx.textBaseline='top'; ctx.fillText(label, node.x - textWidth/2, node.y + 8/globalScale + 1);
                    }}
                    linkWidth={l => highlightedLinks.has(edgeKey(l)) ? 2.5 : 1}
                    linkColor={l => highlightColor(l)}
                    linkDirectionalArrowLength={3}
                    linkDirectionalArrowRelPos={1}
                    onNodeClick={node => onNodeClick(node)}
                    onLinkClick={link => { /* show link info maybe */ }}
                  />
                </div>
              ) : (
                <div>
                  {/* Table view: use previous inspector list simplified */}
                  <div className="bg-white rounded shadow overflow-hidden">
                    <div className="p-3 border-b flex items-center gap-4">
                      <div className="font-semibold">Routes (table)</div>
                      <div className="ml-auto text-xs text-gray-500">Showing {data.edges.length} routes</div>
                    </div>
                    <div className="p-2 max-h-[640px] overflow-auto">
                      <table className="w-full text-sm table-auto">
                        <thead><tr className="text-left text-xs text-gray-500 border-b"><th className="p-2">From</th><th className="p-2">To</th><th className="p-2">Proto</th><th className="p-2">Port</th><th className="p-2">Severity</th><th className="p-2">Risk</th></tr></thead>
                        <tbody>
                          {data.edges.map((r, idx) => (
                            <tr key={idx} className="border-b hover:bg-slate-50 cursor-pointer" onClick={()=>{ setSelectedNode(r.source); setTargetNode(r.target); setMode('table'); }}>
                              <td className="p-2">{short(r.source)}</td>
                              <td className="p-2">{short(r.target)}</td>
                              <td className="p-2">{r.protocol}</td>
                              <td className="p-2">{r.port || '-'}</td>
                              <td className="p-2">{r.severity}</td>
                              <td className="p-2">{r.riskScore}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div>
              <div className="bg-white rounded shadow p-3 sticky top-6">
                <h4 className="font-semibold">Path Explorer</h4>
                <hr className="my-2" />

                <div className="text-sm space-y-2">
                  <div><label className="text-xs">Start node (optional, picks all K8S if empty)</label>
                    <select className="w-full border rounded px-2 py-1 text-sm" value={selectedNode||''} onChange={e=>setSelectedNode(e.target.value||null)}>
                      <option value="">-- all K8S services --</option>
                      {data.nodes.filter(n=>n.type==='K8S_SERVICE').map(n=> <option key={n.id} value={n.id}>{short(n.id)}</option>)}
                    </select>
                  </div>

                  <div>
                    <label className="text-xs">Target node (optional, leave empty to target externals)</label>
                    <select className="w-full border rounded px-2 py-1 text-sm" value={targetNode||''} onChange={e=>setTargetNode(e.target.value||null)}>
                      <option value="">-- any external --</option>
                      {data.nodes.filter(n=>n.id.startsWith('external:')||n.type==='SERVICEENTRY').map(n=> <option key={n.id} value={n.id}>{short(n.id)}</option>)}
                    </select>
                  </div>

                  <div className="flex items-center gap-2">
                    <label className="text-xs">Depth</label>
                    <input type="number" value={depthLimit} onChange={e=>setDepthLimit(Math.max(1, Number(e.target.value)||1))} className="border px-2 py-1 w-20 rounded text-sm" />
                    <label className="text-xs">Max paths</label>
                    <input type="number" value={maxPaths} onChange={e=>setMaxPaths(Math.max(1, Number(e.target.value)||1))} className="border px-2 py-1 w-20 rounded text-sm" />
                  </div>

                  <div className="flex gap-2">
                    <button onClick={handleComputePaths} className="px-3 py-1 bg-emerald-600 text-white rounded">Compute paths</button>
                    <button onClick={clearHighlights} className="px-3 py-1 bg-gray-100 rounded">Clear</button>
                  </div>

                </div>

                <hr className="my-2" />
                <div className="text-sm">
                  <div className="font-medium">Top paths</div>
                  <div className="max-h-64 overflow-auto mt-2 space-y-2">
                    {pathComputeLoading ? <div>Computing...</div> : paths.length===0 ? <div className="text-xs text-gray-500">No paths computed</div> : (
                      paths.map((p, idx) => (
                        <div key={idx} className="p-2 border rounded">
                          <div className="flex justify-between"><div className="font-medium">Path {idx+1} — risk {p.totalRisk} — weakest: {short(p.weakest?.source)}→{short(p.weakest?.target)} ({p.weakest?.severity})</div><div className="text-xs text-gray-500">len {p.nodes.length}</div></div>
                          <div className="text-xs text-gray-600 mt-1">{p.nodes.map(n=>short(n)).join(' → ')}</div>
                          <div className="mt-2 flex gap-2">
                            <button onClick={()=>{ // highlight this single path
                              const s = new Set(); p.edges.forEach(ed => s.add(edgeKey(ed))); setHighlightedLinks(s);
                            }} className="px-2 py-1 rounded bg-sky-600 text-white text-xs">Highlight</button>
                            <button onClick={()=>{ // zoom to first hop
                              const firstNode = nodesById.get(p.nodes[0]); if(firstNode && fgRef.current) fgRef.current.centerAt(firstNode.x||0, firstNode.y||0, 400);
                            }} className="px-2 py-1 rounded bg-gray-100 text-xs">Zoom</button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>

              </div>
            </div>

          </div>
        </div>

      </div>
    </div>
  );

  function short(id){ return String(id || '').replace(/^external:/,'').replace(/^serviceentry:/,'').replace(/\.svc\.cluster\.local$/,''); }
}
