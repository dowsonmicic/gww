export default defineNuxtConfig({
  compatibilityDate: "2025-11-01",
  devtools: { enabled: false },
  css: ["~/assets/main.css"],
  runtimeConfig: {
    // 服务端专用，不向浏览器暴露内部Java地址。
    javaApiBase: "http://127.0.0.1:8083",
    public: {
      defaultLocale: "zh",
      supportUrl: "",
      companyName: "",
      companyNameEn: "",
      contactEmail: "",
      contactPhone: "",
    },
  },
  app: {
    head: {
      meta: [
        { name: "viewport", content: "width=device-width, initial-scale=1" },
        { name: "robots", content: "noindex, nofollow" },
      ],
    },
  },
});
