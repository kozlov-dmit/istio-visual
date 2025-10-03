import React, { useEffect, useMemo, useState } from 'react';
import NamespaceForm from '../NamespaceForm';
import RoutesFilters from './RoutesFilters';
import RoutesTable from './RoutesTable';
import RouteDetails from './RouteDetails';
import { buildLinkLabel } from '../../utils/formatters';

const NODE_TYPE_OPTIONS = ['SERVICE_ENTRY', 'POD', 'DEPLOYMENT', 'MESH', 'UNKNOWN'];

const RoutesTab = ({
  namespace,
  namespaceInput,
  onNamespaceInputChange,
  onNamespaceSubmit,
}) => {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [nameFilter, setNameFilter] = useState('');
  const [protocolFilter, setProtocolFilter] = useState('ALL');
  const [selectedNodeTypes, setSelectedNodeTypes] = useState([]);
  const [selectedNode, setSelectedNode] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    async function fetchRoutes() {
      setLoading(true);
      setError(null);
      setSelectedNode(null);
      const namespaceQuery = namespace.trim();
      const query = namespaceQuery.length > 0 ? `?namespace=${encodeURIComponent(namespaceQuery)}` : '';
      try {
        const response = await fetch(`/api/routes${query}`, { signal: controller.signal });
        if (!response.ok) {
          throw new Error(`Request failed with status ${response.status}`);
        }
        const data = await response.json();
        setRoutes(Array.isArray(data.routes) ? data.routes : []);
      } catch (err) {
        if (controller.signal.aborted) {
          return;
        }
        setError(err.message || 'Failed to load routes');
        setRoutes([]);
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    fetchRoutes();
    return () => controller.abort();
  }, [namespace]);

  const normalizedRoutes = useMemo(() => (
    routes.map((route) => {
      const nodesMap = (route.nodes && typeof route.nodes === 'object') ? route.nodes : {};
      const nodes = Object.values(nodesMap);
      const links = Array.isArray(route.links) ? route.links : [];
      const protocols = Array.from(new Set(
        links.map((link) => (link.protocol ? link.protocol.toUpperCase() : null)).filter(Boolean)
      )).sort();
      const nodeTypes = Array.from(new Set(
        nodes.map((node) => (node.type ? node.type.toUpperCase() : 'UNKNOWN')).filter(Boolean)
      )).sort();
      return {
        ...route,
        links,
        nodes,
        nodesById: nodesMap,
        protocols,
        nodeTypes,
      };
    })
  ), [routes]);

  const protocolOptions = useMemo(() => {
    const fromData = new Set();
    normalizedRoutes.forEach((route) => {
      route.protocols.forEach((protocol) => fromData.add(protocol));
    });
    return ['ALL', ...Array.from(fromData).sort()];
  }, [normalizedRoutes]);

  const filteredRoutes = useMemo(() => {
    const lowerName = nameFilter.trim().toLowerCase();
    return normalizedRoutes.filter((route) => {
      const matchesName = lowerName.length === 0
        || (route.destinationHost && route.destinationHost.toLowerCase().includes(lowerName));
      const matchesProtocol = protocolFilter === 'ALL' || route.protocols.includes(protocolFilter);
      const matchesNodeType = selectedNodeTypes.length === 0
        || selectedNodeTypes.some((type) => route.nodeTypes.includes(type));
      return matchesName && matchesProtocol && matchesNodeType;
    });
  }, [normalizedRoutes, nameFilter, protocolFilter, selectedNodeTypes]);

  const handleNamespaceSubmit = (value) => {
    onNamespaceSubmit(value.trim());
  };

  const handleNodeSelect = (route, node) => {
    const inboundLinks = route.links.filter((link) => link.toId === node.id);
    const outboundLinks = route.links.filter((link) => link.fromId === node.id);
    setSelectedNode({ route, node, inboundLinks, outboundLinks });
  };

  const handleToggleNodeType = (type) => {
    setSelectedNodeTypes((previous) => (
      previous.includes(type)
        ? previous.filter((item) => item !== type)
        : [...previous, type]
    ));
  };

  return (
    <div className="app-shell">
      <section className="routes-card">
        <NamespaceForm
          value={namespaceInput}
          onChange={onNamespaceInputChange}
          onSubmit={handleNamespaceSubmit}
        />

        <RoutesFilters
          nameFilter={nameFilter}
          onNameFilterChange={setNameFilter}
          protocolFilter={protocolFilter}
          protocolOptions={protocolOptions}
          onProtocolFilterChange={setProtocolFilter}
          nodeTypeOptions={NODE_TYPE_OPTIONS}
          selectedNodeTypes={selectedNodeTypes}
          onToggleNodeType={handleToggleNodeType}
        />

        <div className="status-bar">
          {loading && <span className="status status--loading">Loading…</span>}
          {!loading && error && <span className="status status--error">{error}</span>}
          {!loading && !error && (
            <span className="status">Routes found: {filteredRoutes.length}</span>
          )}
        </div>

        <RoutesTable
          routes={filteredRoutes}
          isLoading={loading}
          onNodeSelect={handleNodeSelect}
          selectedNode={selectedNode}
          buildLinkLabel={buildLinkLabel}
        />
      </section>

      <RouteDetails
        selectedNode={selectedNode}
        onClear={() => setSelectedNode(null)}
      />
    </div>
  );
};

export default RoutesTab;