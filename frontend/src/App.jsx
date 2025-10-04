import React, { useState } from 'react';
import RoutesTab from './components/routes/RoutesTab';
import EnvoyTab from './components/envoy/EnvoyTab';
import PodRoutesTab from './components/podRoutes/PodRoutesTab';
import './App.css';

const TABS = {
  ROUTES: 'routes',
  ENVOY: 'envoy',
  POD_ROUTES: 'podRoutes',
};

export default function App() {
  const [activeTab, setActiveTab] = useState(TABS.ROUTES);
  const [namespaceInput, setNamespaceInput] = useState('default');
  const [activeNamespace, setActiveNamespace] = useState('default');

  const handleNamespaceInputChange = (value) => {
    setNamespaceInput(value);
  };

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
        <button
          type="button"
          className={`tab-button${activeTab === TABS.POD_ROUTES ? ' tab-button--active' : ''}`}
          onClick={() => setActiveTab(TABS.POD_ROUTES)}
        >
          Pod Routes
        </button>
      </div>

      {activeTab === TABS.ROUTES && (
        <RoutesTab
          namespace={activeNamespace}
          namespaceInput={namespaceInput}
          onNamespaceInputChange={handleNamespaceInputChange}
          onNamespaceSubmit={handleNamespaceSubmit}
        />
      )}

      {activeTab === TABS.ENVOY && (
        <EnvoyTab
          namespace={activeNamespace}
          namespaceInput={namespaceInput}
          onNamespaceInputChange={handleNamespaceInputChange}
          onNamespaceSubmit={handleNamespaceSubmit}
        />
      )}

      {activeTab === TABS.POD_ROUTES && (
        <PodRoutesTab
          namespace={activeNamespace}
          namespaceInput={namespaceInput}
          onNamespaceInputChange={handleNamespaceInputChange}
          onNamespaceSubmit={handleNamespaceSubmit}
        />
      )}
    </div>
  );
}