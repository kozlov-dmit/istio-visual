import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "jsdom",
    setupFiles: ["./ui-tests/setup.js"],
    globals: true
  }
});
