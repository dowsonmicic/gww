<script setup lang="ts">
import { text, products, type Locale } from "~/data/catalog";
defineProps<{ product: (typeof products)[number]; locale: Locale }>();
</script>
<template>
  <NuxtLink class="product-card" :to="`/${locale}/products/${product.id}`">
    <div class="product-image">
      <img
        :src="`/images/show/${product.image}`"
        :alt="`${product.category} ${product.model}`"
        width="300"
        height="300"
        loading="lazy"
      /><span>{{ product.category }}</span>
    </div>
    <div class="product-info">
      <p>
        {{ product.brand }} <span>{{ text[locale].sample }}</span>
      </p>
      <h3>{{ product.category }} {{ product.model }}</h3>
      <div>{{ text[locale].detail }} <span>↗</span></div>
    </div>
  </NuxtLink>
</template>
