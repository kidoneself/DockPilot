<template>
  <div class="container-list">
    <n-card>
      <template #header>
        <div class="header-content">
          <div class="header-actions">
            <NButton @click="handleRefresh" class="action-btn">
              <template #icon>
                <n-icon><RefreshOutline /></n-icon>
              </template>
              <span class="btn-text">刷新</span>
            </NButton>
            <NButton type="primary" @click="router.push('/containers/create')" class="action-btn">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
              <span class="btn-text">创建容器</span>
            </NButton>
          </div>
          <div class="header-search">
            <SearchBar v-model="searchText" placeholder="搜索容器" />
          </div>
        </div>
      </template>

      <NSpace vertical size="large">
        <ContainerItem
          v-for="container in filteredContainers"
          :key="container.id"
          :container="container"
          :operating="operatingContainers.has(container.id)"
          :current-action="containerActions.get(container.id)"
          @action="handleContainerAction"
        />
      </NSpace>

      <template #footer>
        <n-empty v-if="filteredContainers.length === 0" description="暂无容器" />
      </template>
    </n-card>

    <ContainerLogModal
      v-model:show="showLogModal"
      :title="logModalTitle"
      :logs="logModalLogs"
      @update:auto-scroll="handleLogAutoRefresh"
    />

    <!-- WebUI配置模态框 -->
    <NModal
      v-model:show="showWebUIModal"
      preset="dialog"
      title="配置容器WebUI"
      class="webui-modal"
    >
      <NForm
        ref="webUIFormRef"
        :model="webUIForm"
        :rules="webUIFormRules"
        label-placement="left"
        label-width="120px"
      >
        <NFormItem label="WebUI地址" path="webUrl">
          <NInput
            v-model:value="webUIForm.webUrl"
            placeholder="例如: http://localhost:8080"
            clearable
          />
        </NFormItem>
        <NFormItem label="图标地址（可选）" path="iconUrl">
          <NInput
            v-model:value="webUIForm.iconUrl"
            placeholder="例如: https://example.com/icon.png"
            clearable
          />
        </NFormItem>
      </NForm>
      <template #action>
        <NSpace>
          <NButton @click="showWebUIModal = false">取消</NButton>
          <NButton type="primary" @click="handleSaveWebUI" :loading="savingWebUI">
            保存
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { NButton, NSpace, useMessage, useDialog, NModal, NInput, NFormItem, NForm } from 'naive-ui'
import {
  RefreshOutline,
  AddOutline
} from '@vicons/ionicons5'
import ContainerItem from '@/components/container/ContainerItem.vue'
import ContainerLogModal from '@/components/container/ContainerLogModal.vue'
import SearchBar from '@/components/common/SearchBar.vue'
import {
  getContainerStats,
  startContainer,
  stopContainer,
  removeContainer,
  restartContainer,
  updateContainerInfo
} from '@/api/container'
import { sendWebSocketMessage } from '@/api/websocket/websocketService'
import { useRouter } from 'vue-router'
import { useWebSocketTask } from '@/hooks/useWebSocketTask'
import { MessageType } from '@/api/websocket/types'

const message = useMessage()
const dialog = useDialog()
const router = useRouter()

// 后端原始类型
interface BackendContainerItem {
  id: string
  names: string[]
  image: string
  imageId: string
  command: string
  created: number
  state: string
  status: string
  ports: any[]
  labels: Record<string, string>
  hostConfig: { networkMode: string }
  mounts: any[]
  lastError?: string
  webUrl?: string
  iconUrl?: string
}
// 前端展示类型
interface DisplayContainer {
  id: string
  name: string
  image: string
  status: string
  ports: string[]
  created: string
  cpu: number
  memory: string
  network: string
  lastError?: string
  project: string
  upload: string
  download: string
  names: string[]
  imageId: string
  command: string
  state: string
  mounts: any[]
  labels: Record<string, string>
  hostConfig: { networkMode: string }
  stats?: any
  memoryUsageRaw?: number
  memoryLimitRaw?: number
  webUrl?: string
  iconUrl?: string
}

// 性能数据类型
interface ContainerStatsData {
  cpuPercent?: number
  memoryUsage?: number
  memoryLimit?: number
  networkTx?: number
  networkRx?: number
}

const containers = ref<DisplayContainer[]>([])
const searchText = ref('')
const formRef = ref(null)
const selectedContainers = ref<Set<string>>(new Set())
const operatingContainers = ref<Set<string>>(new Set())
const containerActions = ref<Map<string, string>>(new Map())
const containerLogModal = ref<InstanceType<typeof ContainerLogModal> | null>(null)

let statsTimer: number | null = null
const STATS_UPDATE_INTERVAL = 5000 // 5秒

// 使用 useWebSocketTask 获取容器列表
const {
  start: getContainerList
} = useWebSocketTask({
  type: MessageType.CONTAINER_LIST,
  onComplete: (msg) => {
    const list = msg.data as BackendContainerItem[]
    if (Array.isArray(list)) {
      containers.value = list.map(item => ({
        id: item.id,
        name: Array.isArray(item.names) && item.names[0]
          ? item.names[0].replace(/^\//, '')
          : (item.id ? item.id.slice(0, 12) : '未知'),
        image: item.image,
        status: item.state,
        ports: Array.isArray(item.ports)
          ? item.ports.map(p => `${p.publicPort || p.PublicPort}:${p.privatePort || p.PrivatePort}`)
          : [],
        created: item.created ? new Date(item.created * 1000).toLocaleString() : '',
        cpu: 0,
        memory: '0MB',
        network: item.hostConfig?.networkMode || '',
        lastError: item.lastError || '',
        project: item.labels?.['org.opencontainers.image.title'] || '默认项目',
        upload: '0KB',
        download: '0KB',
        names: item.names,
        imageId: item.imageId,
        command: item.command,
        state: item.state,
        mounts: item.mounts,
        labels: item.labels,
        hostConfig: item.hostConfig,
        stats: undefined,
        webUrl: item.webUrl,
        iconUrl: item.iconUrl
      }))
      startStatsTimer()
    }
  },
  onError: (err) => {
    message.error('获取容器列表失败: ' + err)
  }
})

function formatBytes(bytes: number, decimals = 2) {
  if (!bytes) return '0 Bytes'
  const k = 1024
  const dm = decimals < 0 ? 0 : decimals
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(dm) + ' ' + sizes[i]
}

function formatNetworkBytes(val: number) {
  if (val > 1024 * 1024) return (val / 1024 / 1024).toFixed(2) + 'MB'
  if (val > 1024) return (val / 1024).toFixed(2) + 'KB'
  return val + 'B'
}

// 加载容器性能数据
async function loadContainerStats(containerId: string) {
  try {
    await getContainerStats(containerId, {
      onComplete: (stats) => {
        const container = containers.value.find(c => c.id === containerId)
        if (container) {
          const s = stats.data as ContainerStatsData || {}
          container.cpu = s.cpuPercent ? Number(s.cpuPercent.toFixed(2)) : 0
          if (s.memoryUsage !== undefined) {
            container.memoryUsageRaw = s.memoryUsage
            container.memory = formatBytes(s.memoryUsage)
          } else {
            container.memoryUsageRaw = undefined
            container.memory = 'N/A'
          }
          if (s.memoryLimit !== undefined) {
            container.memoryLimitRaw = s.memoryLimit
          } else {
            container.memoryLimitRaw = undefined
          }
          container.upload = formatNetworkBytes(s.networkTx ?? 0)
          container.download = formatNetworkBytes(s.networkRx ?? 0)
        }
      },
      onError: (error) => {
        console.error(`获取容器 ${containerId} 性能数据失败:`, error)
      }
    })
  } catch (error) {
    console.error(`获取容器 ${containerId} 性能数据失败:`, error)
  }
}

// 启动性能数据定时器
function startStatsTimer() {
  // 先清除已有的定时器，防止重复设置
  if (statsTimer) {
    clearInterval(statsTimer)
    statsTimer = null
  }

  // 获取运行中的容器列表
  const runningContainers = containers.value.filter(container => container.status === 'running')
  
  // 如果没有任何运行中的容器，直接返回
  if (runningContainers.length === 0) {
    return
  }

  // 当前要请求的容器索引
  let currentIndex = 0

  // 设置定时器，每次只请求一个容器的性能数据
  statsTimer = window.setInterval(() => {
    // 获取当前要请求的容器
    const container = runningContainers[currentIndex]
    
    // 请求该容器的性能数据
    loadContainerStats(container.id)
    
    // 更新索引，循环请求
    currentIndex = (currentIndex + 1) % runningContainers.length
  }, STATS_UPDATE_INTERVAL) // 修改定时器间隔，可以根据需要调整
}

// 前端过滤：支持名称和项目字段
const filteredContainers = computed(() => {
  if (!searchText.value) return containers.value
  return containers.value.filter(item =>
    (item.name && item.name.includes(searchText.value)) ||
    (item.project && item.project.includes(searchText.value))
  )
})

// 加载容器列表（替换原有 loadContainers）
function loadContainers() {
  getContainerList()
}

// 页面加载时自动获取
onMounted(() => {
  loadContainers()
})

// 在组件卸载时清除定时器
onUnmounted(() => {
  if (statsTimer) {
    clearInterval(statsTimer)
    statsTimer = null
  }
})

// 刷新按钮点击时重新加载
function handleRefresh() {
  loadContainers()
}

// 日志模态框相关
const showLogModal = ref(false)
const logModalTitle = ref('')
const logModalLogs = ref<string[]>([])
const selectedContainer = ref<DisplayContainer | null>(null)
const logAutoRefresh = ref(true)
let logTimer: number | null = null

function fetchContainerLogs(containerId: string) {
  // 发送WS请求
  sendWebSocketMessage({
    type: 'CONTAINER_LOGS',
    data: { containerId },
    callbacks: {
      onComplete: (msg) => {
        const logStr = typeof msg.data === 'string' ? msg.data : ''
        logModalLogs.value = (logStr || '').split('\n')
      },
      onError: (err: string) => {
        logModalLogs.value = ['[错误] ' + err]
        if (logTimer) {
          clearInterval(logTimer)
          logTimer = null
        }
      }
    },
    timeout: 30000
  })
}

function startLogAutoRefresh(containerId: string) {
  if (logTimer) clearInterval(logTimer)
  fetchContainerLogs(containerId)
  logTimer = window.setInterval(() => {
    fetchContainerLogs(containerId)
  }, 5000)
}
function stopLogAutoRefresh() {
  if (logTimer) {
    clearInterval(logTimer)
    logTimer = null
  }
}

// 监听日志模态框状态，暂停/恢复性能数据更新
watch(showLogModal, (val) => {
  if (val) {
    pauseStatsTimer()
  } else {
    resumeStatsTimer()
  }
})

function handleContainerAction(action: string, container: DisplayContainer) {
  // 如果容器正在操作中，直接返回
  if (operatingContainers.value.has(container.id)) {
    return
  }

  // 记录当前操作
  operatingContainers.value.add(container.id)
  containerActions.value.set(container.id, action)

  switch (action) {
    case 'start':
      startContainer(container.id, {
        onComplete: () => {
          message.success(`启动容器成功: ${container.name}`)
          loadContainers() // 重新加载容器列表以更新状态
          operatingContainers.value.delete(container.id) // 操作完成，移除容器ID
          containerActions.value.delete(container.id) // 清除操作类型
        },
        onError: (error) => {
          message.error(`${error}`)
          operatingContainers.value.delete(container.id) // 操作失败，移除容器ID
          containerActions.value.delete(container.id) // 清除操作类型
        }
      })
      break
    case 'stop':
      stopContainer(container.id, {
        onComplete: () => {
          message.success(`停止容器成功: ${container.name}`)
          loadContainers() // 重新加载容器列表以更新状态
          operatingContainers.value.delete(container.id) // 操作完成，移除容器ID
          containerActions.value.delete(container.id) // 清除操作类型
        },
        onError: (error) => {
          message.error(`停止容器失败: ${error}`)
          operatingContainers.value.delete(container.id) // 操作失败，移除容器ID
          containerActions.value.delete(container.id) // 清除操作类型
        }
      })
      break
    case 'restart':
      restartContainer(container.id, {
        onComplete: () => {
          message.success(`重启容器成功: ${container.name}`)
          loadContainers() // 重新加载容器列表以更新状态
      operatingContainers.value.delete(container.id) // 操作完成，移除容器ID
      containerActions.value.delete(container.id) // 清除操作类型
        },
        onError: (error) => {
          message.error(`重启容器失败: ${error}`)
          operatingContainers.value.delete(container.id) // 操作失败，移除容器ID
          containerActions.value.delete(container.id) // 清除操作类型
        }
      })
      break
    case 'delete':
      dialog.warning({
        title: '确认删除',
        content: `确定要删除容器 "${container.name}" 吗？此操作不可恢复。`,
        positiveText: '确定',
        negativeText: '取消',
        maskClosable: false,
        closeOnEsc: false,
        onClose: () => {
          operatingContainers.value.delete(container.id)
          containerActions.value.delete(container.id)
        },
        onPositiveClick: () => {
          removeContainer(container.id, {
            onComplete: () => {
              message.success(`删除容器成功: ${container.name}`)
              loadContainers() // 重新加载容器列表以更新状态
              operatingContainers.value.delete(container.id) // 操作完成，移除容器ID
              containerActions.value.delete(container.id) // 清除操作类型
            },
            onError: (error) => {
              message.error(`删除容器失败: ${error}`)
              operatingContainers.value.delete(container.id) // 操作失败，移除容器ID
              containerActions.value.delete(container.id) // 清除操作类型
            }
          })
        },
        onNegativeClick: () => {
          operatingContainers.value.delete(container.id) // 取消操作，移除容器ID
          containerActions.value.delete(container.id) // 清除操作类型
        }
      })
      break
    case 'logs':
      showLogModal.value = true
      selectedContainer.value = container
      logModalTitle.value = `容器日志 - ${container.name}`
      logModalLogs.value = []
      logAutoRefresh.value = true
      startLogAutoRefresh(container.id)
      operatingContainers.value.delete(container.id)
      containerActions.value.delete(container.id)
      break
    case 'detail':
      router.push(`/containers/${container.id}`)
      operatingContainers.value.delete(container.id)
      containerActions.value.delete(container.id)
      break
    case 'edit':
      router.push(`/containers/${container.id}/edit`)
      operatingContainers.value.delete(container.id)
      containerActions.value.delete(container.id)
      break
    case 'configWebUI':
      handleConfigWebUI(container)
      operatingContainers.value.delete(container.id)
      containerActions.value.delete(container.id)
      break
  }
}

function handleLogAutoRefresh(val: boolean) {
  logAutoRefresh.value = val
  if (val && selectedContainer.value) {
    startLogAutoRefresh(selectedContainer.value.id)
  } else {
    stopLogAutoRefresh()
  }
}

// WebUI配置相关函数
function handleConfigWebUI(container: DisplayContainer) {
  currentConfigContainer.value = container
  webUIForm.webUrl = container.webUrl || ''
  webUIForm.iconUrl = container.iconUrl || ''
  showWebUIModal.value = true
  
  // 重置表单验证状态
  nextTick(() => {
    webUIFormRef.value?.restoreValidation()
  })
}

function handleSaveWebUI() {
  if (!currentConfigContainer.value) {
    return
  }

  // 先进行表单验证
  webUIFormRef.value?.validate((errors: any) => {
    if (errors) {
      message.error('请检查输入的URL格式')
      return
    }

    // 验证通过，开始保存
    savingWebUI.value = true
    
    // 处理URL格式，确保有协议前缀
    let webUrl = webUIForm.webUrl
    let iconUrl = webUIForm.iconUrl
    
    if (webUrl && !webUrl.startsWith('http')) {
      webUrl = `http://${webUrl}`
    }
    if (iconUrl && !iconUrl.startsWith('http')) {
      iconUrl = `http://${iconUrl}`
    }

    updateContainerInfo(
      currentConfigContainer.value!.id,
      {
        onComplete: () => {
          message.success('WebUI配置保存成功')
          showWebUIModal.value = false
          savingWebUI.value = false
          
          // 更新本地数据
          if (currentConfigContainer.value) {
            currentConfigContainer.value.webUrl = webUrl
            currentConfigContainer.value.iconUrl = iconUrl
          }
        },
        onError: (error) => {
          message.error(`保存WebUI配置失败: ${error}`)
          savingWebUI.value = false
        }
      },
      webUrl,
      iconUrl
    )
  })
}

// 关闭日志模态框时自动停止定时器
watch(showLogModal, (val) => {
  if (!val) stopLogAutoRefresh()
})

// 暂停性能数据更新
function pauseStatsTimer() {
  if (statsTimer) {
    clearInterval(statsTimer)
    statsTimer = null
  }
}

// 恢复性能数据更新
function resumeStatsTimer() {
  startStatsTimer()
}

// WebUI配置相关状态
const showWebUIModal = ref(false)
const webUIForm = reactive({
  webUrl: '',
  iconUrl: ''
})
const savingWebUI = ref(false)
const currentConfigContainer = ref<DisplayContainer | null>(null)
const webUIFormRef = ref(null)

// URL验证函数
function validateUrl(url: string): boolean {
  if (!url) return true // 空值在required规则中处理
  
  // 移除协议前缀进行验证
  let cleanUrl = url.replace(/^https?:\/\//, '')
  
  // 检查是否为IP地址 (IPv4)
  const ipRegex = /^(\d{1,3}\.){3}\d{1,3}(:\d+)?(\/.*)?$/
  if (ipRegex.test(cleanUrl)) {
    // 验证IP地址的每个部分是否在0-255范围内
    const ipParts = cleanUrl.split('.').map(part => part.split(':')[0].split('/')[0])
    if (ipParts.length === 4 && ipParts.every(part => {
      const num = parseInt(part)
      return num >= 0 && num <= 255
    })) {
      return true
    }
  }
  
  // 检查是否为域名格式
  const domainRegex = /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*(\:[0-9]{1,5})?(\/.*)?$/
  if (domainRegex.test(cleanUrl)) {
    // 确保有域名部分（不只是端口和路径）
    const hasValidDomain = cleanUrl.includes('.') || cleanUrl === 'localhost'
    return hasValidDomain
  }
  
  return false
}

const webUIFormRules = {
  webUrl: [
    {
      required: true,
      message: '请输入WebUI地址',
      trigger: 'blur'
    },
    {
      validator: (rule: any, value: string) => {
        if (!value) return true
        if (!validateUrl(value)) {
          return new Error('请输入有效的URL地址\n支持格式: localhost:8080、192.168.1.1:3000、example.com/app')
        }
        return true
      },
      trigger: 'blur'
    }
  ],
  iconUrl: [
    {
      validator: (rule: any, value: string) => {
        if (!value) return true // 图标地址是可选的
        if (!validateUrl(value)) {
          return new Error('请输入有效的URL地址')
        }
        return true
      },
      trigger: 'blur'
    }
  ]
}
</script>

<style scoped>
.container-list {
  height: 100%;
}

/* 头部布局 */
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.header-search {
  min-width: 0;
  flex: 1;
  max-width: 300px;
}

.action-btn {
  white-space: nowrap;
}

:deep(.n-card) {
  background-color: var(--n-card-color);
}

/* WebUI配置模态框响应式 */
:deep(.webui-modal .n-dialog) {
  width: 500px;
  max-width: calc(100vw - 32px);
}

/* 平板适配 */
@media (max-width: 1024px) {
  .header-search {
    max-width: 250px;
  }
}

/* 小屏幕适配 */
@media (max-width: 768px) {
  .container-list {
    padding: 0;
  }
  
  .header-content {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .header-actions {
    justify-content: center;
  }
  
  .header-search {
    max-width: none;
  }
  
  /* WebUI模态框小屏幕适配 */
  :deep(.webui-modal .n-dialog) {
    width: calc(100vw - 16px);
    margin: 8px;
  }
  
  :deep(.webui-modal .n-form-item-label) {
    width: 100px !important;
  }
}

/* 极小屏幕适配 */
@media (max-width: 480px) {
  .header-actions {
    flex-direction: column;
    gap: 8px;
  }
  
  .action-btn {
    width: 100%;
  }
  
  .btn-text {
    display: inline;
  }
}

/* 超小屏幕 - 只显示图标 */
@media (max-width: 360px) {
  .action-btn {
    min-width: 40px;
    padding: 0 8px;
  }
  
  .btn-text {
    display: none;
  }
}
</style> 