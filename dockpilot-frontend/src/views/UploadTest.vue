<template>
  <div class="upload-test-page">
    <div class="page-header">
      <h1>📸 图片上传测试</h1>
      <p>测试图片上传功能，支持拖拽上传、进度显示、预览和删除</p>
    </div>

    <div class="test-sections">
      <!-- 基础上传测试 -->
      <div class="test-section">
        <h2>🚀 基础上传测试</h2>
        <div class="upload-container">
          <ImageUpload 
            v-model="imageUrl1" 
            @upload-success="handleUploadSuccess"
            @upload-error="handleUploadError"
            @delete-success="handleDeleteSuccess"
          />
        </div>
        <div v-if="imageUrl1" class="result-info">
          <p><strong>上传结果：</strong></p>
          <p>图片URL: <code>{{ imageUrl1 }}</code></p>
        </div>
      </div>

      <!-- 自定义大小限制测试 -->
      <div class="test-section">
        <h2>📏 自定义大小限制测试 (最大5MB)</h2>
        <div class="upload-container">
          <ImageUpload 
            v-model="imageUrl2" 
            :max-size="5"
            @upload-success="handleUploadSuccess"
            @upload-error="handleUploadError"
            @delete-success="handleDeleteSuccess"
          />
        </div>
        <div v-if="imageUrl2" class="result-info">
          <p><strong>上传结果：</strong></p>
          <p>图片URL: <code>{{ imageUrl2 }}</code></p>
        </div>
      </div>

      <!-- 多个上传实例测试 -->
      <div class="test-section">
        <h2>🔄 多个上传实例测试</h2>
        <div class="multi-upload">
          <div class="upload-item">
            <h3>上传实例 1</h3>
            <ImageUpload 
              v-model="imageUrl3" 
              @upload-success="handleUploadSuccess"
              @upload-error="handleUploadError"
            />
          </div>
          <div class="upload-item">
            <h3>上传实例 2</h3>
            <ImageUpload 
              v-model="imageUrl4" 
              @upload-success="handleUploadSuccess"
              @upload-error="handleUploadError"
            />
          </div>
        </div>
        <div v-if="imageUrl3 || imageUrl4" class="result-info">
          <p><strong>多个上传结果：</strong></p>
          <p v-if="imageUrl3">实例1: <code>{{ imageUrl3 }}</code></p>
          <p v-if="imageUrl4">实例2: <code>{{ imageUrl4 }}</code></p>
        </div>
      </div>

      <!-- 手动API测试 -->
      <div class="test-section">
        <h2>🔧 手动API测试</h2>
        <div class="api-test">
          <input 
            ref="fileInputRef" 
            type="file" 
            accept="image/*" 
            style="margin-bottom: 10px;"
            @change="handleManualUpload"
          />
          <button class="test-btn" @click="triggerFileSelect">选择文件</button>
          <button 
            v-if="manualUploadResult" 
            class="test-btn delete-btn" 
            @click="deleteManualUpload"
          >
            删除文件
          </button>
        </div>
        <div v-if="manualUploadProgress > 0 && manualUploadProgress < 100" class="progress-info">
          <p>手动上传进度: {{ manualUploadProgress }}%</p>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: manualUploadProgress + '%' }"></div>
          </div>
        </div>
        <div v-if="manualUploadResult" class="result-info">
          <p><strong>手动上传结果：</strong></p>
          <pre>{{ JSON.stringify(manualUploadResult, null, 2) }}</pre>
          <img
v-if="manualImageUrl"
:src="manualImageUrl"
alt="手动上传的图片"
class="manual-preview" />
        </div>
      </div>

      <!-- 操作日志 -->
      <div class="test-section">
        <h2>📋 操作日志</h2>
        <div class="log-container">
          <div
v-for="(log, index) in logs"
:key="index"
class="log-item"
:class="log.type">
            <span class="log-time">{{ log.time }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
          <div v-if="logs.length === 0" class="no-logs">暂无操作日志</div>
          <button v-if="logs.length > 0" class="clear-logs-btn" @click="clearLogs">清空日志</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { uploadImage, deleteFile, getImageUrl } from '@/api/http/file-upload'
import type { FileUploadResponse } from '@/api/http/file-upload'

// 响应式数据
const imageUrl1 = ref('')
const imageUrl2 = ref('')
const imageUrl3 = ref('')
const imageUrl4 = ref('')

// 手动API测试相关
const fileInputRef = ref<HTMLInputElement>()
const manualUploadProgress = ref(0)
const manualUploadResult = ref<FileUploadResponse | null>(null)
const manualImageUrl = ref('')

// 操作日志
const logs = ref<Array<{time: string, message: string, type: 'success' | 'error' | 'info'}>>([])

// 添加日志
const addLog = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
  logs.value.unshift({
    time: new Date().toLocaleTimeString(),
    message,
    type
  })
}

// 上传成功回调
const handleUploadSuccess = (response: FileUploadResponse) => {
  addLog(`上传成功: ${response.originalName} -> ${response.filename}`, 'success')
  console.log('上传成功:', response)
}

// 上传失败回调
const handleUploadError = (error: string) => {
  addLog(`上传失败: ${error}`, 'error')
  console.error('上传失败:', error)
}

// 删除成功回调
const handleDeleteSuccess = () => {
  addLog('文件删除成功', 'success')
  console.log('删除成功')
}

// 触发文件选择
const triggerFileSelect = () => {
  fileInputRef.value?.click()
}

// 手动上传测试
const handleManualUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return

  try {
    manualUploadProgress.value = 0
    manualUploadResult.value = null
    manualImageUrl.value = ''
    
    addLog(`开始手动上传: ${file.name}`, 'info')
    
    const response = await uploadImage(file, (progress) => {
      manualUploadProgress.value = progress
    })
    
    manualUploadResult.value = response
    manualImageUrl.value = getImageUrl(response.filename)
    
    addLog(`手动上传成功: ${response.originalName}`, 'success')
    
  } catch (error) {
    const errorMsg = error instanceof Error ? error.message : '上传失败'
    addLog(`手动上传失败: ${errorMsg}`, 'error')
  }
}

// 删除手动上传的文件
const deleteManualUpload = async () => {
  if (!manualUploadResult.value) return
  
  try {
    await deleteFile(manualUploadResult.value.filename)
    
    manualUploadResult.value = null
    manualImageUrl.value = ''
    manualUploadProgress.value = 0
    
    addLog('手动上传的文件删除成功', 'success')
    
  } catch (error) {
    const errorMsg = error instanceof Error ? error.message : '删除失败'
    addLog(`删除失败: ${errorMsg}`, 'error')
  }
}

// 清空日志
const clearLogs = () => {
  logs.value = []
}
</script>

<style scoped>
.upload-test-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-header h1 {
  color: #333;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
  font-size: 16px;
}

.test-sections {
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.test-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.test-section h2 {
  color: #333;
  margin-bottom: 20px;
  font-size: 20px;
}

.test-section h3 {
  color: #555;
  margin-bottom: 12px;
  font-size: 16px;
}

.upload-container {
  margin-bottom: 20px;
}

.multi-upload {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.upload-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
}

.api-test {
  margin-bottom: 20px;
}

.test-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 8px;
}

.test-btn:hover {
  background: #0056b3;
}

.delete-btn {
  background: #dc3545;
}

.delete-btn:hover {
  background: #c82333;
}

.progress-info {
  margin-bottom: 16px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #007bff;
  transition: width 0.3s;
}

.result-info {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 16px;
  margin-top: 16px;
}

.result-info code {
  background: #e9ecef;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  word-break: break-all;
}

.result-info pre {
  background: #f1f3f4;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 14px;
}

.manual-preview {
  max-width: 200px;
  max-height: 200px;
  margin-top: 12px;
  border-radius: 6px;
  object-fit: contain;
}

.log-container {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 12px;
}

.log-item {
  display: flex;
  gap: 12px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  color: #999;
  font-size: 12px;
  min-width: 80px;
}

.log-message {
  color: #333;
  font-size: 14px;
}

.log-item.success .log-message {
  color: #28a745;
}

.log-item.error .log-message {
  color: #dc3545;
}

.log-item.info .log-message {
  color: #007bff;
}

.no-logs {
  text-align: center;
  color: #999;
  padding: 20px;
}

.clear-logs-btn {
  background: #6c757d;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 12px;
  font-size: 12px;
}

.clear-logs-btn:hover {
  background: #5a6268;
}

@media (max-width: 768px) {
  .multi-upload {
    grid-template-columns: 1fr;
  }
  
  .upload-test-page {
    padding: 16px;
  }
}
</style> 