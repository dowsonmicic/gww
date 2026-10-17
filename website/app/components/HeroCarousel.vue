<script setup lang="ts">
import { text, type Locale } from "~/data/catalog";
const props = defineProps<{ locale: Locale }>();
const t = computed(() => text[props.locale]);
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
    media = window.matchMedia("(prefers-reduced-motion: reduce)");
    onPreference();
    media.addEventListener("change", onPreference);
    timer = setInterval(() => {
        if (!paused.value && !hovering.value && !focused.value && !reduced.value)
            active.value = (active.value + 1) % 3;
    }, 6500);
});
onBeforeUnmount(() => {
    clearInterval(timer);
    media?.removeEventListener("change", onPreference);
});
</script>

<template>
    <section class="hero" @mouseenter="hovering = true" @mouseleave="hovering = false" @focusin="focused = true"
        @focusout="focused = false">
        <img v-for="i in 3" v-show="active === i - 1" :key="i" class="hero-image" :src="`/images/lb/block_${i}.webp`"
            :alt="`${t.slide} ${i}`" width="1920" height="519" :fetchpriority="i === 1 ? 'high' : 'auto'"
            :loading="i === 1 ? 'eager' : 'lazy'" />
        <div class="hero-shade" />
        <div class="container hero-content">
            <p class="eyebrow">{{ t.eyebrow }}</p>
            <h1>{{ t.hero }}</h1>
            <p class="hero-intro">{{ t.intro }}</p>
            <NuxtLink class="button" :to="`/${locale}/products`">{{ t.browse }} <span>↗</span></NuxtLink>
        </div>
        <div class="container carousel-controls">
            <button v-for="i in 3" :key="i" :class="{ active: active === i - 1 }" :aria-label="`${t.slide} ${i}`"
                :aria-pressed="active === i - 1" @click="active = i - 1">
                {{ String(i).padStart(2, "0") }}
            </button>
            <button :aria-label="paused ? t.play : t.pause" :aria-pressed="paused" @click="paused = !paused">
                {{ paused ? "▶" : "Ⅱ" }}
            </button>
        </div>
    </section>
</template>
