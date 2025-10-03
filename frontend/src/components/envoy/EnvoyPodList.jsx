import React from 'react';

const EnvoyPodList = ({ pods, selectedPodName, onSelect }) => (
  <ul className="envoy-pod-list">
    {pods.length === 0 && (
      <li className="empty-value">No istio-proxy pods found</li>
    )}
    {pods.map((pod) => {
      const containers = Array.isArray(pod.containers) ? pod.containers : [];
      const readyCount = containers.filter((status) => status.ready).length;
      const isActive = selectedPodName === pod.name;
      return (
        <li key={pod.name}>
          <button
            type="button"
            className={`envoy-pod-button${isActive ? ' envoy-pod-button--active' : ''}`}
            onClick={() => onSelect(pod.name)}
          >
            <span className="pod-name">{pod.name}</span>
            <span className="pod-meta">Status: {pod.phase || 'UNKNOWN'}</span>
            <span className="pod-meta">Readiness: {readyCount}/{containers.length}</span>
            <span className="pod-meta">Node: {pod.nodeName || '—'}</span>
          </button>
        </li>
      );
    })}
  </ul>
);

export default EnvoyPodList;