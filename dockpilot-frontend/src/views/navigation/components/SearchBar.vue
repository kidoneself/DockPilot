<template>
  <div class="search-section">
    <div class="search-container">
      <n-input
        v-model:value="searchQuery"
        size="large"
        round
        placeholder="搜索应用、容器或服务..."
        clearable
        @update:value="handleSearchUpdate"
      >
        <template #prefix>
          <n-icon :component="SearchOutline" />
        </template>
      </n-input>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { SearchOutline } from '@vicons/ionicons5'

// Props
interface Props {
  modelValue?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: ''
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// 内部搜索值
const searchQuery = ref(props.modelValue)

// 处理搜索更新
const handleSearchUpdate = (value: string) => {
  searchQuery.value = value
  emit('update:modelValue', value)
}

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  searchQuery.value = newValue
})
</script>

<style scoped>
.search-section {
  margin-bottom: 20px;
  width: 100%;
  display: flex;
  justify-content: center;
}

.search-container {
  max-width: 500px;
  width: 100%;
}

.search-container :deep(.n-input) {
  background: transparent;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.search-container :deep(.n-input__input-el) {
  background: transparent;
  color: #f8fafc;
  font-size: 14px;
}

.search-container :deep(.n-input__placeholder) {
  color: #64748b;
}

.search-container :deep(.n-input):hover {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.search-container :deep(.n-input--focus) {
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(59, 130, 246, 0.3) !important;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1) !important;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .search-container {
    max-width: 100%;
  }
}
</style> 