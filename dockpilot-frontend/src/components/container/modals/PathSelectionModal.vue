<template>
  <NModal
    v-model:show="showModal"
    preset="card"
    title="选择要打包的路径"
    class="path-selection-modal"
    style="width: 90%; max-width: 800px;"
  >
    <div v-if="isLoading" style="text-align: center; padding: 40px;">
      <NSpin size="medium" />
      <div style="margin-top: 16px;">正在加载容器路径信息...</div>
    </div>

    <div v-else-if="containerPaths.length === 0" style="text-align: center; padding: 40px;">
      <NEmpty description="未找到可打包的路径" />
    </div>

    <div v-else>
      <!-- 路径统计和全选控制 -->
      <div class="path-stats">
        <div class="stats-info">
          <NText strong>
            找到 {{ totalPaths }} 个路径 ({{ totalContainers }} 个容器)
          </NText>
          <NText depth="3" style="margin-left: 12px;">
            已选择 {{ selectedPaths.size }} 个路径
          </NText>
        </div>
        <div class="stats-actions">
          <NButton size="small" @click="selectAllPaths" :disabled="selectedPaths.size === totalPaths">
            全选
          </NButton>
          <NButton size="small" @click="clearAllSelection" :disabled="selectedPaths.size === 0">
            清空
          </NButton>
          <NButton size="small" @click="selectOnlyUserPaths">
            只选用户路径
          </NButton>
        </div>
      </div>

      <!-- 路径分组展示 -->
      <div class="path-groups">
        <div 
          v-for="container in containerPaths" 
          :key="container.containerId"
          class="container-group"
        >
          <!-- 容器标题 -->
          <div class="container-header">
            <div class="container-info">
              <n-icon size="18" style="color: #2080f0;">
                <ServerOutline />
              </n-icon>
              <span class="container-name">{{ container.serviceName }}</span>
              <NTag size="small" type="info">{{ container.image }}</NTag>
            </div>
            <div class="container-actions">
              <NButton 
                size="tiny" 
                @click="toggleContainerSelection(container)"
                :type="isContainerFullySelected(container) ? 'warning' : 'default'"
              >
                {{ isContainerFullySelected(container) ? '取消全选' : '全选此容器' }}
              </NButton>
              <NText depth="3" style="font-size: 12px;">
                {{ getContainerSelectedCount(container) }}/{{ container.pathMappings.length }}
              </NText>
            </div>
          </div>

          <!-- 路径列表 -->
          <div class="path-list">
            <div 
              v-for="path in container.pathMappings" 
              :key="path.id"
              class="path-item"
              :class="{ 'selected': selectedPaths.has(path.id) }"
            >
              <div class="path-checkbox">
                <NCheckbox
                  :checked="selectedPaths.has(path.id)"
                  @update:checked="(checked) => togglePathSelection(path.id, checked)"
                />
              </div>
              
              <div class="path-info">
                <div class="path-main">
                  <span class="path-mount">{{ path.containerPath }}</span>
                  <n-icon size="16" style="margin: 0 8px; color: #ccc;">
                    <ArrowForwardOutline />
                  </n-icon>
                  <span class="path-host">{{ path.hostPath }}</span>
                </div>
                
                <div class="path-details">
                  <NTag 
                    size="small" 
                    :type="path.isSystemPath ? 'default' : 'success'"
                    style="margin-right: 8px;"
                  >
                    {{ getPathCategoryLabel(path.isSystemPath) }}
                  </NTag>
                  
                  <span class="path-mode">{{ getPathMode(path.readOnly) }}</span>
                  
                  <span class="path-mount-type">
                    • {{ path.mountType }}
                  </span>
                </div>
                
                <!-- 路径说明 -->
                <div v-if="path.description" class="path-description">
                  <n-icon size="14" style="margin-right: 4px; color: #999;">
                    <InformationCircleOutline />
                  </n-icon>
                  {{ path.description }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 选择摘要 -->
      <div v-if="selectedPaths.size > 0" class="selection-summary">
        <NAlert type="info" style="margin-top: 16px;">
          <template #icon>
            <n-icon><CheckmarkCircleOutline /></n-icon>
          </template>
          <div>
            <div style="margin-bottom: 8px;">
              <strong>已选择 {{ selectedPaths.size }} 个路径进行打包：</strong>
            </div>
            <div class="summary-stats">
              <span>用户数据路径：{{ getUserDataPathCount() }} 个</span>
              <span>系统路径：{{ getSystemPathCount() }} 个</span>
              <span v-if="getTotalSize() > 0">预计大小：{{ formatBytes(getTotalSize()) }}</span>
            </div>
          </div>
        </NAlert>
      </div>
    </div>

    <template #action>
      <NSpace>
        <NButton @click="handleCancel">取消</NButton>
        <NButton 
          type="primary" 
          :disabled="selectedPaths.size === 0"
          @click="handleConfirm"
        >
          确定 ({{ selectedPaths.size }})
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import {
  NModal,
  NButton,
  NSpace,
  NText,
  NTag,
  NCheckbox,
  NAlert,
  NSpin,
  NEmpty,
  useMessage
} from 'naive-ui'
import {
  ServerOutline,
  ArrowForwardOutline,
  CheckmarkCircleOutline,
  InformationCircleOutline
} from '@vicons/ionicons5'
import { getContainerPaths, type ContainerPathInfo } from '@/api/containerYaml'

// Props & Emits
interface Props {
  show: boolean
  selectedContainers: Set<string>
}

interface Emits {
  (e: 'update:show', value: boolean): void
  (e: 'confirm', selectedPaths: string[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const message = useMessage()

// 状态管理
const showModal = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value)
})

const isLoading = ref(false)
const containerPaths = ref<ContainerPathInfo[]>([])
const selectedPaths = ref<Set<string>>(new Set())

// 计算属性
const totalContainers = computed(() => containerPaths.value.length)
const totalPaths = computed(() => 
  containerPaths.value.reduce((sum, container) => sum + container.pathMappings.length, 0)
)

// 监听模态框打开
watch(() => props.show, async (newVal) => {
  if (newVal && props.selectedContainers.size > 0) {
    await loadContainerPaths()
  }
})

// 加载容器路径信息
async function loadContainerPaths() {
  try {
    isLoading.value = true
    selectedPaths.value.clear()
    
    const containerIds = Array.from(props.selectedContainers)
    const response = await getContainerPaths({ containerIds })
    
    if (response.success) {
      containerPaths.value = response.data || []
      
      // 默认选择推荐路径
      autoSelectRecommendedPaths()
      
      message.success(`加载了 ${totalPaths.value} 个路径`)
    } else {
      message.error(response.message || '加载路径信息失败')
      containerPaths.value = []
    }
  } catch (error: any) {
    message.error('加载路径信息失败: ' + (error.message || error))
    containerPaths.value = []
  } finally {
    isLoading.value = false
  }
}

// 自动选择推荐路径
function autoSelectRecommendedPaths() {
  const recommendedPathIds = containerPaths.value
    .flatMap(container => container.pathMappings)
    .filter(path => path.recommended && !path.isSystemPath)
    .map(path => path.id)
  
  selectedPaths.value = new Set(recommendedPathIds)
}

// 路径选择控制
function togglePathSelection(pathId: string, checked: boolean) {
  if (checked) {
    selectedPaths.value.add(pathId)
  } else {
    selectedPaths.value.delete(pathId)
  }
}

function selectAllPaths() {
  const allPathIds = containerPaths.value
    .flatMap(container => container.pathMappings)
    .map(path => path.id)
  
  selectedPaths.value = new Set(allPathIds)
}

function clearAllSelection() {
  selectedPaths.value.clear()
}

function selectOnlyUserPaths() {
  autoSelectRecommendedPaths()
}

// 容器级别选择控制
function isContainerFullySelected(container: ContainerPathInfo): boolean {
  return container.pathMappings.every(path => selectedPaths.value.has(path.id))
}

function getContainerSelectedCount(container: ContainerPathInfo): number {
  return container.pathMappings.filter(path => selectedPaths.value.has(path.id)).length
}

function toggleContainerSelection(container: ContainerPathInfo) {
  const isFullySelected = isContainerFullySelected(container)
  
  container.pathMappings.forEach(path => {
    if (isFullySelected) {
      selectedPaths.value.delete(path.id)
    } else {
      selectedPaths.value.add(path.id)
    }
  })
}

// 工具函数
function getPathCategoryLabel(isSystemPath: boolean): string {
  return isSystemPath ? '系统路径' : '用户数据'
}

function getPathMode(readOnly: boolean): string {
  return readOnly ? 'ro' : 'rw'
}

function formatBytes(bytes: number): string {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// 统计函数
function getUserDataPathCount(): number {
  return getSelectedPaths()
    .filter(path => !path.isSystemPath)
    .length
}

function getSystemPathCount(): number {
  return getSelectedPaths()
    .filter(path => path.isSystemPath)
    .length
}

function getTotalSize(): number {
  // 由于当前类型定义中没有size属性，返回0
  return 0
}

function getSelectedPaths() {
  return containerPaths.value
    .flatMap(container => container.pathMappings)
    .filter(path => selectedPaths.value.has(path.id))
}

// 事件处理
function handleCancel() {
  showModal.value = false
  selectedPaths.value.clear()
}

function handleConfirm() {
  if (selectedPaths.value.size === 0) {
    message.warning('请至少选择一个路径')
    return
  }
  
  const selectedPathIds = Array.from(selectedPaths.value)
  emit('confirm', selectedPathIds)
  showModal.value = false
}
</script>

<style scoped>
/* 路径选择模态框样式 */
:deep(.path-selection-modal .n-card) {
  max-height: 80vh;
  overflow-y: auto;
}

.path-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 16px;
}

.stats-info {
  display: flex;
  align-items: center;
}

.stats-actions {
  display: flex;
  gap: 8px;
}

.path-groups {
  max-height: 500px;
  overflow-y: auto;
}

.container-group {
  margin-bottom: 20px;
  border: 1px solid #e0e0e6;
  border-radius: 8px;
  overflow: hidden;
}

.container-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e6;
}

.container-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.container-name {
  font-weight: 600;
  color: #333;
}

.container-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.path-list {
  padding: 12px;
}

.path-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid transparent;
  margin-bottom: 8px;
  transition: all 0.2s ease;
}

.path-item:hover {
  background: #f8f9fa;
  border-color: #e0e0e6;
}

.path-item.selected {
  background: #e6f7ff;
  border-color: #91d5ff;
}

.path-checkbox {
  margin-right: 12px;
  margin-top: 2px;
}

.path-info {
  flex: 1;
}

.path-main {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.path-mount {
  font-family: 'Monaco', 'Consolas', monospace;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #333;
}

.path-host {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  color: #666;
}

.path-details {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #666;
}

.path-mode {
  background: #f0f0f0;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 11px;
}

.path-mount-type {
  color: #999;
  font-size: 12px;
}

.path-description {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #666;
  font-style: italic;
  background: #f8f9fa;
  padding: 4px 8px;
  border-radius: 4px;
  margin-top: 6px;
}

.selection-summary {
  margin-top: 16px;
}

.summary-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.summary-stats span {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 4px;
}
</style> 