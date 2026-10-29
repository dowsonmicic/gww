export default defineNuxtConfig({
  compatibilityDate: "2025-11-01",
  devtools: { enabled: false },
  css: ["~/assets/main.css"],

  // ========== 环境变量（从 .env 或系统环境读，key 必须带 NUXT_ 前缀）==========
  runtimeConfig: {
    // 服务端专属：后端 API 基址（客户端看不到，安全）
    javaApiBase: process.env.NUXT_JAVA_API_BASE || "http://127.0.0.1:8083",
    // 公开变量（客户端/服务端都能读）
    public: {
      defaultLocale: "zh",
      supportUrl: "",
      companyName: process.env.NUXT_PUBLIC_COMPANY_NAME || "GWW",
      companyNameEn: process.env.NUXT_PUBLIC_COMPANY_NAME_EN || "GWW Specialty Materials",
      contactEmail: process.env.NUXT_PUBLIC_CONTACT_EMAIL || "",
      contactPhone: process.env.NUXT_PUBLIC_CONTACT_PHONE || "",
      // 正式域名（生产必改）
      siteUrl: process.env.NUXT_PUBLIC_SITE_URL || "https://yourdomain.com",
    },
  },

  modules: ["@nuxtjs/robots", "@nuxtjs/sitemap"],

  robots: {
    allow: ["/"],
    disallow: ["/admin", "/api/"],
    sitemap: "/sitemap.xml",
  },

  // sitemap 模块的主域名——和 runtimeConfig.public.siteUrl 同源
  site: {
    url: process.env.NUXT_PUBLIC_SITE_URL || "https://yourdomain.com",
  },

  sitemap: {
    changefreq: "weekly",
    priority: 0.8,
  } as any,

  app: {
    head: {
      meta: [
        { name: "viewport", content: "width=device-width, initial-scale=1" },
        // 开发环境 noindex 防收录，生产环境自动移除
        ...(process.env.NODE_ENV !== "production"
          ? [{ name: "robots", content: "noindex, nofollow" }]
          : []),
      ],
    },
  },
});
