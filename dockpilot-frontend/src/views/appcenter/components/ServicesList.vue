<template>
  <div class="section">
    <!-- 服务组件标题栏 -->
    <div class="section-header expandable" @click="toggleServices">
      <h3>服务组件</h3>
      <n-icon size="20" :class="{ 'rotate-180': !servicesExpanded }">
        <ChevronDownOutline />
      </n-icon>
    </div>
    
    <!-- 服务组件内容 -->
    <div v-show="servicesExpanded" class="services-content">
      <div v-for="service in services" :key="service.name" class="service-item-compact">
        <div class="service-row">
          <!-- 服务基本信息 -->
          <div class="service-basic">
            <span class="service-name">{{ service.name }}</span>
            <span class="service-image">{{ service.image }}</span>
          </div>
          
          <!-- 镜像状态和操作 -->
          <div class="service-actions">
            <!-- 镜像状态标签 -->
            <n-tag 
              :type="getStatusTagType(getImageStatusByName(service.image))"
              size="small"
              :style="{ 
                color: getImageStatusColor(getImageStatusByName(service.image)),
                borderColor: getImageStatusColor(getImageStatusByName(service.image))
              }"
            >
              {{ getImageStatusText(getImageStatusByName(service.image)) }}
              <span v-if="getImageStatusByName(service.image) === 'pulling'" class="progress-text">
                ({{ getImageProgressByName(service.image) }}%)
              </span>
            </n-tag>
            
            <!-- 拉取按钮 -->
            <n-button
              v-if="getImageStatusByName(service.image) === 'missing'"
              size="small"
              type="primary"
              @click="$emit('pullImage', service.image)"
            >
              <template #icon>
                <n-icon><DownloadOutline /></n-icon>
              </template>
              拉取镜像
            </n-button>
            
            <!-- 重试按钮 -->
            <n-button
              v-else-if="getImageStatusByName(service.image) === 'failed'"
              size="small"
              type="error"
              @click="$emit('pullImage', service.image)"
            >
              <template #icon>
                <n-icon><RefreshOutline /></n-icon>
              </template>
              重试
            </n-button>
            
            <!-- 拉取中的进度条 -->
            <div v-else-if="getImageStatusByName(service.image) === 'pulling'" class="pull-progress">
              <n-progress
                type="line"
                :percentage="getImageProgressByName(service.image)"
                :height="6"
                :show-indicator="false"
                style="width: 120px;"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { 
  ChevronDownOutline, 
  DownloadOutline, 
  RefreshOutline 
} from '@vicons/ionicons5'
import type { ServiceInfo, ImageStatusInfo } from '@/api/http/applications'

// 组件属性定义
interface Props {
  services: ServiceInfo[]
  images: ImageStatusInfo[]
}

// 组件事件定义
interface Emits {
  pullImage: [imageName: string]
}

// 定义props和emits
const props = defineProps<Props>()
defineEmits<Emits>()

// 组件状态
const servicesExpanded = ref(true)

// 展开/收起切换
const toggleServices = () => {
  servicesExpanded.value = !servicesExpanded.value
}

// 获取镜像状态相关方法
const getImageStatusByName = (imageName: string) => {
  const image = props.images.find(img => img.name === imageName)
  return image?.status || 'missing'
}

const getImageProgressByName = (imageName: string) => {
  const image = props.images.find(img => img.name === imageName) as any
  return image?.pullStatus?.percentage || 0
}

const getImageStatusColor = (status: string) => {
  switch (status) {
    case 'exists':
    case 'success':
      return '#18a058'
    case 'missing':
      return '#f0a020'
    case 'pulling':
      return '#2080f0'
    case 'failed':
      return '#d03050'
    default:
      return '#909399'
  }
}

const getImageStatusText = (status: string) => {
  switch (status) {
    case 'exists':
      return '已存在'
    case 'missing':
      return '需要拉取'
    case 'pulling':
      return '拉取中'
    case 'success':
      return '拉取成功'
    case 'failed':
      return '拉取失败'
    default:
      return '未知'
  }
}

const getStatusTagType = (status: string): 'default' | 'success' | 'warning' | 'error' | 'info' => {
  switch (status) {
    case 'exists':
    case 'success':
      return 'success'
    case 'missing':
      return 'warning'
    case 'pulling':
      return 'info'
    case 'failed':
      return 'error'
    default:
      return 'default'
  }
}
</script>

<style scoped>
.section {
  background: var(--card-color);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin-bottom: 24px;
}

/* 深色模式下的增强效果 */
[data-theme="dark"] .section {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.05);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--card-color-hover);
}

.section-header.expandable {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.section-header.expandable:hover {
  background: var(--bg-color-2);
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color-1);
}

.section-header .n-icon {
  transition: transform 0.3s ease;
}

.section-header .n-icon.rotate-180 {
  transform: rotate(180deg);
}

.services-content {
  padding: 20px 24px;
}

.service-item-compact {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 16px;
  background: var(--card-color-hover);
  border-radius: 8px;
  border: 1px solid var(--border-color);
  transition: all 0.2s ease;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.service-item-compact:hover {
  border-color: #3b82f6;
  background: var(--bg-color-2);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.service-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.service-basic {
  flex: 1;
  min-width: 0;
}

.service-name {
  display: block;
  font-weight: 600;
  color: var(--text-color-1);
  margin-bottom: 4px;
}

.service-image {
  display: block;
  font-size: 12px;
  color: var(--text-color-3);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.service-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.progress-text {
  font-size: 11px;
  opacity: 0.8;
}

.pull-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 暗黑模式下的服务项增强效果 */
[data-theme="dark"] .service-item-compact {
  background: rgba(255, 255, 255, 0.02);
  border-color: rgba(255, 255, 255, 0.06);
}

[data-theme="dark"] .service-item-compact:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .section-header {
    padding: 16px 20px;
  }
  
  .services-content {
    padding: 20px;
  }
  
  .service-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .service-actions {
    align-self: stretch;
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .section-header {
    padding: 12px 16px;
  }
  
  .services-content {
    padding: 16px;
  }
  
  .service-item-compact {
    padding: 16px;
  }
}
</style> 