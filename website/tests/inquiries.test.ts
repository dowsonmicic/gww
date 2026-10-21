import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { parse, compileScript, compileTemplate } from "@vue/compiler-sfc";
import { filterInquiries, inquiryListUrl, todayInquiryDate, inquiryExportUrl, formatInquiryTime, type InquiryRow } from "../app/data/inquiries.ts";

const rows = [
  { id: "old", locale: "zh", createdAt: "2026-10-19T23:59:59" },
  { id: "start", locale: "zh", createdAt: "2026-10-20T00:00:00" },
  { id: "new", locale: "en", createdAt: "2026-10-21T00:00:00" },
  { id: "end", locale: "zh", createdAt: "2026-10-20T23:59:59" },
] as InquiryRow[];

test("inquiry filters include full days and sort newest first without mutating records", () => {
  assert.deepEqual(filterInquiries(rows, "zh", "2026-10-20", "2026-10-20").map(r => r.id), ["end", "start"]);
  assert.deepEqual(filterInquiries(rows, "", "", "").map(r => r.id), ["new", "end", "start", "old"]);
  assert.equal(rows[0].id, "old");
  assert.equal(formatInquiryTime("2026-10-20T23:59:59"), "2026-10-20 23:59:59");
});

test("export uses same dates and language, or all records with no filter", () => {
  assert.equal(inquiryExportUrl("", "", ""), "/api/admin/inquiries/export");
  assert.equal(inquiryExportUrl("2026-10-20", "2026-10-21", "en"), "/api/admin/inquiries/export?startDate=2026-10-20&endDate=2026-10-21&locale=en");
});

test("list sends dates to database API and default end date uses today's local calendar", () => {
  assert.equal(inquiryListUrl("2026-10-20", "2026-10-21", "zh"),
    "/api/admin/inquiries?startDate=2026-10-20&endDate=2026-10-21&locale=zh");
  assert.equal(todayInquiryDate(new Date(2026, 9, 21, 0, 5)), "2026-10-21");
  assert.equal(todayInquiryDate(new Date(2026, 0, 2)), "2026-01-02");
});

test("missing inquiry times do not crash and stay outside date-filtered results", () => {
  const legacyRows = [
    { id: "missing", locale: "zh" },
    { id: "null", locale: "zh", createdAt: null },
    { id: "empty", locale: "zh", createdAt: "" },
    ...rows,
  ] as InquiryRow[];
  const all = filterInquiries(legacyRows, "", "", "");
  assert.deepEqual(all.slice(0, 4).map(r => r.id), ["new", "end", "start", "old"]);
  assert.equal(all.length, 7);
  for (const [start, end] of [["2026-10-20", ""], ["", "2026-10-20"], ["2026-10-20", "2026-10-20"]]) {
    assert.ok(filterInquiries(legacyRows, "", start, end).every(r => r.createdAt));
  }
  assert.equal(formatInquiryTime(undefined), "时间未提供");
  assert.equal(formatInquiryTime(null), "时间未提供");
  assert.equal(formatInquiryTime(""), "时间未提供");
});

test("admin component compiles with unmasked contacts, details and Excel export", () => {
  const filename = "../app/components/AdminDashboard.vue";
  const source = readFileSync(new URL(filename, import.meta.url), "utf8");
  const { descriptor, errors } = parse(source);
  assert.deepEqual(errors, []);
  const script = compileScript(descriptor, { id: "inquiry-admin-test" });
  const template = compileTemplate({
    source: descriptor.template!.content,
    filename,
    id: "inquiry-admin-test",
    compilerOptions: { bindingMetadata: script.bindings },
  });
  assert.deepEqual(template.errors, []);
  assert.doesNotMatch(source, /邮箱（脱敏）|电话（脱敏）/);
  assert.match(source, /查看详情/);
  assert.match(source, /导出 Excel/);
  assert.match(source, /提交时间/);
  assert.match(source, /ref\(todayInquiryDate\(\)\)/);
  assert.match(source, /inquiryListUrl\(inquiryStartDate\.value, inquiryEndDate\.value, inquiryLocale\.value\)/);
  assert.doesNotMatch(source, /filterInquiries\(/);
});
