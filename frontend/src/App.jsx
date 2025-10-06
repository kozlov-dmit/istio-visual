import React, { useState } from 'react';
import RoutesTab from './components/routes/RoutesTab';
import EnvoyTab from './components/envoy/EnvoyTab';
import EnvoyMetricsTab from './components/envoy/EnvoyMetricsTab';
import PodRoutesTab from './components/podRoutes/PodRoutesTab';
import './App.css';

const TABS = {
  ROUTES: 'routes',
  ENVOY: 'envoy',
  ENVOY_METRICS: 'envoyMetrics',
  POD_ROUTES: 'podRoutes',
};

export default function App() {
  const [activeTab, setActiveTab] = useState(TABS.ROUTES);
  const [namespaceInput, setNamespaceInput] = useState('default');
  const [activeNamespace, setActiveNamespace] = useState('default');
  const [refreshToken, setRefreshToken] = useState(0);

  const handleNamespaceInputChange = (value) => {
    setNamespaceInput(value);
  };

  const applyNamespace = (value) => {
    const trimmed = value.trim();
    setNamespaceInput(trimmed);
    setActiveNamespace(trimmed);
  };

  const handleNamespaceSubmit = (value) => {
    applyNamespace(value);
    setRefreshToken((token) => token + 1);
  };

  const handleLoadClick = () => {
    applyNamespace(namespaceInput);
    setRefreshToken((token) => token + 1);
  };

  const handleRefreshClick = () => {
    setRefreshToken((token) => token + 1);
  };

  return (
    <div className="app-background">
      <header className="app-header">
        <div className="header-top-row">
          <div>
            <h1>Istio Route Explorer</h1>
            <p>
              Inspect Istio routes and Envoy sidecar configuration for a chosen namespace.
            </p>
          </div>
          <div className="global-action-bar">
            <button type="button" className="primary-button" onClick={handleLoadClick}>
              Load
            </button>
            <button type="button" className="secondary-button" onClick={handleRefreshClick}>
              Refresh
            </button>
          </div>
        </div>
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
          className={`tab-button${activeTab === TABS.ENVOY_METRICS ? ' tab-button--active' : ''}`}
          onClick={() => setActiveTab(TABS.ENVOY_METRICS)}
        >
          Envoy Metrics
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
          refreshToken={refreshToken}
        />
      )}

      {activeTab === TABS.ENVOY && (
        <EnvoyTab
          namespace={activeNamespace}
          namespaceInput={namespaceInput}
          onNamespaceInputChange={handleNamespaceInputChange}
          onNamespaceSubmit={handleNamespaceSubmit}
          refreshToken={refreshToken}
        />
      )}

      {activeTab === TABS.ENVOY_METRICS && (
        <EnvoyMetricsTab
          namespace={activeNamespace}
          namespaceInput={namespaceInput}
          onNamespaceInputChange={handleNamespaceInputChange}
          onNamespaceSubmit={handleNamespaceSubmit}
          refreshToken={refreshToken}
        />
      )}

      {activeTab === TABS.POD_ROUTES && (
        <PodRoutesTab
          namespace={activeNamespace}
          namespaceInput={namespaceInput}
          onNamespaceInputChange={handleNamespaceInputChange}
          onNamespaceSubmit={handleNamespaceSubmit}
          refreshToken={refreshToken}
        />
      )}
    </div>
  );
}
