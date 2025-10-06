import React, { useEffect, useMemo, useState } from 'react';
import NamespaceForm from '../NamespaceForm';
import EnvoyPodList from './EnvoyPodList';
import { aggregateEnvoyStats } from '../../utils/envoyAggregators';

const EnvoyMetricsTab = ({
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

  const [statsSections, setStatsSections] = useState([]);
  const [hasStats, setHasStats] = useState(false);
  const [statsLoading, setStatsLoading] = useState(false);
  const [statsError, setStatsError] = useState(null);
  const [statsFilter, setStatsFilter] = useState('');

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
          setStatsSections([]);
          setHasStats(false);
        }
      } catch (err) {
        if (controller.signal.aborted) {
          return;
        }
        setPodsError(err.message || 'Failed to load istio-proxy pods');
        setPods([]);
        setSelectedPodName(null);
        setStatsSections([]);
        setHasStats(false);
      } finally {
        if (!controller.signal.aborted) {
          setPodsLoading(false);
        }
      }
    }

    fetchPods();
    return () => controller.abort();
  }, [namespace, refreshToken]);

  useEffect(() => {
    if (!selectedPodName) {
      setStatsSections([]);
      setHasStats(false);
      setStatsError(null);
      return undefined;
    }

    const controller = new AbortController();
    async function fetchStats() {
      setStatsLoading(true);
      setStatsError(null);
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
        const statsSection = data.sections?.find((section) => section.id === 'stats');
        const categories = aggregateEnvoyStats(statsSection?.payload);
        setStatsSections(categories);
        setHasStats(Array.isArray(categories) && categories.length > 0);
      } catch (err) {
        if (controller.signal.aborted) {
          return;
        }
        setStatsError(err.message || 'Failed to load Envoy stats');
        setStatsSections([]);
        setHasStats(false);
      } finally {
        if (!controller.signal.aborted) {
          setStatsLoading(false);
        }
      }
    }

    fetchStats();
    return () => controller.abort();
  }, [namespace, selectedPodName, refreshToken]);

  const filteredPods = useMemo(() => {
    const needle = podFilter.trim().toLowerCase();
    if (!needle) {
      return pods;
    }
    return pods.filter((pod) => pod.name?.toLowerCase().includes(needle));
  }, [pods, podFilter]);

  const filteredMetrics = useMemo(() => {
    const needle = statsFilter.trim().toLowerCase();
    if (!needle) {
      return statsSections;
    }
    return statsSections
      .map((section) => ({
        ...section,
        metrics: section.metrics.filter((metric) => (
          [metric.name, metric.metric, metric.scope, metric.type]
            .filter(Boolean)
            .some((value) => value.toString().toLowerCase().includes(needle))
        )),
      }))
      .filter((section) => section.metrics.length > 0);
  }, [statsSections, statsFilter]);

  const metricsEmpty = filteredMetrics.length === 0;
  const metricsPlaceholder = !hasStats
    ? 'No runtime metrics were returned by Envoy.'
    : 'No metrics match current filter.';

  const handleNamespaceSubmit = (value) => {
    onNamespaceSubmit(value.trim());
  };

  return (
    <div className="app-shell">
      <section className="envoy-card">
        <NamespaceForm
          value={namespaceInput}
          onChange={onNamespaceInputChange}
          onSubmit={handleNamespaceSubmit}
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

      <section className="envoy-details-card">
        <div className="envoy-details-header">
          <h2>Envoy metrics</h2>
        </div>

        {!selectedPodName && <p className="placeholder">Select an istio-proxy pod to inspect metrics.</p>}
        {selectedPodName && statsLoading && <p className="status status--loading">Loading metrics...</p>}
        {selectedPodName && statsError && <p className="status status--error">{statsError}</p>}

        {selectedPodName && !statsLoading && !statsError && (
          <div className="envoy-section">
            <div className="envoy-section-header">
              <h3>Runtime stats</h3>
              <input
                className="filter-input"
                type="text"
                value={statsFilter}
                onChange={(event) => setStatsFilter(event.target.value)}
                placeholder="Filter metrics"
              />
            </div>
            {metricsEmpty ? (
              <p className="empty-value">{metricsPlaceholder}</p>
            ) : (
              <div className="envoy-metrics-collection">
                {filteredMetrics.map((category) => (
                  <div key={category.id} className="envoy-metrics-group">
                    <h4>{category.title}</h4>
                    <div className="envoy-table-wrapper">
                      <table className="envoy-data-table envoy-metrics-table">
                        <thead>
                          <tr>
                            <th>Metric</th>
                            <th>Value</th>
                            <th>Type</th>
                          </tr>
                        </thead>
                        <tbody>
                          {category.metrics.map((metric) => (
                            <tr key={metric.name}>
                              <td>
                                <div className="envoy-metric-name" title={metric.name}>
                                  {metric.metric || metric.name}
                                </div>
                                {metric.scope ? (
                                  <div className="envoy-metric-scope">{metric.scope}</div>
                                ) : null}
                              </td>
                              <td className="envoy-metric-value">
                                {typeof metric.value === 'number'
                                  ? metric.value.toLocaleString()
                                  : (metric.value ?? 'n/a')}
                              </td>
                              <td>{metric.type || 'UNKNOWN'}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  );
};

export default EnvoyMetricsTab;
