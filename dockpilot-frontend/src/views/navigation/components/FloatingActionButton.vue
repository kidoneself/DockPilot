<template>
  <div class="fab-container">
    <!-- 直接显示两个操作按钮，垂直排列 -->
    <div class="fab-menu">
      <n-button
        v-for="(action, index) in fabActions"
        :key="action.name"
        circle
        class="fab-item"
        :style="{ '--delay': index * 0.1 + 's' }"
        @click="handleFabAction(action)"
      >
        <n-icon :component="action.icon" />
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, markRaw } from 'vue'
import {
  SettingsOutline,
  AddOutline
} from '@vicons/ionicons5'

// Emits
const emit = defineEmits<{
  'action': [actionName: string]
}>()

// 浮动操作按钮（移除刷新按钮）
const fabActions = ref([
  { name: 'add', icon: markRaw(AddOutline) },
  { name: 'settings', icon: markRaw(SettingsOutline) }
])

// 处理操作按钮点击
const handleFabAction = (action: any) => {
  emit('action', action.name)
}
</script>

<style scoped>
/* 浮动操作按钮容器 */
.fab-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 100;
}

/* 按钮菜单 - 始终显示，垂直排列 */
.fab-menu {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

/* 单个按钮样式 - 透明背景效果 */
.fab-item {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.15) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
  animation: fabIn 0.4s ease var(--delay) both;
  color: rgba(255, 255, 255, 0.9) !important;
}

.fab-item:hover {
  background: rgba(255, 255, 255, 0.25) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  color: rgba(255, 255, 255, 1) !important;
}

.fab-item:active {
  transform: translateY(0);
  background: rgba(255, 255, 255, 0.3) !important;
}

/* 深色模式下的样式调整 */
[data-theme="dark"] .fab-item {
  background: rgba(0, 0, 0, 0.3) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

[data-theme="dark"] .fab-item:hover {
  background: rgba(0, 0, 0, 0.4) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: rgba(255, 255, 255, 1) !important;
}

[data-theme="dark"] .fab-item:active {
  background: rgba(0, 0, 0, 0.5) !important;
}

/* 入场动画 */
@keyframes fabIn {
  from {
    opacity: 0;
    transform: scale(0.8) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .fab-container {
    bottom: 16px;
    right: 16px;
  }
  
  .fab-item {
    width: 44px;
    height: 44px;
  }
  
  .fab-menu {
    gap: 10px;
  }
}
</style> 