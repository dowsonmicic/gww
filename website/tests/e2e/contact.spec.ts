import { test, expect } from "@playwright/test";
import { execFileSync } from "node:child_process";

test("bilingual inquiries and site settings persist in MySQL", async ({
  page,
}) => {
  test.skip(
    !process.env.ADMIN_TEST_PASSWORD || !process.env.MYSQL_TEST_DATABASE,
    "需要独立本地开发库",
  );
  test.setTimeout(60000);
  page.setDefaultTimeout(5000);
  const marker = `CONTACT-CHECK-${Date.now()}`;
  const ids: string[] = [];
  const old = (
    await (await page.request.get("/api/public/site-settings")).json()
  ).data;
  let changed = false;
  try {
    for (const locale of ["zh", "en"]) {
      await page.goto(`/${locale}/contact`);
      await page
        .getByLabel(locale === "zh" ? "姓名 *" : "Name *", { exact: true })
        .fill(`${marker}-${locale}`);
      await page
        .getByLabel(locale === "zh" ? "邮箱 *" : "Email *", { exact: true })
        .fill("contact-test@example.com");
      await page
        .getByLabel(locale === "zh" ? "电话 *" : "Phone No. *", { exact: true })
        .fill("+84123456789");
      await page.locator(".inquiry-form select").first().selectOption("VN");
      await page
        .getByLabel(locale === "zh" ? "公司名称" : "Company Name", {
          exact: true,
        })
        .fill(marker);
      await page
        .getByLabel(locale === "zh" ? "留言内容 *" : "Message *", {
          exact: true,
        })
        .fill(marker);
      const saved = page.waitForResponse(
        (r) =>
          r.url().endsWith("/api/public/inquiries") &&
          r.request().method() === "POST",
      );
      await page
        .getByRole("button", {
          name: locale === "zh" ? "发送留言" : "Send Now",
          exact: true,
        })
        .click();
      const result = await (await saved).json();
      expect(result.code).toBe(0);
      ids.push(result.data);
      await expect(page.getByRole("status")).toContainText(
        locale === "zh" ? "提交成功" : "Thank you",
      );
    }
    await page.goto("/admin");
    await page.getByLabel("管理员账号").fill("admin");
    await page.getByLabel("管理员密码").fill(process.env.ADMIN_TEST_PASSWORD!);
    await page.getByRole("button", { name: "登录", exact: true }).click();
    await expect(
      page.getByRole("heading", { name: "工作台", exact: true }),
    ).toBeVisible();
    await page
      .locator(".admin-sidebar")
      .getByRole("button", { name: /客户询盘/ })
      .click();
    await expect(
      page.getByRole("cell", { name: `${marker}-zh`, exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("cell", { name: `${marker}-en`, exact: true }),
    ).toHaveCount(0);
    await page
      .getByRole("button", { name: "English 询盘", exact: true })
      .click();
    await expect(
      page.getByRole("cell", { name: `${marker}-en`, exact: true }),
    ).toBeVisible();
    await page
      .locator(".admin-sidebar")
      .getByRole("button", { name: /网站设置/ })
      .click();
    await page
      .getByRole("textbox", { name: "中文企业名称", exact: true })
      .fill(marker);
    await page
      .getByRole("textbox", { name: "英文企业名称", exact: true })
      .fill(`${marker}-EN`);
    await page
      .getByRole("textbox", { name: "联系邮箱", exact: true })
      .fill("info-test@example.com");
    const savedSettings = page.waitForResponse(
      (r) =>
        r.url().endsWith("/api/admin/site-settings") &&
        r.request().method() === "PUT",
    );
    await page
      .getByRole("button", { name: "保存网站设置", exact: true })
      .click();
    expect((await (await savedSettings).json()).code).toBe(0);
    changed = true;
    for (const locale of ["zh", "en"]) {
      const html = await page.request.get(`/${locale}/contact`);
      expect(await html.text()).toContain(
        locale === "zh" ? marker : `${marker}-EN`,
      );
      expect(await html.text()).toContain("info-test@example.com");
    }
    const count = execFileSync(
      "mysql",
      [
        "--host=127.0.0.1",
        `--user=${process.env.MYSQL_TEST_USER || "root"}`,
        "--batch",
        "--skip-column-names",
        process.env.MYSQL_TEST_DATABASE!,
        "--execute",
        `SELECT COUNT(*) FROM inquiry_record WHERE id IN ('${ids.join("','")}') AND country='VN';`,
      ],
      { encoding: "utf8" },
    );
    expect(count.trim()).toBe("2");
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto("/en/contact");
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBeTruthy();
  } finally {
    const args = [
      "--host=127.0.0.1",
      `--user=${process.env.MYSQL_TEST_USER || "root"}`,
      process.env.MYSQL_TEST_DATABASE!,
      "--execute",
    ];
    for (const id of ids) {
      expect(id).toMatch(/^[0-9a-f-]{36}$/);
      execFileSync("mysql", [
        ...args,
        `DELETE FROM inquiry_record WHERE id='${id}';`,
      ]);
    }
    if (changed) {
      if (old) {
        const csrf = (await (await page.request.get("/api/auth/csrf")).json())
          .data;
        await page.request.put("/api/admin/site-settings", {
          headers: { [csrf.headerName]: csrf.token },
          data: old,
        });
      } else {
        execFileSync("mysql", [
          ...args,
          "DELETE FROM content_record WHERE id='site-basic' AND kind='settings';",
        ]);
      }
    }
  }
});
