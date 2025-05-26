<template>
  <div class="settings-container">
    <div class="feature-list">
      <FeatureCard
        v-for="item in features"
        :key="item.key"
        :title="item.title"
        :description="item.desc"
        @config="() => openConfig(item)"
      />
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
        
        <!-- 其他通用配置 -->
        <FormConfig
          v-else-if="currentConfigType === 'form'"
          :model-value="data"
          :fields="currentFormFields"
          :description="currentFormDescription"
          @update:model-value="update"
        />

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
import { ref, watch } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import FeatureCard from '@/components/FeatureCard.vue'
import ConfigModal from '@/components/ConfigModal.vue'
import BackgroundConfig from '@/components/config/BackgroundConfig.vue'
import FormConfig from '@/components/config/FormConfig.vue'
import type { ConfigModalConfig } from '@/components/ConfigModal.vue'
import { getCurrentBackground, setCurrentBackground } from '@/api/http/background'
import { 
  getSetting, 
  setSetting, 
  testProxyLatency, 
  testProxyLatencyWithUrl,
  getImageCheckInterval,
  updateImageCheckInterval
} from '@/api/http/system'

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

// 测试代理速度函数 - 提前定义
const testProxySpeed = async (proxyUrl: string) => {
  if (!proxyUrl || !proxyUrl.trim()) {
    message.warning('请先输入代理URL')
    return
  }

  const url = proxyUrl.trim()
  if (!url.startsWith('http://') && !url.startsWith('https://')) {
    message.error('代理URL必须以 http:// 或 https:// 开头')
    return
  }

  proxyTestLoading.value = true
  
  try {
    message.info('正在测试代理速度...')
    // 直接使用新的API测试指定代理URL，不影响当前配置
    const result = await testProxyLatencyWithUrl(url)
    
    if (result.error) {
      // 处理后端返回的具体错误信息
      if (result.message) {
        message.error('代理测试失败，请检查代理设置')
      } else {
        message.error('代理连接失败，请检查代理设置')
      }
    } else {
      const totalTime = result.totalTime || 0
      const httpTime = result.httpConnectTime || 0
      const httpsTime = result.httpsConnectTime || 0
      
      let speedLevel = ''
      let speedColor = ''
      
      if (totalTime < 500) {
        speedLevel = '优秀'
        speedColor = '🟢'
      } else if (totalTime < 1000) {
        speedLevel = '良好'
        speedColor = '🟡'
      } else if (totalTime < 2000) {
        speedLevel = '较慢'
        speedColor = '🟠'
      } else {
        speedLevel = '很慢'
        speedColor = '🔴'
      }
      
      message.success(`${speedColor} 代理测速完成！
📊 总延迟: ${totalTime}ms (${speedLevel})
🌐 HTTP: ${httpTime}ms
🔒 HTTPS: ${httpsTime}ms`, {
        duration: 5000
      })
    }
  } catch (error) {
    console.error('测试代理速度失败:', error)
    message.error('测试代理速度失败，请检查网络连接和代理设置')
  } finally {
    proxyTestLoading.value = false
  }
}

// 创建代理配置表单字段的函数
const createProxyFormFields = () => [
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
      onClick: testProxySpeed
    }
  }
]

// 监听loading状态变化，重新创建表单字段
watch(proxyTestLoading, () => {
  if (currentConfigType.value === 'proxy') {
    currentFormFields.value = createProxyFormFields()
  }
})

// 功能列表
const features = ref([
  {
    key: 'background',
    title: '系统背景设置',
    desc: '自定义系统背景图片，让界面更个性化',
    configType: 'background'
  },
  {
    key: 'proxy',
    title: '代理设置',
    desc: '配置HTTP代理，提升Docker镜像下载速度',
    configType: 'form'
  },
  {
    key: 'imageCheckInterval',
    title: '镜像检查间隔',
    desc: '设置自动检查镜像更新的时间间隔',
    configType: 'form'
  },
  {
    key: 'theme',
    title: '主题设置',
    desc: '切换深色/浅色主题，调整界面风格',
    configType: 'default'
  },
  {
    key: 'notification',
    title: '通知设置',
    desc: '配置系统通知方式和提醒设置',
    configType: 'default'
  },
  {
    key: 'security',
    title: '安全设置',
    desc: '配置访问权限和安全相关选项',
    configType: 'default'
  },
  {
    key: 'backup',
    title: '备份设置',
    desc: '配置自动备份和数据保护策略',
    configType: 'default'
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
      currentFormFields.value = createProxyFormFields()

      currentFormDescription.value = 
        '配置HTTP代理以提升Docker镜像下载速度。' +
        '支持格式：http://host:port 或 http://username:password@host:port。' +
        '留空表示禁用代理。'

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
      
    default:
      currentConfig.value = {
        title: `⚙️ ${item.title}配置`,
        width: '500px',
        confirmText: '保存配置'
      }
      configData.value = {}
    }
    
    // 显示配置模态框
    showConfigModal.value = true
    message.success(`${item.title} 配置已加载`)
    
  } catch (error) {
    console.error('打开配置失败:', error)
    message.error(`加载 ${item.title} 配置失败`)
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


</script>

<style scoped>
.settings-container {
  padding: 0;
  max-width: none;
  width: 100%;
  margin: 0;
  box-sizing: border-box;
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 24px;
  width: 100%;
}

.config-placeholder {
  padding: 20px;
  min-height: 200px;
}

/* 响应式调整 */
@media (max-width: 1400px) {
  .feature-list {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 1100px) {
  .feature-list {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 800px) {
  .feature-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 500px) {
  .feature-list {
    grid-template-columns: 1fr;
  }
}
</style> 