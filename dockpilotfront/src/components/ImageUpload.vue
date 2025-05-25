<template>
  <div class="image-upload">
    <!-- 模式切换 -->
    <div class="upload-modes">
      <button 
        :class="{ active: currentMode === 'file' }" 
        class="mode-btn"
        @click="currentMode = 'file'"
      >
        📁 上传文件
      </button>
      <button 
        :class="{ active: currentMode === 'url' }" 
        class="mode-btn"
        @click="currentMode = 'url'"
      >
        🔗 从URL下载
      </button>
    </div>

    <!-- 文件上传模式 -->
    <div 
      v-if="currentMode === 'file'" 
      class="upload-content"
      :class="{ 'is-dragging': isDragging }"
      @dragover.prevent="handleDragOver"
      @dragleave.prevent="handleDragLeave"
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @change="handleFileSelect"
      />
      
      <!-- 上传状态 -->
      <div v-if="!uploading && !uploadSuccess" class="upload-controls">
        <n-button
type="primary"
size="medium"
block
@click="triggerFileInput">
          📁 选择图片文件
        </n-button>
        <div class="spacer"></div>
        <p class="upload-tip">支持拖拽文件到此处 • JPG、PNG、GIF、WebP、SVG • 最大 10MB</p>
      </div>
      
      <!-- 上传进度 -->
      <div v-if="uploading" class="upload-progress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        </div>
        <p>上传中... {{ progress }}%</p>
      </div>
      
      <!-- 上传成功 -->
      <div v-if="uploadSuccess && !uploading" class="upload-success">
        <div class="success-icon">✅</div>
        <p>上传成功！</p>
        <p class="success-tip">{{ lastUploadedFile }}</p>
        <n-button size="small" type="primary" @click="resetUpload">再次上传</n-button>
      </div>
    </div>

    <!-- URL下载模式 -->
    <div v-if="currentMode === 'url'" class="upload-content">
      <div v-if="!downloading && !downloadSuccess" class="url-controls">
        <n-input 
          v-model:value="downloadUrl" 
          type="text" 
          placeholder="请输入图片URL地址..."
          style="margin-bottom: 12px;"
          @keyup.enter="handleUrlDownload"
        />
        <n-button
:disabled="!downloadUrl.trim()"
type="primary"
block
@click="handleUrlDownload">
          🔗 下载图片
        </n-button>
        <p class="upload-tip">支持网络图片链接 • JPG、PNG、GIF、WebP、SVG • 最大 10MB</p>
      </div>
      
      <!-- 下载进度 -->
      <div v-if="downloading" class="upload-progress">
        <div class="progress-bar">
          <div class="progress-fill downloading"></div>
        </div>
        <p>下载中...</p>
      </div>
      
      <!-- 下载成功 -->
      <div v-if="downloadSuccess && !downloading" class="upload-success">
        <div class="success-icon">✅</div>
        <p>下载成功！</p>
        <p class="success-tip">{{ lastDownloadedFile }}</p>
        <n-button size="small" type="primary" @click="resetDownload">再次下载</n-button>
      </div>
    </div>
    
    <!-- 错误信息 -->
    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { 
  uploadImage, 
  isImageFile, 
  isValidFileSize, 
  downloadImageFromUrl 
} from '@/api/http/file-upload'
import type { FileUploadResponse, DownloadImageRequest } from '@/api/http/file-upload'

// Props
interface Props {
  modelValue?: string  // v-model 支持
  maxSize?: number     // 最大文件大小（MB）
}

const props = withDefaults(defineProps<Props>(), {
  maxSize: 10
})

// Emits
interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'upload-success', response: FileUploadResponse): void
  (e: 'upload-error', error: string): void
}

const emit = defineEmits<Emits>()

// 响应式数据
const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)
const downloading = ref(false)
const uploadSuccess = ref(false)
const downloadSuccess = ref(false)
const progress = ref(0)
const errorMessage = ref('')
const currentMode = ref<'file' | 'url'>('file')
const downloadUrl = ref('')
const lastUploadedFile = ref('')
const lastDownloadedFile = ref('')
const isDragging = ref(false)

// 监听 v-model
watch(() => props.modelValue, (newValue) => {
  if (newValue && newValue !== '') {
    // 如果外部设置了值，重置状态
    resetUpload()
    resetDownload()
  }
}, { immediate: true })

// 触发文件选择
const triggerFileInput = () => {
  if (!uploading.value && !uploadSuccess.value) {
    fileInput.value?.click()
  }
}

// 重置上传状态
const resetUpload = () => {
  uploadSuccess.value = false
  errorMessage.value = ''
  lastUploadedFile.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 重置下载状态
const resetDownload = () => {
  downloadSuccess.value = false
  downloadUrl.value = ''
  errorMessage.value = ''
  lastDownloadedFile.value = ''
}

// 处理文件选择
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    handleFile(file)
  }
}

// 处理拖拽上传
const handleDrop = (event: DragEvent) => {
  isDragging.value = false
  const file = event.dataTransfer?.files[0]
  if (file) {
    handleFile(file)
  }
}

// 处理拖拽进入
const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  isDragging.value = true
}

// 处理拖拽离开
const handleDragLeave = (event: DragEvent) => {
  event.preventDefault()
  isDragging.value = false
}

// 处理文件上传
const handleFile = async (file: File) => {
  errorMessage.value = ''
  
  // 验证文件类型
  if (!isImageFile(file)) {
    errorMessage.value = '只支持图片文件格式 (JPG、PNG、GIF、WebP、SVG)'
    return
  }
  
  // 验证文件大小
  if (!isValidFileSize(file, props.maxSize)) {
    errorMessage.value = `文件大小不能超过 ${props.maxSize}MB`
    return
  }
  
  try {
    uploading.value = true
    uploadSuccess.value = false
    progress.value = 0
    
    const response = await uploadImage(file, (progressValue) => {
      progress.value = progressValue
    })
    
    // 上传成功
    lastUploadedFile.value = response.originalName
    uploadSuccess.value = true
    
    emit('upload-success', response)
    
  } catch (error) {
    const errorMsg = error instanceof Error ? error.message : '上传失败'
    errorMessage.value = errorMsg
    emit('upload-error', errorMsg)
  } finally {
    uploading.value = false
    progress.value = 0
  }
}

// 处理URL下载
const handleUrlDownload = async () => {
  if (!downloadUrl.value.trim()) return
  
  errorMessage.value = ''
  downloading.value = true
  downloadSuccess.value = false
  
  try {
    const request: DownloadImageRequest = {
      url: downloadUrl.value.trim()
    }
    
    const response = await downloadImageFromUrl(request)
    
    // 下载成功
    lastDownloadedFile.value = response.originalName
    downloadSuccess.value = true
    
    emit('upload-success', response)
    
  } catch (error) {
    const errorMsg = error instanceof Error ? error.message : '下载失败'
    errorMessage.value = errorMsg
    emit('upload-error', errorMsg)
  } finally {
    downloading.value = false
  }
}

// 暴露方法供外部调用
defineExpose({
  triggerFileInput,
  resetUpload,
  resetDownload
})
</script>

<style scoped>
.image-upload {
  width: 100%;
  max-width: 100%;
}

.upload-modes {
  display: flex;
  margin-bottom: 12px;
  gap: 4px;
  background: var(--n-border-color);
  border-radius: 6px;
  padding: 2px;
}

.mode-btn {
  flex: 1;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: var(--n-text-color);
  background: transparent;
  transition: all 0.2s ease;
}

.mode-btn:hover {
  background: var(--n-button-color-hover);
}

.mode-btn.active {
  background: var(--n-button-color-pressed);
  color: var(--n-button-text-color-pressed);
}

.upload-content {
  padding: 16px;
  text-align: center;
  border-radius: 6px;
  transition: all 0.2s ease;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.upload-content.is-dragging {
  border: 2px dashed var(--n-primary-color);
  background: var(--n-primary-color-suppl);
  transform: scale(1.02);
}

.upload-controls, .url-controls {
  color: var(--n-text-color);
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.spacer {
  height: 32px;
}

.upload-tip {
  font-size: 12px;
  color: var(--n-text-color-3);
  margin: 0;
  text-align: center;
}

.upload-progress {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: var(--n-border-color);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--n-primary-color);
  transition: width 0.3s;
}

.upload-success {
  color: var(--n-success-color);
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.success-icon {
  font-size: 32px;
}

.success-tip {
  font-size: 12px;
  color: var(--n-text-color-3);
  margin: 0;
}

.downloading {
  background: var(--n-primary-color);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.error-message {
  color: var(--n-error-color);
  font-size: 12px;
  margin-top: 8px;
  text-align: center;
  padding: 8px;
  background: var(--n-error-color-suppl);
  border: 1px solid var(--n-error-color);
  border-radius: 4px;
}
</style> 