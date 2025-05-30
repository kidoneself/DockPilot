<template>
  <div class="path-selector">
    <NPopover
      v-model:show="showPopover"
      trigger="click"
      placement="bottom-start"
      :show-arrow="false"
      style="padding: 0"
      :disabled="disabled"
    >
      <template #trigger>
        <NInput
          :value="modelValue"
          :placeholder="placeholder"
          :disabled="disabled"
          clearable
          @update:value="handleInputChange"
          @clear="handleClear"
        >
          <template #prefix>
            <NIcon><FolderOutline /></NIcon>
          </template>
          <template #suffix>
            <NButton text size="small" :disabled="disabled">
              <template #icon>
                <NIcon><ChevronDownOutline /></NIcon>
              </template>
            </NButton>
          </template>
        </NInput>
      </template>
      
      <div class="folder-browser">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <NInput
            v-model:value="searchKeyword"
            placeholder="搜索文件夹..."
            clearable
            @update:value="handleSearch"
          >
            <template #prefix>
              <NIcon><SearchOutline /></NIcon>
            </template>
          </NInput>
        </div>

        <!-- 当前路径导航 -->
        <div class="breadcrumb">
          <div class="current-path">
            <NIcon><LocationOutline /></NIcon>
            <span>{{ currentPath || '/' }}</span>
          </div>
          <NButton
            v-if="currentPath !== '/'"
            size="small"
            @click="goToParent"
          >
            <template #icon>
              <NIcon><ArrowUpOutline /></NIcon>
            </template>
            上级目录
          </NButton>
        </div>

        <!-- 文件夹列表 -->
        <div class="folder-list">
          <NScrollbar style="max-height: 300px;">
            <div v-if="loading" class="loading-state">
              <NSpin size="small" />
              <span>加载中...</span>
            </div>
            <div v-else-if="filteredFolders.length === 0" class="empty-state">
              <NIcon size="24"><FolderOutline /></NIcon>
              <span>{{ searchKeyword ? '未找到匹配的文件夹' : '此目录为空' }}</span>
            </div>
            <div
              v-for="folder in filteredFolders"
              :key="folder.path"
              class="folder-item"
              :class="{ 'selected': folder.path === selectedKey }"
              :title="`单击选择，双击进入 ${folder.name}`"
              @click="selectFolder(folder)"
              @dblclick="enterFolder(folder)"
            >
              <div class="folder-icon">
                <NIcon><FolderOutline /></NIcon>
              </div>
              <div class="folder-name">{{ folder.name }}</div>
            </div>
          </NScrollbar>
        </div>

        <!-- 底部操作栏 -->
        <div class="action-bar">
          <div class="selected-info">
            <div v-if="selectedKey" class="selection-text">已选择: {{ selectedKey }}</div>
            <div class="operation-tip">💡 单击选择，双击进入</div>
          </div>
          <NSpace>
            <NButton @click="handleCancel">取消</NButton>
            <NButton
              type="primary"
              :disabled="!selectedKey"
              @click="handleConfirm"
            >
              确定
            </NButton>
          </NSpace>
        </div>
      </div>
    </NPopover>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NInput, NButton, NIcon, NPopover, NScrollbar, NSpin, NSpace, useMessage } from 'naive-ui'
import {
  FolderOutline,
  ChevronDownOutline,
  SearchOutline,
  LocationOutline,
  ArrowUpOutline
} from '@vicons/ionicons5'
import { getFileTree, type FileNode } from '@/api/http/file-tree'

interface Props {
  modelValue?: string
  placeholder?: string
  disabled?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '请选择文件夹路径',
  disabled: false
})

const emit = defineEmits<Emits>()
const message = useMessage()

// 状态管理
const currentPath = ref('/')
const selectedKey = ref<string | null>(null)
const searchKeyword = ref('')
const loading = ref(false)
const allFolders = ref<FileNode[]>([])
const showPopover = ref(false)

// 监听外部value变化
watch(() => props.modelValue, (newVal) => {
  selectedKey.value = newVal || null
}, { immediate: true })

// 监听弹窗打开状态
watch(showPopover, (newVal) => {
  if (newVal) {
    // 弹窗打开时，重置选择状态为当前输入值
    selectedKey.value = props.modelValue || null
    
    // 如果当前值是有效路径，尝试导航到该路径或其父目录
    if (props.modelValue && props.modelValue.startsWith('/')) {
      const targetPath = props.modelValue
      // 如果是绝对路径，尝试导航到该路径的父目录
      const parentPath = targetPath.split('/').slice(0, -1).join('/') || '/'
      if (parentPath !== currentPath.value) {
        loadFolders(parentPath)
      }
    }
  }
})

// 搜索过滤
const filteredFolders = computed(() => {
  if (!searchKeyword.value) {
    return allFolders.value
  }
  return allFolders.value.filter(folder =>
    folder.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 加载文件夹列表
const loadFolders = async (path: string = '/') => {
  loading.value = true
  try {
    const response = await getFileTree(path, 100)
    // 只保留目录，过滤掉文件
    const directories = response.filter(node => node.directory)
    allFolders.value = directories
    currentPath.value = path
  } catch (error) {
    console.error('加载文件夹列表失败:', error)
    message.error('加载文件夹列表失败')
    allFolders.value = []
  } finally {
    loading.value = false
  }
}

// 选择文件夹
const selectFolder = (folder: FileNode) => {
  selectedKey.value = folder.path
}

// 进入文件夹
const enterFolder = (folder: FileNode) => {
  loadFolders(folder.path)
  searchKeyword.value = '' // 清空搜索
}

// 返回上级目录
const goToParent = () => {
  const parentPath = currentPath.value.split('/').slice(0, -1).join('/') || '/'
  loadFolders(parentPath)
  searchKeyword.value = '' // 清空搜索
}

// 搜索处理
const handleSearch = (value: string) => {
  searchKeyword.value = value
}

// 手动输入
const handleInputChange = (value: string) => {
  selectedKey.value = value
  emit('update:modelValue', value)
}

// 清除选择
const handleClear = () => {
  selectedKey.value = null
  emit('update:modelValue', '')
}

// 取消选择
const handleCancel = () => {
  showPopover.value = false
}

// 确认选择
const handleConfirm = () => {
  if (selectedKey.value) {
    emit('update:modelValue', selectedKey.value)
  }
  showPopover.value = false
}

// 初始化加载
loadFolders()
</script>

<style scoped>
.path-selector {
  width: 100%;
}

.folder-browser {
  width: 500px;
  background: var(--n-popover-color);
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.search-bar {
  padding: 12px;
  border-bottom: 1px solid var(--n-divider-color);
}

.breadcrumb {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--n-color-hover);
  border-bottom: 1px solid var(--n-divider-color);
}

.current-path {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--n-text-color-2);
}

.folder-list {
  min-height: 200px;
  max-height: 300px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: var(--n-text-color-3);
}

.folder-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid var(--n-divider-color);
}

.folder-item:hover {
  background: var(--n-color-hover);
}

.folder-item.selected {
  background: var(--n-color-primary-light);
  color: var(--n-color-primary);
}

.folder-icon {
  color: #8e5cff;
}

.folder-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--n-text-color-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-top: 1px solid var(--n-divider-color);
  background: var(--n-color-hover);
}

.selected-info {
  font-size: 12px;
  color: var(--n-text-color-2);
  max-width: 250px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.selection-text {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.operation-tip {
  color: var(--n-text-color-3);
  font-style: italic;
  font-size: 11px;
}
</style> 