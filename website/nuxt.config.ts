export default defineNuxtConfig({
  compatibilityDate: "2025-11-01",
  devtools: { enabled: false },
  css: ["~/assets/main.css"],
  runtimeConfig: {
    javaApiBase: "http://127.0.0.1:8083",
    public: {
      defaultLocale: "zh",
      supportUrl: "",
      companyName: "",
      companyNameEn: "",
      contactEmail: "",
      contactPhone: "",
      siteUrl: "https://yourdomain.com", // TODO: 部署前替换为真实域名
    },
  },
  modules: ["@nuxtjs/robots", "@nuxtjs/sitemap"],
  // robots.txt：allow/disallow 用小写键名，模块自动加 User-agent: *
  robots: {
    allow: ["/"],
    disallow: ["/admin", "/api/"],
    sitemap: "/sitemap.xml",
  },
  // sitemap 基础配置；动态产品/文章路由在 server 侧生成
  site: {
    url: "https://yourdomain.com", // TODO: 部署前替换
  },
  sitemap: {
    changefreq: "weekly",
    priority: 0.8,
  } as any,
  app: {
    head: {
      meta: [
        { name: "viewport", content: "width=device-width, initial-scale=1" },
        ...(process.env.NODE_ENV !== "production"
          ? [{ name: "robots", content: "noindex, nofollow" }]
          : []),
      ],
    },
  },
});
