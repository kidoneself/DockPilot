<template>
  <div class="websocket-test-container">
    <t-card title="WebSocket测试">
      <t-space direction="vertical">
        <!-- 测试按钮 -->
        <t-button @click="sendTestMessage" :loading="loading">
          发送测试消息
        </t-button>
        
        <!-- 处理器状态 -->
        <t-button @click="showHandlerStatus" variant="outline">
          查看处理器状态
        </t-button>
        
        <!-- 处理器状态信息 -->
        <div v-if="Object.keys(handlerStatus).length > 0" class="handler-status">
          <h4>当前注册的WebSocket处理器:</h4>
          <div v-for="(count, type) in handlerStatus" :key="type" class="handler-item">
            <span>{{ type }}: </span>
            <t-tag theme="primary">{{ count }}</t-tag>
          </div>
        </div>

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
import type { WebSocketMessage } from '@/api/model/websocketModel';

const loading = ref(false);
const messages = ref<Array<{ time: string; content: string }>>([]);
const notificationStore = useNotificationStore();
const handlerStatus = ref<Record<string, number>>({});

// 发送测试消息
const sendTestMessage = async () => {
  loading.value = true;
  const taskId = `test_${Date.now()}`;
  
  try {
    // 添加消息处理器
    dockerWebSocketService.addMessageHandler(taskId, (message: WebSocketMessage) => {
      if (message.type === 'TEST_NOTIFY_RESPONSE') {
        messages.value.push({
          time: new Date().toLocaleString(),
          content: message.data?.message || '收到测试响应'
        });
        // 移除消息处理器
        dockerWebSocketService.removeMessageHandler(taskId);
      }
    });

    // 发送测试消息
    await dockerWebSocketService.sendMessage({
      type: 'TEST_NOTIFY',
      taskId,
      data: {
        message: '这是一条测试消息'
      }
    });
  } catch (error) {
    MessagePlugin.error('发送测试消息失败');
  } finally {
    loading.value = false;
  }
};

// 查看处理器状态
const showHandlerStatus = () => {
  handlerStatus.value = dockerWebSocketService.getRegisteredHandlersStatus();
};

// 组件卸载时清理
onUnmounted(() => {
  // 清理所有消息处理器
  const taskIds = [`test_${Date.now()}`];
  taskIds.forEach(taskId => {
    dockerWebSocketService.removeMessageHandler(taskId);
  });
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

.handler-status {
  margin: 16px 0;
  padding: 16px;
  background-color: var(--td-bg-color-container);
  border-radius: 6px;
  border: 1px solid var(--td-component-stroke);
}

.handler-item {
  margin: 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style> 