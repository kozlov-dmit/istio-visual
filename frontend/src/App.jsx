import React, { useEffect, useMemo, useState } from 'react';
import './App.css';

const NODE_TYPE_OPTIONS = ['SERVICE_ENTRY', 'POD', 'DEPLOYMENT', 'MESH', 'UNKNOWN'];

const formatPortList = (ports) => {
  if (!ports || ports.length === 0) {
    return 'N/A';
  }
  return ports.join(', ');
};

const formatMatch = (match) => {
  if (!match) {
    return '';
  }
  return `${match.field}:${match.kind}=${match.value}`;
};

const toMatchSummary = (matches) => {
  if (!Array.isArray(matches)) {
    return '';
  }
  return matches.map(formatMatch).filter(Boolean).join(', ');
};

export default function App() {
  const [namespaceInput, setNamespaceInput] = useState('default');
  const [activeNamespace, setActiveNamespace] = useState('default');
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [nameFilter, setNameFilter] = useState('');
  const [protocolFilter, setProtocolFilter] = useState('ALL');
  const [selectedNodeTypes, setSelectedNodeTypes] = useState([]);
  const [selectedNode, setSelectedNode] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function loadRoutes() {
      setLoading(true);
      setError(null);
      const namespace = activeNamespace.trim();
      const query = namespace.length > 0 ? `?namespace=${encodeURIComponent(namespace)}` : '';
      try {
        const response = await fetch(`/api/routes${query}`);
        if (!response.ok) {
          throw new Error(`Request failed with status ${response.status}`);
        }
        const data = await response.json();
        if (!cancelled) {
          setRoutes(Array.isArray(data.routes) ? data.routes : []);
          setSelectedNode(null);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || 'Failed to load routes');
          setRoutes([]);
          setSelectedNode(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadRoutes();
    return () => {
      cancelled = true;
    };
  }, [activeNamespace]);

  const normalizedRoutes = useMemo(() => {
    return routes.map((route) => {
      const links = Array.isArray(route.links) ? route.links : [];
      const nodesMap = route.nodes && typeof route.nodes === 'object' ? route.nodes : {};
      const nodes = Object.values(nodesMap);
      const protocols = Array.from(
        new Set(
          links
            .map((link) => (link.protocol ? link.protocol.toUpperCase() : null))
            .filter((protocol) => protocol)
        )
      ).sort();
      const nodeTypes = Array.from(
        new Set(
          nodes
            .map((node) => (node.type ? node.type.toUpperCase() : 'UNKNOWN'))
            .filter(Boolean)
        )
      ).sort();
      return {
        ...route,
        links,
        nodes,
        protocols,
        nodeTypes,
      };
    });
  }, [routes]);

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

  const handleNamespaceSubmit = (event) => {
    event.preventDefault();
    setActiveNamespace(namespaceInput);
  };

  const toggleNodeType = (type) => {
    setSelectedNodeTypes((prev) => (
      prev.includes(type)
        ? prev.filter((item) => item !== type)
        : [...prev, type]
    ));
  };

  const handleNodeSelect = (route, node) => {
    if (!node) {
      return;
    }
    const inboundLinks = route.links.filter((link) => link.toId === node.id);
    const outboundLinks = route.links.filter((link) => link.fromId === node.id);
    setSelectedNode({ route, node, inboundLinks, outboundLinks });
  };

  const nodeTypeIsChecked = (type) => selectedNodeTypes.includes(type);

  const renderNodeBadges = (route) => {
    if (!route.nodes.length) {
      return <span className="empty-value">Нет узлов</span>;
    }
    return route.nodes.map((node) => (
      <button
        key={node.id}
        type="button"
        className={`node-chip${selectedNode?.node?.id === node.id ? ' node-chip--active' : ''}`}
        onClick={() => handleNodeSelect(route, node)}
      >
        {node.name || node.id || 'Без имени'}
      </button>
    ));
  };

  return (
    <div className="app-background">
      <header className="app-header">
        <h1>Istio Route Explorer</h1>
        <p>
          Просмотр маршрутов Istio в выбранном namespace. Используйте фильтры, чтобы найти нужные маршруты, и
          кликните на узел, чтобы увидеть подробности.
        </p>
      </header>
      <div className="app-shell">
        <section className="routes-card">
          <form className="namespace-form" onSubmit={handleNamespaceSubmit}>
            <div className="form-control">
              <label htmlFor="namespace-input">Namespace</label>
              <div className="namespace-input-row">
                <input
                  id="namespace-input"
                  type="text"
                  value={namespaceInput}
                  onChange={(event) => setNamespaceInput(event.target.value)}
                  placeholder="Например: istio-system"
                />
                <button type="submit">Загрузить</button>
              </div>
              <small className="hint">Оставьте поле пустым, чтобы использовать namespace по умолчанию на сервере.</small>
            </div>
          </form>

          <div className="filters">
            <div className="form-control">
              <label htmlFor="name-filter">Имя маршрута</label>
              <input
                id="name-filter"
                type="text"
                value={nameFilter}
                onChange={(event) => setNameFilter(event.target.value)}
                placeholder="Часть имени"
              />
            </div>
            <div className="form-control">
              <label htmlFor="protocol-filter">Протокол</label>
              <select
                id="protocol-filter"
                value={protocolFilter}
                onChange={(event) => setProtocolFilter(event.target.value)}
              >
                {protocolOptions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </div>
            <fieldset className="form-control form-control--fieldset">
              <legend>Типы узлов</legend>
              <div className="checkbox-grid">
                {NODE_TYPE_OPTIONS.map((type) => (
                  <label key={type} className="checkbox-item">
                    <input
                      type="checkbox"
                      checked={nodeTypeIsChecked(type)}
                      onChange={() => toggleNodeType(type)}
                    />
                    <span>{type}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          </div>

          <div className="status-bar">
            {loading && <span className="status status--loading">Загрузка...</span>}
            {!loading && error && <span className="status status--error">{error}</span>}
            {!loading && !error && (
              <span className="status">Найдено маршрутов: {filteredRoutes.length}</span>
            )}
          </div>

          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Маршрут</th>
                  <th>Порты</th>
                  <th>Протоколы</th>
                  <th>Типы узлов</th>
                  <th>Узлы</th>
                </tr>
              </thead>
              <tbody>
                {filteredRoutes.length === 0 && (
                  <tr>
                    <td colSpan={5} className="empty-value">
                      {loading ? 'Загрузка...' : 'Маршруты не найдены'}
                    </td>
                  </tr>
                )}
                {filteredRoutes.map((route, index) => (
                  <tr key={`${route.destinationHost || 'route'}-${index}`}>
                    <td>
                      <div className="route-name">{route.destinationHost || 'Без имени'}</div>
                    </td>
                    <td>{formatPortList(route.destinationPorts)}</td>
                    <td>
                      {route.protocols.length > 0
                        ? route.protocols.join(', ')
                        : <span className="empty-value">N/A</span>}
                    </td>
                    <td>
                      {route.nodeTypes.length > 0
                        ? route.nodeTypes.join(', ')
                        : <span className="empty-value">N/A</span>}
                    </td>
                    <td className="node-cell">{renderNodeBadges(route)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <aside className="details-card">
          <h2>Детали узла</h2>
          {!selectedNode && <p className="placeholder">Выберите узел в таблице слева.</p>}
          {selectedNode && (
            <div className="details">
              <div className="detail-row">
                <span className="detail-label">Маршрут</span>
                <span className="detail-value">{selectedNode.route.destinationHost || 'N/A'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Идентификатор</span>
                <span className="detail-value">{selectedNode.node.id || 'N/A'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Имя</span>
                <span className="detail-value">{selectedNode.node.name || 'N/A'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Тип</span>
                <span className="detail-value">{selectedNode.node.type || 'N/A'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Namespace</span>
                <span className="detail-value">{selectedNode.node.metadata?.namespace || 'N/A'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Labels</span>
                <span className="detail-value">
                  {selectedNode.node.metadata?.labels
                    ? Object.entries(selectedNode.node.metadata.labels).map(([key, value]) => (
                        <span key={key} className="badge">{`${key}=${value}`}</span>
                      ))
                    : 'N/A'}
                </span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Annotations</span>
                <span className="detail-value">
                  {selectedNode.node.metadata?.annotations
                    ? Object.entries(selectedNode.node.metadata.annotations).map(([key, value]) => (
                        <span key={key} className="badge">{`${key}=${value}`}</span>
                      ))
                    : 'N/A'}
                </span>
              </div>
              {selectedNode.node.comments && selectedNode.node.comments.length > 0 && (
                <div className="detail-row">
                  <span className="detail-label">Комментарии</span>
                  <span className="detail-value comment-list">
                    {selectedNode.node.comments.map((comment, idx) => (
                      <span key={idx} className="comment-item">{comment}</span>
                    ))}
                  </span>
                </div>
              )}
              {selectedNode.inboundLinks.length > 0 && (
                <div className="detail-row">
                  <span className="detail-label">Входящие</span>
                  <ul className="link-list">
                    {selectedNode.inboundLinks.map((link, idx) => {
                      const matchSummary = toMatchSummary(link.matches);
                      return (
                        <li key={`in-${link.fromId || 'unknown'}-${idx}`}>
                          {link.fromId || 'N/A'}
                          {' -> '}
                          {selectedNode.node.id || 'N/A'} ({link.protocol || 'N/A'}:{link.port || 'N/A'})
                          {matchSummary && <span className="link-match"> [{matchSummary}]</span>}
                        </li>
                      );
                    })}
                  </ul>
                </div>
              )}
              {selectedNode.outboundLinks.length > 0 && (
                <div className="detail-row">
                  <span className="detail-label">Исходящие</span>
                  <ul className="link-list">
                    {selectedNode.outboundLinks.map((link, idx) => {
                      const matchSummary = toMatchSummary(link.matches);
                      return (
                        <li key={`out-${link.toId || 'unknown'}-${idx}`}>
                          {selectedNode.node.id || 'N/A'}
                          {' -> '}
                          {link.toId || 'N/A'} ({link.protocol || 'N/A'}:{link.port || 'N/A'})
                          {matchSummary && <span className="link-match"> [{matchSummary}]</span>}
                        </li>
                      );
                    })}
                  </ul>
                </div>
              )}
              <button type="button" className="clear-button" onClick={() => setSelectedNode(null)}>
                Очистить выбор
              </button>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
