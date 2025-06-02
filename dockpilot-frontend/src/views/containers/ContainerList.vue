<template>
  <div class="container-list">
    <n-card>
      <template #header>
        <div class="header-content">
          <div class="header-actions">
            <NButton class="action-btn" @click="handleRefresh">
              <template #icon>
                <n-icon><RefreshOutline /></n-icon>
              </template>
              <span class="btn-text">刷新</span>
            </NButton>
            <NButton type="primary" class="action-btn" @click="router.push('/containers/create')">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
              <span class="btn-text">创建容器</span>
            </NButton>
            <NButton 
              class="action-btn" 
              :disabled="!isMultiSelectMode || selectedContainers.size === 0"
              @click="handleGenerateYaml"
            >
              <template #icon>
                <n-icon><DocumentOutline /></n-icon>
              </template>
              <span class="btn-text">生成YAML</span>
            </NButton>
            <NButton 
              class="action-btn" 
              @click="toggleMultiSelectMode"
              :type="isMultiSelectMode ? 'warning' : 'default'"
            >
              <template #icon>
                <n-icon v-if="isMultiSelectMode"><CloseOutline /></n-icon>
                <n-icon v-else><CheckboxOutline /></n-icon>
              </template>
              <span class="btn-text">{{ isMultiSelectMode ? '取消选择' : '多选模式' }}</span>
            </NButton>
          </div>
          <div class="header-search">
            <SearchBar v-model="searchText" placeholder="搜索容器" />
          </div>
        </div>
      </template>

      <!-- 多选模式提示 -->
      <div v-if="isMultiSelectMode" class="multi-select-tip">
        <n-alert type="info" :show-icon="false" style="margin-bottom: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>已选择 {{ selectedContainers.size }} 个容器</span>
            <NButton size="small" @click="clearSelection">清空选择</NButton>
          </div>
        </n-alert>
      </div>

      <NSpace vertical size="large">
        <ContainerItem
          v-for="container in filteredContainers"
          :key="container.id"
          :container="container"
          :operating="operatingContainers.has(container.id)"
          :current-action="containerActions.get(container.id)"
          :multi-select-mode="isMultiSelectMode"
          :selected="selectedContainers.has(container.id)"
          @action="handleContainerAction"
          @select="handleContainerSelect"
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
        label-width="80px"
        style="min-width: 420px;"
      >
        <NFormItem label="Web地址" path="webUrl">
          <NInput
            v-model:value="webUIForm.webUrl"
            placeholder="例如: http://localhost:8080"
            clearable
            style="width: 320px;"
          />
        </NFormItem>
        <NFormItem label="图标地址" path="iconUrl">
          <NInputGroup>
            <NInput
              v-model:value="webUIForm.iconUrl"
              placeholder="例如: https://example.com/icon.png"
              clearable
              style="width: 240px;"
            />
            <NButton 
              type="primary" 
              ghost
              :loading="fetchingIcon"
              :disabled="!webUIForm.webUrl || fetchingIcon"
              @click="handleFetchIcon"
            >
              获取图标
            </NButton>
          </NInputGroup>
        </NFormItem>
      </NForm>
      <template #action>
        <NSpace>
          <NButton @click="showWebUIModal = false">取消</NButton>
          <NButton type="primary" :loading="savingWebUI" @click="handleSaveWebUI">
            保存
          </NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- YAML生成模态框 -->
    <NModal
      v-model:show="showYamlModal"
      preset="card"
      title="生成YAML配置"
      class="yaml-modal"
      style="width: 90%; max-width: 1000px;"
    >
      <!-- 基础信息配置 -->
      <NForm
        ref="yamlFormRef"
        :model="yamlForm"
        label-placement="left"
        label-width="100px"
        style="margin-bottom: 16px;"
      >
        <NFormItem label="项目名称">
          <NInput
            v-model:value="yamlForm.projectName"
            placeholder="容器项目名称"
            style="width: 300px;"
          />
        </NFormItem>
        <NFormItem label="项目描述">
          <NInput
            v-model:value="yamlForm.description"
            placeholder="项目描述（可选）"
            style="width: 300px;"
          />
        </NFormItem>
      </NForm>

      <!-- 环境变量配置 -->
      <div v-if="previewEnvVars.length > 0" style="margin-bottom: 20px;">
        <h3>📝 环境变量说明配置</h3>
        <NAlert type="info" style="margin-bottom: 16px;">
          为环境变量添加说明，方便以后使用时理解每个配置的作用（可选）
        </NAlert>
        <div class="env-config-list">
          <div v-for="env in previewEnvVars" :key="env.name" class="env-config-item">
            <div class="env-config-info">
              <code class="env-name">{{ env.name }}</code>
              <span class="env-value">{{ env.value }}</span>
            </div>
            <NInput
              v-model:value="env.description"
              placeholder="可选：添加说明，例如'Emby访问端口'、'数据存储目录'等"
              style="flex: 1; margin-left: 12px;"
              size="small"
            />
          </div>
        </div>
      </div>

      <NSpace style="margin-bottom: 16px;">
        <NButton type="primary" :loading="generatingYaml" @click="generateYamlContent">
          生成完整YAML
        </NButton>
      </NSpace>

      <div v-if="yamlResult">
        <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
          <NText strong>YAML 配置内容：</NText>
          <NSpace>
            <NButton 
              size="small" 
              @click="editableMode = !editableMode"
              :type="editableMode ? 'warning' : 'default'"
            >
              <template #icon>
                <n-icon><CreateOutline /></n-icon>
              </template>
              {{ editableMode ? '退出编辑' : '编辑YAML' }}
            </NButton>
            <NButton size="small" @click="copyYamlContent">
              <template #icon>
                <n-icon><CopyOutline /></n-icon>
              </template>
              复制
            </NButton>
            <NDropdown
              trigger="click"
              :options="downloadOptions"
              @select="handleDownloadSelect"
            >
              <NButton size="small">
                <template #icon>
                  <n-icon><DownloadOutline /></n-icon>
                </template>
                下载 ▼
              </NButton>
            </NDropdown>
          </NSpace>
        </div>
        
        <!-- 可编辑模式 -->
        <div v-if="editableMode" style="margin-bottom: 16px;">
          <NAlert type="info" style="margin-bottom: 12px;">
            <template #icon>
              <n-icon><InformationCircleOutline /></n-icon>
            </template>
            编辑模式：您可以直接修改YAML内容。请注意保持正确的YAML语法格式。
          </NAlert>
          
          <!-- 编辑状态指示 -->
          <div 
            class="yaml-edit-status" 
            :class="hasUnsavedChanges ? 'has-changes' : 'no-changes'"
          >
            <n-icon v-if="hasUnsavedChanges">
              <RefreshOutline />
            </n-icon>
            <n-icon v-else>
              <CheckmarkCircleOutline />
            </n-icon>
            <span>
              {{ hasUnsavedChanges ? '有未保存的修改' : '内容已同步' }}
            </span>
            <span style="margin-left: auto; color: #999;">
              字符数: {{ editableYamlContent.length }}
            </span>
          </div>
          
          <NInput
            v-model:value="editableYamlContent"
            type="textarea"
            placeholder="请输入YAML内容..."
            :rows="20"
            style="font-family: 'Monaco', 'Consolas', monospace; font-size: 13px;"
            show-count
          />
          
          <div style="margin-top: 8px; display: flex; gap: 8px; flex-wrap: wrap;">
            <NButton size="small" type="primary" @click="applyYamlChanges" :disabled="!hasUnsavedChanges">
              <template #icon>
                <n-icon><CheckmarkOutline /></n-icon>
              </template>
              应用修改
            </NButton>
            <NButton size="small" @click="resetYamlChanges" :disabled="!hasUnsavedChanges">
              <template #icon>
                <n-icon><RefreshOutline /></n-icon>
              </template>
              重置修改
            </NButton>
            <NButton size="small" @click="validateYamlSyntax">
              <template #icon>
                <n-icon><CheckmarkCircleOutline /></n-icon>
              </template>
              验证语法
            </NButton>
            <NButton size="small" @click="insertTemplate" type="default">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
              插入模板
            </NButton>
          </div>
          
          <!-- 编辑提示 -->
          <div class="yaml-edit-tips">
            <h5>💡 编辑提示</h5>
            <ul>
              <li>使用2个空格进行缩进，不要使用Tab键</li>
              <li>冒号后面必须有空格: <code>key: value</code></li>
              <li>字符串值建议用双引号包围: <code>"值"</code></li>
              <li>列表项前面用破折号和空格: <code>- item</code></li>
              <li>可以使用Ctrl+Z撤销，Ctrl+Y重做</li>
            </ul>
          </div>
        </div>
        
        <!-- 只读模式 -->
        <NCode 
          v-else
          :code="yamlResult.yamlContent" 
          language="yaml"
          style="max-height: 400px; overflow-y: auto;"
        />
        
        <div style="margin-top: 16px;">
          <NText depth="3">
            包含容器数量：{{ yamlResult.containerCount }} | 
            生成时间：{{ yamlResult.generateTime }}
            <span v-if="editableMode && hasUnsavedChanges" style="color: orange; margin-left: 8px;">
              • 有未保存的修改
            </span>
          </NText>
        </div>
      </div>

      <template #action>
        <NSpace>
          <NButton @click="handleCloseYamlModal">关闭</NButton>
          <NButton v-if="yamlResult" type="primary" @click="saveAsApplication">
            保存为应用
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick, h } from 'vue'
import { NButton, NSpace, useMessage, useDialog, NModal, NInput, NFormItem, NForm, NInputGroup, NCode, NText, NAlert, type FormInst, NDropdown } from 'naive-ui'
import {
  RefreshOutline,
  AddOutline,
  DocumentOutline,
  CheckboxOutline,
  CloseOutline,
  CopyOutline,
  DownloadOutline,
  CreateOutline,
  InformationCircleOutline,
  CheckmarkOutline,
  CheckmarkCircleOutline
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
import { generateContainerYaml, previewContainerYaml, type ContainerYamlResponse, exportProject, exportYamlOnly, type ProjectExportRequest } from '@/api/containerYaml'
import { sendWebSocketMessage } from '@/api/websocket/websocketService'
import { useRouter } from 'vue-router'
import { useWebSocketTask } from '@/hooks/useWebSocketTask'
import { MessageType } from '@/api/websocket/types'
import { getFavicon } from '@/api/http/system'

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
const operatingContainers = ref<Set<string>>(new Set())
const containerActions = ref<Map<string, string>>(new Map())

// 多选模式相关状态
const isMultiSelectMode = ref(false)
const selectedContainers = ref<Set<string>>(new Set())

// YAML生成相关状态
const showYamlModal = ref(false)
const generatingYaml = ref(false)
const yamlResult = ref<ContainerYamlResponse | null>(null)
const yamlForm = reactive({
  projectName: '',
  description: ''
})
const yamlFormRef = ref<FormInst | null>(null)

// YAML编辑相关状态
const editableMode = ref(false)
const editableYamlContent = ref('')
const hasUnsavedChanges = ref(false)
const originalYamlContent = ref('')

// 环境变量预览状态
const previewEnvVars = ref<Array<{
  name: string
  value: string
  description: string
}>>([])

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

// 多选模式切换
function toggleMultiSelectMode() {
  isMultiSelectMode.value = !isMultiSelectMode.value
  if (!isMultiSelectMode.value) {
    selectedContainers.value.clear()
  }
}

// 清空选择
function clearSelection() {
  selectedContainers.value.clear()
}

// 容器选择处理
function handleContainerSelect(containerId: string, selected: boolean) {
  if (selected) {
    selectedContainers.value.add(containerId)
  } else {
    selectedContainers.value.delete(containerId)
  }
}

// 获取环境变量预览数据
async function loadPreviewEnvVars() {
  try {
    // 先生成一个预览YAML来获取环境变量
    const response = await previewContainerYaml({
      containerIds: Array.from(selectedContainers.value),
      projectName: yamlForm.projectName,
      description: yamlForm.description,
      excludeFields: ['environment'] // 排除敏感信息但保留环境变量结构
    })
    
    if (response.success) {
      // 解析YAML内容，提取环境变量
      const yamlLines = response.yamlContent.split('\n')
      const envVars: Array<{name: string, value: string, description: string}> = []
      let inEnvSection = false
      
      for (let i = 0; i < yamlLines.length; i++) {
        const line = yamlLines[i].trim()
        if (line === 'env:') {
          inEnvSection = true
          continue
        }
        if (inEnvSection && line && !line.startsWith(' ')) {
          inEnvSection = false
        }
        if (inEnvSection && line.includes(':')) {
          const envName = line.split(':')[0].trim()
          const envValue = line.split(':')[1]?.trim()?.replace(/"/g, '') || ''
          if (envName && !envVars.find(e => e.name === envName)) {
            envVars.push({
              name: envName,
              value: envValue,
              description: ''
            })
          }
        }
      }
      
      previewEnvVars.value = envVars
    }
  } catch (error) {
    console.warn('预览环境变量失败:', error)
    previewEnvVars.value = []
  }
}

// 生成YAML处理
async function handleGenerateYaml() {
  if (selectedContainers.value.size === 0) {
    message.warning('请先选择要生成YAML的容器')
    return
  }
  
  // 重置表单和结果
  yamlForm.projectName = `容器项目-${new Date().getTime()}`
  yamlForm.description = ''
  yamlResult.value = null
  previewEnvVars.value = []
  
  // 重置编辑模式状态
  editableMode.value = false
  editableYamlContent.value = ''
  hasUnsavedChanges.value = false
  originalYamlContent.value = ''
  
  showYamlModal.value = true
  
  // 加载环境变量预览
  await loadPreviewEnvVars()
}

// 生成完整YAML
async function generateYamlContent() {
  try {
    generatingYaml.value = true
    
    // 收集用户配置的环境变量描述
    const envDescriptions: Record<string, string> = {}
    previewEnvVars.value.forEach(env => {
      if (env.description && env.description.trim()) {
        envDescriptions[env.name] = env.description
      }
    })
    
    const response = await generateContainerYaml({
      containerIds: Array.from(selectedContainers.value),
      projectName: yamlForm.projectName,
      description: yamlForm.description,
      envDescriptions: envDescriptions
    })
    
    if (response.success) {
      yamlResult.value = response
      // 初始化编辑状态
      originalYamlContent.value = response.yamlContent
      message.success('YAML生成成功')
    } else {
      message.error(response.message || 'YAML生成失败')
    }
  } catch (error: any) {
    message.error('生成YAML失败: ' + (error.message || error))
  } finally {
    generatingYaml.value = false
  }
}

// 复制YAML内容
function copyYamlContent() {
  if (!yamlResult.value) return
  
  navigator.clipboard.writeText(yamlResult.value.yamlContent).then(() => {
    message.success('YAML内容已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败，请手动复制')
  })
}

// 下载YAML文件
function downloadYamlFile() {
  if (!yamlResult.value) return
  
  const blob = new Blob([yamlResult.value.yamlContent], { type: 'text/yaml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${yamlResult.value.projectName || 'docker-compose'}.yml`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  message.success('YAML文件下载完成')
}

// 保存为应用（跳转到应用中心）
function saveAsApplication() {
  if (!yamlResult.value) return
  
  // 跳转到应用中心并预填充导入表单
  router.push({
    path: '/appcenter',
    query: {
      mode: 'import',
      yaml: encodeURIComponent(yamlResult.value.yamlContent),
      name: encodeURIComponent(yamlResult.value.projectName)
    }
  })
  
  // 关闭当前弹窗
  showYamlModal.value = false
  message.success('已跳转到应用中心，请完善应用信息后导入')
}

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
    console.log('📊 没有运行中的容器，跳过性能数据获取')
    return
  }

  console.log(`📊 启动性能监控，运行中容器数量: ${runningContainers.length}`)

  // 当前要请求的容器索引
  let currentIndex = 0

  // 设置定时器，智能请求容器性能数据
  statsTimer = window.setInterval(() => {
    // 🔥 动态获取当前运行中的容器（每次都重新筛选）
    const currentRunningContainers = containers.value.filter(container => container.status === 'running')
    
    // 如果没有运行中的容器，暂停请求
    if (currentRunningContainers.length === 0) {
      console.log('📊 当前无运行中容器，暂停性能数据获取')
      return
    }

    // 重置索引如果超出范围
    if (currentIndex >= currentRunningContainers.length) {
      currentIndex = 0
    }

    // 获取要请求的容器
    const targetContainer = currentRunningContainers[currentIndex]
    
    // 二次验证：确保容器仍然存在且状态正确
    const containerStillValid = containers.value.find(c => 
      c.id === targetContainer.id && c.status === 'running'
    )
    
    if (containerStillValid) {
      console.log(`📊 获取容器性能数据: ${targetContainer.name} (${targetContainer.id.slice(0, 12)})`)
      loadContainerStats(targetContainer.id)
    } else {
      console.log(`📊 跳过无效容器: ${targetContainer.id.slice(0, 12)}`)
    }
    
    // 更新索引，循环请求
    currentIndex = (currentIndex + 1) % currentRunningContainers.length
  }, STATS_UPDATE_INTERVAL)
}

// 监听容器列表变化，智能重启性能监控
watch(containers, (newContainers, oldContainers) => {
  // 检查运行中容器数量是否变化
  const newRunningCount = newContainers.filter(c => c.status === 'running').length
  const oldRunningCount = oldContainers ? oldContainers.filter(c => c.status === 'running').length : 0
  
  if (newRunningCount !== oldRunningCount) {
    console.log(`📊 运行中容器数量变化: ${oldRunningCount} -> ${newRunningCount}`)
    // 重启性能监控
    startStatsTimer()
  }
}, { deep: true })

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
    
    // 处理URL格式，确保有协议前缀（如果没有的话）
    let webUrl = webUIForm.webUrl.trim()
    let iconUrl = webUIForm.iconUrl.trim()
    
    // 只有在没有协议前缀时才添加http://
    if (webUrl && !webUrl.startsWith('http://') && !webUrl.startsWith('https://')) {
      webUrl = `http://${webUrl}`
    }
    if (iconUrl && !iconUrl.startsWith('http://') && !iconUrl.startsWith('https://')) {
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
const webUIFormRef = ref<FormInst | null>(null)

// 获取图标相关状态
const fetchingIcon = ref(false)

// 获取图标方法
async function handleFetchIcon() {
  if (!webUIForm.webUrl) return
  fetchingIcon.value = true
  try {
    const url = webUIForm.webUrl.trim()
    const iconUrl = await getFavicon(url)
    if (iconUrl) {
      webUIForm.iconUrl = iconUrl
      message.success('图标获取成功')
    } else {
      message.error('未能获取到图标')
    }
  } catch (e: any) {
    message.error('获取图标失败: ' + (e?.message || e))
  } finally {
    fetchingIcon.value = false
  }
}

// URL验证函数 - 简化版，只做基本检查
function validateUrl(url: string): boolean {
  if (!url) return true // 空值在required规则中处理
  
  const trimmedUrl = url.trim()
  
  // 基本检查：不能只是空格，不能包含明显的非法字符
  if (trimmedUrl.length === 0) return false
  if (trimmedUrl.includes(' ')) return false // URL不应该包含空格
  
  // 其他情况都认为是有效的，让用户自己负责
  return true
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
          return new Error('URL不能包含空格或为空')
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
          return new Error('URL不能包含空格或为空')
        }
        return true
      },
      trigger: 'blur'
    }
  ]
}

// 监听编辑内容变化
watch(editableYamlContent, (newValue) => {
  if (originalYamlContent.value && newValue !== originalYamlContent.value) {
    hasUnsavedChanges.value = true
  } else {
    hasUnsavedChanges.value = false
  }
})

// 监听编辑模式切换
watch(editableMode, (newMode) => {
  if (newMode && yamlResult.value) {
    // 进入编辑模式，初始化编辑内容
    editableYamlContent.value = yamlResult.value.yamlContent
    originalYamlContent.value = yamlResult.value.yamlContent
    hasUnsavedChanges.value = false
  }
})

// YAML编辑相关方法
function applyYamlChanges() {
  if (!yamlResult.value) return
  
  // 应用修改
  yamlResult.value.yamlContent = editableYamlContent.value
  originalYamlContent.value = editableYamlContent.value
  hasUnsavedChanges.value = false
  message.success('YAML修改已应用')
}

function resetYamlChanges() {
  if (!originalYamlContent.value) return
  
  // 重置为原始内容
  editableYamlContent.value = originalYamlContent.value
  hasUnsavedChanges.value = false
  message.success('YAML已重置为原始内容')
}

function validateYamlSyntax() {
  if (!editableYamlContent.value) {
    message.warning('请输入YAML内容')
    return
  }
  
  try {
    // 基本的YAML语法检查
    const lines = editableYamlContent.value.split('\n')
    let hasErrors = false
    const errors: string[] = []
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i]
      const lineNum = i + 1
      
      // 检查缩进（应该是2或4的倍数）
      if (line.length > 0 && line[0] === ' ') {
        const leadingSpaces = line.match(/^ */)?.[0].length || 0
        if (leadingSpaces % 2 !== 0) {
          errors.push(`第${lineNum}行: 缩进应该是2的倍数`)
          hasErrors = true
        }
      }
      
      // 检查冒号后是否有空格
      if (line.includes(':') && !line.includes(': ') && !line.endsWith(':')) {
        const colonIndex = line.indexOf(':')
        if (colonIndex < line.length - 1 && line[colonIndex + 1] !== ' ') {
          errors.push(`第${lineNum}行: 冒号后应该有空格`)
          hasErrors = true
        }
      }
    }
    
    if (hasErrors) {
      message.error('YAML语法检查发现问题:\n' + errors.join('\n'))
    } else {
      message.success('YAML语法验证通过')
    }
  } catch (error) {
    message.error('YAML语法验证失败: ' + (error as Error).message)
  }
}

function handleCloseYamlModal() {
  if (hasUnsavedChanges.value) {
    dialog.warning({
      title: '未保存的修改',
      content: '您有未保存的修改，确定要关闭吗？',
      positiveText: '确定',
      negativeText: '取消',
      maskClosable: false,
      closeOnEsc: false,
      onClose: () => {
        showYamlModal.value = false
      },
      onPositiveClick: () => {
        showYamlModal.value = false
      },
      onNegativeClick: () => {
        // 如果用户取消，不关闭模态框
      }
    })
  } else {
    showYamlModal.value = false
  }
}

function insertTemplate() {
  const templates = [
    {
      name: '新服务模板',
      content: `
  new-service:
    image: nginx:latest
    ports:
      - "8080:80"
    environment:
      - ENV_VAR=value
    volumes:
      - ./data:/app/data
    restart: unless-stopped`
    },
    {
      name: '数据库服务',
      content: `
  database:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: "password"
      MYSQL_DATABASE: "app_db"
    volumes:
      - db_data:/var/lib/mysql
    restart: unless-stopped`
    },
    {
      name: '环境变量配置',
      content: `
    environment:
      - NEW_VAR=value
      - ANOTHER_VAR=another_value`
    }
  ]
  
  // 简化版本，使用字符串选择
  const templateNames = templates.map(t => t.name).join('\n')
  
  dialog.info({
    title: '插入YAML模板',
    content: `可用的模板：\n\n${templateNames}\n\n请在下面的按钮中选择要插入的模板。`,
    action: () => {
      return h('div', { style: 'display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px;' }, 
        templates.map(template => 
          h('button', {
            style: 'padding: 6px 12px; border: 1px solid #ddd; border-radius: 4px; background: #f8f9fa; cursor: pointer;',
            onClick: () => {
              editableYamlContent.value += template.content
              message.success(`已插入${template.name}模板`)
            }
          }, template.name)
        )
      )
    }
  })
}

// 下载选项
const downloadOptions = [
  {
    label: '只下载YAML',
    key: 'yaml'
  },
  {
    label: '同时打包配置文件',
    key: 'all'
  }
]

// 处理下载选择
function handleDownloadSelect(key: string) {
  if (key === 'yaml') {
    downloadYamlFile()
  } else if (key === 'all') {
    downloadProjectPackage()
  }
}

// 下载项目包（YAML + 配置文件）
async function downloadProjectPackage() {
  if (!yamlResult.value) return

  try {
    const params: ProjectExportRequest = {
      containerIds: Array.from(selectedContainers.value),
      projectName: yamlResult.value.projectName,
      description: yamlForm.description,
      includeConfigPackages: true
    }

    const { blob, filename } = await exportProject(params)
    
    // 创建下载链接
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    
    message.success('项目包下载完成')
  } catch (error: any) {
    message.error('项目包下载失败: ' + (error.message || error))
  }
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

/* 多选提示 */
.multi-select-tip {
  margin-bottom: 16px;
}

:deep(.n-card) {
  background-color: var(--n-card-color);
}

/* WebUI配置模态框响应式 */
:deep(.webui-modal .n-dialog) {
  width: 500px;
  max-width: calc(100vw - 32px);
}

/* YAML模态框样式 */
:deep(.yaml-modal .n-card) {
  max-height: 80vh;
  overflow-y: auto;
}

/* YAML编辑器样式 */
.yaml-editor-container {
  border: 1px solid #e0e0e6;
  border-radius: 6px;
  overflow: hidden;
}

.yaml-editor-toolbar {
  background: #f5f5f5;
  border-bottom: 1px solid #e0e0e6;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.yaml-edit-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 12px;
}

.yaml-edit-status.has-changes {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  color: #856404;
}

.yaml-edit-status.no-changes {
  background: #d4edda;
  border: 1px solid #c3e6cb;
  color: #155724;
}

/* 编辑提示样式 */
.yaml-edit-tips {
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid #007bff;
}

.yaml-edit-tips h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #495057;
}

.yaml-edit-tips ul {
  margin: 0;
  padding-left: 20px;
  color: #6c757d;
  font-size: 12px;
}

.yaml-edit-tips li {
  margin-bottom: 4px;
}

/* 语法验证结果样式 */
.yaml-validation-result {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: 4px;
}

.yaml-validation-result.success {
  background: #d4edda;
  border: 1px solid #c3e6cb;
  color: #155724;
}

.yaml-validation-result.error {
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  color: #721c24;
}

/* 平板适配 */
@media (max-width: 1024px) {
  .header-search {
    max-width: 250px;
  }
  
  .header-actions {
    flex-wrap: wrap;
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
    flex-wrap: wrap;
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
  
  /* YAML模态框小屏幕适配 */
  :deep(.yaml-modal) {
    width: calc(100vw - 16px) !important;
    max-width: none !important;
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

/* 环境变量配置样式 */
.env-config-list {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e0e0e6;
  border-radius: 6px;
  padding: 12px;
  background-color: #fafafa;
}

.env-config-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.env-config-item:last-child {
  border-bottom: none;
}

.env-config-info {
  min-width: 300px;
  margin-right: 12px;
}

.env-name {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  color: #666;
  display: inline-block;
  margin-right: 8px;
}

.env-value {
  color: #2080f0;
  font-size: 12px;
  font-weight: 500;
}
</style> 