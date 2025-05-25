<template>
  <n-card
    class="image-card"
    hoverable
    bordered
    :style="{ borderLeft: `6px solid ${statusBarColor}` }"
  >
    <div class="image-row">
      <!-- 中间：镜像信息和图标 -->
      <div class="image-main">
        <!-- 图标 -->
        <div class="image-icon">
          <NIcon :size="40">
            <HardwareChipOutline />
          </NIcon>
        </div>
        <div class="image-info-content">
          <!-- 第一行：名称和拉取状态 -->
          <div class="image-name-row">
            <NEllipsis 
              :tooltip="true" 
              style="max-width: 350px; font-weight: bold; font-size: 16px;"
            >
              {{ image.name }}
            </NEllipsis>
            <!-- 拉取状态标签 -->
            <n-tag 
              v-if="image.pullStatus && image.pullStatus.status !== 'idle'" 
              :type="getStatusType(image.pullStatus.status)"
              size="small"
              style="margin-left: 12px;"
            >
              {{ getStatusText(image.pullStatus.status) }}
            </n-tag>
          </div>
          
          <!-- 第二行：标签和创建时间 -->
          <div class="image-meta-row">
            <span class="image-tag">标签：{{ image.tag }}</span>
            <span class="image-label">创建时间：</span>
            <span class="image-value">{{ formatTime(image.created) }}</span>
            <span class="image-label">大小：</span>
            <span class="image-value">{{ formatSize(image.size) }}</span>
            <span v-if="image.needUpdate" class="update-badge">有新版本</span>
          </div>

          <!-- 第三行：拉取进度（仅在拉取中时显示） -->
          <div v-if="image.pullStatus?.status === 'pulling'" class="image-progress-row">
            <n-progress
              type="line"
              :percentage="image.pullStatus.percentage"
              :processing="true"
              :height="8"
              :border-radius="4"
              :color="'#2080f0'"
              style="flex: 1; max-width: 300px;"
            />
            <span class="progress-text">{{ image.pullStatus.message }}</span>
          </div>

          <!-- 错误信息（仅在失败时显示） -->
          <div v-if="image.pullStatus?.status === 'failed'" class="image-error-row">
            <n-text type="error" depth="2" style="font-size: 12px;">
              ❌ {{ formatErrorMessage(image.pullStatus.error || image.pullStatus.message || '镜像拉取失败') }}
            </n-text>
          </div>
        </div>
      </div>

      <!-- 右侧：操作按钮组 -->
      <div class="image-actions">
        <NSpace>
          <NButtonGroup>
            <!-- 创建容器按钮：仅对真实存在的镜像显示 -->
            <NTooltip v-if="image.isRealImage" trigger="hover">
              <template #trigger>
                <NButton
                  quaternary
                  circle
                  size="small"
                  @click="handleAction('create')"
                >
                  <template #icon>
                    <NIcon><AddOutline /></NIcon>
                  </template>
                </NButton>
              </template>
              创建容器
            </NTooltip>
            
            <!-- 重试按钮（仅在拉取失败时显示） -->
            <NTooltip v-if="image.canRetry" trigger="hover">
              <template #trigger>
                <NButton
                  quaternary
                  circle
                  size="small"
                  type="warning"
                  @click="handleRetryPull"
                >
                  <template #icon>
                    <NIcon><RefreshOutline /></NIcon>
                  </template>
                </NButton>
              </template>
              重新拉取
            </NTooltip>
            
            <!-- 删除按钮：所有镜像都可以删除 -->
            <NTooltip trigger="hover">
              <template #trigger>
                <NButton
                  quaternary
                  circle
                  size="small"
                  @click="handleAction('delete')"
                >
                  <template #icon>
                    <NIcon><TrashOutline /></NIcon>
                  </template>
                </NButton>
              </template>
              {{ image.isRealImage ? '删除镜像' : '删除记录' }}
            </NTooltip>
            
            <!-- 更新按钮：仅对需要更新且真实存在的镜像显示 -->
            <NTooltip v-if="image.needUpdate && image.isRealImage" trigger="hover">
              <template #trigger>
                <NButton
                  quaternary
                  circle
                  size="small"
                  @click="handleAction('update')"
                >
                  <template #icon>
                    <NIcon><ArrowUpOutline /></NIcon>
                  </template>
                </NButton>
              </template>
              更新
            </NTooltip>
          </NButtonGroup>
        </NSpace>
      </div>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  HardwareChipOutline,
  TrashOutline,
  ArrowUpOutline,
  AddOutline,
  RefreshOutline
} from '@vicons/ionicons5'
import { NIcon, NButton, NButtonGroup, NTooltip, NSpace, NEllipsis, NTag, NProgress, NText } from 'naive-ui'
import type { Image } from '@/api/model/imageModel'

const props = defineProps<{
  image: Image
}>()

const emit = defineEmits<{
  (e: 'action', action: string, image: Image): void
  (e: 'retry-pull', image: Image): void
}>()

// 镜像卡片左侧彩色边，根据状态显示不同颜色
const statusBarColor = computed(() => {
  if (props.image.pullStatus) {
    switch (props.image.pullStatus.status) {
      case 'pulling':
        return '#2080f0' // 蓝色，正在拉取
      case 'failed':
        return '#d03050' // 红色，拉取失败
      case 'success':
        return '#18a058' // 绿色，拉取成功
    }
  }
  if (props.image.needUpdate) {
    return '#f0a020' // 橙色，需要更新
  }
  return '#52c41a' // 绿色，正常
})

// 获取状态类型（用于标签颜色）
const getStatusType = (status: string) => {
  switch (status) {
    case 'pulling': return 'info'
    case 'success': return 'success'
    case 'failed': return 'error'
    default: return 'default'
  }
}

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'pulling': return '拉取中'
    case 'success': return '拉取成功'
    case 'failed': 
      // 检查是否是更新失败但原镜像可用的情况
      if (props.image.pullStatus?.message?.includes('更新失败，但原镜像依然可用')) {
        return '更新失败'
      }
      return '拉取失败'
    default: return '未知'
  }
}

// 格式化时间
function formatTime(timestamp: number) {
  return new Date(timestamp).toLocaleString()
}

// 格式化大小
function formatSize(size: number) {
  const units = ['B', 'KB', 'MB', 'GB']
  let value = size
  let unitIndex = 0
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex++
  }
  return `${value.toFixed(2)} ${units[unitIndex]}`
}

// 格式化错误消息 - 后端已处理错误转换，前端只需显示
function formatErrorMessage(error: string): string {
  if (!error) return '拉取失败'
  
  // 后端已经处理了错误信息转换，前端只需要控制长度即可
  if (error.length > 80) {
    return error.substring(0, 80) + '...'
  }
  
  return error
}

function handleAction(action: string) {
  emit('action', action, props.image)
}

function handleRetryPull() {
  emit('retry-pull', props.image)
}
</script>

<style scoped>
.image-card {
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
  border-radius: 12px;
  background: var(--n-card-color);
  padding: 0;
}

.image-row {
  display: flex;
  align-items: flex-start;
}

.image-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: flex-start;
  gap: 18px;
}

.image-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.image-info-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.image-name-row {
  display: flex;
  align-items: center;
  margin-bottom: 0;
  line-height: 1.2;
}

.image-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  color: var(--n-text-color-3);
  font-size: 14px;
}

.image-progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.progress-text {
  font-size: 12px;
  color: var(--n-text-color-3);
  white-space: nowrap;
}

.image-error-row {
  margin-top: 4px;
}

.image-tag {
  color: var(--n-text-color-3);
  font-size: 14px;
}

.image-label {
  color: var(--n-text-color-3);
  font-size: 14px;
}

.image-value {
  color: var(--n-text-color-2);
  font-size: 14px;
}

.update-badge {
  font-size: 12px;
  color: #f0a020;
  background: #fff7e6;
  border-radius: 4px;
  padding: 2px 8px;
  font-weight: 500;
}

.image-actions {
  margin-left: 18px;
  display: flex;
  align-items: center;
}

/* 响应式布局 */
@media (max-width: 1024px) {
  .image-row {
    flex-wrap: wrap;
    gap: 12px;
    padding: 12px;
  }

  .image-main {
    gap: 12px;
  }

  .image-meta-row {
    flex-wrap: wrap;
    gap: 12px;
    font-size: 14px;
  }
}

@media (max-width: 768px) {
  .image-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 8px;
  }

  .image-main {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    width: 100%;
  }

  .image-info-content {
    width: 100%;
  }

  .image-name-row {
    min-width: 0;
    width: 100%;
    flex-wrap: wrap;
    gap: 8px;
  }

  .image-meta-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    width: 100%;
    font-size: 13px;
  }

  .image-meta-row > span {
    display: flex;
    align-items: center;
    margin-bottom: 2px;
  }

  .image-progress-row {
    flex-direction: column;
    align-items: stretch;
    gap: 6px;
  }

  .progress-text {
    font-size: 11px;
  }

  .image-actions {
    margin-left: 0;
    align-self: flex-end;
    margin-top: 10px;
    width: 100%;
    justify-content: flex-end;
  }
}
</style> 