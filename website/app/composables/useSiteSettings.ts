export type SiteSettings = {
  companyName: string;
  companyNameEn: string;
  companyIntro: string;
  companyIntroEn: string;
  address: string;
  addressEn: string;
  contactEmail: string;
  contactPhone: string;
  defaultLocale: string;
  supportUrl: string;
};

// SSR与客户端共享公开设置；未保存配置时才使用启动环境默认值。
export async function useSiteSettings() {
  const config = useRuntimeConfig();
  const { data, error } = await useFetch<{ data: SiteSettings | null }>(
    "/api/public/site-settings",
    { key: "site-settings" },
  );
  if (error.value)
    throw createError({
      statusCode: 503,
      statusMessage: "Site settings unavailable",
    });
  return computed<SiteSettings>(
    () =>
      data.value?.data || {
        companyName: String(config.public.companyName || ""),
        companyNameEn: String(config.public.companyNameEn || ""),
        companyIntro: "",
        companyIntroEn: "",
        address: "",
        addressEn: "",
        contactEmail: String(config.public.contactEmail || ""),
        contactPhone: String(config.public.contactPhone || ""),
        defaultLocale: String(config.public.defaultLocale || "zh"),
        supportUrl: String(config.public.supportUrl || ""),
      },
  );
}
