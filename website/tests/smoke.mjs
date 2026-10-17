import { spawn } from "node:child_process";
import assert from "node:assert/strict";
import { setTimeout } from "node:timers/promises";

async function verify(locale, port) {
  const server = spawn(process.execPath, [".output/server/index.mjs"], {
    env: {
      ...process.env,
      PORT: String(port),
      HOST: "127.0.0.1",
      SITE_DEFAULT_LOCALE: locale,
    },
    stdio: ["ignore", "pipe", "pipe"],
  });
  let output = "";
  server.stdout.on("data", (chunk) => {
    output += chunk;
  });
  server.stderr.on("data", (chunk) => {
    output += chunk;
  });
  const base = `http://127.0.0.1:${port}`;
  try {
    let ready = false;
    for (let attempt = 0; attempt < 30; attempt++) {
      try {
        await fetch(base);
        ready = true;
        break;
      } catch {
        await setTimeout(200);
      }
    }
    assert.ok(ready, output);
    const root = await fetch(base, { redirect: "manual" });
    assert.equal(root.status, 302);
    assert.equal(root.headers.get("location"), `/${locale}`);
    const remembered = await fetch(base, {
      redirect: "manual",
      headers: { cookie: "site-locale=en" },
    });
    assert.equal(remembered.headers.get("location"), "/en");
    for (const language of ["zh", "en"]) {
      for (const path of [
        "",
        "/about",
        "/products",
        "/products/8400",
        "/brands",
        "/news",
        "/news/website-preview",
        "/knowledge",
        "/knowledge/choosing-materials",
        "/honors",
        "/contact",
      ]) {
        const response = await fetch(`${base}/${language}${path}`);
        assert.equal(response.status, 200, `${language}${path}`);
        const html = await response.text();
        assert.ok(html.includes(`lang="${language}"`));
        assert.ok(html.includes("noindex"));
        assert.ok(!html.includes('href="/admin'));
        if (path === "/products/8400") assert.ok(html.includes("POE 8400"));
      }
    }
    for (const path of [
      "/zh/products/missing",
      "/zh/about/extra",
      "/vi",
      "/unknown",
    ]) {
      assert.equal((await fetch(base + path)).status, 404, path);
    }
    for (const path of [
      "/images/lb/block_1.webp",
      "/images/lb/block_2.webp",
      "/images/lb/block_3.webp",
      "/images/brands/brands.webp",
      "/images/show/8400.jpg",
      "/images/show/8440.jpg",
      "/images/show/8450jpg.jpg",
      "/images/show/8480.jpg",
      "/images/show/8540.jpg",
      "/images/show/8842.jpg",
    ]) {
      assert.equal((await fetch(base + path)).status, 200, path);
    }
    console.log(`HTTP smoke checks passed: default=${locale}`);
  } finally {
    server.kill();
    await new Promise((resolve) => {
      if (server.exitCode !== null) resolve();
      else server.once("exit", resolve);
    });
  }
}
await verify("zh", 3211);
await verify("en", 3212);
