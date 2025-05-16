<template>
  <div class="popover-wrapper" ref="wrapper">
    <slot />
    <div v-if="visible" class="popover" :style="popoverStyle">
      <div class="popover-content">
        <slot name="content">确定要执行此操作吗？</slot>
      </div>
      <div class="popover-actions">
        <button class="btn cancel" @click="onCancel">取消</button>
        <button class="btn confirm" @click="onConfirm">确定</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue';
const props = defineProps<{ visible: boolean }>();
const emit = defineEmits(['update:visible', 'confirm', 'cancel']);

const wrapper = ref<HTMLElement | null>(null);
const popoverStyle = ref<any>({});

const visible = ref(props.visible);
watch(() => props.visible, v => visible.value = v);
watch(visible, v => emit('update:visible', v));

const onConfirm = () => {
  visible.value = false;
  emit('confirm');
};
const onCancel = () => {
  visible.value = false;
  emit('cancel');
};

// 点击外部关闭
const handleClickOutside = (e: MouseEvent) => {
  if (wrapper.value && !wrapper.value.contains(e.target as Node)) {
    visible.value = false;
  }
};
onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside);
  nextTick(() => {
    // 简单定位到触发元素下方
    if (wrapper.value) {
      const rect = wrapper.value.getBoundingClientRect();
      popoverStyle.value = {
        position: 'absolute',
        top: rect.height + 8 + 'px',
        left: '0px',
        zIndex: 2000,
      };
    }
  });
});
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickOutside);
});
</script>

<style scoped>
.popover-wrapper {
  display: inline-block;
  position: relative;
}
.popover {
  min-width: 180px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.10);
  padding: 16px 20px 12px 20px;
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 8px;
  z-index: 2000;
}
.popover-content {
  font-size: 14px;
  color: #333;
  margin-bottom: 12px;
}
.popover-actions {
  text-align: right;
}
.btn {
  padding: 4px 16px;
  border-radius: 4px;
  border: none;
  margin-left: 8px;
  cursor: pointer;
  font-size: 14px;
}
.btn.cancel {
  background: #f5f5f5;
  color: #666;
}
.btn.confirm {
  background: #0052d9;
  color: #fff;
}
.btn.confirm:hover {
  background: #1765ad;
}
</style> 