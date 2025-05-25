<template>
  <n-card class="feature-card" :segmented="{ content: true, footer: 'soft' }">
    <template #header>
      <div class="header-row">
        <span class="feature-title">{{ title }}</span>
      </div>
    </template>
    <div class="feature-desc">
      <slot name="desc">{{ description }}</slot>
    </div>
    <template #footer>
      <n-space justify="end">
        <slot name="actions">
          <n-button size="small" @click="onConfig">配置</n-button>
        </slot>
      </n-space>
    </template>
  </n-card>
</template>

<script setup lang="ts">
const props = defineProps({
  title: String,
  description: String
})

const emit = defineEmits(['config'])

const onConfig = () => {
  emit('config')
}
</script>

<style scoped>
.feature-card {
  width: 100%;
  margin: 0;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.feature-title {
  font-weight: bold;
  font-size: 1.1rem;
}
.feature-desc {
  margin: 12px 0 0 0;
  color: #666;
  min-height: 40px;
}
</style> 