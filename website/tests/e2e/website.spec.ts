import { test, expect } from "@playwright/test";

test("search, pagination, language switching and detail navigation", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/zh/products");
  await expect(page.locator(".filters select")).toHaveCount(0);
  const categories = page.getByRole("group", { name: "材料类别", exact: true });
  const brands = page.getByRole("group", { name: "品牌", exact: true });
  await categories.getByRole("button", { name: "POE", exact: true }).click();
  await brands.getByRole("button", { name: "DOW", exact: true }).click();
  await expect(categories.getByRole("button", { name: "POE", exact: true })).toHaveAttribute("aria-pressed", "true");
  await expect(brands.getByRole("button", { name: "DOW", exact: true })).toHaveAttribute("aria-pressed", "true");
  await page.getByRole("button", { name: "清除筛选", exact: true }).click();
  await expect(categories.getByRole("button", { name: "全部", exact: true })).toHaveAttribute("aria-pressed", "true");
  await expect(brands.getByRole("button", { name: "全部", exact: true })).toHaveAttribute("aria-pressed", "true");
  await expect(page.locator(".product-card")).toHaveCount(4);
  await page.getByRole("button", { name: "下一页", exact: true }).click();
  await expect(page.locator(".product-card")).toHaveCount(2);
  await page.getByRole("searchbox").fill("8400");
  await expect(page.locator(".product-card")).toHaveCount(1);
  await page.getByRole("searchbox").fill("unknown");
  await expect(
    page.getByText("没有匹配的产品，请调整筛选条件。"),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "清除筛选", exact: true })
    .first()
    .click();
  await page.locator(".product-card").first().click();
  await expect(page).toHaveURL(/\/zh\/products\/8400$/);
  await page.locator(".language").click();
  await expect(page).toHaveURL(/\/en\/products\/8400$/);
  await expect(page.locator("html")).toHaveAttribute("lang", "en");
  await page.goto("/");
  await expect(page).toHaveURL(/\/en$/);
  expect(errors).toEqual([]);
});

for (const width of [375, 768, 1440]) {
  test(`layout and images at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    for (const path of [
      "/zh",
      "/en",
      "/zh/products",
      "/en/products/8400",
      "/zh/contact",
    ]) {
      await page.goto(path);
      await expect(page.locator("h1")).toBeVisible();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      ).toBeTruthy();
      const broken = await page
        .locator("img")
        .evaluateAll(
          (images) =>
            images.filter((image) => image.complete && image.naturalWidth === 0)
              .length,
        );
      expect(broken).toBe(0);
    }
    if (width < 900) {
      await page.goto("/zh");
      await page.getByRole("button", { name: "切换导航" }).click();
      await expect(page.locator("#main-nav")).toBeVisible();
      await page
        .locator("#main-nav")
        .getByRole("link", { name: "产品中心" })
        .click();
      await expect(page).toHaveURL(/\/zh\/products$/);
    }
  });
}

test("carousel controls and new contact form", async ({ page }) => {
  await page.goto("/zh");
  await page.getByRole("button", { name: "暂停轮播" }).click();
  await page.getByRole("button", { name: "轮播图 2", exact: true }).click();
  await expect(page.locator(".hero-image").nth(1)).toBeVisible();
  await page.locator(".floating-contact").click();
  await expect(page).toHaveURL(/\/zh\/contact$/);
  await expect(page.locator(".inquiry-form")).toBeVisible();
  await expect(page.locator(".page-banner")).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "联系我们", exact: true })).toHaveCount(1);
  await expect(page.locator(".contact-panel")).toHaveCount(0);
  await expect(page.locator(".contact-details")).toHaveCount(0);
  await expect(page.getByLabel("网站地址", { exact: true })).toHaveCount(0);
  await expect(page.getByLabel("选择产品", { exact: true })).toHaveCount(0);
});
