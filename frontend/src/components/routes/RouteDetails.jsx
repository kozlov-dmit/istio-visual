import React from 'react';
import { toMatchSummary } from '../../utils/formatters';

const RouteDetails = ({ selectedNode, onClear }) => {
  if (!selectedNode) {
    return (
      <aside className="details-card">
        <h2>Node details</h2>
        <p className="placeholder">Select a node in the table to inspect details.</p>
      </aside>
    );
  }

  const { route, node, inboundLinks, outboundLinks } = selectedNode;
  const sourceLabel = (link) => (
    route.nodesById?.[link.fromId]?.name
      || route.nodesById?.[link.fromId]?.id
      || link.fromId
      || 'N/A'
  );
  const targetLabel = (link) => (
    route.nodesById?.[link.toId]?.name
      || route.nodesById?.[link.toId]?.id
      || link.toId
      || 'N/A'
  );

  return (
    <aside className="details-card">
      <h2>Node details</h2>
      <div className="details">
        <div className="detail-row">
          <span className="detail-label">Route</span>
          <span className="detail-value">{route.destinationHost || 'N/A'}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">ID</span>
          <span className="detail-value">{node.id || 'N/A'}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Name</span>
          <span className="detail-value">{node.name || 'N/A'}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Type</span>
          <span className="detail-value">{node.type || 'N/A'}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Namespace</span>
          <span className="detail-value">{node.metadata?.namespace || 'N/A'}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Labels</span>
          <span className="detail-value">
            {node.metadata?.labels
              ? Object.entries(node.metadata.labels).map(([key, value]) => (
                  <span key={key} className="badge">{`${key}=${value}`}</span>
                ))
              : 'N/A'}
          </span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Annotations</span>
          <span className="detail-value">
            {node.metadata?.annotations
              ? Object.entries(node.metadata.annotations).map(([key, value]) => (
                  <span key={key} className="badge">{`${key}=${value}`}</span>
                ))
              : 'N/A'}
          </span>
        </div>
        {node.comments && node.comments.length > 0 && (
          <div className="detail-row">
            <span className="detail-label">Comments</span>
            <span className="detail-value comment-list">
              {node.comments.map((comment, index) => (
                <span key={index} className="comment-item">{comment}</span>
              ))}
            </span>
          </div>
        )}
        {inboundLinks.length > 0 && (
          <div className="detail-row">
            <span className="detail-label">Inbound</span>
            <ul className="link-list">
              {inboundLinks.map((link, index) => {
                const matchSummary = toMatchSummary(link.matches);
                return (
                  <li key={`in-${index}`}>
                    {`${sourceLabel(link)} -> ${node.id || 'N/A'} (${link.protocol || 'N/A'}:${link.port || 'N/A'})`}
                    {matchSummary && <span className="link-match"> [{matchSummary}]</span>}
                  </li>
                );
              })}
            </ul>
          </div>
        )}
        {outboundLinks.length > 0 && (
          <div className="detail-row">
            <span className="detail-label">Outbound</span>
            <ul className="link-list">
              {outboundLinks.map((link, index) => {
                const matchSummary = toMatchSummary(link.matches);
                return (
                  <li key={`out-${index}`}>
                    {`${node.id || 'N/A'} -> ${targetLabel(link)} (${link.protocol || 'N/A'}:${link.port || 'N/A'})`}
                    {matchSummary && <span className="link-match"> [{matchSummary}]</span>}
                  </li>
                );
              })}
            </ul>
          </div>
        )}
        <button type="button" className="clear-button" onClick={onClear}>
          Clear selection
        </button>
      </div>
    </aside>
  );
};

export default RouteDetails;