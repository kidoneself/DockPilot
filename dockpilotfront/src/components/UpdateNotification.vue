<!-- 更新通知组件 -->
<template>
  <div class="update-notification">
    <!-- 更新提示按钮 -->
    <n-badge :value="hasUpdate ? '!' : ''" class="update-badge">
      <n-button 
        type="primary" 
        circle 
        @click="showUpdateDialog = true"
        :loading="checking"
        :disabled="isUpdating"
        class="update-button"
      >
        <template #icon>
          <n-icon :component="updateIcon" />
        </template>
      </n-button>
    </n-badge>

    <!-- 更新对话框 -->
    <n-modal 
      v-model:show="showUpdateDialog" 
      preset="dialog"
      title="系统更新" 
      style="width: 600px"
      :close-on-esc="!isUpdating"
      :mask-closable="!isUpdating"
    >
      <!-- 检查更新状态 -->
      <div v-if="!updateInfo && checking" class="loading-state">
        <n-spin size="medium" />
        <span>正在检查更新...</span>
      </div>

      <!-- 没有更新 -->
      <div v-else-if="updateInfo && !updateInfo.hasUpdate" class="no-update">
        <n-result 
          status="success" 
          title="当前已是最新版本"
          :description="'当前版本: ' + updateInfo.currentVersion"
        >
          <template #footer>
            <n-button type="primary" @click="recheckUpdate">重新检查</n-button>
          </template>
        </n-result>
      </div>

      <!-- 有新版本可用 -->
      <div v-else-if="updateInfo && updateInfo.hasUpdate && !isUpdating" class="update-available">
        <div class="version-info">
          <h3>🎉 发现新版本</h3>
          <div class="version-comparison">
            <span class="current-version">当前版本: {{ updateInfo.currentVersion }}</span>
            <n-icon class="arrow-icon"><ArrowForward /></n-icon>
            <span class="latest-version">最新版本: {{ updateInfo.latestVersion }}</span>
          </div>
        </div>

        <div v-if="updateInfo.releaseNotes" class="release-notes">
          <h4>更新内容</h4>
          <div class="notes-content" v-html="formatReleaseNotes(updateInfo.releaseNotes)"></div>
        </div>

        <div class="update-options">
          <n-alert
            title="热更新说明"
            type="info"
            :closable="false"
          >
            将在容器内进行热更新，无需重启容器，期间可能有短暂的服务中断
          </n-alert>
        </div>
      </div>

      <!-- 更新进行中 -->
      <div v-else-if="isUpdating" class="updating-state">
        <div class="update-header">
          <h3>🚀 正在执行热更新</h3>
          <p>目标版本: {{ updateProgress.targetVersion || updateInfo?.latestVersion }}</p>
        </div>

        <div class="progress-section">
          <n-progress 
            type="line"
            :percentage="updateProgress.progress || 0" 
            :status="getProgressStatus()"
            :stroke-width="8"
          />
          <div class="progress-message">
            {{ updateProgress.message || '准备中...' }}
          </div>
        </div>

        <div class="update-logs">
          <h4>更新日志</h4>
          <div class="logs-container" ref="logsContainer">
            <div v-for="(log, index) in updateLogs" :key="index" class="log-item">
              <span class="log-time">{{ log.time }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>

        <!-- 只有在非关键阶段才显示取消按钮 -->
        <div v-if="canCancel" class="cancel-section">
          <n-button type="error" @click="handleCancelUpdate" :loading="cancelling">
            取消更新
          </n-button>
        </div>
      </div>

      <!-- 更新完成 -->
      <div v-else-if="updateCompleted" class="update-completed">
        <n-result 
          status="success" 
          title="更新完成！"
          :description="'已成功更新到版本: ' + (updateProgress.targetVersion || updateInfo?.latestVersion)"
        >
          <template #footer>
            <n-space>
              <n-button type="primary" @click="reloadPage">刷新页面</n-button>
              <n-button @click="closeDialog">稍后刷新</n-button>
            </n-space>
          </template>
        </n-result>
      </div>

      <!-- 更新失败 -->
      <div v-else-if="updateFailed" class="update-failed">
        <n-result 
          status="error" 
          title="更新失败"
          :description="updateProgress.error || '未知错误'"
        >
          <template #footer>
            <n-space>
              <n-button type="primary" @click="retryUpdate">重试更新</n-button>
              <n-button @click="closeDialog">关闭</n-button>
            </n-space>
          </template>
        </n-result>
      </div>

      <!-- 初始状态 -->
      <div v-else class="initial-state">
        <div class="update-content">
          <p>点击下方按钮检查是否有新版本可用</p>
          <p>当前版本: {{ currentVersion }}</p>
        </div>
      </div>

      <template #action v-if="!isUpdating && !updateCompleted && !updateFailed">
        <n-space>
          <n-button @click="closeDialog">关闭</n-button>
          <n-button 
            v-if="!updateInfo"
            type="primary" 
            @click="checkForUpdates"
            :loading="checking"
          >
            检查更新
          </n-button>
          <n-button 
            v-else-if="updateInfo.hasUpdate" 
            type="primary" 
            @click="startUpdate"
            :loading="startingUpdate"
          >
            开始更新
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { CloudUploadOutline, ReloadOutline, ArrowForward } from '@vicons/ionicons5'
import { 
  checkUpdate, 
  applyHotUpdate, 
  getUpdateProgress, 
  cancelUpdate,
  type UpdateInfo,
  type UpdateProgress 
} from '@/api/http/update'

// 组合式API
const message = useMessage()
const dialog = useDialog()

// 响应式数据
const showUpdateDialog = ref(false)
const checking = ref(false)
const updateInfo = ref<UpdateInfo | null>(null)
const updateProgress = ref<UpdateProgress>({
  status: '',
  progress: 0,
  message: '',
  isUpdating: false,
  timestamp: ''
})
const updateLogs = ref<Array<{ time: string, message: string }>>([])
const startingUpdate = ref(false)
const cancelling = ref(false)
const logsContainer = ref<HTMLElement>()
const currentVersion = ref('v1.0.0')

// 定时器
let checkTimer: NodeJS.Timeout | null = null
let progressTimer: NodeJS.Timeout | null = null

// 计算属性
const hasUpdate = computed(() => updateInfo.value?.hasUpdate || false)
const isUpdating = computed(() => updateProgress.value.isUpdating)
const updateCompleted = computed(() => updateProgress.value.status === 'completed')
const updateFailed = computed(() => updateProgress.value.status === 'failed')
const canCancel = computed(() => {
  const status = updateProgress.value.status
  return status === 'downloading' || status === 'starting'
})

const updateIcon = computed(() => {
  if (isUpdating.value) return ReloadOutline
  return CloudUploadOutline
})

// 方法
const checkForUpdates = async () => {
  if (checking.value) return
  
  checking.value = true
  try {
    const result = await checkUpdate()
    updateInfo.value = result
    currentVersion.value = result.currentVersion
    
    if (result.hasUpdate) {
      message.success(`发现新版本 ${result.latestVersion}`)
    } else {
      message.info('当前已是最新版本')
    }
  } catch (error) {
    console.error('检查更新失败:', error)
    message.error('检查更新失败：' + (error as any)?.message || '网络连接错误')
  } finally {
    checking.value = false
  }
}

const recheckUpdate = async () => {
  updateInfo.value = null
  await checkForUpdates()
}

const startUpdate = async () => {
  dialog.warning({
    title: '确认更新',
    content: '确定要开始热更新吗？更新过程中可能有短暂的服务中断。',
    positiveText: '开始更新',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        startingUpdate.value = true
        await applyHotUpdate()
        
        // 开始监控更新进度
        startProgressMonitoring()
        
      } catch (error) {
        console.error('启动更新失败:', error)
        message.error('启动更新失败：' + (error as any)?.message)
      } finally {
        startingUpdate.value = false
      }
    }
  })
}

const startProgressMonitoring = () => {
  if (progressTimer) return
  
  progressTimer = setInterval(async () => {
    try {
      const progress = await getUpdateProgress()
      updateProgress.value = progress
      
      // 添加到日志
      if (progress.message && progress.message !== updateLogs.value[updateLogs.value.length - 1]?.message) {
        addUpdateLog(progress.message)
      }
      
      // 如果更新完成或失败，停止监控
      if (progress.status === 'completed' || progress.status === 'failed') {
        stopProgressMonitoring()
      }
      
    } catch (error) {
      console.error('获取更新进度失败:', error)
      stopProgressMonitoring()
    }
  }, 1000)
}

const stopProgressMonitoring = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

const addUpdateLog = (message: string) => {
  const now = new Date().toLocaleTimeString()
  updateLogs.value.push({ time: now, message })
  
  // 自动滚动到底部
  nextTick(() => {
    if (logsContainer.value) {
      logsContainer.value.scrollTop = logsContainer.value.scrollHeight
    }
  })
}

const handleCancelUpdate = async () => {
  dialog.warning({
    title: '取消更新',
    content: '确定要取消更新吗？这可能导致系统处于不稳定状态。',
    positiveText: '确定取消',
    negativeText: '继续更新',
    onPositiveClick: async () => {
      try {
        cancelling.value = true
        await cancelUpdate()
        stopProgressMonitoring()
        
        message.info('更新已取消')
        closeDialog()
        
      } catch (error) {
        console.error('取消更新失败:', error)
        message.error('取消更新失败')
      } finally {
        cancelling.value = false
      }
    }
  })
}

const retryUpdate = () => {
  updateProgress.value = {
    status: '',
    progress: 0,
    message: '',
    isUpdating: false,
    timestamp: ''
  }
  updateLogs.value = []
  startUpdate()
}

const reloadPage = () => {
  window.location.reload()
}

const closeDialog = () => {
  showUpdateDialog.value = false
  stopProgressMonitoring()
  // 重置状态
  if (!isUpdating.value) {
    updateInfo.value = null
    updateProgress.value = {
      status: '',
      progress: 0,
      message: '',
      isUpdating: false,
      timestamp: ''
    }
    updateLogs.value = []
  }
}

const getProgressStatus = () => {
  const status = updateProgress.value.status
  if (status === 'failed') return 'error'
  if (status === 'completed') return 'success'
  return 'info'
}

const formatReleaseNotes = (notes: string) => {
  // 简单的markdown格式化
  return notes
    .replace(/### (.*)/g, '<h4>$1</h4>')
    .replace(/\*\* (.*) \*\*/g, '<strong>$1</strong>')
    .replace(/\* (.*)/g, '<li>$1</li>')
    .replace(/\n/g, '<br>')
}

// 生命周期
onMounted(() => {
  // 静默检查一次更新（不显示错误）
  checkForUpdates().catch(() => {
    // 静默失败，可能后端未启动
    console.log('后端服务暂未启动，将在需要时连接')
  })
  
  // 每30分钟自动检查一次更新
  checkTimer = setInterval(() => {
    checkForUpdates().catch(() => {
      // 静默失败
    })
  }, 30 * 60 * 1000)
})

onUnmounted(() => {
  if (checkTimer) {
    clearInterval(checkTimer)
  }
  stopProgressMonitoring()
})
</script>

<style scoped>
.update-notification {
  position: relative;
}

.update-button {
  transition: all 0.3s ease;
}

.update-button:hover {
  transform: scale(1.1);
}

.update-badge :deep(.n-badge-sup) {
  background-color: #f56c6c;
  border-color: #f56c6c;
}

.loading-state {
  text-align: center;
  padding: 40px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.no-update {
  text-align: center;
}

.update-available {
  padding: 20px 0;
}

.version-info h3 {
  margin: 0 0 16px 0;
  color: #18a058;
}

.version-comparison {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}

.current-version {
  color: #909399;
}

.latest-version {
  color: #18a058;
  font-weight: bold;
}

.arrow-icon {
  color: #909399;
}

.release-notes {
  margin: 20px 0;
}

.release-notes h4 {
  margin: 0 0 8px 0;
  color: #303133;
}

.notes-content {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.6;
}

.update-options {
  margin-top: 20px;
}

.updating-state {
  padding: 20px 0;
}

.update-header {
  text-align: center;
  margin-bottom: 20px;
}

.update-header h3 {
  margin: 0 0 8px 0;
  color: #18a058;
}

.update-header p {
  margin: 0;
  color: #606266;
}

.progress-section {
  margin-bottom: 20px;
}

.progress-message {
  text-align: center;
  margin-top: 8px;
  color: #606266;
  font-size: 14px;
}

.update-logs h4 {
  margin: 0 0 12px 0;
  color: #303133;
}

.logs-container {
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  height: 150px;
  overflow-y: auto;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.log-item {
  display: flex;
  margin-bottom: 4px;
}

.log-time {
  color: #909399;
  margin-right: 8px;
  min-width: 80px;
}

.log-message {
  color: #303133;
}

.cancel-section {
  text-align: center;
  margin-top: 20px;
}

.update-completed,
.update-failed {
  text-align: center;
}

.initial-state {
  padding: 20px 0;
  text-align: center;
}

.update-content p {
  margin: 10px 0;
  color: #606266;
}
</style> 