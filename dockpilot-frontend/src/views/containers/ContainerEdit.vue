<template>
  <div class="container-edit">
    <n-card>
      <!-- 页面头部 -->
      <template #header>
        <n-space justify="space-between">
          <n-space>
            <n-button @click="handleBack">
              <template #icon>
                <n-icon><ArrowBack /></n-icon>
              </template>
              返回
            </n-button>
            <div class="title-section">
              <n-text>编辑容器 - {{ containerName }}</n-text>
              <!-- 错误信息显示在名称下面 -->
              <div v-if="containerError && operationStatus === 'failed'" class="error-info">
                <n-icon class="error-icon"><WarningOutline /></n-icon>
                <span class="error-text">{{ containerError }}</span>
              </div>
            </div>
          </n-space>
          <n-button @click="showPreviewModal = true">
            <template #icon>
              <n-icon><EyeOutline /></n-icon>
            </template>
            预览 Docker 命令
          </n-button>
        </n-space>
      </template>

      <!-- 加载状态 -->
      <n-spin :show="loading">
        <!-- Tabs 区域 -->
        <n-tabs
          :value="activeTab"
          display-directive="if"
          type="line"
          animated
          style="margin-bottom: 20px;"
          @update:value="handleTabChange"
        >
          <!-- Tab Pane 1: 基本信息 -->
          <n-tab-pane name="basic" tab="基本信息">
            <!-- 基本信息表单内容 -->
            <n-form-item label="容器名称" path="name">
              <n-input v-model:value="formData.name" placeholder="请输入容器名称" />
            </n-form-item>
            <n-form-item label="镜像" path="image">
              <n-input v-model:value="formData.image" placeholder="请输入镜像名称" />
            </n-form-item>
            <n-form-item label="工作目录" path="workingDir">
              <n-input v-model:value="formData.workingDir" placeholder="请输入工作目录" />
            </n-form-item>
            <n-form-item label="启动命令" path="cmd">
              <n-dynamic-input
                v-model:value="formData.cmd"
                :on-create="onCreateCommand"
                placeholder="请输入启动命令参数"
              />
            </n-form-item>
          </n-tab-pane>

          <!-- Tab Pane 2: 网络配置 -->
          <n-tab-pane name="network" tab="网络与端口">
            <!-- 网络配置表单内容 -->
            <n-form-item label="网络模式" path="networkMode">
              <n-select
                v-model:value="formData.networkMode"
                :options="networkModeOptions"
              />
            </n-form-item>
            <n-form-item label="端口映射" path="portMappings">
              <n-dynamic-input
                v-model:value="formData.portMappings"
                :on-create="onCreatePort"
                placeholder="请输入端口映射"
              >
                <template #default="{ value }">
                  <div style="display: flex; align-items: center; width: 100%;">
                    <n-input
                      v-model:value="value.hostPort"
                      placeholder="主机端口"
                      style="margin-right: 8px;"
                    />
                    <span>:</span>
                    <n-input
                      v-model:value="value.containerPort"
                      placeholder="容器端口"
                      style="margin-left: 8px; margin-right: 8px;"
                    />
                    <span>/</span>
                    <n-input
                      v-model:value="value.protocol"
                      placeholder="协议"
                      style="margin-left: 8px;"
                    />
                  </div>
                </template>
                <template #create-button-default>
                  添加端口映射
                </template>
              </n-dynamic-input>
            </n-form-item>
            <n-form-item label="环境变量" path="environmentVariables">
              <n-dynamic-input
                v-model:value="formData.environmentVariables"
                :on-create="onCreateEnv"
                placeholder="请输入环境变量"
              >
                <template #default="{ value }">
                  <div style="display: flex; align-items: center; width: 100%;">
                     <n-input
                      v-model:value="value.key"
                      placeholder="Key"
                      style="margin-right: 8px;"
                    />
                    <span>=</span>
                     <n-input
                      v-model:value="value.value"
                      placeholder="Value"
                      style="margin-left: 8px;"
                    />
                  </div>
                </template>
                <template #create-button-default>
                  添加环境变量
                </template>
              </n-dynamic-input>
            </n-form-item>
          </n-tab-pane>

          <!-- Tab Pane 3: 存储配置 -->
          <n-tab-pane name="storage" tab="卷挂载">
            <!-- 存储配置表单内容 -->
            <n-form-item label="数据卷" path="volumeMappings">
              <n-dynamic-input
                v-model:value="formData.volumeMappings"
                :on-create="onCreateVolume"
                placeholder="请输入数据卷配置"
              >
                <template #default="{ value }">
                  <div style="display: flex; align-items: center; width: 100%; gap: 8px;">
                    <div style="flex: 2;">
                      <PathSelector
                        v-model="value.hostPath"
                        placeholder="选择主机文件夹"
                        @update:model-value="handleVolumeValidation"
                      />
                    </div>
                    <span>:</span>
                    <n-input
                      v-model:value="value.containerPath"
                      placeholder="容器路径"
                      style="flex: 1;"
                    />
                    <n-checkbox v-model:checked="value.readOnly">只读</n-checkbox>
                  </div>
                </template>
                <template #create-button-default>
                  添加数据卷
                </template>
              </n-dynamic-input>
            </n-form-item>
          </n-tab-pane>

          <!-- Tab Pane 4: 高级设置 -->
          <n-tab-pane name="advanced" tab="高级设置">
            <!-- 高级设置表单内容 -->
            <n-form-item label="重启策略" path="restartPolicy">
              <n-select
                v-model:value="formData.restartPolicy"
                :options="restartPolicyOptions"
              />
            </n-form-item>
          </n-tab-pane>
        </n-tabs>

        <!-- 表单包装 -->
        <n-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-placement="left"
          label-width="auto"
          require-mark-placement="right-hanging"
          :show-feedback="true"
          :show-require-mark="true"
        >
          <!-- 表单内容由tabs渲染 -->
        </n-form>
      </n-spin>

      <!-- 底部按钮 -->
      <template #footer>
        <n-space justify="end">
          <n-button @click="handleBack">取消</n-button>
          <n-button
            type="primary"
            :loading="updateLoading"
            @click="handleUpdateConfirm"
          >
            更新
          </n-button>
        </n-space>
      </template>

    </n-card>

    <!-- 预览模态框 -->
    <n-modal
      v-model:show="showPreviewModal"
      preset="card"
      title="Docker 命令预览"
      style="width: 800px"
    >
      <n-tabs type="line" animated>
        <n-tab-pane name="docker-run" tab="Docker Run">
          <n-space vertical>
            <n-code :code="dockerRunCommand" language="bash" />
            <n-button
              type="primary"
              @click="handleCopyToClipboard(dockerRunCommand)"
            >
              <template #icon>
                <n-icon><CopyOutline /></n-icon>
              </template>
              复制 Docker Run 命令
            </n-button>
          </n-space>
        </n-tab-pane>
        <n-tab-pane name="docker-compose" tab="Docker Compose">
          <n-space vertical>
            <n-code
              :code="dockerComposeConfig"
              language="yaml"
              style="max-width: 100%;"
            />
            <n-button
              type="primary"
              @click="handleCopyToClipboard(dockerComposeConfig)"
            >
              <template #icon>
                <n-icon><CopyOutline /></n-icon>
              </template>
              复制 Docker Compose 配置
            </n-button>
          </n-space>
        </n-tab-pane>
      </n-tabs>
    </n-modal>

    <!-- 更新确认对话框 -->
    <n-modal
      v-model:show="showUpdateConfirmModal"
      preset="dialog"
      title="确认更新"
      positive-text="确认"
      negative-text="取消"
      @positive-click="handleSave"
      @negative-click="showUpdateConfirmModal = false"
    >
      <template #default>
        <n-space vertical>
          <n-text>确定要更新容器吗？</n-text>
          <n-text depth="3">更新过程中会创建新容器，原容器将保留为备份，有需要可以自行删除/恢复。</n-text>
        </n-space>
      </template>
    </n-modal>

    <!-- 更新日志模态框 -->
    <n-modal
      v-model:show="showLogModal"
      title="更新日志"
      preset="card"
      style="width: 600px"
    >
      <n-log :lines="logs" />
      <template #footer>
        <n-button
          type="primary"
          :disabled="!updateFinished"
          @click="handleLogModalOk"
        >
          确定
        </n-button>
      </template>
    </n-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage, type FormInst } from 'naive-ui'
import { ArrowBack, CopyOutline, EyeOutline, WarningOutline } from '@vicons/ionicons5'
import { generateDockerRunCommand, generateDockerComposeConfig } from '@/utils/dockerPreview'
import { mapFormDataToUpdateRequest } from '@/utils/container'
import { useWebSocketTask } from '@/hooks/useWebSocketTask'
import { MessageType, type DockerWebSocketMessage } from '@/api/websocket/types'
import { copyToClipboard } from '@/utils/clipboard'
import PathSelector from '@/components/common/PathSelector.vue'
import type {
  ContainerForm,
  ContainerDetail,
  PortMapping,
  EnvironmentVariable,
  VolumeMapping
} from '@/types/container'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const formRef = ref<FormInst | null>(null)
const containerName = ref('')
const containerError = ref('')
const operationStatus = ref('')

// UI 状态
const activeTab = ref('basic')
const showPreviewModal = ref(false)
const showUpdateConfirmModal = ref(false)
const showLogModal = ref(false)
const updateFinished = ref(false)

const containerId = route.params.id as string

// 初始化表单数据
const formData = ref<ContainerForm>({
  name: '',
  image: '',
  tag: '',
  autoPull: false,
  autoRemove: false,
  restartPolicy: 'no',
  portMappings: [],
  networkMode: 'bridge',
  ipAddress: '',
  gateway: '',
  volumeMappings: [],
  devices: [],
  environmentVariables: [],
  privileged: false,
  capAdd: [],
  capDrop: [],
  memoryLimit: '',
  cpuLimit: '',
  entrypoint: [],
  cmd: [],
  workingDir: '',
  user: '',
  labels: [],
  healthcheck: {
    test: [],
    interval: '',
    timeout: '',
    retries: 0,
    startPeriod: ''
  }
})

// 使用 useWebSocketTask hook 获取容器详情
const {
  loading: fetchLoading,
  start: fetchContainerDetail
} = useWebSocketTask({
  type: MessageType.CONTAINER_DETAIL,
  autoStart: false, // 手动启动
  onComplete: (msg: DockerWebSocketMessage) => {
    try {
      // 按照统一标准，业务数据从 msg.data 获取
      const detail = msg.data as ContainerDetail
      
      // 🔍 添加调试日志，查看后端返回的原始数据
      console.log('🔍 后端返回的容器详情数据:', detail)
      console.log('🔍 端口数据原始格式:', detail.ports)
      console.log('🔍 卷数据原始格式:', detail.volumes)
      console.log('🔍 环境变量原始格式:', detail.envs)
      
      if (!detail) {
        throw new Error('容器详情数据为空')
      }

      // 设置容器名称和错误信息
      containerName.value = detail.containerName || detail.containerId?.slice(0, 12) || ''
      containerError.value = detail.lastError || ''
      operationStatus.value = detail.operationStatus || ''

      // 映射后端数据到表单数据
      formData.value = {
        name: detail.containerName || '',
        image: detail.imageName || '',
        tag: '', // 从镜像名中提取标签
        autoPull: false,
        autoRemove: false,
        restartPolicy: detail.restartPolicyName || 'no',
        portMappings: formatPortMappings(detail.ports),
        networkMode: detail.networkMode || 'bridge',
        ipAddress: detail.ipAddress || '',
        gateway: '',
        volumeMappings: formatVolumeMappings(detail.volumes),
        devices: [],
        environmentVariables: formatEnvironmentVariables(detail.envs),
        privileged: detail.privileged || false,
        capAdd: detail.capAdd || [],
        capDrop: detail.capDrop || [],
        memoryLimit: '',
        cpuLimit: '',
        entrypoint: detail.entrypoints || [],
        cmd: detail.command || [],
        workingDir: detail.workingDir || '',
        user: '',
        labels: formatLabels(detail.labels),
        healthcheck: {
          test: [],
          interval: '',
          timeout: '',
          retries: 0,
          startPeriod: ''
        }
      }
      
      // 🔍 检查解析后的端口映射
      console.log('🔍 解析后的端口映射:', formData.value.portMappings)

    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : '数据格式错误'
      message.error('解析容器详情失败：' + errorMessage)
    }
  },
  onError: (err: string) => {
    message.error('获取容器详情失败：' + err)
  }
})

// 使用 useWebSocketTask hook 更新容器
const {
  loading: updateLoading,
  logs,
  start: updateContainerTask
} = useWebSocketTask({
  type: MessageType.CONTAINER_UPDATE,
  autoStart: false,
  onComplete: () => {
    message.success('容器更新成功')
    updateFinished.value = true
  },
  onError: (err: string) => {
    message.error('容器更新失败：' + err)
    updateFinished.value = true
  }
})

// 计算总的 loading 状态
const loading = computed(() => fetchLoading.value || updateLoading.value)

// 数据格式化函数
function formatPortMappings(ports?: string[]): PortMapping[] {
  console.log('🔧 formatPortMappings - 输入端口数据:', ports)
  
  if (!ports || !Array.isArray(ports)) {
    console.log('🔧 端口数据为空或不是数组，返回空数组')
    return []
  }
  
  const result = ports.map((port, index) => {
    console.log(`🔧 处理端口 ${index}:`, port)
    
    // 尝试多种端口格式
    // 🔧 修复：格式1应该是 "containerPort/protocol:hostPort" 如 "3000/tcp:3333"
    let match = port.match(/^(\d+)\/(.+):(\d+)$/)
    if (match) {
      console.log(`🔧 匹配格式1: ${match[1]}/${match[2]}:${match[3]}`)
      return {
        hostPort: match[3],        // 主机端口
        containerPort: match[1],   // 容器端口
        protocol: match[2],        // 协议
        ip: ''
      }
    }
    
    // 格式2: "8080:80" (没有协议) - 保持原有逻辑，但交换位置
    match = port.match(/^(\d+):(\d+)$/)
    if (match) {
      console.log(`🔧 匹配格式2: ${match[1]}:${match[2]}`)
      return {
        hostPort: match[2],        // 第二个是主机端口
        containerPort: match[1],   // 第一个是容器端口
        protocol: 'tcp',
        ip: ''
      }
    }
    
    // 格式3: "80/tcp" (只有容器端口)
    match = port.match(/^(\d+)\/(.+)$/)
    if (match) {
      console.log(`🔧 匹配格式3: ${match[1]}/${match[2]}`)
      return {
        hostPort: '',
        containerPort: match[1],
        protocol: match[2],
        ip: ''
      }
    }
    
    // 格式4: "80" (只有端口号)
    match = port.match(/^(\d+)$/)
    if (match) {
      console.log(`🔧 匹配格式4: ${match[1]}`)
      return {
        hostPort: '',
        containerPort: match[1],
        protocol: 'tcp',
        ip: ''
      }
    }
    
    console.log(`🔧 无法解析端口格式: ${port}，使用默认值`)
    return {
      hostPort: '',
      containerPort: '',
      protocol: 'tcp',
      ip: ''
    }
  })
  
  console.log('🔧 formatPortMappings - 输出结果:', result)
  return result
}

function formatVolumeMappings(
  volumes?: Array<{containerPath: string, hostPath: string, readOnly: boolean}>
): VolumeMapping[] {
  if (!volumes || !Array.isArray(volumes)) return []
  
  return volumes.map(volume => ({
    hostPath: volume.hostPath || '',
    containerPath: volume.containerPath || '',
    readOnly: volume.readOnly || false
  }))
}

function formatEnvironmentVariables(envs?: string[]): EnvironmentVariable[] {
  if (!envs || !Array.isArray(envs)) return []
  
  return envs.map(env => {
    const [key, ...valueParts] = env.split('=')
    return {
      key: key || '',
      value: valueParts.join('=') || ''
    }
  })
}

function formatLabels(labels?: Record<string, string>) {
  if (!labels) return []
  
  return Object.entries(labels).map(([key, value]) => ({
    key,
    value
  }))
}

// 表单验证规则
const rules = {
  name: {
    required: true,
    message: '请输入容器名称',
    trigger: 'blur'
  },
  image: {
    required: true,
    message: '请输入镜像名称',
    trigger: 'blur'
  },
  volumeMappings: {
    type: 'array',
    validator: (rule: any, value: any[]) => {
      if (!value || value.length === 0) return true // 挂载目录可选
      
      const containerPaths = new Set()
      for (let i = 0; i < value.length; i++) {
        const volume = value[i]
        
        // 检查主机路径
        if (!volume.hostPath) {
          return Promise.reject(`第${i + 1}个挂载目录：主机路径不能为空`)
        }
        
        // 检查主机路径格式（必须是绝对路径）
        if (!volume.hostPath.startsWith('/')) {
          return Promise.reject(`第${i + 1}个挂载目录：主机路径必须是绝对路径（以/开头）`)
        }
        
        // 检查容器路径
        if (!volume.containerPath) {
          return Promise.reject(`第${i + 1}个挂载目录：容器路径不能为空`)
        }
        
        // 检查容器路径格式（必须是绝对路径）
        if (!volume.containerPath.startsWith('/')) {
          return Promise.reject(`第${i + 1}个挂载目录：容器路径必须是绝对路径（以/开头）`)
        }
        
        // 检查容器路径重复
        if (containerPaths.has(volume.containerPath)) {
          return Promise.reject(`第${i + 1}个挂载目录：容器路径${volume.containerPath}重复`)
        }
        containerPaths.add(volume.containerPath)
      }
      return true
    },
    trigger: 'blur'
  }
}

// 选项配置
const restartPolicyOptions = [
  { label: '不自动重启', value: 'no' },
  { label: '总是重启', value: 'always' },
  { label: '失败时重启', value: 'on-failure' }
]

const networkModeOptions = [
  { label: 'bridge', value: 'bridge' },
  { label: 'host', value: 'host' },
  { label: 'none', value: 'none' }
]

// 动态输入创建函数
const onCreatePort = (): PortMapping => ({
  hostPort: '',
  containerPort: '',
  protocol: 'tcp',
  ip: ''
})

const onCreateEnv = (): EnvironmentVariable => ({
  key: '',
  value: ''
})

const onCreateVolume = (): VolumeMapping => ({
  hostPath: '',
  containerPath: '',
  readOnly: false
})

const onCreateCommand = () => ''

// 校验处理函数
const handleVolumeValidation = () => {
  formRef.value?.validate(['volumeMappings'])
}

// 事件处理函数
async function handleTabChange(tabName: string) {
  activeTab.value = tabName
}

const handleUpdateConfirm = () => {
  showUpdateConfirmModal.value = true
}

const handleSave = async () => {
  try {
    // 表单验证
    await formRef.value?.validate()
    
    // 🔍 添加调试日志
    console.log('🚀 容器更新 - 表单数据检查:')
    console.log('  - formData.name:', formData.value.name)
    console.log('  - formData.image:', formData.value.image)
    console.log('  - formData.tag:', formData.value.tag)
    console.log('  - 完整表单数据:', formData.value)
    
    // 转换表单数据为更新请求参数
    const updateParams = mapFormDataToUpdateRequest(formData.value)
    
    // 🔍 检查转换后的参数
    console.log('📊 转换后的更新参数:', updateParams)
    console.log('  - updateParams.image:', updateParams.image)
    
    // 重置状态
    updateFinished.value = false
    showUpdateConfirmModal.value = false
    showLogModal.value = true
    
    // 🔧 修复：后端期望的数据结构为 { containerId, config: { ... } }
    const requestData = {
      containerId,
      config: updateParams
    }
    
    console.log('📤 最终发送的请求数据:', requestData)
    
    // 启动更新任务
    updateContainerTask(requestData)
    
  } catch (error: unknown) {
    if (Array.isArray(error)) {
      message.error('表单验证失败，请检查所有必填项')
    } else {
      const errorMessage = error instanceof Error ? error.message : '未知错误'
      message.error('更新失败：' + errorMessage)
    }
    updateFinished.value = true
  }
}

const handleBack = () => {
  router.push('/containers')
}

const handleLogModalOk = () => {
  showLogModal.value = false
  if (updateFinished.value) {
    router.push('/containers')
  }
}

// 计算预览命令
const dockerRunCommand = computed(() => {
  return generateDockerRunCommand(formData.value)
})

const dockerComposeConfig = computed(() => {
  return generateDockerComposeConfig(formData.value)
})

// 复制到剪贴板 - 使用健壮版本
const handleCopyToClipboard = async (text: string) => {
  await copyToClipboard(text, { 
    showMessage: true, 
    messageApi: message 
  })
}

// 页面加载时获取容器详情
onMounted(() => {
  if (containerId) {
    fetchContainerDetail({ containerId })
  } else {
    message.error('缺少容器ID参数')
    router.push('/containers')
  }
})
</script>

<style scoped>
.container-edit {
  padding: 20px;
}
.mb-4 {
  margin-bottom: 16px;
}
.ml-2 {
  margin-left: 8px;
}
.step-buttons {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #eee;
}
.n-layout-sider {
  background-color: var(--n-color);
}
.tab-content-pane {
   padding: 20px !important; /* 恢复 TabPane 内边距 */
}
.n-card > .n-card__footer {
    padding: 16px 20px;
}
.title-section {
  display: flex;
  flex-direction: column;
}
.error-info {
  color: red;
  font-size: 0.8em;
  margin-top: 4px;
  display: flex;
  align-items: center;
}
.error-icon {
  margin-right: 4px;
}
.error-text {
  margin-left: 4px;
}
</style> 