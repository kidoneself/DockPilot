<template>
  <n-card
    class="container-card"
    hoverable
    bordered
    :style="{ borderLeft: `6px solid ${statusBarColor}` }"
  >
    <div class="container-row">
      <!-- 多选模式复选框 -->
      <div v-if="multiSelectMode" class="container-select-section">
        <NCheckbox 
          :checked="selected"
          @update:checked="(checked) => emit('select', container.id, checked)"
        />
      </div>

      <!-- 第一区域：容器基础信息 -->
      <div class="container-basic-section">
        <!-- 图标 -->
        <div class="container-icon">
          <NAvatar
            v-if="container.iconUrl && !iconError"
            :src="container.iconUrl"
            :size="40"
            round
            @error="handleIconError"
          />
          <NIcon v-else :size="40">
            <ServerOutline />
          </NIcon>
        </div>
        <div class="container-basic-info">
          <!-- 第一行：状态和名称 -->
          <div class="container-status-name-row">
            <NTag :type="getStatusType(container.status)" size="small">
              {{ container.status }}
            </NTag>
            <!-- 🔄 三状态标签：1=有更新，2=老版本 -->
            <NTag
v-if="container.needUpdate === 1"
type="warning"
size="small"
class="update-tag">
              <template #icon>
                <NIcon><CloudUploadOutline /></NIcon>
              </template>
              有更新
            </NTag>
            <NTag
v-if="container.needUpdate === 2"
type="default"
size="small"
class="old-version-tag">
              <template #icon>
                <NIcon><ArchiveOutline /></NIcon>
              </template>
              老版本
            </NTag>
            <n-ellipsis 
              :tooltip="true" 
              class="container-name-text"
            >
              {{ container.name }}
            </n-ellipsis>
            <span v-if="container.lastError" class="error-text">
              {{ container.lastError }}
            </span>
          </div>
          <!-- 第二行：镜像信息 -->
          <div class="container-image-row">
            <n-ellipsis 
              :tooltip="true" 
              class="container-image-text"
            >
              {{ container.image }}
            </n-ellipsis>
          </div>
        </div>
      </div>

      <!-- 第二区域：CPU和内存 -->
      <div class="container-cpu-memory-section">
        <div class="cpu-memory-grid">
          <!-- CPU使用率 -->
          <div class="metric-item">
            <div class="metric-label">CPU</div>
            <div class="metric-progress">
              <NProgress
                :percentage="container.cpu"
                :color="cpuColor"
                :height="12"
                type="line"
                :indicator-placement="'inside'"
                processing
              >
                {{ container.cpu }}%
              </NProgress>
            </div>
          </div>
          <!-- 内存使用率 -->
          <div class="metric-item">
            <div class="metric-label">内存</div>
            <div class="metric-progress">
              <NProgress
                :percentage="memoryPercentage"
                :color="memoryColor"
                :height="12"
                type="line"
                :indicator-placement="'inside'"
                processing
              >
                {{ memoryPercentage.toFixed(1) }}%
              </NProgress>
            </div>
          </div>
        </div>
      </div>

      <!-- 第三区域：网络上传下载 -->
      <div class="container-network-section">
        <div class="network-grid">
          <!-- 网络上传 -->
          <div class="network-item">
            <div class="network-label network-up">
              <NIcon class="network-icon"><ArrowUpOutline /></NIcon>
              上传
            </div>
            <div class="network-value network-up">{{ container.upload }}</div>
          </div>
          <!-- 网络下载 -->
          <div class="network-item">
            <div class="network-label network-down">
              <NIcon class="network-icon"><ArrowDownOutline /></NIcon>
              下载
            </div>
            <div class="network-value network-down">{{ container.download }}</div>
          </div>
        </div>
      </div>

      <!-- 第四区域：操作按钮组 -->
      <div v-if="!multiSelectMode" class="container-actions-section">
        <div class="actions-layout">
          <!-- WebUI按钮 -->
          <NTooltip v-if="container.webUrl" trigger="hover">
            <template #trigger>
              <NButton
                size="small"
                circle
                quaternary
                class="web-button"
                @click="openWebUI"
              >
                <template #icon>
                  <NIcon><LinkOutline /></NIcon>
                </template>
              </NButton>
            </template>
            打开WebUI
          </NTooltip>

          <!-- 启停按钮 -->
          <NButton
            :type="container.status === 'running' ? 'error' : 'success'"
            :disabled="operating"
            :loading="
              operating && 
              currentAction === (container.status === 'running' ? 'stop' : 'start')
            "
            size="small"
            class="start-stop-button"
            @click="handleAction(container.status === 'running' ? 'stop' : 'start')"
          >
            <template #icon>
              <NIcon>
                <component 
                  :is="container.status === 'running' ? StopOutline : PlayOutline" 
                />
              </NIcon>
            </template>
            <span class="button-text">{{ container.status === 'running' ? '停止' : '启动' }}</span>
          </NButton>

          <!-- 更多操作 -->
          <NDropdown
            :options="getContainerOptions"
            :disabled="operating"
            placement="bottom-end"
            @select="handleMoreAction"
          >
            <NButton 
              size="small"
              quaternary
              circle
              :loading="operating && !['start', 'stop', 'update'].includes(currentAction || '')"
              class="more-button"
            >
              <template #icon>
                <NIcon><EllipsisHorizontalOutline /></NIcon>
              </template>
            </NButton>
          </NDropdown>
        </div>
      </div>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import {
  PlayOutline,
  StopOutline,
  RefreshOutline,
  TrashOutline,
  ServerOutline,
  InformationCircleOutline,
  DocumentTextOutline,
  CreateOutline,
  EllipsisHorizontalOutline,
  GlobeOutline,
  LinkOutline,
  ArrowUpOutline,
  ArrowDownOutline,
  CloudUploadOutline,
  ArchiveOutline
} from '@vicons/ionicons5'
import { NIcon, NProgress, NButton, NSpace, NAvatar, NTag, NDropdown, NCheckbox, NTooltip } from 'naive-ui'

// 定义用于前端展示的容器接口，与 ContainerList.vue 中的 DisplayContainer 一致
interface DisplayContainer {
  id: string;
  name: string;
  image: string;
  status: string; // 'running', 'exited', etc.
  ports: string[]; // 格式化后的端口映射
  created: string; // 格式化后的创建时间
  cpu: number;
  memory: string; // 格式化后的内存使用量
  memoryUsageRaw?: number; // 原始内存使用量 (字节)
  memoryLimitRaw?: number; // 原始内存限制 (字节)
  network: string;
  lastError?: string; // 错误信息
  project: string; // 项目信息
  upload: string;
  download: string;
  webUrl?: string;
  iconUrl?: string;
  needUpdate?: number; // 🎯 添加needUpdate字段
  // 可能包含后端 Container 原始类型中的其他字段，按需添加
  names: string[];
  imageId: string;
  command: string;
  state: string;
  mounts: any[];
  labels: Record<string, string>;
  hostConfig: { networkMode: string };
  stats?: {
    cpu?: number;
    memory?: string;
    network?: { upload: string; download: string };
    cpuPercent?: number;
    memoryUsage?: number;
    memoryLimit?: number;
    networkTx?: number;
    networkRx?: number;
  };
}

const props = defineProps<{
  container: DisplayContainer // 使用 DisplayContainer 类型
  operating?: boolean
  currentAction?: string
  multiSelectMode?: boolean // 多选模式
  selected?: boolean // 是否被选中
}>()

const emit = defineEmits<{
  (e: 'action', action: string, container: DisplayContainer): void // 使用 DisplayContainer 类型
  (e: 'edit', containerId: string): void
  (e: 'select', containerId: string, selected: boolean): void // 选择事件
}>()

// 图标错误处理
const iconError = ref(false)

// 处理图标加载错误
function handleIconError() {
  iconError.value = true
}

// 打开WebUI
function openWebUI() {
  if (props.container.webUrl) {
    window.open(props.container.webUrl, '_blank')
  }
}

// 获取容器操作选项
const getContainerOptions = computed(() => {
  const options: Array<any> = []

  // 🎯 如果需要更新（状态1），优先显示更新镜像选项
  if (props.container.needUpdate === 1) {
    options.push({
      label: props.operating && props.currentAction === 'update' ? '更新中...' : '更新镜像',
      key: 'update',
      icon: () => h(NIcon, null, { default: () => h(CloudUploadOutline) }),
      disabled: props.operating,
      props: {
        type: 'warning'
      }
    })
    options.push({
      type: 'divider',
      key: 'update-divider'
    })
  }

  // 配置相关选项
  options.push({
    label: '配置Web',
    key: 'configWebUI',
    icon: () => h(NIcon, null, { default: () => h(GlobeOutline) }),
    disabled: props.operating
  })
  
  options.push({
    type: 'divider',
    key: 'config-divider'
  })
  
  // 查看相关选项
  options.push({
    label: '查看详情',
    key: 'detail',
    icon: () => h(NIcon, null, { default: () => h(InformationCircleOutline) }),
    disabled: props.operating
  })
  options.push({
    label: '查看日志',
    key: 'logs',
    icon: () => h(NIcon, null, { default: () => h(DocumentTextOutline) }),
    disabled: props.operating
  })
  options.push({
    label: '编辑配置',
    key: 'edit',
    icon: () => h(NIcon, null, { default: () => h(CreateOutline) }),
    disabled: props.operating
  })

  // 如果容器正在运行，添加重启选项
  if (props.container.status === 'running') {
    options.push({
      type: 'divider',
      key: 'action-divider'
    })
    options.push({
      label: props.operating && props.currentAction === 'restart' ? '重启中...' : '重启容器',
      key: 'restart',
      icon: () => h(NIcon, null, { default: () => h(RefreshOutline) }),
      disabled: props.operating
    })
  }

  // 删除选项
  options.push({
    type: 'divider',
    key: 'd1'
  })
  options.push({
    label: props.operating && props.currentAction === 'delete' ? '删除中...' : '删除容器',
    key: 'delete',
    icon: () => h(NIcon, null, { default: () => h(TrashOutline) }),
    disabled: props.operating,
    props: {
      type: 'error'
    }
  })

  return options
})

// 处理更多操作
function handleMoreAction(key: string) {
  if (key === 'configWebUI') {
    emit('action', 'configWebUI', props.container)
  } else {
    emit('action', key, props.container)
  }
}

// 处理主要操作
function handleAction(action: string) {
  emit('action', action, props.container)
}

const statusBarColor = computed(() => {
  switch (props.container.status) {
    case 'running':
      return '#18a058'
    case 'exited':
      return '#f0a020'
    default:
      return '#2080f0'
  }
})

const cpuColor = computed(() => {
  const cpu = props.container.cpu
  if (cpu < 50) return '#18a058'
  if (cpu < 80) return '#f0a020'
  return '#d03050'
})

const memoryPercentage = computed(() => {
  if (
    props.container.memoryUsageRaw !== undefined && 
    props.container.memoryLimitRaw !== undefined && 
    props.container.memoryLimitRaw > 0
  ) {
    const usage = props.container.memoryUsageRaw
    const limit = props.container.memoryLimitRaw
    return Number(((usage / limit) * 100).toFixed(2))
  }
  return 0
})

const memoryColor = computed(() => {
  const percentage = memoryPercentage.value
  if (percentage < 50) return '#18a058'
  if (percentage < 80) return '#f0a020'
  return '#d03050'
})

const getStatusType = (status: string) => {
  switch (status) {
    case 'running':
      return 'success'
    case 'exited':
      return 'warning'
    default:
      return 'info'
  }
}
</script>

<style scoped>
.container-card {
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
  border-radius: 12px;
  background: var(--n-card-color);
  padding: 0;
}

.container-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
}

/* 多选区域 */
.container-select-section {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* 第一区域：容器基础信息 - 40% */
.container-basic-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 2;
  min-width: 0;
  padding: 2px 6px;
}

.container-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.container-basic-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.container-status-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.1;
}

.container-name-text {
  font-weight: bold;
  font-size: 14px;
  color: var(--n-text-color-base);
  flex: 1;
  min-width: 0;
}

.container-image-row {
  margin-top: 1px;
}

.container-image-text {
  font-size: 12px;
  color: var(--n-text-color-3);
  line-height: 1.3;
}

.error-text {
  color: #d03050;
  font-size: 11px;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
}

/* 第二区域：CPU和内存 - 20% */
.container-cpu-memory-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 4px 6px;
  background: rgba(var(--n-primary-color-rgb), 0.04);
  border-radius: 6px;
  border: 1px solid rgba(var(--n-primary-color-rgb), 0.08);
}

.cpu-memory-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.metric-label {
  font-size: 11px;
  font-weight: 500;
  color: var(--n-text-color-2);
  min-width: 35px;
  text-align: left;
}

.metric-progress {
  flex: 1;
}

.metric-value {
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  padding: 1px 4px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.8);
}

/* 第三区域：网络上传下载 - 20% */
.container-network-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 4px 8px;
  background: rgba(var(--n-success-color-rgb), 0.04);
  border-radius: 6px;
  border: 1px solid rgba(var(--n-success-color-rgb), 0.08);
}

.network-grid {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.network-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
}

.network-label {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 10px;
  font-weight: 500;
  min-width: 40px;
}

.network-label.network-up {
  color: var(--n-warning-color);
}

.network-label.network-down {
  color: var(--n-success-color);
}

.network-icon {
  font-size: 10px;
}

.network-value {
  font-size: 11px;
  font-weight: 600;
  text-align: right;
  min-width: 36px;
}

.network-value.network-up {
  color: var(--n-warning-color);
}

.network-value.network-down {
  color: var(--n-success-color);
}

/* 第四区域：操作按钮组 - 20% */
.container-actions-section {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 4px;
}

/* 🎯 新的按钮布局样式 */
.actions-layout {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
  position: relative;
}

.priority-button {
  flex-shrink: 0;
}

.secondary-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.web-button {
  color: var(--n-primary-color);
  border: 1px solid var(--n-primary-color-suppl);
}

.web-button:hover {
  background: var(--n-primary-color-suppl);
}

.start-stop-button {
  font-weight: 500;
}

.more-button {
  color: var(--n-text-color-2);
  border: 1px solid var(--n-border-color);
}

.more-button:hover {
  background: var(--n-color-hover);
  color: var(--n-text-color-1);
}

/* 按钮文字在小屏幕上的响应式 */
.button-text {
  display: inline;
}

/* 🎯 更新相关样式 */
.update-tag {
  animation: pulse 2s infinite;
}

/* 老版本标签样式 */
.old-version-tag {
  border: 1px solid #d9d9d9;
  background: #f5f5f5;
  color: #666;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .container-row {
    gap: 8px;
    padding: 6px 10px;
  }
  
  .container-basic-section {
    flex: 3;
  }
  
  .container-cpu-memory-section,
  .container-network-section {
    flex: 1;
  }
  
  .container-actions-section {
    flex: 1;
  }
  
  /* 调整按钮间距 */
  .actions-layout {
    gap: 6px;
  }
  
  .secondary-actions {
    gap: 4px;
  }
}

@media (max-width: 900px) {
  .container-row {
    flex-wrap: wrap;
    gap: 6px;
  }
  
  .container-basic-section {
    flex: 1;
    min-width: 300px;
  }
  
  .container-cpu-memory-section,
  .container-network-section {
    flex: 0 1 120px;
  }
  
  .container-actions-section {
    flex: 0 1 200px;
    justify-content: center;
  }
  
  /* 在中等屏幕上缩短按钮文字 */
  .button-text {
    display: inline;
  }
  
  .actions-layout {
    justify-content: center;
  }
}

@media (max-width: 600px) {
  .container-row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .container-basic-section,
  .container-cpu-memory-section,
  .container-network-section,
  .container-actions-section {
    flex: none;
    width: 100%;
  }
  
  .container-actions-section {
    justify-content: center;
  }
  
  .cpu-memory-grid,
  .network-grid {
    flex-direction: row;
    justify-content: space-around;
  }
  
  .metric-item,
  .network-item {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  /* 🎯 小屏幕按钮优化 */
  .actions-layout {
    gap: 8px;
    justify-content: center;
  }
  
  .secondary-actions {
    gap: 8px;
  }
  
  /* 保持按钮文字显示 */
  .button-text {
    display: inline;
  }
}

/* 超小屏幕 - 隐藏按钮文字，只显示图标 */
@media (max-width: 480px) {
  .button-text {
    display: none;
  }
  
  .start-stop-button {
    min-width: 40px;
    padding: 0 8px;
  }
  
  .actions-layout {
    gap: 6px;
  }
  
  .secondary-actions {
    gap: 6px;
  }
}
</style>