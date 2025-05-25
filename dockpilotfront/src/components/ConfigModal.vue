<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    :title="config.title"
    size="large"
    :auto-focus="false"
    :close-on-esc="true"
    :style="{ width: config.width || '600px' }"
    @after-leave="handleAfterLeave"
  >
    <template #header-extra>
      <n-button quaternary circle @click="handleCancel">
        <n-icon :component="CloseOutline" />
      </n-button>
    </template>

    <!-- 配置内容区域 - 使用插槽让父组件传入不同内容 -->
    <div class="config-content">
      <!-- 默认插槽：用于传入具体的配置组件 -->
      <slot name="content" :data="modelValue" :update="updateData">
        <!-- 如果没有传入插槽内容，根据配置类型动态渲染 -->
        <component
          v-if="config.component"
          :is="config.component"
          :model-value="modelValue"
          @update:model-value="updateData"
          v-bind="config.componentProps || {}"
        />
      </slot>
    </div>

    <template #footer>
      <div class="modal-actions">
        <n-button @click="handleCancel">{{ config.cancelText || '取消' }}</n-button>
        <n-button 
          type="primary" 
          @click="handleConfirm"
          :loading="loading"
        >
          {{ config.confirmText || '确定' }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { CloseOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'

// 配置接口定义
export interface ConfigModalConfig {
  title: string;                          // 模态框标题
  width?: string;                         // 模态框宽度
  component?: any;                        // 动态组件（可选）
  componentProps?: Record<string, any>;   // 组件属性
  cancelText?: string;                    // 取消按钮文字
  confirmText?: string;                   // 确认按钮文字
  beforeConfirm?: (data: any) => boolean | Promise<boolean>; // 确认前的验证
  afterConfirm?: (data: any) => void | Promise<void>;       // 确认后的回调
}

interface Props {
  show: boolean
  config: ConfigModalConfig
  modelValue?: any
  loading?: boolean
}

interface Emits {
  (e: 'update:show', value: boolean): void
  (e: 'update:modelValue', value: any): void
  (e: 'confirm', data: any): void
  (e: 'cancel'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const message = useMessage()

// 内部状态
const visible = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value)
})

// 更新数据
const updateData = (data: any) => {
  emit('update:modelValue', data)
}

// 处理确认
const handleConfirm = async () => {
  try {
    // 如果有确认前验证，先执行验证
    if (props.config.beforeConfirm) {
      const isValid = await props.config.beforeConfirm(props.modelValue)
      if (!isValid) {
        return
      }
    }

    // 触发确认事件
    emit('confirm', props.modelValue)

    // 如果有确认后回调，执行回调
    if (props.config.afterConfirm) {
      await props.config.afterConfirm(props.modelValue)
    }

    // 关闭模态框
    visible.value = false
  } catch (error) {
    console.error('Config confirm error:', error)
    message.error('操作失败，请重试')
  }
}

// 处理取消
const handleCancel = () => {
  emit('cancel')
  visible.value = false
}

// 模态框关闭后的清理
const handleAfterLeave = () => {
  // 可以在这里做一些清理工作
}
</script>

<style scoped>
.config-content {
  min-height: 200px;
  max-height: 70vh;
  overflow-y: auto;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style> 