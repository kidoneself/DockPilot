<template>
  <div class="container-detail">
    <!-- 顶部标题栏 -->
    <div class="header">
      <div class="title-section">
        <div class="container-title">
          <t-icon name="server" size="24px" />
          <span class="container-name">{{ containerDetail?.containerName || '-' }}</span>
          <t-tag :theme="getStatusTheme(containerDetail?.status)" class="status-tag">
            {{ getStatusText(containerDetail?.status) }}
          </t-tag>
        </div>
      </div>
      <div class="action-buttons">
      <t-space>
          <t-button theme="primary" @click="handleStart" :disabled="containerDetail?.status === 'running'">
            <template #icon><t-icon name="play-circle" /></template>
            启动
          </t-button>
          <t-button theme="warning" @click="handleStop" :disabled="containerDetail?.status !== 'running'">
            <template #icon><t-icon name="stop-circle" /></template>
            停止
          </t-button>
          <t-button theme="default" @click="handleRestart" :disabled="containerDetail?.status !== 'running'">
            <template #icon><t-icon name="refresh" /></template>
            重启
          </t-button>
          <t-button theme="danger" @click="handleDelete">
            <template #icon><t-icon name="delete" /></template>
            删除
          </t-button>
          <t-button theme="default" @click="handleEdit">
            <template #icon><t-icon name="edit" /></template>
            编辑
          </t-button>
      </t-space>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="main-content">
      <!-- 左侧基础信息 -->
      <div class="basic-info-card">
        <t-card title="基础信息" class="info-card" hover-shadow>
          <template #header>
            <div class="card-header">
              <t-icon name="info-circle" size="20px" />
              <span>基础信息</span>
            </div>
          </template>
          <div class="info-list">
            <div class="info-item">
              <div class="info-label">容器ID</div>
              <div class="info-value">{{ containerDetail?.containerId || '-' }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">容器名</div>
              <div class="info-value">{{ containerDetail?.containerName || '-' }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">镜像名</div>
              <div class="info-value">{{ containerDetail?.imageName || '-' }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">创建时间</div>
              <div class="info-value">{{ formatDate(containerDetail?.createdTime) }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">运行时间</div>
              <div class="info-value">{{ getRunningTime(containerDetail?.createdTime) }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">重启次数</div>
              <div class="info-value">{{ containerDetail?.restartCount || 0 }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">所属网络</div>
              <div class="info-value">{{ containerDetail?.networkMode || '-' }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">所属卷</div>
              <div class="info-value">{{ containerDetail?.volumes?.length || 0 }} 个</div>
            </div>
          </div>
      </t-card>
      </div>

      <!-- 右侧功能区 -->
      <div class="feature-area">
        <t-tabs v-model="activeTab" class="feature-tabs">
          <t-tab-panel value="monitor" label="资源监控">
            <t-card class="feature-card" hover-shadow>
              <template #header>
                <div class="card-header">
                  <t-icon name="chart" size="20px" />
                  <span>资源监控</span>
                  <t-tag :theme="resourceStats?.running ? 'success' : 'danger'" class="status-tag">
                    {{ resourceStats?.running ? '运行中' : '已停止' }}
                  </t-tag>
                </div>
          </template>
              <div class="monitor-content">
                <div v-if="resourceStats" class="monitor-container">
                  <div class="stats-grid">
                    <div class="stats-card">
                      <div class="stats-icon">
                        <t-icon name="cpu" size="24px" />
      </div>
                      <div class="stats-info">
                        <div class="stats-label">CPU 使用率</div>
                        <div class="stats-value">{{ formatPercent(resourceStats.cpuPercent) }}%</div>
                        <t-progress
                          :percentage="Number(formatPercent(resourceStats.cpuPercent))"
                          color="#108ee9"
                          track-color="#e8e8e8"
                        />
                      </div>
                    </div>

                    <div class="stats-card">
                      <div class="stats-icon">
                        <t-icon name="data-base" size="24px" />
      </div>
                      <div class="stats-info">
                        <div class="stats-label">内存使用</div>
                        <div class="stats-value">{{ formatBytes(resourceStats.memoryUsage) }} / {{ formatBytes(resourceStats.memoryLimit) }}</div>
                        <t-progress
                          :percentage="Number(formatPercent((resourceStats.memoryUsage / resourceStats.memoryLimit) * 100))"
                          color="#108ee9"
                          track-color="#e8e8e8"
                        />
            </div>
                    </div>

                    <div class="stats-card">
                      <div class="stats-icon">
                        <t-icon name="download" size="24px" />
      </div>
                      <div class="stats-info">
                        <div class="stats-label">网络接收</div>
                        <div class="stats-value">{{ formatBytes(resourceStats.networkRx) }}</div>
                        <div class="stats-trend">
                          <t-icon name="trending-up" size="16px" />
                          <span>实时流量</span>
                        </div>
                      </div>
                    </div>

                    <div class="stats-card">
                      <div class="stats-icon">
                        <t-icon name="upload" size="24px" />
                      </div>
                      <div class="stats-info">
                        <div class="stats-label">网络发送</div>
                        <div class="stats-value">{{ formatBytes(resourceStats.networkTx) }}</div>
                        <div class="stats-trend">
                          <t-icon name="trending-up" size="16px" />
                          <span>实时流量</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="monitor-placeholder">加载中...</div>
              </div>
            </t-card>
          </t-tab-panel>
          <t-tab-panel value="config" label="配置信息">
            <t-card class="feature-card" hover-shadow>
              <template #header>
                <div class="card-header">
                  <t-icon name="setting" size="20px" />
                  <span>配置信息</span>
                </div>
              </template>
              <t-collapse>
                <t-collapse-panel header="端口映射" value="ports">
                  <t-table 
                    :columns="portColumns" 
                    :data="portMappings" 
                    row-key="key"
                    hover
                    stripe
                  />
                </t-collapse-panel>
                <t-collapse-panel header="环境变量" value="env">
                  <t-table 
                    :columns="envColumns" 
                    :data="environmentVariables" 
                    row-key="id"
                    hover
                    stripe
                  />
                </t-collapse-panel>
                <t-collapse-panel header="命令/Entrypoint" value="cmd">
                  <div class="cmd-info">
                    <div class="cmd-item">
                      <div class="cmd-label">Entrypoint</div>
                      <div class="cmd-value">{{ (containerDetail?.entrypoints || []).join(' ') || '-' }}</div>
                    </div>
                    <div class="cmd-item">
                      <div class="cmd-label">Command</div>
                      <div class="cmd-value">{{ containerDetail?.command || '-' }}</div>
                    </div>
                  </div>
                </t-collapse-panel>
                <t-collapse-panel header="挂载卷" value="volumes">
                  <t-table 
                    :data="volumeMappings" 
                    :columns="volumeColumns" 
                    row-key="id"
                    hover
                    stripe
                  />
                </t-collapse-panel>
              </t-collapse>
            </t-card>
          </t-tab-panel>
          <t-tab-panel value="logs" label="日志查看器">
            <t-card class="feature-card" hover-shadow>
              <template #header>
                <div class="card-header">
                  <t-icon name="file" size="20px" />
                  <span>日志查看器</span>
                  <div class="log-actions">
                    <t-switch v-model="isAutoScroll" />
                    <t-button theme="default" variant="text" @click="fetchLogs">
                      <template #icon><t-icon name="refresh" /></template>
                      刷新
                </t-button>
            </div>
      </div>
              </template>
              <div class="logs-wrapper">
                <div class="logs-content" ref="logsContainer">
                  <div v-if="logs.length === 0" class="logs-empty">
                    暂无日志
            </div>
                  <div v-else class="logs-list">
                    <div v-for="(log, index) in logs" :key="index" class="log-line">
                      <span :class="log.type">{{ (log.content || '').split(':')[0] }}:</span>
                      <span>{{ (log.content || '').split(':').slice(1).join(':') }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </t-card>
          </t-tab-panel>
          <t-tab-panel value="network" label="网络与挂载">
            <t-card class="feature-card" hover-shadow>
              <template #header>
                <div class="card-header">
                  <t-icon name="network" size="20px" />
                  <span>网络与挂载</span>
                </div>
          </template>
              <div class="network-content">
                <div class="network-info">
                  <div class="info-item">
                    <span class="info-label">IP地址：</span>
                    <span class="info-value">{{ containerDetail?.ipAddress || '-' }}</span>
      </div>
                  <div class="info-item">
                    <span class="info-label">网络模式：</span>
                    <span class="info-value">{{ containerDetail?.networkMode || '-' }}</span>
                  </div>
                </div>
                <div class="volume-list">
                  <!-- <div class="sub-title">Volume列表</div> -->
                  <t-table 
                    :data="volumeMappings" 
                    :columns="volumeColumns" 
                    row-key="id"
                    hover
                    stripe
                  />
            </div>
                <a-table
                  :columns="networkColumns"
                  :data-source="networkSettings"
                  :pagination="false"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'network'">
                      {{ record.network }}
              </template>
                    <template v-if="column.dataIndex === 'ipAddress'">
                      {{ record.ipAddress }}
          </template>
                    <template v-if="column.dataIndex === 'gateway'">
                      {{ record.gateway }}
                  </template>
                  </template>
                </a-table>
            </div>
            </t-card>
          </t-tab-panel>
          <t-tab-panel value="advanced" label="高级设置">
            <t-card class="feature-card" hover-shadow>
              <template #header>
                <div class="card-header">
                  <t-icon name="setting" size="20px" />
                  <span>高级设置</span>
                </div>
              </template>
              <div class="advanced-content">
                <div class="network-info">
                  <div class="info-item">
                    <span class="info-label">重启策略：</span>
                    <span class="info-value">{{ containerDetail?.restartPolicyName || '-' }}</span>
          </div>
                  <div class="info-item">
                    <span class="info-label">最大重启次数：</span>
                    <span class="info-value">{{ containerDetail?.restartPolicyMaxRetry || 0 }}</span>
      </div>
                  <div class="info-item">
                    <span class="info-label">当前重启次数：</span>
                    <span class="info-value">{{ containerDetail?.restartCount || 0 }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">特权模式：</span>
                    <span class="info-value">{{ containerDetail?.privileged ? '是' : '否' }}</span>
                  </div>
                  <!-- <div class="info-item">
                    <span class="info-label">CapAdd：</span>
                    <span class="info-value">{{ containerDetail?.capAdd?.join(', ') || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">CapDrop：</span>
                    <span class="info-value">{{ containerDetail?.capDrop?.join(', ') || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">Labels：</span>
                    <div class="info-value">
                      <t-tag v-for="(value, key) in containerDetail?.labels" :key="key" class="label-tag">
                        {{ key }}: {{ value }}
                      </t-tag>
                    </div>
                  </div> -->
                </div>
              </div>
            </t-card>
          </t-tab-panel>
        </t-tabs>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'ContainerDetail',
};
</script>

<script lang="ts" setup>
import { computed, onMounted, ref, watch, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { MessagePlugin, DialogPlugin } from 'tdesign-vue-next';
import { getContainerDetail, getContainerLogs, getContainerStats, ContainerDetail, startContainer, stopContainer, restartContainer, deleteContainer } from '@/api/container';
import { IMAGE_OPTIONS, TYPE_OPTIONS, NETWORK_OPTIONS, RESTART_POLICY_OPTIONS, FORM_RULES } from '@/constants/container';
import type { ContainerForm, CreateContainerParams } from '@/types/container.d.ts';
import { mapFormDataToRequest, mapContainerDetailToForm } from '@/utils/container';

const route = useRoute();
const router = useRouter();
const containerDetail = ref<ContainerDetail | null>(null);
const loading = ref(false);
const activeTab = ref('monitor');

// 日志相关
const logs = ref<any[]>([]);
const isAutoScroll = ref(true);
const logsContainer = ref<HTMLElement | null>(null);
const logTimer = ref<number | null>(null);

// 资源监控相关
const resourceStats = ref<any>(null);
const statsTimer = ref<number | null>(null);

// 获取容器状态主题
const getStatusTheme = (state: string) => {
  switch (state) {
    case 'running':
      return 'success';
    case 'stopped':
      return 'danger';
    case 'exited':
      return 'warning';
    default:
      return 'default';
  }
};

// 获取容器状态文本
const getStatusText = (state: string) => {
  switch (state) {
    case 'running':
      return '运行中';
    case 'stopped':
      return '已停止';
    case 'exited':
      return '已退出';
    default:
      return '未知';
  }
};

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return '-';
  return new Date(date).toLocaleString();
};

// 获取运行时间
const getRunningTime = (startedAt: string) => {
  if (!startedAt) return '-';
  const start = new Date(startedAt).getTime();
  const now = new Date().getTime();
  const diff = now - start;
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  return `${days}天${hours}小时${minutes}分钟`;
};

// 存储配置表格列
const volumeColumns = [
  { colKey: 'hostPath', title: '主机路径' },
  { colKey: 'containerPath', title: '容器路径' },
  { colKey: 'readOnly', title: '权限' },
];

// 存储配置数据
const volumeMappings = computed(() => {
  if (!containerDetail.value?.volumes) return [];
  return containerDetail.value.volumes.map((volume: any) => ({
    id: `${volume.hostPath}:${volume.containerPath}`,
    hostPath: volume.hostPath,
    containerPath: volume.containerPath,
    readOnly: volume.readOnly ? '只读' : '读写'
  }));
});

// 端口映射表格列
const portColumns = [
  { colKey: 'containerPort', title: '容器端口' },
  { colKey: 'hostPort', title: '主机端口' },
  { colKey: 'protocol', title: '协议' },
];

// 端口映射数据
const portMappings = computed(() => {
  if (!containerDetail.value?.ports) return [];
  return containerDetail.value.ports.map((port: string) => {
    const [containerPort, hostPort] = port.split(':');
    const [portNumber, protocol] = containerPort.split('/');
    return {
      key: port,
      containerPort: portNumber,
      hostPort,
      protocol: protocol || 'tcp'
    };
  });
});

// 环境变量表格列
const envColumns = [
  { colKey: 'key', title: '变量名' },
  { colKey: 'value', title: '变量值' },
];

// 环境变量数据
const environmentVariables = computed(() => {
  if (!containerDetail.value?.envs) return [];
  return containerDetail.value.envs.map((env: string) => {
    const [key, value] = env.split('=');
    return {
      id: env,
      key,
      value
    };
  });
});

// 解析日志行
const parseLogLine = (line: string) => {
  // 移除 ANSI 转义序列
  const cleanLine = line.replace(/\x1b\[[0-9;]*m/g, '');
  
  // 判断日志类型并提取内容
  if (line.includes('[32mINFO')) {
    const content = cleanLine.replace('INFO:', '');
    return { type: 'info', content: `INFO:${content}` };
  } else if (line.includes('[31mERROR')) {
    const content = cleanLine.replace('ERROR:', '');
    return { type: 'error', content: `ERROR:${content}` };
  } else if (line.includes('[33mWARN')) {
    const content = cleanLine.replace('WARN:', '');
    return { type: 'warn', content: `WARN:${content}` };
  } else if (line.includes('[36mDEBUG')) {
    const content = cleanLine.replace('DEBUG:', '');
    return { type: 'debug', content: `DEBUG:${content}` };
  }
  
  return { type: 'default', content: cleanLine };
};

// 获取容器详情
const fetchContainerDetail = async () => {
  try {
    loading.value = true;
    const res = await getContainerDetail(route.query.id as string);
    console.log('获取到的容器详情响应:', res);
    if (res.code === 0) {
      containerDetail.value = res.data;
      console.log('设置后的容器详情:', containerDetail.value);
    } else {
      MessagePlugin.error('获取容器详情失败');
    }
  } catch (error) {
    console.error('获取容器详情失败:', error);
    MessagePlugin.error('获取容器详情失败');
  } finally {
    loading.value = false;
  }
};

// 容器操作
const handleStart = async () => {
  try {
    await startContainer(route.query.id as string);
    MessagePlugin.success('启动容器成功');
    await fetchContainerDetail();
  } catch (error) {
    MessagePlugin.error('启动容器失败');
  }
};

const handleStop = async () => {
  try {
    await stopContainer(route.query.id as string);
    MessagePlugin.success('停止容器成功');
    await fetchContainerDetail();
  } catch (error) {
    MessagePlugin.error('停止容器失败');
  }
};

const handleRestart = async () => {
  try {
    await restartContainer(route.query.id as string);
    MessagePlugin.success('重启容器成功');
    await fetchContainerDetail();
  } catch (error) {
    MessagePlugin.error('重启容器失败');
  }
};

const handleDelete = () => {
  const dialog = DialogPlugin.confirm({
    header: '确认删除',
    body: '确定要删除该容器吗？此操作不可恢复。',
    confirmBtn: {
      theme: 'danger',
      content: '删除',
    },
    cancelBtn: {
      theme: 'default',
      content: '取消',
    },
    onConfirm: async () => {
      try {
        await deleteContainer(route.query.id as string);
        MessagePlugin.success('删除容器成功');
        dialog.hide();
        router.push('/docker/containers');
      } catch (error) {
        MessagePlugin.error('删除容器失败');
      }
    },
  });
};

const handleEdit = () => {
  // 停止所有轮询
  stopStatsPolling();
  stopLogPolling();
  // 跳转到编辑页面
  router.push(`/docker/containers/edit?id=${route.query.id}`);
};

// 获取日志
const fetchLogs = async () => {
  try {
    const res = await getContainerLogs(route.query.id as string);
    if (res.data) {
      // 将日志按行分割并解析
      const newLogs = res.data.split('\n')
        .filter(Boolean)
        .map(parseLogLine);
      logs.value = newLogs;
      
      // 自动滚动到底部
      if (isAutoScroll.value && logsContainer.value) {
        setTimeout(() => {
          if (logsContainer.value) {
            logsContainer.value.scrollTop = logsContainer.value.scrollHeight;
          }
        }, 0);
      }
    }
  } catch (error) {
    console.error('获取日志失败:', error);
  }
};

// 监听标签页切换
watch(activeTab, (newTab) => {
  if (newTab === 'logs') {
    startLogPolling();
  } else {
    stopLogPolling();
  }
  
  if (newTab === 'monitor') {
    startStatsPolling();
  } else {
    stopStatsPolling();
  }
});

// 监听路由变化
watch(() => route.query.id, (newId) => {
  if (newId) {
    fetchContainerDetail();
    // 如果当前是资源监控标签页，则开始轮询
    if (activeTab.value === 'monitor') {
      startStatsPolling();
    }
  } else {
    stopStatsPolling();
  }
});

// 获取资源使用情况
const fetchResourceStats = async () => {
  try {
    const res = await getContainerStats(route.query.id as string);
    if (res.code === 0) {
      resourceStats.value = res.data;
    }
  } catch (error) {
    console.error('获取资源使用情况失败:', error);
  }
};

// 开始轮询资源使用情况
const startStatsPolling = () => {
  // 先停止之前的轮询
  stopStatsPolling();
  // 先获取一次数据
  fetchResourceStats();
  // 设置轮询
  statsTimer.value = window.setInterval(fetchResourceStats, 2000);
};

// 停止轮询资源使用情况
const stopStatsPolling = () => {
  if (statsTimer.value) {
    clearInterval(statsTimer.value);
    statsTimer.value = null;
  }
};

// 开始轮询日志
const startLogPolling = () => {
  // 先停止之前的轮询
  stopLogPolling();
  // 先获取一次日志
  fetchLogs();
  // 设置轮询
  logTimer.value = window.setInterval(fetchLogs, 2000);
};

// 停止轮询日志
const stopLogPolling = () => {
  if (logTimer.value) {
    clearInterval(logTimer.value);
    logTimer.value = null;
  }
};

// 组件卸载时清理
onUnmounted(() => {
  stopStatsPolling();
  stopLogPolling();
});

// 页面加载时获取容器详情和资源监控数据
onMounted(() => {
  if (route.query.id) {
    fetchContainerDetail();
    // 如果当前是资源监控标签页，则开始轮询
    if (activeTab.value === 'monitor') {
      startStatsPolling();
    }
  }
});

// 格式化字节数
const formatBytes = (bytes: number) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
};

// 格式化百分比
const formatPercent = (value: number) => {
  return value.toFixed(2);
};

// 网络设置表格列
const networkColumns = [
  { colKey: 'network', title: '网络' },
  { colKey: 'ipAddress', title: 'IP地址' },
  { colKey: 'gateway', title: '网关' },
];

// 网络设置数据
const networkSettings = computed(() => {
  if (!containerDetail.value?.networkSettings) return [];
  return [{
    id: 'default',
    network: containerDetail.value.networkMode || 'default',
    ipAddress: containerDetail.value.networkSettings.IPAddress || '-',
    gateway: containerDetail.value.networkSettings.Gateway || '-'
  }];
});

const containerInfo = computed(() => [
  { label: '容器ID', content: containerDetail.value?.containerId },
  { label: '容器名称', content: containerDetail.value?.containerName },
  { label: '镜像', content: containerDetail.value?.imageName },
  { label: '镜像ID', content: containerDetail.value?.imageId },
  { label: '命令', content: containerDetail.value?.command },
  { label: '状态', content: containerDetail.value?.status },
  { label: '创建时间', content: containerDetail.value?.createdTime },
  { label: '大小', content: containerDetail.value?.data?.sizeRw ? formatSize(containerDetail.value.data.sizeRw) : '-' },
  { label: '根文件系统大小', content: containerDetail.value?.data?.sizeRootFs ? formatSize(containerDetail.value.data.sizeRootFs) : '-' },
]);

const networkInfo = computed(() => [
  { label: '网络模式', content: containerDetail.value?.networkMode },
  { label: 'IP地址', content: containerDetail.value?.ipAddress },
  { label: '重启策略', content: containerDetail.value?.restartPolicyName },
]);

const mountColumns = [
  { colKey: 'Source', title: '源路径' },
  { colKey: 'Destination', title: '目标路径' },
  { 
    colKey: 'ReadOnly', 
    title: '只读',
    cell: ({ row }: { row: { ReadOnly: boolean } }) => row.ReadOnly ? '是' : '否'
  },
];

const labelColumns = [
  { colKey: 'key', title: '键' },
  { colKey: 'value', title: '值' },
];

const labelData = computed(() => {
  if (!containerDetail.value?.labels) return [];
  return Object.entries(containerDetail.value.labels).map(([key, value]) => ({
    key,
    value,
  }));
});

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

const INITIAL_DATA: ContainerForm = {
  image: '',
  tag: '',
  autoPull: false,
  name: '',
  autoRemove: false,
  restartPolicy: '',
  portMappings: [{ hostPort: '', containerPort: '', protocol: '', ip: '' }],
  networkMode: '',
  ipAddress: '',
  gateway: '',
  volumeMappings: [],
  devices: [],
  environmentVariables: [],
  privileged: false,
  capAdd: [],
  capDrop: [],
  memoryLimit: '',
  cpuLimit: '',
  entrypoint: [],
  cmd: [],
  workingDir: '',
  user: '',
  labels: [],
  healthcheck: {
    test: [],
    interval: '',
    timeout: '',
    retries: 0,
    startPeriod: '',
  },
};
</script>

<style lang="less" scoped>
.container-detail {
  padding: 20px;
  background-color: var(--td-bg-color-container);
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding: 20px;
  background-color: var(--td-bg-color-container);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  .title-section {
    .container-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;

      .container-name {
        font-size: 24px;
        font-weight: 600;
        color: var(--td-text-color-primary);
      }

      .status-tag {
        padding: 4px 8px;
        border-radius: 4px;
      }
    }
  }
}

.main-content {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 20px;
}

.basic-info-card {
  .info-card {
    height: 100%;
  }
}

.info-list {
  .info-item {
    padding: 12px 0;
    border-bottom: 1px solid var(--td-component-border);

    &:last-child {
      border-bottom: none;
    }

    .info-label {
      font-size: 14px;
      color: var(--td-text-color-secondary);
      margin-bottom: 4px;
    }

    .info-value {
      font-size: 16px;
      color: var(--td-text-color-primary);
      word-break: break-all;
    }
  }
}

.feature-area {
  .feature-tabs {
    :deep(.t-tabs__nav) {
      background-color: var(--td-bg-color-container);
      border-radius: 8px 8px 0 0;
      padding: 0 16px;
    }
  }

  .feature-card {
    border-radius: 0 0 8px 8px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  color: var(--td-text-color-primary);

  .status-tag {
    margin-left: 8px;
  }

  .log-actions {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-left: auto;
  }
}

.cmd-info {
  .cmd-item {
    margin-bottom: 16px;

    .cmd-label {
      font-size: 14px;
      color: var(--td-text-color-secondary);
      margin-bottom: 4px;
    }

    .cmd-value {
      font-family: monospace;
      background-color: var(--td-bg-color-container);
      padding: 8px;
      border-radius: 4px;
      word-break: break-all;
    }
  }
}

.monitor-content,
.logs-wrapper {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--td-bg-color-container);
  border-radius: 4px;
  color: var(--td-text-color-secondary);
}

.network-content {
  .network-info {
    background-color: var(--td-bg-color-container);
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    .info-item {
      display: flex;
      align-items: center;
      margin-bottom: 12px;

      &:last-child {
        margin-bottom: 0;
      }

      .info-label {
        font-size: 14px;
        color: var(--td-text-color-secondary);
        min-width: 80px;
      }

      .info-value {
        font-size: 14px;
        color: var(--td-text-color-primary);
        font-weight: 500;
      }
    }
  }

  .volume-list {
    .sub-title {
      font-size: 16px;
      font-weight: 500;
      margin-bottom: 16px;
      color: var(--td-text-color-primary);
    }
  }
}

.advanced-content {
  .network-info {
    .info-item {
      margin-bottom: 16px;

      .info-label {
        font-size: 14px;
        color: var(--td-text-color-secondary);
        margin-bottom: 4px;
      }

      .info-value {
        font-size: 16px;
        color: var(--td-text-color-primary);

        .label-tag {
          margin-right: 8px;
          margin-bottom: 8px;
        }
      }
    }
  }
}

.logs-wrapper {
  height: 600px;
  background-color: var(--td-bg-color-container);
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--td-component-border);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 40px;
    background: linear-gradient(to bottom, rgba(0, 0, 0, 0.02), transparent);
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 40px;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.02), transparent);
    pointer-events: none;
  }
}

.logs-content {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
  color: var(--td-text-color-primary);
  white-space: pre-wrap;
  word-break: break-all;
  background-color: var(--td-bg-color-container);
  position: relative;
  z-index: 1;

  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--td-component-border);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-track {
    background: var(--td-bg-color-container);
  }

  .logs-empty {
  display: flex;
  align-items: center;
    justify-content: center;
    height: 100%;
    color: var(--td-text-color-secondary);
  }

  .logs-list {
    .log-line {
      padding: 4px 8px;
      border-bottom: 1px solid var(--td-component-border);
      transition: background-color 0.2s ease;

      &:hover {
        background-color: var(--td-bg-color-container-hover);
      }

      &:last-child {
        border-bottom: none;
      }

      .info {
        color: #52c41a;
        font-weight: 500;
      }

      .error {
        color: #ff4d4f;
        font-weight: 500;
      }

      .warn {
        color: #faad14;
        font-weight: 500;
      }

      .debug {
        color: #8c8c8c;
        font-weight: 500;
      }
    }
  }
}

:deep(.t-table) {
  border-radius: 6px;
  overflow: hidden;
}

:deep(.t-tag) {
  padding: 4px 8px;
  border-radius: 4px;
}

.monitor-container {
  padding: 20px;

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;

    .stats-card {
      background-color: var(--td-bg-color-container);
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
      gap: 16px;

      .stats-icon {
        width: 48px;
        height: 48px;
        border-radius: 8px;
        background-color: var(--td-brand-color-1);
        display: flex;
  align-items: center;
        justify-content: center;
        color: var(--td-brand-color);

        .t-icon {
          color: var(--td-brand-color);
        }
      }

      .stats-info {
        flex: 1;

        .stats-label {
          font-size: 14px;
          color: var(--td-text-color-secondary);
          margin-bottom: 4px;
        }

        .stats-value {
          font-size: 24px;
  font-weight: 600;
  color: var(--td-text-color-primary);
          margin-bottom: 8px;
        }

        .stats-trend {
          display: flex;
          align-items: center;
          gap: 4px;
          color: var(--td-success-color);
          font-size: 12px;

          .t-icon {
            color: var(--td-success-color);
          }
        }
      }
    }
  }
}

.monitor-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--td-text-color-secondary);
}
</style>
