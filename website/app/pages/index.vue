<script setup lang="ts">
const config = useRuntimeConfig();
const site = await useSiteSettings();
const preferred = useCookie<string>("site-locale");
// 根路径优先使用有效的语言记忆，否则采用启动时配置的默认语言。
const locale = ["zh", "en"].includes(preferred.value || "")
  ? preferred.value
  : site.value.defaultLocale;
// 临时重定向，避免浏览器永久记住默认语言；具体语言URL不受此页面影响。
await navigateTo(`/${locale}`, { redirectCode: 302 });
</script>
<template><div /></template>
