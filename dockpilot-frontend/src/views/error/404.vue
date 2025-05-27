<template>
  <div class="error-page">
    <h1>404</h1>
    <p>页面未找到</p>
  </div>
</template>

<script setup lang="ts">
// 404 Not Found Error Page
</script>

<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'ErrorNotFound'
})
</script>

<style scoped>
.error-page {
  text-align: center;
  padding: 50px;
}
</style>
