<template>
  <n-modal
    :show="show"
    preset="card"
    style="width: 100%; max-width: 1000px; height: 100%;"
    :mask-closable="false"
    :close-on-esc="false"
    @update:show="emitUpdateShow"
  >
    <template #header>
      <div class="log-modal-header">
        <span class="log-modal-title">{{ title }}</span>
        <div class="log-modal-actions">
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-switch
                v-model:value="autoScroll"
                size="small"
                :checked="autoScroll"
                @update:value="toggleAutoScroll"
              />
            </template>
            自动滚动
          </n-tooltip>
        </div>
      </div>
    </template>
    <div class="log-modal-content">
      <n-log
        ref="logRef"
        :log="logContent"
        :rows="20"
        :line-numbers="true"
        style="height: 100%; width: 100%;"
      />
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import type { LogInst } from 'naive-ui'

const props = defineProps<{
  show: boolean
  title?: string
  logs: string[]
}>()
const emit = defineEmits(['update:show', 'update:auto-scroll'])

const logContent = computed(() => props.logs.join('\n'))
const autoScroll = ref(true)
const logRef = ref<LogInst | null>(null)

function emitUpdateShow(val: boolean) {
  emit('update:show', val)
}
function toggleAutoScroll(val: boolean) {
  autoScroll.value = val
  emit('update:auto-scroll', autoScroll.value)
}

watch(logContent, () => {
  if (autoScroll.value) {
    nextTick(() => {
      logRef.value?.scrollTo({ position: 'bottom', silent: true })
    })
  }
})
</script>

<style scoped>
:deep(.n-modal-card) {
  width: 100% !important;
  max-width: 100% !important;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
:deep(.n-modal-body) {
  width: 100% !important;
  max-width: 100% !important;
  box-sizing: border-box;
  padding: 0 !important;
  overflow: hidden !important;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.log-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #23272e;
  padding: 8px 16px 8px 20px;
  border-radius: 6px 6px 0 0;
}
.log-modal-title {
  font-weight: bold;
  font-size: 16px;
  color: #fff;
}
.log-modal-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.log-modal-content {
  width: 100%;
  height: calc(70vh - 60px);
  background: #181c20;
  border-radius: 0 0 6px 6px;
  padding: 8px 12px;
  box-sizing: border-box;
  overflow-y: auto;
  overflow-x: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 保证 n-log 内容不撑爆 */
:deep(.n-log) {
  background: transparent !important;
  color: #e6e6e6 !important;
  font-family: 'Fira Mono', 'Consolas', 'Menlo', monospace;
  font-size: 14px;
  white-space: pre;
  word-break: break-all !important;
  overflow-x: auto !important;
  overflow-y: auto !important;
  flex: 1;
  height: 100% !important;
  min-height: 0 !important;
}
:deep(.n-log-line-number) {
  color: #888 !important;
}
</style>