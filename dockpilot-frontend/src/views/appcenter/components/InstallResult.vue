<template>
  <div class="install-result">
    <!-- 安装成功结果 -->
    <n-result status="success" title="安装成功" description="应用已成功部署并启动">
      <template #footer>
        <div class="result-actions">
          <n-button @click="$emit('goContainers')">管理容器</n-button>
          <n-button @click="$emit('installAnother')">安装其他应用</n-button>
          <n-button v-if="accessUrls.length > 0" type="primary" @click="$emit('openApp')">
            立即使用
          </n-button>
        </div>
      </template>
    </n-result>
    
    <!-- 访问信息 -->
    <div v-if="accessUrls.length > 0" class="access-info">
      <h4>访问地址</h4>
      <div class="access-list">
        <div v-for="access in accessUrls" :key="access.name" class="access-item">
          <span class="access-name">{{ access.name }}</span>
          <n-button 
            text 
            type="primary"
            @click="$emit('openUrl', access.url)"
          >
            {{ access.url }}
          </n-button>
        </div>
      </div>
    </div>

    <!-- 部署详情 -->
    <div v-if="result" class="deploy-details">
      <h4>部署详情</h4>
      <div class="details-grid">
        <div class="detail-item">
          <span class="detail-label">部署ID</span>
          <span class="detail-value">{{ result.deployId }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">容器数量</span>
          <span class="detail-value">{{ result.containerIds?.length || 0 }} 个</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">部署状态</span>
          <n-tag :type="result.success ? 'success' : 'error'" size="small">
            {{ result.success ? '成功' : '失败' }}
          </n-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ApplicationDeployResult, AccessUrl } from '@/api/http/applications'

// 组件属性定义
interface Props {
  result: ApplicationDeployResult | null
  accessUrls: AccessUrl[]
}

// 组件事件定义
interface Emits {
  goContainers: []
  installAnother: []
  openApp: []
  openUrl: [url: string]
}

// 定义props和emits
defineProps<Props>()
defineEmits<Emits>()
</script>

<style scoped>
.install-result {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.access-info,
.deploy-details {
  padding: 20px;
  background: var(--card-color);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.access-info h4,
.deploy-details h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color-1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.access-info h4::before {
  content: "🔗";
  font-size: 18px;
}

.deploy-details h4::before {
  content: "📋";
  font-size: 18px;
}

.access-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.access-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  background: var(--card-color-hover);
  border-radius: 8px;
  border: 1px solid var(--border-color);
  transition: all 0.2s ease;
}

.access-item:hover {
  border-color: #3b82f6;
  background: var(--bg-color-2);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.access-name {
  font-weight: 600;
  color: var(--text-color-1);
  flex-shrink: 0;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: var(--card-color-hover);
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.detail-label {
  font-size: 12px;
  color: var(--text-color-3);
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: var(--text-color-1);
  font-weight: 600;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

/* 深色模式增强 */
[data-theme="dark"] .access-info,
[data-theme="dark"] .deploy-details {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

[data-theme="dark"] .access-item,
[data-theme="dark"] .detail-item {
  background: rgba(255, 255, 255, 0.02);
  border-color: rgba(255, 255, 255, 0.06);
}

[data-theme="dark"] .access-item:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .result-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .access-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .details-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .access-info,
  .deploy-details {
    padding: 16px;
  }
  
  .result-actions {
    gap: 8px;
  }
}
</style> 