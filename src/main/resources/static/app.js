import React from "https://esm.sh/react@18.3.1";
import { createRoot } from "https://esm.sh/react-dom@18.3.1/client";
import { createApp } from "./components.js";

const App = createApp(React, { fetchImpl: window.fetch.bind(window), defaultNamespace: "default" });
const container = document.getElementById("root");
if (container) {
    const root = createRoot(container);
    root.render(React.createElement(App));
}
