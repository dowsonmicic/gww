export type SectionItem = {
  title?: string;
  titleEn?: string;
  body?: string;
  bodyEn?: string;
};
export type ContentRow = {
  id: string;
  kind: string;
  title?: string;
  name?: string;
  nameEn?: string;
  category: string;
  brand: string;
  status: string;
  description: string;
  descriptionEn: string;
  imageId: string | null;
  imageUrl: string | null;
  sections?: SectionItem[];
};
type ApiResult<T> = { code: number; msg: string; data: T };

export function useJavaApi() {
  async function request<T>(
    path: string,
    method = "GET",
    body?: object | FormData,
  ): Promise<T> {
    const headers: Record<string, string> = {};
    if (method !== "GET") {
      const csrf =
        await $fetch<ApiResult<{ headerName: string; token: string }>>(
          "/api/auth/csrf",
        );
      headers[csrf.data.headerName] = csrf.data.token;
    }
    const result = await $fetch<ApiResult<T>>(path, {
      method: method as "GET" | "POST" | "PUT" | "DELETE",
      body,
      headers,
      credentials: "same-origin",
    });
    if (result.code !== 0) throw new Error(result.msg);
    return result.data;
  }
  return { request };
}
