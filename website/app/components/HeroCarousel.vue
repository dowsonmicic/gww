<script setup lang="ts">
import { text, type Locale } from "~/data/catalog";
import { useSiteMedia } from "~/composables/useSiteMedia";
// 自动切换间隔，单位毫秒：3000 = 3秒，6500 = 6.5秒。
const AUTO_PLAY_INTERVAL_MS = 3000;
// 与 carousel-1 至 carousel-3 对应；数量由后端固定返回。
const SLIDE_COUNT = 3;
const props = defineProps<{ locale: Locale }>();
const t = computed(() => text[props.locale]);
// 从 Java /api/public/site-media 读取（已写入 MySQL 或兜底静态）
const siteMedia = await useSiteMedia();
const carouselSlides = computed(() =>
  siteMedia.value.filter((m) => m.group === "carousel")
);
// 图片下标从0开始，模板中图片序号从1开始。
const active = ref(0);
const paused = ref(false);
const hovering = ref(false);
const focused = ref(false);
const reduced = ref(false);
let timer: ReturnType<typeof setInterval> | undefined;
let media: MediaQueryList | undefined;
const onPreference = () => {
    reduced.value = media?.matches || false;
};
onMounted(() => {
    // window只在浏览器中存在，不能在Nuxt服务端渲染阶段访问。
    media = window.matchMedia("(prefers-reduced-motion: reduce)");
    onPreference();
    media.addEventListener("change", onPreference);
    timer = setInterval(() => {
        // 手动暂停、鼠标悬停、键盘聚焦或偏好减少动效时，不自动切换。
        if (!paused.value && !hovering.value && !focused.value && !reduced.value)
            active.value = (active.value + 1) % SLIDE_COUNT;
    }, AUTO_PLAY_INTERVAL_MS);
});
onBeforeUnmount(() => {
    // 离开页面时释放定时器和监听器，避免重复运行或内存泄漏。
    clearInterval(timer);
    media?.removeEventListener("change", onPreference);
});
</script>
<template>
    <section class="hero" :class="{ 'hero-highlighted': hovering || focused }" @mouseenter="hovering = true"
        @mouseleave="hovering = false" @focusin="focused = true" @focusout="focused = false">
        <img v-for="(slide, i) in carouselSlides" v-show="active === i" :key="slide.id" class="hero-image"
            :src="slide.url" :alt="`${t.slide} ${i + 1}`" width="1920" height="519"
            :fetchpriority="i === 1 ? 'high' : 'auto'" :loading="i === 1 ? 'eager' : 'lazy'" />
        <div class="hero-shade" />
        <div class="container hero-content">
            <p class="eyebrow">{{ t.eyebrow }}</p>
            <h1>{{ t.hero }}</h1>
            <p class="hero-intro">{{ t.intro }}</p>
            <NuxtLink class="button" :to="`/${locale}/products`">{{ t.browse }} <span>↗</span></NuxtLink>
        </div>
        <div class="container carousel-controls">
            <button v-for="i in SLIDE_COUNT" :key="i" :class="{ active: active === i - 1 }"
                :aria-label="`${t.slide} ${i}`" :aria-pressed="active === i - 1" @click="active = i - 1">
                {{ String(i).padStart(2, "0") }}
            </button>
            <button :aria-label="paused ? t.play : t.pause" :aria-pressed="paused" @click="paused = !paused">
                {{ paused ? "▶" : "Ⅱ" }}
            </button>
        </div>
    </section>
</template>

<style scoped>
/* 叠加内侧边缘效果，不改变图片尺寸，也不遮挡鼠标点击。 */
.hero::after {
    content: "";
    position: absolute;
    inset: 0;
    z-index: 3;
    pointer-events: none;
    border: 3px solid #d5e59c;
    box-shadow: inset 0 0 22px 4px rgb(213 229 156 / 45%);
    opacity: 0;
    transition: opacity 180ms ease;
}

.hero-highlighted::after {
    opacity: 1;
}

@media (prefers-reduced-motion: reduce) {
    .hero::after {
        transition: none;
    }
}
</style>
