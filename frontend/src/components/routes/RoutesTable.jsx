import React from 'react';
import { buildLinkLabel, formatPortList } from '../../utils/formatters';

const RoutesTable = ({ routes, isLoading, onNodeSelect, selectedNode }) => {
  const renderLinkChains = (route) => {
    if (!route.links.length) {
      return <span className="empty-value">No links</span>;
    }
    const chains = route.links.map((link, index) => ({
      key: `${link.fromId || 'unknown'}-${link.toId || 'unknown'}-${index}`,
      ...buildLinkLabel(link, route.nodesById || {}),
    }));

    return (
      <ul className="link-chain-list">
        {chains.map((chain) => (
          <li key={chain.key}>
            {chain.text}
            {chain.matchSummary && <span className="link-match"> [{chain.matchSummary}]</span>}
          </li>
        ))}
      </ul>
    );
  };

  const renderNodeChips = (route) => {
    if (!route.nodes.length) {
      return <span className="empty-value">No nodes</span>;
    }
    return route.nodes.map((node) => (
      <button
        key={node.id}
        type="button"
        className={`node-chip${selectedNode?.node?.id === node.id ? ' node-chip--active' : ''}`}
        onClick={() => onNodeSelect(route, node)}
      >
        {node.name || node.id || 'Unnamed'}
      </button>
    ));
  };

  if (isLoading && routes.length === 0) {
    return (
      <div className="table-wrapper">
        <table>
          <tbody>
            <tr>
              <td className="empty-value">Loading…</td>
            </tr>
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <div className="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>Route</th>
            <th>Ports</th>
            <th>Protocols</th>
            <th>Node types</th>
            <th>Link chains</th>
            <th>Nodes</th>
          </tr>
        </thead>
        <tbody>
          {routes.length === 0 && (
            <tr>
              <td colSpan={6} className="empty-value">
                {isLoading ? 'Loading…' : 'No routes match current filters'}
              </td>
            </tr>
          )}
          {routes.map((route, index) => (
            <tr key={`${route.destinationHost || 'route'}-${index}`}>
              <td>
                <div className="route-name">{route.destinationHost || 'Unnamed route'}</div>
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
              <td>{renderLinkChains(route)}</td>
              <td className="node-cell">{renderNodeChips(route)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default RoutesTable;