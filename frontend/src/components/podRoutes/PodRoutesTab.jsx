import React, { useEffect, useMemo, useState } from 'react';
import NamespaceForm from '../NamespaceForm';
import EnvoyPodList from '../envoy/EnvoyPodList';
import { buildRouteConfigDetails } from '../../utils/envoyAggregators';

const PodRoutesTab = ({
  namespace,
  namespaceInput,
  onNamespaceInputChange,
  onNamespaceSubmit,
  refreshToken,
}) => {
  const [pods, setPods] = useState([]);
  const [podsLoading, setPodsLoading] = useState(false);
  const [podsError, setPodsError] = useState(null);
  const [podFilter, setPodFilter] = useState('');
  const [selectedPodName, setSelectedPodName] = useState(null);

  const [config, setConfig] = useState(null);
  const [configLoading, setConfigLoading] = useState(false);
  const [configError, setConfigError] = useState(null);

  const [routesFilter, setRoutesFilter] = useState('');
  const [expandedConfigId, setExpandedConfigId] = useState(null);
  const [expandedRouteIds, setExpandedRouteIds] = useState(new Set());

  useEffect(() => {
    const controller = new AbortController();
    async function fetchPods() {
      setPodsLoading(true);
      setPodsError(null);
      const namespaceQuery = namespace.trim();
      const query = namespaceQuery.length > 0 ? `?namespace=${encodeURIComponent(namespaceQuery)}` : '';
      try {
        const response = await fetch(`/api/envoy/pods${query}`, { signal: controller.signal });
        if (!response.ok) {
          throw new Error(`Request failed with status ${response.status}`);
        }
        const data = await response.json();
        const podList = Array.isArray(data.pods) ? data.pods : [];
        setPods(podList);
        if (!podList.some((pod) => pod.name === selectedPodName)) {
          setSelectedPodName(null);
          setConfig(null);
        }
      } catch (err) {
        if (controller.signal.aborted) {
          return;
        }
        setPodsError(err.message || 'Failed to load istio-proxy pods');
        setPods([]);
        setSelectedPodName(null);
        setConfig(null);
      } finally {
        if (!controller.signal.aborted) {
          setPodsLoading(false);
        }
      }
    }

    fetchPods();
    return () => controller.abort();
  }, [namespace, selectedPodName, refreshToken]);

  useEffect(() => {
    if (!selectedPodName) {
      setConfig(null);
      setConfigError(null);
      return undefined;
    }

    const controller = new AbortController();
    async function fetchConfig() {
      setConfigLoading(true);
      setConfigError(null);
      const namespaceQuery = namespace.trim();
      const query = namespaceQuery.length > 0 ? `?namespace=${encodeURIComponent(namespaceQuery)}` : '';
      try {
        const response = await fetch(`/api/envoy/pods/${encodeURIComponent(selectedPodName)}${query}`, {
          signal: controller.signal,
        });
        if (!response.ok) {
          throw new Error(`Request failed with status ${response.status}`);
        }
        const data = await response.json();
        setConfig(data);
      } catch (err) {
        if (controller.signal.aborted) {
          return;
        }
        setConfigError(err.message || 'Failed to load Envoy configuration');
        setConfig(null);
      } finally {
        if (!controller.signal.aborted) {
          setConfigLoading(false);
        }
      }
    }

    fetchConfig();
    return () => controller.abort();
  }, [namespace, selectedPodName, refreshToken]);

  const filteredPods = useMemo(() => {
    const needle = podFilter.trim().toLowerCase();
    if (!needle) {
      return pods;
    }
    return pods.filter((pod) => pod.name?.toLowerCase().includes(needle));
  }, [pods, podFilter]);

  const routeConfigs = useMemo(() => {
    const payload = config?.sections?.find((section) => section.id === 'routesFromConfigDump')?.payload;
    return buildRouteConfigDetails(payload);
  }, [config]);

  const filteredRouteConfigs = useMemo(() => {
    const needle = routesFilter.trim().toLowerCase();
    if (!needle) {
      return routeConfigs;
    }
    return routeConfigs.filter((configItem) => {
      if (configItem.name?.toLowerCase().includes(needle)) {
        return true;
      }
      return configItem.virtualHosts.some((vh) => {
        if (vh.name?.toLowerCase().includes(needle)) {
          return true;
        }
        if (vh.domains.some((domain) => domain.toLowerCase().includes(needle))) {
          return true;
        }
        return vh.routes.some((route) => (
          route.name?.toLowerCase().includes(needle)
          || route.matchSummary.toLowerCase().includes(needle)
          || route.actionSummary.toLowerCase().includes(needle)
        ));
      });
    });
  }, [routeConfigs, routesFilter]);

  const handleNamespaceSubmit = (value) => {
    onNamespaceSubmit(value.trim());
  };

  const toggleConfigRaw = (configId) => {
    setExpandedConfigId((prev) => (prev === configId ? null : configId));
  };

  const toggleRouteRaw = (routeId) => {
    setExpandedRouteIds((prev) => {
      const next = new Set(prev);
      if (next.has(routeId)) {
        next.delete(routeId);
      } else {
        next.add(routeId);
      }
      return next;
    });
  };

  return (
    <div className="app-shell">
      <section className="envoy-card">
        <NamespaceForm
          value={namespaceInput}
          onChange={onNamespaceInputChange}
          onSubmit={handleNamespaceSubmit}
          placeholder="istio-system"
          hint="Leave empty to use the backend default namespace."
        />

        <div className="form-control">
          <label htmlFor="pod-filter">Pod filter</label>
          <input
            id="pod-filter"
            type="text"
            value={podFilter}
            onChange={(event) => setPodFilter(event.target.value)}
            placeholder="Substring"
          />
        </div>

        <div className="status-bar">
          {podsLoading && <span className="status status--loading">Loading pods...</span>}
          {!podsLoading && podsError && <span className="status status--error">{podsError}</span>}
          {!podsLoading && !podsError && (
            <span className="status">Pods found: {filteredPods.length}</span>
          )}
        </div>

        <EnvoyPodList
          pods={filteredPods}
          selectedPodName={selectedPodName}
          onSelect={setSelectedPodName}
        />
      </section>

      <section className="pod-routes-details">
        <h2>Pod routes</h2>
        {!selectedPodName && <p className="placeholder">Select a pod to inspect Envoy routes.</p>}
        {selectedPodName && configLoading && <p className="status status--loading">Loading configuration...</p>}
        {selectedPodName && configError && <p className="status status--error">{configError}</p>}

        {selectedPodName && !configLoading && !configError && (
          <>
            <div className="form-control">
              <label htmlFor="routes-filter">Filter routes</label>
              <input
                id="routes-filter"
                type="text"
                value={routesFilter}
                onChange={(event) => setRoutesFilter(event.target.value)}
                placeholder="Search by config, virtual host, domain, match or action"
              />
            </div>

            {filteredRouteConfigs.length === 0 ? (
              <p className="placeholder">No route configurations match current filters.</p>
            ) : (
              <div className="pod-route-config-list">
                {filteredRouteConfigs.map((configItem) => (
                  <div key={configItem.id} className="pod-route-card">
                    <div className="pod-route-card-header">
                      <div>
                        <h3>{configItem.name}</h3>
                        <span className="pod-route-meta">Origin: {configItem.origin}</span>
                        <span className="pod-route-meta">Virtual hosts: {configItem.virtualHosts.length}</span>
                      </div>
                      <button
                        type="button"
                        className="small-button"
                        onClick={() => toggleConfigRaw(configItem.id)}
                      >
                        {expandedConfigId === configItem.id ? 'Hide raw' : 'Show raw'}
                      </button>
                    </div>

                    {configItem.virtualHosts.map((vh) => (
                      <div key={vh.id} className="pod-route-virtual-host">
                        <div className="pod-route-virtual-host-header">
                          <h4>{vh.name}</h4>
                          <span className="pod-route-meta">Domains: {vh.domains.join(', ') || '—'}</span>
                          {vh.requireTls && <span className="pod-route-meta">TLS required</span>}
                        </div>
                        <ul className="pod-route-list">
                          {vh.routes.map((route) => {
                            const isExpanded = expandedRouteIds.has(route.id);
                            return (
                              <li key={route.id} className="pod-route-item">
                                <div className="pod-route-item-header">
                                  <div className="pod-route-item-text">
                                    <strong>{route.name}</strong>
                                    <span className="pod-route-meta">Match: {route.matchSummary}</span>
                                    <span className="pod-route-meta">Action: {route.actionSummary}</span>
                                  </div>
                                  <button
                                    type="button"
                                    className="small-button"
                                    onClick={() => toggleRouteRaw(route.id)}
                                  >
                                    {isExpanded ? 'Hide raw' : 'Show raw'}
                                  </button>
                                </div>
                                {isExpanded && (
                                  <pre className="code-block">{JSON.stringify(route.raw, null, 2)}</pre>
                                )}
                              </li>
                            );
                          })}
                        </ul>
                      </div>
                    ))}

                    {expandedConfigId === configItem.id && (
                      <pre className="code-block">{JSON.stringify(configItem.raw, null, 2)}</pre>
                    )}
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
};

export default PodRoutesTab;

