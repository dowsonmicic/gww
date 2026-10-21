import { test, expect } from "@playwright/test";
import { execFileSync } from "node:child_process";
import { unlinkSync } from "node:fs";
import { resolve } from "node:path";

test("MySQL product persists after reload and published content reaches SSR", async ({
  page,
  request,
}) => {
  test.skip(
    !process.env.ADMIN_TEST_PASSWORD ||
      !process.env.MYSQL_TEST_DATABASE ||
      !process.env.MEDIA_TEST_PUBLIC_ROOT ||
      !process.env.MEDIA_TEST_PRIVATE_ROOT,
    "需要本地管理员密码、独立MySQL测试库及可清理的测试媒体目录",
  );
  const name = `MYSQL-CHECK-${Date.now()}`;
  let id = "";
  let mediaId = "";
  await page.goto("/admin");
  await page.getByLabel("管理员账号").fill("admin");
  await page.getByLabel("管理员密码").fill(process.env.ADMIN_TEST_PASSWORD!);
  await page.getByRole("button", { name: "登录", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "工作台", exact: true }),
  ).toBeVisible();
  const menu = () =>
    page.locator(".admin-sidebar").getByRole("button", { name: /产品管理/ });
  try {
    await menu().click();
    await page.getByRole("button", { name: "新增", exact: true }).click();
    await page
      .getByRole("textbox", { name: "中文名称", exact: true })
      .fill(name);
    await page
      .getByRole("textbox", { name: "英文名称", exact: true })
      .fill(`${name}-EN`);
    const dialog = page.locator(".el-dialog");
    await dialog.getByRole("textbox").nth(2).fill("MYSQL分类");
    await dialog.getByRole("textbox").nth(3).fill("MYSQL品牌");
    await page
      .locator('input[type="file"][aria-label="产品图片"]')
      .setInputFiles(resolve("../.images/show/8400.jpg"));
    await dialog.getByText("草稿", { exact: true }).click();
    await page.getByRole("option", { name: "已发布", exact: true }).click();
    const saved = page.waitForResponse(
      (r) =>
        r.url().includes("/api/admin/content/products") &&
        r.request().method() === "POST",
    );
    const uploaded = page.waitForResponse(
      (r) =>
        r.url().endsWith("/api/admin/media") && r.request().method() === "POST",
    );
    await page
      .getByRole("button", { name: "保存到数据库", exact: true })
      .click();
    mediaId = (await (await uploaded).json()).data.id;
    const result = await (await saved).json();
    expect(result.code).toBe(0);
    id = result.data.id;
    // 本地开发库交叉验证，密码仅通过进程环境传递，不写入命令参数。
    if (process.env.MYSQL_TEST_DATABASE) {
      expect(id).toMatch(/^[0-9a-f-]{36}$/);
      const persisted = execFileSync(
        "mysql",
        [
          "--host=127.0.0.1",
          `--user=${process.env.MYSQL_TEST_USER || "root"}`,
          "--batch",
          "--skip-column-names",
          process.env.MYSQL_TEST_DATABASE,
          "--execute",
          `SELECT COUNT(*) FROM content_record WHERE id='${id}' AND kind='products';`,
        ],
        { encoding: "utf8" },
      );
      expect(persisted.trim()).toBe("1");
    }
    await expect(
      page.locator("td .cell").getByText(name, { exact: true }),
    ).toBeVisible();
    await page.reload();
    await expect(
      page.getByRole("heading", { name: "工作台", exact: true }),
    ).toBeVisible();
    await menu().click();
    await expect(
      page.locator("td .cell").getByText(name, { exact: true }),
    ).toBeVisible();
    const image = await request.get(`/api/public/media/${mediaId}/image`);
    expect(image.status()).toBe(200);
    expect(image.headers()["content-type"]).toContain("image/jpeg");
    expect((await image.body()).length).toBeGreaterThan(100);
    expect(
      (await request.get(`/api/admin/media/${mediaId}/file`)).status(),
    ).toBe(401);
    const publicResult = await request.get("/api/public/content/products");
    expect(
      (await publicResult.json()).data.some(
        (row: { id: string }) => row.id === id,
      ),
    ).toBeTruthy();
    const html = await request.get(`/zh/products/${id}`);
    expect(html.status()).toBe(200);
    expect(await html.text()).toContain(name);
    await page.goto(`/zh/products/${id}`);
    await expect(
      page.getByRole("heading", {
        name: `MYSQL分类 ${name}`,
        level: 1,
        exact: true,
      }),
    ).toBeVisible();
    await page.locator(".language").click();
    await expect(page).toHaveURL(`/en/products/${id}`);
    await expect(
      page.getByRole("heading", {
        name: `MYSQL分类 ${name}-EN`,
        level: 1,
        exact: true,
      }),
    ).toBeVisible();
  } finally {
    // 只清理由本用例创建的记录，通过同源认证及CSRF接口删除。
    if (id) {
      await page.evaluate(async (recordId) => {
        const csrf = await (await fetch("/api/auth/csrf")).json();
        const response = await fetch(
          `/api/admin/content/products/${recordId}`,
          {
            method: "DELETE",
            headers: { [csrf.data.headerName]: csrf.data.token },
          },
        );
        if (!response.ok) throw new Error("测试记录清理失败");
      }, id);
    }
    // 媒体暂未提供删除API，仅在显式配置的本地测试库清理本用例上传的文件。
    if (mediaId && process.env.MYSQL_TEST_DATABASE) {
      expect(mediaId).toMatch(/^[0-9a-f-]{36}$/);
      const args = [
        "--host=127.0.0.1",
        `--user=${process.env.MYSQL_TEST_USER || "root"}`,
        "--batch",
        "--skip-column-names",
        process.env.MYSQL_TEST_DATABASE,
        "--execute",
      ];
      const key = execFileSync(
        "mysql",
        [...args, `SELECT object_key FROM media_record WHERE id='${mediaId}';`],
        { encoding: "utf8" },
      ).trim();
      expect(key).toMatch(/^[0-9a-f-]{36}\.jpg$/);
      execFileSync("mysql", [
        ...args,
        `DELETE FROM media_record WHERE id='${mediaId}';`,
      ]);
      for (const root of [
        process.env.MEDIA_TEST_PUBLIC_ROOT,
        process.env.MEDIA_TEST_PRIVATE_ROOT,
      ]) {
        if (root) unlinkSync(resolve(root, key));
      }
    }
  }
});
