<script setup lang="ts">
import { text, type Locale } from "~/data/catalog";
defineProps<{
  product: {
    id: string;
    model: string;
    category: string;
    brand: string;
    image: string;
  };
  locale: Locale;
}>();
</script>
<template>
  <NuxtLink class="product-card" :to="`/${locale}/products/${product.id}`">
    <div class="product-image">
      <img
        v-if="product.image"
        :src="product.image"
        :alt="`${product.category} ${product.model}`"
        width="300"
        height="300"
        loading="lazy"
      /><span>{{ product.category }}</span>
    </div>
    <div class="product-info">
      <p>
        {{ product.brand }}
      </p>
      <h3>{{ product.category }} {{ product.model }}</h3>
      <div>{{ text[locale].detail }} <span>↗</span></div>
    </div>
  </NuxtLink>
</template>
