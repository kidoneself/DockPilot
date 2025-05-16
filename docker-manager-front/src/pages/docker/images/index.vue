<template>
  <div class="container">
    <t-card>
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
      <div v-if="activePullTasks && activePullTasks.length > 0" class="pulling-images-section">
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

      <t-table :data="images" :columns="columns" :loading="loading" row-key="id">
        <template #id="{ row }">
          <t-tag theme="default" variant="light">{{ row.id?.slice(0, 8) }}</t-tag>
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
        <template #size="{ row }">
          <span>{{ formatSize(row.size) }}</span>
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
    </t-card>

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
        <div class="pull-task-header">
          <span class="pull-task-title">拉取镜像</span>
          <span class="pull-task-image">{{ pullImageFormData.image }}:{{ pullImageFormData.tag }}</span>
        </div>
        <div class="pull-task-progress-row">
          <t-progress :percentage="pullProgress" :label="true" style="flex:1;" />
        </div>
        <div class="pull-task-log-box" ref="logBoxRef">
          <div v-for="(log, index) in logLines" :key="index" class="pull-task-log-line">{{ log }}</div>
        </div>
        <div class="pull-task-btns">
          <t-button v-if="!completed" theme="default" variant="outline" @click="handleCancelPull">
            取消拉取
          </t-button>
          <t-button v-else theme="primary" @click="closePullDialog"> 关闭 </t-button>
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
          {{ currentDeleteImage?.name }}:{{ currentDeleteImage?.tag }} 吗？
        </p>
      </div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { MessagePlugin } from 'tdesign-vue-next';
import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import type { PullImageProgress } from '@/api/model/websocketModel';
import { formatDate } from '@/utils/format';
import { useNotificationStore } from '@/store/modules/notification';
import {
  batchUpdateImages,
  cancelImagePull,
  checkImageUpdates,
  deleteImage,
  getImageList,
  updateImage,
  pullImage,
} from '@/api/websocket/container';

// ==================== 1. 响应式数据定义 ====================
const images = ref([]);
const loading = ref(false);
const router = useRouter();
const deleteConfirmVisible = ref(false);
const currentDeleteImage = ref<any>(null);
const notificationStore = useNotificationStore();

// ==================== 2. 表格列配置 ====================
const columns = [
  { colKey: 'id', title: '镜像ID', width: 120 },
  { colKey: 'name', title: '镜像名称', width: 300 },
  { colKey: 'tag', title: '标签', width: 100 },
  { colKey: 'created', title: '创建时间', width: 180 },
  { colKey: 'size', title: '大小', width: 120 },
  { colKey: 'needUpdate', title: '更新状态', width: 120 },
  { colKey: 'lastChecked', title: '最后检查', width: 180 },
  { colKey: 'operation', title: '操作', width: 200 },
];

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
    const res = await getImageList();
    console.log('获取到的镜像列表:', res);
    // 处理镜像列表数据
    images.value = res.data.map((img) => ({
      id: img.id.replace('sha256:', '') || '',
      name: img.name || '未命名镜像',
      tag: img.tag || 'latest',
      created: img.localCreateTime || img.created,
      lastChecked: img.lastChecked,
      needUpdate: img.needUpdate || false,
      size: img.size || 0,
      RepoTags: [`${img.name}:${img.tag}`],
    }));
  } catch (error) {
    console.error('获取镜像列表失败:', error);
    MessagePlugin.error(error instanceof Error ? error.message : '获取镜像列表失败');
  } finally {
    loading.value = false;
  }
};

// ==================== 5. 事件处理函数 ====================
const handleDelete = async (image: any) => {
  currentDeleteImage.value = image;
  deleteConfirmVisible.value = true;
};

const handleUpdate = async (image: any) => {
  try {
    // 发送更新请求但不等待完成
    updateImage({
      image: image.name,
      tag: image.tag,
    }).then(() => {
      // 发送成功通知
      notificationStore.addNotification({
        id: String(Date.now()),
        content: `镜像 ${image.name}:${image.tag} 更新成功`,
        type: 'success',
        status: true,
        collected: false,
        date: new Date().toLocaleString(),
        quality: 'high'
      });
      fetchImages();
    }).catch((error) => {
      console.error('更新镜像失败:', error);
      // 发送错误通知
      notificationStore.addNotification({
        id: String(Date.now()),
        content: error instanceof Error ? error.message : '更新镜像失败',
        type: 'error',
        status: true,
        collected: false,
        date: new Date().toLocaleString(),
        quality: 'high'
      });
    });
    
    // 立即返回，不阻塞UI
    MessagePlugin.info({
      content: `镜像 ${image.name}:${image.tag} 更新已开始，请稍后查看结果`,
      duration: 3000,
      closeBtn: true,
    });
  } catch (error) {
    console.error('更新镜像失败:', error);
    notificationStore.addNotification({
      id: String(Date.now()),
      content: error instanceof Error ? error.message : '更新镜像失败',
      type: 'error',
      status: true,
      collected: false,
      date: new Date().toLocaleString(),
      quality: 'high'
    });
  }
};

const handleCheckUpdates = async () => {
  try {
    const res = await checkImageUpdates();
    if (res.code === 0) {
      MessagePlugin.success(res.message || '检查更新成功');
      fetchImages();
    } else {
      MessagePlugin.error(res.message || '检查更新失败');
    }
  } catch (error) {
    console.error('检查更新失败:', error);
    MessagePlugin.error(error instanceof Error ? error.message : '检查更新失败');
  }
};

const handleBatchUpdate = async () => {
  try {
    await batchUpdateImages({ useProxy: false });
    MessagePlugin.success('批量更新成功');
    fetchImages();
  } catch (error) {
    console.error('批量更新失败:', error);
    MessagePlugin.error('批量更新失败');
  }
};

const handleCancelPullTask = async (task: any) => {
  try {
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
        // 不需要手动断开连接，pullImage 函数会处理连接管理
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

// ==================== 6. 生命周期钩子 ====================
onMounted(() => {
  fetchImages();
});

// ==================== 7. 其他功能 ====================
const handleRun = (row: any) => {
  router.push({
    path: '/docker/create',
    query: {
      image: row.name,
      tag: row.tag,
    },
  });
};

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

// ==================== 8. 拉取镜像相关状态 ====================
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
const logBoxRef = ref<HTMLElement | null>(null);
const logLines = ref<string[]>([]);
const completed = ref(false);

// 自动滚动到底部，兼容多种日志追加方式
watch(
  () => logLines.value.length,
  () => {
    nextTick(() => {
      if (logBoxRef.value) {
        logBoxRef.value.scrollTop = logBoxRef.value.scrollHeight;
      }
    });
  }
);

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

// 修改拉取镜像确认函数
const onPullImageConfirm = async () => {
  try {
    console.log('开始拉取镜像流程...');
    await pullImageForm.value?.validate();
    isPulling.value = true;
    pullProgress.value = 0;
    pullStatus.value = 'active';
    logLines.value = [];
    completed.value = false;

    console.log('准备调用 pullImage API...');
    await pullImage(
      {
        imageName: `${pullImageFormData.value.image}:${pullImageFormData.value.tag}`,
      },
      {
        // 任务开始回调
        onStart: (taskId) => {
          console.log('onStart 被调用，taskId:', taskId);
          pullTaskId.value = taskId;
          logLines.value.push('开始拉取镜像...');
        },

        // 进度回调
        onProgress: (progress: number) => {
          console.log('onProgress 被调用，progress:', progress);
          pullProgress.value = progress;
        },

        // 日志回调
        onLog: (data: any) => {
          console.log('onLog 被调用，数据:', data);
          logLines.value.push(data);
        },

        // 完成回调
        onComplete: () => {
          console.log('onComplete 被调用');
          pullStatus.value = 'success';
          logLines.value.push('镜像拉取完成');
          completed.value = true;
          MessagePlugin.success('镜像拉取成功');
          setTimeout(() => {
            isPulling.value = false;
            pullImageDialogVisible.value = false;
            fetchImages(); // 刷新镜像列表
          }, 1500);
        },

        // 错误回调
        onError: (error) => {
          console.log('onError 被调用，错误:', error);
          pullStatus.value = 'error';
          logLines.value.push(`拉取失败: ${error}`);
          completed.value = true;
          MessagePlugin.error(`拉取镜像失败: ${error}`);
          setTimeout(() => {
            isPulling.value = false;
            pullImageDialogVisible.value = false;
          }, 3000);
        },
      },
    );
    console.log('pullImage API 调用完成');
  } catch (error) {
    console.error('拉取镜像出错:', error);
    MessagePlugin.error(error instanceof Error ? error.message : '镜像拉取失败');
    isPulling.value = false;
    pullImageDialogVisible.value = false;
  }
};

// 修改取消拉取函数
const handleCancelPull = async () => {
  if (!pullTaskId.value) {
    MessagePlugin.error('任务ID不存在');
    return;
  }
  try {
    await cancelImagePull(pullTaskId.value);
    pullStatus.value = 'warning'; // 改为 warning 而不是 canceled
    logLines.value.push('用户取消了拉取');
    completed.value = true;
    MessagePlugin.warning('已取消拉取镜像');
    setTimeout(() => {
      isPulling.value = false;
      pullImageDialogVisible.value = false;
    }, 1500);
  } catch (error) {
    console.error('取消拉取失败:', error);
    MessagePlugin.error('取消拉取失败');
  }
};

// 修改关闭对话框函数
const closePullDialog = () => {
  pullImageDialogVisible.value = false;
  isPulling.value = false;
  pullProgress.value = 0;
  logLines.value = [];
  completed.value = false;
  pullTaskId.value = ''; // 清空任务ID
};

// 确认删除镜像
const confirmDelete = async () => {
  if (!currentDeleteImage.value) return;

  // 调用deleteImage函数，获取操作结果
  const result = await deleteImage(currentDeleteImage.value.id);

  // 根据操作结果处理UI
  if (result.success) {
    MessagePlugin.success('删除成功');
    deleteConfirmVisible.value = false;
    currentDeleteImage.value = null;
    fetchImages();
  } else if (result.message) {
    MessagePlugin.error(result.message);
  }
};

// 组件卸载时清除连接
onUnmounted(() => {
  // 不需要手动断开连接，pullImage 函数会处理连接管理
});
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

.scrollbar-container::-webkit-scrollbar {
  width: 6px;
}

.scrollbar-container::-webkit-scrollbar-thumb {
  background-color: #ccc;
  border-radius: 3px;
}

.scrollbar-container::-webkit-scrollbar-track {
  background-color: #f1f1f1;
}

/* 添加进度条样式 */
.t-progress-domo-width {
  width: 100%;
  margin-bottom: 8px;
}
.log-box {
  background: #f7f8fa;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.7;
  border: 1px solid #e5e6eb;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
  transition: min-height 0.2s;
}
.log-line {
  margin-bottom: 2px;
  padding: 4px 8px;
  border-radius: 4px;
  word-break: break-all;
  background: #fff;
}

/* 任务卡片风格样式 */
.task-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.10);
  padding: 24px 24px 16px 24px;
  min-width: 420px;
  max-width: 600px;
  margin: 0 auto;
}
.task-card-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.task-card-icon {
  color: #0052d9;
  margin-right: 16px;
}
.task-card-title {
  flex: 1;
}
.task-card-title-main {
  font-size: 18px;
  font-weight: 600;
  color: #222;
}
.task-card-title-sub {
  font-size: 13px;
  color: #888;
  margin-top: 2px;
}
.task-card-progress-num {
  font-size: 22px;
  font-weight: bold;
  color: #0052d9;
  margin-left: 16px;
  min-width: 56px;
  text-align: right;
}
.task-card-log-box {
  background: #f7f8fa;
  border-radius: 6px;
  padding: 10px 14px;
  min-height: 36px;
  max-height: 120px;
  overflow-y: auto;
  font-size: 14px;
  margin-bottom: 12px;
  border: 1px solid #e5e6eb;
  transition: min-height 0.2s;
}
.task-card-log-line {
  color: #333;
  line-height: 1.7;
  font-family: 'Fira Mono', 'Consolas', 'Menlo', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  margin-bottom: 2px;
}
.task-card-btns {
  text-align: right;
  margin-top: 4px;
}

/* 极简主次分明风格样式 */
.pull-task-simple-card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  padding: 20px 20px 12px 20px;
  min-width: 400px;
  max-width: 540px;
  margin: 0 auto;
}
.pull-task-header {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}
.pull-task-title {
  font-size: 16px;
  font-weight: 600;
  color: #222;
  margin-right: 12px;
}
.pull-task-image {
  font-size: 13px;
  color: #888;
}
.pull-task-progress-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.pull-task-progress-num {
  font-size: 16px;
  color: #0052d9;
  font-weight: bold;
  margin-left: 12px;
  min-width: 40px;
  text-align: right;
}
.pull-task-log-box {
  background: #f7f8fa;
  border-radius: 6px;
  padding: 8px 12px;
  min-height: 60px;
  max-height: 220px;
  overflow-y: auto;
  font-size: 13px;
  margin-bottom: 10px;
  border: 1px solid #e5e6eb;
}
.pull-task-log-line {
  color: #333;
  font-family: 'Fira Mono', 'Consolas', 'Menlo', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  margin-bottom: 2px;
}
.pull-task-btns {
  text-align: right;
  margin-top: 2px;
}
</style>
