<!-- 简洁的升级提示组件 -->
<template>
  <div class="update-notification">
    <!-- 升级提示按钮 -->
    <div class="update-button-container">
      <n-tooltip trigger="hover" placement="bottom">
        <template #trigger>
          <n-button 
            text
            class="update-button"
            :class="{ 'has-update': hasUpdate }"
            @click="showUpgradeModal = true"
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
      
      <!-- 更新提示指示器 -->
      <div v-if="hasUpdate && !isDownloading" class="update-indicator">
        <div class="update-dot"></div>
        <div class="update-ring"></div>
      </div>
    </div>

    <!-- 升级提示弹窗 -->
    <div v-if="showUpgradeModal" class="modal-overlay" @click="closeModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">🚀 升级提示</h2>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-content">
          <div class="project-info">
            <h3>系统名称：DockPilot 容器管理平台</h3>
            <p>这是一个现代化的 Docker 容器管理平台，支持容器管理、镜像管理、实时监控、热更新等功能。</p>
          </div>
          
          <!-- 根据更新状态显示不同内容 -->
          <div v-if="updateStage === 'ready-to-download'" class="project-info">
            <h3>本次更新</h3>
            <ul>
              <li>新增系统资源监控图表</li>
              <li>优化容器管理界面</li>
              <li>增强镜像更新功能</li>
              <li>修复部分安全性问题</li>
            </ul>
          </div>
          
          <!-- 下载中状态 -->
          <div v-else-if="updateStage === 'downloading'" class="download-section">
            <h3>正在下载更新</h3>
            <div class="progress-container">
              <n-progress 
                :percentage="downloadStatus.progress" 
                :status="getProgressStatus()" 
                :show-indicator="true"
              />
              <div class="progress-message">{{ downloadStatus.message }}</div>
            </div>
          </div>
          
          <!-- 准备重启状态 -->
          <div v-else-if="updateStage === 'ready-to-restart'" class="restart-section">
            <h3>更新下载完成</h3>
            <p>新版本 {{ downloadStatus.version }} 已下载完成，点击"立即升级"重启应用以完成更新。</p>
          </div>
          
          <!-- 重启中状态 -->
          <div v-else-if="updateStage === 'restarting'" class="restarting-section">
            <h3>正在重启更新</h3>
            <div class="progress-container">
              <n-progress 
                :percentage="restartProgress" 
                status="info"
                :show-indicator="true"
              />
              <div class="progress-message">预计30秒完成，页面将自动刷新</div>
            </div>
          </div>
          
          <!-- 下载失败状态 -->
          <div v-else-if="updateStage === 'download-failed'" class="error-section">
            <h3>下载失败</h3>
            <p class="error-message">{{ downloadStatus.message }}</p>
          </div>
          
          <!-- 默认状态：检查更新 -->
          <div v-else class="check-section">
            <h3>检查更新</h3>
            <p>点击下方按钮检查是否有新版本可用。</p>
          </div>
          
          <!-- GitHub Star 链接 -->
          <a class="github-star" href="https://github.com/kidoneself/DockPilot" target="_blank">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
            </svg>
            点个 Star 支持一下！
          </a>
          
          <!-- 联系方式区域 -->
          <div class="contact-section">
            <h4>联系作者</h4>
            <div class="contact-links">
              <!-- 微信联系 -->
              <div class="contact-item" @click="showWechatQR = true">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.171 4.203 3.002 5.55l-.568 2.273 2.778-1.555c.935.193 1.902.193 2.837 0C9.967 16.757 12.188 18.188 15.188 18.188c.339 0 .677-.016 1.016-.048-.271-.839-.419-1.727-.419-2.639 0-4.054 3.891-7.342 8.691-7.342.295 0 .588.016.881.048C24.66 5.166 21.743 2.188 8.691 2.188z"/>
                  <circle cx="6.188" cy="9.53" r="0.97"/>
                  <circle cx="11.188" cy="9.53" r="0.97"/>
                </svg>
                <span>微信群</span>
              </div>
              
              <!-- Telegram群组 -->
              <a class="contact-item" href="https://t.me/dockpilot" target="_blank">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z"/>
                </svg>
                <span>Telegram 群组</span>
              </a>
            </div>
          </div>
          
          <!-- 操作按钮 -->
          <div class="modal-actions">
            <button 
              class="btn btn-secondary" 
              @click="handleSecondaryAction"
              :disabled="isDownloading || updateStage === 'restarting'"
            >
              {{ getSecondaryButtonText() }}
            </button>
            <button 
              class="btn btn-primary" 
              @click="handlePrimaryAction"
              :disabled="updateStage === 'restarting'"
            >
              {{ getPrimaryButtonText() }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 微信二维码弹窗 -->
    <div v-if="showWechatQR" class="qr-overlay" @click="showWechatQR = false">
      <div class="qr-modal" @click.stop>
        <div class="qr-header">
          <h3>微信群</h3>
          <button class="close-btn" @click="showWechatQR = false">&times;</button>
        </div>
        <div class="qr-content">
          <!-- 微信二维码图片 -->
          <div class="qr-placeholder">
            <img src="/wechat-qr.png" alt="微信群二维码" class="qr-image" />
          </div>
          <p>扫描二维码加入微信群</p>
          <p class="wechat-id">DockPilot 用户交流群</p>
        </div>
      </div>
    </div>
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
const showUpgradeModal = ref(false)
const showWechatQR = ref(false)
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

// 计算属性
const hasUpdate = computed(() => updateInfo.value?.hasUpdate || false)
const isDownloading = computed(() => updateStage.value === 'downloading')
const displayVersion = computed(() => currentVersion.value)

// 关闭弹窗
const closeModal = () => {
  if (updateStage.value === 'downloading' || updateStage.value === 'restarting') {
    message.info('更新进行中，请稍候...')
    return
  }
  showUpgradeModal.value = false
}

// 获取按钮文本
const getSecondaryButtonText = () => {
  switch (updateStage.value) {
    case 'downloading':
      return '取消下载'
    case 'download-failed':
      return '取消'
    case 'ready-to-restart':
      return '稍后重启'
    default:
      return '以后再说'
  }
}

const getPrimaryButtonText = () => {
  switch (updateStage.value) {
    case 'ready-to-download':
      return '立即下载'
    case 'downloading':
      return '下载中...'
    case 'ready-to-restart':
      return '立即升级'
    case 'restarting':
      return '重启中...'
    case 'download-failed':
      return '重试下载'
    default:
      return '检查更新'
  }
}

// 处理次要操作按钮
const handleSecondaryAction = async () => {
  switch (updateStage.value) {
    case 'downloading':
      await cancelDownload()
      break
    case 'download-failed':
      resetUpdateStage()
      break
    case 'ready-to-restart':
      laterRestart()
      break
    default:
      closeModal()
  }
}

// 处理主要操作按钮
const handlePrimaryAction = async () => {
  switch (updateStage.value) {
    case 'ready-to-download':
      await startDownload()
      break
    case 'ready-to-restart':
      await confirmRestart()
      break
    case 'download-failed':
      await retryDownload()
      break
    default:
      await checkForUpdates()
  }
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
    } catch {
      // 继续等待
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
  showUpgradeModal.value = false
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
    } catch {
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
})
</script>

<style scoped>
.update-notification {
  position: relative;
}

.update-button-container {
  position: relative;
  display: inline-block;
}

.update-button {
  font-size: 12px;
  color: var(--n-text-color-2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 6px;
  padding: 4px 8px;
}

.update-button:hover {
  color: var(--n-color-primary);
  background: rgba(64, 158, 255, 0.1);
  transform: translateY(-1px);
}

.update-button.has-update {
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

/* 弹窗样式 - 参考1.html设计 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal {
  background: var(--n-color);
  padding: 2rem;
  border-radius: 16px;
  max-width: 600px;
  width: 90%;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  position: relative;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: bold;
  margin: 0;
  color: var(--n-text-color);
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--n-text-color-2);
  border-radius: 4px;
  padding: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--n-color-hover);
  color: var(--n-text-color);
}

.modal-content {
  margin-top: 1rem;
}

.project-info {
  margin-bottom: 1.5rem;
}

.project-info h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.2rem;
  color: var(--n-text-color);
}

.project-info p {
  margin: 0;
  color: var(--n-text-color-2);
  line-height: 1.6;
}

.project-info ul {
  margin: 0.5rem 0 0 1rem;
  color: var(--n-text-color-2);
}

.project-info li {
  margin-bottom: 0.25rem;
  line-height: 1.5;
}

/* 状态区域样式 */
.download-section,
.restart-section,
.restarting-section,
.error-section,
.check-section {
  margin-bottom: 1.5rem;
}

.download-section h3,
.restart-section h3,
.restarting-section h3,
.error-section h3,
.check-section h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.2rem;
  color: var(--n-text-color);
}

.progress-container {
  margin: 1rem 0;
}

.progress-message {
  margin-top: 0.5rem;
  font-size: 14px;
  color: var(--n-text-color-2);
  text-align: center;
}

.error-message {
  color: var(--n-color-error);
  margin: 0;
  line-height: 1.6;
}

/* GitHub Star 链接 */
.github-star {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 1.5rem 0;
  color: #333;
  text-decoration: none;
  font-weight: 500;
  padding: 0.5rem;
  border-radius: 8px;
  transition: all 0.2s;
}

.github-star:hover {
  background: #f5f5f5;
  color: #007bff;
}

.github-star svg {
  width: 24px;
  height: 24px;
}

.modal-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 1.5rem;
  gap: 1rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
  font-size: 14px;
  min-width: 120px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background-color: #007bff;
  color: white;
  border: 2px solid #007bff;
}

.btn-primary:hover:not(:disabled) {
  background-color: #0056b3;
  border-color: #0056b3;
  transform: translateY(-1px);
}

.btn-secondary {
  background-color: #28a745;
  color: white;
  border: 2px solid #28a745;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #218838;
  border-color: #218838;
  transform: translateY(-1px);
}

/* 联系方式区域样式 */
.contact-section {
  margin-top: 1.5rem;
  margin-bottom: 1.5rem;
}

.contact-section h4 {
  margin: 0 0 1rem 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--n-text-color);
}

.contact-links {
  display: flex;
  gap: 1rem;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  cursor: pointer;
  color: #666;
  text-decoration: none;
  transition: all 0.2s;
  background: #f9f9f9;
}

.contact-item:hover {
  color: #007bff;
  border-color: #007bff;
  background: #f0f7ff;
  transform: translateY(-1px);
}

.contact-item svg {
  width: 20px;
  height: 20px;
}

.contact-item span {
  font-size: 14px;
  font-weight: 500;
}

/* 微信二维码弹窗样式 */
.qr-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.qr-modal {
  background: var(--n-color);
  padding: 2rem;
  border-radius: 16px;
  max-width: 600px;
  width: 90%;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  position: relative;
  max-height: 80vh;
  overflow-y: auto;
}

.qr-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.qr-header h3 {
  font-size: 1.5rem;
  font-weight: bold;
  margin: 0;
  color: var(--n-text-color);
}

.qr-header .close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--n-text-color-2);
  border-radius: 4px;
  padding: 4px;
  transition: all 0.2s;
}

.qr-header .close-btn:hover {
  background: var(--n-color-hover);
  color: var(--n-text-color);
}

.qr-content {
  text-align: center;
}

.qr-placeholder {
  margin-bottom: 1rem;
  display: flex;
  justify-content: center;
}

.qr-image {
  width: 200px;
  height: 200px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.qr-placeholder svg {
  width: 200px;
  height: 200px;
}

.qr-content p {
  margin: 0.5rem 0;
  color: var(--n-text-color-2);
  line-height: 1.6;
}

.wechat-id {
  margin-top: 1rem;
  padding: 0.5rem 1rem;
  background: #f0f7ff;
  border: 1px solid #007bff;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #007bff;
  display: inline-block;
}
</style> 