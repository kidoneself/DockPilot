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
import { ref } from 'vue'
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

// 功能列表
const features = ref([
  {
    key: 'background',
    title: '系统背景设置',
    desc: '自定义系统背景图片，让界面更个性化',
    configType: 'background'
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

// 配置确认处理
const handleConfigConfirm = (data: any) => {
  console.log('Config confirmed:', currentConfigType.value, data)
  
  switch (currentConfigType.value) {
    case 'background':
      // 背景配置的确认逻辑已在afterConfirm中处理
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