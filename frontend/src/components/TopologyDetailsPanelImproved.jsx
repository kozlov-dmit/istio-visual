import React, { useEffect, useState } from 'react';

export default function TopologyDetailsPanel({ namespace = 'default', nodeId, onClose }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!nodeId) return;
    let active = true;
    setLoading(true); setError(null); setData(null);
    fetch(`/api/topology/details-enhanced-v2?namespace=${encodeURIComponent(namespace)}&nodeId=${encodeURIComponent(nodeId)}`)
      .then(res => res.ok ? res.json() : res.text().then(t => { throw new Error(t || res.statusText); }))
      .then(json => { if (active) setData(json); })
      .catch(err => { if (active) setError(String(err)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [namespace, nodeId]);

  if (!nodeId) return <div className="p-4">Select an object to see details</div>;
  if (loading) return <div className="p-4">Loading details...</div>;
  if (error) return <div className="p-4 text-red-600">Error: {error}</div>;
  if (!data) return null;

  return (
    <div className="p-3" style={{ maxHeight: '80vh', overflow: 'auto' }}>
      <div className="flex items-start gap-2">
        <h3 className="text-lg font-semibold">{data.node?.id || nodeId}</h3>
        <div className="text-sm text-gray-500">{data.node?.type}</div>
        <div className="ml-auto"><button onClick={() => onClose && onClose()} className="text-sm text-blue-600">Close</button></div>
      </div>

      <section className="mt-3">
        <h4 className="font-medium">Incoming</h4>
        {(!data.incoming || data.incoming.length === 0) ? <div className="text-xs text-gray-500">No incoming routes</div> : (
          <ul className="mt-2 space-y-2">
            {data.incoming.map((r, i) => (
              <li key={i} className="p-2 border rounded">
                <div className="text-sm"><strong>{r.from}</strong> → <strong>{r.to}</strong></div>
                <div className="text-xs text-gray-600">{r.protocol} {r.port ? ':'+r.port : ''}</div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="mt-3">
        <h4 className="font-medium">Outgoing</h4>
        {(!data.outgoing || data.outgoing.length === 0) ? <div className="text-xs text-gray-500">No outgoing routes</div> : (
          <ul className="mt-2 space-y-2">
            {data.outgoing.map((r, i) => (
              <li key={i} className="p-2 border rounded">
                <div className="text-sm"><strong>{r.from}</strong> → <strong>{r.to}</strong></div>
                <div className="text-xs text-gray-600">{r.protocol} {r.port ? ':'+r.port : ''}</div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="mt-3">
        <h4 className="font-medium">Composite routes (via egress)</h4>
        {(!data.composite || data.composite.length === 0) ? <div className="text-xs text-gray-500">No composite routes</div> : (
          <ul className="mt-2 space-y-2">
            {data.composite.map((c, i) => (
              <li key={i} className="p-2 border rounded">
                <div className="text-sm"><strong>{c.from}</strong> → <strong>{c.to}</strong></div>
                <div className="text-xs text-gray-600">via: {c.meta?.via || 'egress'}</div>
                <div className="text-xs mt-1">Port mapping: {renderPortMap(c.meta?.portMap)}</div>
                {c.notes && c.notes.length>0 && <div className="mt-2 text-xs text-gray-700">Notes: {c.notes.join(', ')}</div>}
              </li>
            ))}
          </ul>
        )}
      </section>

    </div>
  );

  function renderPortMap(pm){ if (!pm) return '-'; const fromP = pm.fromPort || '-'; const toP = pm.toPort || '-'; return `${fromP} → ${toP}`; }
}
