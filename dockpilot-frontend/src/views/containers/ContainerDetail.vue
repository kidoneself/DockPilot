<template>
  <div class="container-detail">
    <NCard>
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <NButton @click="router.back()">
              <template #icon>
                <NIcon><ArrowBackOutline /></NIcon>
              </template>
              返回
            </NButton>
            <div class="title-section">
              <div class="title-and-status">
                <span class="title" :title="container?.containerName || container?.containerId || '容器详情'">
                  {{ container?.containerName || container?.containerId?.slice(0, 12) || '容器详情' }}
                </span>
                <NTag v-if="container" :type="getStatusType(container.status)" size="small" class="status-tag">
                  {{ container.status }}
                </NTag>
              </div>
              <div 
                v-if="container?.lastError && container?.operationStatus === 'failed'" 
                class="error-info"
              >
                <NIcon class="error-icon"><WarningOutline /></NIcon>
                <span class="error-text">{{ container.lastError }}</span>
              </div>
            </div>
          </div>
          <div class="header-right">
            <NButton :loading="wsLoading" @click="handleRefresh">
              <template #icon>
                <NIcon><RefreshOutline /></NIcon>
              </template>
              刷新
            </NButton>
            <NButton type="primary" style="margin-left: 8px;" @click="handleEdit">
              <template #icon>
                <NIcon><CreateOutline /></NIcon>
              </template>
              编辑
            </NButton>
          </div>
        </div>
      </template>

      <NTabs type="line" animated>
        <!-- 基本信息标签页 -->
        <NTabPane name="basic" tab="基本信息">
          <div class="info-section-wrapper">
            <!-- 资源使用分组 -->
            <NCard size="small" style="margin-bottom: 24px;">
              <div class="resource-section">
                <div class="resource-item">
                  <span class="resource-label">CPU使用率</span>
                  <NProgress
                    type="line"
                    :percentage="container?.cpuPercent || 0"
                    :color="getCpuColor(container?.cpuPercent || 0)"
                    :indicator-placement="'inside'"
                  />
                  <span class="resource-value">
                    {{ container?.cpuPercent?.toFixed(2) || 0 }}%
                  </span>
                </div>
                <div class="resource-item">
                  <span class="resource-label">内存使用</span>
                  <NProgress
                    type="line"
                    :percentage="getMemoryPercentage(container)"
                    :color="getMemoryColor(container)"
                    :indicator-placement="'inside'"
                  />
                  <span class="resource-value">{{ container?.memory || 'N/A' }}</span>
                </div>
                <div class="resource-item">
                  <span class="resource-label">网络上传</span>
                  <span class="resource-value">{{ container?.upload || '0.00KB' }}</span>
                  <span style="margin-left: 32px;" class="resource-label">网络下载</span>
                  <span class="resource-value">{{ container?.download || '0.00KB' }}</span>
                </div>
              </div>
            </NCard>
            <!-- 基本信息部分 -->
            <n-descriptions
              label-placement="left"
              bordered
              :column="1"
              :label-style="{ width: '200px' }"
            >
              <n-descriptions-item label="容器ID">
                {{ container?.containerId || '-' }}
              </n-descriptions-item>
              <n-descriptions-item label="容器名称">
                {{ container?.containerName || '-' }}
              </n-descriptions-item>
              <n-descriptions-item label="镜像名称">
                <span>{{ container?.imageName || '-' }}</span>
                <NIcon
                  style="
                    cursor: pointer; 
                    margin-left: 8px; 
                    font-size: 14px; 
                    vertical-align: middle;
                  "
                  @click="copyToClipboard(container?.imageName || '-')"
                >
                  <CopyOutline />
                </NIcon>
              </n-descriptions-item>
              <n-descriptions-item label="镜像ID">
                {{ container?.imageId || '-' }}
              </n-descriptions-item>
              <n-descriptions-item label="创建时间">
                {{ container?.createdTime || '-' }}
              </n-descriptions-item>
              <n-descriptions-item label="状态">
                <NTag
                  v-if="container"
                  :type="getStatusType(container.status)"
                  size="small"
                  round
                >
                  {{ container.status }}
                </NTag>
              </n-descriptions-item>
              <n-descriptions-item label="重启次数">
                {{ container?.restartCount !== undefined ? container.restartCount : '-' }}
              </n-descriptions-item>
            </n-descriptions>
          </div>
        </NTabPane>

        <!-- 高级信息标签页 -->
        <NTabPane name="advanced" tab="高级信息">
          <div class="info-section-wrapper">
            <!-- 存储分组 -->
            <NCard size="small" style="margin-bottom: 24px;">
              <n-descriptions
                label-placement="left"
                bordered
                :column="1"
                :label-style="{ width: '200px' }"
              >
                <n-descriptions-item label="挂载卷">
                  <n-data-table
                    :columns="volumeColumns"
                    :data="container?.volumes || []"
                    size="small"
                    :bordered="false"
                    style="background: transparent"
                    :header-cell-style="() => ({ color: 'var(--n-text-color-disabled)' })"
                  />
                </n-descriptions-item>
              </n-descriptions>
            </NCard>
            <!-- 网络分组 -->
            <NCard size="small" style="margin-bottom: 24px;">
              <n-descriptions
                label-placement="left"
                bordered
                :column="1"
                :label-style="{ width: '200px' }"
              >
                <n-descriptions-item label="网络模式">
                  {{ container?.networkMode || '-' }}
                </n-descriptions-item>
                <n-descriptions-item label="IP 地址">
                  {{ container?.ipAddress || '-' }}
                </n-descriptions-item>
                <n-descriptions-item label="端口映射">
                  <template v-if="container?.ports && container.ports.length > 0">
                    <div 
                      v-for="(port, index) in container.ports" 
                      :key="index" 
                      style="margin-bottom: 8px;"
                    >
                      <span>主机端口: {{ port.split(':')[1] }}</span>
                      <NIcon 
                        style="cursor: pointer; margin-left: 4px; font-size: 14px;" 
                        @click="copyToClipboard(port.split(':')[1])"
                      >
                        <CopyOutline />
                      </NIcon>
                      <span style="margin-left: 16px;">容器端口: {{ port.split('/')[0] }}</span>
                      <span style="margin-left: 16px;">
                        协议: {{ port.split('/')[1].split(':')[0] }}
                      </span>
                    </div>
                  </template>
                  <template v-else>--</template>
                </n-descriptions-item>
                <n-descriptions-item label="暴露端口">
                  <template v-if="container?.exposedPorts && container.exposedPorts.length > 0">
                    <div 
                      v-for="(port, index) in container.exposedPorts" 
                      :key="index" 
                      style="margin-bottom: 8px;"
                    >
                      <span>端口: {{ port.split('/')[0] }}</span>
                      <span style="margin-left: 16px;">协议: {{ port.split('/')[1] }}</span>
                    </div>
                  </template>
                  <template v-else>--</template>
                </n-descriptions-item>
              </n-descriptions>
            </NCard>
            <!-- 配置分组 -->
            <NCard size="small" style="margin-bottom: 24px;">
              <n-descriptions
                label-placement="left"
                bordered
                :column="1"
                :label-style="{ width: '200px' }"
              >
                <n-descriptions-item label="Command">
                  <template v-if="container?.command && container.command.length > 0">
                    <span 
                      v-for="(cmd, index) in container.command" 
                      :key="index" 
                      style="margin-right: 8px; margin-bottom: 4px;"
                    >
                      {{ cmd }}
                    </span>
                  </template>
                  <template v-else>--</template>
                </n-descriptions-item>
                <n-descriptions-item label="Entrypoints">
                  <template v-if="container?.entrypoints && container.entrypoints.length > 0">
                    <span 
                      v-for="(ep, index) in container.entrypoints" 
                      :key="index" 
                      style="margin-right: 8px; margin-bottom: 4px;"
                    >
                      {{ ep }}
                    </span>
                  </template>
                  <template v-else>--</template>
                </n-descriptions-item>
                <n-descriptions-item label="工作目录">
                  {{ container?.workingDir || '-' }}
                </n-descriptions-item>
                <n-descriptions-item label="重启策略">
                  {{ container?.restartPolicyName || '-' }}
                  <span 
                    v-if="
                      container?.restartPolicyMaxRetry !== undefined && 
                      container?.restartPolicyMaxRetry > 0
                    " 
                    label="重启次数限制"
                  >
                    (最大重试次数: {{ container.restartPolicyMaxRetry }})
                  </span>
                </n-descriptions-item>
                <n-descriptions-item label="特权模式 Privileged">
                  {{ container?.privileged ? '是' : '否' }}
                </n-descriptions-item>
              </n-descriptions>
            </NCard>
            <!-- 设备分组 -->
            <NCard size="small">
              <n-descriptions
                label-placement="left"
                bordered
                :column="1"
                :label-style="{ width: '200px' }"
              >
                <n-descriptions-item label="设备映射">
                  <template v-if="container?.devices && container.devices.length > 0">
                    <div 
                      v-for="(device, index) in container.devices" 
                      :key="index" 
                      style="margin-bottom: 8px;"
                    >
                      {{ device }}
                    </div>
                  </template>
                  <template v-else>--</template>
                </n-descriptions-item>
              </n-descriptions>
            </NCard>
          </div>
        </NTabPane>

        <!-- 环境变量与标签标签页 -->
        <NTabPane name="env-labels" tab="环境变量">
          <div class="config-section">
            <n-descriptions
              label-placement="left"
              bordered
              :column="1"
              :label-style="{ width: '200px' }"
            >
              <template v-if="container?.envs && container.envs.length > 0">
                <n-descriptions-item
                  v-for="(env, index) in container.envs"
                  :key="index"
                  :label="env.split('=')[0]"
                >
                  <span>{{ env.split('=').slice(1).join('=') }}</span>
                  <NIcon
                    style="
                      cursor: pointer; 
                      margin-left: 8px; 
                      font-size: 14px; 
                      vertical-align: middle;
                    "
                    @click="copyToClipboard(env.split('=').slice(1).join('='))"
                  >
                    <CopyOutline />
                  </NIcon>
                </n-descriptions-item>
              </template>
              <template v-else>
                <n-descriptions-item label="环境变量">--</n-descriptions-item>
              </template>
            </n-descriptions>
            <!-- 标签部分保留原有分组 -->
            <!-- <div class="config-group" style="margin-top: 24px;">
              <h3>标签 Labels</h3>
              <div v-if="container?.labels && Object.keys(container.labels).length > 0">
                <div 
                  v-for="(value, key) in container.labels" 
                  :key="key" 
                  class="config-item label-item"
                >
                  <strong>{{ key }}:</strong> {{ value }}
                </div>
              </div>
              <div v-else class="config-item">无标签</div>
            </div> -->
          </div>
        </NTabPane>
      </NTabs>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NButton, NCard, NTabs, NTabPane, NTag, NProgress, NIcon, useMessage } from 'naive-ui'
import { 
  ArrowBackOutline, 
  RefreshOutline, 
  CopyOutline, 
  CreateOutline, 
  WarningOutline 
} from '@vicons/ionicons5'
import { useWebSocketTask } from '@/hooks/useWebSocketTask'
import { MessageType } from '@/api/websocket/types'
import { getContainerStats } from '@/api/container'

const router = useRouter()
const route = useRoute()
const message = useMessage()

const container = ref<any>(null)
let statsTimer: number | null = null
const STATS_UPDATE_INTERVAL = 5000 // 5秒

const {
  loading: wsLoading,
  start: fetchContainerDetail
} = useWebSocketTask({
  type: MessageType.CONTAINER_DETAIL,
  data: { containerId: route.params.id },
  onComplete: (msg) => {
    container.value = msg.data
    // 如果容器是运行状态，开始获取stats
    if (container.value?.status === 'running') {
      startStatsTimer()
    }
  },
  onError: (err) => {
    message.error('获取容器详情失败: ' + err)
  }
})

// 获取容器状态对应的 Tag 类型 (从列表页复制)
function getStatusType(status: string) {
  switch (status) {
    case 'running':
      return 'success'
    case 'exited':
      return 'warning'
    default:
      return 'info'
  }
}

// 获取 CPU 使用率对应的颜色 (从列表页复制)
function getCpuColor(cpu: number) {
  if (cpu < 50) return '#18a058'
  if (cpu < 80) return '#f0a020'
  return '#d03050'
}

// 计算内存使用百分比 (从列表页复制)
function getMemoryPercentage(container: any | null) {
  if (
    container?.memoryUsage !== undefined && 
    container?.memoryLimit !== undefined && 
    container.memoryLimit > 0
  ) {
    return Number(((container.memoryUsage / container.memoryLimit) * 100).toFixed(2))
  }
  return 0
}

// 获取内存使用百分比对应的颜色 (从列表页复制)
function getMemoryColor(container: any | null) {
  const percentage = getMemoryPercentage(container)
  if (percentage < 50) return '#18a058'
  if (percentage < 80) return '#f0a020'
  return '#d03050'
}

// 获取容器性能数据
async function loadContainerStats() {
  if (!container.value?.containerId) return
  
  try {
    await getContainerStats(container.value.containerId, {
      onComplete: (stats) => {
        if (container.value) {
          const s = stats.data || {}
          // 更新容器的性能数据
          container.value.cpuPercent = s.cpuPercent ? Number(s.cpuPercent.toFixed(2)) : 0
          container.value.memoryUsage = s.memoryUsage || 0
          container.value.memoryLimit = s.memoryLimit || 0
          container.value.memory = formatBytes(s.memoryUsage || 0)
          container.value.upload = formatSpeed(s.networkTx || 0)
          container.value.download = formatSpeed(s.networkRx || 0)
        }
      },
      onError: (error) => {
        console.error('获取容器性能数据失败:', error)
      }
    })
  } catch (error) {
    console.error('获取容器性能数据失败:', error)
  }
}

// 启动性能数据定时器
function startStatsTimer() {
  // 先清除已有的定时器
  if (statsTimer) {
    clearInterval(statsTimer)
    statsTimer = null
  }

  // 只有运行中的容器才获取stats
  if (container.value?.status !== 'running') {
    return
  }

  // 立即获取一次
  loadContainerStats()

  // 设置定时器
  statsTimer = window.setInterval(() => {
    loadContainerStats()
  }, STATS_UPDATE_INTERVAL)
}

// 停止性能数据定时器
function stopStatsTimer() {
  if (statsTimer) {
    clearInterval(statsTimer)
    statsTimer = null
  }
}

// 格式化字节数
function formatBytes(bytes: number, decimals = 2) {
  if (!bytes) return '0 Bytes'
  const k = 1024
  const dm = decimals < 0 ? 0 : decimals
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(dm) + ' ' + sizes[i]
}

// 格式化传输速度
function formatSpeed(val: number) {
  if (val > 1024 * 1024) return (val / 1024 / 1024).toFixed(2) + 'MB/s'
  if (val > 1024) return (val / 1024).toFixed(2) + 'KB/s'
  return val + 'B/s'
}

// 刷新按钮点击时重新加载静态和动态数据
function handleRefresh() {
  fetchContainerDetail()
}

// 跳转到编辑页面
function handleEdit() {
  if (container.value?.containerId) {
    router.push(`/containers/${container.value.containerId}/edit`)
  }
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    message.success('复制成功')
  }).catch(err => {
    message.error('复制失败: ' + err)
  })
}

const volumeColumns = [
  {
    title: '主机路径',
    key: 'hostPath',
    render(row: any) {
      return [
        row.hostPath,
        h(
          NIcon,
          {
            style: 'cursor:pointer; margin-left:4px; font-size:14px; verticalAlign:middle;',
            onClick: () => copyToClipboard(row.hostPath)
          },
          { default: () => h(CopyOutline) }
        )
      ]
    }
  },
  { title: '容器路径', key: 'containerPath' },
  {
    title: '只读',
    key: 'readOnly',
    render(row: any) {
      return row.readOnly ? '是' : '否'
    }
  }
]

onMounted(() => {
  fetchContainerDetail()
})

onUnmounted(() => {
  // 停止性能数据定时器
  stopStatsTimer()
})
</script>

<style scoped>
.container-detail {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.title-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.title-and-status {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 32px;
}

.title {
  font-size: 18px;
  font-weight: bold;
  color: var(--n-text-color-base);
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 1;
}

.status-tag {
  flex-shrink: 0;
}

.error-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background-color: rgba(208, 48, 80, 0.1);
  border-radius: 4px;
  border-left: 3px solid #d03050;
}

.error-icon {
  color: #d03050;
  font-size: 14px;
}

.error-text {
  color: #d03050;
  font-size: 13px;
  font-weight: 500;
}

/* 调整这些区域的样式，使其有外包围感 */
.info-section-wrapper,
.resource-section-wrapper,
.config-section {
  padding: 16px;
  border-radius: 8px;
  background-color: var(--n-card-color);
  margin-bottom: 20px;
  border: 1px solid var(--n-border-color);
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--n-border-color);
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  color: var(--n-text-color-3);
  min-width: 100px; /* 增加标签最小宽度 */
  font-weight: 500;
}

.resource-section-wrapper {
   /* 资源使用部分内部的 flex 布局 */
}

.resource-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  /* 移除这里的 padding，由 wrapper 提供 */
  /* padding: 16px; */
}

.resource-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.resource-label {
  min-width: 120px; /* 增加标签最小宽度 */
  color: var(--n-text-color-3);
  font-weight: 500;
}

.resource-value {
   min-width: 80px; /* 确保值有最小宽度 */
  color: var(--n-text-color-base);
  font-weight: normal;
}

.config-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  /* background-color: var(--n-color); */ /* 这个由 wrapper 提供 */
}

.config-group {
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  padding: 16px;
  background-color: var(--n-color);
}

.config-group h3 {
  margin: 0 0 12px 0;
  color: var(--n-text-color-base);
  font-size: 16px;
  border-bottom: 1px solid var(--n-border-color);
  padding-bottom: 8px;
}

.config-item {
  padding: 8px 0;
  color: var(--n-text-color-base);
  word-break: break-all; /* 长文本换行 */
}

.port-item,
.volume-item,
.env-item,
.device-item,
.label-item {
  border-bottom: 1px dashed var(--n-border-color);
  padding: 8px 0;
}

.port-item:last-child,
.volume-item:last-child,
.env-item:last-child,
.device-item:last-child,
.label-item:last-child {
  border-bottom: none;
}

.volume-item span {
  margin-right: 20px; /* 增加卷信息项之间的间隔 */
}

.label-item strong {
  margin-right: 8px;
  color: var(--n-text-color-base);
}

:deep(.n-tabs .n-tabs-pane) {
  padding: 0;
}

/* 可以根据需要添加更多响应式样式 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .header-left {
    width: 100%;
    justify-content: space-between;
  }
  .header-right {
    width: 100%;
    justify-content: flex-end;
  }

   .info-section {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
   }

   .resource-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
   }
   .resource-label {
    min-width: auto;
   }
   .resource-value {
     min-width: auto;
   }
    .n-progress {
     width: 100%;
   }
}

</style> 