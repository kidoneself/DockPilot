<template>
  <t-dialog 
    :visible="visible" 
    @update:visible="$emit('update:visible', $event)"
    :header="title" 
    :footer="false" 
    width="80%" 
    height="80%"
  >
    <iframe v-if="url" :src="url" class="window-iframe" frameborder="0" />
  </t-dialog>
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
    default: ''
  },
  url: {
    type: String,
    default: ''
  }
});

// 定义组件事件
defineEmits(['update:visible']);
</script>

<style scoped>
.window-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

:deep(.t-dialog) {
  background: rgba(20, 20, 40, 0.75) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
}

:deep(.t-dialog__header) {
  color: #fff !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.t-dialog__body) {
  color: #fff !important;
}
</style> 