<template>
  <div class="settings-container">
    <!-- 设置分组 -->
    <div class="settings-content">
      <div 
        v-for="group in settingsGroups" 
        :key="group.key" 
        class="settings-group"
      >
        <div class="group-header">
          <div class="group-info">
            <n-icon size="20" class="group-icon">
              <component :is="group.icon" />
            </n-icon>
            <div>
              <h3 class="group-title">{{ group.title }}</h3>
            </div>
          </div>
          <n-badge 
            :value="group.items.length" 
            :max="99"
            type="info"
            class="group-badge"
          />
        </div>
        
        <div class="group-items">
          <SettingCard
            v-for="item in group.items"
            :key="item.key"
            :setting="item"
            @config="() => openConfig(item)"
          />
        </div>
      </div>
    </div>

    <!-- 通用配置模态框 -->
    <ConfigModal
      v-model:show="showConfigModal"
      v-model:model-value="configData"
      :config="currentConfig"
      @confirm="handleConfigConfirm"
      @cancel="handleConfigCancel"
      @reset="handleConfigReset"
    >
      <!-- 使用插槽传入不同的配置组件 -->
      <template #content="{ data, update }">
        <!-- 背景配置 -->
        <BackgroundConfig
          v-if="currentConfigType === 'background'"
          :model-value="data"
          modal-title="📸 上传背景图片"
          @update:model-value="update"
        />
        
        <!-- 分类管理配置 -->
        <CategoryManageConfig
          v-else-if="currentConfigType === 'category-manage'"
          :model-value="data"
          @update:model-value="update"
        />
        
        <!-- 其他通用配置 -->
        <FormConfig
          v-else-if="currentConfigType === 'form' && currentFormFields.length > 0"
          :model-value="data"
          :fields="currentFormFields"
          :description="currentFormDescription"
          @update:model-value="update"
        />
        
        <!-- FormConfig加载失败时的占位符 -->
        <div v-else-if="currentConfigType === 'form'" class="config-placeholder">
          <n-empty description="配置表单加载失败">
            <template #extra>
              <n-button size="small" @click="handleConfigCancel">返回</n-button>
            </template>
          </n-empty>
        </div>

        <!-- 默认配置界面 -->
        <div v-else class="config-placeholder">
          <n-empty description="该功能的配置界面正在开发中">
            <template #extra>
              <n-button size="small" @click="handleConfigCancel">返回</n-button>
            </template>
          </n-empty>
        </div>
      </template>
    </ConfigModal>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import SettingCard from '@/components/SettingCard.vue'
import ConfigModal from '@/components/ConfigModal.vue'
import BackgroundConfig from '@/components/config/BackgroundConfig.vue'
import FormConfig from '@/components/config/FormConfig.vue'
import CategoryManageConfig from '@/components/config/CategoryManageConfig.vue'
import type { ConfigModalConfig } from '@/components/ConfigModal.vue'
import { getCurrentBackground, setCurrentBackground } from '@/api/http/background'
import { 
  getSetting, 
  setSetting, 
  testProxyLatency,
  getImageCheckInterval,
  updateImageCheckInterval
} from '@/api/http/system'
import {
  ColorPaletteOutline,
  SettingsOutline,
  FolderOutline,
  ShieldCheckmarkOutline,
  NotificationsOutline
} from '@vicons/ionicons5'

const message = useMessage()
const dialog = useDialog()

// 配置相关状态
const showConfigModal = ref(false)
const currentConfigType = ref('')
const configData = ref<any>({})
const currentConfig = ref<ConfigModalConfig>({
  title: '',
  width: '700px'
})
const currentFormFields = ref<any[]>([])
const currentFormDescription = ref('')

// 代理测速状态
const proxyTestLoading = ref(false)
// 配置加载状态
const configLoading = ref(false)

// 测试代理速度函数 - 安全版本
const testProxySpeed = async (proxyUrl: string) => {
  if (!proxyUrl || !proxyUrl.trim()) {
    message.warning('请先输入代理URL')
    return
  }

  if (proxyTestLoading.value) {
    message.warning('测速正在进行中，请稍候...')
    return
  }

  proxyTestLoading.value = true
  message.info('正在测试代理连接...')

  try {
    const result = await testProxyLatency()
    
    if (result.error) {
      message.error(`代理测试失败: ${result.error}`)
    } else {
      const totalTime = result.totalTime || 0
      if (totalTime < 500) {
        message.success(`代理连接成功，延迟: ${totalTime}ms (优秀)`)
      } else if (totalTime < 1000) {
        message.success(`代理连接成功，延迟: ${totalTime}ms (良好)`)
      } else if (totalTime < 2000) {
        message.warning(`代理连接成功，延迟: ${totalTime}ms (较慢)`)
      } else {
        message.warning(`代理连接成功，延迟: ${totalTime}ms (很慢)`)
      }
    }
  } catch (error) {
    console.error('代理测试失败:', error)
    message.error('代理测试失败，请检查代理设置')
  } finally {
    proxyTestLoading.value = false
  }
}

// 创建代理表单字段
const createProxyFormFields = () => {
  console.log('🔧 创建代理表单字段，当前loading状态:', proxyTestLoading.value)
  
  return [
    {
      key: 'url',
      label: '代理URL',
      type: 'input',
      placeholder: 'http://proxy.example.com:8080 或 http://user:pass@proxy.example.com:8080',
      required: false,
      suffix: {
        type: 'button',
        buttonType: 'primary',
        loading: proxyTestLoading.value,
        text: '测速',
        onClick: (value: string) => {
          console.log('🔧 测速按钮被点击，当前值:', value)
          try {
            if (typeof testProxySpeed === 'function') {
              testProxySpeed(value || '')
            } else {
              console.error('❌ testProxySpeed 函数未定义')
              message.error('测速功能暂时不可用')
            }
          } catch (error) {
            console.error('❌ 调用测速函数时发生错误:', error)
            message.error('测速功能调用失败')
          }
        }
      }
    }
  ]
}

// 监听loading状态变化，重新创建代理表单字段
watch(proxyTestLoading, () => {
  console.log('🔧 代理测速loading状态变化:', proxyTestLoading.value)
  // 只有在显示代理配置时才更新表单字段
  if (showConfigModal.value && currentConfig.value.title?.includes('代理')) {
    try {
      currentFormFields.value = createProxyFormFields()
      console.log('🔧 代理表单字段已更新')
    } catch (error) {
      console.error('❌ 更新代理表单字段失败:', error)
    }
  }
})

// 设置分组数据
const settingsGroups = ref([
  {
    key: 'appearance',
    title: '外观与个性化',
    description: '自定义界面外观和主题设置',
    icon: ColorPaletteOutline,
    items: [
      {
        key: 'background',
        title: '系统背景设置',
        desc: '自定义系统背景图片，让界面更个性化',
        configType: 'background',
        status: 'active' as const,
        icon: '🎨'
      },
      {
        key: 'theme',
        title: '主题设置',
        desc: '切换深色/浅色主题，调整界面风格',
        configType: 'default',
        status: 'developing' as const,
        icon: '🌙'
      }
    ]
  },
  {
    key: 'system',
    title: '系统与性能',
    description: '系统运行参数和性能优化设置',
    icon: SettingsOutline,
    items: [
      {
        key: 'proxy',
        title: '代理设置',
        desc: '配置HTTP代理，提升Docker镜像下载速度',
        configType: 'form',
        status: 'active' as const,
        icon: '🌐'
      },
      {
        key: 'mirrorUrls',
        title: '镜像加速',
        desc: '配置Docker镜像加速地址，提升镜像拉取速度',
        configType: 'form',
        status: 'active' as const,
        icon: '🚀'
      },
      {
        key: 'imageCheckInterval',
        title: '镜像检查间隔',
        desc: '设置自动检查镜像更新的时间间隔',
        configType: 'form',
        status: 'active' as const,
        icon: '⏰'
      },
      {
        key: 'dockerBaseDir',
        title: 'Docker运行目录',
        desc: '设置Docker容器运行和数据存储的基础目录',
        configType: 'form',
        status: 'active' as const,
        icon: '📁'
      }
    ]
  },
  {
    key: 'management',
    title: '管理与组织',
    description: '内容管理和组织结构设置',
    icon: FolderOutline,
    items: [
      {
        key: 'categoryManage',
        title: '分类管理',
        desc: '管理应用分类，支持增删改查和排序',
        configType: 'category-manage',
        status: 'active' as const,
        icon: '📁'
      }
    ]
  },
  {
    key: 'security',
    title: '安全与隐私',
    description: '安全设置和隐私保护配置',
    icon: ShieldCheckmarkOutline,
    items: [
      {
        key: 'security',
        title: '安全设置',
        desc: '配置访问权限和安全相关选项',
        configType: 'default',
        status: 'developing' as const,
        icon: '🔒'
      },
      {
        key: 'backup',
        title: '备份设置',
        desc: '配置自动备份和数据保护策略',
        configType: 'default',
        status: 'developing' as const,
        icon: '💾'
      }
    ]
  },
  {
    key: 'notifications',
    title: '通知与提醒',
    description: '通知方式和提醒设置',
    icon: NotificationsOutline,
    items: [
      {
        key: 'notification',
        title: '通知设置',
        desc: '配置系统通知方式和提醒设置',
        configType: 'default',
        status: 'developing' as const,
        icon: '🔔'
      }
    ]
  }
])

// 打开配置
const openConfig = async (item: any) => {
  console.log('🎯 配置按钮被点击了!', item)
  
  // 如果正在加载配置，则阻止重复请求
  if (configLoading.value) {
    message.warning('配置正在加载中，请稍候...')
    return
  }
  
  configLoading.value = true
  message.info(`正在加载 ${item.title} 的配置...`)
  
  try {
    currentConfigType.value = item.configType || 'default'
    
    // 根据不同的配置类型设置不同的配置
    switch (item.key) {
      case 'background':
        currentConfig.value = {
          title: '🎨 背景图片配置',
          width: '700px',
          confirmText: '应用背景',
          showResetButton: true,
          resetText: '清除背景',
          beforeConfirm: () => {
            // 验证背景数据
            return true
          },
          beforeReset: async () => {
            // 重置前确认
            return new Promise((resolve) => {
              dialog.warning({
                title: '确认重置',
                content: '确定要清除背景图片吗？',
                positiveText: '确定',
                negativeText: '取消',
                onPositiveClick: () => resolve(true),
                onNegativeClick: () => resolve(false)
              })
            })
          },
          afterConfirm: async (data) => {
            // 应用背景设置
            await applyBackground(data)
          },
          afterReset: async () => {
            await clearSystemBackground()
          }
        }
        
        // 从后端加载当前背景配置
        try {
          const backgroundUrl = await getCurrentBackground()
          configData.value = backgroundUrl || ''
        } catch {
          configData.value = ''
        }
        break

      case 'categoryManage':
        currentConfig.value = {
          title: '📁 分类管理',
          width: '800px',
          confirmText: '关闭',
          showResetButton: false,
          beforeConfirm: () => {
            // 分类管理不需要确认，直接关闭
            return true
          },
          afterConfirm: async () => {
            // 分类管理的保存操作在组件内部处理
            message.success('分类管理操作完成')
          }
        }
        
        // 分类管理不需要初始数据
        configData.value = {}
        break

      case 'proxy':
        currentConfig.value = {
          title: '🌐 代理配置',
          width: '600px',
          confirmText: '保存配置',
          showResetButton: true,
          resetText: '清除代理',
          beforeConfirm: async (data) => {
            // 简单验证：如果有内容就验证URL格式
            if (data.url && data.url.trim()) {
              const url = data.url.trim()
              if (!url.startsWith('http://') && !url.startsWith('https://')) {
                message.error('代理URL必须以 http:// 或 https:// 开头')
                return false
              }
            }
            return true
          },
          beforeReset: async () => {
            // 重置前确认
            return new Promise((resolve) => {
              dialog.warning({
                title: '确认重置',
                content: '确定要清除代理配置吗？',
                positiveText: '确定',
                negativeText: '取消',
                onPositiveClick: () => resolve(true),
                onNegativeClick: () => resolve(false)
              })
            })
          },
          afterConfirm: async (data) => {
            await saveProxyConfig(data)
          },
          afterReset: async () => {
            await resetProxyConfig()
          }
        }

        // 设置代理配置表单字段
        console.log('🔧 开始设置代理表单字段...')
        try {
          const formFields = createProxyFormFields()
          console.log('🔧 表单字段创建成功:', formFields)
          currentFormFields.value = formFields
          console.log('🔧 表单字段设置完成')
        } catch (error) {
          console.error('❌ 创建表单字段失败:', error)
          throw error
        }

        currentFormDescription.value = 
          '配置HTTP代理以提升Docker镜像下载速度。' +
          '支持格式：http://host:port 或 http://username:password@host:port。' +
          '留空表示禁用代理。'
        console.log('🔧 表单描述设置完成')

        // 从后端加载当前代理配置
        try {
          const proxyUrl = await getSetting('proxy')
          configData.value = {
            url: proxyUrl || ''
          }
          console.log('✅ 代理配置加载成功:', proxyUrl || '(未配置)')
        } catch (error) {
          console.error('加载代理配置失败:', error)
          message.warning('加载代理配置失败，使用默认值')
          configData.value = { url: '' }
        }
        break

      case 'mirrorUrls':
        currentConfig.value = {
          title: '🚀 镜像加速配置',
          width: '600px',
          confirmText: '保存配置',
          showResetButton: true,
          resetText: '清除加速',
          beforeConfirm: async (data) => {
            // 验证镜像加速地址格式
            if (data.urls && data.urls.trim()) {
              const urls = data.urls.trim().split('\n')
              for (const url of urls) {
                const trimmedUrl = url.trim()
                if (trimmedUrl && !isValidMirrorUrl(trimmedUrl)) {
                  message.error(`无效的镜像加速地址: ${trimmedUrl}`)
                  return false
                }
              }
            }
            return true
          },
          beforeReset: async () => {
            // 重置前确认
            return new Promise((resolve) => {
              dialog.warning({
                title: '确认重置',
                content: '确定要清除所有镜像加速地址吗？',
                positiveText: '确定',
                negativeText: '取消',
                onPositiveClick: () => resolve(true),
                onNegativeClick: () => resolve(false)
              })
            })
          },
          afterConfirm: async (data) => {
            await saveMirrorConfig(data)
          },
          afterReset: async () => {
            await resetMirrorConfig()
          }
        }

        // 设置镜像加速配置表单字段
        currentFormFields.value = [
          {
            key: 'urls',
            label: '镜像加速地址',
            type: 'textarea',
            placeholder: '每行一个加速地址，例如：\ndocker.1ms.run\ndocker.m.daocloud.io\ndockerhub.azk8s.cn',
            required: false,
            rows: 6
          }
        ]

        currentFormDescription.value = 
          '配置Docker镜像加速地址以提升镜像拉取速度。' +
          '每行输入一个加速地址，系统将按顺序尝试。' +
          '常用加速地址：docker.1ms.run、docker.m.daocloud.io、dockerhub.azk8s.cn 等。'

        // 从后端加载当前镜像加速配置
        try {
          const mirrorUrls = await getSetting('mirror_urls')
          configData.value = {
            urls: mirrorUrls || ''
          }
          console.log('✅ 镜像加速配置加载成功:', mirrorUrls || '(未配置)')
        } catch (error) {
          console.error('加载镜像加速配置失败:', error)
          message.warning('加载镜像加速配置失败，使用默认值')
          configData.value = { urls: '' }
        }
        break

      case 'imageCheckInterval':
        currentConfig.value = {
          title: '⏰ 镜像检查间隔配置',
          width: '600px',
          confirmText: '保存配置',
          showResetButton: true,
          resetText: '重置为默认',
          beforeConfirm: async (data) => {
            // 验证间隔值
            const interval = parseInt(data.interval)
            if (isNaN(interval) || interval < 10 || interval > 1440) {
              message.error('检查间隔必须在 10-1440 分钟之间')
              return false
            }
            return true
          },
          beforeReset: async () => {
            // 重置前确认
            return new Promise((resolve) => {
              dialog.warning({
                title: '确认重置',
                content: '确定要重置为默认间隔(60分钟)吗？',
                positiveText: '确定',
                negativeText: '取消',
                onPositiveClick: () => resolve(true),
                onNegativeClick: () => resolve(false)
              })
            })
          },
          afterConfirm: async (data) => {
            await saveImageCheckInterval(data)
          },
          afterReset: async () => {
            await resetImageCheckInterval()
          }
        }

        // 设置表单字段
        console.log('🔧 开始设置镜像检查间隔表单字段...')
        currentFormFields.value = [
          {
            key: 'interval',
            label: '检查间隔（分钟）',
            type: 'number',
            placeholder: '请输入检查间隔（10-1440分钟）',
            required: true,
            min: 10,
            max: 1440,
            step: 10
          }
        ]
        console.log('🔧 镜像检查间隔表单字段设置完成')

        currentFormDescription.value = 
          '设置系统自动检查Docker镜像更新的时间间隔。' +
          '范围：10-1440分钟（10分钟到24小时）。' +
          '间隔越短检查越频繁，但会消耗更多系统资源。'

        // 从后端加载当前配置
        try {
          const intervalStr = await getImageCheckInterval()
          const interval = intervalStr && !isNaN(parseInt(intervalStr)) ? parseInt(intervalStr) : 60
          configData.value = {
            interval: interval
          }
          console.log('✅ 镜像检查间隔配置加载成功:', interval, '分钟')
        } catch (error) {
          console.error('加载镜像检查间隔配置失败:', error)
          message.warning('加载配置失败，使用默认值(60分钟)')
          configData.value = { interval: 60 }
        }
        break
        
      case 'dockerBaseDir':
        currentConfig.value = {
          title: '📁 Docker运行目录配置',
          width: '600px',
          confirmText: '保存配置',
          showResetButton: true,
          resetText: '清空配置',
          beforeConfirm: async (data) => {
            // 验证目录路径格式
            const path = data.path ? data.path.trim() : ''
            if (!path) {
              message.error('Docker运行目录不能为空')
              return false
            }
            if (!path.startsWith('/')) {
              message.error('目录路径必须以 / 开头')
              return false
            }
            return true
          },
          beforeReset: async () => {
            // 重置前确认
            return new Promise((resolve) => {
              dialog.warning({
                title: '确认清空',
                content: '确定要清空Docker运行目录配置吗？清空后需要重新设置。',
                positiveText: '确定',
                negativeText: '取消',
                onPositiveClick: () => resolve(true),
                onNegativeClick: () => resolve(false)
              })
            })
          },
          afterConfirm: async (data) => {
            await saveDockerBaseDirConfig(data)
          },
          afterReset: async () => {
            await resetDockerBaseDirConfig()
          }
        }

        // 设置表单字段
        currentFormFields.value = [
          {
            key: 'path',
            label: 'Docker运行目录',
            type: 'input',
            placeholder: '请输入Docker运行目录的绝对路径，例如：/opt/docker',
            required: true
          }
        ]

        currentFormDescription.value = 
          '设置Docker容器运行和数据存储的基础目录。' +
          '此目录将用于存储所有容器的配置、数据等文件。' +
          '修改后将影响新部署的应用，已部署的应用不受影响。'

        // 从后端加载当前配置
        try {
          const dockerBaseDirStr = await getSetting('docker_base_dir')
          const dockerBaseDir = dockerBaseDirStr || ''
          configData.value = {
            path: dockerBaseDir
          }
          console.log('✅ Docker运行目录配置加载成功:', dockerBaseDir || '(未配置)')
        } catch (error) {
          console.error('加载Docker运行目录配置失败:', error)
          message.warning('加载配置失败，请重新设置')
          configData.value = { path: '' }
        }
        break
        
      default:
        currentConfig.value = {
          title: `⚙️ ${item.title}配置`,
          width: '500px',
          confirmText: '保存配置'
        }
        configData.value = {}
    }
    
    // 延迟显示配置模态框，确保数据完全准备好
    await nextTick()
    
    // 只对form类型验证必要数据
    if (currentConfigType.value === 'form') {
      if (!currentFormFields.value || currentFormFields.value.length === 0) {
        throw new Error('表单字段配置为空')
      }
      console.log('🔧 表单字段验证通过，字段数量:', currentFormFields.value.length)
    }
    
    console.log('🔧 准备显示配置模态框...')
    showConfigModal.value = true
    console.log('🔧 配置模态框显示状态设置完成')
    message.success(`${item.title} 配置已加载`)
    
  } catch (error) {
    console.error('打开配置失败:', error)
    const errorMessage = error instanceof Error ? error.message : String(error)
    message.error(`加载 ${item.title} 配置失败: ${errorMessage}`)
    
    // 重置状态
    showConfigModal.value = false
    currentConfigType.value = ''
    configData.value = {}
    currentFormFields.value = []
  } finally {
    // 确保loading状态重置
    configLoading.value = false
  }
}

// 配置确认处理
const handleConfigConfirm = (data: any) => {
  console.log('Config confirmed:', currentConfigType.value, data)
  
  switch (currentConfigType.value) {
    case 'background':
      // 背景配置的确认逻辑已在afterConfirm中处理
      break
      
    case 'proxy':
      // 代理配置的确认逻辑已在afterConfirm中处理
      break
      
    default:
      message.success('配置已保存')
  }
}

// 配置取消处理
const handleConfigCancel = () => {
  console.log('Config cancelled')
}

// 配置重置处理
const handleConfigReset = () => {
  console.log('Config reset:', currentConfigType.value)
}

// 应用背景
const applyBackground = async (backgroundUrl: string) => {
  if (backgroundUrl) {
    try {
      // 直接保存URL到后端（后端会处理URL的完整性）
      await setCurrentBackground(backgroundUrl)
      console.log('✅ 背景配置已保存:', backgroundUrl)
    } catch (error) {
      console.error('❌ 保存背景配置失败:', error)
      message.error('保存背景配置失败')
      return
    }
    
    // 直接使用保存的URL应用背景
    document.body.style.backgroundImage = `url(${backgroundUrl}?t=${Date.now()})`
    document.body.style.backgroundSize = 'cover'
    document.body.style.backgroundPosition = 'center'
    document.body.style.backgroundAttachment = 'fixed'
    document.body.style.backgroundRepeat = 'no-repeat'
    
    message.success('背景已应用')
  } else {
    await clearSystemBackground()
  }
}

// 清除系统背景
const clearSystemBackground = async () => {
  try {
    // 从后端清除
    await setCurrentBackground('')
    console.log('✅ 背景配置已从后端清除')
  } catch (error) {
    console.error('❌ 从后端清除背景配置失败:', error)
    message.error('清除背景配置失败')
    return
  }
  
  // 清除背景样式
  document.body.style.backgroundImage = ''
  document.body.style.backgroundSize = ''
  document.body.style.backgroundPosition = ''
  document.body.style.backgroundAttachment = ''
  document.body.style.backgroundRepeat = ''
  
  message.success('背景已清除')
}

// 保存代理配置
const saveProxyConfig = async (proxyData: any) => {
  try {
    const proxyUrl = proxyData.url ? proxyData.url.trim() : ''
    
    // 直接保存URL字符串到后端
    await setSetting({ key: 'proxy', value: proxyUrl })
    
    if (proxyUrl) {
      message.success('代理配置已保存')
      console.log('✅ 代理配置已保存:', proxyUrl)
      
      // 测试代理连接
      try {
        message.info('正在测试代理连接...')
        const testResult = await testProxyLatency()
        
        if (testResult.error) {
          message.warning('代理配置已保存，但连接测试失败，请检查代理设置')
        } else {
          const totalTime = testResult.totalTime || 0
          if (totalTime < 500) {
            message.success(`代理配置已保存并测试成功，延迟: ${totalTime}ms (优秀)`)
          } else if (totalTime < 1000) {
            message.success(`代理配置已保存并测试成功，延迟: ${totalTime}ms (良好)`)
          } else if (totalTime < 2000) {
            message.warning(`代理配置已保存并测试成功，延迟: ${totalTime}ms (较慢)`)
          } else {
            message.warning(`代理配置已保存并测试成功，延迟: ${totalTime}ms (很慢)`)
          }
        }
      } catch (testError) {
        console.error('代理测试失败:', testError)
        message.warning('代理配置已保存，但连接测试失败')
      }
    } else {
      message.success('代理已禁用')
      console.log('✅ 代理已禁用')
    }
  } catch (error) {
    console.error('❌ 保存代理配置失败:', error)
    message.error('保存代理配置失败')
    throw error
  }
}

// 重置代理配置
const resetProxyConfig = async () => {
  try {
    // 清除代理配置（保存空字符串）
    await setSetting({ key: 'proxy', value: '' })
    
    message.success('代理配置已清除')
    console.log('✅ 代理配置已重置')
    
    // 更新当前配置数据
    configData.value = { url: '' }
  } catch (error) {
    console.error('❌ 重置代理配置失败:', error)
    message.error('重置代理配置失败')
    throw error
  }
}

// 🎯 镜像检查间隔配置相关函数

// 保存镜像检查间隔配置
const saveImageCheckInterval = async (data: any) => {
  try {
    const interval = parseInt(data.interval)
    
    if (isNaN(interval) || interval < 10 || interval > 1440) {
      message.error('检查间隔必须在 10-1440 分钟之间')
      return
    }
    
    // 使用通用的setSetting API
    await updateImageCheckInterval(interval)
    
    message.success(`镜像检查间隔已更新为 ${interval} 分钟`)
    console.log('✅ 镜像检查间隔配置已保存:', interval, '分钟')
    
    // 配置会通过事件监听器自动热更新
    message.info('配置已自动热更新，无需重启服务')
    
  } catch (error) {
    console.error('❌ 保存镜像检查间隔配置失败:', error)
    message.error('保存配置失败: ' + (error as Error).message)
    throw error
  }
}

// 重置镜像检查间隔配置
const resetImageCheckInterval = async () => {
  try {
    // 重置为默认值 60 分钟
    await updateImageCheckInterval(60)
    
    message.success('镜像检查间隔已重置为默认值(60分钟)')
    console.log('✅ 镜像检查间隔配置已重置为默认值')
    
    // 更新当前配置数据
    configData.value = { interval: 60 }
    
    // 配置会通过事件监听器自动热更新
    message.info('配置已自动热更新，无需重启服务')
    
  } catch (error) {
    console.error('❌ 重置镜像检查间隔配置失败:', error)
    message.error('重置配置失败: ' + (error as Error).message)
    throw error
  }
}

// 🚀 镜像加速配置相关函数

// 验证镜像加速地址格式
const isValidMirrorUrl = (url: string): boolean => {
  // 简单的域名格式验证
  const domainRegex = /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/
  
  // 移除可能的协议前缀
  const cleanUrl = url.replace(/^https?:\/\//, '')
  
  // 检查是否包含端口号
  const [domain, port] = cleanUrl.split(':')
  
  // 验证域名
  if (!domainRegex.test(domain)) {
    return false
  }
  
  // 如果有端口号，验证端口号
  if (port) {
    const portNum = parseInt(port)
    if (isNaN(portNum) || portNum < 1 || portNum > 65535) {
      return false
    }
  }
  
  return true
}

// 保存镜像加速配置
const saveMirrorConfig = async (mirrorData: any) => {
  try {
    const urls = mirrorData.urls ? mirrorData.urls.trim() : ''
    
    // 验证每个URL
    if (urls) {
      const urlList = urls.split('\n')
      for (const url of urlList) {
        const trimmedUrl = url.trim()
        if (trimmedUrl && !isValidMirrorUrl(trimmedUrl)) {
          message.error(`无效的镜像加速地址: ${trimmedUrl}`)
          return
        }
      }
    }
    
    // 保存到后端
    await setSetting({ key: 'mirror_urls', value: urls })
    
    if (urls) {
      const urlCount = urls.split('\n').filter((url: string) => url.trim()).length
      message.success(`镜像加速配置已保存，共 ${urlCount} 个地址`)
      console.log('✅ 镜像加速配置已保存:', urls)
    } else {
      message.success('镜像加速已禁用')
      console.log('✅ 镜像加速已禁用')
    }
  } catch (error) {
    console.error('❌ 保存镜像加速配置失败:', error)
    message.error('保存镜像加速配置失败')
    throw error
  }
}

// 重置镜像加速配置
const resetMirrorConfig = async () => {
  try {
    // 清除镜像加速配置（保存空字符串）
    await setSetting({ key: 'mirror_urls', value: '' })
    
    message.success('镜像加速配置已清除')
    console.log('✅ 镜像加速配置已重置')
    
    // 更新当前配置数据
    configData.value = { urls: '' }
  } catch (error) {
    console.error('❌ 重置镜像加速配置失败:', error)
    message.error('重置镜像加速配置失败')
    throw error
  }
}

// 📁 Docker运行目录配置相关函数

// 保存Docker运行目录配置
const saveDockerBaseDirConfig = async (data: any) => {
  try {
    const path = data.path ? data.path.trim() : ''
    
    if (!path) {
      message.error('Docker运行目录不能为空')
      return
    }
    
    if (!path.startsWith('/')) {
      message.error('目录路径必须以 / 开头')
      return
    }
    
    // 保存到后端
    await setSetting({ key: 'docker_base_dir', value: path })
    
    message.success(`Docker运行目录已设置为: ${path}`)
    console.log('✅ Docker运行目录配置已保存:', path)
    
    // 配置会通过事件监听器自动热更新
    message.info('配置已自动热更新，新部署的应用将使用新目录')
    
  } catch (error) {
    console.error('❌ 保存Docker运行目录配置失败:', error)
    message.error('保存配置失败: ' + (error as Error).message)
    throw error
  }
}

// 重置Docker运行目录配置
const resetDockerBaseDirConfig = async () => {
  try {
    // 清空配置
    await setSetting({ key: 'docker_base_dir', value: '' })
    
    message.success('Docker运行目录配置已清空')
    console.log('✅ Docker运行目录配置已清空')
    
    // 更新当前配置数据
    configData.value = { path: '' }
    
    // 配置会通过事件监听器自动热更新
    message.info('配置已清空，请重新设置Docker运行目录')
    
  } catch (error) {
    console.error('❌ 清空Docker运行目录配置失败:', error)
    message.error('清空配置失败: ' + (error as Error).message)
    throw error
  }
}
</script>

<style scoped>
.settings-container {
  padding: 0;
  max-width: none;
  width: 100%;
  margin: 0;
  box-sizing: border-box;
}

/* 设置内容区域 */
.settings-content {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* 设置分组 */
.settings-group {
  background: var(--n-card-color);
  border-radius: 16px;
  border: 1px solid var(--n-border-color);
  overflow: hidden;
  transition: all 0.3s ease;
}

.settings-group:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, var(--n-color-target) 0%, var(--n-color-target-hover) 100%);
  border-bottom: 1px solid var(--n-border-color);
}

.group-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-icon {
  color: var(--n-primary-color);
}

.group-title {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--n-text-color-base);
}

.group-badge {
  opacity: 0.8;
}

.group-items {
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

/* 空状态 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  background: var(--n-card-color);
  border-radius: 16px;
  border: 1px solid var(--n-border-color);
}

.config-placeholder {
  padding: 20px;
  min-height: 200px;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .group-items {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .group-header {
    padding: 16px 20px;
  }
  
  .group-info {
    gap: 12px;
  }
  
  .group-items {
    grid-template-columns: 1fr;
    padding: 12px;
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .group-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .group-info {
    width: 100%;
  }
}
</style> 