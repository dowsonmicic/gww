/** Read deployment settings at startup, without rebuilding the frontend. */
import process from "node:process";
export default defineNitroPlugin(() => {
  const config = useRuntimeConfig();
  const locale = process.env.SITE_DEFAULT_LOCALE || config.public.defaultLocale;
  if (!["zh", "en"].includes(String(locale))) {
    throw new Error("SITE_DEFAULT_LOCALE must be zh or en.");
  }
  const support = process.env.SITE_SUPPORT_URL || config.public.supportUrl;
  if (support) {
    const url = new URL(String(support));
    if (url.protocol !== "https:")
      throw new Error("SITE_SUPPORT_URL must use HTTPS.");
  }
});
