export default defineNuxtConfig({
  compatibilityDate: "2025-11-01",
  devtools: { enabled: false },
  css: ["~/assets/main.css"],
  runtimeConfig: {
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
