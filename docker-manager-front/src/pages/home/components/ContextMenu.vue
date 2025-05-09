<template>
  <div 
    v-show="visible" 
    class="context-menu"
    :style="menuStyle"
  >
    <!-- 应用卡片菜单 -->
    <template v-if="contextItem">
      <div class="context-menu-item" @click="handleOpenApp('internal')">
        <t-icon name="cloud-download" />
        <span>打开内网</span>
      </div>
      <div class="context-menu-item" @click="handleOpenApp('external')">
        <t-icon name="cloud-upload" />
        <span>打开外网</span>
      </div>
      <div class="context-menu-divider"></div>
      <div class="context-menu-item" @click="handleMenuClick('edit')">
        <t-icon name="edit" />
        <span>编辑</span>
      </div>
    </template>
    
    <!-- 页面菜单 -->
    <template v-else>
      <div class="context-menu-item" @click="handleMenuClick('refresh')">
        <t-icon name="refresh" />
        <span>刷新</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue';
import { useHomeStore } from '@/store/modules/home';
import type { AppItem } from '@/store/modules/home';

// 定义组件属性
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  menuStyle: {
    type: Object,
    default: () => ({})
  },
  contextItem: {
    type: Object as () => AppItem | null,
    default: null
  }
});

// 初始化 store
const homeStore = useHomeStore();

// 处理打开应用
function handleOpenApp(type: 'internal' | 'external') {
  if (!props.contextItem) return;
  homeStore.handleOpenAppFromMenu({ type, item: props.contextItem as AppItem });
}

// 处理菜单点击
function handleMenuClick(action: string) {
  homeStore.handleContextMenuClick({ action, item: props.contextItem as AppItem | null });
}
</script>

<style scoped>
.context-menu {
  position: fixed;
  background: rgba(20, 20, 40, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  padding: 4px;
  min-width: 120px;
  z-index: 1000;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  color: #fff;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.context-menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.context-menu-item .t-icon {
  font-size: 16px;
}

.context-menu-item span {
  font-size: 14px;
}

.context-menu-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 4px 0;
}

/* 响应式布局调整 */
@media screen and (max-width: 480px) {
  .context-menu {
    min-width: 160px;
  }
  
  .context-menu-item {
    padding: 10px 12px;
  }
}
</style> 