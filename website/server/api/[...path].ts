// 固定上游同源转发，保留Cookie、CSRF和multipart请求，不接受客户端指定目标地址。
export default defineEventHandler((event) => {
  const base = useRuntimeConfig(event).javaApiBase.replace(/\/$/, "");
  const path = getRequestURL(event);
  return proxyRequest(event, `${base}${path.pathname}${path.search}`);
});
