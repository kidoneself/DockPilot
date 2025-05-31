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
          <div v-else-if="isDownloading" style="color: #409eff;">📡 正在下载中</div>
          <div v-else style="color: #18a058;">✅ 当前最新版本</div>
        </div>
      </n-tooltip>
      
      <!-- 更新提示小红点 -->
      <div v-if="hasUpdate && !isDownloading" class="update-dot"></div>
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
              <n-tag v-else-if="isDownloading" type="info" size="small">📡 下载中</n-tag>
              <n-tag v-else-if="updateStage === 'ready-to-restart'" type="success" size="small">✅ 可以重启</n-tag>
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
          <!-- 发现新版本 -->
          <div v-if="updateStage === 'ready-to-download'" class="update-stage">
            <n-alert type="warning" :closable="false">
              <template #header>🎉 发现新版本 {{ updateInfo?.latestVersion }}</template>
              <div class="update-actions">
                <n-button type="primary" size="small" @click="startDownload" :loading="false">
                  开始下载
                </n-button>
                <n-button size="small" @click="recheckUpdate">重新检查</n-button>
              </div>
            </n-alert>
          </div>
          
          <!-- 下载中 -->
          <div v-else-if="updateStage === 'downloading'" class="update-stage">
            <h3>📡 正在下载新版本...</h3>
            <n-progress :percentage="downloadStatus.progress" :status="getProgressStatus()" />
            <p class="download-message">{{ downloadStatus.message }}</p>
            <p style="color: #18a058;">✅ 服务正常运行，可继续使用</p>
            <div class="update-actions">
              <n-button size="small" @click="cancelDownload" :loading="cancelling">
                取消下载
              </n-button>
            </div>
          </div>

          <!-- 下载完成，等待重启确认 -->
          <div v-else-if="updateStage === 'ready-to-restart'" class="update-stage">
            <h3>✅ 新版本下载完成</h3>
            <p>版本 {{ downloadStatus.version }} 已下载并验证完毕</p>
            <div class="restart-options">
              <n-button type="primary" @click="confirmRestart" :loading="restarting">
                立即重启更新
              </n-button>
              <n-button @click="laterRestart">稍后重启</n-button>
            </div>
            <p style="color: #909399;">💡 重启前服务保持正常运行</p>
          </div>
          
          <!-- 重启中 -->
          <div v-else-if="updateStage === 'restarting'" class="update-stage">
            <h3>🔄 正在重启更新...</h3>
            <p>预计30秒完成，页面将自动刷新</p>
            <n-progress :percentage="restartProgress" />
          </div>

          <!-- 下载失败 -->
          <div v-else-if="updateStage === 'download-failed'" class="update-stage">
            <n-alert type="error" :closable="false">
              <template #header>❌ 下载失败</template>
              <p>{{ downloadStatus.message }}</p>
              <div class="update-actions">
                <n-button type="primary" size="small" @click="retryDownload">
                  重试下载
                </n-button>
                <n-button size="small" @click="resetUpdateStage">
                  取消
                </n-button>
              </div>
            </n-alert>
          </div>
          
          <!-- 正常状态 -->
          <div v-else class="update-stage">
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useMessage } from 'naive-ui'
import { InformationCircleOutline } from '@vicons/ionicons5'
import { 
  checkUpdate, 
  startDownload as apiStartDownload,
  getDownloadStatus,
  confirmRestart as apiConfirmRestart,
  cancelDownload as apiCancelDownload,
  getCurrentVersion,
  type UpdateInfo,
  type DownloadStatus
} from '@/api/http/update'

// 组合式API
const message = useMessage()

// 响应式数据
const showAboutDialog = ref(false)
const checking = ref(false)
const cancelling = ref(false)
const restarting = ref(false)
const updateInfo = ref<UpdateInfo | null>(null)
const downloadStatus = ref<DownloadStatus>({
  status: 'idle',
  progress: 0,
  message: '就绪',
  version: '',
  timestamp: ''
})
const currentVersion = ref('v1.0.0')
const updateStage = ref('idle') // 'idle' | 'ready-to-download' | 'downloading' | 'ready-to-restart' | 'restarting' | 'download-failed'
const restartProgress = ref(0)

// 定时器
let downloadTimer: NodeJS.Timeout | null = null
let restartTimer: NodeJS.Timeout | null = null

// 计算属性
const hasUpdate = computed(() => updateInfo.value?.hasUpdate || false)
const isDownloading = computed(() => updateStage.value === 'downloading')
const displayVersion = computed(() => currentVersion.value)

// 方法
const openGithub = () => {
  window.open('https://github.com/kidoneself/DockPilot', '_blank')
}

// 检查更新
const checkForUpdates = async () => {
  if (checking.value) return
  
  checking.value = true
  try {
    console.log('🔍 检查新版本...')
    
    const result = await checkUpdate()
    updateInfo.value = result
    
    if (result.hasUpdate) {
      updateStage.value = 'ready-to-download'
      message.success(`🎉 发现新版本 ${result.latestVersion}`)
    } else {
      updateStage.value = 'idle'
      message.info(`✅ 当前已是最新版本 ${result.currentVersion}`)
    }
    
    console.log('✅ 版本检查完成:', result)
  } catch (error) {
    console.error('检查更新失败:', error)
    message.error('检查更新失败：' + (error as any)?.message || '网络连接错误')
  } finally {
    checking.value = false
  }
}

const recheckUpdate = async () => {
  updateInfo.value = null
  updateStage.value = 'idle'
  await checkForUpdates()
}

// 开始下载
const startDownload = async () => {
  try {
    console.log('📡 开始下载新版本...')
    updateStage.value = 'downloading'
    
    const result = await apiStartDownload(updateInfo.value?.latestVersion)
    message.info(result)
    
    // 开始轮询下载状态
    pollDownloadStatus()
  } catch (error) {
    console.error('开始下载失败:', error)
    message.error('开始下载失败：' + (error as any)?.message)
    updateStage.value = 'ready-to-download'
  }
}

// 轮询下载状态
const pollDownloadStatus = () => {
  if (downloadTimer) {
    clearInterval(downloadTimer)
  }
  
  downloadTimer = setInterval(async () => {
    try {
      const status = await getDownloadStatus()
      downloadStatus.value = status
      
      console.log('📊 下载状态:', status)
      
      if (status.status === 'completed') {
        updateStage.value = 'ready-to-restart'
        clearInterval(downloadTimer!)
        message.success('下载完成，可以重启更新')
      } else if (status.status === 'failed') {
        updateStage.value = 'download-failed'
        clearInterval(downloadTimer!)
        message.error('下载失败')
      } else if (status.status === 'cancelled') {
        updateStage.value = 'ready-to-download'
        clearInterval(downloadTimer!)
        message.info('下载已取消')
      }
    } catch (error) {
      console.error('获取下载状态失败:', error)
    }
  }, 2000) // 2秒轮询一次
}

// 取消下载
const cancelDownload = async () => {
  try {
    cancelling.value = true
    const result = await apiCancelDownload()
    message.info(result)
    
    if (downloadTimer) {
      clearInterval(downloadTimer)
    }
    updateStage.value = 'ready-to-download'
  } catch (error) {
    console.error('取消下载失败:', error)
    message.error('取消下载失败：' + (error as any)?.message)
  } finally {
    cancelling.value = false
  }
}

// 确认重启
const confirmRestart = async () => {
  try {
    restarting.value = true
    updateStage.value = 'restarting'
    
    const result = await apiConfirmRestart()
    message.success(result)
    
    // 开始重启进度模拟
    startRestartProgress()
  } catch (error) {
    console.error('确认重启失败:', error)
    message.error('确认重启失败：' + (error as any)?.message)
    updateStage.value = 'ready-to-restart'
    restarting.value = false
  }
}

// 稍后重启
const laterRestart = () => {
  message.info('新版本已就绪，您可以稍后重启更新')
  showAboutDialog.value = false
}

// 重试下载
const retryDownload = () => {
  updateStage.value = 'ready-to-download'
  startDownload()
}

// 重置更新阶段
const resetUpdateStage = () => {
  updateStage.value = 'idle'
  updateInfo.value = null
}

// 开始重启进度
const startRestartProgress = () => {
  restartProgress.value = 0
  
  const progressInterval = setInterval(() => {
    restartProgress.value += 10
    if (restartProgress.value >= 100) {
      clearInterval(progressInterval)
    }
  }, 300)
  
  // 5秒后开始检测服务恢复
  setTimeout(() => {
    checkServiceRecovery()
  }, 5000)
}

// 检测服务恢复
const checkServiceRecovery = () => {
  const checkInterval = setInterval(async () => {
    try {
      await fetch('/api/update/health')
      clearInterval(checkInterval)
      message.success('更新完成，页面即将刷新')
      setTimeout(() => {
        window.location.reload()
      }, 1000)
    } catch (e) {
      // 继续等待
    }
  }, 3000)
}

// 获取进度状态
const getProgressStatus = () => {
  const status = downloadStatus.value.status
  if (status === 'failed') return 'error'
  if (status === 'completed') return 'success'
  return 'info'
}

// 获取当前版本
const loadCurrentVersion = async () => {
  try {
    const versionInfo = await getCurrentVersion()
    currentVersion.value = versionInfo.currentVersion
    console.log('✅ 当前版本:', versionInfo.currentVersion)
  } catch (error) {
    console.warn('获取当前版本失败:', error)
  }
}

// 生命周期
onMounted(async () => {
  console.log('🔍 初始化更新组件...')
  
  // 加载当前版本
  await loadCurrentVersion()
  
  // 页面加载时检查更新状态
  try {
    const status = await getDownloadStatus()
    if (status.status === 'completed') {
      updateStage.value = 'ready-to-restart'
      downloadStatus.value = status
    } else if (status.status === 'downloading') {
      updateStage.value = 'downloading'
      downloadStatus.value = status
      pollDownloadStatus()
    }
  } catch (error) {
    console.warn('检查下载状态失败:', error)
  }
})

onUnmounted(() => {
  if (downloadTimer) {
    clearInterval(downloadTimer)
  }
  if (restartTimer) {
    clearInterval(restartTimer)
  }
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
  font-size: 12px;
  color: #666;
  transition: all 0.2s ease;
}

.about-button:hover {
  color: #409eff;
}

.about-button.has-update {
  color: #f0a020;
  font-weight: 500;
}

.update-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(245, 108, 108, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0);
  }
}

.tooltip-content {
  text-align: center;
  font-size: 12px;
  line-height: 1.4;
}

.about-content {
  max-height: 70vh;
  overflow-y: auto;
}

.project-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.project-logo {
  width: 48px;
  height: 48px;
  margin-right: 12px;
}

.project-info h2 {
  margin: 0 0 4px 0;
  font-size: 18px;
  color: #333;
}

.project-info p {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #666;
}

.version-status {
  margin-top: 8px;
}

.contact-section {
  margin: 16px 0;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
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

.update-stage {
  margin: 12px 0;
}

.update-stage h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #333;
}

.update-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.download-message {
  margin: 8px 0;
  font-size: 12px;
  color: #666;
}

.restart-options {
  margin: 16px 0;
  display: flex;
  gap: 12px;
}

.up-to-date {
  margin-top: 8px;
  font-size: 12px;
  color: #18a058;
  text-align: center;
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
</style> 