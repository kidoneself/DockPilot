<template>
  <div class="icon-selector">
    <!-- 图标预览和选择 -->
    <div class="selector-content" @click="openIconPicker">
      <div class="icon-display" :class="{ 'has-icon': displayIconUrl, 'error': imageError }">
        <img 
          v-if="displayIconUrl && !imageError" 
          :src="displayIconUrl" 
          class="icon-img" 
          @error="handleImageError"
          @load="imageError = false"
        />
        <div v-else class="icon-placeholder">
          <n-icon :size="24">
            <ImageOutline />
          </n-icon>
          <span>{{ imageError ? '图标加载失败' : '选择图标' }}</span>
        </div>
      </div>
      
      <div class="selector-info">
        <div class="selected-name">
          {{ selectedIconName || '点击选择图标' }}
        </div>
        <n-icon :size="16" class="arrow-icon">
          <ChevronForwardOutline />
        </n-icon>
      </div>
    </div>
    
    <!-- 图标选择器弹窗 -->
    <IconPickerDialog 
      v-model:visible="showPicker"
      @select="onIconSelect"
      :current="selectedIconName"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NIcon } from 'naive-ui'
import { ImageOutline, ChevronForwardOutline } from '@vicons/ionicons5'
import IconPickerDialog from '@/components/IconPickerDialog.vue'

interface Props {
  modelValue?: string  // 当前选中的图标名称
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'change', iconName: string): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: ''
})

const emit = defineEmits<Emits>()

// 响应式数据
const selectedIconName = ref('')
const showPicker = ref(false)
const imageError = ref(false)

// 计算属性
const displayIconUrl = computed(() => {
  return selectedIconName.value ? `/api/icons/${selectedIconName.value}` : ''
})

// 监听props变化
watch(() => props.modelValue, (newVal) => {
  selectedIconName.value = newVal || ''
  imageError.value = false
}, { immediate: true })

// 方法
const onIconSelect = (iconName: string) => {
  selectedIconName.value = iconName
  imageError.value = false
  emitValue()
}

const openIconPicker = () => {
  showPicker.value = true
}

const emitValue = () => {
  emit('update:modelValue', selectedIconName.value)
  emit('change', selectedIconName.value)
}

const handleImageError = () => {
  imageError.value = true
}
</script>

<style scoped>
.icon-selector {
  border: 1px solid var(--n-border-color);
  border-radius: var(--n-border-radius);
  background: var(--n-color);
  overflow: hidden;
  transition: all 0.2s ease;
}

.selector-content {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.selector-content:hover {
  background: var(--n-color-hover);
}

.icon-display {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border: 2px dashed var(--n-border-color);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  margin-right: 12px;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.icon-display.has-icon {
  border-color: var(--n-color-primary);
  background: var(--n-color);
}

.icon-display.error {
  border-color: var(--n-color-error);
  background: rgba(var(--n-color-error-rgb), 0.1);
}

.icon-img {
  max-width: 44px;
  max-height: 44px;
  object-fit: contain;
  border-radius: 6px;
}

.icon-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: var(--n-text-color-placeholder);
  font-size: 10px;
  gap: 2px;
}

.selector-info {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.selected-name {
  color: var(--n-text-color);
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  color: var(--n-text-color-placeholder);
  margin-left: 8px;
  flex-shrink: 0;
}
</style> 