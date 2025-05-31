<template>
  <n-modal 
    v-model:show="visible" 
    preset="card" 
    style="max-width: 600px;"
    :title="editMode ? '编辑应用' : '添加应用'"
    size="huge"
    :bordered="false"
    :segmented="false"
    :mask-closable="false"
    :close-on-esc="false"
    :closable="true"
    @update:show="handleVisibleChange"
  >

    <div class="add-app-form">
      <!-- 效果预览 -->
      <div class="preview-section">
        <div class="preview-options">
          <h4 style="margin: 0; color: #ffffff; font-size: 14px;">实时预览</h4>
          <span class="preview-tip">预览使用当前系统背景，效果更真实</span>
        </div>
        
        <!-- 预览区域 -->
        <div 
          class="preview-area real-background"
          :style="{ 
            backgroundImage: currentBackgroundImage ? `url(${currentBackgroundImage})` : 'none'
          }"
        >
          <!-- 透明度控制 - 简洁进度条 -->
          <div class="preview-opacity-slider">
            <n-slider
              v-model:value="opacityValue"
              :min="0"
              :max="100"
              :step="1"
              :tooltip="true"
              :format-tooltip="(value: number) => `透明度: ${value}%`"
              @update:value="handleOpacityChange"
              style="width: 150px;"
            />
          </div>
          
          <div 
            class="preview-card" 
            :class="{ 
              'preview-text': newApp.cardType === 'text',
              'preview-transparent': newApp.bgColor === 'transparent' || newApp.bgColor === 'rgba(0, 0, 0, 0)'
            }"
            :style="{ 
              backgroundColor: newApp.bgColor || 'rgba(42, 42, 42, 0.42)'
            }"
          >
            <div v-if="newApp.cardType !== 'text'" class="preview-icon">
              <!-- 文字图标 -->
              <span v-if="newApp.iconType === 'text'" class="preview-text-icon">
                {{ newApp.textContent || (newApp.title || 'A').charAt(0).toUpperCase() }}
              </span>
              <!-- 图片图标 -->
              <!-- <img 
                v-else-if="newApp.iconType === 'image' && newApp.iconUrl && !previewImageError" 
                :src="newApp.iconUrl" 
                alt=""
                @error="previewImageError = true"
                @load="previewImageError = false"
              >
              <div 
                v-else-if="newApp.iconType === 'image' && newApp.iconUrl && previewImageError"
                class="preview-fallback-icon"
                title="图片加载失败，显示文字图标"
              >
                {{ (newApp.title || 'A').charAt(0).toUpperCase() }}
              </div>
              <div 
                v-else-if="newApp.iconType === 'image' && !newApp.iconUrl"
                class="preview-placeholder-icon"
                title="请输入图片地址"
              >
                <n-icon :size="24" :component="ImageOutline" />
              </div> -->
              <!-- 在线图标 -->
              <img 
                v-else-if="newApp.iconType === 'online' && newApp.iconUrl && !previewImageError" 
                :src="newApp.iconUrl" 
                alt=""
                @error="previewImageError = true"
                @load="previewImageError = false"
              >
              <div 
                v-else-if="newApp.iconType === 'online' && newApp.iconUrl && previewImageError"
                class="preview-fallback-icon"
                title="在线图标加载失败，显示文字图标"
              >
                {{ (newApp.title || 'A').charAt(0).toUpperCase() }}
              </div>
              <div 
                v-else-if="newApp.iconType === 'online' && !newApp.iconUrl"
                class="preview-placeholder-icon"
                title="请输入网站地址获取图标"
              >
                <n-icon :size="24" :component="GlobeOutline" />
              </div>
            </div>
            <div class="preview-content">
              <div class="preview-title">{{ newApp.title || '应用标题' }}</div>
              <div class="preview-desc">{{ newApp.description || '应用描述' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 表单内容 -->
      <n-form :model="newApp" label-placement="top">
        <div class="form-row">
          <div class="form-item">
            <n-form-item label="分组" required>
              <n-select 
                v-model:value="newApp.category" 
                :options="categoryOptions"
                placeholder="选择分组"
              />
            </n-form-item>
          </div>
          <div class="form-item">
            <n-form-item label="卡片类型">
              <n-select 
                v-model:value="newApp.cardType" 
                :options="cardTypeOptions"
                placeholder="选择卡片类型"
              />
            </n-form-item>
          </div>
          <div class="form-item">
            <n-form-item label="卡片背景色">
              <div class="color-picker-section">
                <n-color-picker 
                  v-model:value="newApp.bgColor" 
                  :show-alpha="false"
                  :modes="['hex']"
                />
              </div>
            </n-form-item>
          </div>
        </div>

        <div class="form-row">
          <div class="form-item">
            <n-form-item label="标题" required>
              <n-input 
                v-model:value="newApp.title" 
                placeholder="请输入标题"
                :maxlength="20"
                show-count
              />
            </n-form-item>
          </div>
          <div class="form-item">
            <n-form-item label="描述信息">
              <n-input 
                v-model:value="newApp.description" 
                placeholder="请输入描述"
                :maxlength="100"
                show-count
              />
            </n-form-item>
          </div>
        </div>

        <n-form-item label="图标风格">
          <n-radio-group v-model:value="newApp.iconType">
            <n-radio value="text">文字</n-radio>
            <!-- <n-radio value="image">图片</n-radio> -->
            <n-radio value="online">在线图标</n-radio>
          </n-radio-group>
        </n-form-item>

        <!-- 文字图标 - 输入文本内容 -->
        <n-form-item v-if="newApp.iconType === 'text'" label="文本内容">
          <div class="icon-input-container">
            <n-input 
              v-model:value="newApp.textContent" 
              placeholder="最多2个字符"
              :maxlength="2"
              show-count
            />
          </div>
        </n-form-item>

        <!-- 图片图标 - 输入地址或上传 -->
        <!-- <n-form-item v-if="newApp.iconType === 'image'" label="图像地址">
          <div class="icon-input-container">
            <n-input 
              v-model:value="newApp.iconUrl" 
              placeholder="输入图标地址或上传"
            />
            <n-button>本地上传</n-button>
          </div>
        </n-form-item> -->

        <!-- 在线图标 - 输入网站地址或图标地址 -->
        <n-form-item v-if="newApp.iconType === 'online'" label="网站地址">
          <div class="icon-input-container">
            <n-input 
              v-model:value="newApp.iconUrl" 
              placeholder="输入网站地址或直接输入图标地址"
            />
            <n-button quaternary @click="getWebsiteIcon">获取图标</n-button>
          </div>
        </n-form-item>

        <n-form-item label="外网地址">
          <n-input 
            v-model:value="newApp.url" 
            placeholder="http(s)://"
          />
        </n-form-item>

        <n-form-item label="内网地址">
          <n-input 
            v-model:value="newApp.internalUrl" 
            placeholder="http(s):// (内网环境，会跳转该地址)"
          />
        </n-form-item>

        <n-form-item label="打开方式">
          <n-radio-group v-model:value="newApp.openType">
            <!-- <n-radio value="current">当前窗口</n-radio> -->
            <n-radio value="new">新窗口</n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
    </div>

    <template #action>
      <div class="modal-actions">
        <n-button type="primary" @click="handleSave">
          {{ editMode ? '更新' : '保存' }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { getFavicon } from '@/api/http/system'
import { getCurrentBackground } from '@/api/http/background'
import defaultBackgroundImg from '@/assets/background.png'
import {
  ImageOutline,
  GlobeOutline
} from '@vicons/ionicons5'

// Props
interface Props {
  modelValue?: boolean
  categoryOptions?: Array<{ label: string, value: any }>
  editMode?: boolean
  appData?: any
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  categoryOptions: () => [],
  editMode: false,
  appData: null
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'save': [appData: any]
  'update': [appData: any]
}>()

const message = useMessage()

// 弹窗显示状态
const visible = ref(props.modelValue)

// 预览区域图片错误状态
const previewImageError = ref(false)

// 当前背景图片
const currentBackgroundImage = ref('')

// 透明度滑块值
const opacityValue = ref(42) // 默认42%

// 新应用数据
const newApp = ref({
  category: '',
  cardType: 'normal',
  bgColor: '#2a2a2a6b',
  title: '',
  description: '',
  iconType: 'text', // 'text', 'image', 'online'
  textContent: '', // 文字图标的文本内容
  iconUrl: '', // 图片图标的地址
  websiteUrl: '', // 在线图标的网站地址
  url: '',
  internalUrl: '',
  openType: 'new' // 默认新窗口
})

// 卡片类型选项
const cardTypeOptions = ref([
  { label: '普通图标', value: 'normal' },
  { label: '文字卡片', value: 'text' }
])

// 监听编辑数据变化，回填表单
watch(() => props.appData, (newData) => {
  if (props.editMode && newData) {
    console.log('🔧 编辑模式回填数据:', newData)
    console.log('🏷️ categoryId:', newData.categoryId, '类型:', typeof newData.categoryId)
    console.log('📂 categoryOptions:', props.categoryOptions)
    
    // 回填编辑数据
    newApp.value = {
      category: newData.categoryId,
      cardType: newData.cardType || 'normal',
      bgColor: newData.bgColor || '#2a2a2a6b',
      title: newData.name || '',
      description: newData.description || '',
      iconType: newData.iconType || 'text',
      textContent: newData.iconType === 'text' ? newData.iconUrl : '',
      iconUrl: (newData.iconType === 'image' || newData.iconType === 'online') ? newData.iconUrl : '',
      websiteUrl: '',
      url: newData.externalUrl || '',
      internalUrl: newData.internalUrl || '',
      openType: newData.openType || 'new'
    }
    
    console.log('✅ 回填后的表单数据:', newApp.value)
  }
}, { immediate: true })

// 处理弹窗显示状态变化
const handleVisibleChange = (value: boolean) => {
  visible.value = value
  emit('update:modelValue', value)
}

// 关闭弹窗
const handleClose = () => {
  visible.value = false
  emit('update:modelValue', false)
  if (!props.editMode) {
    resetForm()
  }
}

// 保存应用
const handleSave = () => {
  if (!newApp.value.title || !newApp.value.category) {
    message.error('请填写必填字段（标题和分组）')
    return
  }

  if (props.editMode) {
    // 编辑模式，发出更新事件，包含原始ID
    emit('update', { 
      id: props.appData?.id,
      ...newApp.value 
    })
  } else {
    // 新增模式，发出保存事件
    emit('save', { ...newApp.value })
  }
  
  handleClose()
}

// 重置表单
const resetForm = () => {
  newApp.value = {
    category: '',
    cardType: 'normal',
    bgColor: '#2a2a2a6b',
    title: '',
    description: '',
    iconType: 'text',
    textContent: '',
    iconUrl: '',
    websiteUrl: '',
    url: '',
    internalUrl: '',
    openType: 'new' // 默认新窗口
  }
  previewImageError.value = false
  opacityValue.value = 42 // 重置透明度为42%
}

// 获取在线图标
const getWebsiteIcon = async () => {
  if (!newApp.value.iconUrl) {
    message.warning('请先输入网站地址')
    return
  }

  const loadingMessage = message.loading('正在获取网站图标...', { duration: 0 })
  
  try {
    // 从 iconUrl 字段获取网站地址
    const faviconUrl = await getFavicon(newApp.value.iconUrl)
    
    loadingMessage.destroy() // 关闭加载提示
    
    if (faviconUrl) {
      // 直接覆盖 iconUrl，实现回填效果
      newApp.value.iconUrl = faviconUrl
      previewImageError.value = false // 重置预览错误状态
      message.success(`图标获取成功！`)
      console.log('获取在线图标成功:', faviconUrl)
    } else {
      message.warning('未能获取到网站图标，请手动输入图片地址')
    }
  } catch (error) {
    loadingMessage.destroy() // 关闭加载提示
    console.error('获取在线图标失败:', error)
    message.error('获取图标失败，请检查网址是否正确')
  }
}

// 处理透明度滑块变化
const handleOpacityChange = (value: number) => {
  const currentColor = newApp.value.bgColor
  const opacity = value / 100 // 转换为0-1范围
  
  if (value === 0) {
    newApp.value.bgColor = 'transparent'
    return
  }
  
  // 如果当前是透明，使用默认灰色
  if (currentColor === 'transparent') {
    const alphaHex = Math.round(opacity * 255).toString(16).padStart(2, '0')
    newApp.value.bgColor = `#2a2a2a${alphaHex}`
    return
  }
  
  // 处理十六进制颜色
  if (currentColor.startsWith('#')) {
    let baseColor = currentColor
    if (currentColor.length === 9) {
      // 移除现有的透明度
      baseColor = currentColor.slice(0, 7)
    }
    const alphaHex = Math.round(opacity * 255).toString(16).padStart(2, '0')
    newApp.value.bgColor = `${baseColor}${alphaHex}`
    return
  }
  
  // 处理rgba格式 - 转换为十六进制
  if (currentColor.startsWith('rgba')) {
    const match = currentColor.match(/rgba\((.*?),(.*?),(.*?),.*?\)/)
    if (match) {
      const r = parseInt(match[1].trim())
      const g = parseInt(match[2].trim())
      const b = parseInt(match[3].trim())
      const baseHex = '#' + [r, g, b].map(x => x.toString(16).padStart(2, '0')).join('')
      const alphaHex = Math.round(opacity * 255).toString(16).padStart(2, '0')
      newApp.value.bgColor = `${baseHex}${alphaHex}`
      return
    }
  }
  
  // 其他情况，使用默认颜色
  const alphaHex = Math.round(opacity * 255).toString(16).padStart(2, '0')
  newApp.value.bgColor = `#2a2a2a${alphaHex}`
}

// 加载背景图片
const loadCurrentBackground = async () => {
  try {
    const backgroundUrl = await getCurrentBackground()
    if (backgroundUrl) {
      // 使用后端配置的背景
      currentBackgroundImage.value = backgroundUrl
    } else {
      // 使用默认背景图片
      currentBackgroundImage.value = defaultBackgroundImg
    }
    console.log('✅ 预览背景已加载:', currentBackgroundImage.value)
  } catch (error) {
    // 如果获取失败，使用默认背景
    currentBackgroundImage.value = defaultBackgroundImg
    console.log('⚠️ 获取背景失败，使用默认背景:', error)
  }
}

// 监听外部可见状态变化
watch(() => props.modelValue, (newValue) => {
  visible.value = newValue
})

// 监听颜色变化，同步透明度滑块
watch(() => newApp.value.bgColor, (newColor) => {
  if (newColor === 'transparent') {
    opacityValue.value = 0
  } else if (newColor.startsWith('#') && newColor.length === 9) {
    const alpha = parseInt(newColor.slice(7, 9), 16)
    opacityValue.value = Math.round((alpha / 255) * 100)
  } else if (newColor.startsWith('rgba')) {
    const match = newColor.match(/rgba\(.*?,.*?,.*?,(.*?)\)/)
    if (match) {
      opacityValue.value = Math.round(parseFloat(match[1]) * 100)
    }
  } else {
    // 纯色，设置为100%
    opacityValue.value = 100
  }
})

onMounted(() => {
  // 加载预览背景
  loadCurrentBackground()
})
</script>

<style scoped>
/* 添加应用弹窗样式 */
.add-app-form {
  padding: 20px 0;
}

.preview-section {
  margin-bottom: 24px;
}

.preview-options {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.preview-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-style: italic;
}

.preview-area {
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 120px;
  position: relative;
  overflow: hidden;
}

.preview-opacity-slider {
  position: absolute;
  top: 12px;
  right: 16px;
  z-index: 10;
  opacity: 0.9;
  transition: opacity 0.2s ease;
}

.preview-opacity-slider:hover {
  opacity: 1;
}

.preview-area.real-background {
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.preview-area.real-background::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  pointer-events: none;
  will-change: auto;
}

.preview-card {
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

/* 透明预览卡片样式 */
.preview-card.preview-transparent {
  background: transparent !important;
  backdrop-filter: none !important;
  border: none !important;
  box-shadow: none !important;
}

.preview-card.preview-transparent .preview-title,
.preview-card.preview-transparent .preview-desc {
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8);
}

.preview-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  font-size: 20px;
  font-weight: 700;
}

/* 透明卡片中的预览图标 */
.preview-card.preview-transparent .preview-icon {
  background: transparent;
}

.preview-icon img {
  width: 36px;
  height: 36px;
  border-radius: 8px;
}

/* 预览区域的降级图标样式 */
.preview-fallback-icon,
.preview-text-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

/* 预览区域的占位符图标样式 */
.preview-placeholder-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  border: 2px dashed rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
}

.preview-content {
  flex: 1;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 2px;
  line-height: 1.3;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.preview-desc {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.3;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

/* 预览文字卡片样式 */
.preview-card.preview-text {
  min-width: 180px;
  padding: 12px 16px;
  border-radius: 8px;
}

.preview-card.preview-text .preview-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 2px;
}

.preview-card.preview-text .preview-desc {
  font-size: 11px;
  opacity: 0.8;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.form-item {
  min-width: 0;
}

.color-picker-section {
  width: 100%;
}

/* 统一的图标输入容器样式 */
.icon-input-container {
  display: flex;
  gap: 8px;
  width: 100%;
}

.icon-input-container .n-input {
  flex: 1;
}

.icon-input-container .n-button {
  flex-shrink: 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 弹窗响应式 */
@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .preview-card {
    min-width: 160px;
    padding: 12px;
  }
  
  .preview-icon {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }
  
  .preview-icon img {
    width: 24px;
    height: 24px;
  }
}
</style> 