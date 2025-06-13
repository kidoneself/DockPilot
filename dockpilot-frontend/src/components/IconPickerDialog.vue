<template>
  <n-modal
    :show="visible"
    preset="dialog"
    title="选择应用图标"
    style="width: 700px;"
    class="icon-picker-dialog"
    @update:show="handleVisibleChange"
    @after-leave="handleClose"
  >
    <div class="picker-content">
      <!-- 搜索区域 -->
      <div class="search-section">
        <div class="search-row">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索图标..."
            clearable
            class="search-input"
            @input="handleSearch"
          >
            <template #prefix>
              <n-icon>
                <SearchOutline />
              </n-icon>
            </template>
          </n-input>
          
          <n-button text size="small" @click="refreshIcons" style="margin-left: 12px;">
            <n-icon>
              <RefreshOutline />
            </n-icon>
            刷新
          </n-button>
        </div>
        
        <!-- 上传按钮区域 -->
        <div class="upload-section">
          <input
            ref="singleFileInput"
            type="file"
            accept=".png,.jpg,.jpeg,.svg,.ico"
            style="display: none"
            @change="handleSingleFileUpload"
          />
          <input
            ref="multipleFilesInput"
            type="file"
            accept=".png,.jpg,.jpeg,.svg,.ico"
            multiple
            style="display: none"
            @change="handleMultipleFilesUpload"
          />
          <input
            ref="zipFileInput"
            type="file"
            accept=".zip"
            style="display: none"
            @change="handleZipFileUpload"
          />
          
          <n-button 
            size="small" 
            @click="triggerSingleFileUpload"
            :loading="uploadingStates.single"
            :disabled="isAnyUploading"
          >
            <n-icon>
              <CloudUploadOutline />
            </n-icon>
            上传单个
          </n-button>
          
          <n-button 
            size="small" 
            @click="triggerMultipleFilesUpload"
            :loading="uploadingStates.multiple"
            :disabled="isAnyUploading"
          >
            <n-icon>
              <CloudUploadOutline />
            </n-icon>
            上传多个
          </n-button>
          
          <n-button 
            size="small" 
            @click="triggerZipFileUpload"
            :loading="uploadingStates.zip"
            :disabled="isAnyUploading"
          >
            <n-icon>
              <ArchiveOutline />
            </n-icon>
            上传ZIP
          </n-button>
        </div>
        
        <div class="info-row">
          <span class="icon-count">共找到 {{ filteredIcons.length }} 个图标</span>
          <span v-if="displayedCount < filteredIcons.length" class="load-info">
            已显示 {{ displayedCount }} 个，向下滚动加载更多
          </span>
        </div>
      </div>
      
      <!-- 图标网格 -->
      <div 
        class="icons-grid-container" 
        v-if="!loading"
        @scroll="handleScroll"
        ref="scrollContainer"
      >
        <div 
          v-if="filteredIcons.length === 0"
          class="empty-state"
        >
          <n-icon :size="48">
            <ImageOutline />
          </n-icon>
          <p>{{ searchKeyword ? '未找到匹配的图标' : '暂无图标' }}</p>
          <n-button text @click="refreshIcons">点击刷新</n-button>
        </div>
        
        <div v-else class="icons-grid">
          <div 
            v-for="icon in displayedIcons" 
            :key="icon.name"
            class="icon-item"
            :class="{ 
              'selected': selectedIcon === icon.name
            }"
            @click="selectIcon(icon.name)"
            :title="`${icon.displayName} (${icon.name})`"
          >
            <div class="icon-image">
              <img 
                :src="icon.url" 
                :alt="icon.displayName"
                @load="onImageLoad(icon.name)"
                @error="onImageError(icon.name)"
                style="opacity: 1;"
              />
            </div>
            <div class="icon-info">
              <div class="icon-name">{{ icon.displayName }}</div>
            </div>
          </div>
        </div>
        
        <!-- 加载更多提示 -->
        <div v-if="isLoadingMore" class="loading-more">
          <n-spin size="small" />
          <span>加载更多图标...</span>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-else class="loading-state">
        <n-spin size="large" />
        <p>正在加载图标...</p>
      </div>
      
      <!-- 底部操作 -->
      <div class="dialog-footer">
        <n-button @click="handleClose">取消</n-button>
        <n-button 
          type="primary" 
          @click="confirmSelection"
          :disabled="!selectedIcon"
        >
          确定
        </n-button>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { NModal, NInput, NButton, NIcon, NSpin, useMessage } from 'naive-ui'
import { SearchOutline, RefreshOutline, ImageOutline, CloudUploadOutline, ArchiveOutline } from '@vicons/ionicons5'
import { IconApi } from '@/api/icon'
import type { IconInfo } from '@/types/icon'

interface Props {
  visible: boolean
  current?: string  // 当前选中的图标名称
}

interface Emits {
  (e: 'update:visible', visible: boolean): void
  (e: 'select', iconName: string): void
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  current: ''
})

const emit = defineEmits<Emits>()

// 响应式数据
const loading = ref(false)
const allIcons = ref<IconInfo[]>([])
const searchKeyword = ref('')
const selectedIcon = ref('')
const scrollContainer = ref<HTMLElement>()
const isLoadingMore = ref(false)
const message = useMessage()

// 上传相关的引用
const singleFileInput = ref<HTMLInputElement>()
const multipleFilesInput = ref<HTMLInputElement>()
const zipFileInput = ref<HTMLInputElement>()

// 上传状态
const uploadingStates = ref({
  single: false,
  multiple: false,
  zip: false
})

// 懒加载相关
const batchSize = 50  // 每次加载50个图标
const displayedCount = ref(0)

// 计算属性
const filteredIcons = computed(() => {
  let icons = allIcons.value

  // 搜索筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    icons = icons.filter(icon => 
      icon.displayName.toLowerCase().includes(keyword) ||
      icon.name.toLowerCase().includes(keyword)
    )
  }

  return icons
})

const displayedIcons = computed(() => {
  return filteredIcons.value.slice(0, displayedCount.value)
})

const isAnyUploading = computed(() => {
  return uploadingStates.value.single || uploadingStates.value.multiple || uploadingStates.value.zip
})

// 监听弹窗显示
watch(() => props.visible, (visible) => {
  if (visible) {
    loadIcons()
    selectedIcon.value = props.current || ''
  } else {
    // 关闭时重置状态
    searchKeyword.value = ''
    displayedCount.value = 0
  }
})

// 监听当前选中的图标
watch(() => props.current, (current) => {
  selectedIcon.value = current || ''
})

// 监听搜索和过滤结果变化，重置显示数量
watch(filteredIcons, () => {
  displayedCount.value = Math.min(filteredIcons.value.length, batchSize)
  nextTick(() => {
    // 滚动到顶部
    if (scrollContainer.value) {
      scrollContainer.value.scrollTop = 0
    }
  })
})

// 方法
const loadIcons = async () => {
  if (allIcons.value.length > 0) return // 已加载过了

  loading.value = true
  try {
    const iconsResponse = await IconApi.getIconList()
    
    allIcons.value = iconsResponse || []
    displayedCount.value = Math.min(allIcons.value.length, batchSize)
    
    console.log(`✅ 加载了 ${allIcons.value.length} 个图标`)
    console.log('📊 图标数据示例:', allIcons.value.slice(0, 3))
  } catch (error) {
    console.error('加载图标失败:', error)
    allIcons.value = []
    displayedCount.value = 0
  } finally {
    loading.value = false
  }
}

const refreshIcons = async () => {
  allIcons.value = []
  displayedCount.value = 0
  await loadIcons()
}

const handleSearch = () => {
  // 搜索时会自动触发 filteredIcons 的 watch，重置显示数量
}

// 滚动懒加载
const handleScroll = (event: Event) => {
  const container = event.target as HTMLElement
  const { scrollTop, scrollHeight, clientHeight } = container
  
  // 当滚动到接近底部时（剩余100px）加载更多
  if (scrollHeight - scrollTop - clientHeight < 100) {
    loadMoreIcons()
  }
}

const loadMoreIcons = () => {
  // 防止重复加载
  if (isLoadingMore.value || displayedCount.value >= filteredIcons.value.length) {
    return
  }

  isLoadingMore.value = true
  
  // 模拟网络延迟，提供更好的用户体验
  setTimeout(() => {
    const newCount = Math.min(
      displayedCount.value + batchSize,
      filteredIcons.value.length
    )
    displayedCount.value = newCount
    isLoadingMore.value = false
    
    console.log(`📈 懒加载：显示 ${displayedCount.value}/${filteredIcons.value.length} 个图标`)
  }, 300)
}

const selectIcon = (iconName: string) => {
  selectedIcon.value = iconName
}

const confirmSelection = () => {
  if (selectedIcon.value) {
    emit('select', selectedIcon.value)
    handleClose()
  }
}

const handleClose = () => {
  emit('update:visible', false)
}

const handleVisibleChange = (show: boolean) => {
  emit('update:visible', show)
}

const onImageLoad = (iconName: string) => {
  // 图标加载成功
  console.log(`✅ 图标加载成功: ${iconName}`)
}

const onImageError = (iconName: string) => {
  // 图标加载失败
  console.error(`❌ 图标加载失败: ${iconName}`)
}

// 上传相关方法
const triggerSingleFileUpload = () => {
  singleFileInput.value?.click()
}

const triggerMultipleFilesUpload = () => {
  multipleFilesInput.value?.click()
}

const triggerZipFileUpload = () => {
  zipFileInput.value?.click()
}

const handleSingleFileUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return
  
  uploadingStates.value.single = true
  
  try {
    const result = await IconApi.uploadIcon(file)
    message.success(`成功上传图标: ${file.name}`)
    
    // 刷新图标列表
    await refreshIcons()
    
    // 自动选中刚上传的图标
    if (result.name) {
      selectedIcon.value = result.name
    }
  } catch (error: any) {
    console.error('上传单个图标失败:', error)
    message.error(`上传失败: ${error.message || '未知错误'}`)
  } finally {
    uploadingStates.value.single = false
    // 清空文件输入
    target.value = ''
  }
}

const handleMultipleFilesUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  
  if (files.length === 0) return
  
  uploadingStates.value.multiple = true
  
  try {
    const results = await IconApi.uploadIcons(files)
    message.success(`成功上传 ${results.length} 个图标`)
    
    // 刷新图标列表
    await refreshIcons()
    
    // 自动选中第一个上传的图标
    if (results.length > 0 && results[0].name) {
      selectedIcon.value = results[0].name
    }
  } catch (error: any) {
    console.error('批量上传图标失败:', error)
    message.error(`批量上传失败: ${error.message || '未知错误'}`)
  } finally {
    uploadingStates.value.multiple = false
    // 清空文件输入
    target.value = ''
  }
}

const handleZipFileUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return
  
  uploadingStates.value.zip = true
  
  try {
    const results = await IconApi.uploadIconsFromZip(file)
    message.success(`成功从压缩包中导入 ${results.length} 个图标`)
    
    // 刷新图标列表
    await refreshIcons()
    
    // 自动选中第一个导入的图标
    if (results.length > 0 && results[0].name) {
      selectedIcon.value = results[0].name
    }
  } catch (error: any) {
    console.error('ZIP上传失败:', error)
    message.error(`ZIP上传失败: ${error.message || '未知错误'}`)
  } finally {
    uploadingStates.value.zip = false
    // 清空文件输入
    target.value = ''
  }
}
</script>

<style scoped>
.picker-content {
  display: flex;
  flex-direction: column;
  height: 600px;
}

.search-section {
  padding: 16px;
  border-bottom: 1px solid var(--n-border-color);
  background: var(--n-color);
}

.search-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.search-input {
  flex: 1;
}

.upload-section {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--n-text-color-2);
}

.icon-count {
  color: var(--n-text-color-placeholder);
}

.load-info {
  color: var(--n-color-primary);
  font-size: 11px;
}

.icons-grid-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  scroll-behavior: smooth;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 16px;
  color: var(--n-text-color-placeholder);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: var(--n-text-color-placeholder);
  gap: 12px;
}

.icons-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--n-color);
}

.icon-item:hover {
  background: var(--n-color-hover);
  border-color: var(--n-color-primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.icon-item.selected {
  border-color: var(--n-color-primary);
  background: var(--n-color-primary-hover);
}

.icon-image {
  position: relative;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.icon-image img {
  max-width: 48px;
  max-height: 48px;
  object-fit: contain;
  border-radius: 6px;
}

.icon-info {
  text-align: center;
  width: 100%;
}

.icon-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--n-text-color);
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  color: var(--n-text-color-placeholder);
  font-size: 12px;
}

.dialog-footer {
  padding: 16px;
  border-top: 1px solid var(--n-border-color);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  background: var(--n-color);
}

/* 优化滚动条样式 */
.icons-grid-container::-webkit-scrollbar {
  width: 6px;
}

.icons-grid-container::-webkit-scrollbar-track {
  background: var(--n-scrollbar-track-color);
  border-radius: 3px;
}

.icons-grid-container::-webkit-scrollbar-thumb {
  background: var(--n-scrollbar-color);
  border-radius: 3px;
}

.icons-grid-container::-webkit-scrollbar-thumb:hover {
  background: var(--n-scrollbar-color-hover);
}
</style> 