<template>
  <div class="container">
    <div class="header">
      <div class="title">镜像列表</div>
      <div class="actions">
        <t-button theme="primary" @click="handlePull">
          <template #icon>
            <t-icon name="add" />
          </template>
          拉取镜像
        </t-button>
        <t-button theme="warning" @click="handleCheckUpdates">
          <template #icon>
            <t-icon name="swap" />
          </template>
          检查更新
        </t-button>
        <t-button @click="fetchImages">
          <template #icon>
            <t-icon name="refresh" />
          </template>
          刷新
        </t-button>
      </div>
    </div>

    <!-- 添加正在拉取的镜像列表 -->
    <div v-if="activePullTasks.length > 0" class="pulling-images-section">
      <div class="section-title">正在拉取的镜像</div>
      <t-table :data="activePullTasks" :columns="pullTaskColumns" row-key="taskId">
        <template #image="{ row }">
          <t-tag theme="primary">{{ row.image }}:{{ row.tag }}</t-tag>
        </template>
        <template #progress="{ row }">
          <div class="inline-progress">
            <t-progress :percentage="row.progress" :stroke-width="4" :show-label="false" />
            <span class="progress-text">{{ row.progress }}%</span>
          </div>
        </template>
        <template #status="{ row }">
          <t-tag :theme="getTaskStatusTheme(row.status)" variant="light">
            {{ getTaskStatusText(row.status) }}
          </t-tag>
        </template>
        <template #operation="{ row }">
          <t-space>
            <t-button size="small" @click="showPullTaskDetails(row)">查看详情</t-button>
            <t-button
              v-if="row.status !== 'success' && row.status !== 'error'"
              size="small"
              theme="danger"
              @click="handleCancelPullTask(row)"
              >取消
            </t-button>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-table :data="images" :columns="columns" :loading="loading" row-key="Id">
      <template #id="{ row }">
        <t-tag theme="default" variant="light">{{ row.Id.replace('sha256:', '').slice(0, 12) }}</t-tag>
      </template>
      <template #name="{ row }">
        <t-tag theme="primary" variant="light">{{ row.name }}</t-tag>
      </template>
      <template #tag="{ row }">
        <t-tag theme="warning" variant="light">{{ row.tag }}</t-tag>
      </template>
      <template #created="{ row }">
        <span>{{ formatDate(row.created) }}</span>
      </template>
      <template #needUpdate="{ row }">
        <t-tag :theme="row.needUpdate ? 'warning' : 'success'" variant="light">
          <template #icon>
            <t-icon :name="row.needUpdate ? 'time' : 'check-circle'" />
          </template>
          {{ row.needUpdate ? '需要更新' : '已是最新' }}
        </t-tag>
      </template>
      <template #lastChecked="{ row }">
        <span>{{ row.lastChecked ? formatDate(row.lastChecked) : '尚未检查' }}</span>
      </template>
      <template #operation="{ row }">
        <t-space size="small">
          <t-button size="small" theme="primary" @click="handleRun(row)">
            <template #icon>
              <t-icon name="play" />
            </template>
            创建
          </t-button>
          <t-button v-if="row.needUpdate" size="small" theme="warning" @click="handleUpdate(row)">
            <template #icon>
              <t-icon name="refresh" />
            </template>
            更新
          </t-button>
          <t-button size="small" theme="danger" @click="handleDelete(row)">
            <template #icon>
              <t-icon name="delete" />
            </template>
            删除
          </t-button>
        </t-space>
      </template>
    </t-table>

    <!-- 拉取镜像对话框 -->
    <t-dialog
      v-model:visible="pullImageDialogVisible"
      header="拉取镜像"
      :on-confirm="onPullImageConfirm"
      :close-on-overlay-click="false"
      :footer="!isPulling"
      width="800px"
    >
      <t-form
        v-if="!isPulling"
        ref="pullImageForm"
        :data="pullImageFormData"
        :rules="pullImageFormRules"
        label-width="100px"
      >
        <t-form-item label="镜像地址" name="image">
          <t-input v-model="pullImageFormData.image" placeholder="请输入镜像地址，例如：nginx" />
        </t-form-item>
        <t-form-item label="版本" name="tag">
          <t-input v-model="pullImageFormData.tag" placeholder="请输入版本，例如：latest" />
        </t-form-item>

      </t-form>
      <div v-else class="pull-progress-container">
        <div class="space-y-4">
          <t-progress
            :percentage="pullProgress"
            theme="plump"
            label="inside"
          />
          
          <div class="text-sm text-gray-600">
            当前阶段：{{ currentStage }}
          </div>

          <t-scrollbar style="height: 120px;">
            <div class="text-xs text-gray-400">
              <div v-for="(log, index) in logLines" :key="index" class="log-line">
                {{ log }}
              </div>
            </div>
          </t-scrollbar>

          <div class="text-right mt-4">
            <t-button
              v-if="!completed"
              theme="default"
              variant="outline"
              @click="handleCancelPull"
            >
              取消拉取
            </t-button>
            <t-button
              v-else
              theme="primary"
              @click="closePullDialog"
            >
              关闭
            </t-button>
          </div>
        </div>
      </div>
    </t-dialog>

    <!-- 拉取任务详情对话框 -->
    <t-dialog
      v-model:visible="pullTaskDetailsVisible"
      header="拉取任务详情"
      :close-on-overlay-click="true"
      width="800px"
    >
      <div v-if="currentTaskDetails" class="pull-progress-container">
        <div class="progress-header">
          <span class="image-title">镜像: {{ currentTaskDetails.image }}:{{ currentTaskDetails.tag }}</span>
          <span class="progress-percentage">{{ currentTaskDetails.progress }}%</span>
        </div>
        <t-progress
          :percentage="currentTaskDetails.progress"
          :status="getProgressStatus(currentTaskDetails.status)"
          :stroke-width="12"
        />
        <div class="progress-info">
          <div v-if="currentTaskDetails.layers && currentTaskDetails.layers.length > 0" class="layers-info">
            <div
              v-for="(layer, index) in currentTaskDetails.layers"
              :key="index"
              class="layer-item"
              :class="{
                'layer-completed': isLayerCompleted(layer),
                'layer-in-progress': !isLayerCompleted(layer),
              }"
            >
              <div class="layer-header">
                <span class="layer-id">{{ layer.id?.slice(0, 12) }}</span>
                <t-progress
                  v-if="getLayerProgress(layer) > 0"
                  theme="plump"
                  :percentage="getLayerProgress(layer)"
                  :stroke-width="6"
                  :show-label="false"
                  class="layer-progress-bar"
                />
              </div>
              <div class="layer-content">
                <span class="layer-status">{{ layer.status }}</span>
                <span v-if="layer.progress" class="layer-progress">{{ formatLayerProgress(layer.progress) }}</span>
              </div>
            </div>
          </div>
          <div v-else class="progress-message">
            {{ currentTaskDetails.message || '准备开始拉取...' }}
          </div>
        </div>
      </div>
    </t-dialog>

    <!-- 删除确认对话框 -->
    <t-dialog
      v-model:visible="deleteConfirmVisible"
      header="确认删除"
      :on-confirm="confirmDelete"
      :close-on-overlay-click="false"
    >
      <div class="delete-confirm-body">
        <p>
          确定要删除镜像
          {{ currentDeleteImage?.RepoTags?.[0] || currentDeleteImage?.Id?.slice(0, 12) || '未命名镜像' }} 吗？
        </p>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { MessagePlugin } from 'tdesign-vue-next';
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { InputValue } from 'tdesign-vue-next';
import { dockerWebSocketAPI } from '@/api/docker';
import type { PullImageProgress } from '@/api/docker';
import { IMAGE_TABLE_COLUMNS } from '@/constants/tableColumns';
import { formatDate } from '@/utils/format';

import {
  batchUpdateImages,
  cancelImagePull,
  checkImageUpdates,
  deleteImage,
  getImageList,
  updateImage,
} from '@/api/container';

// ==================== 1. 响应式数据定义 ====================
const images = ref([]);
const loading = ref(false);

// ==================== 2. 表格列配置 ====================
const columns = IMAGE_TABLE_COLUMNS;

// ==================== 3. 工具函数 ====================
const formatSize = (size: number): string => {
  if (!size) return '未知';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let index = 0;
  let formattedSize = size;

  while (formattedSize >= 1024 && index < units.length - 1) {
    formattedSize /= 1024;
    index++;
  }

  return `${formattedSize.toFixed(2)} ${units[index]}`;
};

// ==================== 4. API调用函数 ====================
// 获取镜像列表
const fetchImages = async () => {
  loading.value = true;
  try {
    const response = await getImageList();
    if (response.code === 0) {
      console.log('API response:', response.data);
      images.value = response.data.map((img) => {
        const imageId = img.id || '';
        if (!imageId) {
          console.warn('镜像ID为空:', img);
        }
        return {
          ...img,
          Id: imageId.replace('sha256:', '').slice(0, 8),
          name: img.name || '未命名镜像',
          tag: img.tag || 'latest',
          created: img.localCreateTime || img.created,
          lastChecked: img.lastChecked,
          needUpdate: img.needUpdate || false
        };
      });
    } else {
      MessagePlugin.error(response.message || '获取镜像列表失败');
    }
  } catch (error) {
    console.error('获取镜像列表失败:', error);
    MessagePlugin.error('获取镜像列表失败');
  } finally {
    loading.value = false;
  }
};

// ==================== 5. 事件处理函数 ====================
const handleDelete = (row: any) => {
  if (!row.Id) {
    MessagePlugin.error('无法获取镜像ID，删除操作失败');
    return;
  }
  currentDeleteImage.value = row;
  deleteConfirmVisible.value = true;
};

const confirmDelete = async () => {
  if (!currentDeleteImage.value) {
    console.error('当前删除镜像对象为空');
    return;
  }

  try {
    const result = await deleteImage(currentDeleteImage.value.Id);
    if (result.code === 0) {
      MessagePlugin.success('镜像删除成功');
      fetchImages();
    } else {
      MessagePlugin.error(result.message || '镜像删除失败');
    }
  } catch (error) {
    console.error('删除镜像出错:', error);
    MessagePlugin.error('镜像删除失败');
  } finally {
    deleteConfirmVisible.value = false;
    currentDeleteImage.value = null;
  }
};

// ==================== 6. 生命周期钩子 ====================
onMounted(() => {
  fetchImages();
  const cleanupInterval = setInterval(cleanupOldTasks, 60 * 60 * 1000);
  
  onUnmounted(() => {
    clearInterval(cleanupInterval);
  });
});

// ==================== 7. 拉取镜像相关状态 ====================
const pullImageDialogVisible = ref(false);
const pullImageForm = ref<any>(null);
const pullImageFormData = ref({
  image: '',
  tag: 'latest',
});
const isPulling = ref(false);
const pullProgress = ref(0);
const pullStatus = ref<'active' | 'success' | 'warning' | 'error'>('active');
const pullMessage = ref('');
const pullLayers = ref<any[]>([]);
const pullTaskId = ref('');

const pullImageFormRules = {
  image: [{ required: true, message: '请输入镜像地址' }],
  tag: [{ required: true, message: '请输入版本' }],
};

interface PullTask {
  taskId: string;
  image: string;
  tag: string;
  progress: number;
  status: string;
  message: string;
  layers: any[];
  startTime: number;
}

const activePullTasks = ref<PullTask[]>([]);
const pullTaskDetailsVisible = ref(false);
const currentTaskDetails = ref<any>(null);

// 添加拉取任务表格列定义
const pullTaskColumns = [
  { colKey: 'image', title: '镜像名称', width: 200 },
  { colKey: 'progress', title: '进度', width: 250 },
  { colKey: 'status', title: '状态', width: 100 },
  { colKey: 'operation', title: '操作', width: 150 },
];

// 获取任务状态主题颜色
const getTaskStatusTheme = (status: string) => {
  if (!status) return 'default';
  switch (status) {
    case 'pending':
      return 'warning';
    case 'running':
      return 'primary';
    case 'success':
      return 'success';
    case 'error':
      return 'danger';
    case 'canceled':
      return 'warning';
    default:
      return 'default';
  }
};

// 获取任务状态文本
const getTaskStatusText = (status: string) => {
  if (!status) return '未知';
  switch (status) {
    case 'pending':
      return '等待中';
    case 'running':
      return '拉取中';
    case 'success':
      return '已完成';
    case 'error':
      return '失败';
    case 'canceled':
      return '已取消';
    default:
      return status;
  }
};

// 获取进度条状态
const getProgressStatus = (status: string) => {
  if (!status) return 'active';
  switch (status) {
    case 'success':
      return 'success';
    case 'error':
      return 'error';
    case 'canceled':
      return 'warning';
    default:
      return 'active';
  }
};

// 添加新的响应式变量
const currentStage = ref('等待开始');
const logLines = ref<string[]>([]);
const completed = ref(false);

// 修改更新进度函数
const updatePullProgress = (data: PullImageProgress) => {
  if (!data) return;

  pullProgress.value = data.progress || 0;
  if (data.status) {
    logLines.value.push(data.status);
  }

  if (data.status) {
    if (data.status.includes('Copying config')) {
      currentStage.value = '复制配置层';
    } else if (data.status.includes('Copying blob')) {
      currentStage.value = '复制数据层';
    } else if (data.status.includes('Writing manifest')) {
      currentStage.value = '写入清单';
    } else if (data.status.includes('Storing signatures')) {
      currentStage.value = '存储签名';
    } else if (data.status.includes('Getting image source signatures')) {
      currentStage.value = '获取镜像签名';
    }
  }

  if (pullProgress.value >= 100) {
    completed.value = true;
  }
};

// 修改关闭对话框函数
const closePullDialog = () => {
  pullImageDialogVisible.value = false;
  isPulling.value = false;
  pullProgress.value = 0;
  currentStage.value = '等待开始';
  logLines.value = [];
  completed.value = false;
};

// 修改取消拉取函数
const handleCancelPull = async () => {
  if (!pullTaskId.value) {
    closePullDialog();
    return;
  }

  try {
    await dockerWebSocketAPI.cancelPull(pullTaskId.value);
    MessagePlugin.warning('已取消拉取镜像');
    closePullDialog();
  } catch (error) {
    console.error('取消拉取失败:', error);
    MessagePlugin.error(error instanceof Error ? error.message : '取消拉取失败');
  }
};

// 组件卸载时清除连接
onUnmounted(() => {
  dockerWebSocketAPI.disconnect();
});

// 修改创建容器处理函数
const router = useRouter();
const handleRun = (row: any) => {
  router.push({
    path: '/docker/create',
    query: {
      image: row.name,
      tag: row.tag
    }
  });
};

const deleteConfirmVisible = ref(false);
const currentDeleteImage = ref<any>(null);

// 计算各层的进度百分比
const getLayerProgress = (layer: any) => {
  if (!layer || !layer.progress) return 0;

  const progressText = layer.progress;
  // 从形如 "[=====>    ] 65.32MB/120.13MB" 的字符串中提取进度
  const progressMatch = progressText.match(/\[(=+>?\s*)\]\s*([\d.]+)MB\/([\d.]+)MB/);

  if (progressMatch) {
    const current = parseFloat(progressMatch[2]);
    const total = parseFloat(progressMatch[3]);
    if (!isNaN(current) && !isNaN(total) && total > 0) {
      return Math.floor((current / total) * 100);
    }
  }

  // 根据等号数量估算进度
  const barMatch = progressText.match(/\[(=+>?)\s*\]/);
  if (barMatch) {
    const bar = barMatch[1];
    const barLength = bar.length;
    const totalLength = 10; // 假设总长度为10
    return Math.floor((barLength / totalLength) * 100);
  }

  return 0;
};

// 修改格式化层级进度的函数，去掉百分比数字
const formatLayerProgress = (progressText: string) => {
  if (!progressText) return '';

  // 从形如 "[=====>    ] 65.32MB/120.13MB" 的字符串中提取数值部分
  const mbMatch = progressText.match(/([\d.]+)MB\/([\d.]+)MB/);
  if (mbMatch) {
    const current = parseFloat(mbMatch[1]);
    const total = parseFloat(mbMatch[2]);
    if (!isNaN(current) && !isNaN(total) && total > 0) {
      return `${current.toFixed(2)}MB/${total.toFixed(2)}MB`;
    }
    return `${mbMatch[0]}`;
  }

  // 如果没有MB格式，则去除进度条部分
  const cleanedText = progressText.replace(/\[=*>*\s*\]\s*/, '').trim();
  return cleanedText || progressText;
};

// 判断层是否已完成
const isLayerCompleted = (layer: any) => {
  if (!layer || !layer.status) return false;

  const status = layer.status.toLowerCase();
  return (
    status.includes('完成') ||
    status.includes('complete') ||
    status.includes('已下载') ||
    status.includes('downloaded') ||
    status.includes('已提取') ||
    status.includes('extracted')
  );
};

// 显示拉取任务详情
const showPullTaskDetails = (task: any) => {
  currentTaskDetails.value = { ...task };
  pullTaskDetailsVisible.value = true;
};

// 取消指定任务
const handleCancelPullTask = async (task: any) => {
  try {
    console.log('取消拉取任务:', task.taskId);
    const result = await cancelImagePull(task.taskId);

    if (result.code === 0) {
      // 更新任务状态
      const taskIndex = activePullTasks.value.findIndex((t) => t.taskId === task.taskId);
      if (taskIndex >= 0) {
        const updatedTask = { ...activePullTasks.value[taskIndex] };
        updatedTask.status = 'canceled';
        updatedTask.message = '用户取消了拉取';
        activePullTasks.value.splice(taskIndex, 1, updatedTask);
      }

      // 如果当前正在显示该任务的详情，也需要断开连接
      if (pullTaskId.value === task.taskId) {
        dockerWebSocketAPI.disconnect();
      }

      MessagePlugin.warning('已取消拉取镜像');
    } else {
      MessagePlugin.error(result.message || '取消拉取失败');
    }
  } catch (error) {
    console.error('取消拉取失败:', error);
    MessagePlugin.error('取消拉取失败');
  }
};

// 清理过期任务（如24小时前的成功或失败任务）
const cleanupOldTasks = () => {
  const currentTime = Date.now();
  const oneDayMs = 24 * 60 * 60 * 1000;

  activePullTasks.value = activePullTasks.value.filter((task) => {
    // 保留所有活动任务
    if (task.status === 'pending' || task.status === 'running') {
      return true;
    }

    // 保留24小时内的已完成或失败任务
    if (task.startTime && currentTime - task.startTime < oneDayMs) {
      return true;
    }

    return false;
  });
};


// 添加 handlePull 方法
const handlePull = () => {
  pullImageDialogVisible.value = true;
  isPulling.value = false;
  pullProgress.value = 0;
  pullStatus.value = 'active';
  pullMessage.value = '';
  pullLayers.value = [];
  pullTaskId.value = '';
};

// 处理单个镜像更新
const handleUpdate = async (row: any) => {
  try {
    // 添加到拉取任务列表中
    const newTask = {
      taskId: '', // 任务ID由后端生成
      image: row.name,
      tag: row.tag,
      progress: 0,
      status: 'pending',
      message: '准备更新镜像...',
      layers: [] as any[],
      startTime: Date.now(),
    };
    activePullTasks.value = [newTask, ...activePullTasks.value];

    // 发送WebSocket消息
    await dockerWebSocketAPI.pullImage({
      imageName: `${row.name}:${row.tag}`
    }, {
      onStart: (taskId) => {
        newTask.taskId = taskId;
      },
      onProgress: (progress: PullImageProgress) => {
        newTask.progress = progress.progress;
        newTask.message = progress.status;
        if (progress.layers) {
          newTask.layers = progress.layers;
        }
      },
      onComplete: () => {
        newTask.status = 'success';
        newTask.progress = 100;
        newTask.message = '镜像更新完成';
        fetchImages();
      },
      onError: (error) => {
        newTask.status = 'error';
        newTask.message = error;
      }
    });

    MessagePlugin.success('镜像更新请求已提交');
  } catch (error) {
    console.error('更新镜像失败:', error);
    MessagePlugin.error('更新镜像失败');
  }
};

// 检查镜像更新
const handleCheckUpdates = async () => {
  try {
    loading.value = true;
    
    // 发送WebSocket消息
    await dockerWebSocketAPI.checkImageUpdates(images.value.map(img => ({
      name: img.name,
      tag: img.tag
    })));

    MessagePlugin.info('开始检查镜像更新...');
  } catch (error) {
    console.error('检查镜像更新失败:', error);
    MessagePlugin.error('检查镜像更新失败');
  } finally {
    loading.value = false;
  }
};

// 添加WebSocket消息处理
const handleWebSocketMessage = (message: any) => {
  if (!message) return;

  switch (message.type) {
    case 'UPDATE_START':
      // 更新任务状态为运行中
      updateTaskStatus(message.taskId, 'running', '开始更新镜像...');
      break;
    case 'UPDATE_PROGRESS':
      // 更新任务进度
      updateTaskProgress(message.taskId, message.data.progress, message.data.status);
      break;
    case 'UPDATE_COMPLETE':
      // 更新任务完成
      updateTaskStatus(message.taskId, 'success', '镜像更新完成');
      fetchImages(); // 刷新镜像列表
      break;
    case 'CHECK_UPDATES_START':
      MessagePlugin.info('开始检查镜像更新...');
      break;
    case 'CHECK_UPDATES_COMPLETE':
      // 更新镜像列表中的更新状态
      if (message.data) {
        images.value = images.value.map(img => {
          const imageKey = `${img.name}:${img.tag}`;
          const updateInfo = message.data[imageKey];
          if (updateInfo) {
            return {
              ...img,
              needUpdate: updateInfo.hasUpdate,
              lastChecked: new Date().toISOString()
            };
          }
          return img;
        });
      }
      MessagePlugin.success('镜像更新检查完成');
      break;
  }
};

// 更新任务状态
const updateTaskStatus = (taskId: string, status: string, message: string) => {
  const taskIndex = activePullTasks.value.findIndex(t => t.taskId === taskId);
  if (taskIndex >= 0) {
    const task = activePullTasks.value[taskIndex];
    task.status = status;
    task.message = message;
    if (status === 'success' || status === 'error') {
      task.progress = 100;
    }
  }
};

// 更新任务进度
const updateTaskProgress = (taskId: string, progress: number, status: string) => {
  const taskIndex = activePullTasks.value.findIndex(t => t.taskId === taskId);
  if (taskIndex >= 0) {
    const task = activePullTasks.value[taskIndex];
    task.progress = progress;
    task.message = status;
  }
};

// 修改拉取镜像确认函数
const onPullImageConfirm = async () => {
  try {
    await pullImageForm.value?.validate();
    isPulling.value = true;
    pullProgress.value = 0;
    pullStatus.value = 'active';
    currentStage.value = '等待开始';
    logLines.value = [];
    completed.value = false;

    await dockerWebSocketAPI.pullImage(
      {
        imageName: `${pullImageFormData.value.image}:${pullImageFormData.value.tag}`,
      },
      {
        onStart: (taskId) => {
          pullTaskId.value = taskId;
          logLines.value.push('开始拉取镜像...');
        },
        onProgress: (progress) => {
          updatePullProgress(progress);
        },
        onComplete: () => {
          pullStatus.value = 'success';
          logLines.value.push('镜像拉取完成');
          completed.value = true;
          MessagePlugin.success('镜像拉取成功');
          setTimeout(() => {
            isPulling.value = false;
            pullImageDialogVisible.value = false;
            fetchImages();
          }, 1500);
        },
        onError: (error) => {
          pullStatus.value = 'error';
          logLines.value.push(`拉取失败: ${error}`);
          completed.value = true;
          MessagePlugin.error(`拉取镜像失败: ${error}`);
          setTimeout(() => {
            isPulling.value = false;
            pullImageDialogVisible.value = false;
          }, 3000);
        }
      }
    );
  } catch (error) {
    console.error('拉取镜像出错:', error);
    MessagePlugin.error(error instanceof Error ? error.message : '镜像拉取失败');
    isPulling.value = false;
    pullImageDialogVisible.value = false;
  }
};
</script>

<style scoped>
.container {
  padding: 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.title {
  font-size: 16px;
  font-weight: 500;
}

.actions {
  display: flex;
  gap: 8px;
}

.pull-progress-container {
  padding: 16px;
  min-height: 350px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.image-title {
  font-size: 16px;
  font-weight: 500;
}

.progress-percentage {
  font-size: 18px;
  font-weight: bold;
  color: #0052d9;
}

.progress-info {
  flex: 1;
  overflow-y: auto;
  max-height: 300px;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 12px;
  background-color: #f9f9f9;
}

.layers-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.layer-item {
  display: flex;
  flex-direction: column;
  font-size: 14px;
  padding: 8px 10px;
  border-radius: 4px;
  background-color: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border-left: 3px solid #ccc;
  transition: all 0.3s ease;
}

.layer-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  justify-content: space-between;
}

.layer-id {
  width: 120px;
  color: #0052d9;
  font-family: monospace;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.layer-status {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
}

.layer-progress {
  width: 200px;
  text-align: right;
  color: #0052d9;
  font-family: monospace;
}

.layer-progress-bar {
  flex: 1;
  margin-left: 12px;
  max-width: 150px;
}

.progress-message {
  padding: 16px;
  color: #666;
  font-size: 14px;
  text-align: center;
}

.progress-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.layer-completed {
  border-left: 3px solid #00a870; /* 绿色，表示已完成 */
  background-color: #f0faf5; /* 浅绿背景 */
}

.layer-in-progress {
  border-left: 3px solid #0052d9; /* 蓝色，表示进行中 */
  background-color: #f0f5ff; /* 浅蓝背景 */
}

.layer-completed .layer-status {
  color: #00a870; /* 绿色文字 */
}

.layer-in-progress .layer-status {
  color: #0052d9; /* 蓝色文字 */
}

.pulling-images-section {
  margin-bottom: 16px;
  background-color: #f8f8f8;
  border-radius: 4px;
  padding: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
}

.inline-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.progress-text {
  min-width: 40px;
  text-align: right;
  color: #0052d9;
  font-size: 14px;
  font-weight: 500;
}

/* 添加代理测试结果样式 */
.proxy-test-result {
  padding: 16px;
}

.test-metrics {
  margin: 16px 0;
  padding: 16px;
  background-color: #f8f8f8;
  border-radius: 4px;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.metric-label {
  color: #666;
}

.metric-value {
  font-weight: 500;
  color: #0052d9;
  font-size: 18px;
}

.proxy-test-loading {
  padding: 32px;
  text-align: center;
}
</style>
