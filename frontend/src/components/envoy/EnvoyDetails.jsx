import React from 'react';

const EnvoyDetails = ({
  selectedPodName,
  podSummary,
  warnings,
  isLoading,
  error,
  listenerFilter,
  onListenerFilterChange,
  listenerRows,
  clusterFilter,
  onClusterFilterChange,
  clusterRows,
  routesFilter,
  onRoutesFilterChange,
  routesRows,
}) => {
  if (!selectedPodName) {
    return (
      <aside className="envoy-details-card">
        <h2>Envoy configuration</h2>
        <p className="placeholder">Select an istio-proxy pod to inspect configuration.</p>
      </aside>
    );
  }

  if (isLoading) {
    return (
      <aside className="envoy-details-card">
        <h2>Envoy configuration</h2>
        <p className="status status--loading">Loading configuration…</p>
      </aside>
    );
  }

  if (error) {
    return (
      <aside className="envoy-details-card">
        <h2>Envoy configuration</h2>
        <p className="status status--error">{error}</p>
      </aside>
    );
  }

  if (!podSummary) {
    return (
      <aside className="envoy-details-card">
        <h2>Envoy configuration</h2>
        <p className="placeholder">No configuration data available.</p>
      </aside>
    );
  }

  return (
    <aside className="envoy-details-card">
      <h2>Envoy configuration</h2>
      <div className="envoy-details">
        <div className="detail-row">
          <span className="detail-label">Pod</span>
          <span className="detail-value">{podSummary.name || selectedPodName}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Namespace</span>
          <span className="detail-value">{podSummary.namespace || '—'}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Node</span>
          <span className="detail-value">{podSummary.nodeName || '—'}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Pod IP</span>
          <span className="detail-value">{podSummary.podIp || '—'}</span>
        </div>
        {Array.isArray(warnings) && warnings.length > 0 && (
          <div className="detail-row">
            <span className="detail-label">Warnings</span>
            <ul className="warning-list">
              {warnings.map((warning, index) => (
                <li key={`warning-${index}`}>{warning}</li>
              ))}
            </ul>
          </div>
        )}

        <div className="envoy-section">
          <div className="envoy-section-header">
            <h3>Listeners</h3>
            <input
              className="filter-input"
              type="text"
              value={listenerFilter}
              onChange={(event) => onListenerFilterChange(event.target.value)}
              placeholder="Filter listeners"
            />
          </div>
          <div className="envoy-table-wrapper">
            <table className="envoy-data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Origin</th>
                  <th>Address</th>
                  <th>Port</th>
                  <th>Filters</th>
                </tr>
              </thead>
              <tbody>
                {listenerRows.length === 0 && (
                  <tr>
                    <td colSpan={5} className="empty-value">No listeners match current filter</td>
                  </tr>
                )}
                {listenerRows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.name}</td>
                    <td>{row.origin}</td>
                    <td>{row.address}</td>
                    <td>{row.port}</td>
                    <td>{row.filters?.length ? row.filters.join(', ') : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="envoy-section">
          <div className="envoy-section-header">
            <h3>Clusters</h3>
            <input
              className="filter-input"
              type="text"
              value={clusterFilter}
              onChange={(event) => onClusterFilterChange(event.target.value)}
              placeholder="Filter clusters"
            />
          </div>
          <div className="envoy-table-wrapper">
            <table className="envoy-data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Origin</th>
                  <th>Type</th>
                  <th>Endpoints</th>
                  <th>Added via API</th>
                </tr>
              </thead>
              <tbody>
                {clusterRows.length === 0 && (
                  <tr>
                    <td colSpan={5} className="empty-value">No clusters match current filter</td>
                  </tr>
                )}
                {clusterRows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.name}</td>
                    <td>{row.origin}</td>
                    <td>{row.type}</td>
                    <td>{row.endpointCount ?? '—'}</td>
                    <td>{row.addedViaApi === undefined ? '—' : (row.addedViaApi ? 'Yes' : 'No')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="envoy-section">
          <div className="envoy-section-header">
            <h3>Routes</h3>
            <input
              className="filter-input"
              type="text"
              value={routesFilter}
              onChange={(event) => onRoutesFilterChange(event.target.value)}
              placeholder="Filter routes"
            />
          </div>
          <div className="envoy-table-wrapper">
            <table className="envoy-data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Origin</th>
                  <th>Virtual hosts</th>
                  <th>Routes</th>
                  <th>Sample domains</th>
                </tr>
              </thead>
              <tbody>
                {routesRows.length === 0 && (
                  <tr>
                    <td colSpan={5} className="empty-value">No routes match current filter</td>
                  </tr>
                )}
                {routesRows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.name}</td>
                    <td>{row.origin}</td>
                    <td>{row.virtualHostCount}</td>
                    <td>{row.routeCount}</td>
                    <td>
                      {row.sampleDomains.length > 0
                        ? `${row.sampleDomains.join(', ')}${row.totalDomains > row.sampleDomains.length ? '…' : ''}`
                        : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </aside>
  );
};

export default EnvoyDetails;