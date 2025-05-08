<template>
  <div class="fixed-buttons">
    <div class="button-group">
      <t-tooltip content="添加应用" placement="left">
        <t-button theme="primary" @click="$emit('addApp')" class="action-btn">
          <template #icon>
            <t-icon name="add" />
          </template>
        </t-button>
      </t-tooltip>
      <t-tooltip content="进入后台" placement="left">
        <t-button theme="primary" @click="$emit('openSettings')" class="action-btn">
          <template #icon>
            <t-icon name="setting" />
          </template>
        </t-button>
      </t-tooltip>
      <t-tooltip :content="currentTab === 'dashboard' ? '应用' : '仪表盘'" placement="left">
        <t-button 
          theme="primary" 
          @click="$emit('toggleView')" 
          class="action-btn view-btn"
          :class="{ 'is-dashboard': currentTab === 'dashboard' }"
        >
          <template #icon>
            <t-icon :name="currentTab === 'dashboard' ? 'app' : 'dashboard'" />
          </template>
        </t-button>
      </t-tooltip>
      <t-tooltip :content="isInternalNetwork ? '当前：内网模式' : '当前：外网模式'" placement="left">
        <t-button 
          theme="primary" 
          @click="$emit('toggleNetworkMode')" 
          class="action-btn network-btn"
          :class="{ 'is-external': !isInternalNetwork }"
        >
          <template #icon>
            <t-icon :name="isInternalNetwork ? 'cloud-download' : 'cloud-upload'" />
          </template>
        </t-button>
      </t-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue';

// 定义组件属性
const props = defineProps({
  currentTab: {
    type: String,
    required: true
  },
  isInternalNetwork: {
    type: Boolean,
    default: true
  }
});

// 定义组件事件
defineEmits(['addApp', 'openSettings', 'toggleView', 'toggleNetworkMode']);
</script>

<style scoped>
.fixed-buttons {
  position: fixed;
  right: 2rem;
  bottom: 2rem;
  z-index: 10;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.action-btn {
  width: 2.8rem !important;
  height: 2.8rem !important;
  border-radius: 12px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease !important;
  margin: 0 !important;
  cursor: pointer !important;
}

.action-btn:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.15) !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-btn :deep(.t-icon) {
  font-size: 1.3rem;
  color: #fff;
}

.view-btn {
  background: rgba(59, 130, 246, 0.2) !important;
  border-color: rgba(59, 130, 246, 0.3) !important;
}

.view-btn.is-dashboard {
  background: rgba(16, 185, 129, 0.2) !important;
  border-color: rgba(16, 185, 129, 0.3) !important;
}

.view-btn:hover {
  background: rgba(59, 130, 246, 0.3) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
}

.view-btn.is-dashboard:hover {
  background: rgba(16, 185, 129, 0.3) !important;
  border-color: rgba(16, 185, 129, 0.4) !important;
}

.network-btn {
  background: rgba(59, 130, 246, 0.2) !important;
  border-color: rgba(59, 130, 246, 0.3) !important;
}

.network-btn.is-external {
  background: rgba(16, 185, 129, 0.2) !important;
  border-color: rgba(16, 185, 129, 0.3) !important;
}

.network-btn:hover {
  background: rgba(59, 130, 246, 0.3) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
}

.network-btn.is-external:hover {
  background: rgba(16, 185, 129, 0.3) !important;
  border-color: rgba(16, 185, 129, 0.4) !important;
}

/* 视图切换按钮动画 */
.view-btn {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.view-btn:hover {
  transform: translateY(-2px) rotate(5deg);
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .fixed-buttons {
    right: 1rem;
    bottom: 1rem;
  }
  
  .action-btn {
    width: 2.5rem !important;
    height: 2.5rem !important;
  }
}

@media screen and (max-width: 480px) {
  .fixed-buttons {
    right: 0.5rem;
    bottom: 0.5rem;
  }
  
  .action-btn {
    width: 2.2rem !important;
    height: 2.2rem !important;
  }
}
</style> 