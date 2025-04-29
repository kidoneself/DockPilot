<template>
  <div class="install-container">
    <t-card :bordered="false">
      <!-- 应用信息 -->
      <div class="app-info">
        <div class="app-header">
          <h2 class="app-title">{{ appDetail?.name }}</h2>
          <p class="app-description">{{ appDetail?.description }}</p>
        </div>
      </div>

      <!-- 主要内容区域 -->
      <div class="main-content">
        <!-- 左侧参数配置 -->
        <div class="parameters-section">
          <div class="section-card">
            <h3 class="section-title">参数配置</h3>
            <t-alert theme="info" title="配置说明" class="config-alert">
              <template #message>
                <p>1. 当前显示的是默认参数配置，您可以根据需要修改</p>
                <p>2. 参数修改后请点击"校验参数"按钮进行验证</p>
                <p>3. 校验通过后即可开始安装</p>
              </template>
            </t-alert>

            <!-- 镜像信息 -->
            <div class="images-section">
              <h4 class="subsection-title">所需镜像</h4>
              <div class="images-list">
                <div v-for="service in appDetail?.services" :key="service.id" class="image-item">
                  <div class="image-info">
                    <span class="service-name">{{ service.name }}</span>
                    <t-tag theme="primary" variant="light" size="small">{{ service.template.image }}</t-tag>
                    <t-tag 
                      :theme="imageCheckStatus[service.template.image] ? 'success' : 'warning'"
                      variant="light" 
                      size="small"
                    >
                      {{ imageCheckStatus[service.template.image] ? '已存在' : '未存在' }}
                    </t-tag>
                    <template v-if="!imageCheckStatus[service.template.image]">
                      <t-button
                        v-if="!imagePullStates[service.template.image]?.status"
                        theme="primary"
                        size="small"
                        :disabled="isAnyImagePulling"
                        @click="pullImage(service.template.image)"
                      >
                        拉取
                      </t-button>
                      <div v-else class="pull-progress">
                        <t-progress
                          :percentage="imagePullStates[service.template.image]?.progress || 0"
                          :label="false"
                          size="small"
                        />
                        <span class="progress-text">{{ imagePullStates[service.template.image]?.progress || 0 }}%</span>
                      </div>
                    </template>
                  </div>
                </div>
              </div>
            </div>

            <t-form ref="form" :data="formData" :rules="rules" class="parameters-form">
              <div class="parameters-grid">
                <div v-for="param in appDetail?.parameters" :key="param.key" class="parameter-card">
                  <div class="parameter-header">
                    <span class="parameter-label">{{ param.name }}</span>
                    <t-tag theme="primary" variant="light" size="small">{{ param.key }}</t-tag>
                  </div>
                  <t-input
                    v-model="formData[param.key]"
                    :placeholder="param.value"
                    class="parameter-input"
                  />
                  <!-- 显示校验结果 -->
                  <div v-if="validationResults[`${param.key}`]" class="validation-result">
                    <t-tag
                      :theme="validationResults[`${param.key}`].valid ? 'success' : 'danger'"
                      variant="light"
                      size="small"
                    >
                      {{ validationResults[`${param.key}`].message }}
                    </t-tag>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="action-buttons">
                <t-space size="large">
                  <t-button
                    theme="default"
                    size="large"
                    :loading="isValidating"
                    :disabled="isValidating"
                    @click="validateParams"
                  >
                    校验参数
                  </t-button>
                  <t-button
                    v-if="!isInstalling && !isInstallComplete"
                    theme="primary"
                    size="large"
                    :disabled="!isValidated"
                    @click="handleInstall"
                  >
                    开始安装
                  </t-button>
                  <t-button
                    v-else-if="!showLogDialog"
                    theme="primary"
                    size="large"
                    @click="reopenLogDialog"
                  >
                    {{ isInstallComplete ? '查看结果' : '查看日志' }}
                  </t-button>
                  <t-button
                    v-if="!isInstalling && !isInstallComplete"
                    theme="default"
                    size="large"
                    @click="handleCancel"
                  >
                    取消
                  </t-button>
                </t-space>
              </div>
            </t-form>
          </div>
        </div>

        <!-- 右侧服务状态 -->
        <!-- <div class="service-status">
          <div class="section-card">
            <h3 class="section-title">服务状态</h3>
            <div class="status-list">
              <div v-for="service in appDetail?.services" :key="service.id" class="status-item">
                <div class="service-name">{{ service.name }}</div>
                <div class="service-status">
                  <t-tag :theme="getStatusTheme(service.status)">
                    {{ service.status }}
                  </t-tag>
                </div>
              </div>
            </div>
          </div>
        </div> -->
      </div>

      <!-- 校验结果 -->
      <div v-if="validationResult" class="validation-result">
        <t-alert
          :theme="validationResult.success ? 'success' : 'error'"
          :message="validationResult.message"
        />
      </div>

      <!-- 安装日志弹出框 -->
      <t-dialog
        v-model:visible="showLogDialog"
        :header="'安装日志'"
        :footer="isInstallComplete"
        :close-on-overlay-click="!isInstalling"
        :close-on-esc-keydown="!isInstalling"
        width="800px"
        class="install-log-dialog"
      >
        <div class="log-content" ref="logContentRef">
          <div v-for="(log, index) in installLogs" :key="index" class="log-item">
            <t-icon :name="getLogIcon(log.type)" :class="['log-icon', log.type]" />
            <span class="log-message">{{ log.message }}</span>
            <span class="log-time">{{ log.time }}</span>
          </div>
        </div>
        <template #footer>
          <t-space>
            <t-button theme="primary" @click="handleConfirm">确定</t-button>
          </t-space>
        </template>
      </t-dialog>
    </t-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { MessagePlugin } from 'tdesign-vue-next';
import { getAppDetail } from '@/api/appStore';
import type { AppStoreAppDetail, ParameterConfig } from '@/api/model/appStoreModel';
import { dockerWebSocketService } from '@/api/websocket/DockerWebSocketService';
import type { WebSocketMessage } from '@/api/websocket/types';
import { checkImages } from '@/api/docker';

const route = useRoute();
const router = useRouter();

// 应用详情
const appDetail = ref<AppStoreAppDetail>();
const formData = ref<Record<string, any>>({});
const isValidated = ref(false);
const validationResult = ref<{ success: boolean; message: string } | null>(null);

// 安装状态
const currentStep = ref(0);
const isInstalling = ref(false);
const isValidating = ref(false);

// 安装日志
const installLogs = ref<Array<{
  type: 'info' | 'success' | 'warning' | 'error';
  message: string;
  time: string;
}>>([]);

// 服务状态
const serviceStatus = ref([
  {
    name: 'Transmission',
    status: '等待安装',
    progress: 0,
  },
  {
    name: 'Emby',
    status: '等待安装',
    progress: 0,
  },
  // 更多服务...
]);

// 表格列配置
const statusColumns = [
  { colKey: 'name', title: '服务名称' },
  { colKey: 'status', title: '状态' },
  { colKey: 'progress', title: '进度' },
];

// 表单校验规则
const rules = {
  // 动态生成校验规则
};

// 镜像检查状态
const imageCheckStatus = ref<Record<string, boolean>>({});
const isCheckingImages = ref(false);

// 镜像拉取状态
interface ImagePullState {
    status: boolean;      // 是否正在拉取
    progress: number;     // 拉取进度
    taskId: string | null; // 任务ID
}

const imagePullStates = ref<Record<string, ImagePullState>>({});
const isAnyImagePulling = ref(false); // 是否有镜像正在拉取

// 初始化镜像状态
const initImageState = (imageName: string) => {
    if (!imagePullStates.value[imageName]) {
        imagePullStates.value[imageName] = {
            status: false,
            progress: 0,
            taskId: null
        };
    }
};

// 获取应用详情
const fetchAppDetail = async () => {
  try {
    const id = route.params.id as string;
    const res = await getAppDetail(id);
    
    if (res.code === 0) {
      appDetail.value = res.data;
      // 初始化表单数据
      if (appDetail.value?.parameters) {
        appDetail.value.parameters.forEach(param => {
          formData.value[param.key] = param.value;
        });
      }
    } else {
      MessagePlugin.error(res.message || '获取应用详情失败');
    }
  } catch (error) {
    console.error('获取应用详情失败:', error);
    MessagePlugin.error('获取应用详情失败');
  }
};

// 参数校验状态
const validationResults = ref<Record<string, { valid: boolean; message: string }>>({});

// 校验参数
const validateParams = () => {
    if (!appDetail.value?.services) return;
    
    isValidating.value = true;
    validationResults.value = {};
    
    // 收集需要校验的端口和路径
    const ports: { hostPort: number }[] = [];
    const paths: { hostPath: string }[] = [];
    
    // 遍历参数配置
    appDetail.value.parameters.forEach(param => {
        const value = formData.value[param.key];
        if (!value) return;
        
        // 检查是否是端口参数
        if (param.key.toLowerCase().includes('port')) {
            const port = parseInt(value);
            if (!isNaN(port)) {
                ports.push({ hostPort: port });
            }
        }
        
        // 检查是否是路径参数
        if (param.key.toLowerCase().includes('path') || param.key.toLowerCase().includes('dir')) {
            paths.push({ hostPath: value });
        }
    });
    
    // 注册校验结果处理器
    dockerWebSocketService.on('INSTALL_VALIDATE_RESULT', (message: WebSocketMessage) => {
        const results = message.data as Array<{ type: string; valid: boolean; message: string; port?: number; path?: string }>;
        results.forEach(result => {
            const key = result.type === 'port' ? `port_${result.port}` : `path_${result.path}`;
            validationResults.value[key] = {
                valid: result.valid,
                message: result.message
            };
        });
        isValidating.value = false;
        
        // 检查是否所有参数都验证通过
        const allValid = results.every(result => result.valid);
        isValidated.value = allValid;
        
        if (allValid) {
            MessagePlugin.success('参数校验通过');
        } else {
            MessagePlugin.warning('参数校验未通过，请检查参数配置');
        }
    });
    
    // 发送校验请求
    dockerWebSocketService.validateParams({ ports, paths });
};

// 添加弹出框控制
const showLogDialog = ref(false);

// 添加日志容器引用
const logContentRef = ref<HTMLElement | null>(null);

// 滚动到底部的函数
const scrollToBottom = async () => {
  await nextTick();
  if (logContentRef.value) {
    logContentRef.value.scrollTop = logContentRef.value.scrollHeight;
  }
};

// 修改日志添加逻辑
const addLog = (type: 'info' | 'success' | 'warning' | 'error', message: string) => {
  installLogs.value.push({
    type,
    message,
    time: new Date().toLocaleTimeString()
  });
  scrollToBottom();
};

// 修改安装日志处理器
dockerWebSocketService.on('INSTALL_LOG', (message: WebSocketMessage) => {
  const { type, message: logMessage } = message.data as {
    type: 'info' | 'success' | 'warning' | 'error';
    message: string;
  };
  addLog(type, logMessage);
});

// 添加安装完成状态
const isInstallComplete = ref(false);

// 重新打开日志框
const reopenLogDialog = () => {
  showLogDialog.value = true;
};

// 处理安装完成
const handleInstallComplete = () => {
  isInstallComplete.value = true;
  isInstalling.value = false;
};

// 处理确定按钮点击
const handleConfirm = () => {
  router.push('/docker/containers');
};

// 开始安装
const handleInstall = async () => {
  isInstalling.value = true;
  showLogDialog.value = true; // 显示日志弹出框
  installLogs.value = [{
    type: 'info',
    message: '开始安装...',
    time: new Date().toLocaleTimeString()
  }];

  try {
    // 发送安装请求
    dockerWebSocketService.send({
      type: 'INSTALL_START',
      data: {
        appId: route.params.id,
        params: formData.value
      }
    } as any);

    // 注册安装日志处理器
    dockerWebSocketService.on('INSTALL_LOG', (message: WebSocketMessage) => {
      const { type, message: logMessage } = message.data as {
        type: 'info' | 'success' | 'warning' | 'error';
        message: string;
      };
      
      addLog(type, logMessage);
    });

    // 注册安装结果处理器
    dockerWebSocketService.on('INSTALL_START_RESULT', (message: WebSocketMessage) => {
      const { success, message: resultMessage, template } = message.data as {
        success: boolean;
        message: string;
        template: any;
      };

      if (success) {
        addLog('success', '模板处理成功');
        console.log('处理后的模板:', template);
        handleInstallComplete();
      } else {
        addLog('error', resultMessage || '安装失败');
        isInstalling.value = false;
      }
    });

  } catch (error) {
    addLog('error', '安装失败');
    isInstalling.value = false;
  }
};

// 取消安装
const handleCancel = () => {
  // 只返回上一页，不进行其他操作
  router.back();
};

// 获取日志图标
const getLogIcon = (type: string) => {
  const icons: Record<string, string> = {
    info: 'info-circle',
    success: 'check-circle',
    warning: 'warning',
    error: 'error-circle',
  };
  return icons[type] || 'info-circle';
};

// 获取状态主题
// const getStatusTheme = (status: string) => {
//   const themes: Record<string, string> = {
//     '等待安装': 'default',
//     '安装中': 'warning',
//     '已完成': 'success',
//     '失败': 'danger',
//   };
//   return themes[status] || 'default';
// };

// 检查应用所需的镜像
const checkAppImages = async () => {
    if (!appDetail.value?.services) return;
    
    isCheckingImages.value = true;
    imageCheckStatus.value = {};
    
    // 初始化所有镜像的状态
    appDetail.value.services.forEach(service => {
        const imageName = service.template.image;
        imageCheckStatus.value[imageName] = false;
        initImageState(imageName);
    });
    
    try {
        // 收集所有需要检查的镜像
        const imagesToCheck = appDetail.value.services.map(service => {
            const imageName = service.template.image;
            const [name, tag = 'latest'] = imageName.split(':');
            return { name, tag };
        });
        
        // 发送检查请求
        await checkImages(imagesToCheck);
        
        // 结果会通过 WebSocket 消息 'INSTALL_CHECK_IMAGES_RESULT' 返回
        // 在 onMounted 中已经注册了相应的处理函数
    } catch (error) {
        console.error('检查镜像失败:', error);
        addLog('error', `检查镜像失败: ${error}`);
    } finally {
        isCheckingImages.value = false;
    }
};

// 拉取镜像
const pullImage = async (imageName: string) => {
  try {
    // 初始化镜像状态
    initImageState(imageName);
    imagePullStates.value[imageName].status = true;
    isAnyImagePulling.value = true;

    await dockerWebSocketService.pullImage(
      { imageName },
      {
        onStart: (taskId) => {
          console.log('开始拉取镜像:', imageName, '任务ID:', taskId)
          imagePullStates.value[imageName].taskId = taskId;
        },
        onProgress: (progress) => {
          console.log('拉取进度:', progress)
          imagePullStates.value[imageName].progress = progress.progress;
        },
        onComplete: async () => {
          console.log('拉取完成:', imageName)
          imagePullStates.value[imageName].status = false;
          imagePullStates.value[imageName].progress = 100;
          isAnyImagePulling.value = false;
          MessagePlugin.success(`镜像 ${imageName} 拉取成功`)
          
          // 等待一小段时间确保镜像已经准备好
          await new Promise(resolve => setTimeout(resolve, 1000));
          
          // 重新检查所有镜像状态
          await checkAppImages();
          
          // 强制更新 imageCheckStatus
          const [name, tag] = imageName.split(':');
          imageCheckStatus.value[imageName] = true;
        },
        onError: (error) => {
          console.error('拉取失败:', error)
          imagePullStates.value[imageName].status = false;
          imagePullStates.value[imageName].progress = 0;
          isAnyImagePulling.value = false;
          MessagePlugin.error(`镜像 ${imageName} 拉取失败: ${error}`)
        }
      }
    )
  } catch (error) {
    console.error('拉取镜像失败:', error)
    imagePullStates.value[imageName].status = false;
    imagePullStates.value[imageName].progress = 0;
    isAnyImagePulling.value = false;
    MessagePlugin.error('拉取镜像失败')
  }
}

// 在组件挂载时检查镜像
onMounted(() => {
    // 注册 WebSocket 消息处理器
    dockerWebSocketService.on('INSTALL_CHECK_IMAGES_RESULT', (message: WebSocketMessage) => {
        console.log('收到镜像检查结果:', message.data);
        const results = message.data as Array<{ name: string; tag: string; exists: boolean }>;
        results.forEach(result => {
            const fullImageName = `${result.name}:${result.tag}`;
            console.log('更新镜像状态:', fullImageName, result.exists);
            imageCheckStatus.value[fullImageName] = result.exists;
        });
        console.log('更新后的镜像状态:', imageCheckStatus.value);
    });

    fetchAppDetail().then(() => {
        checkAppImages();
    });
});

// 在组件卸载时移除处理器
onUnmounted(() => {
    dockerWebSocketService.off('INSTALL_CHECK_IMAGES_RESULT', () => {});
    dockerWebSocketService.off('INSTALL_LOG', () => {});
    dockerWebSocketService.off('INSTALL_START_RESULT', () => {});
    dockerWebSocketService.off('INSTALL_VALIDATE_RESULT', () => {});
});
</script>

<style scoped>
.install-container {
  padding: 24px;
  height: 100%;
  width: 100%;
  background-color: var(--td-bg-color-page);
  display: flex;
  flex-direction: column;
}

.install-container :deep(.t-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.install-container :deep(.t-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
  overflow-y: auto;
}

.app-info {
  border-bottom: 1px solid var(--td-component-stroke);
  padding-bottom: 16px;
}

.app-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.app-description {
  color: var(--td-text-color-secondary);
  margin: 0;
  flex: 1;
}

.main-content {
  display: flex;
  gap: 24px;
  min-height: 0;
}

.parameters-section {
  flex: 1;
  min-width: 0;
}

.section-card {
  background-color: var(--td-bg-color-container);
  border-radius: 8px;
  padding: 16px;
  box-shadow: var(--td-shadow-1);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.info-icon {
  color: var(--td-text-color-secondary);
  font-size: 16px;
  cursor: help;
}

.parameters-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.parameters-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.parameter-card {
  background-color: var(--td-bg-color-secondarycontainer);
  border-radius: 6px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
}

.parameter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.parameter-label {
  font-weight: 500;
  color: var(--td-text-color-primary);
  font-size: 14px;
}

.parameter-input {
  width: 100%;
}

.parameter-input :deep(.t-input) {
  background-color: var(--td-bg-color-container);
  height: 32px;
}

.parameter-input :deep(.t-input__inner) {
  font-size: 13px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--td-component-stroke);
}

.action-buttons :deep(.t-button) {
  min-width: 120px;
  height: 40px;
  font-size: 14px;
  border-radius: 4px;
}

.action-buttons :deep(.t-space) {
  gap: 16px;
}

.validation-result {
  margin-top: 4px;
  font-size: 12px;
}

.install-log-dialog :deep(.t-dialog__body) {
  padding: 0;
}

.install-log-dialog :deep(.t-dialog__header) {
  padding: 16px 24px;
  border-bottom: 1px solid var(--td-component-stroke);
}

.log-content {
  max-height: 500px;
  overflow-y: auto;
  padding: 16px;
  background-color: var(--td-bg-color-secondarycontainer);
  font-family: monospace;
}

.log-item {
  display: flex;
  align-items: center;
  padding: 4px 0;
  font-size: 13px;
  line-height: 1.5;
}

.log-icon {
  margin-right: 8px;
  font-size: 16px;
}

.log-icon.info {
  color: var(--td-brand-color);
}

.log-icon.success {
  color: var(--td-success-color);
}

.log-icon.warning {
  color: var(--td-warning-color);
}

.log-icon.error {
  color: var(--td-error-color);
}

.log-message {
  flex: 1;
  white-space: pre-wrap;
  word-break: break-all;
}

.log-time {
  color: var(--td-text-color-secondary);
  font-size: 12px;
  margin-left: 16px;
}

/* 自定义滚动条样式 */
.log-content::-webkit-scrollbar {
  width: 6px;
}

.log-content::-webkit-scrollbar-thumb {
  background-color: var(--td-text-color-disabled);
  border-radius: 3px;
}

.log-content::-webkit-scrollbar-track {
  background-color: transparent;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .main-content {
    flex-direction: column;
  }
  
  .parameters-form {
    padding: 16px;
  }
  
  .app-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

.config-alert {
  margin-bottom: 16px;
}

.config-alert :deep(.t-alert__message) {
  font-size: 13px;
  line-height: 1.6;
}

.config-alert :deep(.t-alert__message p) {
  margin: 4px 0;
}

.images-section {
  margin-bottom: 24px;
  padding: 16px;
  background-color: var(--td-bg-color-secondarycontainer);
  border-radius: 6px;
}

.subsection-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--td-text-color-primary);
  margin: 0 0 12px;
}

.images-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.image-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: var(--td-bg-color-container);
  border-radius: 4px;
}

.image-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.service-name {
  font-weight: 500;
  color: var(--td-text-color-primary);
}

.t-tag {
  margin-left: 8px;
}

.t-button {
  margin-left: 8px;
}

.pull-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
  width: 120px;
}

.progress-text {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}
</style> 