<template>
  <div class="settings-container">
    <div class="feature-list">
      <FeatureCard
        v-for="item in features"
        :key="item.key"
        :title="item.title"
        :description="item.desc"
        :enabled="item.enabled"
        :model-value="item.enabled"
        @update:model-value="val => updateFeatureStatus(item, val)"
        @config="() => openConfig(item)"
        @run="() => runFeature(item)"
      />
    </div>

    <!-- 通用配置模态框 -->
    <ConfigModal
      v-model:show="showConfigModal"
      v-model:model-value="configData"
      :config="currentConfig"
      @confirm="handleConfigConfirm"
      @cancel="handleConfigCancel"
    >
      <!-- 使用插槽传入不同的配置组件 -->
      <template #content="{ data, update }">
        <!-- 背景配置 -->
        <BackgroundConfig
          v-if="currentConfigType === 'background'"
          :model-value="data"
          @update:model-value="update"
        />
        
        <!-- ALIST配置 -->
        <FormConfig
          v-else-if="currentConfigType === 'alist'"
          :model-value="data"
          :fields="alistFields"
          description="配置ALIST同步参数，用于生成基于Alist的strm文件。注意：不要用来同步115，记得关闭alist里面的签名（有两处）。"
          @update:model-value="update"
        />
        
        <!-- 封面生成配置 -->
        <FormConfig
          v-else-if="currentConfigType === 'cover'"
          :model-value="data"
          :fields="coverFields"
          description="配置媒体库封面生成参数，可以自动为你的媒体库生成精美海报，支持定时执行。"
          @update:model-value="update"
        />
        
        <!-- 115清空配置 -->
        <FormConfig
          v-else-if="currentConfigType === '115clear'"
          :model-value="data"
          :fields="clear115Fields"
          description="⚠️ 危险操作：清空115文件夹。请务必确保CID是正确的，清空后数据无法恢复！"
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
import { ref, computed } from 'vue'
import { useMessage } from 'naive-ui'
import FeatureCard from '@/components/FeatureCard.vue'
import ConfigModal from '@/components/ConfigModal.vue'
import BackgroundConfig from '@/components/config/BackgroundConfig.vue'
import FormConfig from '@/components/config/FormConfig.vue'
import type { ConfigModalConfig } from '@/components/ConfigModal.vue'
import { getCurrentBackground, setCurrentBackground } from '@/api/http/background'

const message = useMessage()

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

// 背景功能状态（动态加载）
const backgroundEnabled = ref(false)

// 加载背景功能状态
const loadBackgroundStatus = async () => {
  try {
    const backgroundUrl = await getCurrentBackground()
    backgroundEnabled.value = !!backgroundUrl
  } catch (error) {
    backgroundEnabled.value = false
  }
}

// ALIST配置字段
const alistFields = ref([
  {
    key: 'interval',
    label: '同步间隔',
    type: 'number',
    min: 1,
    max: 86400,
    step: 1,
    placeholder: '秒',
    required: true
  },
  {
    key: 'path',
    label: '目标路径',
    type: 'input',
    placeholder: '请输入目标路径，如: /media',
    required: true
  },
  {
    key: 'enableSign',
    label: '启用签名验证',
    type: 'switch'
  },
  {
    key: 'maxRetry',
    label: '最大重试次数',
    type: 'number',
    min: 0,
    max: 10,
    step: 1
  },
  {
    key: 'timeout',
    label: '请求超时时间',
    type: 'number',
    min: 5,
    max: 300,
    step: 5,
    placeholder: '秒'
  }
])

// 封面生成配置字段
const coverFields = ref([
  {
    key: 'quality',
    label: '海报质量',
    type: 'select',
    options: [
      { label: '高质量 (推荐)', value: 'high' },
      { label: '中等质量', value: 'medium' },
      { label: '低质量 (节省空间)', value: 'low' }
    ],
    required: true
  },
  {
    key: 'scheduled',
    label: '启用定时执行',
    type: 'switch'
  },
  {
    key: 'executeTime',
    label: '执行时间',
    type: 'time',
    placeholder: '选择执行时间'
  },
  {
    key: 'overwrite',
    label: '覆盖已存在的封面',
    type: 'switch'
  },
  {
    key: 'formats',
    label: '支持的格式',
    type: 'checkbox',
    options: [
      { label: 'JPG', value: 'jpg' },
      { label: 'PNG', value: 'png' },
      { label: 'WebP', value: 'webp' }
    ]
  },
  {
    key: 'maxWidth',
    label: '最大宽度',
    type: 'number',
    min: 300,
    max: 4000,
    step: 100,
    placeholder: '像素'
  }
])

// 115清空配置字段
const clear115Fields = ref([
  {
    key: 'cid',
    label: '文件夹CID',
    type: 'input',
    placeholder: '请输入要清空的文件夹CID',
    required: true,
    validator: (value: string) => {
      if (!value || value.length < 10) {
        return '请输入有效的CID（至少10位）'
      }
      return true
    }
  },
  {
    key: 'confirmText',
    label: '确认文本',
    type: 'input',
    placeholder: '请输入"我确认清空"',
    required: true,
    validator: (value: string) => {
      if (value !== '我确认清空') {
        return '请输入正确的确认文本'
      }
      return true
    }
  },
  {
    key: 'scheduled',
    label: '启用定时清空',
    type: 'switch'
  },
  {
    key: 'scheduleTime',
    label: '定时清空时间',
    type: 'time',
    placeholder: '选择定时清空时间'
  },
  {
    key: 'backupFirst',
    label: '清空前备份',
    type: 'switch'
  }
])

// 功能列表
const features = ref([
  {
    key: 'background',
    title: '系统背景设置',
    desc: '自定义系统背景图片，让界面更个性化',
    enabled: backgroundEnabled.value,
    configType: 'background'
  },
  {
    key: 'alist',
    title: 'ALIST同步',
    desc: '用于生成基于Alist的strm，不要用来同步115，记得关闭alist里面的签名（有两处）',
    enabled: false,
    configType: 'alist'
  },
  {
    key: 'cover',
    title: '媒体库封面生成',
    desc: '自动为你的媒体库生成精美海报，可定时执行',
    enabled: false,
    configType: 'cover'
  },
  {
    key: '115clear',
    title: '115文件夹清空',
    desc: '可以定时清空115文件夹，一定要保证cid是对的',
    enabled: false,
    configType: 'default'
  },
  {
    key: '115recycle',
    title: '115回收站清空',
    desc: '可以定时清空115回收站，数据无价，清空了就真没了',
    enabled: true,
    configType: 'default'
  },
  {
    key: 'aliyunfile',
    title: '阿里云盘文件夹清空',
    desc: '可以定时清空阿里云盘的文件夹，id一定要对',
    enabled: false,
    configType: 'default'
  },
  {
    key: 'aliyunrecycle',
    title: '阿里云盘回收站清空',
    desc: '可以定时清空阿里云盘回收站，数据无价，清空了就真没了',
    enabled: false,
    configType: 'default'
  },
  {
    key: 'strmreplace',
    title: 'STRM字符串替换',
    desc: '对.strm文件中的字符串进行替换，用于当cms地址发生变化时，替换strm文件中的cms地址',
    enabled: false,
    configType: 'default'
  }
])

// 更新功能状态
const updateFeatureStatus = async (item: any, enabled: boolean) => {
  item.enabled = enabled
  
  // 如果是背景功能被禁用，清除背景
  if (item.key === 'background' && !enabled) {
    await clearSystemBackground()
  }
}

// 打开配置
const openConfig = async (item: any) => {
  console.log('🎯 配置按钮被点击了!', item)
  message.success(`正在打开 ${item.title} 的配置`)
  
  currentConfigType.value = item.configType || 'default'
  
  // 根据不同的配置类型设置不同的配置
  switch (item.key) {
    case 'background':
      currentConfig.value = {
        title: '🎨 背景图片配置',
        width: '700px',
        confirmText: '应用背景',
        beforeConfirm: (data) => {
          // 验证背景数据
          return true
        },
        afterConfirm: async (data) => {
          // 应用背景设置
          await applyBackground(data)
        }
      }
      
      // 从后端加载当前背景配置
      try {
        const backgroundUrl = await getCurrentBackground()
        configData.value = backgroundUrl || ''
      } catch (error) {
        configData.value = ''
      }
      break
      
    case 'alist':
      currentConfig.value = {
        title: '⚙️ ALIST同步配置',
        width: '600px',
        confirmText: '保存配置'
      }
      configData.value = {
        interval: 300,
        path: '/media',
        enableSign: false,
        maxRetry: 3,
        timeout: 30
      }
      break
      
    case 'cover':
      currentConfig.value = {
        title: '🎬 封面生成配置',
        width: '600px',
        confirmText: '保存配置'
      }
      configData.value = {
        quality: 'high',
        scheduled: false,
        executeTime: null,
        overwrite: false,
        formats: ['jpg', 'png'],
        maxWidth: 1920
      }
      break
      
    case '115clear':
      currentConfig.value = {
        title: '🗑️ 115文件夹清空配置',
        width: '600px',
        confirmText: '保存配置',
        beforeConfirm: (data) => {
          // 验证危险操作
          if (data.confirmText !== '我确认清空') {
            message.error('请输入正确的确认文本')
            return false
          }
          if (!data.cid || data.cid.length < 10) {
            message.error('请输入有效的CID')
            return false
          }
          return true
        }
      }
      configData.value = {
        cid: '',
        confirmText: '',
        scheduled: false,
        scheduleTime: null,
        backupFirst: true
      }
      break
      
    case '115recycle':
      currentConfigType.value = 'form'
      currentConfig.value = {
        title: '🗑️ 115回收站清空配置',
        width: '500px',
        confirmText: '保存配置'
      }
      currentFormFields.value = [
        {
          key: 'confirmText',
          label: '确认文本',
          type: 'input',
          placeholder: '请输入"我确认清空回收站"',
          required: true,
          validator: (value: string) => {
            if (value !== '我确认清空回收站') {
              return '请输入正确的确认文本'
            }
            return true
          }
        },
        {
          key: 'scheduled',
          label: '启用定时清空',
          type: 'switch'
        },
        {
          key: 'scheduleTime',
          label: '定时清空时间',
          type: 'time',
          placeholder: '选择定时清空时间'
        }
      ]
      currentFormDescription.value = '⚠️ 危险操作：清空115回收站。数据无价，清空了就真没了！'
      configData.value = {
        confirmText: '',
        scheduled: false,
        scheduleTime: null
      }
      break
      
    case 'aliyunfile':
      currentConfigType.value = 'form'
      currentConfig.value = {
        title: '☁️ 阿里云盘文件夹清空配置',
        width: '600px',
        confirmText: '保存配置'
      }
      currentFormFields.value = [
        {
          key: 'folderId',
          label: '文件夹ID',
          type: 'input',
          placeholder: '请输入要清空的文件夹ID',
          required: true
        },
        {
          key: 'confirmText',
          label: '确认文本',
          type: 'input',
          placeholder: '请输入"我确认清空"',
          required: true,
          validator: (value: string) => {
            if (value !== '我确认清空') {
              return '请输入正确的确认文本'
            }
            return true
          }
        },
        {
          key: 'scheduled',
          label: '启用定时清空',
          type: 'switch'
        },
        {
          key: 'scheduleTime',
          label: '定时清空时间',
          type: 'time'
        }
      ]
      currentFormDescription.value = '⚠️ 危险操作：清空阿里云盘文件夹。ID一定要正确，清空后数据无法恢复！'
      configData.value = {
        folderId: '',
        confirmText: '',
        scheduled: false,
        scheduleTime: null
      }
      break
      
    case 'strmreplace':
      currentConfigType.value = 'form'
      currentConfig.value = {
        title: '🔄 STRM字符串替换配置',
        width: '600px',
        confirmText: '保存配置'
      }
      currentFormFields.value = [
        {
          key: 'sourcePattern',
          label: '源字符串',
          type: 'input',
          placeholder: '要替换的字符串或正则表达式',
          required: true
        },
        {
          key: 'targetString',
          label: '目标字符串',
          type: 'input',
          placeholder: '替换后的字符串',
          required: true
        },
        {
          key: 'useRegex',
          label: '使用正则表达式',
          type: 'switch'
        },
        {
          key: 'caseSensitive',
          label: '区分大小写',
          type: 'switch'
        },
        {
          key: 'backupOriginal',
          label: '备份原文件',
          type: 'switch'
        },
        {
          key: 'targetPath',
          label: '目标目录',
          type: 'input',
          placeholder: '指定要处理的.strm文件目录',
          required: true
        }
      ]
      currentFormDescription.value = '对.strm文件中的字符串进行替换，用于当CMS地址发生变化时，批量替换strm文件中的地址。'
      configData.value = {
        sourcePattern: '',
        targetString: '',
        useRegex: false,
        caseSensitive: false,
        backupOriginal: true,
        targetPath: ''
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
  
  showConfigModal.value = true
}

// 运行功能
const runFeature = async (item: any) => {
  switch (item.key) {
    case 'background':
      // 立即应用当前背景
      try {
        const backgroundUrl = await getCurrentBackground()
        if (backgroundUrl) {
          await applyBackground(backgroundUrl)
          message.success('背景已应用')
        } else {
          message.info('请先配置背景图片')
        }
      } catch (error) {
        message.error('获取背景配置失败')
      }
      break
      
    default:
      message.success(`正在运行：${item.title}`)
  }
}

// 配置确认处理
const handleConfigConfirm = (data: any) => {
  console.log('Config confirmed:', currentConfigType.value, data)
  
  switch (currentConfigType.value) {
    case 'background':
      // 背景配置的确认逻辑已在afterConfirm中处理
      break
      
    case 'alist':
      message.success('ALIST同步配置已保存')
      break
      
    case 'cover':
      message.success('封面生成配置已保存')
      break
      
    default:
      message.success('配置已保存')
  }
}

// 配置取消处理
const handleConfigCancel = () => {
  console.log('Config cancelled')
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
    
    // 更新功能状态
    backgroundEnabled.value = true
    const backgroundFeature = features.value.find(f => f.key === 'background')
    if (backgroundFeature) {
      backgroundFeature.enabled = true
    }
    
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
  
  // 更新功能状态
  backgroundEnabled.value = false
  const backgroundFeature = features.value.find(f => f.key === 'background')
  if (backgroundFeature) {
    backgroundFeature.enabled = false
  }
  
  message.success('背景已清除')
}

// 组件挂载时加载背景功能状态
import { onMounted } from 'vue'
onMounted(async () => {
  // 加载背景功能状态
  await loadBackgroundStatus()
})
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