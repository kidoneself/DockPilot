<template>
  <div class="sidebar">
    <AppMenu />
  </div>
</template>

<script setup lang="ts">
import AppMenu from '@/components/AppMenu.vue'
</script>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style> 