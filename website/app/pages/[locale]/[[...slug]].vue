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
const t = computed(() => text[locale.value]);
const companyIntro = computed(
  () =>
    (locale.value === "en"
      ? site.value.companyIntroEn
      : site.value.companyIntro) || t.value.companyText,
);
// SSR读取公开API，空数据库保持空列表，故障不回退静态演示记录。
const { data: productResult, error: productError } = await useFetch<{
  data: ContentRow[];
}>("/api/public/content/products");
const { data: articleResult, error: articleError } = await useFetch<{
  data: ContentRow[];
}>("/api/public/content/articles");
if (productError.value || articleError.value) {
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
  })),
);
const articles = computed(() =>
  (articleResult.value?.data || []).map((row) => ({
    id: row.id,
    kind: "news",
    title: { zh: row.title || "", en: row.title || "" },
    summary: { zh: row.description, en: row.descriptionEn || row.description },
    paragraphs: {
      zh: row.description.split("\n"),
      en: (row.descriptionEn || row.description).split("\n"),
    },
  })),
);
const categories = computed(() => [
  ...new Set(products.value.map((p) => p.category).filter(Boolean)),
]);
const brands = computed(() => [
  ...new Set(products.value.map((p) => p.brand).filter(Boolean)),
]);
// 将可选的剩余路径统一转换为数组，再解析页面类型和详情ID。
// /zh/products/8400 -> ["products", "8400"] -> { type: "product", id: "8400" }
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
      parts[0] === "news" &&
      articles.value.some((a) => a.id === parts[1] && a.kind === parts[0])
    )
      return { type: "article", id: parts[1]! };
    return null;
  }
  return resolvePath(parts);
});
// 首次访问无效路径返回404；客户端切换路径时也检查有效性。
if (!view.value)
  throw createError({ statusCode: 404, statusMessage: "Page not found" });
watch(view, (value) => {
  if (!value) showError({ statusCode: 404, statusMessage: "Page not found" });
});
// 筛选条件是响应式状态，输入/选择变化会自动重新计算产品列表。
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
// 先筛选再分页；slice起点包含、终点不包含，每页展示4条。
const pageSize = 4;
const pageCount = computed(() =>
  Math.max(1, Math.ceil(filtered.value.length / pageSize)),
);
const pageProducts = computed(() =>
  filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize),
);
// 防止筛选后仍停留在原来的高页码，导致结果存在却显示空列表。
watch([query, category, brand], () => {
  page.value = 1;
});
// A步从本地数据查详情；B步可替换为后端查询，URL不需要变更。
const product = computed(() =>
  products.value.find((p) => p.id === view.value?.id),
);
const article = computed(() =>
  articles.value.find((a) => a.id === view.value?.id),
);
const listing = computed(() =>
  articles.value.filter((a) => a.kind === view.value?.type),
);
const title = computed(() => {
  if (view.value?.type === "product")
    return `${product.value?.category} ${product.value?.model}`;
  if (view.value?.type === "article")
    return article.value?.title[locale.value] || "";
  const names: Record<string, string> = {
    home: t.value.nav[0],
    about: t.value.nav[1],
    products: t.value.nav[2],
    brands: t.value.nav[3],
    news: t.value.nav[4],
    contact: t.value.nav[5],
  };
  return names[view.value?.type || "home"];
});
// 同步浏览器标签标题和页面描述，服务端渲染时也会输出。
useHead(() => ({
  title: `${title.value} | ${locale.value === "en" ? site.value.companyNameEn || t.value.name : site.value.companyName || t.value.name}`,
  meta: [{ name: "description", content: t.value.intro }],
}));
function reset() {
  query.value = "";
  category.value = "";
  brand.value = "";
  page.value = 1;
}
</script>

<template>
  <div v-if="view">
    <!-- 当前文件统一处理各栏目，view.type决定渲染哪个页面分支。 -->
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
        <img class="brands-image" :src="partnersUrl" :alt="t.brandsTitle" width="1100" height="340"
            loading="lazy" />
          loading="lazy" />
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
            <ProductCard v-for="item in products" :key="item.id" :product="item" :locale="locale" />
          </div>
        </div>
      </section>
      <section class="section container about-panel">
        <div class="about-visual">
          <img src="/images/lb/block_3.webp" :alt="t.companyTitle"
            loading="lazy" /><span>SPECIALTY<br />MATERIALS</span>
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
      <section class="section container">
        <div class="section-heading">
          <div>
            <p class="eyebrow">NEWS</p>
            <h2>{{ t.news }}</h2>
          </div>
          <NuxtLink class="text-link" :to="`/${locale}/news`">{{ t.detail }} ↗</NuxtLink>
        </div>
        <div class="article-grid">
          <NuxtLink v-for="item in articles" :key="item.id" class="article-card"
            :to="`/${locale}/${item.kind}/${item.id}`">
            <h3>{{ item.title[locale] }}</h3>
            <p>{{ item.summary[locale] }}</p>
            <span class="text-link">{{ t.read }} ↗</span>
          </NuxtLink>
        </div>
      </section>
    </template>
    <template v-else>
      <section v-if="view.type !== 'contact'" class="page-banner">
        <div class="container">
          <div class="breadcrumb">
            <NuxtLink :to="`/${locale}`">{{ t.nav[0] }}</NuxtLink><span>/</span><span>{{ title }}</span>
          </div>
          <p class="eyebrow">SPECIALTY MATERIALS</p>
          <h1>{{ title }}</h1>
        </div>
      </section>
      <section v-if="view.type === 'products'" class="section container catalog-layout">
        <!-- 筛选项直接展开，点击更新状态；ProductCard负责展示和跳转。 -->
        <aside class="filters">
          <h2>{{ t.nav[2] }}</h2>
          <div class="filter-group" role="group" :aria-label="t.category">
            <h3>{{ t.category }}</h3>
            <div class="filter-options">
              <button type="button" :class="{ active: category === '' }" :aria-pressed="category === ''"
                @click="category = ''">
                {{ t.all }}
              </button>
              <button v-for="value in categories" :key="value" type="button" :class="{ active: category === value }"
                :aria-pressed="category === value" @click="category = value">
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
              <button v-for="value in brands" :key="value" type="button" :class="{ active: brand === value }"
                :aria-pressed="brand === value" @click="brand = value">
                {{ value }}
              </button>
            </div>
          </div>
          <button class="text-link" @click="reset">{{ t.reset }}</button>
          <p class="small muted">{{ t.featuredSub }}</p>
        </aside>
        <div>
          <div class="search-row">
            <label class="sr-only" for="search">{{ t.search }}</label><input id="search" v-model="query" type="search"
              :placeholder="t.search" /><span>{{ filtered.length }} {{ t.results }}</span>
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
              {{ t.previous }}</button><button v-for="i in pageCount" :key="i" :class="{ active: page === i }"
              :aria-current="page === i ? 'page' : undefined" @click="page = i">
              {{ i }}</button><button :disabled="page === pageCount" @click="page++">
              {{ t.next }}
            </button>
          </nav>
        </div>
      </section>
      <section v-else-if="view.type === 'product' && product" class="section container">
        <div class="product-detail">
          <img v-if="product.image" :src="product.image" :alt="title" width="300" height="300" />
          <div>
            <h2>{{ title }}</h2>
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
          <h2>{{ t.parameters }}</h2>
          <p>{{ t.parametersText }}</p>
          <h2>{{ t.application }}</h2>
          <p>{{ t.applicationText }}</p>
          <p class="notice">{{ t.brandsNote }}</p>
        </div>
      </section>
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
      <section v-else-if="view.type === 'brands'" class="section container">
        <h2>{{ t.brandsTitle }}</h2>
        <img class="brands-image" :src="partnersUrl" :alt="t.brandsTitle" width="1100" height="340"
            loading="lazy" />
        <p class="notice">{{ t.brandsNote }}</p>
        <NuxtLink class="button" :to="`/${locale}/products`">{{ t.browse }} ↗</NuxtLink>
      </section>
      <section v-else-if="view.type === 'news'" class="section container">
        <div class="article-grid">
          <NuxtLink v-for="item in listing" :key="item.id" class="article-card"
            :to="`/${locale}/${item.kind}/${item.id}`">
            <h2>{{ item.title[locale] }}</h2>
            <p>{{ item.summary[locale] }}</p>
            <span class="text-link">{{ t.read }} ↗</span>
          </NuxtLink>
        </div>
      </section>
      <article v-else-if="view.type === 'article' && article" class="section container article-body">
        <p class="lead">{{ article.summary[locale] }}</p>
        <p v-for="paragraph in article.paragraphs[locale]" :key="paragraph">
          {{ paragraph }}
        </p>
        <NuxtLink class="text-link" :to="`/${locale}/${article.kind}`">← {{ locale === "zh" ? "返回列表" : "Back to list" }}
        </NuxtLink>
      </article>
      <section v-else-if="view.type === 'contact'" class="section container">
        <ContactForm :locale="locale" />
      </section>
    </template>
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
