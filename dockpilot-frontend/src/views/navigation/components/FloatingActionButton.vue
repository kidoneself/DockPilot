<template>
  <div class="fab-container">
    <n-button
      type="primary"
      circle
      size="large"
      class="fab-main"
      @click="showFabMenu = !showFabMenu"
    >
      <n-icon :component="showFabMenu ? CloseOutline : MenuOutline" />
    </n-button>
    
    <transition-group name="fab" tag="div" class="fab-menu">
      <n-button
        v-for="(action, index) in fabActions"
        v-show="showFabMenu"
        :key="action.name"
        :type="action.type"
        circle
        class="fab-item"
        :style="{ '--delay': index * 0.1 + 's' }"
        @click="handleFabAction(action)"
      >
        <n-icon :component="action.icon" />
      </n-button>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { ref, markRaw } from 'vue'
import {
  SettingsOutline,
  AddOutline,
  MenuOutline,
  CloseOutline,
  RefreshOutline
} from '@vicons/ionicons5'

// Emits
const emit = defineEmits<{
  'action': [actionName: string]
}>()

// 界面状态
const showFabMenu = ref(false)

// 浮动操作按钮
const fabActions = ref([
  { name: 'refresh', icon: markRaw(RefreshOutline), type: 'info' },
  { name: 'add', icon: markRaw(AddOutline), type: 'warning' },
  { name: 'settings', icon: markRaw(SettingsOutline), type: 'default' }
])

// 处理操作按钮点击
const handleFabAction = (action: any) => {
  showFabMenu.value = false
  emit('action', action.name)
}
</script>

<style scoped>
/* 浮动操作按钮 */
.fab-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 100;
}

.fab-main {
  width: 50px;
  height: 50px;
  box-shadow: 0 6px 24px rgba(59, 130, 246, 0.3);
}

.fab-menu {
  position: absolute;
  bottom: 60px;
  right: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fab-item {
  width: 42px;
  height: 42px;
  animation: fabIn 0.3s ease var(--delay) both;
}

/* 动画 */
.fab-enter-active,
.fab-leave-active {
  transition: all 0.3s ease;
}

.fab-enter-from,
.fab-leave-to {
  opacity: 0;
  transform: scale(0.5) translateX(20px);
}

@keyframes fabIn {
  from {
    opacity: 0;
    transform: scale(0.5) translateY(20px);
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
}
</style> 