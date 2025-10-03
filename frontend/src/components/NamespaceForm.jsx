import React from 'react';

const NamespaceForm = ({
  value,
  onChange,
  onSubmit,
  inputId = 'namespace-input',
  placeholder = 'istio-system',
  hint = 'Leave empty to use the backend default namespace.',
}) => {
  const handleSubmit = (event) => {
    event.preventDefault();
    onSubmit(value);
  };

  return (
    <form className="namespace-form" onSubmit={handleSubmit}>
      <div className="form-control">
        <label htmlFor={inputId}>Namespace</label>
        <div className="namespace-input-row">
          <input
            id={inputId}
            type="text"
            value={value}
            onChange={(event) => onChange(event.target.value)}
            placeholder={placeholder}
          />
          <button type="submit">Load</button>
        </div>
        <small className="hint">{hint}</small>
      </div>
    </form>
  );
};

export default NamespaceForm;