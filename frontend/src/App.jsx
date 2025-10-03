import React, { useState } from 'react';
import RoutesTab from './components/routes/RoutesTab';
import EnvoyTab from './components/envoy/EnvoyTab';
import './App.css';

const TABS = {
  ROUTES: 'routes',
  ENVOY: 'envoy',
};

export default function App() {
  const [activeTab, setActiveTab] = useState(TABS.ROUTES);
  const [namespaceInput, setNamespaceInput] = useState('default');
  const [activeNamespace, setActiveNamespace] = useState('default');

  const handleNamespaceSubmit = (value) => {
    const trimmed = value.trim();
    setNamespaceInput(value);
    setActiveNamespace(trimmed);
  };

  return (
    <div className="app-background">
      <header className="app-header">
        <h1>Istio Route Explorer</h1>
        <p>
          Inspect Istio routes and Envoy sidecar configuration for a chosen namespace.
        </p>
      </header>

      <div className="tab-bar">
        <button
          type="button"
          className={`tab-button${activeTab === TABS.ROUTES ? ' tab-button--active' : ''}`}
          onClick={() => setActiveTab(TABS.ROUTES)}
        >
          Routes
        </button>
        <button
          type="button"
          className={`tab-button${activeTab === TABS.ENVOY ? ' tab-button--active' : ''}`}
          onClick={() => setActiveTab(TABS.ENVOY)}
        >
          Envoy
        </button>
      </div>

      {activeTab === TABS.ROUTES ? (
        <RoutesTab
          namespace={activeNamespace}
          namespaceInput={namespaceInput}
          onNamespaceInputChange={setNamespaceInput}
          onNamespaceSubmit={handleNamespaceSubmit}
        />
      ) : (
        <EnvoyTab
          namespace={activeNamespace}
          namespaceInput={namespaceInput}
          onNamespaceInputChange={setNamespaceInput}
          onNamespaceSubmit={handleNamespaceSubmit}
        />
      )}
    </div>
  );
}