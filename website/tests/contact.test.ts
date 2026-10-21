import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { test } from "node:test";
import {
  parse,
  compileScript,
  compileStyle,
  compileTemplate,
} from "@vue/compiler-sfc";
import { createSSRApp, computed, reactive, ref } from "vue";
import { renderToString } from "@vue/server-renderer";
import ts from "typescript";

const require = createRequire(import.meta.url);
const source = readFileSync(
  new URL("../app/components/ContactForm.vue", import.meta.url),
  "utf8",
);
const { descriptor, errors } = parse(source);
assert.deepEqual(errors, []);
const compiled = compileScript(descriptor, {
  id: "contact-test",
  inlineTemplate: true,
});
const javascript = ts.transpileModule(compiled.content, {
  compilerOptions: {
    module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2022,
  },
}).outputText;

for (const locale of ["zh", "en"]) {
  test(`contact form renders only retained fields (${locale})`, async () => {
    const module = { exports: {} as { default?: object } };
    // 提供 Nuxt 自动导入，让组件独立渲染，无需启动服务或打包。
    new Function(
      "require",
      "module",
      "exports",
      "computed",
      "reactive",
      "ref",
      "useJavaApi",
      javascript,
    )(require, module, module.exports, computed, reactive, ref, () => ({
      request: () => {
        throw new Error("Rendering must not submit an inquiry");
      },
    }));
    const html = await renderToString(
      createSSRApp(module.exports.default!, { locale }),
    );
    assert.equal((html.match(/<select\b/g) || []).length, 2);
    assert.equal((html.match(/<input\b/g) || []).length, 4);
    assert.equal((html.match(/<textarea\b/g) || []).length, 1);
    assert.equal((html.match(/<h1\b/g) || []).length, 1);
    assert.match(html, locale === "zh" ? /<h1>联系我们<\/h1>/ : /<h1>Contact Us<\/h1>/);
    assert.match(html, locale === "zh" ? /公司名称/ : /Company Name/);
    assert.match(html, locale === "zh" ? /发送留言/ : /Send Now/);
    assert.match(html, /class="contact-intro"/);
    assert.match(html, /class="contact-guide"/);
    assert.match(html, /class="full form-actions"/);
    assert.match(html, locale === "zh" ? /发送咨询/ : /Send an Inquiry/);
    assert.doesNotMatch(
      html,
      /网站地址|选择产品|Web Page Address|Select Item|type="url"/,
    );
    assert.doesNotMatch(compiled.content, /(?:product|website):\s*["']/);
  });
}

test("contact page removes old content and retains reusable styles", () => {
  const page = readFileSync(
    new URL("../app/pages/[locale]/[[...slug]].vue", import.meta.url),
    "utf8",
  );
  assert.match(page, /<ContactForm :locale="locale" \/>/);
  assert.match(page, /<section v-if="view\.type !== 'contact'" class="page-banner">/);
  assert.doesNotMatch(
    page,
    /contact-panel|contact-details|contactIntro|:products="products"/,
  );
  const parsedPage = parse(page);
  assert.deepEqual(parsedPage.errors, []);
  const pageScript = compileScript(parsedPage.descriptor, {
    id: "contact-page",
  });
  const pageTemplate = compileTemplate({
    source: parsedPage.descriptor.template!.content,
    filename: "app/pages/[locale]/[[...slug]].vue",
    id: "contact-page",
    compilerOptions: { bindingMetadata: pageScript.bindings },
  });
  assert.deepEqual(pageTemplate.errors, []);
  const css = readFileSync(
    new URL("../app/assets/main.css", import.meta.url),
    "utf8",
  );
  assert.match(css, /\.contact-panel\s*\{/);
  assert.match(css, /\.contact-details/);
  for (const style of descriptor.styles) {
    assert.match(style.content, /var\(--green\)/);
    assert.match(style.content, /var\(--soft\)/);
    assert.match(style.content, /@media \(max-width: 900px\)/);
    assert.match(style.content, /@media \(max-width: 640px\)/);
    assert.deepEqual(
      compileStyle({ source: style.content, id: "contact-test", scoped: true })
        .errors,
      [],
    );
  }
});
