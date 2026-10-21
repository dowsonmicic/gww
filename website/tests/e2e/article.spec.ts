import { test, expect } from "@playwright/test";

test("article has one title and category filtering", async ({
  page,
  request,
}) => {
  test.skip(!process.env.ADMIN_TEST_PASSWORD, "需要本地开发管理员密码");
  test.setTimeout(60000);
  page.setDefaultTimeout(5000);
  const title = `ARTICLE-CHECK-${Date.now()}`;
  let id = "";
  await page.goto("/admin");
  await page.getByLabel("管理员账号").fill("admin");
  await page.getByLabel("管理员密码").fill(process.env.ADMIN_TEST_PASSWORD!);
  await page.getByRole("button", { name: "登录", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "工作台", exact: true }),
  ).toBeVisible();
  try {
    await page
      .locator(".admin-sidebar")
      .getByRole("button", { name: /文章管理/ })
      .click();
    await page.getByRole("button", { name: "新增", exact: true }).click();
    const dialog = page.locator(".el-dialog");
    await expect(dialog.getByLabel("英文名称", { exact: true })).toHaveCount(0);
    await dialog.getByLabel("标题", { exact: true }).fill(title);
    await dialog
      .locator(".el-form-item")
      .filter({ hasText: /^分类/ })
      .getByRole("textbox")
      .fill("测试分类");
    await dialog.getByText("草稿", { exact: true }).click();
    await page.getByRole("option", { name: "已发布", exact: true }).click();
    const saved = page.waitForResponse(
      (r) =>
        r.url().includes("/api/admin/content/articles") &&
        r.request().method() === "POST",
    );
    await page
      .getByRole("button", { name: "保存到数据库", exact: true })
      .click();
    const response = await saved;
    const body = await response.json();
    id = body.data.id;
    expect(response.request().postDataJSON()).toHaveProperty("title", title);
    expect(response.request().postDataJSON()).not.toHaveProperty("nameEn");
    expect(body.data).not.toHaveProperty("name");
    expect(body.data).not.toHaveProperty("nameEn");
    await expect(dialog).not.toBeVisible();
    await page.getByRole("combobox", { name: "分类筛选" }).focus();
    await page.keyboard.press("ArrowDown");
    await page.getByRole("option", { name: "测试分类", exact: true }).click();
    await expect(
      page.locator("td .cell").getByText(title, { exact: true }),
    ).toBeVisible();
    for (const locale of ["zh", "en"]) {
      const html = await request.get(`/${locale}/news/${id}`);
      expect(html.status()).toBe(200);
      expect(await html.text()).toContain(title);
      expect(await html.text()).not.toContain("展示样例");
    }
  } finally {
    if (id) {
      const csrf = await (await page.context().request.get("/api/auth/csrf")).json();
      const result = await page.context().request.delete(`/api/admin/content/articles/${id}`, {
        headers: { [csrf.data.headerName]: csrf.data.token },
      });
      expect(result.ok()).toBeTruthy();
    }
  }
});
