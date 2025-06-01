<!-- 现代化的关于项目组件 -->
<template>
  <div class="about-notification">
    <!-- 现代化的关于按钮 -->
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
          <div class="tooltip-version">DockPilot {{ displayVersion }}</div>
          <div v-if="hasUpdate" class="tooltip-status update">🎉 有新版本可用</div>
          <div v-else-if="isDownloading" class="tooltip-status downloading">📡 正在下载中</div>
          <div v-else class="tooltip-status latest">✅ 当前最新版本</div>
        </div>
      </n-tooltip>
      
      <!-- 现代化更新提示 -->
      <div v-if="hasUpdate && !isDownloading" class="update-indicator">
        <div class="update-dot"></div>
        <div class="update-ring"></div>
      </div>
    </div>

    <!-- 现代化关于项目对话框 -->
    <n-modal 
      v-model:show="showAboutDialog" 
      preset="card"
      title=""
      style="width: 600px; max-width: 90vw"
      :bordered="false"
      class="about-modal"
    >
      <div class="about-content">
        <!-- 现代化项目头部 -->
        <div class="project-header">
          <div class="header-background">
            <div class="gradient-bg"></div>
            <div class="pattern-overlay"></div>
          </div>
          <div class="header-content">
            <div class="logo-container">
              <img 
                src="/logo.svg" 
                class="project-logo" 
                alt="DockPilot"
              />
              <div class="logo-glow"></div>
            </div>
            <div class="project-info">
              <h1 class="project-title">DockPilot</h1>
              <div class="version-badge">{{ displayVersion }}</div>
              <p class="project-description">现代化 Docker 容器管理平台</p>
              <div class="status-indicator">
                <div v-if="hasUpdate" class="status-badge update-available">
                  <n-icon :component="InformationCircleOutline" />
                  <span>有新版本可用</span>
                </div>
                <div v-else-if="isDownloading" class="status-badge downloading">
                  <div class="loading-spinner"></div>
                  <span>下载中</span>
                </div>
                <div v-else-if="updateStage === 'ready-to-restart'" class="status-badge ready">
                  <n-icon :component="InformationCircleOutline" />
                  <span>可以重启</span>
                </div>
                <div v-else class="status-badge latest">
                  <n-icon :component="InformationCircleOutline" />
                  <span>最新版本</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 功能特性卡片 -->
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon">🐳</div>
            <h3>容器管理</h3>
            <p>直观的 Docker 容器管理界面</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🔄</div>
            <h3>热更新</h3>
            <p>无需重新部署的在线更新</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">📊</div>
            <h3>实时监控</h3>
            <p>容器状态实时监控展示</p>
          </div>
        </div>

        <!-- 版本更新区域 -->
        <div class="update-section">
          <!-- 发现新版本 -->
          <div v-if="updateStage === 'ready-to-download'" class="update-card">
            <div class="update-header">
              <div class="update-icon new-version">🎉</div>
              <div class="update-info">
                <h3>发现新版本</h3>
                <p>{{ updateInfo?.latestVersion }} 版本已发布</p>
              </div>
            </div>
            <div class="update-actions">
              <n-button type="primary" size="medium" @click="startDownload" class="primary-action">
                立即下载
              </n-button>
              <n-button size="medium" @click="recheckUpdate" class="secondary-action">
                重新检查
              </n-button>
            </div>
          </div>
          
          <!-- 下载中 -->
          <div v-else-if="updateStage === 'downloading'" class="update-card downloading">
            <div class="update-header">
              <div class="update-icon downloading">
                <div class="download-spinner"></div>
              </div>
              <div class="update-info">
                <h3>正在下载新版本</h3>
                <p>{{ downloadStatus.message }}</p>
              </div>
            </div>
            <div class="progress-container">
              <n-progress 
                :percentage="downloadStatus.progress" 
                :status="getProgressStatus()" 
                :show-indicator="false"
                class="custom-progress"
              />
              <div class="progress-text">{{ downloadStatus.progress }}%</div>
            </div>
            <div class="download-note">
              <n-icon :component="InformationCircleOutline" />
              <span>服务正常运行，可继续使用</span>
            </div>
            <div class="update-actions">
              <n-button size="medium" @click="cancelDownload" :loading="cancelling" class="cancel-action">
                取消下载
              </n-button>
            </div>
          </div>

          <!-- 下载完成，等待重启确认 -->
          <div v-else-if="updateStage === 'ready-to-restart'" class="update-card completed">
            <div class="update-header">
              <div class="update-icon completed">✅</div>
              <div class="update-info">
                <h3>新版本下载完成</h3>
                <p>版本 {{ downloadStatus.version }} 已准备就绪</p>
              </div>
            </div>
            <div class="restart-container">
              <div class="restart-options">
                <n-button type="primary" size="medium" @click="confirmRestart" :loading="restarting" class="restart-action">
                  立即重启更新
                </n-button>
                <n-button size="medium" @click="laterRestart" class="later-action">
                  稍后重启
                </n-button>
              </div>
              <div class="restart-note">
                <n-icon :component="InformationCircleOutline" />
                <span>重启前服务保持正常运行</span>
              </div>
            </div>
          </div>
          
          <!-- 重启中 -->
          <div v-else-if="updateStage === 'restarting'" class="update-card restarting">
            <div class="update-header">
              <div class="update-icon restarting">
                <div class="restart-spinner"></div>
              </div>
              <div class="update-info">
                <h3>正在重启更新</h3>
                <p>预计30秒完成，页面将自动刷新</p>
              </div>
            </div>
            <div class="progress-container">
              <n-progress 
                :percentage="restartProgress" 
                status="info"
                :show-indicator="false"
                class="custom-progress"
              />
              <div class="progress-text">{{ restartProgress }}%</div>
            </div>
          </div>

          <!-- 下载失败 -->
          <div v-else-if="updateStage === 'download-failed'" class="update-card failed">
            <div class="update-header">
              <div class="update-icon failed">❌</div>
              <div class="update-info">
                <h3>下载失败</h3>
                <p>{{ downloadStatus.message }}</p>
              </div>
            </div>
            <div class="update-actions">
              <n-button type="primary" size="medium" @click="retryDownload" class="retry-action">
                重试下载
              </n-button>
              <n-button size="medium" @click="resetUpdateStage" class="cancel-action">
                取消
              </n-button>
            </div>
          </div>
          
          <!-- 正常状态 -->
          <div v-else class="update-card normal">
            <div class="check-update-container">
              <n-button 
                type="primary" 
                size="medium" 
                @click="checkForUpdates" 
                :loading="checking"
                class="check-update-btn"
              >
                <template #icon>
                  <n-icon :component="InformationCircleOutline" />
                </template>
                检查更新
              </n-button>
              <div v-if="updateInfo && !hasUpdate" class="up-to-date">
                <div class="up-to-date-icon">✅</div>
                <span>当前已是最新版本</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 现代化作者信息 -->
        <div class="author-section">
          <div class="author-card">
            <div class="author-avatar">👨‍💻</div>
            <div class="author-info">
              <h3>kidoneself</h3>
              <p class="author-title">项目开发者</p>
              <div class="github-link" @click="openGithub">
                <svg class="github-icon" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                </svg>
                <span>GitHub</span>
                <svg class="external-icon" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3m-2 16H5V5h7V3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7h-2v7z"/>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <!-- 现代化支持区域 -->
        <div class="support-section">
          <div class="support-card">
            <div class="support-content">
              <div class="support-icon">🙏</div>
              <div class="support-text">
                <h3>支持项目</h3>
                <p>如果项目对你有帮助，欢迎 Star ⭐ 或打赏支持开发</p>
              </div>
            </div>
          </div>
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
  color: var(--n-text-color-2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 6px;
  padding: 4px 8px;
}

.about-button:hover {
  color: var(--n-color-primary);
  background: rgba(64, 158, 255, 0.1);
  transform: translateY(-1px);
}

.about-button.has-update {
  color: var(--n-color-warning);
  font-weight: 500;
  background: rgba(240, 160, 32, 0.1);
}

.update-indicator {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 12px;
  height: 12px;
}

.update-dot {
  width: 8px;
  height: 8px;
  background: linear-gradient(45deg, #f56c6c, #ff8a8a);
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  box-shadow: 0 2px 4px rgba(245, 108, 108, 0.3);
}

.update-ring {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(245, 108, 108, 0.3);
  border-radius: 50%;
  animation: ripple 2s infinite ease-out;
}

@keyframes ripple {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

.tooltip-content {
  text-align: center;
  font-size: 12px;
  line-height: 1.5;
  padding: 4px;
}

.tooltip-version {
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--n-text-color);
}

.tooltip-status {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 10px;
  display: inline-block;
}

.tooltip-status.update {
  background: rgba(240, 160, 32, 0.2);
  color: var(--n-color-warning);
}

.tooltip-status.downloading {
  background: rgba(64, 158, 255, 0.2);
  color: var(--n-color-primary);
}

.tooltip-status.latest {
  background: rgba(24, 160, 88, 0.2);
  color: var(--n-color-success);
}

/* 模态框样式 */
:deep(.about-modal .n-card) {
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  background: var(--n-color);
}

:deep(.about-modal .n-card-header) {
  display: none;
}

.about-content {
  max-height: 80vh;
  overflow-y: auto;
  margin: -24px;
}

/* 项目头部样式 */
.project-header {
  position: relative;
  padding: 40px 24px;
  overflow: hidden;
}

.header-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
}

.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, 
    #2c2c2c 0%, 
    #1a1a1a 100%);
}

.pattern-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 25% 25%, rgba(255, 255, 255, 0.05) 2px, transparent 2px),
    radial-gradient(circle at 75% 75%, rgba(255, 255, 255, 0.05) 2px, transparent 2px);
  background-size: 60px 60px;
  opacity: 0.4;
}

.header-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 24px;
  color: white;
}

.logo-container {
  position: relative;
}

.project-logo {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.1);
  padding: 8px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.logo-glow {
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  background: linear-gradient(45deg, #404040, #2a2a2a);
  border-radius: 20px;
  opacity: 0.3;
  filter: blur(8px);
  z-index: -1;
}

.project-info {
  flex: 1;
}

.project-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
  background: linear-gradient(45deg, #ffffff, #f0f0f0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.version-badge {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 8px;
}

.project-description {
  font-size: 16px;
  opacity: 0.9;
  margin: 0 0 16px 0;
  line-height: 1.5;
}

.status-indicator {
  display: flex;
  align-items: center;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.15);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.status-badge.update-available {
  background: rgba(240, 160, 32, 0.2);
  border-color: rgba(240, 160, 32, 0.3);
}

.status-badge.downloading {
  background: rgba(64, 158, 255, 0.2);
  border-color: rgba(64, 158, 255, 0.3);
}

.status-badge.ready {
  background: rgba(24, 160, 88, 0.2);
  border-color: rgba(24, 160, 88, 0.3);
}

.loading-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 功能特性网格 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
  padding: 24px;
  background: var(--n-color-hover);
}

.feature-card {
  background: var(--n-color);
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  transition: all 0.3s ease;
  border: 1px solid var(--n-border-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 24px;
  margin-bottom: 12px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feature-card h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--n-text-color);
}

.feature-card p {
  font-size: 12px;
  color: var(--n-text-color-2);
  margin: 0;
  line-height: 1.4;
}

/* 更新区域 */
.update-section {
  padding: 24px;
}

.update-card {
  background: var(--n-color);
  border-radius: 12px;
  padding: 24px;
  border: 1px solid var(--n-border-color);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.update-card.downloading {
  border-color: rgba(64, 158, 255, 0.2);
  background: var(--n-color);
}

.update-card.completed {
  border-color: rgba(24, 160, 88, 0.2);
  background: var(--n-color);
}

.update-card.failed {
  border-color: rgba(245, 108, 108, 0.2);
  background: var(--n-color);
}

.update-card.restarting {
  border-color: rgba(64, 158, 255, 0.2);
  background: var(--n-color);
}

.update-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.update-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
}

.update-icon.new-version {
  background: linear-gradient(135deg, #f0a020, #ff8a8a);
  color: white;
  box-shadow: 0 4px 12px rgba(240, 160, 32, 0.3);
}

.update-icon.downloading {
  background: linear-gradient(135deg, #409eff, #66b3ff);
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.update-icon.completed {
  background: linear-gradient(135deg, #18a058, #4ade80);
  color: white;
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.3);
}

.update-icon.failed {
  background: linear-gradient(135deg, #f56c6c, #ff8a8a);
  color: white;
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);
}

.update-icon.restarting {
  background: linear-gradient(135deg, #409eff, #66b3ff);
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.download-spinner, .restart-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.update-info h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--n-text-color);
}

.update-info p {
  font-size: 14px;
  color: var(--n-text-color-2);
  margin: 0;
  line-height: 1.4;
}

.update-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.progress-container {
  position: relative;
  margin: 16px 0;
}

:deep(.custom-progress .n-progress-graph) {
  height: 8px;
  border-radius: 4px;
  overflow: hidden;
}

:deep(.custom-progress .n-progress-graph-line-fill) {
  background: linear-gradient(90deg, #409eff, #66b3ff);
  border-radius: 4px;
}

.progress-text {
  position: absolute;
  right: 0;
  top: -20px;
  font-size: 12px;
  color: var(--n-text-color-2);
  font-weight: 600;
}

.download-note, .restart-note {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0;
  padding: 8px 12px;
  background: rgba(24, 160, 88, 0.1);
  border-radius: 8px;
  font-size: 12px;
  color: var(--n-color-success);
}

.restart-container {
  margin-top: 16px;
}

.restart-options {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.check-update-container {
  text-align: center;
  padding: 20px 0;
}

.check-update-btn {
  margin-bottom: 16px;
}

.up-to-date {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--n-color-success);
  font-size: 14px;
}

.up-to-date-icon {
  font-size: 16px;
}

/* 按钮样式 */
.primary-action, .restart-action, .retry-action {
  background: linear-gradient(135deg, #409eff, #66b3ff);
  border: none;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
}

.primary-action:hover, .restart-action:hover, .retry-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}

.secondary-action, .later-action {
  background: var(--n-color-hover);
  border: 1px solid var(--n-border-color);
  color: var(--n-text-color);
  transition: all 0.3s ease;
}

.secondary-action:hover, .later-action:hover {
  background: var(--n-color-pressed);
  transform: translateY(-1px);
}

.cancel-action {
  background: var(--n-color-hover);
  border: 1px solid rgba(245, 108, 108, 0.2);
  color: var(--n-color-error);
  transition: all 0.3s ease;
}

.cancel-action:hover {
  background: rgba(245, 108, 108, 0.1);
  transform: translateY(-1px);
}

/* 作者信息 */
.author-section {
  padding: 24px;
  background: var(--n-color-hover);
}

.author-card {
  background: var(--n-color);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--n-border-color);
  display: flex;
  align-items: center;
  gap: 20px;
  transition: all 0.3s ease;
}

.author-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.author-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #404040, #2a2a2a);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.author-info h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--n-text-color);
}

.author-title {
  font-size: 14px;
  color: var(--n-text-color-2);
  margin: 0 0 12px 0;
}

.github-link {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--n-color-primary);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(64, 158, 255, 0.1);
  width: fit-content;
}

.github-link:hover {
  background: rgba(64, 158, 255, 0.2);
  transform: translateX(4px);
}

.github-icon, .external-icon {
  width: 16px;
  height: 16px;
}

/* 支持区域 */
.support-section {
  padding: 24px;
}

.support-card {
  background: linear-gradient(135deg, #2c2c2c, #1a1a1a);
  border-radius: 12px;
  padding: 24px;
  color: white;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
}

.support-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.support-icon {
  font-size: 32px;
  opacity: 0.9;
}

.support-text h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
}

.support-text p {
  font-size: 14px;
  margin: 0;
  opacity: 0.9;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 600px) {
  .header-content {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
  
  .features-grid {
    grid-template-columns: 1fr;
  }
  
  .author-card {
    flex-direction: column;
    text-align: center;
  }
  
  .support-content {
    flex-direction: column;
    text-align: center;
  }
  
  .update-actions, .restart-options {
    flex-direction: column;
  }
}
</style> 