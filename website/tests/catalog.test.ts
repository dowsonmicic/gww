import assert from "node:assert/strict";
import { test } from "node:test";
import {
  filterProducts,
  resolvePath,
  products,
  text,
  locales,
} from "../app/data/catalog.ts";

test("catalog search handles whitespace and case, and combines filters", () => {
  assert.equal(filterProducts().length, 6);
  assert.equal(filterProducts("  dow ").length, 6);
  assert.deepEqual(
    filterProducts("8400").map((p) => p.id),
    ["8400"],
  );
  assert.equal(filterProducts("", "PC").length, 0);
  assert.equal(filterProducts("", "POE", "DOW").length, 6);
  assert.equal(filterProducts("missing").length, 0);
});

test("routes reject unknown detail pages and invalid nesting", () => {
  assert.equal(resolvePath([])?.type, "home");
  for (const product of products)
    assert.equal(resolvePath(["products", product.id])?.type, "product");
  assert.equal(resolvePath(["products", "unknown"]), null);
  assert.equal(resolvePath(["about", "extra"]), null);
  assert.equal(resolvePath(["news", "choosing-materials"])?.type, "article");
  assert.equal(resolvePath(["knowledge", "choosing-materials"]), null);
  assert.equal(resolvePath(["knowledge"]), null);
  assert.equal(resolvePath(["honors"]), null);
  assert.equal(resolvePath(["contact"])?.type, "contact");
});

test("both languages have the same interface keys", () => {
  assert.deepEqual(Object.keys(text.zh).sort(), Object.keys(text.en).sort());
  assert.deepEqual(locales, ["zh", "en"]);
  assert.equal(text.zh.nav.length, text.en.nav.length);
});
