import React from "react";
import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { createApp } from "../src/main/resources/static/components.js";
import { computeLayout } from "../src/main/resources/static/topology.js";

const sampleGraph = {
  namespace: "default",
  generatedAt: "2025-09-25T10:00:00Z",
  summary: { nodes: 3, edges: 2 },
  warnings: [],
  nodes: [
    {
      id: "container:default/pod/app",
      type: "appContainer",
      properties: {
        namespace: "default",
        pod: "pod",
        container: "app",
        displayName: "app",
        containerType: "app"
      }
    },
    {
      id: "container:default/pod/istio-proxy",
      type: "sidecarContainer",
      properties: {
        namespace: "default",
        pod: "pod",
        container: "istio-proxy",
        displayName: "istio-proxy",
        containerType: "sidecar"
      }
    },
    {
      id: "external:reviews",
      type: "externalService",
      properties: {
        host: "reviews.company.com",
        displayName: "reviews.company.com"
      }
    }
  ],
  edges: [
    {
      id: "link",
      kind: "podLink",
      source: "container:default/pod/app",
      target: "container:default/pod/istio-proxy",
      properties: {}
    },
    {
      id: "traffic",
      kind: "traffic",
      source: "container:default/pod/istio-proxy",
      target: "external:reviews",
      properties: { destinationHost: "reviews.company.com" }
    }
  ]
};

describe("computeLayout", () => {
  it("groups containers by pod and positions them", () => {
    const layout = computeLayout(sampleGraph);
    const podGroup = layout.groups.find((group) => group.type === "pod");
    expect(podGroup).toBeDefined();
    expect(layout.nodes.find((node) => node.id.includes("istio-proxy"))).toBeDefined();
    layout.nodes.forEach((node) => {
      expect(Number.isFinite(node.x)).toBe(true);
      expect(Number.isFinite(node.y)).toBe(true);
    });
    expect(layout.edges.length).toBe(2);
  });
});

describe("TopologyApp", () => {
  it("renders summary and selection after loading", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => sampleGraph
    });
    const App = createApp(React, { fetchImpl: fetchMock, defaultNamespace: "default" });
    render(React.createElement(App));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.getByText("nodes")).toBeInTheDocument());

    const nodeButton = screen.getByLabelText("app");
    fireEvent.click(nodeButton);
    expect(screen.getByText(/Node container:default\/pod\/app/)).toBeInTheDocument();
  });
});
