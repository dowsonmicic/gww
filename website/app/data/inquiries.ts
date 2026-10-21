export type InquiryRow = {
  id: string;
  name: string;
  email: string;
  phone: string;
  message: string;
  status: string;
  locale: string;
  country: string;
  industry: string;
  product: string;
  website: string;
  company: string;
  createdAt?: string | null;
};

export function filterInquiries(
  rows: InquiryRow[],
  locale: string,
  startDate: string,
  endDate: string,
) {
  // 使用数据库时间的日期部分，不通过浏览器时区转换日期边界。
  return rows.filter((row) => {
    const date = (row.createdAt || "").slice(0, 10);
    return (!locale || row.locale === locale)
      && (!(startDate || endDate) || Boolean(date))
      && (!startDate || date >= startDate)
      && (!endDate || date <= endDate);
  }).sort((a, b) =>
    (b.createdAt || "").localeCompare(a.createdAt || "") || b.id.localeCompare(a.id));
}

export function inquiryExportUrl(startDate: string, endDate: string, locale: string) {
  return inquiryListUrl(startDate, endDate, locale).replace("/inquiries", "/inquiries/export");
}

export function inquiryListUrl(startDate: string, endDate: string, locale: string) {
  const params = new URLSearchParams();
  if (startDate) params.set("startDate", startDate);
  if (endDate) params.set("endDate", endDate);
  if (locale) params.set("locale", locale);
  return `/api/admin/inquiries${params.size ? `?${params}` : ""}`;
}

/** 使用本地日历日期，避免UTC转换导致默认日期偏移一天。 */
export function todayInquiryDate(now = new Date()) {
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

export function formatInquiryTime(value?: string | null) {
  if (!value) return "时间未提供";
  return value.replace("T", " ").slice(0, 19);
}
