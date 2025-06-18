<template>
  <div class="app-header">
    <div class="app-basic-info">
      <img :src="app?.icon" :alt="app?.name" class="app-icon" />
      <div class="app-info">
        <h1>{{ app?.name }}</h1>
        <p class="app-desc">{{ app?.description }}</p>
        <div class="app-meta">
          <n-tag :type="app?.type === '官方应用' ? 'success' : 'info'" size="small">
            {{ app?.type }}
          </n-tag>
          <span class="meta-text">{{ app?.deployCount }}人安装过</span>
        </div>
      </div>
      <div class="header-actions">
        <n-tag v-if="!allImagesReady" type="warning" size="small">
          <template #icon><n-icon><DownloadOutline /></n-icon></template>
          需要拉取镜像
        </n-tag>
        <n-tag v-else type="success" size="small">
          <template #icon><n-icon><CheckmarkCircleOutline /></n-icon></template>
          准备就绪
        </n-tag>
        <n-button quaternary @click="$emit('back')">返回</n-button>
        <n-button 
          type="primary" 
       
          @click="$emit('install')"
        >
          立即安装
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DownloadOutline, CheckmarkCircleOutline } from '@vicons/ionicons5'
import type { ApplicationInstallInfo } from '@/api/http/applications'

// 组件属性定义
interface Props {
  app: ApplicationInstallInfo['app'] | null
  allImagesReady: boolean
}

// 组件事件定义
interface Emits {
  back: []
  install: []
}

// 定义props和emits
defineProps<Props>()
defineEmits<Emits>()
</script>

<style scoped>
.app-header {
  margin-bottom: 24px;
}

.app-basic-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: var(--card-color);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 深色模式下的增强效果 */
[data-theme="dark"] .app-basic-info {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.05);
}

.app-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: contain;
  flex-shrink: 0;
}

.app-info {
  flex: 1;
  min-width: 0;
}

.app-info h1 {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-color-1);
}

.app-desc {
  margin: 0 0 8px 0;
  color: var(--text-color-2);
  font-size: 14px;
  line-height: 1.4;
}

.app-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.meta-text {
  color: var(--text-color-3);
  font-size: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .app-basic-info {
    flex-direction: column;
    text-align: center;
    gap: 12px;
    padding: 16px;
  }

  .header-actions {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 640px) {
  .app-basic-info {
    padding: 12px;
  }

  .header-actions {
    flex-direction: column;
    gap: 8px;
  }
}
</style> 