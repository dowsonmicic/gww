<script setup lang="ts">
import { text, type Locale } from "~/data/catalog";
const config = useRuntimeConfig();
const site = await useSiteSettings();
const route = useRoute();
// 后台使用独立界面，不显示官网导航、页脚和浮动客服。
const isAdmin = computed(
  () => route.path === "/admin" || route.path.startsWith("/admin/"),
);
// URL决定当前显示语言；中英文页面共享导航和页脚组件。
const locale = computed<Locale>(() =>
  route.params.locale === "en" ? "en" : "zh",
);
const t = computed(() => text[locale.value]);
const menuOpen = ref(false);
// 记住手动选择的语言一年，供根路径跳转使用；此Cookie不是登录凭证。
const preferred = useCookie<string>("site-locale", {
  sameSite: "lax",
  maxAge: 31536000,
});
const links = ["", "about", "products", "articles", "brands", "contact"];
const company = computed(
  () =>
    (locale.value === "en"
      ? site.value.companyNameEn
      : site.value.companyName) || t.value.name,
);
// 只替换语言前缀，保留当前产品/文章路径。
const switchLink = computed(() =>
  route.path.replace(
    /^\/(zh|en)(?=\/|$)/,
    locale.value === "zh" ? "/en" : "/zh",
  ),
);
// 点击导航后收起手机菜单。
watch(
  () => route.path,
  () => {
    menuOpen.value = false;
  },
);
useHead(() => ({
  htmlAttrs: { lang: locale.value },
  // 全局 Organization schema — 告诉 Google "这个站属于 GWW 公司"
  // 每页都注入一次，和页面级 BreadcrumbList/Product schema 并存
  script: [
    {
      type: "application/ld+json",
      innerHTML: JSON.stringify({
        "@context": "https://schema.org",
        "@type": "Organization",
        name: company.value,
        url: config.public.siteUrl,
        logo: `${config.public.siteUrl}/images/brands/brand_j.png`,
        contactPoint: config.public.contactEmail || config.public.contactPhone ? {
          "@type": "ContactPoint",
          email: config.public.contactEmail || undefined,
          telephone: config.public.contactPhone || undefined,
          contactType: "sales",
        } : undefined,
      }),
    },
  ],
}));
</script>

<template>
  <div v-if="isAdmin">
    <NuxtPage />
  </div>
  <div v-else>
    <div class="top-strip">
      <div class="container">
        <span></span><span>{{ t.tagline }}</span>
      </div>
    </div>
    <header class="site-header">
      <div class="container header-inner">
        <NuxtLink :to="`/${locale}`" class="identity" :aria-label="t.nav[0]">
          <img class="brand-mark" src="/images/brands/brand_j.png" alt="logo" />
          <span><strong>{{ company }}</strong><small>{{ t.tagline }}</small></span>
        </NuxtLink>
        <button class="menu-toggle" :aria-label="t.menu" :aria-expanded="menuOpen" aria-controls="main-nav"
          @click="menuOpen = !menuOpen">
          ☰
        </button>
        <nav id="main-nav" :class="{ open: menuOpen }" :aria-label="locale === 'zh' ? '主导航' : 'Main navigation'">
          <NuxtLink v-for="(path, i) in links" :key="path" :to="`/${locale}${path ? '/' + path : ''}`">{{ t.nav[i] }}
          </NuxtLink>
        </nav>
        <NuxtLink class="language" :to="switchLink" @click="preferred = locale === 'zh' ? 'en' : 'zh'">{{ locale ===
          "zh" ? "English" : "中文" }}
          <span aria-hidden="true">↗</span>
        </NuxtLink>
      </div>
    </header>
    <main id="main-content">
      <NuxtPage />
    </main>
    <footer class="site-footer">
      <div class="container footer-grid">
        <div>
          <h2>{{ company }}</h2>
          <p>{{ t.tagline }}</p>
          <p class="footer-note">{{ t.note }}</p>
        </div>
        <div>
          <h3>{{ t.nav[1] }}</h3>
          <NuxtLink :to="`/${locale}/products`">{{ t.nav[1] }}</NuxtLink>
          <!-- <NuxtLink :to="`/${locale}/brands`">{{ t.nav[3] }}</NuxtLink> -->
        </div>
        <div>
          <h3>{{ t.nav[2] }}</h3>
          <NuxtLink :to="`/${locale}/products`">{{ t.nav[2] }}</NuxtLink>
          <NuxtLink :to="`/${locale}/brands`">{{ t.nav[3] }}</NuxtLink>
        </div>
        <div>
          <h3>{{ t.contact }}</h3>
          <p>{{ t.email }}: {{ site.contactEmail || t.unconfigured }}</p>
          <p>{{ t.phone }}: {{ site.contactPhone || t.unconfigured }}</p>
          <NuxtLink :to="`/${locale}/contact`">{{ t.contact }} ↗</NuxtLink>
        </div>
      </div>
      <div class="container footer-bottom">
        <span>© {{ new Date().getFullYear() }} {{ company }}</span>
        <!-- <span>{{
          locale === "zh"
            ? "展示网站"
            : "Demonstration website"
        }}</span> -->
      </div>
    </footer>
    <a v-if="site.supportUrl" class="floating-contact" :href="site.supportUrl" target="_blank"
      rel="noopener noreferrer">{{ t.support }} ↗</a>
    <NuxtLink v-else class="floating-contact" :to="`/${locale}/contact`">{{ t.contact }} ↗</NuxtLink>
  </div>
</template>
