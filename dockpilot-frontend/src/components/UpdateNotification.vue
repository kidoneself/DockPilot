<!-- 关于项目组件 -->
<template>
  <div class="about-notification">
    <!-- 简洁的关于按钮 -->
    <div class="about-button-container">
      <n-tooltip trigger="hover" placement="bottom">
        <template #trigger>
          <n-button 
            text
            @click="showAboutDialog = true"
            class="about-button"
            :class="{ 'has-update': hasUpdate }"
          >
            <template #icon>
              <n-icon size="16" :component="InformationCircleOutline" />
            </template>
            {{ displayVersion }}
          </n-button>
        </template>
        <div class="tooltip-content">
          <div>DockPilot {{ displayVersion }}</div>
          <div v-if="hasUpdate" style="color: #f0a020;">🎉 有新版本可用</div>
          <div v-else-if="isUpdating" style="color: #409eff;">🔄 正在更新中</div>
          <div v-else style="color: #18a058;">✅ 当前最新版本</div>
        </div>
      </n-tooltip>
      
      <!-- 更新提示小红点 -->
      <div v-if="hasUpdate && !isUpdating" class="update-dot"></div>
      </div>

        <!-- 关于项目对话框 -->
    <n-modal 
      v-model:show="showAboutDialog" 
      preset="card"
      title="关于 DockPilot" 
      style="width: 500px"
    >
      <div class="about-content">
        <!-- 项目头部 -->
        <div class="project-header">
          <img 
            src="@/assets/icons/logo.svg" 
            class="project-logo" 
            alt="DockPilot"
          />
          <div class="project-info">
            <h2>DockPilot {{ displayVersion }}</h2>
            <p>现代化Docker容器管理平台</p>
            <div class="version-status">
              <n-tag v-if="hasUpdate" type="warning" size="small">🎉 有新版本可用</n-tag>
              <n-tag v-else-if="isUpdating" type="info" size="small">🔄 更新中</n-tag>
              <n-tag v-else type="success" size="small">✅ 最新版本</n-tag>
      </div>
          </div>
        </div>

        <!-- 作者信息 -->
        <div class="contact-section">
          <div class="author-info">
            <h3>👨‍💻 kidoneself</h3>
            <p>GitHub: <n-text type="info" @click="openGithub" class="github-link">kidoneself/DockPilot</n-text></p>
          </div>
        </div>

        <!-- 版本更新区域 -->
        <div class="update-section">
          <!-- 有新版本 -->
          <div v-if="hasUpdate && !isUpdating" class="update-alert">
            <n-alert type="warning" :closable="false">
              <template #header>🎉 发现新版本 {{ updateInfo?.latestVersion }}</template>
              <div class="update-actions">
                <n-button type="primary" size="small" @click="startUpdate" :loading="startingUpdate">
                  立即更新
                </n-button>
                <n-button size="small" @click="recheckUpdate">重新检查</n-button>
              </div>
            </n-alert>
          </div>
          
          <!-- 更新中 -->
          <div v-else-if="isUpdating" class="updating-status">
            <n-progress :percentage="updateProgress.progress || 0" :status="getProgressStatus()" />
            <p class="update-message">{{ updateProgress.message || '正在更新...' }}</p>
          </div>
          
          <!-- 正常状态 -->
          <div v-else class="normal-status">
            <n-button text size="small" @click="checkForUpdates" :loading="checking">
              检查更新
            </n-button>
            <div v-if="updateInfo && !hasUpdate" class="up-to-date">
              ✅ 当前已是最新版本
            </div>
          </div>
        </div>

        <!-- 支持项目 -->
        <div class="support-section">
          <p class="support-text">🙏 如果项目对你有帮助，欢迎 Star ⭐ 或打赏支持</p>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { 
  InformationCircleOutline, 
  CloudUploadOutline, 
  ReloadOutline
} from '@vicons/ionicons5'
import { 
  checkUpdate, 
  applyHotUpdate, 
  getUpdateProgress, 
  cancelUpdate,
  type UpdateInfo,
  type UpdateProgress 
} from '@/api/http/update'
import { useThemeStore } from '@/store/theme'
import logo from '@/assets/icons/logo.svg'

// 组合式API
const message = useMessage()
const dialog = useDialog()
const themeStore = useThemeStore()

// 响应式数据
const showAboutDialog = ref(false)
const checking = ref(false)
const updateInfo = ref<UpdateInfo | null>(null)
const updateProgress = ref<UpdateProgress>({
  status: '',
  progress: 0,
  message: '',
  isUpdating: false,
  timestamp: ''
})
const startingUpdate = ref(false)
const currentVersion = ref('v1.0.0')

// 定时器
let checkTimer: NodeJS.Timeout | null = null
let progressTimer: NodeJS.Timeout | null = null

// 删除不需要的数据，简化组件

// 计算属性
const hasUpdate = computed(() => updateInfo.value?.hasUpdate || false)
const isUpdating = computed(() => updateProgress.value.isUpdating)
const updateCompleted = computed(() => updateProgress.value.status === 'completed')
const updateFailed = computed(() => updateProgress.value.status === 'failed')
const canCancel = computed(() => {
  const status = updateProgress.value.status
  return status === 'downloading' || status === 'starting'
})

const displayVersion = computed(() => {
  if (updateInfo.value?.currentVersion) {
    return updateInfo.value.currentVersion
  }
  return currentVersion.value
})

// 方法

const openGithub = () => {
  window.open('https://github.com/kidoneself/DockPilot', '_blank')
}

// 从缓存快速检查更新（页面加载时使用）
const checkForUpdatesFromCache = async () => {
  if (checking.value) return
  
  checking.value = true
  try {
    console.log('🔍 从缓存快速检查版本信息...')
    
    const result = await checkUpdate()
    updateInfo.value = result
    
    // 确保版本信息有效，防止显示 "unknown" 或空值
    if (result.currentVersion && result.currentVersion !== 'unknown' && result.currentVersion.trim() !== '') {
      currentVersion.value = result.currentVersion
      console.log('✅ 从后端获取版本:', result.currentVersion)
    } else {
      console.warn('⚠️ 后端返回的版本信息无效:', result.currentVersion, '保持前端默认版本:', currentVersion.value)
      // 确保不会被覆盖为unknown
      if (!currentVersion.value || currentVersion.value === 'unknown') {
        currentVersion.value = 'v1.0.7'
      }
    }
    
    console.log('✅ 缓存检查完成:', {
      hasUpdate: result.hasUpdate,
      currentVersion: currentVersion.value,
      latestVersion: result.latestVersion
    })
    
  } catch (error) {
    console.warn('⚠️ 缓存检查失败，但不影响使用:', error)
  } finally {
    checking.value = false
  }
}

// 强制检查更新（用户手动点击时使用）
const checkForUpdates = async () => {
  if (checking.value) return
  
  checking.value = true
  try {
    console.log('🔍 用户手动检查更新，强制获取最新信息...')
    
    // 先清除后端缓存，确保获取最新信息
    try {
      await fetch('/api/update/clear-cache', { method: 'POST' })
      console.log('🗑️ 后端缓存已清除')
    } catch (e) {
      console.warn('清除后端缓存失败，继续检查:', e)
    }
    
    const result = await checkUpdate()
    updateInfo.value = result
    
    // 确保版本信息有效，防止显示 "unknown" 或空值
    if (result.currentVersion && result.currentVersion !== 'unknown' && result.currentVersion.trim() !== '') {
      currentVersion.value = result.currentVersion
      console.log('✅ 从后端获取版本:', result.currentVersion)
    } else {
      console.warn('⚠️ 后端返回的版本信息无效:', result.currentVersion, '保持前端默认版本:', currentVersion.value)
      // 确保不会被覆盖为unknown
      if (!currentVersion.value || currentVersion.value === 'unknown') {
        currentVersion.value = 'v1.0.7'
      }
    }
    
    console.log('✅ 强制检查完成:', {
      hasUpdate: result.hasUpdate,
      currentVersion: currentVersion.value,
      latestVersion: result.latestVersion,
      详细信息: result
    })
    
    if (result.hasUpdate) {
      message.success(`🎉 发现新版本 ${result.latestVersion}，当前版本 ${currentVersion.value}`)
    } else {
      message.info(`✅ 当前已是最新版本 ${currentVersion.value}`)
    }
  } catch (error) {
    console.error('检查更新失败:', error)
    // 开发环境显示错误，生产环境静默处理
    if (process.env.NODE_ENV === 'development') {
      message.error('检查更新失败：' + (error as any)?.message || '网络连接错误')
    }
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

// 处理更新完成和失败的情况
const handleUpdateComplete = () => {
  message.success('更新完成！页面即将刷新')
  setTimeout(() => {
    window.location.reload()
  }, 2000)
}

const handleUpdateFailed = () => {
  message.error('更新失败，请稍后重试')
  // 重置状态以便重新尝试
  setTimeout(() => {
    updateInfo.value = null
    updateProgress.value = {
      status: '',
      progress: 0,
      message: '',
      isUpdating: false,
      timestamp: ''
    }
  }, 3000)
}

const startProgressMonitoring = () => {
  if (progressTimer) return
  
  progressTimer = setInterval(async () => {
    try {
      const progress = await getUpdateProgress()
      updateProgress.value = progress
      
      // 简化版本不显示详细日志
      
      // 如果更新完成或失败，停止监控并处理
      if (progress.status === 'completed') {
        stopProgressMonitoring()
        handleUpdateComplete()
      } else if (progress.status === 'failed') {
        stopProgressMonitoring()
        handleUpdateFailed()
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

// 删除日志和取消相关功能，简化版本不需要

// 删除不再需要的方法

const closeDialog = () => {
  showAboutDialog.value = false
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
onMounted(async () => {
  // 设置默认版本 - 确保总是有一个合理的版本显示
  const defaultVersion = 'v1.0.7'
  currentVersion.value = process.env.VUE_APP_VERSION || defaultVersion
  
  console.log('🔍 初始化版本信息:', {
    envVersion: process.env.VUE_APP_VERSION,
    currentVersion: currentVersion.value,
    nodeEnv: process.env.NODE_ENV
  })
  
  // 页面加载时从缓存快速检查版本信息
  try {
    console.log('🔄 页面加载：从缓存读取版本信息...')
    await checkForUpdatesFromCache()
    console.log('✅ 缓存版本检查完成')
  } catch (error) {
    console.warn('⚠️ 缓存版本检查失败，但不影响使用:', error)
    // 生产环境静默处理，开发环境可以看到错误
  }
  
  // 注意：后端已有定时检查机制，前端不需要定时检查
  // 用户可以手动点击"检查更新"按钮强制刷新
})

onUnmounted(() => {
  if (checkTimer) {
    clearInterval(checkTimer)
  }
  stopProgressMonitoring()
})
</script>

<style scoped>
.about-notification {
  position: relative;
}

.about-button-container {
  position: relative;
  display: inline-block;
}

.about-button {
  color: #666666 !important;
  transition: all 0.3s ease;
  padding: 4px 8px;
  font-size: 13px;
  border-radius: 6px;
  font-weight: 500;
}

.about-button:hover {
  color: #333333 !important;
}

.about-button.has-update {
  color: #f0a020 !important;
}

.update-dot {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border: 1px solid white;
  border-radius: 50%;
  animation: pulse-dot 2s infinite;
  z-index: 10;
}

@keyframes pulse-dot {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
}

.tooltip-content {
  text-align: center;
  font-size: 12px;
}

/* 简化的关于页面样式 */
.about-content {
  padding: 0;
}

.project-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.project-logo {
  width: 120px;  /* 调整尺寸以适应对话框 */
  height: auto;
  object-fit: contain;
  /* 确保 logo 颜色跟随主题 */
  filter: var(--logo-filter, none);
}

/* 深色主题下的 logo 样式 */
:root[data-theme="dark"] .project-logo {
  --logo-filter: brightness(0) invert(1);
}

.project-info h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  color: #ffffff !important;
}

.project-info p {
  margin: 0 0 8px 0;
  font-size: 14px;
  opacity: 0.95;
  color: #ffffff !important;
}

.version-status {
  display: flex;
  gap: 8px;
}

.version-status .n-tag {
  background: rgba(255, 255, 255, 0.2) !important;
  color: #ffffff !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  backdrop-filter: blur(10px);
  font-weight: 500;
}

.contact-section {
  margin-bottom: 20px;
  text-align: center;
}

.author-info {
  padding: 16px;
}

.author-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #333;
}

.author-info p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.github-link {
  cursor: pointer;
  transition: all 0.2s ease;
}

.github-link:hover {
  text-decoration: underline;
}

.update-section {
  margin: 16px 0;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}



.update-alert .update-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.updating-status {
  text-align: center;
}

.update-message {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #666;
}

.normal-status {
  text-align: center;
}

.up-to-date {
  margin-top: 8px;
  font-size: 12px;
  color: #18a058;
}

.support-section {
  margin-top: 16px;
  text-align: center;
}

.support-text {
  margin: 0;
  font-size: 13px;
  color: #666;
  line-height: 1.4;
}

/* 更新相关样式 */
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

.version-info-section h3 {
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

.update-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
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