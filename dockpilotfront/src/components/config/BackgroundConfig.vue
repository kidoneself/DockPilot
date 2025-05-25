<template>
  <div class="background-config">
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

onMounted(async () => {
  // 先加载所有可用图片
  await loadAvailableImages()
  
  // 然后加载当前背景配置
  await loadCurrentBackground()
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
}
</style> 