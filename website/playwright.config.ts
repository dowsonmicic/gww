import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests/e2e",
  workers: 1,
  use: { baseURL: "http://127.0.0.1:3213", headless: true },
  webServer: {
    command: "node .output/server/index.mjs",
    url: "http://127.0.0.1:3213/zh",
    env: { PORT: "3213", HOST: "127.0.0.1", SITE_DEFAULT_LOCALE: "zh" },
    reuseExistingServer: false,
  },
});
