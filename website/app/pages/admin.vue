<script setup lang="ts">
useHead({ title: "官网管理后台", htmlAttrs: { lang: "zh" } });
</script>
<template>
  <!-- 仅后台加载Element Plus；本机文件预览在浏览器执行。 -->
  <ClientOnly>
    <AdminDashboard />
    <template #fallback>
      <div style="padding: 40px">正在加载管理后台…</div>
    </template>
  </ClientOnly>
</template>