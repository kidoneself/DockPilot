<template>
  <div class="app-install">
    <!-- 应用头部信息 -->
    <AppHeader 
      :app="app" 
      :all-images-ready="allImagesReady"
      @back="handleBack"
      @install="startInstall"
    />

    <!-- 主要内容区 -->
    <div class="install-content">
      <!-- 服务组件列表 -->
      <ServicesList 
        :services="appServices"
        :images="appImages"
        @pull-image="pullImageByName"
      />
      
      <!-- 配置编辑器 -->
      <ConfigEditor
        v-model:yaml-content="yamlContent"
        :original-yaml="originalYaml"
        @yaml-sync-error="handleYamlSyncError"
      />
    </div>

    <!-- 安装进度弹窗 -->
    <InstallModal
      v-model:show="showInstallModal"
      :progress="installProgress"
              :status="installStatus"
      :logs="installLogs"
      :progress-text="progressText"
      @retry="retryInstall"
      @finish="finishInstall"
      @cancel="cancelInstall"
    />
    
    <!-- 安装结果 -->
    <InstallResult
      v-if="installFinished"
      :result="installResult"
      :access-urls="accessUrls"
      @go-containers="goToContainers"
      @install-another="installAnother"
      @open-app="openApp"
      @open-url="openUrl"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import yaml from 'js-yaml'

// 导入组件
import AppHeader from './components/AppHeader.vue'
import ServicesList from './components/ServicesList.vue'
import ConfigEditor from './components/ConfigEditor.vue'
import InstallModal from './components/InstallModal.vue'
import InstallResult from './components/InstallResult.vue'

// 导入API和类型
import {
  getInstallInfo,
  installApplicationWS,
  type ApplicationInstallInfo,
  type ImageStatusInfo,
  type EnvVarInfo,
  type ServiceInfo,
  type ApplicationDeployResult,
  type AppInstallParams,
  type AppInstallCallbacks
} from '@/api/http/applications'

// 导入WebSocket镜像拉取API
import { pullImage as pullImageWS } from '@/api/image'
import type { PullImageParams } from '@/api/model/imageModel'

// 导入任务管理器用于全局处理器
import { taskManager } from '@/api/websocket/websocketService'

// 初始化基础状态
const route = useRoute()
const router = useRouter()
const message = useMessage()

// 安装状态
const installProgress = ref(0)
const installStatus = ref<'active' | 'success' | 'error'>('active')
const progressText = ref('')
const installLogs = ref<any[]>([])
const showInstallModal = ref(false)
const installFinished = ref(false)
const loading = ref(true)

// 应用数据
const app = ref<ApplicationInstallInfo['app'] | null>(null)
const appConfig = ref({ name: '' })
const appImages = ref<ImageStatusInfo[]>([])
const appServices = ref<ServiceInfo[]>([])
const installResult = ref<ApplicationDeployResult | null>(null)

// YAML编辑相关状态
const yamlContent = ref('')
const originalYaml = ref('')

// 计算属性
const allImagesReady = computed(() => {
  return appImages.value.every(img => img.status === 'exists' || img.status === 'success')
})

const accessUrls = computed(() => {
  // 优先使用安装结果中的访问地址
  if (installResult.value?.accessUrls) {
    return installResult.value.accessUrls
  }
  
  // 🎯 从yamlContent解析端口信息
  if (!yamlContent.value.trim()) {
    return []
  }
  
  try {
    const parsed = yaml.load(yamlContent.value) as any
    
    if (!parsed || !parsed.services) {
      return []
    }
    
    // 获取第一个服务的环境变量配置
    const serviceKey = Object.keys(parsed.services)[0]
    const service = parsed.services[serviceKey]
    
    if (!service || !service.environment) {
      return []
    }
    
    const urls: Array<{name: string, url: string, description: string}> = []
  const hostIp = window.location.hostname || 'localhost'
  
    // 从环境变量中提取端口信息
    Object.entries(service.environment).forEach(([key, value]) => {
      if (key.toUpperCase().includes('PORT') && value && isValidPort(String(value))) {
        const serviceName = key.replace('_PORT', '').replace('PORT', '')
      
      urls.push({
        name: serviceName,
          url: `http://${hostIp}:${value}`,
          description: `端口 ${value}`
      })
    }
  })
  
  return urls
  } catch (error) {
    console.warn('⚠️ 解析YAML端口信息失败:', error)
    return []
  }
})

// 工具方法
const isValidPort = (port: string) => {
  const portNum = parseInt(port.trim())
  return !isNaN(portNum) && portNum > 0 && portNum <= 65535
}

const getImageStatusByName = (imageName: string) => {
  const image = appImages.value.find(img => img.name === imageName)
  return image?.status || 'missing'
}

const getImageProgressByName = (imageName: string) => {
  const image = appImages.value.find(img => img.name === imageName) as any
  return image?.pullStatus?.percentage || 0
}

// 镜像拉取方法
const pullImage = async (image: any) => {
  console.log(`🚀 开始拉取镜像: ${image.name}`)
  image.status = 'pulling'
  
  // 使用和镜像列表相同的方式 - pullStatus 对象
  image.pullStatus = {
    status: 'pulling',
    percentage: 0,
    message: '开始拉取...'
  }
  
  try {
    await pullImageWS(
      { imageName: image.name } as PullImageParams,
      {
        onProgress: (progress: number, taskId: string) => {
          console.log(`📈 镜像 ${image.name} 拉取进度: ${progress}%`)
          image.pullStatus = {
            status: 'pulling',
            percentage: progress,
            message: `拉取进度: ${progress}%`
          }
        },
        onLog: (log: string, taskId: string) => {
          console.log(`📝 镜像 ${image.name} 拉取日志: ${log}`)
          image.pullStatus = {
            status: 'pulling',
            percentage: image.pullStatus?.percentage || 0,
            message: log
          }
        },
        onComplete: (data: any) => {
          console.log(`✅ 镜像拉取完成: ${image.name}`)
          image.status = 'success'
          image.pullStatus = {
            status: 'success',
            percentage: 100,
            message: '拉取完成'
          }
          message.success(`${image.name} 拉取成功`)
        },
        onError: (error: string, taskId: string) => {
          console.error(`❌ 镜像拉取失败: ${image.name} - ${error}`)
          image.status = 'failed'
          image.pullStatus = {
            status: 'failed',
            percentage: 0,
            message: '拉取失败',
            error: error
          }
          message.error(`${image.name} 拉取失败: ${error}`)
        }
      }
    )
  } catch (error) {
    console.error(`💥 镜像拉取系统错误: ${image.name} - ${error}`)
    image.status = 'failed'
    image.pullStatus = {
      status: 'failed',
      percentage: 0,
      message: '系统错误',
      error: String(error)
    }
    message.error(`${image.name} 拉取失败`)
  }
}

const pullImageByName = async (imageName: string) => {
  const image = appImages.value.find(img => img.name === imageName)
  if (image) {
    await pullImage(image)
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
    
    // 设置服务信息
    appServices.value = installInfo.services
    
    // 🎯 设置YAML内容（现在只需要这个，环境变量会从YAML实时解析）
    if (installInfo.yamlContent) {
      originalYaml.value = installInfo.yamlContent
      yamlContent.value = installInfo.yamlContent
    }
    
    console.log('加载安装信息成功:', installInfo)
  } catch (error) {
    console.error('加载应用安装信息失败:', error)
    message.error('加载应用安装信息失败')
  } finally {
    loading.value = false
  }
}

// 添加日志函数
const addLog = (level: 'info' | 'warn' | 'error', message: string) => {
  installLogs.value.push({
    id: Date.now() + Math.random(),
    time: new Date().toLocaleTimeString(),
    level,
    message
  })
}

// WebSocket安装函数
const startInstall = async () => {
  showInstallModal.value = true
  installProgress.value = 0
  installStatus.value = 'active'
  progressText.value = '准备开始安装...'
  installLogs.value = []
  installResult.value = null
  
  const params: AppInstallParams = {
    appId: app.value!.id,
    appName: appConfig.value.name
  }
  
  // 🎯 统一发送YAML内容（两种模式都编辑同一个YAML）
  params.yamlContent = yamlContent.value
  params.installMode = 'yaml'
  addLog('info', '使用YAML配置安装')
  
  // 🔍 调试信息：显示发送的YAML内容长度
  console.log('📤 发送YAML内容长度:', yamlContent.value.length)
  console.log('📤 YAML内容预览:', yamlContent.value.substring(0, 200) + '...')
  
  try {
    await installApplicationWS(params, {
      onProgress: (progress: number, taskId: string) => {
        installProgress.value = progress
        // 进度更新时添加日志
        if (progress === 100) {
          addLog('info', '安装完成!')
        }
      },
      onLog: (log: string, taskId: string) => {
        addLog('info', log)
      },
      onComplete: (result: ApplicationDeployResult) => {
        installProgress.value = 100
        installStatus.value = 'success'
        progressText.value = '安装完成!'
        addLog('info', '🎉 应用安装成功')
        
        // 保存安装结果
        installResult.value = result
      },
      onError: (error: string, taskId: string) => {
        installStatus.value = 'error'
        progressText.value = '安装失败'
        addLog('error', error)
      }
    })
  } catch (error) {
    installStatus.value = 'error'
    progressText.value = '安装失败'
    addLog('error', `系统错误: ${error}`)
  }
}

// 安装相关操作
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

// 导航操作
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

// 处理YAML同步错误
const handleYamlSyncError = (error: string) => {
  console.error('YAML同步错误:', error)
  message.error(`配置同步失败: ${error}`)
}

// 恢复拉取任务
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
        targetImage.status = 'pulling';
        (targetImage as any).pullStatus = {
          status: 'pulling',
          percentage: progress,
          message: `拉取进度: ${progress}%`
        }
      }
    },
    
    onLog: (log: string, taskId: string, imageName?: string) => {
      console.log(`📝 应用安装页面全局处理器收到日志更新: ${log} (taskId: ${taskId}, imageName: ${imageName})`)
    },
    
    onComplete: (data: any, taskId: string) => {
      console.log(`✅ 应用安装页面全局处理器收到完成消息 (taskId: ${taskId})`)
      
      // 找到拉取中的镜像并标记完成
      const pullingImages = appImages.value.filter(img => img.status === 'pulling')
      for (const targetImage of pullingImages) {
        targetImage.status = 'success';
        (targetImage as any).pullStatus = {
          status: 'success',
          percentage: 100,
          message: '拉取完成'
        }
        console.log(`✅ 应用安装页面镜像拉取完成: ${targetImage.name}`)
      }
      
      if (pullingImages.length > 0) {
        message.success('镜像拉取完成')
      }
    },
    
    onError: (error: string, taskId: string) => {
      console.error(`❌ 应用安装页面全局处理器收到错误消息: ${error} (taskId: ${taskId})`)
      
      // 找到拉取中的镜像并标记失败
      const pullingImages = appImages.value.filter(img => img.status === 'pulling')
      for (const targetImage of pullingImages) {
        targetImage.status = 'failed';
        (targetImage as any).pullStatus = {
          status: 'failed',
          percentage: 0,
          message: '拉取失败',
          error: error
        }
        console.log(`❌ 应用安装页面镜像拉取失败: ${targetImage.name}`)
      }
      
      if (pullingImages.length > 0) {
        message.error('镜像拉取失败')
      }
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

// 生命周期
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

/* 主要内容区 */
.install-content {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .app-install {
    padding: 12px;
  }
  
  .install-content {
    gap: 20px;
  }
}

@media (max-width: 640px) {
  .app-install {
    padding: 8px;
  }
}
</style>