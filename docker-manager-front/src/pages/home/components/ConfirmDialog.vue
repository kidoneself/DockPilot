<template>
  <div v-if="visible" class="confirm-dialog-overlay" @click="handleOverlayClick">
    <div class="confirm-dialog" @click.stop>
      <div class="confirm-dialog-header">
        <t-icon name="warning" class="warning-icon" />
        <span>{{ title }}</span>
      </div>
      <div class="confirm-dialog-body">
        {{ content }}
      </div>
      <div class="confirm-dialog-footer">
        <t-button theme="default" @click="handleCancel">取消</t-button>
        <t-button theme="danger" @click="handleConfirm">确认</t-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue';

// 定义组件属性
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '确认操作'
  },
  content: {
    type: String,
    default: '确定要执行此操作吗？'
  }
});

// 定义组件事件
const emit = defineEmits(['update:visible', 'confirm', 'cancel']);

// 点击遮罩层
function handleOverlayClick() {
  emit('update:visible', false);
}

// 点击取消按钮
function handleCancel() {
  emit('update:visible', false);
  emit('cancel');
}

// 点击确认按钮
function handleConfirm() {
  emit('update:visible', false);
  emit('confirm');
}
</script>

<style scoped>
.confirm-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.confirm-dialog {
  background: rgba(20, 20, 40, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  width: 360px;
  overflow: hidden;
  animation: dialogFadeIn 0.2s ease;
}

.confirm-dialog-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
}

.warning-icon {
  color: #ef4444;
  font-size: 20px;
}

.confirm-dialog-body {
  padding: 20px;
  color: #e2e8f0;
  font-size: 14px;
  line-height: 1.5;
}

.confirm-dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@keyframes dialogFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .confirm-dialog {
    width: 90%;
  }
}
</style> 