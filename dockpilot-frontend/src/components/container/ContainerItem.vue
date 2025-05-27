<template>
  <n-card
    class="container-card"
    hoverable
    bordered
    :style="{ borderLeft: `6px solid ${statusBarColor}` }"
  >
    <div class="container-row">
      <!-- 中间：容器信息和图标 -->
      <div class="container-main">
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
        <div class="container-info-content">
          <!-- 第一行：名称 -->
          <div class="container-name-row">
            <n-ellipsis 
              :tooltip="true" 
              class="container-name-text"
            >
              {{ container.name }}
            </n-ellipsis>
          </div>
          <!-- 第二行：状态和错误信息 -->
          <div class="container-status-row">
            <NTag :type="getStatusType(container.status)" size="small">
              {{ container.status }}
            </NTag>
            <span v-if="container.lastError" class="error-text">
              {{ container.lastError }}
            </span>
          </div>
          <!-- 第三行：资源信息 -->
          <div class="container-meta-row">
            <span class="resource-label">CPU：</span>
            <div class="resource-item">
              <NProgress
                :percentage="container.cpu"
                :color="cpuColor"
                :height="16"
                type="line"
                :indicator-placement="'inside'"
                processing
              >
                {{ container.cpu }}%
              </NProgress>
            </div>
            <span class="resource-label">内存：</span>
            <div class="resource-item">
              <NProgress
                :percentage="memoryPercentage"
                :color="memoryColor"
                :height="16"
                type="line"
                :indicator-placement="'inside'"
                processing
              >
                {{ memoryPercentage.toFixed(2) }}%
              </NProgress>
            </div>
            <span class="resource-label up">↑</span>
            <span class="resource-value up">{{ container.upload }}</span>
            <span class="resource-label down">↓</span>
            <span class="resource-value down">{{ container.download }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧：操作按钮组 -->
      <div class="container-actions">
        <NSpace>
          <!-- WebUI链接按钮 -->
          <NButton
            v-if="container.webUrl"
            size="small"
            circle
            quaternary
            @click="openWebUI"
          >
            <template #icon>
              <NIcon><LinkOutline /></NIcon>
            </template>
          </NButton>

          <!-- 主要操作按钮 -->
          <NButton
            :type="container.status === 'running' ? 'warning' : 'success'"
            :disabled="operating"
            :loading="
              operating && 
              currentAction === (container.status === 'running' ? 'stop' : 'start')
            "
            @click="handleAction(container.status === 'running' ? 'stop' : 'start')"
          >
            <template #icon>
              <NIcon>
                <component 
                  :is="container.status === 'running' ? StopOutline : PlayOutline" 
                />
              </NIcon>
            </template>
            {{ container.status === 'running' ? '停止' : '启动' }}
          </NButton>

          <!-- 更多操作下拉菜单 -->
          <NDropdown
            :options="getContainerOptions"
            :disabled="operating"
            @select="handleMoreAction"
          >
            <NButton 
              :loading="operating && currentAction !== 'start' && currentAction !== 'stop'"
            >
              <template #icon>
                <NIcon><EllipsisHorizontalOutline /></NIcon>
              </template>
              更多操作
            </NButton>
          </NDropdown>
        </NSpace>
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
  LinkOutline
} from '@vicons/ionicons5'
import { NIcon, NProgress, NButton, NSpace, NAvatar, NTag, NDropdown } from 'naive-ui'

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
}>()

const emit = defineEmits<{
  (e: 'action', action: string, container: DisplayContainer): void // 使用 DisplayContainer 类型
  (e: 'edit', containerId: string): void
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
  const options: Array<any> = [
    {
      label: '配置Web',
      key: 'configWebUI',
      icon: () => h(NIcon, null, { default: () => h(GlobeOutline) }),
      disabled: props.operating
    },
    {
      type: 'divider',
      key: 'config-divider'
    },
    {
      label: '查看详情',
      key: 'detail',
      icon: () => h(NIcon, null, { default: () => h(InformationCircleOutline) }),
      disabled: props.operating
    },
    {
      label: '查看日志',
      key: 'logs',
      icon: () => h(NIcon, null, { default: () => h(DocumentTextOutline) }),
      disabled: props.operating
    },
    {
      label: '编辑配置',
      key: 'edit',
      icon: () => h(NIcon, null, { default: () => h(CreateOutline) }),
      disabled: props.operating
    }
  ]

  // 如果容器正在运行，添加重启选项
  if (props.container.status === 'running') {
    options.push({
      label: props.operating && props.currentAction === 'restart' ? '重启中...' : '重启容器',
      key: 'restart',
      icon: () => h(NIcon, null, { default: () => h(RefreshOutline) }),
      disabled: props.operating
    })
  }

  // 添加删除选项
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
  align-items: flex-start;
}

.container-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: flex-start; /* Align icon and info to the top */
  gap: 18px; /* Gap between icon and info content */
}

.container-icon {
  /* Style remains similar, but now inside .container-main */
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.container-info-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.container-name-row {
  margin-bottom: 0;
  line-height: 1.2;
}

.container-name-text {
  font-weight: bold;
  font-size: 16px;
  max-width: 400px;
}

.container-status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.container-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  color: #888;
  font-size: 14px;
}

.container-actions {
  margin-left: 18px;
  display: flex;
  align-items: center;
}

.resource-label {
  margin-left: 8px;
}

.resource-label.up {
  color: #18a058;
}

.resource-label.down {
  color: #d03050;
}

.resource-value {
  color: var(--n-text-color-2);
  font-size: 15px;
  margin-left: 4px;
}

.resource-item {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 100px;
}

.resource-value.up { color: #18a058; }
.resource-value.down { color: #d03050; }

.error-text {
  color: #d03050;
  font-size: 13px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 响应式布局 */
/* 大平板 */
@media (max-width: 1200px) {
  .container-name-text {
    max-width: 350px;
  }
  
  .container-meta-row {
    flex-wrap: wrap;
    gap: 8px;
    font-size: 14px;
  }
  .resource-item {
    min-width: 80px;
  }
}

/* 中等平板 */
@media (max-width: 1024px) {
  .container-name-text {
    max-width: 300px;
  }
  
  .container-row {
    flex-wrap: wrap;
    gap: 12px;
    padding: 12px;
  }

  .container-main {
    gap: 12px;
  }

  .container-name-row {
    min-width: 120px;
  }
  .container-status-row {
    min-width: 120px;
  }
  .container-meta-row {
    flex-wrap: wrap;
    gap: 10px;
    font-size: 14px;
  }
  .resource-label, .resource-value {
    font-size: 14px;
  }
  .resource-item {
    min-width: 70px;
  }
  .n-progress {
    width: 60px !important;
  }
}

/* 小平板 */
@media (max-width: 900px) {
  .container-name-text {
    max-width: 250px;
  }
  
  .container-actions {
    margin-left: 12px;
  }
  
  /* 按钮组优化 */
  .container-actions :deep(.n-space) {
    gap: 8px !important;
  }
  
  .container-actions :deep(.n-button) {
    font-size: 13px;
    padding: 0 12px;
  }
}

/* 手机横屏 */
@media (max-width: 768px) {
  .container-name-text {
    max-width: 200px;
  }
  
  .container-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 8px;
  }

  .container-main {
    flex-direction: row;
    align-items: flex-start;
    gap: 12px;
    width: 100%;
  }
  
  .container-icon {
    flex-shrink: 0;
  }

  .container-info-content {
    flex: 1;
    min-width: 0;
  }
  
  .container-name-row {
    min-width: 0;
    width: 100%;
  }
  
  .container-status-row {
    min-width: 0;
    width: 100%;
  }
  
  .container-meta-row {
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    width: 100%;
    font-size: 13px;
  }

  .resource-item {
    min-width: 60px;
    gap: 2px;
  }

  .resource-label, .resource-value {
    font-size: 13px;
    margin-left: 0;
  }
  
  .container-actions {
    margin-left: 0;
    align-self: flex-end;
    margin-top: 8px;
    width: 100%;
    justify-content: flex-end;
  }
  
  .n-progress {
    width: 50px !important;
    height: 14px !important;
  }
}

/* 手机竖屏 */
@media (max-width: 480px) {
  .container-name-text {
    max-width: 150px;
    font-size: 15px;
  }
  
  .container-row {
    padding: 6px;
    gap: 8px;
  }

  .container-main {
    flex-direction: column;
    gap: 8px;
  }
  
  .container-icon {
    align-self: flex-start;
  }

  .container-info-content {
    width: 100%;
  }
  
  .container-meta-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    font-size: 12px;
  }

  .container-meta-row > span,
  .container-meta-row > div {
    display: flex;
    align-items: center;
    margin-bottom: 2px;
  }

  .resource-item {
    min-width: auto;
    width: 100%;
    gap: 4px;
  }

  .resource-label, .resource-value {
    font-size: 12px;
  }
  
  .container-actions {
    width: 100%;
    justify-content: center;
    margin-top: 10px;
  }
  
  .container-actions :deep(.n-button) {
    font-size: 12px;
    min-width: 70px;
  }
  
  .n-progress {
    width: 100% !important;
    max-width: 120px;
  }
}

/* 超小屏幕 */
@media (max-width: 360px) {
  .container-name-text {
    max-width: 120px;
    font-size: 14px;
  }
  
  .container-row {
    padding: 4px;
  }
  
  .container-actions :deep(.n-button) {
    min-width: 60px;
    padding: 0 6px;
  }
  
  /* 更多操作按钮文字简化 */
  .container-actions :deep(.n-button:last-child .n-button__content) {
    font-size: 11px;
  }
}
</style>