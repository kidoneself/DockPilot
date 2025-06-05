<template>
  <div class="background-config">
    <!-- 系统预设 -->
    <div class="image-gallery">
      <div class="gallery-header">
        <h4>🎨 系统预设</h4>
      </div>
      
      <div class="gallery-grid">
        <!-- 默认背景 -->
        <div 
          class="gallery-item"
          :class="{ active: selectedBackground === defaultBackgroundImg }"
          @click="selectBackground(defaultBackgroundImg)"
        >
          <div class="gallery-thumbnail" :style="{ backgroundImage: `url(${defaultBackgroundImg})` }">
            <div class="system-badge">系统</div>
          </div>
          <div class="gallery-name">默认背景</div>
        </div>
        
        <!-- 自动随机背景 -->
        <div 
          class="gallery-item"
          :class="{ active: selectedBackground === 'auto-background' }"
          @click="selectBackground('auto-background')"
        >
          <div class="gallery-thumbnail auto-background-thumbnail">
            <div class="auto-background-icon">🌐</div>
            <div class="auto-background-text">自动背景</div>
            <div class="system-badge auto-badge">随机</div>
          </div>
          <div class="gallery-name">网络随机背景</div>
        </div>
        
        <!-- 透明背景选项 -->
        <div 
          class="gallery-item"
          :class="{ active: selectedBackground === '' }"
          @click="selectBackground('')"
        >
          <div class="gallery-thumbnail transparent-bg">
            <div class="transparent-icon">🚫</div>
          </div>
          <div class="gallery-name">无背景</div>
        </div>
      </div>
    </div>

    <!-- 自动背景配置区域 -->
    <div v-if="selectedBackground === 'auto-background'" class="auto-background-config">
      <div class="config-header">
        <h4>🌐 自动背景配置</h4>
      </div>
      
      <div class="config-form">
        <div class="form-item">
          <label>随机背景API地址：</label>
          <n-input
            v-model:value="autoBackgroundApiUrl"
            placeholder="请输入随机背景API地址，如：https://bing.img.run/rand_uhd.php"
            @input="handleApiUrlChange"
          />
        </div>
        
        <div class="config-tips">
          <div class="tip-item">
            <n-icon size="16" color="#10b981">
              <svg viewBox="0 0 24 24">
                <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
              </svg>
            </n-icon>
            <span>API应返回图片URL或直接返回图片文件</span>
          </div>
          <div class="tip-item">
            <n-icon size="16" color="#10b981">
              <svg viewBox="0 0 24 24">
                <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
              </svg>
            </n-icon>
            <span>支持必应、Unsplash等随机图片API</span>
          </div>
          <div class="tip-item">
            <n-icon size="16" color="#f59e0b">
              <svg viewBox="0 0 24 24">
                <path fill="currentColor" d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
              </svg>
            </n-icon>
            <span>请确保API支持CORS跨域访问</span>
          </div>
        </div>
        
        <div class="test-section">
          <n-button 
            type="primary" 
            size="small" 
            :loading="testingApi"
            :disabled="!autoBackgroundApiUrl"
            @click="testAutoBackgroundApi"
          >
            验证格式
          </n-button>
          
          <div class="test-notice">
            <n-alert type="info" size="small" style="margin-top: 8px;">
              💡 由于浏览器安全限制，无法直接测试外部API。请确保您的API支持CORS跨域访问，或在应用背景后查看效果。
            </n-alert>
          </div>
        </div>
      </div>
    </div>

    <!-- 本地图库 -->
    <div class="image-gallery">
      <div class="gallery-header">
        <h4>我的图库</h4>
        <n-button type="primary" size="small" @click="showUploadModal = true">
          上传图片
        </n-button>
      </div>

      <!-- 图片库网格 -->
      <div v-if="availableImages.length > 0" class="gallery-grid">
        <div 
          v-for="img in availableImages" 
          :key="img.url"
          class="gallery-item"
          :class="{ active: selectedBackground === img.url }"
          @click="selectBackground(img.url)"
        >
          <div class="gallery-thumbnail" :style="{ backgroundImage: `url(${img.url})` }">
            <n-button 
              size="tiny" 
              type="error" 
              class="delete-btn"
              @click.stop="handleDeleteImage(img)"
            >
              删除
            </n-button>
          </div>
          <div class="gallery-name">{{ img.name }}</div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-gallery">
        <div class="empty-icon">🖼️</div>
        <p>图库是空的</p>
        <p class="empty-tip">点击"上传图片"添加您的第一张背景图</p>
      </div>
    </div>

    <!-- 上传图片模态框 -->
    <n-modal
      v-model:show="showUploadModal"
      preset="card"
      :title="modalTitle"
      size="medium"
      :auto-focus="false"
      :close-on-esc="true"
      style="width: 480px; max-width: 90vw;"
      :mask-closable="true"
    >
      <ImageUpload 
        @upload-success="handleBackgroundUpload"
        @upload-error="handleUploadError"
      />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import ImageUpload from '@/components/ImageUpload.vue'
import type { FileUploadResponse } from '@/api/http/file-upload'
import { getAllImages, getImageUrl } from '@/api/http/file-upload'
import { getCurrentBackground } from '@/api/http/background'
import { getSetting, setSetting } from '@/api/http/system'
// 导入默认背景图片
import defaultBackgroundImg from '@/assets/background.png'

interface Props {
  modelValue?: string
  modalTitle?: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
}

const props = withDefaults(defineProps<Props>(), {
  modalTitle: '📸 上传图片'
})
const emit = defineEmits<Emits>()
const message = useMessage()

// 当前背景和选中的背景
const selectedBackground = ref(props.modelValue || '')

// 所有可用图片列表
const availableImages = ref<Array<{name: string, url: string, filename: string}>>([])

// 界面状态
const loading = ref(false)
const showUploadModal = ref(false)

// 自动背景配置
const autoBackgroundApiUrl = ref('')
const testingApi = ref(false)

// 加载所有可用图片
const loadAvailableImages = async () => {
  try {
    const images = await getAllImages()
    
    availableImages.value = images.map(img => ({
      name: img.name,
      url: getImageUrl(img.filename),
      filename: img.filename
    }))
    
    console.log('✅ 已加载所有可用图片:', availableImages.value.length, '张')
  } catch (error) {
    console.error('❌ 加载图片列表失败:', error)
    message.error('无法加载图片列表')
  }
}

// 加载当前背景设置为选中状态
const loadCurrentBackground = async () => {
  try {
    loading.value = true
    const backgroundUrl = await getCurrentBackground()
    
    // 如果有当前背景且props没有值，设置为选中
    if (backgroundUrl && !selectedBackground.value) {
      selectedBackground.value = backgroundUrl
    }
    
    console.log('✅ 当前背景已加载:', backgroundUrl)
  } catch (error) {
    console.error('❌ 加载背景配置失败:', error)
  } finally {
    loading.value = false
  }
}

// 选择背景
const selectBackground = async (url: string) => {
  // 如果点击的是已选中的图片，则取消选择
  if (selectedBackground.value === url) {
    selectedBackground.value = ''
    emit('update:modelValue', '')
  } else {
    // 否则选择新图片
    selectedBackground.value = url
    emit('update:modelValue', url)
  }
  // 只选择，不立即应用，等用户点击"应用背景"按钮
}

// 上传成功处理
const handleBackgroundUpload = async (response: FileUploadResponse) => {
  try {
    // 重新加载图片列表
    await loadAvailableImages()
    
    // 收起上传区域
    showUploadModal.value = false
    
    message.success(`图片上传成功: ${response.originalName}`)
    console.log('✅ 图片上传成功')
  } catch (error) {
    console.error('❌ 上传后刷新图库失败:', error)
    message.warning('图片上传成功，但刷新图库失败，请手动刷新页面')
  }
}

// 上传错误处理
const handleUploadError = (error: string) => {
  message.error(`上传失败: ${error}`)
}

// 删除图片
const handleDeleteImage = async (image: any) => {
  try {
    // 导入删除API
    const { deleteFile } = await import('@/api/http/file-upload')
    
    // 删除文件
    await deleteFile(image.filename)
    
    // 重新加载图片列表
    await loadAvailableImages()
    
    // 如果删除的是当前选中的背景，清空选择
    if (selectedBackground.value === image.url) {
      selectedBackground.value = ''
      emit('update:modelValue', '')
    }
    
    message.success(`已删除图片: ${image.name}`)
    console.log('✅ 图片已删除:', image.filename)
  } catch (error) {
    console.error('❌ 删除图片失败:', error)
    message.error('删除图片失败，请稍后重试')
  }
}

// 监听props变化
import { watch } from 'vue'
watch(() => props.modelValue, (newValue) => {
  if (newValue !== undefined) {
    selectedBackground.value = newValue
  }
})

// 自动背景配置相关方法
const handleApiUrlChange = () => {
  // 实时保存API地址变化
  saveAutoBackgroundConfig()
}

const testAutoBackgroundApi = async () => {
  if (!autoBackgroundApiUrl.value) {
    message.warning('请先输入API地址')
    return
  }

  testingApi.value = true
  try {
    // 简单的URL格式验证
    const url = new URL(autoBackgroundApiUrl.value)
    if (!url.protocol.startsWith('http')) {
      throw new Error('API地址必须以http://或https://开头')
    }
    
    message.success('API地址格式正确！请应用背景后查看实际效果')
    console.log('✅ API地址验证通过:', autoBackgroundApiUrl.value)
    
    // 保存配置
    await saveAutoBackgroundConfig()
  } catch (error) {
    console.error('❌ API地址验证失败:', error)
    const errorMsg = error instanceof Error ? error.message : String(error)
    message.error(`API地址格式错误: ${errorMsg}`)
  } finally {
    testingApi.value = false
  }
}

// 保存自动背景配置
const saveAutoBackgroundConfig = async () => {
  try {
    await setSetting({
      key: 'auto_background_api_url',
      value: autoBackgroundApiUrl.value
    })
    console.log('✅ 自动背景API配置已保存:', autoBackgroundApiUrl.value)
  } catch (error) {
    console.error('❌ 保存自动背景API配置失败:', error)
  }
}

// 加载自动背景配置
const loadAutoBackgroundConfig = async () => {
  try {
    const apiUrl = await getSetting('auto_background_api_url')
    if (apiUrl) {
      autoBackgroundApiUrl.value = apiUrl
      console.log('✅ 已加载自动背景API配置:', apiUrl)
    } else {
      // 设置默认API地址
      autoBackgroundApiUrl.value = 'https://bing.img.run/rand_uhd.php'
    }
  } catch (error) {
    console.error('❌ 加载自动背景API配置失败:', error)
    // 使用默认配置
    autoBackgroundApiUrl.value = 'https://bing.img.run/rand_uhd.php'
  }
}

onMounted(async () => {
  // 先加载所有可用图片
  await loadAvailableImages()
  
  // 然后加载当前背景配置
  await loadCurrentBackground()
  
  // 加载自动背景API配置
  await loadAutoBackgroundConfig()
})
</script>

<style scoped>
.background-config {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.image-gallery {
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  padding: 16px;
}

.image-gallery h4 {
  margin: 0 0 12px 0;
  color: var(--n-text-color);
  font-size: 16px;
  font-weight: 600;
}

.gallery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.gallery-item {
  border: 2px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--n-card-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.gallery-item:hover {
  border-color: var(--n-primary-color-hover);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.gallery-item.active {
  border-color: var(--n-primary-color);
  box-shadow: 0 4px 12px var(--n-primary-color-suppl);
}

.gallery-thumbnail {
  width: 100%;
  height: 120px;
  background-size: cover;
  background-position: center;
  position: relative;
  overflow: hidden;
}

.delete-btn {
  position: absolute;
  bottom: 8px;
  right: 8px;
  height: 24px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 1;
}

.gallery-item:hover .delete-btn {
  opacity: 1;
}

.gallery-name {
  padding: 8px 12px;
  font-size: 12px;
  color: var(--n-text-color);
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background: var(--n-card-color);
  border-top: 1px solid var(--n-border-color);
}

.empty-gallery {
  text-align: center;
  padding: 40px 20px;
  color: var(--n-text-color-3);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-gallery p {
  margin: 8px 0;
}

.empty-tip {
  font-size: 14px;
  color: var(--n-text-color-disabled);
}

/* 系统预设样式 */
.system-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: var(--n-primary-color);
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.transparent-bg {
  background: repeating-conic-gradient(#808080 0% 25%, transparent 0% 50%) 50% / 20px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.transparent-icon {
  font-size: 32px;
  opacity: 0.6;
}

/* 自动背景样式 */
.auto-background-thumbnail {
  background: linear-gradient(135deg, 
    #667eea 0%, 
    #764ba2 25%, 
    #f093fb 50%, 
    #f5576c 75%, 
    #4facfe 100%);
  background-size: 300% 300%;
  animation: autoBackgroundAnimation 6s ease infinite;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

@keyframes autoBackgroundAnimation {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.auto-background-icon {
  font-size: 28px;
  margin-bottom: 4px;
  opacity: 0.9;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.auto-background-text {
  font-size: 11px;
  color: white;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
  letter-spacing: 0.5px;
}

.auto-badge {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  font-weight: 600;
}

@media (max-width: 768px) {
  .gallery-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 12px;
  }
  
  .gallery-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .auto-background-icon {
    font-size: 20px;
  }
  
  .auto-background-text {
    font-size: 9px;
  }
}

/* 自动背景配置区域样式 */
.auto-background-config {
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  padding: 16px;
}

.config-header {
  margin-bottom: 16px;
}

.config-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.form-item label {
  margin-bottom: 8px;
  font-weight: 600;
}

.config-tips {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.test-section {
  text-align: right;
}

.test-notice {
  margin-top: 8px;
}
</style> 