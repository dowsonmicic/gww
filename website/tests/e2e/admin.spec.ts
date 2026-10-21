import { test, expect } from "@playwright/test";

test("admin demo edits stay isolated and settings expose no filesystem path", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/admin");
  await expect(
    page.getByRole("heading", { name: "工作台", exact: true }),
  ).toBeVisible();
  await expect(
    page.locator(".site-header,.site-footer,.floating-contact"),
  ).toHaveCount(0);
  await page
    .locator(".admin-sidebar")
    .getByRole("button", { name: /产品管理/ })
    .click();
  await page.getByRole("button", { name: "新增（演示）" }).click();
  await page
    .getByRole("textbox", { name: "中文名称", exact: true })
    .fill("测试产品");
  await page
    .getByRole("textbox", { name: "英文名称", exact: true })
    .fill("Demo product");
  await page.getByRole("button", { name: "保存演示", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: "测试产品", exact: true }),
  ).toBeVisible();
  await page.reload();
  await page
    .locator(".admin-sidebar")
    .getByRole("button", { name: /产品管理/ })
    .click();
  await expect(
    page.getByRole("cell", { name: "测试产品", exact: true }),
  ).toHaveCount(0);
  await page
    .locator(".admin-sidebar")
    .getByRole("button", { name: /网站设置/ })
    .click();
  await expect(
    page.getByText("MEDIA_PUBLIC_ROOT", { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "保存配置（演示）" }).click();
  await expect(page.getByRole("status")).toContainText("未写入服务器");
  expect(errors).toEqual([]);
});

test("fixed carousel and partner images can only be replaced locally", async ({
  page,
}) => {
  const writes: string[] = [];
  page.on("request", (request) => {
    if (["POST", "PUT", "DELETE"].includes(request.method()))
      writes.push(request.url());
  });
  await page.goto("/admin");
  await page
    .locator(".admin-sidebar")
    .getByRole("button", { name: /媒体资源/ })
    .click();
  await expect(page.locator(".admin-media-grid article")).toHaveCount(3);
  await expect(page.locator(".admin-media-grid img").first()).toHaveAttribute(
    "src",
    "/images/lb/block_1.webp",
  );
  await page
    .getByLabel("修改轮播图 1", { exact: true })
    .setInputFiles("public/images/show/8400.jpg");
  await expect(page.locator(".admin-media-grid img").first()).toHaveAttribute(
    "src",
    /^blob:/,
  );
  await expect(page.locator(".admin-media-grid article")).toHaveCount(3);
  await page.getByRole("button", { name: "合作商标", exact: true }).click();
  await expect(page.locator(".admin-media-grid article")).toHaveCount(1);
  await page
    .getByLabel("修改合作商标", { exact: true })
    .setInputFiles("public/images/brands/brands.webp");
  await expect(page.locator(".admin-media-grid img")).toHaveAttribute(
    "src",
    /^blob:/,
  );
  await expect(page.getByRole("button", { name: /新增|删除/ })).toHaveCount(0);
  expect(writes).toEqual([]);
});

test("product images belong to products and types are derived read-only", async ({
  page,
}) => {
  await page.goto("/admin");
  await page
    .locator(".admin-sidebar")
    .getByRole("button", { name: /产品管理/ })
    .click();
  await page.getByRole("button", { name: "编辑", exact: true }).first().click();
  await page
    .locator('input[type="file"][aria-label="产品图片"]')
    .setInputFiles("public/images/show/8440.jpg");
  await expect(page.getByAltText("产品图片预览")).toHaveAttribute(
    "src",
    /^blob:/,
  );
  await page.locator(".el-dialog").getByRole("textbox").nth(2).fill("测试分类");
  await page.locator(".el-dialog").getByRole("textbox").nth(3).fill("测试品牌");
  await page.getByRole("button", { name: "保存演示", exact: true }).click();
  await expect(page.locator(".admin-product-image").first()).toHaveAttribute(
    "src",
    /^blob:/,
  );
  await page
    .locator(".admin-sidebar")
    .getByRole("button", { name: /分类与品牌/ })
    .click();
  await expect(
    page.getByRole("cell", { name: "测试分类", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: /新增|编辑|删除/ }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "品牌目录", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: "测试品牌", exact: true }),
  ).toBeVisible();
});

test("admin mobile navigation has no page overflow", async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 900 });
  await page.goto("/admin");
  await expect(
    page.getByRole("heading", { name: "工作台", exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.getByRole("button", { name: "后台菜单" }).click();
  await page
    .locator(".admin-sidebar")
    .getByRole("button", { name: /客户询盘/ })
    .click();
  await expect(
    page.getByRole("heading", { name: "询盘服务尚未接入" }),
  ).toBeVisible();
});
