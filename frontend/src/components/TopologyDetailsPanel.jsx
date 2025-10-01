import React, { useEffect, useState } from 'react';

// TopologyDetailsPanel.jsx
// Fetches /api/topology/details-enhanced and renders it in a human-friendly format.
// This file has been verified for correct braces and syntax.

export default function TopologyDetailsPanelImproved({ namespace = 'default', nodeId, onClose }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!nodeId) return;
    let active = true;
    setLoading(true); setError(null); setData(null);
    fetch(`/api/topology/details-enhanced?namespace=${encodeURIComponent(namespace)}&nodeId=${encodeURIComponent(nodeId)}`)
      .then(res => res.ok ? res.json() : res.text().then(t => { throw new Error(t || res.statusText); }))
      .then(json => { if (active) setData(json); })
      .catch(err => { if (active) setError(String(err)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [namespace, nodeId]);

  function copy(text){
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(()=> { /* success */ }).catch(()=>{ /* ignore */ });
      } else {
        // Fallback for older browsers
        const ta = document.createElement('textarea');
        ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0';
        document.body.appendChild(ta); ta.select(); try { document.execCommand('copy'); } catch(e) { /* ignore */ } finally { document.body.removeChild(ta); }
      }
      // small UX feedback (non-blocking)
      try { alert('copied'); } catch(e) { /* ignore */ }
    } catch (e) {
      console.warn('copy failed', e);
    }
  }

  if (!nodeId) return <div className="p-4">Select an object to see details</div>;
  if (loading) return <div className="p-4">Loading details...</div>;
  if (error) return <div className="p-4 text-red-600">Error: {error}</div>;
  if (!data) return null;

  return (
    <div className="p-4" style={{ maxHeight: '80vh', overflow: 'auto' }}>
      <div className="flex items-start gap-2">
        <h3 className="text-lg font-semibold" title={data.basic?.id}>{data.basic?.id}</h3>
        <div className="text-sm text-gray-500">{data.basic?.type}</div>
        <div className="ml-auto"><button className="text-sm text-blue-600" onClick={() => onClose && onClose()}>Close</button></div>
      </div>

      {/* node diagnostics */}
      <section className="mt-3">
        <h4 className="font-medium">Diagnostics</h4>
        {data.nodeDiagnostics && data.nodeDiagnostics.length > 0 ? (
          <ul className="mt-2 space-y-2">
            {data.nodeDiagnostics.map((d, i) => (
              <li key={i} className={`p-2 rounded ${d.severity==='ERROR'?'bg-red-50':'bg-gray-50'}`}>
                <div className="flex items-center gap-2">
                  <div className="text-xs font-semibold">{d.severity}</div>
                  <div className="text-sm font-medium">{d.code}</div>
                </div>
                <div className="text-xs text-gray-600 mt-1">{d.message}</div>
                <pre className="mt-2 p-2 bg-white text-xs rounded">{d.suggestion}</pre>
              </li>
            ))}
          </ul>
        ) : (
          <div className="text-sm text-gray-500 mt-2">No immediate diagnostics for this object.</div>
        )}
      </section>

      {/* incoming/outgoing */}
      <section className="mt-4">
        <h4 className="font-medium">Routes</h4>
        <div className="mt-2">
          <div className="text-sm font-semibold">Incoming</div>
          {(!data.incomingRoutes || data.incomingRoutes.length === 0) ? <div className="text-xs text-gray-500">No incoming routes</div> : (
            <ul className="mt-2 space-y-2">
              {data.incomingRoutes.map((r, idx) => (
                <li key={idx} className="p-2 border rounded">
                  <div className="text-sm"><strong>{r.from}</strong> → <strong>{r.to}</strong></div>
                  <div className="text-xs text-gray-600">{r.protocol} {r.port ? ':'+r.port : ''}</div>
                  {r.diagnostics && r.diagnostics.length>0 && (
                    <div className="mt-1 text-xs text-red-600">{r.diagnostics.length} issues</div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="mt-3">
          <div className="text-sm font-semibold">Outgoing</div>
          {(!data.outgoingRoutes || data.outgoingRoutes.length === 0) ? <div className="text-xs text-gray-500">No outgoing routes</div> : (
            <ul className="mt-2 space-y-2">
              {data.outgoingRoutes.map((r, idx) => (
                <li key={idx} className="p-2 border rounded">
                  <div className="text-sm"><strong>{r.from}</strong> → <strong>{r.to}</strong></div>
                  <div className="text-xs text-gray-600">{r.protocol} {r.port ? ':'+r.port : ''}</div>
                  {r.diagnostics && r.diagnostics.length>0 && (
                    <div className="mt-1 text-xs text-red-600">{r.diagnostics.length} issues</div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      {/* VirtualServices */}
      <section className="mt-4">
        <h4 className="font-medium">VirtualServices (in namespace)</h4>
        {(!data.virtualServices || data.virtualServices.length===0) ? <div className="text-xs text-gray-500">No VirtualService references found in this namespace.</div> : (
          <div className="mt-2 space-y-2">
            {data.virtualServices.map((vs,i) => (
              <div key={i} className="p-2 border rounded">
                <div className="font-medium">{vs.name}</div>
                <div className="text-xs text-gray-600">Routes: {vs.routes.length}</div>
                <div className="mt-2 text-xs">
                  {vs.routes.map((r,j) => (<div key={j}><strong>{r.type}</strong> → {r.host} {r.weight ? `(w=${r.weight})` : ''}</div>))}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* DestinationRules */}
      <section className="mt-4">
        <h4 className="font-medium">DestinationRules</h4>
        {(!data.destinationRules || data.destinationRules.length===0) ? <div className="text-xs text-gray-500">No DestinationRule targeting this host.</div> : (
          <div className="mt-2 space-y-2">
            {data.destinationRules.map((dr,i) => (
              <div key={i} className="p-2 border rounded">
                <div className="font-medium">{dr.name}</div>
                <div className="text-xs text-gray-600">Subsets: {dr.subsets.length}</div>
                <div className="mt-2 text-xs">
                  {dr.subsets.map((s,si) => (
                    <div key={si} className="mb-1">• {s.name} — labels: {Object.entries(s.labels || {}).map(([k,v]) => `${k}=${v}`).join(', ')}</div>
                  ))}
                </div>
                {dr.hasTrafficPolicy && <div className="mt-1 text-xs text-amber-700">Has trafficPolicy (check TLS settings)</div>}
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ServiceEntries */}
      <section className="mt-4">
        <h4 className="font-medium">ServiceEntries</h4>
        {(!data.serviceEntries || data.serviceEntries.length===0) ? <div className="text-xs text-gray-500">No ServiceEntry for this host in namespace.</div> : (
          <div className="mt-2 space-y-2">
            {data.serviceEntries.map((se,i) => (
              <div key={i} className="p-2 border rounded">
                <div className="font-medium">{se.name}</div>
                <div className="text-xs text-gray-600">Hosts: {se.hosts.join(', ')}</div>
                <div className="mt-2 text-xs">Ports:
                  <ul className="mt-1 ml-4 list-disc">
                    {se.ports.map((p,pi) => (<li key={pi}>{p.name} — {p.number} ({p.protocol})</li>))}
                  </ul>
                </div>
                <div className="mt-2 text-xs">Resolution: {se.resolution}</div>
                <div className="mt-2"><button className="text-sm text-blue-600" onClick={()=>copy(makeSEYAML(se))}>Copy ServiceEntry</button></div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Sidecars */}
      <section className="mt-4">
        <h4 className="font-medium">Sidecars</h4>
        {(!data.sidecars || data.sidecars.length===0) ? <div className="text-xs text-gray-500">No Sidecars in namespace.</div> : (
          <div className="mt-2 space-y-2">
            {data.sidecars.map((sc,i) => (
              <div key={i} className="p-2 border rounded">
                <div className="font-medium">{sc.name} {sc.matches ? <span className="text-xs text-emerald-700">(matches)</span> : <span className="text-xs text-gray-500">(no match)</span>}</div>
                <div className="text-xs text-gray-600">Hosts: {sc.hosts.join(', ')}</div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* TLS Hints */}
      <section className="mt-4">
        <h4 className="font-medium">TLS hints</h4>
        {(!data.tlsHints || data.tlsHints.length===0) ? <div className="text-xs text-gray-500">No TLS hints detected.</div> : (
          <div className="mt-2 space-y-2 text-xs">
            {data.tlsHints.map((t,i)=> (
              <div key={i} className="p-2 border rounded">DR: {t.dr} — TLS object present (inspect DestinationRule)</div>
            ))}
          </div>
        )}
      </section>

      {/* EnvoyFilters */}
      <section className="mt-4">
        <h4 className="font-medium">EnvoyFilters</h4>
        {(!data.envoyFilters || data.envoyFilters.length === 0) ? <div className="text-xs text-gray-500">None in namespace.</div> : (
          <ul className="mt-2 text-xs space-y-1">
            {data.envoyFilters.map((ef, i) => <li key={i}>{ef}</li>)}
          </ul>
        )}
      </section>

      {/* Reachability hints */}
      <section className="mt-4">
        <h4 className="font-medium">Mesh reachability</h4>
        {data.meshReachable ? <div className="text-sm text-emerald-700">There are routes from mesh to this host (visible in namespace).</div> : (
          <div>
            <div className="text-sm text-amber-700">No routes from mesh to this host were found in the namespace.</div>
            <div className="mt-2 text-xs text-gray-600">Likely causes:</div>
            <ul className="ml-4 mt-1 text-xs list-disc">
              {(data.likelyCauses || []).map((c,i)=><li key={i}>{c}</li>)}
            </ul>
            <div className="mt-2 text-xs">Suggested commands:</div>
            <ul className="ml-4 mt-1 text-xs list-disc">
              {(data.suggestedCommands || []).map((c,i)=> <li key={i}><code className="text-xs bg-gray-100 p-1 rounded">{c}</code></li>)}
            </ul>
          </div>
        )}
      </section>

    </div>
  );

  function makeSEYAML(se) {
    const host = (se.hosts && se.hosts[0]) || 'example.com';
    const port = (se.ports && se.ports[0] && se.ports[0].number) || 9092;
    return `apiVersion: networking.istio.io/v1beta1\nkind: ServiceEntry\nmetadata:\n  name: se-${host.replace(/[^a-zA-Z0-9]/g,'-')}\n  namespace: ${namespace}\nspec:\n  hosts:\n  - ${host}\n  location: MESH_EXTERNAL\n  ports:\n  - number: ${port}\n    name: tcp-${port}\n    protocol: TCP\n  resolution: DNS\n`;
  }
}
