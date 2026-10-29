<script setup lang="ts">
import { text, locales, resolvePath, type Locale } from "~/data/catalog";
import type { ContentRow } from "~/composables/useJavaApi";
// 动态目录[locale]会匹配任意字符串，这里只接受已启用语言。
definePageMeta({
  validate: (route) => locales.includes(route.params.locale as Locale),
});
const route = useRoute();
const config = useRuntimeConfig();
const site = await useSiteSettings();
const media = await useSiteMedia();
const partnersUrl = computed(
  () => media.value.find((m) => m.id === "partners")?.url ?? "/images/brands/brands.webp",
);
const locale = computed(() => route.params.locale as Locale);
const siteCompany = computed(() =>
  locale.value === "en"
    ? site.value.companyNameEn || t.value.name
    : site.value.companyName || t.value.name,
);
const t = computed(() => text[locale.value]);
const companyIntro = computed(
  () =>
    (locale.value === "en"
      ? site.value.companyIntroEn
      : site.value.companyIntro) || t.value.companyText,
);

// ========== API 数据 ==========
const { data: productResult, error: productError } = await useFetch<{
  data: ContentRow[];
}>("/api/public/content/products", {
  key: () => route.fullPath + "_products",
});
const { data: articleResult } = await useFetch<{
  data: ContentRow[];
}>("/api/public/content/articles", {
  key: () => route.fullPath + "_articles",
});
if (productError.value) {
  throw createError({
    statusCode: 503,
    statusMessage: "Content service unavailable",
  });
}

const products = computed(() =>
  (productResult.value?.data || []).map((row) => ({
    id: row.id,
    model: (locale.value === "en" ? row.nameEn : row.name) || "",
    category: row.category,
    brand: row.brand,
    image: row.imageId ? `/api/public/media/${row.imageId}/image` : "",
    description: locale.value === "en" ? row.descriptionEn : row.description,
    sections: row.sections,
  })),
);
const articles = computed(() =>
  (articleResult.value?.data || []).map((row) => ({
    id: row.id,
    title: (locale.value === "en" ? row.nameEn : row.title) || row.title || "",
    body: locale.value === "en" ? row.descriptionEn : row.description,
    sections: row.sections,
  })),
);

const categories = computed(() => [
  ...new Set(products.value.map((p) => p.category).filter(Boolean)),
]);
const brands = computed(() => [
  ...new Set(products.value.map((p) => p.brand).filter(Boolean)),
]);

// ========== 路由解析 ==========
const view = computed(() => {
  const slug = route.params.slug;
  const parts = Array.isArray(slug) ? slug : slug ? [String(slug)] : [];
  if (parts.length === 2) {
    if (
      parts[0] === "products" &&
      products.value.some((p) => p.id === parts[1])
    )
      return { type: "product", id: parts[1]! };
    if (
      parts[0] === "articles" &&
      articles.value.some((a) => a.id === parts[1])
    )
      return { type: "article", id: parts[1]! };
    return null;
  }
  return resolvePath(parts);
});
if (!view.value)
  throw createError({ statusCode: 404, statusMessage: "Page not found" });
watch(view, (value) => {
  if (!value) showError({ statusCode: 404, statusMessage: "Page not found" });
});

// ========== 产品筛选 ==========
const query = ref("");
const category = ref("");
const brand = ref("");
const page = ref(1);
const filtered = computed(() =>
  products.value.filter(
    (p) =>
      (!category.value || p.category === category.value) &&
      (!brand.value || p.brand === brand.value) &&
      `${p.model} ${p.category} ${p.brand}`
        .toLowerCase()
        .includes(query.value.trim().toLowerCase()),
  ),
);
const pageSize = 4;
const pageCount = computed(() =>
  Math.max(1, Math.ceil(filtered.value.length / pageSize)),
);
const pageProducts = computed(() =>
  filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize),
);
watch([query, category, brand], () => {
  page.value = 1;
});

// ========== 当前详情对象 ==========
const product = computed(() =>
  products.value.find((p) => p.id === view.value?.id),
);
const article = computed(() =>
  articles.value.find((a) => a.id === view.value?.id),
);

// ========== SEO 标题 & 描述 ==========
const SITE_URL = config.public.siteUrl || "https://yourdomain.com";

const title = computed(() => {
  const v = view.value;
  if (!v) return siteCompany.value;

  if (v.type === "product") {
    const p = product.value;
    if (!p) return siteCompany.value;
    const brand = p.brand ? `${p.brand} ` : "";
    const cat = p.category ? ` ${p.category}` : "";
    return locale.value === "en"
      ? `${brand}${p.model}${cat} | GWW Specialty Materials`
      : `${brand}${p.model}${cat} | GWW 塑料原料供应商`;
  }
  if (v.type === "article") {
    return `${article.value?.title || ""} | ${siteCompany.value}`;
  }
  if (v.type === "home") return siteCompany.value;
  const names: Record<string, string> = {
    about: locale.value === "en" ? "About GWW" : "关于 GWW",
    products: locale.value === "en" ? "Plastic Materials Catalog" : "塑料原料产品中心",
    articles: locale.value === "en" ? "Industry News & Knowledge" : "行业资讯与知识",
    brands: locale.value === "en" ? "Our Partner Brands" : "代理品牌",
    contact: locale.value === "en" ? "Contact GWW for Material Quotes" : "联系 GWW 获取报价",
  };
  return `${names[v.type] || ""} | ${siteCompany.value}`;
});

const pageDescription = computed(() => {
  const v = view.value;
  if (!v) return t.value.intro;

  if (v.type === "product") {
    const p = product.value;
    if (!p) return t.value.intro;
    // 优先用后台填写的 description，否则自动兜底
    if (p.description && p.description.trim()) {
      return p.description.length > 160 ? p.description.slice(0, 157) + "..." : p.description;
    }
    const brand = p.brand || "";
    const model = p.model || "";
    const cat = p.category || "";
    return locale.value === "en"
      ? `GWW supplies ${brand} ${model} ${cat}. Contact us for pricing and availability.`
      : `GWW 代理${brand} ${model} ${cat}塑料原料，欢迎咨询报价和现货情况。`;
  }
  if (v.type === "article") {
    const a = article.value;
    if (!a) return t.value.intro;
    if (a.body && a.body.trim()) {
      return a.body.length > 160 ? a.body.slice(0, 157) + "..." : a.body;
    }
    return `${a.title} — ${siteCompany.value}`;
  }
  const descs: Record<string, string> = {
    home: locale.value === "en"
      ? "GWW is a specialty plastics supplier distributing TPV, TPU, PP, PE and other plastic materials from leading global brands."
      : "GWW 专业代理道达尔、中石化、LG化学等品牌的 TPV、TPU、PP、PE 等塑料原料，服务汽车、建筑、工业领域。",
    about: locale.value === "en"
      ? "About GWW — a specialty plastics import and distribution company connecting global brands with customers in Asia and beyond."
      : "关于 GWW — 专注塑料原料进口分销的专业公司，连接全球品牌与亚洲客户。",
    products: locale.value === "en"
      ? "Browse GWW's catalog of specialty plastic materials: TPV thermoplastic vulcanizates, TPU elastomers, PP polypropylene, PE polyethylene and more."
      : "浏览 GWW 塑料原料产品目录：TPV 热塑性硫化橡胶、TPU 弹性体、PP 聚丙烯、PE 聚乙烯等。",
    articles: locale.value === "en"
      ? "Industry insights and knowledge about specialty plastic materials, TPV, TPU, polypropylene, and their applications."
      : "关于塑料原料、TPV、TPU、聚丙烯及其应用的行业资讯与知识分享。",
    brands: locale.value === "en"
      ? "GWW partners with leading global plastic material brands including TotalEnergies, Sinopec, LG Chem and Dow."
      : "GWW 与道达尔、中石化、LG化学、陶氏等全球领先塑料原料品牌合作。",
    contact: locale.value === "en"
      ? "Contact GWW for plastic material pricing, product inquiries, and sample requests."
      : "联系 GWW 获取塑料原料报价、产品咨询和样品申请。",
  };
  return descs[v.type] || t.value.intro;
});

const pageKeywords = computed(() => {
  const v = view.value;
  if (!v) return "plastic materials, 塑料原料, GWW";
  if (v.type === "product") {
    const p = product.value;
    if (!p) return "plastic materials, 塑料原料";
    const items = p.brand ? [p.brand] : [];
    if (p.model) items.push(p.model);
    if (p.category) items.push(p.category);
    items.push(locale.value === "en" ? "plastic materials" : "塑料原料");
    items.push("GWW");
    return items.join(", ");
  }
  if (v.type === "articles")
    return locale.value === "en"
      ? "plastic materials news, TPV, TPU, polypropylene, GWW"
      : "塑料原料资讯, TPV, TPU, 聚丙烯, GWW";
  if (v.type === "products")
    return locale.value === "en"
      ? "TPV, TPU, polypropylene PP, polyethylene PE, plastic materials supplier"
      : "TPV 热塑性硫化橡胶, TPU 弹性体, 聚丙烯 PP, 聚乙烯 PE, 塑料原料供应商";
  return locale.value === "en" ? "plastic materials, GWW" : "塑料原料, GWW";
});

const canonicalUrl = computed(() => `${SITE_URL}${route.path}`);
const zhPath = computed(() => route.path.replace(`/${locale.value}/`, "/zh/"));
const enPath = computed(() => route.path.replace(`/${locale.value}/`, "/en/"));
const zhUrl = computed(() => `${SITE_URL}${zhPath.value}`);
const enUrl = computed(() => `${SITE_URL}${enPath.value}`);
const defaultUrl = computed(() => `${SITE_URL}${enPath.value}`); // 英文为 x-default

// ========== 搜索结果面包屑（用于 title 显示） ==========
const titleDisplay = computed(() => {
  const v = view.value;
  if (!v) return "";
  if (v.type === "product") return product.value?.model || "";
  if (v.type === "article") return article.value?.title || "";
  const names: Record<string, string> = {
    home: t.value.nav[0],
    about: t.value.nav[1],
    products: t.value.nav[2],
    articles: t.value.nav[3],
    brands: locale.value === "zh" ? "品牌展示" : "Brands",
    contact: locale.value === "zh" ? "联系我们" : "Contact",
  };
  return names[v.type] || "";
});

// ========== 页面级 useHead（SEO 全套） ==========
useHead(() => {
  const v = view.value;
  const isProduct = v?.type === "product";
  const isArticle = v?.type === "article";

  // BreadcrumbList JSON-LD
  const homeUrl = `${SITE_URL}/${locale.value}`;
  const breadcrumbList: any = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: t.value.nav[0], item: homeUrl },
    ],
  };
  if (v?.type === "products") {
    breadcrumbList.itemListElement.push({ "@type": "ListItem", position: 2, name: titleDisplay.value, item: `${SITE_URL}/${locale.value}/products` });
  } else if (isProduct && product.value) {
    breadcrumbList.itemListElement.push({ "@type": "ListItem", position: 2, name: t.value.nav[2], item: `${SITE_URL}/${locale.value}/products` });
    breadcrumbList.itemListElement.push({ "@type": "ListItem", position: 3, name: titleDisplay.value, item: canonicalUrl.value });
  } else if (v?.type === "articles") {
    breadcrumbList.itemListElement.push({ "@type": "ListItem", position: 2, name: titleDisplay.value, item: `${SITE_URL}/${locale.value}/articles` });
  } else if (isArticle && article.value) {
    breadcrumbList.itemListElement.push({ "@type": "ListItem", position: 2, name: t.value.nav[3], item: `${SITE_URL}/${locale.value}/articles` });
    breadcrumbList.itemListElement.push({ "@type": "ListItem", position: 3, name: titleDisplay.value, item: canonicalUrl.value });
  } else if (v && v.type !== "home") {
    breadcrumbList.itemListElement.push({ "@type": "ListItem", position: 2, name: titleDisplay.value, item: canonicalUrl.value });
  }

  // Product JSON-LD（仅产品详情页）
  const productSchema = isProduct && product.value ? {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.value.model,
    brand: product.value.brand ? { "@type": "Brand", name: product.value.brand } : undefined,
    category: product.value.category,
    description: pageDescription.value,
    image: product.value.image ? [`${SITE_URL}${product.value.image}`] : [],
    url: canonicalUrl.value,
  } : null;

  // Article JSON-LD（仅文章详情页）
  const articleSchema = isArticle && article.value ? {
    "@context": "https://schema.org",
    "@type": "Article",
    headline: article.value.title,
    description: pageDescription.value,
    url: canonicalUrl.value,
    publisher: { "@type": "Organization", name: siteCompany.value },
    inLanguage: locale.value,
  } : null;

  const scripts = [JSON.stringify(breadcrumbList)];
  if (productSchema) scripts.push(JSON.stringify(productSchema));
  if (articleSchema) scripts.push(JSON.stringify(articleSchema));

  return {
    title: title.value,
    meta: [
      { name: "description", content: pageDescription.value },
      { name: "keywords", content: pageKeywords.value },
      // Open Graph
      { property: "og:title", content: title.value },
      { property: "og:description", content: pageDescription.value },
      { property: "og:type", content: isProduct ? "product" : "website" },
      { property: "og:url", content: canonicalUrl.value },
      { property: "og:locale", content: locale.value === "zh" ? "zh_CN" : "en_US" },
      { property: "og:site_name", content: siteCompany.value },
      // Twitter Card
      { name: "twitter:card", content: "summary_large_image" },
      { name: "twitter:title", content: title.value },
      { name: "twitter:description", content: pageDescription.value },
    ],
    link: [
      { rel: "canonical", href: canonicalUrl.value },
      { rel: "alternate", hreflang: "zh", href: zhUrl.value },
      { rel: "alternate", hreflang: "en", href: enUrl.value },
      { rel: "alternate", hreflang: "x-default", href: defaultUrl.value },
    ],
    script: scripts.map((s) => ({
      type: "application/ld+json",
      innerHTML: s,
    })),
  };
});

function reset() {
  query.value = "";
  category.value = "";
  brand.value = "";
  page.value = 1;
}
</script>

<template>
  <div v-if="view">
    <!-- ========== 首页 ========== -->
    <template v-if="view.type === 'home'">
      <HeroCarousel :locale="locale" />
      <section class="section brands-home container">
        <div class="section-heading">
          <div>
            <p class="eyebrow">MATERIAL BRANDS</p>
            <h2>{{ t.brandsTitle }}</h2>
          </div>
          <NuxtLink class="text-link" :to="`/${locale}/brands`">{{ t.detail }} ↗</NuxtLink>
        </div>
        <img class="brands-image" :src="partnersUrl" :alt="t.brandsTitle" width="1100" height="340" loading="lazy" />
        <p class="muted small">{{ t.brandsNote }}</p>
      </section>
      <section class="section soft">
        <div class="container">
          <div class="section-heading">
            <div>
              <p class="eyebrow">PRODUCT PORTFOLIO</p>
              <h2>{{ t.featured }}</h2>
              <p class="muted">{{ t.featuredSub }}</p>
            </div>
            <NuxtLink class="text-link" :to="`/${locale}/products`">{{ t.browse }} ↗</NuxtLink>
          </div>
          <div class="product-grid">
            <ProductCard v-for="item in products.slice(0, 6)" :key="item.id" :product="item" :locale="locale" />
          </div>
        </div>
      </section>
      <section class="section container about-panel">
        <div class="about-visual">
          <img src="/images/lb/block_3.webp" :alt="t.companyTitle" loading="lazy" /><span>SPECIALTY<br />MATERIALS</span>
        </div>
        <div>
          <p class="eyebrow">ABOUT THE COMPANY</p>
          <h2>{{ t.companyTitle }}</h2>
          <p class="muted prose">{{ companyIntro }}</p>
          <NuxtLink class="button" :to="`/${locale}/about`">{{ t.detail }} ↗</NuxtLink>
        </div>
      </section>
      <section class="advantages">
        <div class="container advantage-grid">
          <div v-for="(item, i) in t.advantages" :key="item">
            <span class="number">0{{ i + 1 }}</span>
            <h3>{{ item }}</h3>
            <p>{{ t.advantageText[i] }}</p>
          </div>
        </div>
      </section>
    </template>

    <!-- ========== 内页公共 Banner ========== -->
    <template v-else>
      <section v-if="view.type !== 'contact'" class="page-banner">
        <div class="container">
          <div class="breadcrumb">
            <NuxtLink :to="`/${locale}`">{{ t.nav[0] }}</NuxtLink><span>/</span><span>{{ titleDisplay }}</span>
          </div>
          <p class="eyebrow">SPECIALTY MATERIALS</p>
          <h1>{{ titleDisplay }}</h1>
        </div>
      </section>

      <!-- ========== 产品列表页 ========== -->
      <section v-if="view.type === 'products'" class="section container catalog-layout">
        <aside class="filters">
          <h2>{{ t.nav[2] }}</h2>
          <div class="filter-group" role="group" :aria-label="t.category">
            <h3>{{ t.category }}</h3>
            <div class="filter-options">
              <button type="button" :class="{ active: category === '' }" :aria-pressed="category === ''" @click="category = ''">
                {{ t.all }}
              </button>
              <button v-for="value in categories" :key="value" type="button" :class="{ active: category === value }" :aria-pressed="category === value" @click="category = value">
                {{ value }}
              </button>
            </div>
          </div>
          <div class="filter-group" role="group" :aria-label="t.brand">
            <h3>{{ t.brand }}</h3>
            <div class="filter-options">
              <button type="button" :class="{ active: brand === '' }" :aria-pressed="brand === ''" @click="brand = ''">
                {{ t.all }}
              </button>
              <button v-for="value in brands" :key="value" type="button" :class="{ active: brand === value }" :aria-pressed="brand === value" @click="brand = value">
                {{ value }}
              </button>
            </div>
          </div>
          <button class="text-link" @click="reset">{{ t.reset }}</button>
          <p class="small muted">{{ t.featuredSub }}</p>
        </aside>
        <div>
          <div class="search-row">
            <label class="sr-only" for="search">{{ t.search }}</label><input id="search" v-model="query" type="search" :placeholder="t.search" /><span>{{ filtered.length }} {{ t.results }}</span>
          </div>
          <div v-if="pageProducts.length" class="product-grid catalog-grid">
            <ProductCard v-for="item in pageProducts" :key="item.id" :product="item" :locale="locale" />
          </div>
          <div v-else class="empty-state">
            <h2>{{ t.empty }}</h2>
            <button class="button" @click="reset">{{ t.reset }}</button>
          </div>
          <nav v-if="pageCount > 1" class="pagination" :aria-label="locale === 'zh' ? '产品分页' : 'Product pagination'">
            <button :disabled="page === 1" @click="page--">
              {{ t.previous }}</button><button v-for="i in pageCount" :key="i" :class="{ active: page === i }" :aria-current="page === i ? 'page' : undefined" @click="page = i">
              {{ i }}</button><button :disabled="page === pageCount" @click="page++">
              {{ t.next }}
            </button>
          </nav>
        </div>
      </section>

      <!-- ========== 产品详情页 ========== -->
      <section v-else-if="view.type === 'product' && product" class="section container">
        <div class="product-detail">
          <img v-if="product.image" :src="product.image" :alt="titleDisplay" width="300" height="300" />
          <div>
            <h2>{{ titleDisplay }}</h2>
            <dl>
              <div>
                <dt>{{ t.brand }}</dt>
                <dd>{{ product.brand }}</dd>
              </div>
              <div>
                <dt>{{ t.category }}</dt>
                <dd>{{ product.category }}</dd>
              </div>
              <div>
                <dt>{{ t.model }}</dt>
                <dd>{{ product.model }}</dd>
              </div>
            </dl>
            <p class="muted">{{ product.description }}</p>
            <NuxtLink class="button" :to="`/${locale}/contact`">{{ t.contact }} ↗</NuxtLink>
          </div>
        </div>
        <div class="detail-sections">
          <template v-if="product.sections && product.sections.length">
            <div v-for="(sec, idx) in product.sections" :key="idx" class="detail-section">
              <h2>{{ locale === "zh" ? sec.title : sec.titleEn }}</h2>
              <p>{{ locale === "zh" ? sec.body : sec.bodyEn }}</p>
            </div>
          </template>
          <template v-else>
            <h2>{{ t.parameters }}</h2>
            <p>{{ t.parametersText }}</p>
            <h2>{{ t.application }}</h2>
            <p>{{ t.applicationText }}</p>
            <p class="notice">{{ t.brandsNote }}</p>
          </template>
        </div>
      </section>

      <!-- ========== 文章列表页 ========== -->
      <section v-else-if="view.type === 'articles'" class="section container">
        <div v-if="articles.length" class="article-grid">
          <NuxtLink v-for="item in articles" :key="item.id" class="article-card" :to="`/${locale}/articles/${item.id}`">
            <h3>{{ item.title }}</h3>
            <p>{{ item.body }}</p>
            <span class="text-link">{{ t.read }} ↗</span>
          </NuxtLink>
        </div>
        <div v-else class="empty-state">
          <h2>{{ locale === "zh" ? "暂无资讯" : "No articles yet" }}</h2>
        </div>
      </section>

      <!-- ========== 文章详情页 ========== -->
      <section v-else-if="view.type === 'article' && article" class="section container">
        <article class="article-detail">
          <h2>{{ article.title }}</h2>
          <p v-if="article.body" class="prose muted">{{ article.body }}</p>
          <div v-if="article.sections && article.sections.length" class="detail-sections">
            <div v-for="(sec, idx) in article.sections" :key="idx" class="detail-section">
              <h3>{{ locale === "zh" ? sec.title : sec.titleEn }}</h3>
              <p>{{ locale === "zh" ? sec.body : sec.bodyEn }}</p>
            </div>
          </div>
          <NuxtLink class="button" :to="`/${locale}/articles`">{{ locale === "zh" ? "返回列表" : "Back to articles" }} ←</NuxtLink>
        </article>
      </section>

      <!-- ========== 关于页 ========== -->
      <section v-else-if="view.type === 'about'" class="section container">
        <div class="about-panel">
          <div class="about-visual">
            <img src="/images/lb/block_2.webp" :alt="t.companyTitle" /><span>MATERIALS<br />& APPLICATIONS</span>
          </div>
          <div>
            <p class="eyebrow">OUR APPROACH</p>
            <h2>{{ t.companyTitle }}</h2>
            <p class="prose muted">{{ companyIntro }}</p>
          </div>
        </div>
        <div class="article-grid section">
          <div v-for="(item, i) in t.advantages" :key="item" class="article-card">
            <span class="number">0{{ i + 1 }}</span>
            <h3>{{ item }}</h3>
            <p>{{ t.advantageText[i] }}</p>
          </div>
        </div>
      </section>

      <!-- ========== 品牌页 ========== -->
      <section v-else-if="view.type === 'brands'" class="section container">
        <h2>{{ t.brandsTitle }}</h2>
        <img class="brands-image" :src="partnersUrl" :alt="t.brandsTitle" width="1100" height="340" loading="lazy" />
        <p class="notice">{{ t.brandsNote }}</p>
        <NuxtLink class="button" :to="`/${locale}/products`">{{ t.browse }} ↗</NuxtLink>
      </section>

      <!-- ========== 联系页 ========== -->
      <section v-else-if="view.type === 'contact'" class="section container">
        <ContactForm :locale="locale" />
      </section>
    </template>

    <!-- ========== CTA ========== -->
    <section v-if="view.type !== 'contact'" class="cta">
      <div class="container">
        <div>
          <p class="eyebrow">FROM MATERIALS TO POSSIBILITIES</p>
          <h2>{{ t.contactTitle }}</h2>
        </div>
        <NuxtLink class="button light" :to="`/${locale}/contact`">{{ t.contact }} ↗</NuxtLink>
      </div>
    </section>
  </div>
</template>
