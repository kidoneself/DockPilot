<template>
  <t-config-provider :global-config="getComponentsLocale">
    <router-view :class="[mode]" />
  </t-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue';
import { useLocale } from '@/locales/useLocale';
import { useSettingStore } from '@/store';
import { dockerWebSocketService } from '@/api/websocket/DockerWebSocketService';
import { useNotificationStore } from '@/store/modules/notification';

const store = useSettingStore();
const notificationStore = useNotificationStore();

const mode = computed(() => {
  return store.displayMode;
});

const { getComponentsLocale } = useLocale();

// 初始化 WebSocket 连接
onMounted(async () => {
  try {
    await dockerWebSocketService.connect();
    console.log('WebSocket 连接已建立');
  } catch (error) {
    console.error('WebSocket 连接失败:', error);
  }
});

// 组件卸载时断开连接
onUnmounted(() => {
  dockerWebSocketService.disconnect();
  console.log('WebSocket 连接已断开');
});
</script>

<style lang="less" scoped>
#nprogress .bar {
  background: var(--td-brand-color) !important;
}
</style>
