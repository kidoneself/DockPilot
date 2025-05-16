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
            <t-button v-if="showConfigButton" theme="default" size="small" @click="handleShowConfig(row)">
              <template #icon>
                <t-icon name="file-code" />
              </template>
              获取配置
            </t-button>
          </t-space>
        </template>
      </t-table>
    </t-card>

    <!-- 配置展示弹窗 -->
    <t-dialog v-model:visible="configDialogVisible" header="容器配置" :width="680" :footer="false">
      <div class="config-dialog-content">
        <div class="config-actions">
          <t-button theme="primary" size="small" @click="handleSaveConfig">
            <template #icon>
              <t-icon name="download" />
            </template>
            保存配置
          </t-button>
          <t-button theme="default" size="small" @click="handleCopyConfig">
            <template #icon>
              <t-icon name="file-copy" />
            </template>
            复制配置
          </t-button>
        </div>
        <t-textarea
          v-model="formattedConfig"
          :autosize="{ minRows: 15, maxRows: 20 }"
          readonly
          class="config-textarea"
        />
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
// 导入表格列类型
import type { PrimaryTableCol } from 'tdesign-vue-next';
// 导入 TDesign 组件库的消息提示组件
import { MessagePlugin } from 'tdesign-vue-next';
// 导入 Vue 相关功能
import { computed, onMounted, ref } from 'vue';
// 导入路由相关功能
import { useRoute, useRouter } from 'vue-router';
// 导入容器相关的 API 和类型
import {
  getContainerJsonConfig,
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
} from '@/pages/docker/containers/utils';
import { useNotificationStore } from '@/store/modules/notification';

// 获取路由实例
const router = useRouter();
const route = useRoute();

const notificationStore = useNotificationStore();
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

// 控制配置按钮显示
const showConfigButton = computed(() => route.query.config === 'true');

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

// 配置相关
const configDialogVisible = ref(false);
const formattedConfig = ref('');
const currentConfig = ref<any>(null);

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
    console.log('获取到的容器列表数据:', res);
    containers.value = res || [];

    // 发送普通通知
    notificationStore.addNotification({
      id: String(Date.now()),
      content: '操作成功！',
      type: '成功',
      status: true,
      collected: false,
      date: new Date().toLocaleString(),
      quality: 'normal',
    });

    // 处理WebSocket推送
    notificationStore.handleWebSocketNotification({
      content: '有新的消息啦！',
      type: 'WebSocket',
    });
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
 * 处理容器的启动/停止操作
 * 根据容器当前状态决定执行启动还是停止操作
 * 
 * @param container - 容器对象，包含容器的基本信息（ID、状态等）
 */
const handleStartStop = async (container: Container) => {
  // 判断容器当前是否处于运行状态
  // container.state === 'running' 表示容器正在运行
  const isRunning = container.state === 'running';

  // 根据容器状态选择要执行的操作函数
  // 如果容器正在运行，则选择 stopContainer 函数来停止容器
  // 如果容器未运行，则选择 startContainer 函数来启动容器
  const action = isRunning ? stopContainer : startContainer;

  // 根据容器状态选择操作类型
  // 用于在界面上显示当前正在执行的操作（启动中/停止中）
  const type = isRunning ? operatingContainers.value.stopping : operatingContainers.value.starting;

  // 根据容器状态选择成功提示消息
  // 操作成功后会显示对应的提示信息
  const successMsg = isRunning ? '停止成功' : '启动成功';

  try {
    // 执行容器操作
    // 1. 调用 handleContainerOperation 函数执行具体的启动/停止操作
    // 2. 传入操作函数、容器ID和操作类型
    await handleContainerOperation(() => action(container.id), container.id, type);
    MessagePlugin.success(successMsg);

    // 操作成功后，重新获取容器列表
    // 更新界面显示的容器状态
    await fetchContainers();

    // 显示操作成功的提示消息
  } catch (error) {
    // 捕获并处理操作过程中可能出现的错误
    // 如果错误是 Error 类型，显示其错误信息
    // 否则显示默认的错误提示
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

/**
 * 显示容器配置
 * @param container 容器对象
 */
const handleShowConfig = async (container: Container) => {
  try {
    const response = await getContainerJsonConfig(container.id);
    currentConfig.value = response.data;
    formattedConfig.value = JSON.stringify(response.data, null, 2);
    configDialogVisible.value = true;
  } catch (error) {
    MessagePlugin.error(error instanceof Error ? error.message : '获取配置失败');
  }
};

/**
 * 保存配置
 */
const handleSaveConfig = () => {
  if (!currentConfig.value) return;

  try {
    const containerName = currentConfig.value.name.replace(/[^a-zA-Z0-9-_]/g, '_');
    const fileName = `container-config-${containerName}.json`;

    const blob = new Blob([formattedConfig.value], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);

    MessagePlugin.success('配置保存成功');
  } catch (error) {
    MessagePlugin.error('保存配置失败');
  }
};

/**
 * 复制配置
 */
const handleCopyConfig = () => {
  if (!formattedConfig.value) return;

  navigator.clipboard
    .writeText(formattedConfig.value)
    .then(() => {
      MessagePlugin.success('配置已复制到剪贴板');
    })
    .catch(() => {
      MessagePlugin.error('复制失败');
    });
};

// 组件挂载时获取容器列表
onMounted(() => {
  fetchContainers();
});
</script>

<style scoped>
.container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

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

.config-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-actions {
  display: flex;
  gap: 8px;
}

.config-textarea {
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
  background-color: var(--td-bg-color-container);
  border: 1px solid var(--td-component-border);
  border-radius: var(--td-radius-default);
  padding: 8px;
}
</style>
