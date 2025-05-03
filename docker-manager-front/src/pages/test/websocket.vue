<template>
  <div class="websocket-test-container">
    <t-card title="WebSocket测试">
      <t-space direction="vertical">
        <!-- 测试按钮 -->
        <t-button @click="sendTestMessage" :loading="loading">
          发送测试消息
        </t-button>

        <!-- 消息记录 -->
        <t-card title="消息记录" :bordered="true">
          <template v-if="messages.length > 0">
            <div v-for="(msg, index) in messages" :key="index" class="message-item">
              <t-tag theme="primary" variant="light">{{ msg.time }}</t-tag>
              <span class="message-content">{{ msg.content }}</span>
            </div>
          </template>
          <template v-else>
            <t-empty description="暂无消息" />
          </template>
        </t-card>
      </t-space>
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { dockerWebSocketService } from '@/api/websocket/DockerWebSocketService';
import { useNotificationStore } from '@/store/modules/notification';
import type { WebSocketMessage } from '@/api/websocket/types';

const loading = ref(false);
const messages = ref<Array<{ time: string; content: string }>>([]);
const notificationStore = useNotificationStore();

// 发送测试消息
const sendTestMessage = async () => {
  try {
    loading.value = true;
    await dockerWebSocketService.sendMessage({
      type: 'TEST_NOTIFY',
      taskId: String(Date.now()),
      data: {
        message: '这是一条测试消息',
        timestamp: new Date().toISOString()
      }
    });
    MessagePlugin.success('测试消息已发送');
  } catch (error) {
    MessagePlugin.error('发送失败：' + (error instanceof Error ? error.message : String(error)));
  } finally {
    loading.value = false;
  }
};

// 接收测试消息响应
const handleTestResponse = (message: WebSocketMessage) => {
  console.log('收到测试响应:', message);
  
  // 只添加到消息记录
  messages.value.unshift({
    time: new Date().toLocaleTimeString(),
    content: message.data.content || JSON.stringify(message.data)
  });
};

// 组件挂载时注册消息监听
onMounted(() => {
  dockerWebSocketService.on('TEST_NOTIFY_RESPONSE', handleTestResponse);
});

// 组件卸载时移除消息监听
onUnmounted(() => {
  dockerWebSocketService.off('TEST_NOTIFY_RESPONSE', handleTestResponse);
});
</script>

<style scoped>
.websocket-test-container {
  padding: 20px;
}

.message-item {
  margin: 10px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.message-content {
  font-size: 14px;
}
</style> 