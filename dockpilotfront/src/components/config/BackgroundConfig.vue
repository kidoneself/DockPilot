<template>
  <div class="background-config">
    <!-- 本地图库 -->
    <div class="image-gallery">
      <div class="gallery-header">
        <h4>我的图库</h4>
        <n-button @click="showUploadArea = !showUploadArea" type="primary" size="small">
          {{ showUploadArea ? '收起上传' : '上传图片' }}
        </n-button>
      </div>

      <!-- 上传区域 -->
      <div v-if="showUploadArea" class="upload-section">
        <ImageUpload 
          @upload-success="handleBackgroundUpload"
          @upload-error="handleUploadError"
        />
        <div class="upload-tips">
          <p>💡 建议使用 1920x1080 或更高分辨率的图片</p>
          <p>🎯 支持 JPG、PNG、GIF、WebP、SVG 格式，最大 10MB</p>
          <p>🌐 可上传本地文件或从网址下载图片</p>
        </div>
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
            <div class="gallery-overlay">
              <div class="gallery-actions">
                <n-button size="tiny" type="primary" @click.stop="selectBackground(img.url)">
                  选择
                </n-button>
                <n-button size="tiny" type="error" @click.stop="handleDeleteImage(img)">
                  删除
                </n-button>
              </div>
            </div>
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

      <!-- 清除背景按钮 -->
      <div v-if="selectedBackground" class="clear-background">
        <n-button @click="clearBackground" type="warning" size="small">
          清除背景
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import ImageUpload from '@/components/ImageUpload.vue'
import type { FileUploadResponse } from '@/api/http/file-upload'
import { getAllImages, getImageUrl, type ImageInfo } from '@/api/http/file-upload'
import { getCurrentBackground, setCurrentBackground } from '@/api/http/background'

interface Props {
  modelValue?: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const message = useMessage()

// 当前背景和选中的背景
const selectedBackground = ref(props.modelValue || '')

// 所有可用图片列表
const availableImages = ref<Array<{name: string, url: string, filename: string}>>([])

// 界面状态
const loading = ref(false)
const showUploadArea = ref(false)

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
  selectedBackground.value = url
  emit('update:modelValue', url)
  message.success('背景已选择，点击"应用背景"生效')
}

// 清除背景
const clearBackground = async () => {
  selectedBackground.value = ''
  emit('update:modelValue', '')
  message.success('背景已清除')
}

// 上传成功处理
const handleBackgroundUpload = async (response: FileUploadResponse) => {
  try {
    // 重新加载图片列表
    await loadAvailableImages()
    
    // 生成完整URL并自动选择新上传的背景
    const newImageUrl = getImageUrl(response.filename)
    selectBackground(newImageUrl)
    
    // 收起上传区域
    showUploadArea.value = false
    
    message.success(`背景图片上传成功: ${response.originalName}`)
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
      selectBackground('')
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
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.image-gallery h4 {
  margin: 0 0 12px 0;
  color: #374151;
  font-size: 16px;
  font-weight: 600;
}

.gallery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.upload-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px dashed #d1d5db;
}

.upload-tips {
  margin-top: 12px;
}

.upload-tips p {
  margin: 4px 0;
  font-size: 13px;
  color: #6b7280;
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
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.gallery-item:hover {
  border-color: #93c5fd;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.gallery-item.active {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.gallery-thumbnail {
  width: 100%;
  height: 120px;
  background-size: cover;
  background-position: center;
  position: relative;
  overflow: hidden;
}

.gallery-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.gallery-item:hover .gallery-overlay {
  opacity: 1;
}

.gallery-actions {
  display: flex;
  gap: 8px;
}

.gallery-name {
  padding: 8px 12px;
  font-size: 12px;
  color: #374151;
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background: #f9fafb;
}

.empty-gallery {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
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
  color: #9ca3af;
}

.clear-background {
  margin-top: 16px;
  text-align: center;
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