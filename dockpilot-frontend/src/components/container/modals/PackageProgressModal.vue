<template>
  <NModal
    v-model:show="showModal"
    preset="card"
    title="项目打包进度"
    class="package-progress-modal"
    style="width: 600px;"
    :mask-closable="false"
    :close-on-esc="false"
  >
    <div v-if="!packageTask" style="text-align: center; padding: 40px;">
      <NSpin size="medium" />
      <div style="margin-top: 16px;">正在初始化打包任务...</div>
    </div>

    <div v-else>
      <!-- 任务基本信息 -->
      <div class="task-info">
        <div class="info-row">
          <span class="label">项目名称：</span>
          <span class="value">{{ packageTask.projectName }}</span>
        </div>
        <div class="info-row">
          <span class="label">容器数量：</span>
          <span class="value">{{ packageTask.containerIds.length }} 个</span>
        </div>
        <div class="info-row">
          <span class="label">任务状态：</span>
          <NTag 
            :type="getStatusType(packageTask.status)" 
            size="small"
          >
            {{ getStatusText(packageTask.status) }}
          </NTag>
        </div>
        <div class="info-row">
          <span class="label">创建时间：</span>
          <span class="value">{{ formatTime(packageTask.createTime) }}</span>
        </div>
      </div>

      <!-- 进度显示 -->
      <div class="progress-section">
        <div class="progress-header">
          <span class="progress-text">
            打包进度：{{ packageProgress }}%
          </span>
          <span v-if="packageTask.status === 'completed' && packageTask.fileSize" class="file-size">
            文件大小：{{ formatFileSize(packageTask.fileSize) }}
          </span>
        </div>
        
        <NProgress 
          :percentage="packageProgress"
          :status="getProgressStatus(packageTask.status)"
          :show-indicator="false"
          style="margin: 16px 0;"
        />
        
        <!-- 当前步骤 -->
        <div class="current-step">
          <div class="step-icon">
            <n-icon 
              v-if="packageTask.status === 'processing'" 
              size="16" 
              style="color: #2080f0;"
            >
              <SyncOutline class="spinning" />
            </n-icon>
            <n-icon 
              v-else-if="packageTask.status === 'completed'" 
              size="16" 
              style="color: #52c41a;"
            >
              <CheckmarkCircleOutline />
            </n-icon>
            <n-icon 
              v-else-if="packageTask.status === 'failed'" 
              size="16" 
              style="color: #ff4d4f;"
            >
              <CloseCircleOutline />
            </n-icon>
            <n-icon 
              v-else 
              size="16" 
              style="color: #faad14;"
            >
              <TimeOutline />
            </n-icon>
          </div>
          <span class="step-text">{{ packageTask.currentStep }}</span>
        </div>
      </div>

      <!-- 错误信息 -->
      <div v-if="packageTask.status === 'failed' && packageTask.errorMessage" class="error-section">
        <NAlert type="error" style="margin-top: 16px;">
          <template #icon>
            <n-icon><CloseCircleOutline /></n-icon>
          </template>
          <div>
            <div style="font-weight: 600; margin-bottom: 8px;">打包失败</div>
            <div style="font-size: 13px; word-break: break-all;">
              {{ packageTask.errorMessage }}
            </div>
          </div>
        </NAlert>
      </div>

      <!-- 完成信息 -->
      <div v-if="packageTask.status === 'completed'" class="success-section">
        <NAlert type="success" style="margin-top: 16px;">
          <template #icon>
            <n-icon><CheckmarkCircleOutline /></n-icon>
          </template>
          <div>
            <div style="font-weight: 600; margin-bottom: 8px;">打包完成！</div>
            <div style="font-size: 13px;">
              <div v-if="packageTask.completeTime">
                完成时间：{{ formatTime(packageTask.completeTime) }}
              </div>
              <div v-if="packageTask.fileSize">
                文件大小：{{ formatFileSize(packageTask.fileSize) }}
              </div>
              <div v-if="packageTask.fileName">
                文件名：{{ packageTask.fileName }}
              </div>
            </div>
          </div>
        </NAlert>
      </div>

      <!-- 操作提示 -->
      <div class="action-tips">
        <div v-if="packageTask.status === 'pending' || packageTask.status === 'processing'" class="tip">
          <n-icon size="14" style="margin-right: 6px; color: #faad14;">
            <InformationCircleOutline />
          </n-icon>
          打包进行中，请耐心等待...您可以关闭此窗口，任务会在后台继续执行
        </div>
        
        <div v-if="packageTask.status === 'completed'" class="tip success">
          <n-icon size="14" style="margin-right: 6px; color: #52c41a;">
            <CheckmarkCircleOutline />
          </n-icon>
          文件已自动开始下载，如果下载没有开始，请检查浏览器下载设置
        </div>
        
        <div v-if="packageTask.status === 'failed'" class="tip error">
          <n-icon size="14" style="margin-right: 6px; color: #ff4d4f;">
            <CloseCircleOutline />
          </n-icon>
          打包失败，请检查错误信息或联系管理员
        </div>
      </div>
    </div>

    <template #action>
      <NSpace>
        <NButton 
          v-if="packageTask?.status === 'completed'"
          @click="handleManualDownload"
        >
          重新下载
        </NButton>
        <NButton 
          v-if="packageTask?.status === 'failed'"
          type="primary"
          @click="handleRetry"
        >
          重试
        </NButton>
        <NButton 
          :type="packageTask?.status === 'completed' ? 'primary' : 'default'"
          @click="handleClose"
        >
          {{ packageTask?.status === 'completed' ? '完成' : '关闭' }}
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NModal, NButton, NSpace, NTag, NProgress, NAlert, NSpin, useMessage } from 'naive-ui'
import {
  SyncOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  TimeOutline,
  InformationCircleOutline
} from '@vicons/ionicons5'
import { downloadPackageFile, type PackageTask } from '@/api/containerYaml'

// Props & Emits
interface Props {
  show: boolean
  packageTask: PackageTask | null
}

interface Emits {
  (e: 'update:show', value: boolean): void
  (e: 'retry'): void
  (e: 'manual-download'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const message = useMessage()

// 状态管理
const showModal = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value)
})

// 计算属性
const packageProgress = computed(() => {
  if (!props.packageTask) return 0
  return typeof props.packageTask.progress === 'number' 
    ? props.packageTask.progress 
    : Number(props.packageTask.progress) || 0
})

// 工具函数
function getStatusType(status: string) {
  const statusMap: Record<string, any> = {
    'pending': 'warning',
    'processing': 'info',
    'completed': 'success',
    'failed': 'error'
  }
  return statusMap[status] || 'default'
}

function getStatusText(status: string) {
  const statusMap: Record<string, string> = {
    'pending': '等待处理',
    'processing': '处理中',
    'completed': '已完成',
    'failed': '失败'
  }
  return statusMap[status] || status
}

function getProgressStatus(status: string) {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'error'
  return 'default'
}

function formatTime(timeStr: string): string {
  try {
    const date = new Date(timeStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (error) {
    return timeStr
  }
}

function formatFileSize(bytes: number): string {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// 事件处理
function handleClose() {
  showModal.value = false
}

function handleRetry() {
  emit('retry')
}

function handleManualDownload() {
  if (!props.packageTask?.taskId) {
    message.error('任务ID不存在，无法下载')
    return
  }
  
  try {
    const downloadUrl = downloadPackageFile(props.packageTask.taskId)
    
    // 创建隐藏的下载链接
    const a = document.createElement('a')
    a.href = downloadUrl
    a.style.display = 'none'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    
    message.success('下载已启动')
    emit('manual-download')
    
  } catch (error: any) {
    message.error('下载失败: ' + (error.message || error))
  }
}
</script>

<style scoped>
/* 打包进度模态框样式 */
:deep(.package-progress-modal .n-card) {
  max-height: 70vh;
  overflow-y: auto;
}

.task-info {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.label {
  min-width: 80px;
  font-weight: 500;
  color: #666;
}

.value {
  color: #333;
}

.progress-section {
  margin-bottom: 20px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.progress-text {
  font-weight: 600;
  color: #333;
}

.file-size {
  font-size: 13px;
  color: #666;
}

.current-step {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid #2080f0;
}

.step-icon {
  margin-right: 8px;
}

.step-text {
  font-size: 14px;
  color: #333;
}

/* 旋转动画 */
.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.error-section {
  margin-top: 16px;
}

.success-section {
  margin-top: 16px;
}

.action-tips {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.tip {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 13px;
  color: #666;
}

.tip.success {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #389e0d;
}

.tip.error {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #cf1322;
}
</style> 