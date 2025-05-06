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
      <!-- 容器表格 -->
      <t-table
        :data="containers"
        :loading="isLoading"
        :columns="columns"
        row-key="id"
        hover
        v-model:selectedRowKeys="selectedRowKeys"
      >
        <!-- 容器名称列 -->
        <template #name="{ row }">
          <t-space>
            <div class="container-logo-wrapper">
              <div class="container-initial" :style="{ backgroundColor: getStatusColor(row.state as ContainerState) }">
                {{ getContainerInitial(row.names?.[0]) }}
              </div>
            </div>
            <div class="container-info">
              <div class="container-name">{{ row.names?.[0]?.replace('/', '') || '未设置' }}</div>
              <div class="container-image">{{ row.image || '未设置' }}</div>
            </div>
          </t-space>
        </template>

        <!-- 状态列 -->
        <template #state="{ row }">
          <t-tag :theme="getStatusTheme(row.state as ContainerState)" variant="light">
            <template #icon>
              <t-icon :name="getStatusIcon(row.state as ContainerState)" />
            </template>
            {{ getStatusText(row.state as ContainerState) }}
          </t-tag>
        </template>

        <!-- 更新状态列 -->
        <template #updateStatus="{ row }">
          <t-tag v-if="row.needUpdate" theme="warning" variant="light">
            <template #icon>
              <t-icon name="info-circle-filled" />
            </template>
            需要更新
          </t-tag>
          <t-tag v-else theme="success" variant="light">
            <template #icon>
              <t-icon name="check-circle-filled" />
            </template>
            最新
          </t-tag>
        </template>

        <!-- 操作列 -->
        <template #op="{ row }">
          <t-space>
            <t-popconfirm :content="getStartStopConfirmMessage(row)" @confirm="handleStartStop(row)">
              <t-button
                :theme="row.state === 'running' ? 'warning' : 'success'"
                size="small"
                :loading="isContainerOperating(row.id)"
                :disabled="isContainerOperating(row.id)"
              >
                <template #icon>
                  <t-icon :name="row.state === 'running' ? 'stop' : 'play'" />
                </template>
                {{ row.state === 'running' ? '停止' : '启动' }}
              </t-button>
            </t-popconfirm>
            <t-popconfirm :content="getRestartConfirmMessage(row)" @confirm="handleRestartContainer(row)">
              <t-button
                theme="primary"
                size="small"
                :loading="isContainerOperating(row.id)"
                :disabled="isContainerOperating(row.id)"
              >
                <template #icon>
                  <t-icon name="refresh" />
                </template>
                重启
              </t-button>
            </t-popconfirm>
            <t-button theme="default" size="small" @click="handleShowDetails(row)">
              <template #icon>
                <t-icon name="info-circle" />
              </template>
              详情
            </t-button>
            <t-popconfirm v-if="row.needUpdate" :content="getUpdateConfirmMessage(row)" @confirm="handleUpdate(row)">
              <t-button
                theme="warning"
                size="small"
                :loading="isContainerOperating(row.id)"
                :disabled="isContainerOperating(row.id)"
              >
                <template #icon>
                  <t-icon name="download" />
                </template>
                更新
              </t-button>
            </t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </t-card>
  </div>
</template>

<script setup lang="ts">
// 导入表格列类型
import type { PrimaryTableCol } from 'tdesign-vue-next';
// 导入 TDesign 组件库的消息提示组件
import { MessagePlugin } from 'tdesign-vue-next';
// 导入 Vue 相关功能
import { onMounted, ref } from 'vue';
// 导入路由相关功能
import { useRouter } from 'vue-router';
// 导入容器相关的 API 和类型
import {
  getContainerList,
  restartContainer,
  startContainer,
  stopContainer,
  updateContainer,
} from '@/api/websocket/container';
import type { Container } from '@/api/model/containerModel';
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
const isLoading = ref(false); // 列表加载状态
// 定义选中行
const selectedRowKeys = ref<Array<string | number>>([]);

// 定义表格列
const columns = [
  {
    colKey: 'name',
    title: '容器信息',
    width: 300,
  },
  {
    colKey: 'state',
    title: '状态',
    width: 120,
  },
  {
    colKey: 'updateStatus',
    title: '更新状态',
    width: 120,
  },
  {
    colKey: 'op',
    title: '操作',
    width: 280,
    fixed: 'right' as const,
  },
] as PrimaryTableCol[];

/**
 * 检查容器是否正在操作
 * @param containerId 容器ID
 * @returns 是否正在操作
 */
const isContainerOperating = (containerId: string) => {
  return Object.values(operatingContainers.value).some((set) => set.has(containerId));
};

/**
 * 获取容器名称的首字母
 * @param name 容器名称
 * @returns 首字母
 */
const getContainerInitial = (name: string) => {
  if (!name) return '?';
  return name.charAt(1).toUpperCase();
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
    default:
      return 'var(--td-error-disabled)';
  }
};

/**
 * 获取容器列表
 */
const fetchContainers = async () => {
  isLoading.value = true;
  try {
    const res = await getContainerList();
    containers.value = res || [];
  } catch (error) {
    console.error('获取容器列表失败:', error);
    MessagePlugin.error('获取容器列表失败');
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
 * 处理启动/停止
 * @param container 容器对象
 */
const handleStartStop = async (container: Container) => {
  try {
    if (container.state === 'running') {
      await handleContainerOperation(
        () => stopContainer(container.id),
        container.id,
        operatingContainers.value.stopping,
      );
      await fetchContainers();
      MessagePlugin.success('停止成功');
    } else {
      await handleContainerOperation(
        () => startContainer(container.id),
        container.id,
        operatingContainers.value.starting,
      );
      await fetchContainers();
      MessagePlugin.success('启动成功');
    }
  } catch (error) {
    MessagePlugin.error(error instanceof Error ? error.message : '操作失败');
  }
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

const handleRestartContainer = async (container: Container) => {
  try {
    await handleContainerOperation(
      () => restartContainer(container.id),
      container.id,
      operatingContainers.value.restarting,
    );
    await fetchContainers();
    MessagePlugin.success('重启成功');
  } catch (error) {
    MessagePlugin.error(error instanceof Error ? error.message : '操作失败');
  }
};

const handleUpdate = async (container: any) => {
  try {
    //todo 实现容器更新，如果已经更新镜像了，就没有镜像名字了，得自己存
    await updateContainer(container.id, {});
    MessagePlugin.success('更新成功');
    fetchContainers();
  } catch (error) {
    console.error('更新容器失败:', error);
    MessagePlugin.error('更新容器失败');
  }
};

const getStartStopConfirmMessage = (container: Container) => {
  return `确认要${container.state === 'running' ? '停止' : '启动'}容器 ${container.names?.[0]?.replace('/', '')} 吗？`;
};

const getRestartConfirmMessage = (container: Container) => {
  return `确认要重启容器 ${container.names?.[0]?.replace('/', '')} 吗？`;
};

const getUpdateConfirmMessage = (container: Container) => {
  return `确认要更新容器 ${container.names?.[0]?.replace('/', '')} 吗？`;
};

// 组件挂载时获取容器列表
onMounted(() => {
  fetchContainers();
});
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

/* 容器信息样式 */
.container-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.container-name {
  font-weight: 500;
  color: var(--td-text-color-primary);
}

.container-image {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

/* 容器首字母样式 */
.container-initial {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 16px;
  border-radius: 4px;
}

/* 容器 logo 包装器样式 */
.container-logo-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 4px;
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

/* 列标题样式 */
.column-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: var(--td-text-color-primary);
  position: relative;
  padding-right: 20px;
}

.help-icon {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  color: var(--td-text-color-secondary);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 16px;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: var(--td-bg-color-container);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.help-icon:hover {
  color: var(--td-brand-color);
  background-color: var(--td-brand-color-light);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 提示框样式 */
:deep(.t-tooltip) {
  max-width: 300px;
}

:deep(.t-tooltip__content) {
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--td-text-color-primary);
  background-color: var(--td-bg-color-container);
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.t-tooltip__arrow) {
  border-color: var(--td-bg-color-container);
}

:deep(.t-tooltip__arrow::before) {
  background-color: var(--td-bg-color-container);
}
</style>
