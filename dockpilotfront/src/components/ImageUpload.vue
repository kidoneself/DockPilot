<template>
  <div class="image-upload">
    <!-- 模式切换 -->
    <div class="upload-modes">
      <button 
        @click="currentMode = 'file'" 
        :class="{ active: currentMode === 'file' }"
        class="mode-btn"
      >
        📁 上传文件
      </button>
      <button 
        @click="currentMode = 'url'" 
        :class="{ active: currentMode === 'url' }"
        class="mode-btn"
      >
        🔗 从URL下载
      </button>
    </div>

    <!-- 文件上传模式 -->
    <div v-if="currentMode === 'file'" class="upload-area" @click="triggerFileInput" @dragover.prevent @drop.prevent="handleDrop">
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        @change="handleFileSelect"
        style="display: none"
      />
      
      <!-- 上传状态 -->
      <div v-if="!uploading && !uploadSuccess" class="upload-placeholder">
        <div class="upload-icon">📸</div>
        <p>点击或拖拽图片到这里上传</p>
        <p class="upload-tip">支持 JPG、PNG、GIF、WebP、SVG 格式，最大 10MB</p>
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
        <n-button @click="resetUpload" size="small" type="primary">再次上传</n-button>
      </div>
    </div>

    <!-- URL下载模式 -->
    <div v-if="currentMode === 'url'" class="url-download-area">
      <div v-if="!downloading && !downloadSuccess" class="url-input-area">
        <div class="url-icon">🌐</div>
        <p>从网址下载图片</p>
        <input 
          v-model="downloadUrl" 
          type="url" 
          placeholder="请输入图片URL地址..."
          class="url-input"
          @keyup.enter="handleUrlDownload"
        />
        <input 
          v-model="customName" 
          type="text" 
          placeholder="自定义名称（可选）"
          class="name-input"
        />
        <button @click="handleUrlDownload" :disabled="!downloadUrl.trim()" class="download-btn">
          下载图片
        </button>
        <p class="upload-tip">支持 JPG、PNG、GIF、WebP、SVG 格式，最大 10MB</p>
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
        <n-button @click="resetDownload" size="small" type="primary">再次下载</n-button>
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
import { uploadImage, isImageFile, isValidFileSize, downloadImageFromUrl } from '@/api/http/file-upload'
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
const customName = ref('')
const lastUploadedFile = ref('')
const lastDownloadedFile = ref('')

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
  customName.value = ''
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
  const file = event.dataTransfer?.files[0]
  if (file) {
    handleFile(file)
  }
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
      url: downloadUrl.value.trim(),
      name: customName.value.trim() || undefined
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
  max-width: 400px;
}

.upload-modes {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.mode-btn {
  flex: 1;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: white;
  background-color: #6c757d;
  transition: background-color 0.3s;
  margin: 0 5px;
}

.mode-btn.active {
  background-color: #007bff;
}

.upload-area, .url-download-area {
  border: 2px dashed #ddd;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.3s;
  position: relative;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-area:hover {
  border-color: #007bff;
}

.upload-placeholder, .url-input-area {
  color: #666;
  width: 100%;
}

.upload-icon, .url-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.upload-progress {
  width: 100%;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background-color: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-fill {
  height: 100%;
  background-color: #007bff;
  transition: width 0.3s;
}

.upload-success {
  color: #28a745;
  width: 100%;
}

.success-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.success-tip {
  font-size: 12px;
  color: #666;
  margin: 5px 0 15px 0;
}

.url-input, .name-input {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  width: 100%;
  margin-bottom: 10px;
  font-size: 14px;
}

.url-input:focus, .name-input:focus {
  outline: none;
  border-color: #007bff;
}

.download-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  color: white;
  background-color: #007bff;
  transition: background-color 0.3s;
  margin-bottom: 10px;
}

.download-btn:hover:not(:disabled) {
  background-color: #0056b3;
}

.download-btn:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.downloading {
  background: linear-gradient(90deg, #007bff 0%, #0056b3 50%, #007bff 100%);
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: -200px 0; }
  100% { background-position: 200px 0; }
}

.error-message {
  color: #dc3545;
  font-size: 14px;
  margin-top: 10px;
  text-align: center;
}
</style> 