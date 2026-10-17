<script setup lang="ts">
import {
  text,
  locales,
  products,
  articles,
  filterProducts,
  resolvePath,
  type Locale,
} from "~/data/catalog";
definePageMeta({
  validate: (route) => locales.includes(route.params.locale as Locale),
});
const route = useRoute();
const config = useRuntimeConfig();
const locale = computed(() => route.params.locale as Locale);
const t = computed(() => text[locale.value]);
const view = computed(() => {
  const slug = route.params.slug;
  return resolvePath(Array.isArray(slug) ? slug : slug ? [String(slug)] : []);
});
if (!view.value)
  throw createError({ statusCode: 404, statusMessage: "Page not found" });
watch(view, (value) => {
  if (!value) showError({ statusCode: 404, statusMessage: "Page not found" });
});
const query = ref("");
const category = ref("");
const brand = ref("");
const page = ref(1);
const filtered = computed(() =>
  filterProducts(query.value, category.value, brand.value),
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
const product = computed(() => products.find((p) => p.id === view.value?.id));
const article = computed(() => articles.find((a) => a.id === view.value?.id));
const listing = computed(() =>
  articles.filter((a) => a.kind === view.value?.type),
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
    knowledge: t.value.nav[5],
    honors: t.value.nav[6],
    contact: t.value.nav[7],
  };
  return names[view.value?.type || "home"];
});
useHead(() => ({
  title: `${title.value} | ${locale.value === "en" ? config.public.companyNameEn || t.value.name : config.public.companyName || t.value.name}`,
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
    <template v-if="view.type === 'home'">
      <HeroCarousel :locale="locale" />
      <section class="section brands-home container">
        <div class="section-heading">
          <div>
            <p class="eyebrow">MATERIAL BRANDS</p>
            <h2>{{ t.brandsTitle }}</h2>
          </div>
          <NuxtLink class="text-link" :to="`/${locale}/brands`"
            >{{ t.detail }} ↗</NuxtLink
          >
        </div>
        <img
          class="brands-image"
          src="/images/brands/brands.webp"
          :alt="t.brandsTitle"
          width="1100"
          height="340"
          loading="lazy"
        />
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
            <NuxtLink class="text-link" :to="`/${locale}/products`"
              >{{ t.browse }} ↗</NuxtLink
            >
          </div>
          <div class="product-grid">
            <ProductCard
              v-for="item in products"
              :key="item.id"
              :product="item"
              :locale="locale"
            />
          </div>
        </div>
      </section>
      <section class="section container about-panel">
        <div class="about-visual">
          <img
            src="/images/lb/block_3.webp"
            :alt="t.companyTitle"
            loading="lazy"
          /><span>SPECIALTY<br />MATERIALS</span>
        </div>
        <div>
          <p class="eyebrow">ABOUT THE COMPANY</p>
          <h2>{{ t.companyTitle }}</h2>
          <p class="muted prose">{{ t.companyText }}</p>
          <NuxtLink class="button" :to="`/${locale}/about`"
            >{{ t.detail }} ↗</NuxtLink
          >
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
            <p class="eyebrow">NEWS & INSIGHTS</p>
            <h2>{{ t.news }}</h2>
          </div>
          <NuxtLink class="text-link" :to="`/${locale}/news`"
            >{{ t.detail }} ↗</NuxtLink
          >
        </div>
        <div class="article-grid">
          <NuxtLink
            v-for="item in articles"
            :key="item.id"
            class="article-card"
            :to="`/${locale}/${item.kind}/${item.id}`"
            ><span class="badge">{{ t.sample }}</span>
            <h3>{{ item.title[locale] }}</h3>
            <p>{{ item.summary[locale] }}</p>
            <span class="text-link">{{ t.read }} ↗</span></NuxtLink
          >
        </div>
      </section>
    </template>
    <template v-else>
      <section class="page-banner">
        <div class="container">
          <div class="breadcrumb">
            <NuxtLink :to="`/${locale}`">{{ t.nav[0] }}</NuxtLink
            ><span>/</span><span>{{ title }}</span>
          </div>
          <p class="eyebrow">SPECIALTY MATERIALS</p>
          <h1>{{ title }}</h1>
        </div>
      </section>
      <section
        v-if="view.type === 'products'"
        class="section container catalog-layout"
      >
        <aside class="filters">
          <h2>{{ t.nav[2] }}</h2>
          <label for="category">{{ t.category }}</label
          ><select id="category" v-model="category">
            <option value="">{{ t.all }}</option>
            <option value="POE">POE</option></select
          ><label for="brand">{{ t.brand }}</label
          ><select id="brand" v-model="brand">
            <option value="">{{ t.all }}</option>
            <option value="DOW">DOW</option></select
          ><button class="text-link" @click="reset">{{ t.reset }}</button>
          <p class="small muted">{{ t.featuredSub }}</p>
        </aside>
        <div>
          <div class="search-row">
            <label class="sr-only" for="search">{{ t.search }}</label
            ><input
              id="search"
              v-model="query"
              type="search"
              :placeholder="t.search"
            /><span>{{ filtered.length }} {{ t.results }}</span>
          </div>
          <div v-if="pageProducts.length" class="product-grid catalog-grid">
            <ProductCard
              v-for="item in pageProducts"
              :key="item.id"
              :product="item"
              :locale="locale"
            />
          </div>
          <div v-else class="empty-state">
            <h2>{{ t.empty }}</h2>
            <button class="button" @click="reset">{{ t.reset }}</button>
          </div>
          <nav
            v-if="pageCount > 1"
            class="pagination"
            :aria-label="locale === 'zh' ? '产品分页' : 'Product pagination'"
          >
            <button :disabled="page === 1" @click="page--">
              {{ t.previous }}</button
            ><button
              v-for="i in pageCount"
              :key="i"
              :class="{ active: page === i }"
              :aria-current="page === i ? 'page' : undefined"
              @click="page = i"
            >
              {{ i }}</button
            ><button :disabled="page === pageCount" @click="page++">
              {{ t.next }}
            </button>
          </nav>
        </div>
      </section>
      <section
        v-else-if="view.type === 'product' && product"
        class="section container"
      >
        <div class="product-detail">
          <img
            :src="`/images/show/${product.image}`"
            :alt="title"
            width="300"
            height="300"
          />
          <div>
            <span class="badge">{{ t.sample }}</span>
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
            <p class="muted">{{ t.featuredSub }}</p>
            <NuxtLink class="button" :to="`/${locale}/contact`"
              >{{ t.contact }} ↗</NuxtLink
            >
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
            <img src="/images/lb/block_2.webp" :alt="t.companyTitle" /><span
              >MATERIALS<br />& APPLICATIONS</span
            >
          </div>
          <div>
            <p class="eyebrow">OUR APPROACH</p>
            <h2>{{ t.companyTitle }}</h2>
            <p class="prose muted">{{ t.companyText }}</p>
          </div>
        </div>
        <div class="article-grid section">
          <div
            v-for="(item, i) in t.advantages"
            :key="item"
            class="article-card"
          >
            <span class="number">0{{ i + 1 }}</span>
            <h3>{{ item }}</h3>
            <p>{{ t.advantageText[i] }}</p>
          </div>
        </div>
      </section>
      <section v-else-if="view.type === 'brands'" class="section container">
        <h2>{{ t.brandsTitle }}</h2>
        <img
          class="brands-image"
          src="/images/brands/brands.webp"
          :alt="t.brandsTitle"
          width="1100"
          height="340"
        />
        <p class="notice">{{ t.brandsNote }}</p>
        <NuxtLink class="button" :to="`/${locale}/products`"
          >{{ t.browse }} ↗</NuxtLink
        >
      </section>
      <section
        v-else-if="['news', 'knowledge'].includes(view.type)"
        class="section container"
      >
        <div class="article-grid">
          <NuxtLink
            v-for="item in listing"
            :key="item.id"
            class="article-card"
            :to="`/${locale}/${item.kind}/${item.id}`"
            ><span class="badge">{{ t.sample }}</span>
            <h2>{{ item.title[locale] }}</h2>
            <p>{{ item.summary[locale] }}</p>
            <span class="text-link">{{ t.read }} ↗</span></NuxtLink
          >
        </div>
      </section>
      <article
        v-else-if="view.type === 'article' && article"
        class="section container article-body"
      >
        <span class="badge">{{ t.sample }}</span>
        <p class="lead">{{ article.summary[locale] }}</p>
        <p v-for="paragraph in article.paragraphs[locale]" :key="paragraph">
          {{ paragraph }}
        </p>
        <NuxtLink class="text-link" :to="`/${locale}/${article.kind}`"
          >← {{ locale === "zh" ? "返回列表" : "Back to list" }}</NuxtLink
        >
      </article>
      <section
        v-else-if="view.type === 'honors'"
        class="section container empty-state"
      >
        <span class="number">DOCUMENTS</span>
        <h2>{{ t.pending }}</h2>
        <p>{{ t.honorsText }}</p>
      </section>
      <section v-else-if="view.type === 'contact'" class="section container">
        <div class="contact-panel">
          <div>
            <p class="eyebrow">LET'S CONNECT</p>
            <h2>{{ t.contactTitle }}</h2>
            <p class="prose muted">{{ t.contactText }}</p>
          </div>
          <div class="contact-details">
            <div>
              <span>{{ t.email }}</span
              ><a
                v-if="config.public.contactEmail"
                :href="`mailto:${config.public.contactEmail}`"
                >{{ config.public.contactEmail }}</a
              ><strong v-else>{{ t.unconfigured }}</strong>
            </div>
            <div>
              <span>{{ t.phone }}</span
              ><a
                v-if="config.public.contactPhone"
                :href="`tel:${config.public.contactPhone}`"
                >{{ config.public.contactPhone }}</a
              ><strong v-else>{{ t.unconfigured }}</strong>
            </div>
            <a
              v-if="config.public.supportUrl"
              class="button"
              :href="String(config.public.supportUrl)"
              target="_blank"
              rel="noopener noreferrer"
              >{{ t.support }} ↗</a
            >
          </div>
        </div>
      </section>
    </template>
    <section v-if="view.type !== 'contact'" class="cta">
      <div class="container">
        <div>
          <p class="eyebrow">FROM MATERIALS TO POSSIBILITIES</p>
          <h2>{{ t.contactTitle }}</h2>
        </div>
        <NuxtLink class="button light" :to="`/${locale}/contact`"
          >{{ t.contact }} ↗</NuxtLink
        >
      </div>
    </section>
  </div>
</template>
