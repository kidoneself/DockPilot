<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { dockerWebSocketService } from '@/api/websocket/DockerWebSocketService';

interface WebSocketMessage {
  type: string;
  data: {
    progress?: number;
    status?: string;
    [key: string]: any;
  };
}

const imageName = ref('');
const isPulling = ref(false);
const pullProgress = ref(0);
const pullStatus = ref('');
const pullError = ref('');

// 注册消息处理器
const registerHandlers = () => {
  dockerWebSocketService.on('PULL_PROGRESS', (message: WebSocketMessage) => {
    const { progress } = message.data;
    pullProgress.value = progress || 0;
  });

  dockerWebSocketService.on('PULL_COMPLETE', (message: WebSocketMessage) => {
    const { status } = message.data;
    pullStatus.value = status || '';
    isPulling.value = false;
  });

  dockerWebSocketService.on('ERROR', (message: WebSocketMessage) => {
    pullError.value = message.data.toString();
    isPulling.value = false;
  });
};

// 移除消息处理器
const removeHandlers = () => {
  dockerWebSocketService.off('PULL_PROGRESS', () => {});
  dockerWebSocketService.off('PULL_COMPLETE', () => {});
  dockerWebSocketService.off('ERROR', () => {});
};

// 拉取镜像
const pullImage = () => {
  if (!imageName.value) return;
  
  isPulling.value = true;
  pullProgress.value = 0;
  pullStatus.value = '';
  pullError.value = '';
  
  dockerWebSocketService.send({
    type: 'PULL_IMAGE',
    taskId: '',
    data: { imageName: imageName.value }
  });
};

onMounted(() => {
  registerHandlers();
});

onUnmounted(() => {
  removeHandlers();
});
</script>

<template>
    <div class="docker-image">
        <div class="operation-panel">
            <input v-model="imageName" placeholder="输入镜像名称" />
            <button @click="pullImage">拉取镜像</button>
        </div>
        
        <div class="status-panel">
            <div class="status">状态: {{ pullStatus }}</div>
            <div class="progress">
                <div class="progress-bar" :style="{ width: pullProgress + '%' }"></div>
                <span>{{ pullProgress }}%</span>
            </div>
            <div v-if="pullError" class="error">{{ pullError }}</div>
        </div>
    </div>
</template>

<style scoped>
.docker-image {
    padding: 20px;
}

.operation-panel {
    margin-bottom: 20px;
}

.status-panel {
    border: 1px solid #eee;
    padding: 15px;
    border-radius: 4px;
}

.progress {
    margin: 10px 0;
    background: #f5f5f5;
    height: 20px;
    border-radius: 10px;
    overflow: hidden;
    position: relative;
}

.progress-bar {
    background: #1890ff;
    height: 100%;
    transition: width 0.3s;
}

.error {
    color: #ff4d4f;
    margin-top: 10px;
}
</style> 