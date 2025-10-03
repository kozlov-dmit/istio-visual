import React from 'react';

const RoutesFilters = ({
  nameFilter,
  onNameFilterChange,
  protocolFilter,
  protocolOptions,
  onProtocolFilterChange,
  nodeTypeOptions,
  selectedNodeTypes,
  onToggleNodeType,
}) => (
  <div className="filters">
    <div className="form-control">
      <label htmlFor="name-filter">Route name</label>
      <input
        id="name-filter"
        type="text"
        value={nameFilter}
        onChange={(event) => onNameFilterChange(event.target.value)}
        placeholder="Substring"
      />
    </div>
    <div className="form-control">
      <label htmlFor="protocol-filter">Protocol</label>
      <select
        id="protocol-filter"
        value={protocolFilter}
        onChange={(event) => onProtocolFilterChange(event.target.value)}
      >
        {protocolOptions.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </div>
    <fieldset className="form-control form-control--fieldset">
      <legend>Node types</legend>
      <div className="checkbox-grid">
        {nodeTypeOptions.map((type) => (
          <label key={type} className="checkbox-item">
            <input
              type="checkbox"
              checked={selectedNodeTypes.includes(type)}
              onChange={() => onToggleNodeType(type)}
            />
            <span>{type}</span>
          </label>
        ))}
      </div>
    </fieldset>
  </div>
);

export default RoutesFilters;