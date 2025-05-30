<template>
  <div class="app-install">
    <!-- 顶部应用信息 -->
    <div class="app-header">
      <div class="app-basic-info">
        <img :src="app?.icon" :alt="app?.name" class="app-icon" />
        <div class="app-info">
          <h1>{{ app?.name }}</h1>
          <p class="app-desc">{{ app?.description }}</p>
          <div class="app-meta">
            <n-tag :type="app?.type === '官方应用' ? 'success' : 'info'" size="small">
              {{ app?.type }}
            </n-tag>
            <span class="meta-text">{{ app?.deployCount }}人安装过</span>
          </div>
        </div>
        <div class="header-actions">
          <n-tag v-if="!allImagesReady" type="warning" size="small">
            <template #icon><n-icon><DownloadOutline /></n-icon></template>
            需要拉取镜像
          </n-tag>
          <n-tag v-else type="success" size="small">
            <template #icon><n-icon><CheckmarkCircleOutline /></n-icon></template>
            准备就绪
          </n-tag>
          <n-button @click="handleBack" quaternary>返回</n-button>
          <n-button 
            type="primary" 
            @click="startInstall"
            :disabled="!allImagesReady"
          >
            立即安装
          </n-button>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="install-content">
      <!-- 服务组件 -->
      <div class="section">
        <div class="section-header expandable" @click="toggleServices">
          <h3>服务组件</h3>
          <n-icon size="20" :class="{ 'rotate-180': !servicesExpanded }">
            <ChevronDownOutline />
          </n-icon>
        </div>
        <div v-show="servicesExpanded" class="services-content">
          <div v-for="service in appServices" :key="service.name" class="service-item">
            <div class="service-main">
              <div class="service-icon">
                <n-icon size="20" color="#059669">
                  <SettingsOutline />
                </n-icon>
              </div>
              <div class="service-info">
                <div class="service-name">{{ service.name }}</div>
              </div>
              <div class="service-badges">
                <n-tag v-if="service.configUrl" type="info" size="small">
                  配置包
                </n-tag>
              </div>
            </div>
            
            <!-- 服务使用的镜像 -->
            <div class="service-image">
              <div class="image-info">
                <div class="image-icon">
                  <n-icon size="16" color="#0ea5e9">
                    <DownloadOutline />
                  </n-icon>
                </div>
                <div class="image-details">
                  <div class="image-name">{{ service.image }}</div>
                  <div class="image-size">{{ getImageSizeByName(service.image) }}</div>
                </div>
              </div>
              <div class="image-status">
                <n-tag :type="getImageStatusType(getImageStatusByName(service.image))" size="small">
                  {{ getImageStatusText(getImageStatusByName(service.image)) }}
                </n-tag>
              </div>
              <div class="image-actions">
                <n-button 
                  v-if="getImageStatusByName(service.image) === 'missing' || getImageStatusByName(service.image) === 'failed'"
                  size="small"
                  type="primary"
                  @click="pullImageByName(service.image)"
                >
                  {{ getImageStatusByName(service.image) === 'failed' ? '重试' : '拉取' }}
                </n-button>
                <n-button 
                  v-else-if="getImageStatusByName(service.image) === 'pulling'"
                  size="small"
                  loading
                  disabled
                >
                  拉取中
                </n-button>
              </div>
            </div>
            
            <!-- 拉取进度显示 -->
            <div v-if="getImageStatusByName(service.image) === 'pulling'" class="image-progress">
              <div class="progress-header">
                <span class="progress-text">拉取进度</span>
                <span class="progress-percent">{{ getImageProgressByName(service.image) }}%</span>
              </div>
              <n-progress 
                :percentage="getImageProgressByName(service.image)" 
                :height="6"
                :show-indicator="false"
                type="line"
                status="active"
                :key="`progress-${service.image}-${getImageProgressByName(service.image)}`"
              />
              
              <!-- 拉取日志 -->
              <div v-if="getImageLogsByName(service.image).length > 0" class="image-logs">
                <div class="logs-header">拉取日志</div>
                <div class="logs-content">
                  <div 
                    v-for="(log, index) in getImageLogsByName(service.image).slice(-3)" 
                    :key="index" 
                    class="log-item"
                  >
                    {{ log }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 配置参数 -->
      <div class="section">
        <div class="section-header expandable" @click="toggleConfig">
          <h3>配置参数</h3>
          <n-icon size="20" :class="{ 'rotate-180': !configExpanded }">
            <ChevronDownOutline />
          </n-icon>
        </div>
        
        <div v-show="configExpanded">
          <!-- 端口配置 -->
          <div v-if="portEnvs.length > 0" class="config-group">
            <div class="config-group-header">
              <h4>端口配置</h4>
              <span class="config-count">{{ portEnvs.length }} 个配置</span>
            </div>
            <div class="config-grid port-grid">
              <div v-for="env in portEnvs" :key="env.name" class="config-item port-item">
                <div class="config-header">
                  <span class="config-label">{{ env.description || env.name }}</span>
                  <div class="config-tags">
                    <n-tag v-if="env.required" type="error" size="tiny">必填</n-tag>
                    <n-tag type="info" size="tiny">端口</n-tag>
                  </div>
                </div>
                
                <div class="config-input">
                  <n-input 
                    v-model:value="env.value"
                    :placeholder="env.defaultValue || '请输入端口号'"
                    :type="env.sensitive ? 'password' : 'text'"
                    size="small"
                    clearable
                    @blur="handlePortCheck(env)"
                    @input="handleInputChange(env)"
                  >
                    <template #suffix>
                      <div class="port-status-actions">
                        <div class="port-status">
                          <n-spin v-if="portCheckStates[env.name]?.checking" size="small" />
                          <n-icon 
                            v-else-if="portCheckStates[env.name]?.available === true" 
                            size="14" 
                            color="#18a058"
                          >
                            <CheckmarkCircleOutline />
                          </n-icon>
                          <n-icon 
                            v-else-if="portCheckStates[env.name]?.available === false" 
                            size="14" 
                            color="#d03050"
                          >
                            <CloseCircleOutline />
                          </n-icon>
                        </div>
                        <div class="port-actions-inline">
                          <n-button 
                            size="tiny"
                            @click="checkPortAvailability(env)"
                            :loading="portCheckStates[env.name]?.checking"
                            :disabled="!env.value || !isValidPort(env.value)"
                          >
                            检测
                          </n-button>
                          <n-button 
                            v-if="portCheckStates[env.name]?.available === false" 
                            size="tiny"
                            type="primary"
                            @click="handleFindAvailablePort(env)"
                            :loading="findingPort[env.name]"
                          >
                            换端口
                          </n-button>
                        </div>
                      </div>
                    </template>
                  </n-input>
                </div>
              </div>
            </div>
          </div>

          <!-- 其他配置 -->
          <div v-if="otherEnvs.length > 0" class="config-group">
            <div class="config-group-header">
              <h4>其他配置</h4>
              <span class="config-count">{{ otherEnvs.length }} 个配置</span>
            </div>
            <div class="config-grid other-grid">
              <div v-for="env in otherEnvs" :key="env.name" class="config-item" :class="{ 'other-item': !isPathEnv(env), 'path-item': isPathEnv(env) }">
                <div class="config-header">
                  <span class="config-label">{{ env.description || env.name }}</span>
                  <div class="config-tags">
                    <n-tag v-if="env.required" type="error" size="tiny">必填</n-tag>
                    <n-tag v-if="isPathEnv(env)" type="warning" size="tiny">路径</n-tag>
                  </div>
                </div>
                
                <div class="config-input">
                  <PathSelector
                    v-if="isPathEnv(env)"
                    v-model="env.value"
                    :placeholder="env.defaultValue || '点击选择文件夹路径'"
                  />
                  <n-input 
                    v-else
                    v-model:value="env.value"
                    :placeholder="env.defaultValue || '请输入值'"
                    :type="env.sensitive ? 'password' : 'text'"
                    size="small"
                    clearable
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 安装进度模态框 -->
    <n-modal v-model:show="showInstallModal" :mask-closable="false" style="width: 600px;">
      <n-card title="正在安装应用" :bordered="false" size="huge" role="dialog" aria-modal="true">
        <div class="install-modal-content">
          <div class="install-progress">
            <n-progress
              type="line"
              :percentage="installProgress"
              :status="installStatus"
              :height="8"
              style="margin-bottom: 16px;"
            />
            <div class="progress-text">{{ progressText }}</div>
          </div>

          <div class="install-logs">
            <h4>安装日志</h4>
            <div class="log-container" ref="logContainer">
              <div v-for="log in installLogs" :key="log.id" class="log-item">
                <span class="log-time">{{ log.time }}</span>
                <span class="log-level" :class="log.level">{{ log.level.toUpperCase() }}</span>
                <span class="log-message">{{ log.message }}</span>
              </div>
            </div>
          </div>
        </div>

        <template #footer>
          <div class="modal-actions">
            <n-button 
              v-if="installStatus === 'error'"
              @click="retryInstall"
              type="warning"
            >
              重试安装
            </n-button>
            <n-button 
              v-if="installStatus === 'success'"
              type="primary"
              @click="finishInstall"
            >
              完成
            </n-button>
            <n-button 
              v-if="installStatus === 'active'"
              @click="cancelInstall"
              :disabled="true"
            >
              取消
            </n-button>
          </div>
        </template>
      </n-card>
    </n-modal>

    <!-- 安装成功结果 -->
    <div v-if="installFinished" class="install-result">
      <n-result status="success" title="安装成功" description="应用已成功部署并启动">
        <template #footer>
          <div class="result-actions">
            <n-button @click="goToContainers">管理容器</n-button>
            <n-button @click="installAnother">安装其他应用</n-button>
            <n-button type="primary" @click="openApp" v-if="accessUrls.length > 0">
              立即使用
            </n-button>
          </div>
        </template>
      </n-result>
      
      <!-- 访问信息 -->
      <div class="access-info" v-if="accessUrls.length > 0">
        <h4>访问地址</h4>
        <div class="access-list">
          <div v-for="access in accessUrls" :key="access.name" class="access-item">
            <span class="access-name">{{ access.name }}</span>
            <n-button 
              text 
              type="primary"
              @click="openUrl(access.url)"
            >
              {{ access.url }}
            </n-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import {
  DownloadOutline,
  SettingsOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  SearchOutline,
  RefreshOutline,
  ChevronDownOutline,
  ChevronUpOutline
} from '@vicons/ionicons5'

// 导入真实API
import {
  getInstallInfo,
  deployApplication as deployApplicationAPI,
  type ApplicationInstallInfo,
  type ImageStatusInfo,
  type EnvVarInfo,
  type ServiceInfo,
  type ApplicationDeployRequest,
  type ApplicationDeployResult
} from '@/api/http/applications'

// 导入WebSocket镜像拉取API
import { pullImage as pullImageWS } from '@/api/image'
import type { PullImageParams } from '@/api/model/imageModel'

// 导入任务管理器用于全局处理器
import { taskManager } from '@/api/websocket/websocketService'

// 导入端口检测API
import { checkPort, findAvailablePorts } from '@/api/http/port'

// 导入路径选择器组件
import PathSelector from '@/components/common/PathSelector.vue'

// 本地环境变量类型（支持description字段）
interface LocalEnvVarInfo {
  name: string
  description: string
  value: string
  defaultValue: string
  required: boolean
  sensitive: boolean
}

const route = useRoute()
const router = useRouter()
const message = useMessage()

// 状态
const installProgress = ref(0)
const installStatus = ref<'active' | 'success' | 'error'>('active')
const progressText = ref('')
const installLogs = ref<any[]>([])
const showDetails = ref(false)
const showInstallModal = ref(false)
const installFinished = ref(false)
const logContainer = ref<HTMLElement | null>(null)
const loading = ref(true)

// 应用信息
const app = ref<ApplicationInstallInfo['app'] | null>(null)

// 应用配置
const appConfig = ref({
  name: '',
  dataDir: './data'
})

// 镜像列表
const appImages = ref<ImageStatusInfo[]>([])

// 环境变量
const appEnvs = ref<LocalEnvVarInfo[]>([])

// 服务信息
const appServices = ref<ServiceInfo[]>([])

// 端口检测状态
const portCheckStates = ref<Record<string, { checking: boolean; available: boolean | null }>>({})

// 端口检测防抖
let portCheckTimeout: number | null = null

// 查找可用端口状态
const findingPort = ref<Record<string, boolean>>({})

// 镜像拉取进度和日志状态
const imageProgress = ref<Record<string, number>>({})
const imageLogs = ref<Record<string, string[]>>({})

// 展开/收起状态
const servicesExpanded = ref(true)
const configExpanded = ref(true)

// 智能分组计算属性
const portEnvs = computed(() => {
  return appEnvs.value.filter(env => 
    env.name.toLowerCase().includes('port') || 
    env.description?.toLowerCase().includes('端口') ||
    env.description?.toLowerCase().includes('port')
  )
})

const otherEnvs = computed(() => {
  return appEnvs.value.filter(env => !portEnvs.value.includes(env))
})

// 计算属性
const allImagesReady = computed(() => {
  return appImages.value.every(img => img.status === 'exists' || img.status === 'success')
})

const hasConfigPackages = computed(() => {
  return appServices.value.some(service => service.configUrl && service.configUrl.trim() !== '')
})

const accessUrls = computed(() => {
  const urls: Array<{name: string, url: string, description: string}> = []
  
  // 从环境变量推算访问地址
  appEnvs.value.forEach(env => {
    if (env.name.includes('PORT') && env.value) {
      const serviceName = env.name.split('_')[0].toLowerCase()
      urls.push({
        name: `${serviceName} 服务`,
        url: `http://localhost:${env.value}`,
        description: `${serviceName} 访问地址`
      })
    }
  })
  
  return urls
})

// 方法
const getImageStatusType = (status: string) => {
  switch (status) {
    case 'exists':
    case 'success':
      return 'success'
    case 'missing':
      return 'warning'
    case 'pulling':
      return 'info'
    case 'failed':
      return 'error'
    default:
      return 'default'
  }
}

const getImageStatusText = (status: string) => {
  switch (status) {
    case 'exists':
      return '已存在'
    case 'missing':
      return '需要拉取'
    case 'pulling':
      return '拉取中'
    case 'success':
      return '拉取成功'
    case 'failed':
      return '拉取失败'
    default:
      return '未知'
  }
}

const pullImage = async (image: any) => {
  console.log(`🚀 开始拉取镜像: ${image.name}`)
  image.status = 'pulling'
  
  // 强制初始化进度和日志（确保响应式更新）
  imageProgress.value = { ...imageProgress.value, [image.name]: 0 }
  imageLogs.value = { ...imageLogs.value, [image.name]: [] }
  
  // 强制DOM更新
  await nextTick()
  console.log(`📊 初始化完成: ${image.name} 进度=0% 日志=[]`)
  
  try {
    // 使用WebSocket API拉取镜像，支持实时进度和日志
    await pullImageWS(
      { imageName: image.name } as PullImageParams,
      {
        onProgress: (progress: number, taskId: string) => {
          console.log(`镜像 ${image.name} 拉取进度: ${progress}%`)
          // 使用展开操作符确保Vue能检测到对象变化
          imageProgress.value = { ...imageProgress.value, [image.name]: progress }
          // 强制Vue更新DOM
          nextTick(() => {
            console.log(`✅ 进度条已更新: ${image.name} - ${progress}%`)
          })
        },
        onLog: (log: string, taskId: string) => {
          console.log(`镜像 ${image.name} 拉取日志: ${log}`)
          const currentLogs = imageLogs.value[image.name] || []
          const newLogs = [...currentLogs, log]
          // 限制日志数量，只保留最近20条
          const trimmedLogs = newLogs.length > 20 ? newLogs.slice(-20) : newLogs
          // 使用展开操作符确保Vue能检测到对象变化
          imageLogs.value = { ...imageLogs.value, [image.name]: trimmedLogs }
          // 强制Vue更新DOM
          nextTick(() => {
            console.log(`✅ 日志已更新: ${image.name}`)
          })
        },
        onComplete: (data: any) => {
          image.status = 'success'
          // 使用展开操作符确保Vue能检测到对象变化
          imageProgress.value = { ...imageProgress.value, [image.name]: 100 }
          message.success(`${image.name} 拉取成功`)
        },
        onError: (error: string, taskId: string) => {
          image.status = 'failed'
          const currentLogs = imageLogs.value[image.name] || []
          // 使用展开操作符确保Vue能检测到对象变化
          imageLogs.value = { ...imageLogs.value, [image.name]: [...currentLogs, `❌ 错误: ${error}`] }
          message.error(`${image.name} 拉取失败: ${error}`)
        }
      }
    )
  } catch (error) {
    image.status = 'failed'
    const currentLogs = imageLogs.value[image.name] || []
    // 使用展开操作符确保Vue能检测到对象变化
    imageLogs.value = { ...imageLogs.value, [image.name]: [...currentLogs, `❌ 系统错误: ${error}`] }
    message.error(`${image.name} 拉取失败`)
  }
}

// 加载应用安装信息
const loadInstallInfo = async (appId: number) => {
  try {
    loading.value = true
    const installInfo = await getInstallInfo(appId)
    
    // 设置应用信息
    app.value = installInfo.app
    appConfig.value.name = installInfo.app.name
    
    // 设置镜像信息
    appImages.value = installInfo.images
    
    // 设置环境变量信息，支持新的对象结构
    appEnvs.value = installInfo.envVars.map(env => {
      let finalValue = ''
      let finalDescription = ''
      
      // 优先使用顶级的description字段
      if (env.description && env.description.trim()) {
        finalDescription = env.description
      }
      
      // 处理value字段
      if (typeof env.value === 'object' && env.value !== null) {
        // 新格式：{ value: "...", description: "..." }
        const objValue = env.value as { value: string; description: string }
        finalValue = objValue.value || env.defaultValue || ''
        
        // 如果顶级没有描述，使用value对象中的描述
        if (!finalDescription && objValue.description && objValue.description.trim()) {
          finalDescription = objValue.description
        }
      } else {
        // 字符串格式（包括普通字符串和可能的异常格式）
        const strValue = String(env.value || env.defaultValue || '')
        
        // 如果是异常的对象字符串格式，尝试解析
        if (strValue.startsWith('{') && strValue.includes('value=') && strValue.includes('description=')) {
          // 提取value和description（简单的正则处理）
          const valueMatch = strValue.match(/value=([^,}]+)/)
          const descMatch = strValue.match(/description=([^}]+)/)
          
          if (valueMatch) {
            finalValue = valueMatch[1].trim()
          }
          if (descMatch && !finalDescription) {
            finalDescription = descMatch[1].trim()
          }
        } else {
          finalValue = strValue
        }
      }
      
      return {
        ...env,
        value: finalValue,
        description: finalDescription
      }
    })
    
    // 设置服务信息
    appServices.value = installInfo.services
    
    console.log('加载安装信息成功:', installInfo)
  } catch (error) {
    console.error('加载应用安装信息失败:', error)
    message.error('加载应用安装信息失败')
  } finally {
    loading.value = false
  }
}

const startInstall = async () => {
  showInstallModal.value = true
  installProgress.value = 0
  installStatus.value = 'active'
  progressText.value = '开始安装应用...'
  installLogs.value = []
  
  try {
    if (!app.value) {
      throw new Error('应用信息不存在')
    }

    // 构建部署请求
    const deployRequest: ApplicationDeployRequest = {
      appName: appConfig.value.name,
      envVars: {},
      dataDir: appConfig.value.dataDir
    }
    
    // 收集环境变量 - 保持原有description，只更新value
    appEnvs.value.forEach(env => {
      if (env.value !== undefined && env.value !== null) {
        deployRequest.envVars[env.name] = {
          value: env.value || '',
          description: env.description || '' // 保持已有的description
        }
      }
    })
    
    // 调用部署API
    const result = await deployApplicationAPI(app.value.id, deployRequest)
    
    if (result.success) {
      installProgress.value = 100
      installStatus.value = 'success'
      progressText.value = '安装完成!'
      
      installLogs.value.push({
        id: Date.now(),
        time: new Date().toLocaleTimeString(),
        level: 'info',
        message: result.message
      })
    } else {
      installStatus.value = 'error'
      progressText.value = '安装失败'
      
      installLogs.value.push({
        id: Date.now(),
        time: new Date().toLocaleTimeString(),
        level: 'error',
        message: result.message
      })
    }
  } catch (error: any) {
    installStatus.value = 'error'
    progressText.value = '安装失败'
    
    installLogs.value.push({
      id: Date.now(),
      time: new Date().toLocaleTimeString(),
      level: 'error',
      message: error.message || '部署过程中发生错误'
    })
  }
}

const retryInstall = () => {
  startInstall()
}

const finishInstall = () => {
  showInstallModal.value = false
  installFinished.value = true
}

const cancelInstall = () => {
  showInstallModal.value = false
}

const handleBack = () => {
  router.back()
}

const goToContainers = () => {
  router.push('/containers')
}

const installAnother = () => {
  router.push('/appcenter')
}

const openApp = () => {
  openUrl(accessUrls.value[0].url)
}

const openUrl = (url: string) => {
  window.open(url, '_blank')
}

const getInputStatus = (env: LocalEnvVarInfo) => {
  if (isPortEnv(env) && portCheckStates.value[env.name]?.available === false) {
    return 'error'
  }
  return undefined
}

const handleInputChange = (env: LocalEnvVarInfo) => {
  if (isPortEnv(env) && env.value) {
    // 重置状态
    if (portCheckStates.value[env.name]) {
      portCheckStates.value[env.name].available = null
    }
    
    // 防抖检测
    if (portCheckTimeout) {
      clearTimeout(portCheckTimeout)
    }
    
    portCheckTimeout = window.setTimeout(() => {
      checkPortAvailability(env)
    }, 1000) // 1秒防抖
  }
}

const handlePortCheck = (env: LocalEnvVarInfo) => {
  if (isPortEnv(env) && env.value) {
    checkPortAvailability(env)
  }
}

const checkPortAvailability = async (env: LocalEnvVarInfo) => {
  const port = parseInt(env.value)
  
  // 验证端口号
  if (isNaN(port) || port <= 0 || port > 65535) {
    if (portCheckStates.value[env.name]) {
      portCheckStates.value[env.name] = { checking: false, available: null }
    }
    return
  }
  
  // 设置检测状态
  portCheckStates.value[env.name] = { checking: true, available: null }
  
  try {
    const available = await checkPort(port)
    
    portCheckStates.value[env.name] = { checking: false, available }
    
    if (!available) {
      message.warning(`端口 ${port} 已被占用，建议选择其他端口`)
    }
  } catch (error) {
    console.error('端口检测失败:', error)
    portCheckStates.value[env.name] = { checking: false, available: null }
    message.error(`端口 ${port} 检测失败`)
  }
}

const isPortEnv = (env: LocalEnvVarInfo) => {
  return env.name.toLowerCase().includes('port') || 
         env.description?.toLowerCase().includes('端口') ||
         env.description?.toLowerCase().includes('port')
}

// 判断是否为路径相关的环境变量
const isPathEnv = (env: LocalEnvVarInfo) => {
  const pathKeywords = ['path', 'dir', 'directory', 'folder', 'data', 'config', 'log', 'storage', 'volume', 'mount']
  const pathDescriptions = ['路径', '目录', '文件夹', '存储', '挂载', '地址']
  
  const nameMatch = pathKeywords.some(keyword => 
    env.name.toLowerCase().includes(keyword)
  )
  
  const descMatch = pathDescriptions.some(keyword =>
    env.description?.toLowerCase().includes(keyword)
  ) || pathKeywords.some(keyword =>
    env.description?.toLowerCase().includes(keyword)
  )
  
  return nameMatch || descMatch
}

const getImageSizeByName = (imageName: string) => {
  const image = appImages.value.find(img => img.name === imageName)
  return image?.size || '未知大小'
}

const getImageStatusByName = (imageName: string) => {
  const image = appImages.value.find(img => img.name === imageName)
  return image?.status || 'missing'
}

const getImageProgressByName = (imageName: string) => {
  return imageProgress.value[imageName] || 0
}

const getImageLogsByName = (imageName: string) => {
  return imageLogs.value[imageName] || []
}

const pullImageByName = async (imageName: string) => {
  const image = appImages.value.find(img => img.name === imageName)
  if (image) {
    await pullImage(image)
  }
}

const handleFindAvailablePort = async (env: LocalEnvVarInfo) => {
  const currentPort = parseInt(env.value)
  if (isNaN(currentPort)) return
  
  findingPort.value[env.name] = true
  
  try {
    // 在当前端口附近查找可用端口（±100范围内）
    const startPort = Math.max(1, currentPort - 50)
    const endPort = Math.min(65535, currentPort + 50)
    
    const availablePorts = await findAvailablePorts(startPort, endPort, 5)
    
    if (availablePorts && availablePorts.length > 0) {
      // 使用第一个可用端口
      const newPort = availablePorts[0]
      env.value = newPort.toString()
      
      // 更新检测状态
      portCheckStates.value[env.name] = { checking: false, available: true }
      
      message.success(`已为您找到可用端口: ${newPort}`)
    } else {
      message.error(`在 ${currentPort} 附近未找到可用端口，请手动输入其他端口`)
    }
  } catch (error) {
    console.error('查找可用端口失败:', error)
    message.error('查找可用端口失败，请手动输入其他端口')
  } finally {
    findingPort.value[env.name] = false
  }
}

// 重新建立正在进行的拉取任务的进度监听
const restoreActivePullTasks = () => {
  console.log('🔄 应用安装页面：检查并恢复正在进行的拉取任务...')
  
  // 设置全局消息处理器，处理页面刷新后的消息
  taskManager.setGlobalHandler({
    onProgress: (progress: number, taskId: string, imageName?: string) => {
      console.log(`📈 应用安装页面全局处理器收到进度更新: ${progress}% (taskId: ${taskId}, imageName: ${imageName})`)
      
      let targetImage = null
      
      if (imageName) {
        // 精确匹配镜像名称
        targetImage = appImages.value.find(img => img.name === imageName)
        if (targetImage) {
          console.log(`🎯 应用安装页面精确匹配到镜像: ${imageName}`)
        } else {
          console.warn(`⚠️ 应用安装页面未找到镜像: ${imageName}`)
        }
      } else {
        // 备用方案：查找正在拉取的镜像
        const pullingImages = appImages.value.filter(img => img.status === 'pulling')
        if (pullingImages.length === 1) {
          targetImage = pullingImages[0]
          console.log(`🎯 应用安装页面只有一个拉取任务，直接匹配: ${targetImage.name}`)
        } else if (pullingImages.length > 1) {
          targetImage = pullingImages[0] // 取第一个
          console.log(`🎯 应用安装页面多个拉取任务，选择第一个: ${targetImage.name}`)
        }
      }
      
      if (targetImage) {
        console.log(`✅ 应用安装页面更新镜像进度: ${targetImage.name} - ${progress}%`)
        targetImage.status = 'pulling'
        // 使用展开操作符确保Vue能检测到对象变化
        imageProgress.value = { ...imageProgress.value, [targetImage.name]: progress }
        // 强制Vue更新DOM
        nextTick(() => {
          console.log(`✅ 全局处理器进度条已更新: ${targetImage.name} - ${progress}%`)
        })
      }
    },
    
    onLog: (log: string, taskId: string, imageName?: string) => {
      console.log(`📝 应用安装页面全局处理器收到日志更新: ${log} (taskId: ${taskId}, imageName: ${imageName})`)
      
      let targetImage = null
      
      if (imageName) {
        targetImage = appImages.value.find(img => img.name === imageName)
      } else {
        const pullingImages = appImages.value.filter(img => img.status === 'pulling')
        if (pullingImages.length > 0) {
          targetImage = pullingImages[0]
        }
      }
      
      if (targetImage) {
        console.log(`✅ 应用安装页面更新镜像日志: ${targetImage.name} - ${log}`)
        const currentLogs = imageLogs.value[targetImage.name] || []
        const newLogs = [...currentLogs, log]
        // 限制日志数量
        const trimmedLogs = newLogs.length > 20 ? newLogs.slice(-20) : newLogs
        // 使用展开操作符确保Vue能检测到对象变化
        imageLogs.value = { ...imageLogs.value, [targetImage.name]: trimmedLogs }
      }
    },
    
    onComplete: (data: any, taskId: string) => {
      console.log(`✅ 应用安装页面全局处理器收到完成消息 (taskId: ${taskId})`)
      
      // 找到拉取中的镜像并标记完成
      const pullingImages = appImages.value.filter(img => img.status === 'pulling')
      for (const targetImage of pullingImages) {
        targetImage.status = 'success'
        // 使用展开操作符确保Vue能检测到对象变化
        imageProgress.value = { ...imageProgress.value, [targetImage.name]: 100 }
        console.log(`✅ 应用安装页面镜像拉取完成: ${targetImage.name}`)
      }
      
      message.success('镜像拉取完成')
    },
    
    onError: (error: string, taskId: string) => {
      console.error(`❌ 应用安装页面全局处理器收到错误消息: ${error} (taskId: ${taskId})`)
      
      // 找到拉取中的镜像并标记失败
      const pullingImages = appImages.value.filter(img => img.status === 'pulling')
      for (const targetImage of pullingImages) {
        targetImage.status = 'failed'
        const currentLogs = imageLogs.value[targetImage.name] || []
        // 使用展开操作符确保Vue能检测到对象变化
        imageLogs.value = { ...imageLogs.value, [targetImage.name]: [...currentLogs, `❌ 错误: ${error}`] }
        console.log(`❌ 应用安装页面镜像拉取失败: ${targetImage.name}`)
      }
      
      message.error('镜像拉取失败')
    }
  })
  
  const pullingImages = appImages.value.filter(img => img.status === 'pulling')
  if (pullingImages.length > 0) {
    console.log(`🔄 应用安装页面发现 ${pullingImages.length} 个正在拉取的镜像，全局处理器已设置`)
    pullingImages.forEach(img => {
      console.log(`📋 应用安装页面正在拉取: ${img.name}`)
    })
  } else {
    console.log('✅ 应用安装页面没有正在拉取的镜像')
  }
  
  console.log('✅ 应用安装页面拉取任务恢复检查完成，全局处理器已设置')
}

const isValidPort = (port: string) => {
  const parsedPort = parseInt(port)
  return !isNaN(parsedPort) && parsedPort > 0 && parsedPort <= 65535
}

// 展开/收起切换方法
const toggleServices = () => {
  servicesExpanded.value = !servicesExpanded.value
}

const toggleConfig = () => {
  configExpanded.value = !configExpanded.value
}

// 初始化
onMounted(async () => {
  console.log('🚀 AppInstall 组件初始化')
  // 根据路由参数获取应用ID
  const appId = parseInt(route.query.id as string) || 1
  
  // 加载应用安装信息
  await loadInstallInfo(appId)
  
  // 加载完成后，恢复正在进行的拉取任务监听
  restoreActivePullTasks()
})

// 组件卸载时清理WebSocket监听
onUnmounted(() => {
  console.log('🧹 AppInstall 组件卸载，清理全局处理器')
  // 清理全局处理器（如果需要的话）
})
</script>

<style scoped>
.app-install {
  padding: 16px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 120px);
}

/* 顶部应用信息 */
.app-header {
  margin-bottom: 24px;
}

.app-basic-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.app-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: contain;
  flex-shrink: 0;
}

.app-info {
  flex: 1;
  min-width: 0;
}

.app-info h1 {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.app-desc {
  margin: 0 0 8px 0;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.4;
}

.app-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.meta-text {
  color: #9ca3af;
  font-size: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* 主要内容区 */
.install-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section {
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafbfc;
}

.section-header.expandable {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.section-header.expandable:hover {
  background: #f1f5f9;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.section-header .n-icon {
  transition: transform 0.3s ease;
}

.section-header .n-icon.rotate-180 {
  transform: rotate(180deg);
}

/* 展开/收起动画 */
.services-content,
.config-group {
  transition: all 0.3s ease;
}

/* 组件区域 */
.services-content {
  padding: 20px 24px;
}

.service-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;
  margin-bottom: 16px;
}

.service-item:hover {
  border-color: #3b82f6;
  background: #f1f5f9;
}

.service-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.service-icon {
  flex-shrink: 0;
}

.service-info {
  flex: 1;
  min-width: 0;
}

.service-name {
  font-weight: 500;
  color: #1f2937;
  font-size: 14px;
  margin-bottom: 2px;
}

.service-badges {
  flex-shrink: 0;
  display: flex;
  gap: 6px;
  align-items: center;
}

.service-image {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 6px;
  border-left: 3px solid #0ea5e9;
}

.image-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.image-icon {
  flex-shrink: 0;
}

.image-details {
  flex: 1;
  min-width: 0;
}

.image-name {
  font-weight: 500;
  color: #1f2937;
  font-size: 12px;
  margin-bottom: 2px;
  font-family: 'Monaco', 'Consolas', monospace;
}

.image-size {
  color: #6b7280;
  font-size: 11px;
}

.image-status,
.image-actions {
  flex-shrink: 0;
  display: flex;
  gap: 6px;
  align-items: center;
}

/* 镜像拉取进度样式 */
.image-progress {
  margin-top: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
  border-left: 3px solid #3b82f6;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-text {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.progress-percent {
  font-size: 12px;
  color: #3b82f6;
  font-weight: 600;
}

.image-logs {
  margin-top: 12px;
}

.logs-header {
  font-size: 11px;
  color: #6b7280;
  font-weight: 500;
  margin-bottom: 6px;
}

.logs-content {
  background: #1f2937;
  border-radius: 4px;
  padding: 8px;
  max-height: 120px;
  overflow-y: auto;
}

.log-item {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 10px;
  color: #e5e7eb;
  line-height: 1.4;
  margin-bottom: 2px;
  word-break: break-word;
}

.log-item:last-child {
  margin-bottom: 0;
}

/* 配置网格 */
.config-grid {
  padding: 24px;
  display: grid;
  gap: 20px;
}

/* 端口配置 - 一行3个 */
.port-grid {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
}

@media (min-width: 1200px) {
  .port-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* 其他配置 - 一行2个 */
.other-grid {
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
}

@media (min-width: 1000px) {
  .other-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 配置组样式 */
.config-group {
  border-bottom: 1px solid #f3f4f6;
}

.config-group:last-child {
  border-bottom: none;
}

.config-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0 24px;
  margin-bottom: 4px;
}

.config-group-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.config-count {
  font-size: 12px;
  color: #6b7280;
  font-weight: 400;
}

.config-item {
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: white;
  transition: all 0.2s ease;
}

.config-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.port-item {
  border-left: 3px solid #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
}

.other-item {
  border-left: 3px solid #6b7280;
  background: linear-gradient(135deg, #f9fafb 0%, #f8fafc 100%);
}

.path-item {
  border-left: 3px solid #f59e0b;
  background: linear-gradient(135deg, #fffbeb 0%, #f8fafc 100%);
}

.config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.config-label {
  font-weight: 500;
  color: #1f2937;
  font-size: 14px;
}

.config-tags {
  display: flex;
  gap: 6px;
}

.config-input {
  margin-bottom: 12px;
}

.config-input :deep(.n-input) {
  width: 100%;
  margin-bottom: 8px;
}

.config-input :deep(.path-selector) {
  width: 100%;
}

.config-input :deep(.path-selector .n-input) {
  margin-bottom: 0;
}

.port-status-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.port-status {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
}

.port-actions-inline {
  display: flex;
  gap: 6px;
  align-items: center;
}

.port-actions-inline .n-button {
  height: 20px;
  font-size: 11px;
  padding: 0 6px;
}

/* 模态框样式 */
.install-modal-content {
  padding: 0;
}

.install-progress {
  margin-bottom: 24px;
}

.progress-text {
  text-align: center;
  color: #6b7280;
  margin-top: 8px;
  font-size: 14px;
}

.install-logs h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.log-container {
  max-height: 200px;
  overflow-y: auto;
  background: #1f2937;
  border-radius: 6px;
  padding: 12px;
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 11px;
  color: #e5e7eb;
}

.log-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 2px;
  line-height: 1.3;
}

.log-time {
  min-width: 60px;
  color: #9ca3af;
  flex-shrink: 0;
  font-size: 10px;
}

.log-level {
  min-width: 40px;
  font-weight: 600;
  flex-shrink: 0;
  font-size: 10px;
}

.log-level.info {
  color: #60a5fa;
}

.log-level.warn {
  color: #fbbf24;
}

.log-level.error {
  color: #f87171;
}

.log-message {
  flex: 1;
  min-width: 0;
  color: #e5e7eb;
  font-size: 11px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.install-result {
  margin-top: 24px;
}

.access-info {
  margin-top: 16px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.access-info h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.access-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.access-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f9fafb;
  border-radius: 6px;
}

.access-name {
  min-width: 80px;
  font-weight: 500;
  font-size: 13px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .port-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 1000px) {
  .other-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .app-install {
    padding: 12px;
  }
  
  .app-basic-info {
    flex-direction: column;
    text-align: center;
    gap: 12px;
    padding: 16px;
  }
  
  .header-actions {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .section-header {
    padding: 16px 20px;
  }
  
  .services-content,
  .config-grid {
    padding: 20px;
  }
  
  .port-grid,
  .other-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .install-content {
    gap: 20px;
  }
}

@media (max-width: 640px) {
  .app-install {
    padding: 8px;
  }
  
  .app-basic-info {
    padding: 12px;
  }
  
  .section-header {
    padding: 12px 16px;
  }
  
  .services-content,
  .config-grid {
    padding: 16px;
  }
  
  .port-grid,
  .other-grid {
    gap: 12px;
  }
  
  .config-item {
    padding: 16px;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 8px;
  }
}
</style>