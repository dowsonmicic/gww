<script setup lang="ts">
import { text } from "~/data/catalog";
defineProps<{ error: { statusCode: number } }>();
const t = computed(() => text[useRoute().path.startsWith("/en") ? "en" : "zh"]);
</script>
<template>
  <main class="section container empty-state">
    <p class="eyebrow">{{ error.statusCode }}</p>
    <h1>{{ t.notFound }}</h1>
    <p>{{ t.notFoundText }}</p>
    <button
      class="button"
      @click="clearError({ redirect: t === text.en ? '/en' : '/zh' })"
    >
      {{ t.home }}
    </button>
  </main>
</template>
