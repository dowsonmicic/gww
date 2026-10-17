<script setup lang="ts">
const config = useRuntimeConfig();
const preferred = useCookie<string>("site-locale");
const locale = ["zh", "en"].includes(preferred.value || "")
  ? preferred.value
  : config.public.defaultLocale;
await navigateTo(`/${locale}`, { redirectCode: 302 });
</script>
<template><div /></template>
