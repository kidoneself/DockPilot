<template>
  <n-modal v-model:show="localShow" :mask-closable="true" style="width: 900px;">
    <n-card
      title="正在安装应用"
      :bordered="false"
      size="huge"
      role="dialog"
      aria-modal="true"
    >
      <div class="install-modal-content">
        <!-- 安装进度 -->
        <div class="install-progress">
          <n-progress
            type="line"
            :percentage="progress"
            :status="status"
            :height="8"
            style="margin-bottom: 16px;"
          />
          <div class="progress-text">{{ progressText }}</div>
        </div>

        <!-- 安装日志 -->
        <div class="install-logs">
          <h4>安装日志</h4>
          <div ref="logContainer" class="log-container">
            <div v-for="log in logs" :key="log.id" class="log-item">
              <span class="log-time">{{ log.time }}</span>
              <span class="log-level" :class="log.level">{{ log.level.toUpperCase() }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="modal-actions">
          <n-button 
            v-if="status === 'error'"
            type="warning"
            @click="$emit('retry')"
          >
            重试安装
          </n-button>
          <n-button 
            v-if="status === 'success'"
            type="primary"
            @click="$emit('finish')"
          >
            完成
          </n-button>
          <n-button 
            v-if="status === 'active'"
            @click="$emit('cancel')"
          >
            关闭
          </n-button>
          <n-button 
            v-if="status === 'error'"
            @click="$emit('cancel')"
          >
            关闭
          </n-button>
        </div>
      </template>
    </n-card>
  </n-modal>
</template>

<script setup lang="ts">
import { computed, ref, nextTick, watch } from 'vue'

// 日志项类型定义
interface LogItem {
  id: number
  time: string
  level: 'info' | 'warn' | 'error'
  message: string
}

// 组件属性定义
interface Props {
  show: boolean
  progress: number
  status: 'active' | 'success' | 'error'
  logs: LogItem[]
  progressText?: string
}

// 组件事件定义
interface Emits {
  'update:show': [show: boolean]
  retry: []
  finish: []
  cancel: []
}

// 定义props和emits
const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 组件状态
const logContainer = ref<HTMLElement | null>(null)

// 双向绑定show属性
const localShow = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value)
})

// 自动滚动到日志底部
const scrollToBottom = () => {
  nextTick(() => {
    const container = logContainer.value
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

// 监听日志变化，自动滚动
watch(() => props.logs, scrollToBottom, { deep: true })
</script>

<style scoped>
.install-modal-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-height: 70vh;
}

.install-progress {
  padding: 20px;
  background: var(--card-color-hover);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.progress-text {
  font-size: 14px;
  color: var(--text-color-2);
  margin-top: 8px;
  text-align: center;
}

.install-logs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.install-logs h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color-1);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.log-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: var(--code-color);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  line-height: 1.5;
  max-height: 300px;
  min-height: 200px;
}

.log-item {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  word-break: break-all;
}

.log-time {
  color: var(--text-color-3);
  flex-shrink: 0;
  width: 60px;
  font-size: 11px;
}

.log-level {
  flex-shrink: 0;
  width: 50px;
  font-weight: 600;
  font-size: 11px;
}

.log-level.info {
  color: #10b981;
}

.log-level.warn {
  color: #f59e0b;
}

.log-level.error {
  color: #f87171;
}

.log-message {
  flex: 1;
  min-width: 0;
  color: var(--text-color-1);
  font-size: 12px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

/* 深色模式增强 */
[data-theme="dark"] .install-progress {
  background: rgba(255, 255, 255, 0.02);
  border-color: rgba(255, 255, 255, 0.06);
}

[data-theme="dark"] .log-container {
  background: rgba(0, 0, 0, 0.4);
  border-color: rgba(255, 255, 255, 0.06);
}

/* 滚动条样式 */
.log-container::-webkit-scrollbar {
  width: 6px;
}

.log-container::-webkit-scrollbar-track {
  background: var(--scrollbar-color);
  border-radius: 3px;
}

.log-container::-webkit-scrollbar-thumb {
  background: var(--scrollbar-hover-color);
  border-radius: 3px;
}

.log-container::-webkit-scrollbar-thumb:hover {
  background: var(--primary-color);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .install-modal-content {
    max-height: 60vh;
  }
  
  .log-container {
    max-height: 200px;
    min-height: 150px;
  }
  
  .modal-actions {
    flex-direction: column;
    gap: 8px;
  }
}
</style> 