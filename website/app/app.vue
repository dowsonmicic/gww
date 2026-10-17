<script setup lang="ts">
import { text, type Locale } from "~/data/catalog";
const config = useRuntimeConfig();
const route = useRoute();
const locale = computed<Locale>(() =>
  route.params.locale === "en" ? "en" : "zh",
);
const t = computed(() => text[locale.value]);
const menuOpen = ref(false);
const preferred = useCookie<string>("site-locale", {
  sameSite: "lax",
  maxAge: 31536000,
});
const links = [
  "",
  "about",
  "products",
  "brands",
  "news",
  "knowledge",
  "honors",
  "contact",
];
const company = computed(
  () =>
    (locale.value === "en"
      ? config.public.companyNameEn
      : config.public.companyName) || t.value.name,
);
const switchLink = computed(() =>
  route.path.replace(
    /^\/(zh|en)(?=\/|$)/,
    locale.value === "zh" ? "/en" : "/zh",
  ),
);
watch(
  () => route.path,
  () => {
    menuOpen.value = false;
  },
);
useHead(() => ({ htmlAttrs: { lang: locale.value } }));
</script>

<template>
  <div>
    <div class="top-strip">
      <div class="container">
        <span>{{ t.note }}</span
        ><span>{{ t.tagline }}</span>
      </div>
    </div>
    <header class="site-header">
      <div class="container header-inner">
        <NuxtLink :to="`/${locale}`" class="identity" :aria-label="t.nav[0]">
          <span class="brand-mark" aria-hidden="true">M<span>·</span></span>
          <span
            ><strong>{{ company }}</strong
            ><small>{{ t.tagline }}</small></span
          >
        </NuxtLink>
        <button
          class="menu-toggle"
          :aria-label="t.menu"
          :aria-expanded="menuOpen"
          aria-controls="main-nav"
          @click="menuOpen = !menuOpen"
        >
          ☰
        </button>
        <nav
          id="main-nav"
          :class="{ open: menuOpen }"
          :aria-label="locale === 'zh' ? '主导航' : 'Main navigation'"
        >
          <NuxtLink
            v-for="(path, i) in links"
            :key="path"
            :to="`/${locale}${path ? '/' + path : ''}`"
            >{{ t.nav[i] }}</NuxtLink
          >
        </nav>
        <NuxtLink
          class="language"
          :to="switchLink"
          @click="preferred = locale === 'zh' ? 'en' : 'zh'"
          >{{ locale === "zh" ? "English" : "中文" }}
          <span aria-hidden="true">↗</span></NuxtLink
        >
      </div>
    </header>
    <main id="main-content"><NuxtPage /></main>
    <footer class="site-footer">
      <div class="container footer-grid">
        <div>
          <h2>{{ company }}</h2>
          <p>{{ t.tagline }}</p>
          <p class="footer-note">{{ t.note }}</p>
        </div>
        <div>
          <h3>{{ t.nav[2] }}</h3>
          <NuxtLink :to="`/${locale}/products`">POE · DOW</NuxtLink
          ><NuxtLink :to="`/${locale}/brands`">{{ t.nav[3] }}</NuxtLink>
        </div>
        <div>
          <h3>{{ t.contact }}</h3>
          <p>{{ config.public.contactEmail || t.unconfigured }}</p>
          <NuxtLink :to="`/${locale}/contact`">{{ t.contact }} ↗</NuxtLink>
        </div>
      </div>
      <div class="container footer-bottom">
        <span>© {{ new Date().getFullYear() }} {{ company }}</span
        ><span>{{
          locale === "zh"
            ? "一期A步 · 展示网站"
            : "Stage A · Demonstration website"
        }}</span>
      </div>
    </footer>
    <a
      v-if="config.public.supportUrl"
      class="floating-contact"
      :href="String(config.public.supportUrl)"
      target="_blank"
      rel="noopener noreferrer"
      >{{ t.support }} ↗</a
    >
    <NuxtLink v-else class="floating-contact" :to="`/${locale}/contact`"
      >{{ t.contact }} ↗</NuxtLink
    >
  </div>
</template>
