export type SiteMediaItem = {
  id: string;       // carousel-1 / carousel-2 / carousel-3 / partners
  name: string;
  group: string;     // carousel | partners
  imageId: string | null;
  url: string;       // 最终可用的图片路径（API 或兜底静态）
};

// 从 /api/public/site-media 读取数据库中的轮播图+合作商标。
// 服务端未保存时 url 已是兜底静态路径，组件直接用即可。
export async function useSiteMedia() {
  const { data, error } = await useFetch<{ data: SiteMediaItem[] }>(
    "/api/public/site-media",
    { key: "site-media" },
  );
  if (error.value)
    throw createError({
      statusCode: 503,
      statusMessage: "Site media unavailable",
    });
  return computed<SiteMediaItem[]>(
    () =>
      data.value?.data ?? [
        { id: "carousel-1", name: "轮播图 1", group: "carousel", imageId: null, url: "/images/lb/block_1.webp" },
        { id: "carousel-2", name: "轮播图 2", group: "carousel", imageId: null, url: "/images/lb/block_2.webp" },
        { id: "carousel-3", name: "轮播图 3", group: "carousel", imageId: null, url: "/images/lb/block_3.webp" },
        { id: "partners",   name: "合作商标",  group: "partners",  imageId: null, url: "/images/brands/brands.webp" },
      ],
  );
}
