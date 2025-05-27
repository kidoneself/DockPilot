<template>
  <div class="stats-section">
    <div class="section-header">
      <h3>系统状态</h3>
      <div class="header-actions">
        <n-button text size="tiny" @click="showDetailedInfo = !showDetailedInfo">
          {{ showDetailedInfo ? '收起详情' : '查看详情' }}
        </n-button>
      </div>
    </div>
    
    <!-- 紧凑型性能卡片 -->
    <div class="compact-stats-grid">
      <div 
        v-for="stat in systemStats" 
        :key="stat.title"
        class="compact-stat-card"
      >
        <div class="compact-stat-content">
          <div class="stat-icon-small" :style="{ color: stat.color }">
            <n-icon :size="14" :component="stat.icon" />
          </div>
          <div class="stat-main">
            <div class="stat-title-small">{{ stat.title }}</div>
            <div class="stat-value-large">{{ stat.value }}</div>
            <div v-if="stat.total || stat.free" class="stat-extra-small">
              <span v-if="stat.total">/ {{ stat.total }}</span>
              <span v-if="stat.free" class="free-space-small">剩余 {{ stat.free }}</span>
            </div>
          </div>
          
          <!-- 网速卡片：显示实时指示器而不是百分比 -->
          <div v-if="stat.title === '网络速度'" class="stat-network-indicator">
            <div v-if="stat.value.includes('未知') || stat.value.includes('初始化') || stat.value.includes('计算中') || stat.value.includes('获取失败')" class="network-status-text">
              <span class="status-text">{{ stat.value.includes('未知') ? '检测中' : stat.value.includes('初始化') ? '准备中' : stat.value.includes('计算中') ? '计算中' : '获取失败' }}</span>
            </div>
            <div v-else class="network-status" :class="{ active: stat.percentage > 0 }">
              <div class="signal-bars">
                <div class="bar" :class="{ active: stat.percentage >= 25 }"></div>
                <div class="bar" :class="{ active: stat.percentage >= 50 }"></div>
                <div class="bar" :class="{ active: stat.percentage >= 75 }"></div>
                <div class="bar" :class="{ active: stat.percentage >= 100 }"></div>
              </div>
            </div>
          </div>
          
          <!-- 其他卡片：显示百分比圆形进度条 -->
          <div v-else class="stat-percentage">
            <div class="percentage-text">{{ stat.percentage.toFixed(0) }}%</div>
            <n-progress
              type="circle"
              :percentage="stat.percentage"
              :stroke-width="8"
              :show-indicator="false"
              :color="stat.color"
              :rail-color="'rgba(255,255,255,0.1)'"
              style="width: 32px; height: 32px;"
            />
          </div>
        </div>
      </div>
    </div>
    
    <!-- 详细系统信息 - 可折叠 -->
    <div v-show="showDetailedInfo" class="detailed-info-section">
      <div class="info-summary">
        <div class="summary-item">
          <span class="summary-label">主机</span>
          <span class="summary-value">{{ systemInfo.hostname }} ({{ systemInfo.os }})</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">硬件</span>
          <span class="summary-value">{{ systemInfo.cpuCores }}核心 {{ systemInfo.cpuModel }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">运行</span>
          <span class="summary-value">{{ systemInfo.uptime }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">网络</span>
          <span class="summary-value">{{ systemInfo.ipAddress }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">网速</span>
          <span class="summary-value">{{ systemStats[4]?.value || '初始化中...' }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">Docker</span>
          <span class="summary-value">{{ systemInfo.dockerVersion }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">内核</span>
          <span class="summary-value">{{ systemInfo.kernel }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, markRaw } from 'vue'
import { useMessage } from 'naive-ui'
import { getSystemStatus } from '@/api/system'
import {
  CubeOutline,
  StatsChartOutline,
  GlobeOutline
} from '@vicons/ionicons5'

const message = useMessage()

// 系统统计数据
const systemStats = ref([
  {
    title: 'CPU使用率',
    value: '0%',
    percentage: 0,
    color: '#ef4444',
    icon: markRaw(StatsChartOutline),
    description: '处理器负载'
  },
  {
    title: '内存使用',
    value: '0MB',
    total: '0MB',
    percentage: 0,
    color: '#f59e0b',
    icon: markRaw(StatsChartOutline),
    description: '系统内存'
  },
  {
    title: '磁盘使用',
    value: '0%',
    free: '0GB',
    percentage: 0,
    color: '#8b5cf6',
    icon: markRaw(StatsChartOutline),
    description: '存储空间'
  },
  {
    title: '运行容器',
    value: '0',
    total: '0',
    percentage: 0,
    color: '#10b981',
    icon: markRaw(CubeOutline),
    description: 'Docker容器'
  },
  {
    title: '网络速度',
    value: '初始化中...',
    percentage: 0,
    color: '#06b6d4',
    icon: markRaw(GlobeOutline),
    description: '实时网速'
  }
])

// 系统基础信息
const systemInfo = ref({
  hostname: '获取中...',
  os: '获取中...',
  kernel: '获取中...',
  uptime: '获取中...',
  cpuModel: '获取中...',
  cpuCores: 0,
  ipAddress: '获取中...',
  gateway: '获取中...',
  dockerVersion: '获取中...'
})

// 系统状态加载状态
const systemStatusLoading = ref(false)

// 详细信息显示状态
const showDetailedInfo = ref(false)

// 系统状态定时器
let systemStatusTimer: NodeJS.Timeout | null = null

// 获取系统状态
const loadSystemStatus = async () => {
  if (systemStatusLoading.value) return
  
  systemStatusLoading.value = true
  
  try {
    await getSystemStatus({
      onComplete: (data) => {
        const status = data.data
        if (status) {
          // 更新系统统计数据
          // 0. CPU使用率
          if (status.cpuUsage !== undefined) {
            systemStats.value[0].value = `${status.cpuUsage.toFixed(1)}%`
            systemStats.value[0].percentage = status.cpuUsage
          }
          
          // 1. 内存使用
          if (status.memoryTotal && status.memoryUsed) {
            const memoryUsedGB = (status.memoryUsed / 1024).toFixed(1)
            const memoryTotalGB = (status.memoryTotal / 1024).toFixed(1)
            systemStats.value[1].value = `${memoryUsedGB}GB`
            systemStats.value[1].total = `${memoryTotalGB}GB`
            systemStats.value[1].percentage = status.memoryUsage || 0
          }
          
          // 2. 磁盘使用
          if (status.diskUsage && status.diskFree) {
            systemStats.value[2].value = status.diskUsage
            systemStats.value[2].free = status.diskFree
            // 从百分比字符串中提取数字
            const diskPercent = parseInt(status.diskUsage.replace('%', ''))
            systemStats.value[2].percentage = isNaN(diskPercent) ? 0 : diskPercent
          }
          
          // 3. 运行容器
          systemStats.value[3].value = status.runningContainers?.toString() || '0'
          systemStats.value[3].total = status.totalContainers?.toString() || '0'
          systemStats.value[3].percentage = status.totalContainers > 0 
            ? Math.round((status.runningContainers / status.totalContainers) * 100) 
            : 0
          
          // 4. 网络速度
          if (status.networkDownloadSpeed && status.networkUploadSpeed) {
            // 检查是否为特殊状态
            if (status.networkDownloadSpeed.includes('初始化') || status.networkDownloadSpeed.includes('计算中') || status.networkDownloadSpeed.includes('未知') || status.networkDownloadSpeed.includes('获取失败')) {
              systemStats.value[4].value = status.networkDownloadSpeed
              systemStats.value[4].percentage = 0
            } else {
              systemStats.value[4].value = `↓${status.networkDownloadSpeed} ↑${status.networkUploadSpeed}`
              // 根据网速设置信号强度（用下载速度计算）
              const downloadSpeedRaw = status.networkDownloadSpeedRaw || 0
              
              // 信号强度分级：
              // 0: 无网络 (0KB/s)
              // 25: 低速 (0-100KB/s) 
              // 50: 中速 (100KB/s-1MB/s)
              // 75: 高速 (1MB/s-10MB/s)
              // 100: 极速 (>10MB/s)
              let signalStrength = 0
              if (downloadSpeedRaw > 0) {
                if (downloadSpeedRaw < 100 * 1024) {        // < 100KB/s
                  signalStrength = 25
                } else if (downloadSpeedRaw < 1024 * 1024) { // < 1MB/s
                  signalStrength = 50
                } else if (downloadSpeedRaw < 10 * 1024 * 1024) { // < 10MB/s
                  signalStrength = 75
                } else {                                      // >= 10MB/s
                  signalStrength = 100
                }
              }
              systemStats.value[4].percentage = signalStrength
            }
          } else {
            systemStats.value[4].value = '0KB/s'
            systemStats.value[4].percentage = 0
          }
          
          // 更新系统基础信息
          systemInfo.value = {
            hostname: status.hostname || '未知',
            os: status.os || '未知',
            kernel: status.kernel || '未知',
            uptime: status.uptime || '未知',
            cpuModel: status.cpuModel || '未知',
            cpuCores: status.cpuCores || 0,
            ipAddress: status.ipAddress || '未知',
            gateway: status.gateway || '未知',
            dockerVersion: status.dockerVersion || '未知'
          }
        }
      },
      onError: (error) => {
        console.error('获取系统状态失败:', error)
        message.error('获取系统状态失败: ' + error)
      }
    })
  } catch (error) {
    console.error('获取系统状态失败:', error)
    message.error('获取系统状态失败')
  } finally {
    systemStatusLoading.value = false
  }
}

// 定义暴露的方法，供父组件调用
defineExpose({
  loadSystemStatus
})

onMounted(async () => {
  // 立即加载一次系统状态
  await loadSystemStatus()
  
  // 启动系统状态定时刷新（5秒间隔）
  systemStatusTimer = setInterval(() => {
    loadSystemStatus()
  }, 5000)
})

onUnmounted(() => {
  if (systemStatusTimer) {
    clearInterval(systemStatusTimer)
  }
})
</script>

<style scoped>
.stats-section {
  margin-bottom: 16px;
  width: 100%;
  max-width: 1200px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  margin-bottom: 12px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 紧凑型性能卡片网格 */
.compact-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.compact-stat-card {
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  padding: 12px;
  transition: all 0.3s ease;
  backdrop-filter: blur(20px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.compact-stat-card:hover {
  background: rgba(0, 0, 0, 0.35);
  border-color: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.4);
}

.compact-stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon-small {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.5));
}

.stat-main {
  flex: 1;
  min-width: 0;
}

.stat-title-small {
  font-size: 10px;
  color: #e2e8f0;
  margin-bottom: 2px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.stat-value-large {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.2;
  margin-bottom: 1px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.7);
}

.stat-extra-small {
  font-size: 9px;
  color: #cbd5e1;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.free-space-small {
  color: #34d399;
  font-weight: 500;
}

.stat-percentage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.percentage-text {
  font-size: 9px;
  color: #e2e8f0;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

/* 网络速度指示器样式 */
.stat-network-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.network-status-text {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.status-text {
  font-size: 8px;
  color: #94a3b8;
  text-align: center;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.network-status {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.signal-bars {
  display: flex;
  align-items: end;
  gap: 2px;
  height: 16px;
}

.signal-bars .bar {
  width: 3px;
  border-radius: 1px;
  background: rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.signal-bars .bar:nth-child(1) { height: 4px; }
.signal-bars .bar:nth-child(2) { height: 7px; }
.signal-bars .bar:nth-child(3) { height: 10px; }
.signal-bars .bar:nth-child(4) { height: 13px; }

.signal-bars .bar.active {
  background: #06b6d4;
  box-shadow: 0 0 4px rgba(6, 182, 212, 0.4);
}

.network-status.active .signal-bars .bar.active {
  animation: pulse-bar 2s ease-in-out infinite;
}

@keyframes pulse-bar {
  0%, 100% {
    opacity: 1;
    transform: scaleY(1);
  }
  50% {
    opacity: 0.7;
    transform: scaleY(1.2);
  }
}

/* 详细系统信息 - 可折叠 */
.detailed-info-section {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.info-summary {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px;
  backdrop-filter: blur(20px);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 0;
}

.summary-label {
  font-size: 9px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.summary-value {
  font-size: 11px;
  color: #ffffff;
  font-weight: 500;
  word-break: break-all;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.7);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .compact-stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 6px;
  }
  
  .compact-stat-card {
    padding: 10px;
  }
  
  .stat-value-large {
    font-size: 14px;
  }
  
  .info-summary {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 6px;
  }
}

@media (max-width: 768px) {
  .compact-stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 6px;
  }
  
  .compact-stat-card {
    padding: 10px;
  }
  
  .compact-stat-content {
    gap: 8px;
  }
  
  .stat-icon-small {
    width: 20px;
    height: 20px;
  }
  
  .stat-value-large {
    font-size: 13px;
  }
  
  .stat-title-small {
    font-size: 9px;
  }
  
  .stat-extra-small {
    font-size: 8px;
  }
  
  .percentage-text {
    font-size: 8px;
  }
  
  .info-summary {
    grid-template-columns: 1fr;
    gap: 6px;
    padding: 10px;
  }
  
  .summary-item {
    padding: 4px 0;
  }
  
  .summary-label {
    font-size: 8px;
  }
  
  .summary-value {
    font-size: 10px;
  }
}

@media (max-width: 480px) {
  .section-header h3 {
    font-size: 14px;
  }
  
  .compact-stats-grid {
    grid-template-columns: 1fr;
    gap: 4px;
  }
  
  .compact-stat-card {
    padding: 8px;
  }
  
  .compact-stat-content {
    gap: 6px;
  }
  
  .stat-icon-small {
    width: 18px;
    height: 18px;
  }
  
  .stat-value-large {
    font-size: 11px;
  }
  
  .stat-title-small {
    font-size: 8px;
  }
  
  .stat-extra-small {
    font-size: 7px;
  }
  
  .percentage-text {
    font-size: 7px;
  }
  
  .info-summary {
    grid-template-columns: 1fr;
    gap: 4px;
    padding: 8px;
  }
  
  .summary-item {
    padding: 3px 0;
  }
  
  .summary-label {
    font-size: 7px;
  }
  
  .summary-value {
    font-size: 9px;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 4px;
  }
}
</style> 