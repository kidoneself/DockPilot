<!-- 容器管理页面 -->
<template>
  <div class="container">
    <t-card>
      <!-- 页面头部，包含标题和刷新按钮 -->
      <div class="header">
        <div class="title">容器列表</div>
        <t-button @click="handleRefresh" :loading="isLoading">
          <template #icon>
            <t-icon name="refresh" />
          </template>
          刷新列表
        </t-button>
      </div>
      <!-- 容器列表 -->
      <t-list :split="true" :loading="isLoading">
        <!-- 遍历显示每个容器 -->
        <t-list-item v-for="container in containers" :key="container.id">
          <div class="container-item">
            <!-- 容器基本信息 -->
            <t-list-item-meta
              :title="container.names?.[0]?.replace('/', '') || '未设置'"
              :description="container.image || '未设置'"
            >
              <!-- 容器图标/首字母 -->
              <template #image>
                <div class="container-logo-wrapper">
                  <!-- 如果有容器logo则显示logo -->
                  <img
                    v-if="getContainerLogo(container.image, container.id, showInitial)"
                    :src="getContainerLogo(container.image, container.id, showInitial)"
                    :alt="container.image"
                    class="container-logo"
                    @error="handleImageError(container.id)"
                  />
                  <!-- 如果没有logo则显示首字母 -->
                  <div
                    v-else
                    class="container-initial"
                    :style="{ backgroundColor: getStatusColor(container.state as ContainerState) }"
                  >
                    {{ getContainerInitial(container.names?.[0]) }}
                  </div>
                </div>
              </template>
            </t-list-item-meta>
            <!-- 容器操作按钮组 -->
            <t-space>
              <!-- 容器状态标签 -->
              <t-tag :theme="getStatusTheme(container.state as ContainerState)" variant="light">
                <template #icon>
                  <t-icon :name="getStatusIcon(container.state as ContainerState)" />
                </template>
                {{ getStatusText(container.state as ContainerState) }}
              </t-tag>
              <!-- 启动/停止按钮 -->
              <t-button
                :theme="container.state === 'running' ? 'warning' : 'success'"
                :loading="isContainerOperating(container.id)"
                :disabled="isContainerOperating(container.id)"
                @click="container.state === 'running' ? handleStop(container) : handleStart(container)"
              >
                <template #icon>
                  <t-icon :name="container.state === 'running' ? 'stop' : 'play'" />
                </template>
                {{ container.state === 'running' ? '停止' : '启动' }}
              </t-button>
              <!-- 重启按钮 -->
              <t-button
                theme="primary"
                :loading="isContainerOperating(container.id)"
                :disabled="isContainerOperating(container.id)"
                @click="handleRestart(container)"
              >
                <template #icon>
                  <t-icon name="refresh" />
                </template>
                重启
              </t-button>
              <!-- 删除按钮 -->
              <t-button theme="danger" :disabled="container.state === 'running'" @click="handleDelete(container)">
                <template #icon>
                  <t-icon name="delete" />
                </template>
                删除
              </t-button>
              <!-- 查看日志按钮 -->
              <t-button theme="default" @click="handleShowLogs(container)">
                <template #icon>
                  <t-icon name="file" />
                </template>
                日志
              </t-button>
              <!-- 详情按钮 -->
              <t-button theme="default" @click="handleShowDetails(container)">
                <template #icon>
                  <t-icon name="info-circle" />
                </template>
                详情
              </t-button>
            </t-space>
          </div>
        </t-list-item>
      </t-list>
    </t-card>

    <!-- 日志查看对话框 -->
    <t-dialog
      v-model:visible="logsDialogVisible"
      :header="'容器日志 - ' + currentLogsContainer?.names?.[0]?.replace('/', '')"
      :footer="false"
      width="800px"
      class="logs-dialog"
      :close-on-overlay-click="false"
      @close="handleCloseLogs"
    >
      <div class="logs-content">
        <!-- 日志工具栏 -->
        <div class="logs-toolbar">
          <t-space>
            <t-switch #label v-model="logsFollow">实时跟踪</t-switch>
          </t-space>
          <t-button theme="primary" @click="refreshLogs" :loading="isLogsLoading">
            <template #icon>
              <t-icon name="refresh" />
            </template>
            刷新
          </t-button>
        </div>
        <!-- 日志内容显示区域 -->
        <div class="log-content" ref="logsTextRef">
          <div v-for="(log, index) in parsedLogs" :key="index" class="log-item">
            <span class="log-message" v-html="log.content"></span>
          </div>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
// 导入 TDesign 组件库的消息提示组件
import { MessagePlugin } from 'tdesign-vue-next';
// 导入 Vue 相关功能
import { nextTick, onMounted, ref, watch } from 'vue';
// 导入路由相关功能
import { useRouter } from 'vue-router';
// 导入容器相关的 API 和类型
import {
  Container,
  deleteContainer,
  getContainerList,
  getContainerLogs,
  restartContainer,
  startContainer,
  stopContainer,
} from '@/api/container';
// 导入容器 logo 获取函数
import { getContainerLogo } from '@/constants/container-logos';
// 导入容器状态相关的工具函数
import {
  ContainerOperationState,
  ContainerState,
  getStatusIcon,
  getStatusText,
  getStatusTheme,
  handleContainerOperation,
} from './utils';

// 获取路由实例
const router = useRouter();

// 响应式数据定义
const containers = ref<Container[]>([]); // 容器列表
const operatingContainers = ref<ContainerOperationState>({
  starting: new Set(), // 正在启动的容器
  stopping: new Set(), // 正在停止的容器
  restarting: new Set(), // 正在重启的容器
});
const showInitial = ref<Set<string>>(new Set()); // 显示首字母的容器ID集合
const isLoading = ref(false); // 列表加载状态

// 日志相关状态
const logsDialogVisible = ref(false); // 日志对话框显示状态
const currentLogsContainer = ref<Container | null>(null); // 当前查看日志的容器
const logsContent = ref(''); // 日志内容
const logsFollow = ref(true); // 是否实时跟踪，默认开启
const logsTextRef = ref<HTMLElement | null>(null); // 日志文本区域引用
const logsPollingTimer = ref<number | null>(null); // 轮询定时器
const isLogsLoading = ref(false); // 是否正在请求日志

// 解析日志内容
const parsedLogs = ref<Array<{ content: string; type: string }>>([]);

/**
 * 检查容器是否正在操作
 * @param containerId 容器ID
 * @returns 是否正在操作
 */
const isContainerOperating = (containerId: string) => {
  return Object.values(operatingContainers.value).some((set) => set.has(containerId));
};

/**
 * 解析 ANSI 颜色代码
 * @param text 日志文本
 */
const parseAnsiColors = (text: string) => {
  const lines = text.split('\n');
  parsedLogs.value = lines.map((line) => {
    // 移除时间戳
    const timeMatch = line.match(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{3,9}Z\s*/);
    let content = timeMatch ? line.slice(timeMatch[0].length) : line;

    // 解析 ANSI 颜色代码
    const ansiRegex = /\x1b\[([0-9;]+)m/g;
    let match;
    let lastIndex = 0;
    let formattedContent = '';
    let currentColor = '';

    while ((match = ansiRegex.exec(content)) !== null) {
      // 添加颜色代码前的文本
      formattedContent += content.slice(lastIndex, match.index);

      // 解析颜色代码
      const codes = match[0].slice(2, -1).split(';');
      const color = codes.reduce((acc, code) => {
        const colorMap: Record<string, string> = {
          '30': 'black',
          '31': 'red',
          '32': 'green',
          '33': 'yellow',
          '34': 'blue',
          '35': 'magenta',
          '36': 'cyan',
          '37': 'white',
          '90': 'gray',
          '91': 'light-red',
          '92': 'light-green',
          '93': 'light-yellow',
          '94': 'light-blue',
          '95': 'light-magenta',
          '96': 'light-cyan',
          '97': 'light-white',
        };
        return colorMap[code] || acc;
      }, '');

      if (color) {
        currentColor = color;
        formattedContent += `<span class="ansi-color-${color}">`;
      } else if (codes[0] === '0') {
        if (currentColor) {
          formattedContent += '</span>';
          currentColor = '';
        }
      }

      lastIndex = match.index + match[0].length;
    }

    // 添加剩余的文本
    formattedContent += content.slice(lastIndex);
    if (currentColor) {
      formattedContent += '</span>';
    }

    // 解析日志类型
    let type = 'info';
    if (content.includes('error') || content.includes('Error')) {
      type = 'error';
    } else if (content.includes('warning') || content.includes('Warning')) {
      type = 'warning';
    } else if (content.includes('success') || content.includes('Success')) {
      type = 'success';
    }

    return {
      content: formattedContent,
      type,
    };
  });
};

/**
 * 开始轮询日志
 */
const startLogsPolling = () => {
  if (logsPollingTimer.value) {
    clearInterval(logsPollingTimer.value);
  }
  if (logsFollow.value) {
    logsPollingTimer.value = window.setInterval(refreshLogs, 2000);
  }
};

/**
 * 停止轮询日志
 */
const stopLogsPolling = () => {
  if (logsPollingTimer.value) {
    clearInterval(logsPollingTimer.value);
    logsPollingTimer.value = null;
  }
};

/**
 * 关闭日志对话框
 */
const handleCloseLogs = () => {
  stopLogsPolling();
  logsFollow.value = false;
  isLogsLoading.value = false;
  currentLogsContainer.value = null;
  logsContent.value = '';
};

/**
 * 刷新日志内容
 */
const refreshLogs = async () => {
  if (!currentLogsContainer.value) return;

  try {
    isLogsLoading.value = true;
    const response = await getContainerLogs(currentLogsContainer.value.id, {
      tail: 100,
      follow: false,
      timestamps: true,
    });
    parseAnsiColors(response.data);

    nextTick(() => {
      const logsElement = logsTextRef.value;
      if (logsElement) {
        logsElement.scrollTop = logsElement.scrollHeight;
      }
    });
  } catch (error) {
    MessagePlugin.error('获取容器日志失败');
  } finally {
    isLogsLoading.value = false;
  }
};

/**
 * 显示容器日志
 * @param container 容器对象
 */
const handleShowLogs = async (container: Container) => {
  currentLogsContainer.value = container;
  logsDialogVisible.value = true;
  await refreshLogs();
  startLogsPolling(); // 自动开始轮询
};

/**
 * 显示容器详情
 * @param container 容器对象
 */
const handleShowDetails = (container: Container) => {
  router.push({
    path: '/docker/containers/detail',
    query: { id: container.id },
  });
};

/**
 * 获取容器列表
 */
const fetchContainers = async () => {
  try {
    isLoading.value = true;
    const response = await getContainerList();
    containers.value = response.map((container: Container) => ({
      ...container,
      names: container.names?.map((name: string) => name.replace('/', '')) || [],
    }));
  } catch (error) {
    await MessagePlugin.error('获取容器列表失败');
  } finally {
    isLoading.value = false;
  }
};

/**
 * 刷新容器列表
 */
const handleRefresh = () => {
  fetchContainers();
};

/**
 * 启动容器
 * @param container 容器对象
 */
const handleStart = async (container: Container) => {
  operatingContainers.value.starting.add(container.id);
  await handleContainerOperation(
    async () => {
      await startContainer(container.id);
      await fetchContainers();
    },
    '启动容器成功',
    '启动容器失败',
  );
  operatingContainers.value.starting.delete(container.id);
};

/**
 * 停止容器
 * @param container 容器对象
 */
const handleStop = async (container: Container) => {
  operatingContainers.value.stopping.add(container.id);
  await handleContainerOperation(
    async () => {
      await stopContainer(container.id);
      await fetchContainers();
    },
    '停止容器成功',
    '停止容器失败',
  );
  operatingContainers.value.stopping.delete(container.id);
};

/**
 * 重启容器
 * @param container 容器对象
 */
const handleRestart = async (container: Container) => {
  operatingContainers.value.restarting.add(container.id);
  await handleContainerOperation(
    async () => {
      await restartContainer(container.id);
      await fetchContainers();
    },
    '重启容器成功',
    '重启容器失败',
  );
  operatingContainers.value.restarting.delete(container.id);
};

/**
 * 删除容器
 * @param container 容器对象
 */
const handleDelete = async (container: Container) => {
  await handleContainerOperation(
    async () => {
      await deleteContainer(container.id);
      await fetchContainers();
    },
    '删除容器成功',
    '删除容器失败',
  );
};

/**
 * 获取容器名称的首字母
 * @param name 容器名称
 * @returns 首字母
 */
const getContainerInitial = (name: string) => {
  if (!name) return '?';
  return name.charAt(0).toUpperCase();
};

/**
 * 获取容器状态对应的颜色
 * @param state 容器状态
 * @returns 颜色值
 */
const getStatusColor = (state: ContainerState) => {
  switch (state) {
    case 'running':
      return 'var(--td-success-color)';
    case 'exited':
      return 'var(--td-error-color)';
    case 'created':
      return 'var(--td-warning-color)';
    default:
      return 'var(--td-text-color-disabled)';
  }
};

/**
 * 处理图片加载错误
 * @param containerId 容器ID
 */
const handleImageError = (containerId: string) => {
  showInitial.value.add(containerId);
};

// 监听实时跟踪状态变化
watch(logsFollow, (newVal) => {
  if (newVal) {
    startLogsPolling();
  } else {
    stopLogsPolling();
  }
});

// 监听对话框关闭
watch(logsDialogVisible, (newVal) => {
  if (!newVal) {
    handleCloseLogs();
  }
});

// 组件挂载时获取容器列表
onMounted(() => {
  fetchContainers();
});

/**
 * 格式化日期
 * @param timestamp 时间戳
 * @returns 格式化后的日期字符串
 */
const formatDate = (timestamp: number) => {
  return new Date(timestamp * 1000).toLocaleString();
};

/**
 * 格式化文件大小
 * @param size 文件大小（字节）
 * @returns 格式化后的大小字符串
 */
const formatSize = (size: number) => {
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = size;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex++;
  }
  return `${value.toFixed(2)} ${units[unitIndex]}`;
};
</script>

<style scoped>
/* 主容器样式 */
.container {
  padding: 20px;
}

/* 页面头部样式 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

/* 标题样式 */
.title {
  font-size: 20px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

/* 容器列表项样式 */
.container-item {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 20px;
  padding: 0 20px;
  flex-wrap: wrap;
}

/* 容器信息区域样式 */
:deep(.t-list-item__meta) {
  flex: 1;
  min-width: 200px;
  max-width: calc(100% - 420px);
}

/* 容器操作按钮区域样式 */
.container-actions {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .container-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  :deep(.t-list-item__meta) {
    max-width: 100%;
  }

  :deep(.t-space) {
    width: 100%;
    justify-content: flex-start;
  }
}

/* 日志对话框样式 */
.logs-dialog {
  max-height: 80vh;
}

/* 日志内容区域样式 */
.logs-content {
  display: flex;
  flex-direction: column;
  height: 60vh;
}

/* 日志工具栏样式 */
.logs-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

/* 轮询状态指示器样式 */
.polling-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--td-text-color-secondary);
  font-size: 14px;
}

.loading-icon {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 日志内容区域样式 */
.log-content {
  max-height: 500px;
  overflow: auto;
  padding: 16px;
  background-color: var(--td-bg-color-secondarycontainer);
  font-family: monospace;
}

/* 日志项样式 */
.log-item {
  display: flex;
  align-items: flex-start;
  padding: 4px 0;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre;
}

/* 日志消息样式 */
.log-message {
  flex: 1;
  white-space: pre;
  min-width: 0;
}

/* ANSI 颜色样式 */
:deep(.ansi-color-black) {
  color: #000000;
}
:deep(.ansi-color-red) {
  color: #ff0000;
}
:deep(.ansi-color-green) {
  color: #00ff00;
}
:deep(.ansi-color-yellow) {
  color: #ffff00;
}
:deep(.ansi-color-blue) {
  color: #0000ff;
}
:deep(.ansi-color-magenta) {
  color: #ff00ff;
}
:deep(.ansi-color-cyan) {
  color: #00ffff;
}
:deep(.ansi-color-white) {
  color: #ffffff;
}
:deep(.ansi-color-gray) {
  color: #808080;
}
:deep(.ansi-color-light-red) {
  color: #ff8080;
}
:deep(.ansi-color-light-green) {
  color: #80ff80;
}
:deep(.ansi-color-light-yellow) {
  color: #ffff80;
}
:deep(.ansi-color-light-blue) {
  color: #8080ff;
}
:deep(.ansi-color-light-magenta) {
  color: #ff80ff;
}
:deep(.ansi-color-light-cyan) {
  color: #80ffff;
}
:deep(.ansi-color-light-white) {
  color: #ffffff;
}

/* 自定义滚动条样式 */
.log-content::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.log-content::-webkit-scrollbar-thumb {
  background-color: var(--td-text-color-disabled);
  border-radius: 3px;
}

.log-content::-webkit-scrollbar-track {
  background-color: transparent;
}

/* 容器首字母样式 */
.container-initial {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 20px;
  border-radius: 8px;
}

/* 容器 logo 包装器样式 */
.container-logo-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--td-bg-color-container);
}

/* 容器 logo 样式 */
.container-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 列表项头像样式 */
:deep(.t-list-item__meta-avatar) {
  width: 48px;
  height: 48px;
}
</style>
