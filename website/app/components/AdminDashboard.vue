<script setup lang="ts">
import {
  ElButton,
  ElInput,
  ElSelect,
  ElOption,
  ElTable,
  ElTableColumn,
  ElTag,
  ElDialog,
  ElForm,
  ElFormItem,
  ElPagination,
  ElMessage,
  ElMessageBox,
} from "element-plus";
import "element-plus/dist/index.css";
import type { ContentRow } from "~/composables/useJavaApi";
import type { SiteSettings } from "~/composables/useSiteSettings";
import { inquiryListUrl, todayInquiryDate, inquiryExportUrl, formatInquiryTime, type InquiryRow } from "~/data/inquiries";
const api = useJavaApi();
const user = ref("");
const ready = ref(false);
const busy = ref(false);
const feedback = ref("");
const loginForm = reactive({ username: "admin", password: "" });
const inquiries = ref<InquiryRow[]>([]);
const inquiryLocale = ref("zh");
const inquiryStartDate = ref("");
const inquiryEndDate = ref(todayInquiryDate());
const queriedInquiries = ref<InquiryRow[]>([]);
const loadingInquiries = ref(false);
let inquiryQueryVersion = 0;
const inquiryDetail = ref<InquiryRow | null>(null);
const inquiryDialog = ref(false);
const exportingInquiries = ref(false);
const inquiryStatusLabels: Record<string, string> = {
  NEW: "待处理", PROCESSING: "跟进中", CLOSED: "已完成",
};
const visibleInquiries = computed(() => queriedInquiries.value);
async function queryInquiries() {
  if (!user.value) return;
  const version = ++inquiryQueryVersion;
  if (inquiryStartDate.value && inquiryEndDate.value && inquiryStartDate.value > inquiryEndDate.value) {
    queriedInquiries.value = [];
    loadingInquiries.value = false;
    ElMessage.error("开始日期不能晚于结束日期");
    return;
  }
  loadingInquiries.value = true;
  try {
    const rows = await api.request<InquiryRow[]>(
      inquiryListUrl(inquiryStartDate.value, inquiryEndDate.value, inquiryLocale.value),
    );
    // 快速切换条件时，忽略先前请求的响应。
    if (version === inquiryQueryVersion) queriedInquiries.value = rows;
  } catch (error) {
    if (version === inquiryQueryVersion) {
      queriedInquiries.value = [];
      ElMessage.error(errorMessage(error));
    }
  } finally {
    if (version === inquiryQueryVersion) loadingInquiries.value = false;
  }
}
watch([inquiryStartDate, inquiryEndDate, inquiryLocale], () => queryInquiries());
async function viewInquiry(row: InquiryRow) {
  try {
    inquiryDetail.value = await api.request<InquiryRow>(`/api/admin/inquiries/${row.id}`);
    inquiryDialog.value = true;
  } catch (error) {
    ElMessage.error(errorMessage(error));
  }
}
async function exportInquiries() {
  if (inquiryStartDate.value && inquiryEndDate.value && inquiryStartDate.value > inquiryEndDate.value) {
    ElMessage.error("开始日期不能晚于结束日期");
    return;
  }
  exportingInquiries.value = true;
  try {
    const response = await fetch(inquiryExportUrl(inquiryStartDate.value, inquiryEndDate.value, inquiryLocale.value), {
      credentials: "same-origin",
    });
    if (!response.ok) {
      const result = await response.json();
      throw new Error(result.msg || "导出失败");
    }
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement("a");
    link.href = url;
    link.download = `客户询盘_${inquiryStartDate.value || "全部"}_${inquiryEndDate.value || "全部"}.xlsx`;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    ElMessage.success("Excel已导出");
  } catch (error) {
    ElMessage.error(errorMessage(error));
  } finally {
    exportingInquiries.value = false;
  }
}
async function updateInquiry(row: InquiryRow, value: string) {
  try {
    await api.request(`/api/admin/inquiries/${row.id}/status`, "PUT", {
      status: value,
    });
    inquiries.value = await api.request<InquiryRow[]>("/api/admin/inquiries");
    await queryInquiries();
  } catch (error) {
    ElMessage.error(errorMessage(error));
  }
}
const statusLabels: Record<string, string> = {
  DRAFT: "草稿",
  PUBLISHED: "已发布",
  ARCHIVED: "已归档",
};
function errorMessage(error: unknown) {
  const e = error as { data?: { msg?: string }; message?: string };
  return e.data?.msg || e.message || "接口请求失败";
}
async function loadRecords() {
  const savedSettings = await api.request<SiteSettings | null>(
    "/api/admin/site-settings",
  );
  if (savedSettings) Object.assign(settings, savedSettings);
  inquiries.value = await api.request<InquiryRow[]>("/api/admin/inquiries");
  await queryInquiries();
  for (const kind of ["products", "articles"]) {
    const rows: ContentRow[] = [];
    // 当前后端无total，逐页读取直到不足100条，不用静态数据补齐。
    for (let page = 1; page <= 10000; page++) {
      const batch = await api.request<ContentRow[]>(
        `/api/admin/content/${kind}?page=${page}&size=100`,
      );
      rows.push(...batch);
      if (batch.length < 100) break;
    }
    records[kind] = rows.map((row) => ({
      ...row,
      name: kind === "articles" ? row.title || "" : row.name || "",
      nameEn: row.nameEn || "",
      image: row.imageId ? `/api/admin/media/${row.imageId}/file` : "",
    }));
  }
}
async function login() {
  busy.value = true;
  feedback.value = "";
  try {
    const result = await api.request<{ username: string }>(
      "/api/auth/login",
      "POST",
      loginForm,
    );
    user.value = result.username;
    loginForm.password = "";
    await loadRecords();
  } catch (error) {
    feedback.value = errorMessage(error);
  } finally {
    busy.value = false;
  }
}
async function logout() {
  try {
    await api.request("/api/auth/logout", "POST");
    user.value = "";
    records.products = [];
    records.articles = [];
    inquiries.value = [];
    ++inquiryQueryVersion;
    queriedInquiries.value = [];
    loadingInquiries.value = false;
    inquiryDetail.value = null;
    inquiryDialog.value = false;
    selected.value = "overview";
    feedback.value = "";
  } catch (error) {
    ElMessage.error(errorMessage(error));
  }
}
onMounted(async () => {
  try {
    const result = await api.request<{ username: string }>("/api/auth/me");
    user.value = result.username;
    await loadRecords();
  } catch (error) {
    const e = error as { statusCode?: number; status?: number };
    if (e.statusCode !== 401 && e.status !== 401)
      feedback.value = errorMessage(error);
  } finally {
    ready.value = true;
  }
});

type Row = {
  id: string;
  name: string;
  nameEn: string;
  category: string;
  brand: string;
  status: string;
  description: string;
  image?: string;
  imageId?: string | null;
  descriptionEn?: string;
};
const menus = [
  { id: "overview", name: "工作台", icon: "▦" },
  { id: "products", name: "产品管理", icon: "▤" },
  { id: "categories", name: "分类与品牌", icon: "◇" },
  { id: "media", name: "媒体资源", icon: "▧" },
  { id: "articles", name: "文章管理", icon: "≡" },
  { id: "inquiries", name: "客户询盘", icon: "○" },
  { id: "settings", name: "网站设置", icon: "⚙" },
];
const selected = ref("overview");
const mobileOpen = ref(false);
const heading = computed(
  () => menus.find((m) => m.id === selected.value)?.name || "",
);
const categoryTab = ref("categories");
const search = ref("");
const status = ref("");
const categoryFilter = ref("");
const filterCategories = computed(() => [
  ...new Set(
    (records[recordKey.value] || []).map((row) => row.category).filter(Boolean),
  ),
]);
const page = ref(1);
// 管理列表仅来自Java/MySQL，空数据库显示空列表。
const records = reactive<Record<string, Row[]>>({
  products: [],
  articles: [],
});
const recordKey = computed(() =>
  selected.value === "categories" ? categoryTab.value : selected.value,
);
// 分类与品牌只汇总产品中的现有值，不维护独立可编辑目录。
const productTypes = computed(() => {
  const key = categoryTab.value === "categories" ? "category" : "brand";
  const counts = new Map<string, number>();
  for (const product of records.products || []) {
    const value = product[key].trim();
    if (value) counts.set(value, (counts.get(value) || 0) + 1);
  }
  return Array.from(counts, ([name, count]) => ({ name, count })).filter(
    (item) =>
      item.name.toLowerCase().includes(search.value.trim().toLowerCase()),
  );
});
const filtered = computed(() =>
  (records[recordKey.value] || []).filter(
    (row) =>
      `${row.name} ${row.nameEn} ${row.id} ${row.brand} ${row.category}`
        .toLowerCase()
        .includes(search.value.trim().toLowerCase()) &&
      (!status.value || row.status === status.value) &&
      (!categoryFilter.value || row.category === categoryFilter.value),
  ),
);
const paged = computed(() =>
  filtered.value.slice((page.value - 1) * 5, page.value * 5),
);
watch(selected, async () => {
  categoryFilter.value = "";
  if (selected.value === "media") {
    await loadMedia();
  }
});
watch([selected, categoryTab, search, status, categoryFilter], () => {
  page.value = 1;
  mobileOpen.value = false;
});
const dialog = ref(false);
const editId = ref("");
const form = reactive<Row>({
  id: "",
  name: "",
  nameEn: "",
  category: "",
  brand: "",
  status: "DRAFT",
  description: "",
});
function openEditor(row?: Row) {
  productFile.value = null;
  editId.value = row?.id || "";
  Object.assign(
    form,
    row || {
      id: "",
      name: "",
      nameEn: "",
      category: "",
      brand: "",
      status: "DRAFT",
      description: "",
      descriptionEn: "",
      imageId: null,
      image: "",
    },
  );
  dialog.value = true;
}
async function save() {
  if (
    !form.name.trim() ||
    (selected.value !== "articles" && !form.nameEn.trim())
  ) {
    ElMessage.warning(
      selected.value === "articles" ? "请填写标题" : "请填写中文和英文名称",
    );
    return;
  }
  busy.value = true;
  try {
    let imageId = form.imageId || null;
    if (productFile.value) {
      const body = new FormData();
      body.append("file", productFile.value);
      const uploaded = await api.request<{ id: string }>(
        "/api/admin/media",
        "POST",
        body,
      );
      await api.request(`/api/admin/media/${uploaded.id}/publish`, "POST");
      imageId = uploaded.id;
      form.imageId = imageId;
      productFile.value = null;
    }
    const path = `/api/admin/content/${recordKey.value}${editId.value ? `/${editId.value}` : ""}`;
    await api.request(path, editId.value ? "PUT" : "POST", {
      ...(recordKey.value === "articles"
        ? { title: form.name.trim() }
        : { name: form.name.trim(), nameEn: form.nameEn.trim() }),
      category: form.category.trim(),
      brand: form.brand.trim(),
      status: form.status,
      description: form.description,
      descriptionEn: form.descriptionEn || "",
      imageId,
    });
    dialog.value = false;
    page.value = 1;
    await loadRecords();
    ElMessage.success("已保存到数据库");
  } catch (error) {
    ElMessage.error(errorMessage(error));
  } finally {
    busy.value = false;
  }
}
async function remove(row: Row) {
  try {
    await ElMessageBox.confirm(`确认从数据库删除“${row.name}”？`, "删除内容", {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  try {
    await api.request(
      `/api/admin/content/${recordKey.value}/${row.id}`,
      "DELETE",
    );
    await loadRecords();
    ElMessage.success("已删除数据库记录");
  } catch (error) {
    ElMessage.error(errorMessage(error));
  }
  page.value = Math.min(
    page.value,
    Math.max(1, Math.ceil(filtered.value.length / 5)),
  );
}
type SiteMedia = {
  id: string;
  name: string;
  url: string;
  imageId: string | null;
  group: "carousel" | "partners";
  local: boolean;
};
// 固定官网媒体位置只允许替换，不增加或删除；产品图片由产品表单管理。
// 初始值只是占位，进入"媒体资源"页面时立即从 Java 后端拉取真实数据。
const media = ref<SiteMedia[]>([
  ...[1, 2, 3].map((number): SiteMedia => ({
    id: `carousel-${number}`,
    name: `轮播图 ${number}`,
    url: `/images/lb/block_${number}.webp`,
    imageId: null,
    group: "carousel",
    local: false,
  })),
  {
    id: "partners",
    name: "合作商标",
    url: "/images/brands/brands.webp",
    imageId: null,
    group: "partners",
    local: false,
  },
]);
async function loadMedia() {
  try {
    const list = await api.request<SiteMedia[]>(
      "/api/public/site-media",
    );
    // 保持 media.id/name/group 固定顺序，用后端返回覆盖 url/imageId
    media.value = media.value.map((slot) => {
      const server = list.find((s) => s.id === slot.id);
      return server
        ? {
            ...slot,
            url: server.url,
            imageId: server.imageId,
            local: false,
          }
        : slot;
    });
  } catch (error) {
    console.warn("媒体资源加载失败，使用本地占位", error);
  }
}
const mediaTab = ref<SiteMedia["group"]>("carousel");
const objectUrls = new Set<string>();
const mediaSearch = ref("");
const mediaList = computed(() =>
  media.value.filter(
    (m) =>
      m.group === mediaTab.value &&
      m.name.toLowerCase().includes(mediaSearch.value.trim().toLowerCase()),
  ),
);
function selectImage(event: Event): string | undefined {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) return;
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning("图片不能超过20MB");
    return;
  }
  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
    ElMessage.warning("请选择 JPG/PNG/WebP 图片");
    return;
  }
  const url = URL.createObjectURL(file);
  objectUrls.add(url);
  return url;
}
const productFile = shallowRef<File | null>(null);
function selectProductImage(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  const url = selectImage(event);
  if (url && file) {
    form.image = url;
    productFile.value = file;
  }
}
async function replaceMedia(item: SiteMedia, event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) return;
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning("图片不能超过20MB");
    return;
  }
  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
    ElMessage.warning("请选择 JPG/PNG/WebP 图片");
    return;
  }
  busy.value = true;
  try {
    // 1. 上传图片到 Java 后端（私有区，未发布）
    const body = new FormData();
    body.append("file", file);
    const uploaded = await api.request<{ id: string }>(
      "/api/admin/media",
      "POST",
      body,
    );
    // 2. 发布图片（从 private → public 目录）
    await api.request(`/api/admin/media/${uploaded.id}/publish`, "POST");
    // 3. 把 slot 绑定到这张新图（写 MySQL content_record.image_id）
    const saved = await api.request<SiteMedia>(
      `/api/admin/site-media/${item.id}`,
      "PUT",
      { imageId: uploaded.id },
    );
    // 4. 本地更新
    item.url = saved.url;
    item.imageId = saved.imageId;
    item.local = false;
    ElMessage.success(`已保存 ${item.name} 到数据库`);
  } catch (error) {
    ElMessage.error(errorMessage(error));
  } finally {
    busy.value = false;
  }
}
// 产品编辑取消时仍可能需要旧预览，所有产品预览统一在组件销毁时释放。
onBeforeUnmount(() => objectUrls.forEach((url) => URL.revokeObjectURL(url)));
const settings = reactive<SiteSettings>({
  companyName: "",
  companyNameEn: "",
  companyIntro: "",
  companyIntroEn: "",
  address: "",
  addressEn: "",
  contactEmail: "",
  contactPhone: "",
  defaultLocale: "zh",
  supportUrl: "",
});
async function saveSettings() {
  busy.value = true;
  try {
    Object.assign(
      settings,
      await api.request<SiteSettings>(
        "/api/admin/site-settings",
        "PUT",
        settings,
      ),
    );
    await refreshNuxtData("site-settings");
    ElMessage.success("网站设置已保存，官网刷新后生效");
  } catch (error) {
    ElMessage.error(errorMessage(error));
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <section v-if="!ready || !user" class="admin-login">
    <h1>官网管理后台登录</h1>
    <p>产品和文章读取Java/MySQL，不使用静态演示记录。</p>
    <p v-if="!ready">正在检查登录状态…</p>
    <ElForm v-else label-position="top" @submit.prevent="login">
      <ElFormItem label="账号"
        ><ElInput
          v-model="loginForm.username"
          aria-label="管理员账号"
          autocomplete="username"
      /></ElFormItem>
      <ElFormItem label="密码"
        ><ElInput
          v-model="loginForm.password"
          aria-label="管理员密码"
          type="password"
          show-password
          autocomplete="current-password"
      /></ElFormItem>
      <ElButton native-type="submit" type="primary" :loading="busy"
        >登录</ElButton
      >
    </ElForm>
    <p v-if="feedback" role="alert">{{ feedback }}</p>
  </section>
  <div v-else class="admin-shell">
    <aside class="admin-sidebar" :class="{ 'is-open': mobileOpen }">
      <div class="admin-logo">
        <b>M<span>·</span></b>
        <div>官网管理<small>CONTENT WORKSPACE</small></div>
      </div>
      <div class="admin-menu-label">内容与业务</div>
      <button
        v-for="menu in menus"
        :key="menu.id"
        class="admin-nav-item"
        :class="{ active: selected === menu.id }"
        :aria-current="selected === menu.id ? 'page' : undefined"
        @click="
          selected = menu.id;
          mobileOpen = false;
        "
      >
        <span>{{ menu.icon }}</span
        >{{ menu.name }}
      </button>
      <div class="admin-sidebar-bottom">
        <span class="admin-dot" /> Java 数据接口<small
          >产品/文章来自数据库</small
        >
      </div>
    </aside>
    <div class="admin-workspace">
      <header class="admin-header">
        <div>
          <button
            class="admin-mobile-toggle"
            :aria-expanded="mobileOpen"
            aria-label="后台菜单"
            @click="mobileOpen = !mobileOpen"
          >
            ☰</button
          ><span>管理控制台</span><span class="admin-divider">/</span
          ><strong>{{ heading }}</strong>
        </div>
        <div>
          <NuxtLink to="/zh" class="admin-site-link">查看官网 ↗</NuxtLink
          ><span>{{ user }}</span
          ><ElButton @click="logout">退出登录</ElButton>
        </div>
      </header>
      <main class="admin-main">
        <div class="admin-title">
          <div>
            <p>WEBSITE MANAGEMENT</p>
            <h1>{{ heading }}</h1>
          </div>
          <ElTag type="success" effect="plain">已认证 · 数据库内容</ElTag>
        </div>
        <div class="admin-notice">
          产品、文章、询盘、网站设置、轮播图和合作商标均已接入Java/MySQL，图片在替换时直接上传并写入数据库。
          <span v-if="feedback" role="alert">{{ feedback }}</span>
        </div>

        <template v-if="selected === 'overview'">
          <div class="admin-stats">
            <div
              v-for="item in [
                {
                  label: '数据库产品',
                  count: records.products?.length || 0,
                  note: 'Java API实时读取',
                },
                {
                  label: '文章内容',
                  count: records.articles?.length || 0,
                  note: '新闻与材料知识',
                },
                {
                  label: '媒体资源',
                  count: media.length,
                  note: '含本次会话预览',
                },
                {
                  label: '真实询盘',
                  count: inquiries.length,
                  note: 'Java API读取',
                },
              ]"
              :key="item.label"
            >
              <span>{{ item.label }}</span
              ><strong>{{ item.count }}</strong
              ><small>{{ item.note }}</small>
            </div>
          </div>
          <div class="admin-overview-grid">
            <section class="admin-panel">
              <h2>常用操作</h2>
              <div class="admin-quick-grid">
                <button
                  v-for="menu in menus.slice(1, 5)"
                  :key="menu.id"
                  @click="selected = menu.id"
                >
                  <span>{{ menu.icon }}</span
                  ><strong>{{ menu.name }}</strong
                  ><small>进入管理 →</small>
                </button>
              </div>
            </section>
            <section class="admin-panel">
              <h2>接入进度</h2>
              <ul class="admin-progress">
                <li>官网与后台展示界面 <ElTag type="success">已实现</ElTag></li>
                <li>Java / MySQL <ElTag type="success">已接入</ElTag></li>
                <li>登录与服务端权限 <ElTag type="success">已接入</ElTag></li>
                <li>服务器媒体目录 <ElTag type="warning">待核实</ElTag></li>
              </ul>
              <p class="admin-muted">
                保留已有宝塔站点配置，不默认新建目录或重装 Nginx。
              </p>
            </section>
          </div>
          <section class="admin-panel">
            <h2>数据库产品</h2>
            <ElTable :data="records.products?.slice(0, 4)"
              ><ElTableColumn prop="name" label="产品名称" /><ElTableColumn
                prop="category"
                label="类别"
              /><ElTableColumn prop="brand" label="品牌" /><ElTableColumn
                label="状态"
                ><template #default
                  ><ElTag type="info">数据库记录</ElTag></template
                ></ElTableColumn
              ></ElTable
            >
          </section>
        </template>

        <section v-else-if="selected === 'categories'" class="admin-panel">
          <div class="admin-tabs">
            <button
              :class="{ active: categoryTab === 'categories' }"
              @click="categoryTab = 'categories'"
            >
              材料分类
            </button>
            <button
              :class="{ active: categoryTab === 'brands' }"
              @click="categoryTab = 'brands'"
            >
              品牌目录
            </button>
          </div>
          <p class="admin-muted">
            仅展示产品管理中已有的分类与品牌及产品数量，不提供新增、编辑或删除。请在产品编辑中填写分类和品牌。
          </p>
          <ElInput
            v-model="search"
            placeholder="搜索分类或品牌"
            aria-label="类型搜索"
            clearable
          />
          <ElTable :data="productTypes" empty-text="产品中暂无对应类型">
            <ElTableColumn
              prop="name"
              :label="categoryTab === 'categories' ? '材料分类' : '品牌'"
            />
            <ElTableColumn prop="count" label="产品数量" />
          </ElTable>
        </section>
        <section
          v-else-if="['products', 'articles'].includes(selected)"
          class="admin-panel"
        >
          <div class="admin-toolbar">
            <ElInput
              v-model="search"
              placeholder="搜索名称、分类、品牌或编号"
              clearable
              aria-label="后台搜索"
            /><ElSelect
              v-model="status"
              placeholder="全部状态"
              clearable
              aria-label="内容状态"
              ><ElOption
                v-for="(label, value) in statusLabels"
                :key="value"
                :label="label"
                :value="value" /></ElSelect
            ><ElSelect
              v-model="categoryFilter"
              placeholder="全部分类"
              clearable
              aria-label="分类筛选"
            >
              <ElOption
                v-for="item in filterCategories"
                :key="item"
                :label="item"
                :value="item"
              /> </ElSelect
            ><ElButton
              @click="
                search = '';
                status = '';
                categoryFilter = '';
              "
              >重置</ElButton
            ><ElButton type="primary" @click="openEditor()">新增</ElButton>
          </div>
          <ElTable :data="paged" empty-text="暂无匹配内容"
            ><ElTableColumn
              v-if="selected === 'products'"
              label="产品图片"
              width="100"
            >
              <template #default="{ row }">
                <img
                  v-if="row.image"
                  :src="row.image"
                  :alt="row.name"
                  class="admin-product-image"
                />
                <span v-else>未设置</span>
              </template> </ElTableColumn
            ><ElTableColumn
              prop="name"
              :label="selected === 'articles' ? '标题' : '中文名称'"
              min-width="170"
            /><ElTableColumn
              v-if="selected !== 'articles'"
              prop="nameEn"
              label="英文名称"
              min-width="170"
            /><ElTableColumn
              v-if="selected === 'products'"
              prop="brand"
              label="品牌"
              width="100"
            /><ElTableColumn
              v-if="selected !== 'categories'"
              prop="category"
              label="分类"
              width="110"
            /><ElTableColumn label="状态" width="110"
              ><template #default="{ row }"
                ><ElTag :type="row.status === 'DRAFT' ? 'warning' : 'info'">{{
                  statusLabels[row.status] || row.status
                }}</ElTag></template
              ></ElTableColumn
            ><ElTableColumn label="操作" width="140"
              ><template #default="{ row }"
                ><ElButton link type="primary" @click="openEditor(row as Row)"
                  >编辑</ElButton
                ><ElButton link type="danger" @click="remove(row as Row)"
                  >删除</ElButton
                ></template
              ></ElTableColumn
            ></ElTable
          >
          <div class="admin-pagination">
            <span>共 {{ filtered.length }} 条数据库记录</span
            ><ElPagination
              v-model:current-page="page"
              :page-size="5"
              :total="filtered.length"
              layout="prev, pager, next"
            />
          </div>
        </section>

        <template v-else-if="selected === 'media'">
          <section class="admin-panel">
            <div class="admin-tabs">
              <button
                :class="{ active: mediaTab === 'carousel' }"
                @click="mediaTab = 'carousel'"
              >
                轮播图
              </button>
              <button
                :class="{ active: mediaTab === 'partners' }"
                @click="mediaTab = 'partners'"
              >
                合作商标
              </button>
            </div>
            <div class="admin-toolbar">
              <ElInput
                v-model="mediaSearch"
                placeholder="搜索文件名"
                aria-label="搜索媒体"
              />
            </div>
            <p class="admin-muted">
              固定位置仅提供替换，不新增或删除。产品图片请到产品管理维护。替换时直接上传并写入数据库，最大20MB。
            </p>
            <div class="admin-media-grid">
              <article v-for="item in mediaList" :key="item.id">
                <img :src="item.url" :alt="item.name" />
                <div>
                  <strong>{{ item.name }}</strong
                  ><small
                    >{{ item.local ? "本机临时预览" : item.imageId ? "已上传数据库" : "项目静态素材" }} ·
                    {{
                      item.group === "carousel" ? "轮播图" : "合作商标"
                    }}</small
                  >
                  <label class="admin-file-button"
                    >修改图片
                    <input
                      type="file"
                      :aria-label="`修改${item.name}`"
                      accept=".jpg,.jpeg,.png,.webp"
                      @change="replaceMedia(item, $event)"
                    />
                  </label>
                </div>
              </article>
            </div>
            <p v-if="!mediaList.length" class="admin-muted">没有匹配资源</p>
          </section>
        </template>

        <section v-else-if="selected === 'inquiries'" class="admin-panel">
          <h2>客户询盘</h2>
          <div class="admin-tabs">
            <button :class="{ active: inquiryLocale === '' }" @click="inquiryLocale = ''">
              全部询盘
            </button>
            <button
              :class="{ active: inquiryLocale === 'zh' }"
              @click="inquiryLocale = 'zh'"
            >
              中文询盘
            </button>
            <button
              :class="{ active: inquiryLocale === 'en' }"
              @click="inquiryLocale = 'en'"
            >
              English 询盘
            </button>
          </div>
          <div class="admin-toolbar">
            <label>开始日期 <input v-model="inquiryStartDate" type="date" aria-label="询盘开始日期" /></label>
            <label>结束日期 <input v-model="inquiryEndDate" type="date" aria-label="询盘结束日期" /></label>
            <ElButton @click="inquiryStartDate = ''; inquiryEndDate = ''">清除日期</ElButton>
            <ElButton :loading="loadingInquiries" @click="queryInquiries">查询</ElButton>
            <ElButton type="primary" :loading="exportingInquiries" @click="exportInquiries">导出 Excel</ElButton>
          </div>
          <p class="admin-muted">{{ loadingInquiries ? '正在查询…' : `共 ${visibleInquiries.length} 条` }}，按数据库 created_at 从新到旧查询；结束日期默认今天，起止日包含全天。导出使用相同日期及语言条件。</p>
          <ElTable :data="visibleInquiries" empty-text="所选条件暂无客户询盘">
            <ElTableColumn label="提交时间" min-width="180">
              <template #default="{ row }">{{ formatInquiryTime(row.createdAt) }}</template>
            </ElTableColumn>
            <ElTableColumn prop="name" label="姓名" />
            <ElTableColumn prop="email" label="邮箱" min-width="220" />
            <ElTableColumn prop="phone" label="电话" min-width="180" />
            <ElTableColumn prop="message" label="需求" show-overflow-tooltip />
            <ElTableColumn prop="country" label="国家/地区" />
            <ElTableColumn prop="industry" label="行业" />
            <ElTableColumn label="产品">
              <template #default="{ row }">{{
                records.products?.find((p) => p.id === row.product)?.name ||
                row.product
              }}</template>
            </ElTableColumn>
            <ElTableColumn prop="company" label="公司" />
            <ElTableColumn prop="website" label="网站" />
            <ElTableColumn label="跟进状态">
              <template #default="{ row }">
                <ElSelect
                  :model-value="row.status"
                  @change="updateInquiry(row as InquiryRow, $event)"
                >
                  <ElOption label="待处理" value="NEW" />
                  <ElOption label="跟进中" value="PROCESSING" />
                  <ElOption label="已完成" value="CLOSED" />
                </ElSelect>
              </template>
            </ElTableColumn>
            <ElTableColumn label="操作" fixed="right" width="110">
              <template #default="{ row }">
                <ElButton link type="primary" @click="viewInquiry(row)">查看详情</ElButton>
              </template>
            </ElTableColumn>
          </ElTable>
          <ElDialog v-model="inquiryDialog" title="询盘详情" width="min(720px, 94vw)">
            <dl v-if="inquiryDetail" class="inquiry-detail">
              <dt>提交时间</dt><dd>{{ formatInquiryTime(inquiryDetail.createdAt) }}</dd>
              <dt>姓名</dt><dd>{{ inquiryDetail.name }}</dd>
              <dt>邮箱</dt><dd><a :href="`mailto:${inquiryDetail.email}`">{{ inquiryDetail.email }}</a></dd>
              <dt>电话</dt><dd>{{ inquiryDetail.phone }}</dd>
              <dt>公司</dt><dd>{{ inquiryDetail.company || '未填写' }}</dd>
              <dt>国家/地区</dt><dd>{{ inquiryDetail.country || '未填写' }}</dd>
              <dt>行业</dt><dd>{{ inquiryDetail.industry || '未填写' }}</dd>
              <dt>语言</dt><dd>{{ inquiryDetail.locale === 'zh' ? '中文' : 'English' }}</dd>
              <dt>跟进状态</dt><dd>{{ inquiryStatusLabels[inquiryDetail.status] || inquiryDetail.status }}</dd>
              <dt v-if="inquiryDetail.product">历史产品</dt><dd v-if="inquiryDetail.product">{{ records.products?.find((p) => p.id === inquiryDetail?.product)?.name || inquiryDetail.product }}</dd>
              <dt v-if="inquiryDetail.website">历史网站</dt><dd v-if="inquiryDetail.website">{{ inquiryDetail.website }}</dd>
              <dt>留言内容</dt><dd>{{ inquiryDetail.message }}</dd>
              <dt>询盘ID</dt><dd>{{ inquiryDetail.id }}</dd>
            </dl>
          </ElDialog>
        </section>

        <template v-else-if="selected === 'settings'">
          <section class="admin-panel">
            <h2>网站基础信息</h2>
            <p class="admin-muted">
              保存到数据库后，官网页头、页脚、企业介绍和联系页读取这些设置。
            </p>
            <ElForm label-position="top" class="admin-settings-form"
              ><section>
                <h3>中文信息</h3>
                <ElFormItem label="中文企业名称"
                  ><ElInput v-model="settings.companyName" maxlength="120"
                /></ElFormItem>
                <ElFormItem label="中文企业介绍"
                  ><ElInput
                    v-model="settings.companyIntro"
                    type="textarea"
                    :rows="4"
                    maxlength="4000"
                /></ElFormItem>
                <ElFormItem label="中文地址"
                  ><ElInput v-model="settings.address" maxlength="300"
                /></ElFormItem>
              </section>
              <section>
                <h3>English 信息</h3>
                <ElFormItem label="英文企业名称"
                  ><ElInput v-model="settings.companyNameEn" maxlength="180"
                /></ElFormItem>
                <ElFormItem label="英文企业介绍"
                  ><ElInput
                    v-model="settings.companyIntroEn"
                    type="textarea"
                    :rows="4"
                    maxlength="4000"
                /></ElFormItem>
                <ElFormItem label="英文地址"
                  ><ElInput v-model="settings.addressEn" maxlength="300"
                /></ElFormItem>
              </section>
              <ElFormItem label="联系邮箱"
                ><ElInput
                  v-model="settings.contactEmail"
                  type="email" /></ElFormItem
              ><ElFormItem label="联系电话"
                ><ElInput v-model="settings.contactPhone" /></ElFormItem
              ><ElFormItem label="默认语言"
                ><ElSelect v-model="settings.defaultLocale"
                  ><ElOption label="中文" value="zh" /><ElOption
                    label="English"
                    value="en" /></ElSelect></ElFormItem
              ><ElFormItem label="人工客服 HTTPS 地址"
                ><ElInput v-model="settings.supportUrl" /></ElFormItem></ElForm
            ><ElButton type="primary" :loading="busy" @click="saveSettings"
              >保存网站设置</ElButton
            >
          </section>
          <section class="admin-panel">
            <h2>服务器文件存储 · 预留</h2>
            <dl class="admin-storage">
              <div>
                <dt>存储方式</dt>
                <dd>服务器本地文件（产品图片已接入 Java）</dd>
              </div>
              <div>
                <dt>公开访问前缀</dt>
                <dd><code>/media/</code>（建议值，可配置）</dd>
              </div>
              <div>
                <dt>公开目录配置</dt>
                <dd><code>MEDIA_PUBLIC_ROOT</code> · 待核实宝塔目录</dd>
              </div>
              <div>
                <dt>私有目录配置</dt>
                <dd><code>MEDIA_PRIVATE_ROOT</code> · 与公开目录隔离</dd>
              </div>
              <div>
                <dt>磁盘路径</dt>
                <dd>仅服务端配置，前端不获取实际路径</dd>
              </div>
            </dl>
            <p class="admin-muted">
              独立于 .output，数据库记录 object key
              和媒体元数据。目前产品保存前会发布所选图片，草稿关联的图片也可能公开；上传目录不可执行脚本。
            </p>
          </section>
        </template>
        <footer class="admin-footer">
          官网管理后台 · 产品、文章与询盘来自 Java / MySQL
        </footer>
      </main>
    </div>
    <ElDialog
      v-model="dialog"
      :title="editId ? '编辑内容' : '新增内容'"
      width="min(580px, 94vw)"
      destroy-on-close
    >
      <ElForm label-position="top"
        ><ElFormItem
          :label="selected === 'articles' ? '标题（必填）' : '中文名称（必填）'"
          ><ElInput
            v-model="form.name"
            :aria-label="selected === 'articles' ? '标题' : '中文名称'"
            maxlength="120" /></ElFormItem
        ><ElFormItem v-if="selected !== 'articles'" label="英文名称（必填）"
          ><ElInput v-model="form.nameEn" aria-label="英文名称" maxlength="180"
        /></ElFormItem>
        <div class="admin-form-grid">
          <ElFormItem v-if="selected !== 'categories'" label="分类"
            ><ElInput v-model="form.category" /></ElFormItem
          ><ElFormItem v-if="selected === 'products'" label="品牌"
            ><ElInput v-model="form.brand"
          /></ElFormItem>
        </div>
        <ElFormItem v-if="selected === 'products'" label="产品图片">
          <div>
            <img
              v-if="form.image"
              :src="form.image"
              alt="产品图片预览"
              class="admin-product-preview"
            />
            <label class="admin-file-button"
              >选择产品图片
              <input
                type="file"
                aria-label="产品图片"
                accept=".jpg,.jpeg,.png,.webp"
                @change="selectProductImage"
              />
            </label>
            <p class="admin-muted">
              保存时上传Java服务器并发布图片后关联产品；草稿图片也可公开，请勿上传敏感资料。
            </p>
          </div>
        </ElFormItem>
        <ElFormItem label="状态"
          ><ElSelect v-model="form.status"
            ><ElOption
              v-for="(label, value) in statusLabels"
              :key="value"
              :label="label"
              :value="value" /></ElSelect></ElFormItem
        ><ElFormItem label="内容"
          ><ElInput
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="20000" /></ElFormItem
        ><ElFormItem label="英文内容（选填）"
          ><ElInput
            v-model="form.descriptionEn"
            type="textarea"
            :rows="3" /></ElFormItem
      ></ElForm>
      <template #footer
        ><ElButton @click="dialog = false">取消</ElButton
        ><ElButton type="primary" :loading="busy" @click="save"
          >保存到数据库</ElButton
        ></template
      >
    </ElDialog>
  </div>
</template>

<style scoped>
.inquiry-detail {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  gap: 14px;
}
.inquiry-detail dd {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.admin-login {
  max-width: 440px;
  margin: 80px auto;
  padding: 24px;
}
.admin-product-image {
  width: 64px;
  height: 64px;
  object-fit: contain;
}
.admin-product-preview {
  display: block;
  width: 160px;
  height: 120px;
  object-fit: contain;
  margin-bottom: 12px;
}
.admin-shell {
  --el-color-primary: #67783a;
  --el-color-primary-light-3: #8c9a6a;
  --el-color-primary-light-9: #f1f4eb;
  display: flex;
  min-height: 100vh;
  background: #f3f5f7;
  color: #263238;
  font-size: 14px;
}
.admin-sidebar {
  width: 232px;
  flex-shrink: 0;
  background: #203029;
  color: #becbc2;
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 24px 14px;
}
.admin-logo {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 0 16px 38px;
  color: white;
  font-size: 18px;
  font-weight: 600;
}
.admin-logo b {
  font-size: 34px;
  color: #b8ca89;
}
.admin-logo small {
  display: block;
  font-size: 8px;
  letter-spacing: 1.5px;
  color: #8ea496;
  margin-top: 6px;
}
.admin-menu-label {
  font-size: 10px;
  letter-spacing: 2px;
  padding: 0 18px 18px;
  color: #82968a;
}
.admin-nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 18px;
  margin-bottom: 6px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  text-align: left;
}
.admin-nav-item > span {
  font-size: 19px;
  width: 22px;
}
.admin-nav-item:hover {
  background: #ffffff0a;
  color: white;
}
.admin-nav-item.active {
  background: #67783a;
  color: white;
}
.admin-sidebar-bottom {
  margin-top: auto;
  padding: 24px 16px 0;
  border-top: 1px solid #ffffff15;
  font-size: 12px;
}
.admin-sidebar-bottom small {
  display: block;
  color: #82968a;
  margin-top: 8px;
}
.admin-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #adc585;
  margin-right: 8px;
}
.admin-workspace {
  flex: 1;
  min-width: 0;
}
.admin-header {
  height: 72px;
  background: white;
  border-bottom: 1px solid #e6e9ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  font-size: 12px;
}
.admin-header > div {
  display: flex;
  align-items: center;
  gap: 14px;
}
.admin-divider {
  color: #c4c9c6;
}
.admin-site-link {
  color: #67783a;
  margin-right: 15px;
}
.admin-avatar {
  background: #edf1e5;
  color: #67783a;
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 50%;
}
.admin-main {
  padding: 30px;
  max-width: 1600px;
  margin: auto;
}
.admin-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}
.admin-title p {
  font-size: 10px;
  letter-spacing: 2px;
  color: #8a9690;
  margin: 0 0 8px;
}
.admin-title h1 {
  font-size: 26px;
  margin: 0;
}
.admin-notice {
  padding: 14px 18px;
  background: #fff8e7;
  border: 1px solid #f0e2bf;
  color: #8d7138;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.8;
  margin-bottom: 24px;
}
.admin-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 24px;
}
.admin-stats > div {
  padding: 24px;
  background: white;
  border: 1px solid #e7ebee;
  border-radius: 8px;
}
.admin-stats span {
  display: block;
  font-size: 13px;
  color: #697970;
}
.admin-stats strong {
  display: block;
  font-size: 34px;
  margin: 16px 0 10px;
}
.admin-stats small {
  color: #98a29c;
  font-size: 11px;
}
.admin-panel {
  background: white;
  border: 1px solid #e7ebee;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  min-width: 0;
}
.admin-panel h2 {
  font-size: 16px;
  margin-bottom: 22px;
  letter-spacing: 0;
}
.admin-overview-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 24px;
}
.admin-quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.admin-quick-grid button {
  padding: 22px 8px;
  border: 1px solid #e7ebee;
  background: #fafbf8;
  border-radius: 6px;
}
.admin-quick-grid span {
  display: block;
  font-size: 28px;
  color: #67783a;
}
.admin-quick-grid strong {
  display: block;
  font-size: 12px;
  margin: 16px 0 8px;
}
.admin-quick-grid small {
  font-size: 10px;
  color: #929b95;
}
.admin-progress {
  list-style: none;
  padding: 0;
  margin: 0;
}
.admin-progress li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  font-size: 12px;
}
.admin-muted {
  font-size: 12px;
  color: #89958e;
  line-height: 1.9;
}
.admin-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
}
.admin-toolbar > .el-input {
  width: 280px;
}
.admin-toolbar > .el-select {
  width: 150px;
}
.admin-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  font-size: 12px;
  color: #89958e;
}
.admin-tabs {
  display: flex;
  gap: 20px;
  border-bottom: 1px solid #e8ece8;
  margin-bottom: 24px;
}
.admin-tabs button {
  border: 0;
  background: none;
  padding: 0 6px 15px;
  color: #89958e;
}
.admin-tabs .active {
  color: #67783a;
  border-bottom: 2px solid #67783a;
  font-weight: 600;
}
.admin-media-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}
.admin-media-grid article {
  border: 1px solid #e7ebee;
  border-radius: 6px;
  overflow: hidden;
}
.admin-media-grid img,
.admin-media-grid video,
.admin-pdf {
  width: 100%;
  height: 165px;
  object-fit: cover;
  background: #eef1eb;
}
.admin-pdf {
  display: grid;
  place-items: center;
  color: #67783a;
  font-size: 28px;
}
.admin-media-grid article > div:not(.admin-pdf) {
  padding: 14px;
}
.admin-media-grid strong {
  display: block;
  font-size: 12px;
  overflow-wrap: anywhere;
}
.admin-media-grid small {
  display: block;
  margin-top: 8px;
  color: #929b95;
  font-size: 10px;
}
.admin-media-grid a {
  display: block;
  font-size: 12px;
  color: #67783a;
  margin-top: 8px;
}
.admin-file-button {
  display: inline-block;
  background: #67783a;
  color: white;
  padding: 9px 15px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
}
.admin-file-button input {
  display: block;
  max-width: 240px;
  margin-top: 8px;
  font-size: 10px;
}
.admin-empty {
  text-align: center;
  padding: 65px 24px;
  min-height: 340px;
}
.admin-empty > span {
  font-size: 48px;
  color: #b6c49e;
}
.admin-empty p {
  font-size: 13px;
  max-width: 560px;
  color: #89958e;
  margin: 0 auto 24px;
}
.admin-settings-form,
.admin-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
}
.admin-storage {
  margin: 0;
}
.admin-storage > div {
  display: flex;
  gap: 30px;
  padding: 14px 0;
  border-bottom: 1px solid #eef0ed;
  font-size: 12px;
}
.admin-storage dt {
  color: #89958e;
  width: 130px;
  flex-shrink: 0;
}
.admin-storage dd {
  margin: 0;
  overflow-wrap: anywhere;
}
.admin-footer {
  text-align: center;
  font-size: 11px;
  color: #a1aaa5;
  padding: 8px 0 16px;
}
.admin-mobile-toggle {
  display: none;
}
@media (max-width: 1100px) {
  .admin-sidebar {
    width: 200px;
  }
  .admin-main {
    padding: 22px;
  }
  .admin-overview-grid {
    grid-template-columns: 1fr;
  }
  .admin-media-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 760px) {
  .admin-sidebar {
    display: none;
    position: fixed;
    z-index: 40;
    left: 0;
    top: 72px;
    height: calc(100vh - 72px);
  }
  .admin-sidebar.is-open {
    display: flex;
  }
  .admin-header {
    padding: 0 16px;
  }
  .admin-header > div {
    gap: 9px;
  }
  .admin-header > div:last-child > span:last-child {
    display: none;
  }
  .admin-mobile-toggle {
    display: block;
    border: 0;
    background: #f3f5f0;
    padding: 8px;
  }
  .admin-site-link {
    margin: 0;
  }
  .admin-main {
    padding: 18px 12px;
  }
  .admin-title {
    align-items: flex-start;
  }
  .admin-title h1 {
    font-size: 22px;
  }
  .admin-stats {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .admin-stats > div {
    padding: 18px;
  }
  .admin-panel {
    padding: 18px 12px;
  }
  .admin-quick-grid {
    grid-template-columns: 1fr 1fr;
  }
  .admin-media-grid {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .admin-pagination {
    flex-wrap: wrap;
  }
  .admin-settings-form,
  .admin-form-grid {
    grid-template-columns: 1fr;
  }
  .admin-storage > div {
    flex-direction: column;
    gap: 8px;
  }
  .admin-notice {
    padding: 12px;
  }
  .admin-toolbar > .el-input {
    width: 100%;
  }
}
</style>
