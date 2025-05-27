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
    <!-- 配置内容区域 - 使用插槽让父组件传入不同内容 -->
    <div class="config-content">
      <!-- 默认插槽：用于传入具体的配置组件 -->
      <slot name="content" :data="modelValue" :update="updateData">
        <!-- 如果没有传入插槽内容，根据配置类型动态渲染 -->
        <component
          :is="config.component"
          v-if="config.component"
          :model-value="modelValue"
          v-bind="config.componentProps || {}"
          @update:model-value="updateData"
        />
      </slot>
    </div>

    <template #footer>
      <div class="modal-actions">
        <div class="left-actions">
          <n-button 
            v-if="config.showResetButton"
            secondary
            type="error"
            size="medium"
            @click="handleReset"
          >
            <template #icon>
              <svg
width="16"
height="16"
viewBox="0 0 24 24"
fill="none"
stroke="currentColor"
stroke-width="2">
                <polyline points="1 4 1 10 7 10"></polyline>
                <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
              </svg>
            </template>
            {{ config.resetText || '重置配置' }}
          </n-button>
        </div>
        <div class="right-actions">
          <n-button @click="handleCancel">{{ config.cancelText || '取消' }}</n-button>
          <n-button 
            type="primary" 
            :loading="loading"
            @click="handleConfirm"
          >
            {{ config.confirmText || '确定' }}
          </n-button>
        </div>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useMessage } from 'naive-ui'

// 配置接口定义
export interface ConfigModalConfig {
  title: string;                          // 模态框标题
  width?: string;                         // 模态框宽度
  component?: any;                        // 动态组件（可选）
  componentProps?: Record<string, any>;   // 组件属性
  cancelText?: string;                    // 取消按钮文字
  confirmText?: string;                   // 确认按钮文字
  showResetButton?: boolean;              // 是否显示重置按钮
  resetText?: string;                     // 重置按钮文字
  beforeConfirm?: (data: any) => boolean | Promise<boolean>; // 确认前的验证
  afterConfirm?: (data: any) => void | Promise<void>;       // 确认后的回调
  beforeReset?: () => boolean | Promise<boolean>;           // 重置前的确认
  afterReset?: () => void | Promise<void>;                  // 重置后的回调
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
  (e: 'reset'): void
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

// 处理重置
const handleReset = async () => {
  try {
    // 如果有重置前确认，先执行确认
    if (props.config.beforeReset) {
      const confirmed = await props.config.beforeReset()
      if (!confirmed) {
        return
      }
    }

    // 触发重置事件
    emit('reset')

    // 如果有重置后回调，执行回调
    if (props.config.afterReset) {
      await props.config.afterReset()
    }

    // 关闭模态框
    visible.value = false
  } catch (error) {
    console.error('Config reset error:', error)
    message.error('重置失败，请重试')
  }
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
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.left-actions {
  display: flex;
  gap: 12px;
}

.right-actions {
  display: flex;
  gap: 12px;
}
</style> 