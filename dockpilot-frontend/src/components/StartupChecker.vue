<template>
  <div v-if="!isBackendReady" class="startup-overlay">
    <div class="startup-container">
      <div class="logo-section">
        <img :src="logo" class="startup-logo" alt="DockPilot" />
        <h1 class="startup-title">DockPilot</h1>
        <p class="startup-subtitle">正在启动中...</p>
      </div>
      
      <div class="progress-section">
        <n-progress 
          type="line" 
          :percentage="progress" 
          :show-indicator="false"
          status="info"
          :height="4"
        />
        <div class="progress-text">
          <span class="progress-message">{{ statusMessage }}</span>
          <span class="progress-percent">{{ progress }}%</span>
        </div>
      </div>
      
      <div class="status-section">
        <n-space vertical size="small">
          <div class="status-item" :class="{ 'completed': frontendReady }">
            <n-icon :component="frontendReady ? CheckmarkCircle : Time" />
            <span>前端服务</span>
            <span class="status">{{ frontendReady ? '✓ 就绪' : '启动中...' }}</span>
          </div>
          <div class="status-item" :class="{ 'completed': backendReady }">
            <n-icon :component="backendReady ? CheckmarkCircle : Time" />
            <span>后端服务</span>
            <span class="status">{{ backendReady ? '✓ 就绪' : '启动中...' }}</span>
          </div>
        </n-space>
      </div>
      
      <div class="tips-section">
        <p class="startup-tip">
          <n-icon :component="InformationCircle" />
          初次启动可能需要1-2分钟，请稍候...
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { NProgress, NSpace, NIcon } from 'naive-ui'
import { CheckmarkCircle, Time, InformationCircle } from '@vicons/ionicons5'
import logo from '@/assets/icons/logo.svg'

// 响应式数据
const isBackendReady = ref(false)
const frontendReady = ref(true) // 前端已经启动了
const backendReady = ref(false)
const progress = ref(0)
const statusMessage = ref('正在检查服务状态...')
const checkInterval = ref<NodeJS.Timeout | null>(null)
const startTime = ref(Date.now())

// 检查后端健康状态
const checkBackendHealth = async (): Promise<boolean> => {
  try {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 5000)
    
    const response = await fetch('/api/update/version', {
      method: 'GET',
      signal: controller.signal,
      headers: {
        'Accept': 'application/json'
      }
    })
    
    clearTimeout(timeoutId)
    return response.ok
  } catch (error) {
    return false
  }
}

// 更新进度和状态
const updateProgress = () => {
  const elapsed = Date.now() - startTime.value
  const estimatedTotal = 90000 // 预估90秒启动时间
  
  if (!backendReady.value) {
    // 基于时间的进度估算
    const timeProgress = Math.min((elapsed / estimatedTotal) * 80, 80)
    progress.value = Math.floor(timeProgress)
    
    if (elapsed < 30000) {
      statusMessage.value = '正在启动后端服务...'
    } else if (elapsed < 60000) {
      statusMessage.value = '正在初始化数据库连接...'
    } else {
      statusMessage.value = '正在加载应用组件...'
    }
  } else {
    progress.value = 100
    statusMessage.value = '启动完成，即将进入系统'
  }
}

// 执行健康检查
const performHealthCheck = async () => {
  try {
    const healthy = await checkBackendHealth()
    
    if (healthy && !backendReady.value) {
      console.log('✅ 后端服务已就绪')
      backendReady.value = true
      progress.value = 100
      statusMessage.value = '启动完成，即将进入系统'
      
      // 短暂延迟后隐藏启动页面
      setTimeout(() => {
        isBackendReady.value = true
      }, 1500)
    }
  } catch (error) {
    console.warn('健康检查失败:', error)
  }
  
  updateProgress()
}

// 开始健康检查
const startHealthCheck = () => {
  // 立即执行一次检查
  performHealthCheck()
  
  // 每2秒检查一次
  checkInterval.value = setInterval(performHealthCheck, 2000)
  
  // 最多检查2分钟，避免无限等待
  setTimeout(() => {
    if (!isBackendReady.value) {
      console.warn('⚠️ 启动超时，强制进入系统')
      isBackendReady.value = true
    }
  }, 120000)
}

// 生命周期
onMounted(() => {
  console.log('🚀 启动检查器已挂载，开始健康检查...')
  startHealthCheck()
})

onUnmounted(() => {
  if (checkInterval.value) {
    clearInterval(checkInterval.value)
  }
})

// 暴露给父组件
defineExpose({
  isBackendReady
})
</script>

<style scoped>
.startup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.startup-container {
  text-align: center;
  max-width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.logo-section {
  margin-bottom: 40px;
}

.startup-logo {
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
  animation: pulse 2s infinite;
}

.startup-title {
  font-size: 32px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px 0;
}

.startup-subtitle {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.progress-section {
  margin-bottom: 32px;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  font-size: 14px;
}

.progress-message {
  color: #666;
}

.progress-percent {
  color: #333;
  font-weight: 600;
}

.status-section {
  margin-bottom: 24px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  border-radius: 8px;
  background: #f8f9fa;
  color: #666;
  transition: all 0.3s ease;
}

.status-item.completed {
  background: #e8f5e8;
  color: #2c5530;
}

.status-item .n-icon {
  font-size: 18px;
}

.status-item span:nth-child(2) {
  flex: 1;
  text-align: left;
}

.status-item .status {
  font-size: 12px;
  font-weight: 500;
}

.tips-section {
  border-top: 1px solid #eee;
  padding-top: 20px;
}

.startup-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 14px;
  color: #666;
  justify-content: center;
}

.startup-tip .n-icon {
  font-size: 16px;
  color: #4a9eff;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .startup-container {
    margin: 20px;
    padding: 32px 24px;
  }
  
  .startup-logo {
    width: 64px;
    height: 64px;
  }
  
  .startup-title {
    font-size: 28px;
  }
}
</style> 