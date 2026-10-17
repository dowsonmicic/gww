/** Request-scoped runtime config is mutable; the global config is intentionally frozen. */
import process from "node:process";
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const values = {
    defaultLocale: process.env.SITE_DEFAULT_LOCALE,
    supportUrl: process.env.SITE_SUPPORT_URL,
    companyName: process.env.SITE_COMPANY_NAME,
    companyNameEn: process.env.SITE_COMPANY_NAME_EN,
    contactEmail: process.env.SITE_CONTACT_EMAIL,
    contactPhone: process.env.SITE_CONTACT_PHONE,
  };
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined) config.public[key] = value;
  }
  if (event.path === "/")
    setHeader(event, "Cache-Control", "private, no-store");
});
