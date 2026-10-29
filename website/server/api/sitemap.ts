/**
 * sitemap.xml 动态数据源。
 * 从后端拉所有 PUBLISHED 产品和文章，返回每个的 zh/en 两条 URL + hreflang alternates。
 * @nuxtjs/sitemap 会自动扫描 server/api/sitemap* 端点加入 sitemap。
 */
export default defineEventHandler(async () => {
  const runtimeConfig = useRuntimeConfig();
  const base = runtimeConfig.public.siteUrl || "https://yourdomain.com";
  const apiBase = runtimeConfig.javaApiBase || "http://127.0.0.1:8083";
  const routes: Array<{
    loc: string;
    alternates?: Array<{ hreflang: string; href: string }>;
    changefreq?: string;
    priority?: number;
  }> = [];

  // 静态栏目页（双语）
  const staticPages = ["", "about", "products", "articles", "brands", "contact"];
  for (const slug of staticPages) {
    routes.push({
      loc: `${base}/en${slug ? "/" + slug : ""}`,
      alternates: [
        { hreflang: "zh", href: `${base}/zh${slug ? "/" + slug : ""}` },
        { hreflang: "en", href: `${base}/en${slug ? "/" + slug : ""}` },
        { hreflang: "x-default", href: `${base}/en${slug ? "/" + slug : ""}` },
      ],
      changefreq: "weekly",
      priority: slug === "" ? 1.0 : 0.8,
    });
  }

  try {
    // 拉产品
    const products: Array<{ id: string }> = await $fetch(
      `${apiBase}/api/public/content/products`,
    ).then((r: any) => r?.data || []).catch(() => []);

    for (const p of products) {
      routes.push({
        loc: `${base}/en/products/${p.id}`,
        alternates: [
          { hreflang: "zh", href: `${base}/zh/products/${p.id}` },
          { hreflang: "en", href: `${base}/en/products/${p.id}` },
          { hreflang: "x-default", href: `${base}/en/products/${p.id}` },
        ],
        changefreq: "monthly",
        priority: 0.7,
      });
    }

    // 拉文章
    const articles: Array<{ id: string }> = await $fetch(
      `${apiBase}/api/public/content/articles`,
    ).then((r: any) => r?.data || []).catch(() => []);

    for (const a of articles) {
      routes.push({
        loc: `${base}/en/articles/${a.id}`,
        alternates: [
          { hreflang: "zh", href: `${base}/zh/articles/${a.id}` },
          { hreflang: "en", href: `${base}/en/articles/${a.id}` },
          { hreflang: "x-default", href: `${base}/en/articles/${a.id}` },
        ],
        changefreq: "weekly",
        priority: 0.7,
      });
    }
  } catch (e) {
    // 后端没启动时仍然返回静态页面 sitemap，不中断构建
  }

  return routes;
});
