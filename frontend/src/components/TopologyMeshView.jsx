import React, { useEffect, useState } from 'react';
import TopologyDetailsPanel from './TopologyDetailsPanel';

// TopologyMeshView.jsx
// Visualizes: mesh box with pods (with sidecar), routes based on VirtualServices,
// handles composite routes and synthesizes pods when necessary.

export default function TopologyMeshView({ initialNamespace = 'default' }) {
  const [namespace, setNamespace] = useState(initialNamespace);
  const [snapshot, setSnapshot] = useState(null);
  const [selectedNode, setSelectedNode] = useState(null);

  useEffect(() => { load(namespace); }, [namespace]);

  async function load(ns) {
    try {
      const res = await fetch(`/api/topology/mesh?namespace=${encodeURIComponent(ns)}`);
      if (!res.ok) throw new Error(await res.text().catch(()=>res.statusText));
      const body = await res.json();
      // normalize: ensure arrays
      setSnapshot({
        pods: Array.isArray(body.pods) ? body.pods : [],
        services: Array.isArray(body.services) ? body.services : [],
        virtualServices: Array.isArray(body.virtualServices) ? body.virtualServices : [],
        gateways: Array.isArray(body.gateways) ? body.gateways : [],
        serviceEntries: Array.isArray(body.serviceEntries) ? body.serviceEntries : []
      });
    } catch (e) { console.error('load mesh', e); setSnapshot(null); }
  }

  if (!snapshot) return (
    <div style={{ padding: 12 }}>
      <div style={{ display: 'flex', gap: 8 }}>
        <input value={namespace} onChange={e=>setNamespace(e.target.value)} style={{ padding: 6 }} />
        <button onClick={()=>load(namespace)} style={{ padding: '6px 10px' }}>Reload</button>
      </div>
      <div style={{ marginTop: 24 }}>Loading snapshot...</div>
    </div>
  );

  // helpers
  function labelsMatch(selector, labels) {
    if (!selector || Object.keys(selector).length === 0) return false;
    if (!labels) return false;
    for (const [k,v] of Object.entries(selector)) {
      if (!labels[k] || labels[k] !== v) return false;
    }
    return true;
  }

  function synthNameFromSelector(sel) {
    if (!sel) return 'synth';
    return 'synth-' + Object.entries(sel).map(([k,v]) => `${k}-${v}`).join('-').replace(/[^a-zA-Z0-9-]/g,'-');
  }

  // build internal mesh pods list (only with sidecar)
  const meshPods = snapshot.pods.filter(p => p.hasSidecar);

  // build map of services by name
  const servicesByName = new Map(snapshot.services.map(s => [s.name, s]));

  // gather routes from virtualServices
  const routeSpecs = []; // {fromType: 'mesh'|'gateway'|'vs', fromId, toHost, toPort, vsName, gateways}
  snapshot.virtualServices.forEach(vs => {
    const vsName = vs.name;
    const gateways = vs.gateways || [];
    (vs.routes || []).forEach(r => {
      routeSpecs.push({ fromType: gateways && gateways.length>0 ? 'gateway' : 'vs', fromId: gateways && gateways.length>0 ? gateways[0] : ('virtualservice:'+vsName), toHost: r.host, toPort: r.port, vsName, gateways });
    });
  });

  // we'll create drawable nodes for pods in mesh (real and synthetic)
  const drawablePods = [...meshPods.map(p => ({ id: p.name, labels: p.labels, synthetic: false }))];

  // map to collect synthetic pods created
  const syntheticMap = new Map();

  // edges to draw: {fromId, toId, label}
  const edges = [];

  for (const rs of routeSpecs) {
    const toHost = rs.toHost || '';
    const toSimple = toHost.split('\.')[0];
    // find service
    const svc = servicesByName.get(toSimple) || servicesByName.get(toHost);
    if (svc) {
      // find pods matching service selector
      const selector = svc.selector || {};
      const matched = meshPods.filter(p => labelsMatch(selector, p.labels));
      if (matched.length > 0) {
        // create edges from source (vs or gateway) to each pod
        matched.forEach(mp => {
          const fromId = rs.fromType === 'gateway' ? ('gateway:' + (rs.gateways[0] || 'unknown')) : ('virtualservice:' + rs.vsName + '.' + namespace);
          edges.push({ fromId, toId: mp.name, label: rs.toPort ? (rs.toPort) : '' });
        });
      } else {
        // create synthetic pod based on selector
        const sname = syntheticMap.get(svc.name) || synthNameFromSelector(svc.selector);
        if (!syntheticMap.has(svc.name)) {
          syntheticMap.set(svc.name, sname);
          drawablePods.push({ id: sname, labels: svc.selector, synthetic: true });
        }
        const fromId = rs.fromType === 'gateway' ? ('gateway:' + (rs.gateways[0] || 'unknown')) : ('virtualservice:' + rs.vsName + '.' + namespace);
        edges.push({ fromId, toId: sname, label: rs.toPort ? (rs.toPort) : '' });
      }
    } else {
      // No service - treat target as external host. Create external node
      const externalId = 'external:' + toHost;
      const fromId = rs.fromType === 'gateway' ? ('gateway:' + (rs.gateways[0] || 'unknown')) : ('virtualservice:' + rs.vsName + '.' + namespace);
      edges.push({ fromId, toId: externalId, label: rs.toPort ? (rs.toPort) : '' });
      // create synthetic external node on right
      if (!drawablePods.find(p=>p.id===externalId)) {
        drawablePods.push({ id: externalId, external: true });
      }
    }
  }

  // additionally handle routes originating from gateways explicit: find gateway selectors -> pods
  snapshot.gateways.forEach(gw => {
    const gwId = 'gateway:' + gw.name;
    const sel = gw.selector || {};
    const matched = meshPods.filter(p => labelsMatch(sel, p.labels));
    if (matched.length === 0 && Object.keys(sel).length>0) {
      const sname = syntheticMap.get(gw.name) || synthNameFromSelector(sel);
      if (!syntheticMap.has(gw.name)) {
        syntheticMap.set(gw.name, sname);
        drawablePods.push({ id: sname, labels: sel, synthetic: true });
      }
    }
  });

  // build simple layout
  const meshBox = { x: 40, y: 60, width: 360, height: 520 };
  const rightX = 600;

  // draw
  return (
    <div style={{ display: 'flex', gap: 12, padding: 12 }}>
      <div style={{ flex: 1 }}>
        <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
          <input value={namespace} onChange={e=>setNamespace(e.target.value)} style={{ padding: 6 }} />
          <button onClick={()=>load(namespace)} style={{ padding: '6px 10px' }}>Reload</button>
        </div>

        <svg width="100%" height="700" viewBox="0 0 1100 700" style={{ background: '#f8fafc' }}>
          {/* mesh box */}
          <rect x={meshBox.x} y={meshBox.y} width={meshBox.width} height={meshBox.height} rx={12} fill="#ffffff" stroke="#c7d2fe" />
          <text x={meshBox.x + 12} y={meshBox.y + 24} fontSize={14} fontWeight={700}>mesh (pods with sidecar)</text>

          {/* place pods vertically inside mesh */}
          {drawablePods.filter(p=>!p.external).map((p,i) => {
            const px = meshBox.x + 20 + (i%2)*160;
            const py = meshBox.y + 50 + Math.floor(i/2)*60;
            return (
              <g key={p.id} onClick={()=>setSelectedNode(p.id)} style={{ cursor: 'pointer' }}>
                <rect x={px} y={py} rx={8} width={140} height={36} fill={p.synthetic? '#fff7ed': '#e6f6ff'} stroke={p.synthetic? '#f97316' : '#0369a1'} />
                <text x={px+8} y={py+22} fontSize={12} fill={p.synthetic? '#92400e':'#075985'}>{p.id}</text>
              </g>
            );
          })}

          {/* egress box */}
          <g>
            <rect x={meshBox.x + meshBox.width + 40} y={meshBox.y + 210} width={180} height={120} rx={12} fill="#f0fdf4" stroke="#86efac" />
            <text x={meshBox.x + meshBox.width + 130} y={meshBox.y + 260} fontSize={13} fontWeight={600} textAnchor="middle">egress</text>
          </g>

          {/* external nodes on right */}
          {drawablePods.filter(p=>p.external).map((p,i) => {
            const x = rightX + 80;
            const y = meshBox.y + 40 + i*60;
            return (
              <g key={p.id} onClick={()=>setSelectedNode(p.id)} style={{ cursor: 'pointer' }}>
                <circle cx={x} cy={y} r={20} fill="#fff1f2" stroke="#b91c1c" />
                <text x={x} y={y+4} fontSize={11} fill="#7f1d1d" textAnchor="middle">{p.id.replace(/^external:/,'')}</text>
              </g>
            );
          })}

          {/* gateways visually as small boxes near mesh top */}
          {snapshot.gateways.map((g,i) => {
            const gx = meshBox.x + meshBox.width/2 - 40 + i*60;
            const gy = meshBox.y + 6;
            return (
              <g key={g.name} onClick={()=>setSelectedNode('gateway:'+g.name)} style={{ cursor: 'pointer' }}>
                <rect x={gx} y={gy} width={80} height={30} rx={6} fill="#fff7ed" stroke="#f97316" />
                <text x={gx+40} y={gy+20} fontSize={11} textAnchor="middle">{g.name}</text>
              </g>
            );
          })}

          {/* draw edges */}
          {edges.map((ed, idx) => {
            const fromIsExternal = ed.fromId && ed.fromId.startsWith('external:');
            const toIsExternal = ed.toId && ed.toId.startsWith('external:');

            // compute coordinates
            let fromX = meshBox.x + meshBox.width/2, fromY = meshBox.y + meshBox.height/2;
            let toX = rightX, toY = meshBox.y + meshBox.height/2;

            // find from node position
            const fromDrawable = drawablePods.find(p => p.id === ed.fromId);
            if (fromDrawable) {
              // find its index
              const i = drawablePods.filter(p=>!p.external).findIndex(p=>p.id===ed.fromId);
              if (i>=0) { fromX = meshBox.x + 20 + (i%2)*160 + 140; fromY = meshBox.y + 50 + Math.floor(i/2)*60 + 18; }
            } else if (ed.fromId && ed.fromId.startsWith('virtualservice:')) {
              // virtualservice source: draw from top-left of mesh
              fromX = meshBox.x + 20; fromY = meshBox.y + 30;
            } else if (ed.fromId && ed.fromId.startsWith('gateway:')) {
              // gateway position: find in snapshot.gateways
              const gi = snapshot.gateways.findIndex(g=>('gateway:'+g.name)===ed.fromId);
              if (gi>=0) { fromX = meshBox.x + meshBox.width/2 - 40 + gi*60 + 40; fromY = meshBox.y + 6 + 15; }
            }

            // to node
            const toDrawable = drawablePods.find(p => p.id === ed.toId);
            if (toDrawable) {
              if (toDrawable.external) {
                const ei = drawablePods.filter(p=>p.external).findIndex(p=>p.id===ed.toId);
                toX = rightX + 80 - 20; toY = meshBox.y + 40 + ei*60;
              } else {
                const i = drawablePods.filter(p=>!p.external).findIndex(p=>p.id===ed.toId);
                toX = meshBox.x + 20 + (i%2)*160; toY = meshBox.y + 50 + Math.floor(i/2)*60 + 18;
              }
            }

            const d = `M ${fromX} ${fromY} Q ${(fromX+toX)/2} ${(fromY+toY)/2 - 40} ${toX} ${toY}`;
            return (
              <g key={`edge-${idx}`}>
                <path d={d} fill="none" stroke="#374151" strokeWidth={2} strokeOpacity={0.9} />
                <text x={(fromX+toX)/2} y={(fromY+toY)/2 - 44} fontSize={11} textAnchor="middle">{ed.label}</text>
              </g>
            );
          })}

        </svg>
      </div>

      <div style={{ width: 360 }}>
        <div style={{ background: '#fff', borderRadius: 8, padding: 12 }}>
          <h3 style={{ marginTop: 0 }}>Details</h3>
          <TopologyDetailsPanel namespace={namespace} nodeId={selectedNode} onClose={()=>setSelectedNode(null)} />
        </div>
      </div>
    </div>
  );
}
