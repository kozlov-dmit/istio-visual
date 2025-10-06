import React, { useEffect, useMemo, useState } from 'react';
import NamespaceForm from '../NamespaceForm';
import EnvoyPodList from './EnvoyPodList';
import EnvoyDetails from './EnvoyDetails';
import {
  aggregateClusters,
  aggregateListeners,
  aggregateRoutesFromDump,
  aggregateEnvoyStats,
} from '../../utils/envoyAggregators';

const EnvoyTab = ({
  namespace,
  namespaceInput,
  onNamespaceInputChange,
  onNamespaceSubmit,
}) => {
  const [pods, setPods] = useState([]);
  const [podsLoading, setPodsLoading] = useState(false);
  const [podsError, setPodsError] = useState(null);
  const [podFilter, setPodFilter] = useState('');
  const [selectedPodName, setSelectedPodName] = useState(null);

  const [config, setConfig] = useState(null);
  const [configLoading, setConfigLoading] = useState(false);
  const [configError, setConfigError] = useState(null);

  const [listenerFilter, setListenerFilter] = useState('');
  const [clusterFilter, setClusterFilter] = useState('');
  const [routesFilter, setRoutesFilter] = useState('');
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
  }, [namespace, selectedPodName]);

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
  }, [namespace, selectedPodName]);

  const filteredPods = useMemo(() => {
    const needle = podFilter.trim().toLowerCase();
    if (!needle) {
      return pods;
    }
    return pods.filter((pod) => pod.name?.toLowerCase().includes(needle));
  }, [pods, podFilter]);

  const listenerRows = useMemo(() => (
    config ? aggregateListeners(config.sections?.find((section) => section.id === 'listenersFromConfigDump')?.payload) : []
  ), [config]);

  const clusterRows = useMemo(() => (
    config ? aggregateClusters(config.sections?.find((section) => section.id === 'clustersFromConfigDump')?.payload) : []
  ), [config]);

  const routesRows = useMemo(() => (
    config ? aggregateRoutesFromDump(config.sections?.find((section) => section.id === 'routesFromConfigDump')?.payload) : []
  ), [config]);

  const statsSections = useMemo(() => (
    config ? aggregateEnvoyStats(config.sections?.find((section) => section.id === 'stats')?.payload) : []
  ), [config]);

  const filteredStatsSections = useMemo(() => {
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

  const filteredListenerRows = useMemo(() => {
    const needle = listenerFilter.trim().toLowerCase();
    if (!needle) {
      return listenerRows;
    }
    return listenerRows.filter((row) => (
      [row.name, row.origin, row.address, row.port, ...(row.filters || [])]
        .filter(Boolean)
        .some((value) => value.toString().toLowerCase().includes(needle))
    ));
  }, [listenerRows, listenerFilter]);

  const filteredClusterRows = useMemo(() => {
    const needle = clusterFilter.trim().toLowerCase();
    if (!needle) {
      return clusterRows;
    }
    return clusterRows.filter((row) => (
      [row.name, row.origin, row.type, row.endpointCount, row.addedViaApi]
        .filter((value) => value !== undefined)
        .some((value) => value.toString().toLowerCase().includes(needle))
    ));
  }, [clusterRows, clusterFilter]);

  const filteredRoutesRows = useMemo(() => {
    const needle = routesFilter.trim().toLowerCase();
    if (!needle) {
      return routesRows;
    }
    return routesRows.filter((row) => (
      [row.name, row.origin, row.virtualHostCount, row.routeCount, ...(row.sampleDomains || [])]
        .filter(Boolean)
        .some((value) => value.toString().toLowerCase().includes(needle))
    ));
  }, [routesRows, routesFilter]);

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
          {podsLoading && <span className="status status--loading">Loading pods…</span>}
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

      <EnvoyDetails
        selectedPodName={selectedPodName}
        podSummary={config?.pod}
        warnings={config?.warnings || []}
        isLoading={configLoading}
        error={configError}
        listenerFilter={listenerFilter}
        onListenerFilterChange={setListenerFilter}
        listenerRows={filteredListenerRows}
        clusterFilter={clusterFilter}
        onClusterFilterChange={setClusterFilter}
        clusterRows={filteredClusterRows}
        routesFilter={routesFilter}
        onRoutesFilterChange={setRoutesFilter}
        routesRows={filteredRoutesRows}
        statsFilter={statsFilter}
        onStatsFilterChange={setStatsFilter}
        statsSections={filteredStatsSections}
        hasStats={statsSections.length > 0}
      />
    </div>
  );
};

export default EnvoyTab;
