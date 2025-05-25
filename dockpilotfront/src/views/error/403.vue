<template>
  <div class="error-page">
    <h1>403</h1>
    <p>禁止访问</p>
  </div>
</template>

<script setup lang="ts">
// 403 Forbidden Error Page
</script>

<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'ErrorForbidden'
})
</script>

<style scoped>
.error-page {
  text-align: center;
  padding: 50px;
}
</style>
