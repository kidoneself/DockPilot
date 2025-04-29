<template>
  <div class="proxy-setting">
    <t-tabs default-value="proxy" class="custom-tabs">
      <t-tab-panel value="proxy" label="代理加速">
        <t-alert theme="info" title="配置说明" :close="true">
          <template #message>
            <p>1. 代理地址格式为：http://host:port 或 https://host:port</p>
            <p>2. 启用代理后，Docker 将使用配置的代理服务器进行网络访问</p>
            <p>3. 可以通过"测试代理延迟"功能检查代理服务器的连接速度</p>
          </template>
        </t-alert>
        <t-form ref="form" :data="formData" :rules="rules" @submit="onSubmit" class="custom-form">
          <t-form-item label="代理地址" name="proxyUrl">
            <t-input v-model="formData.proxyUrl" placeholder="例如：http://proxy.example.com:8080" />
          </t-form-item>
          <t-form-item label="启用代理加速" name="proxyEnabled">
            <t-switch v-model="formData.proxyEnabled" />
          </t-form-item>
          <div class="form-actions">
            <t-space>
              <t-button theme="primary" @click="handleSaveProxy">保存</t-button>
              <t-button theme="default" @click="onReset">重置</t-button>
              <t-button @click="handleTestProxy">
                <template #icon>
                  <t-icon name="time" />
                </template>
                测试代理延迟
              </t-button>
            </t-space>
          </div>
        </t-form>
      </t-tab-panel>

      <t-tab-panel value="mirror" label="镜像加速">
        <t-alert theme="info" title="配置说明" :close="true">
          <template #message>
            <p>1. 镜像加速地址格式为：https://mirror.example.com/</p>
            <p>2. 可以添加多个镜像加速地址，但只能启用一个</p>
            <p>3. 建议使用国内镜像源，如阿里云、腾讯云等提供的镜像加速服务</p>
          </template>
        </t-alert>
        <t-form ref="mirrorForm" :data="formData" :rules="rules" @submit="onSubmit" class="custom-form">
          <t-form-item v-for="(item, index) in formData.mirrorUrls" :key="index" :label="index === 0 ? '镜像地址' : ''" :name="`mirrorUrls.${index}.url`">
            <t-space style="width: 100%">
              <t-input v-model="item.url" placeholder="例如：https://hub.naspt.de/" style="width: 100%" />
              <t-space size="small" style="flex-shrink: 0; display: flex; align-items: center; gap: 8px">
                <t-switch v-model="item.enabled" @change="() => toggleMirrorUrl(index)" />
                <t-button 
                  v-if="index !== 0"
                  theme="danger" 
                  variant="text" 
                  @click="() => removeMirrorUrl(index)"
                >
                  <t-icon name="delete" />
                </t-button>
              </t-space>
            </t-space>
          </t-form-item>
          <div class="form-actions">
            <t-space>
              <t-button theme="primary" @click="handleSaveMirror">保存</t-button>
              <t-button theme="default" @click="onReset">重置</t-button>
              <t-button 
                theme="primary" 
                variant="outline" 
                @click="addMirrorUrl"
                :disabled="formData.mirrorUrls.length >= MAX_MIRROR_URLS"
              >
                <t-icon name="add" />添加
              </t-button>
            </t-space>
          </div>
        </t-form>
      </t-tab-panel>
    </t-tabs>

    <!-- 添加代理测试结果对话框 -->
    <t-dialog v-model:visible="proxyTestDialogVisible" header="代理延迟测试结果" :close-on-overlay-click="false">
      <div v-if="proxyTestResult" class="proxy-test-result">
        <t-alert theme="info" message="测试目标: Docker Hub (registry-1.docker.io)" />
        <div class="test-metrics">
          <div class="metric-item">
            <span class="metric-label">HTTP延迟时间:</span>
            <span class="metric-value">{{ proxyTestResult.httpConnectTime }}ms</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">HTTPS延迟时间:</span>
            <span class="metric-value">{{ proxyTestResult.httpsConnectTime }}ms</span>
          </div>
        </div>
        <t-alert :theme="getProxyTestAlertTheme()" :message="getProxyTestSuggestion()" />
      </div>
      <div v-else class="proxy-test-loading">
        <t-loading text="正在测试代理延迟..." />
      </div>
    </t-dialog>
  </div>
</template>

<script lang="ts" setup>
import type { FormInstanceFunctions, FormRule, SubmitContext, Data as TData } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { onMounted, ref } from 'vue';

import { setProxySetting, setMirrorSetting, getSystemSetting, testProxyLatency } from '@/api/system';

const MAX_MIRROR_URLS = 5;

interface Data extends TData {
  proxyEnabled: boolean;
  proxyUrl: string;
  mirrorUrls: {
    url: string;
    enabled: boolean;
  }[];
}

const form = ref<FormInstanceFunctions | null>(null);
const mirrorForm = ref<FormInstanceFunctions | null>(null);
const formData = ref<Data>({
  proxyEnabled: false,
  proxyUrl: '',
  mirrorUrls: [{ url: '', enabled: false }],
});

const rules: Record<string, FormRule[]> = {
  // 移除所有校验规则
};

const addMirrorUrl = () => {
  if (formData.value.mirrorUrls.length >= MAX_MIRROR_URLS) {
    MessagePlugin.warning('最多只能添加5个镜像加速地址');
    return;
  }
  formData.value.mirrorUrls.push({ url: '', enabled: false });
};

const removeMirrorUrl = (index: number) => {
  formData.value.mirrorUrls.splice(index, 1);
};

const toggleMirrorUrl = (index: number) => {
  formData.value.mirrorUrls.forEach((item, i) => {
    item.enabled = i === index;
  });
};

const onSubmit = async ({ validateResult, firstError }: SubmitContext) => {
  if (validateResult === true) {
    // 表单验证通过，可以在这里处理提交逻辑
    return true;
  }
  MessagePlugin.warning(firstError);
  return false;
};

const handleSaveProxy = async () => {
  if (!form.value) return;
  const result = await form.value.validate();
  if (result === true) {
    try {
      const result = await setProxySetting({
        enabled: formData.value.proxyEnabled,
        url: formData.value.proxyUrl
      });
      if (result.code === 0) {
        MessagePlugin.success('保存成功');
      } else {
        MessagePlugin.error(result.message || '保存失败');
      }
    } catch (error) {
      console.error('保存代理设置失败:', error);
      MessagePlugin.error('保存失败');
    }
  }
};

const handleSaveMirror = async () => {
  if (!mirrorForm.value) return;
  const result = await mirrorForm.value.validate();
  if (result === true) {
    try {
      const res = await setMirrorSetting({
        urls: formData.value.mirrorUrls
      });
      if (res.code === 0) {
        MessagePlugin.success('保存成功');
      } else {
        MessagePlugin.error(res.message || '保存失败');
      }
    } catch (error) {
      MessagePlugin.error('保存失败');
    }
  }
};

const onReset = () => {
  formData.value = {
    proxyEnabled: false,
    proxyUrl: '',
    mirrorUrls: [{ url: '', enabled: false }],
  };
};

// 添加代理测试相关状态
const proxyTestDialogVisible = ref(false);
const proxyTestResult = ref<any>(null);
const proxyTestLoading = ref(false);

// 添加代理测试方法
const handleTestProxy = async () => {
  proxyTestDialogVisible.value = true;
  proxyTestLoading.value = true;
  proxyTestResult.value = null;

  try {
    const result = await testProxyLatency();
    if (result.code === 0) {
      proxyTestResult.value = result.data;
    } else {
      MessagePlugin.error(result.message || '测试代理延迟失败');
    }
  } catch (error) {
    console.error('测试代理延迟失败:', error);
    MessagePlugin.error('测试代理延迟失败');
  } finally {
    proxyTestLoading.value = false;
  }
};

// 获取代理测试建议的Alert主题
const getProxyTestAlertTheme = () => {
  if (!proxyTestResult.value) return 'info';

  const { totalTime } = proxyTestResult.value;
  if (totalTime < 500) return 'success';
  if (totalTime < 1000) return 'info';
  if (totalTime < 2000) return 'warning';
  return 'error';
};

// 获取代理测试建议文本
const getProxyTestSuggestion = () => {
  if (!proxyTestResult.value) return '正在测试...';

  const { totalTime } = proxyTestResult.value;
  if (totalTime < 500) return '代理速度良好，建议使用代理';
  if (totalTime < 1000) return '代理速度一般，可以使用代理';
  if (totalTime < 2000) return '代理速度较慢，建议检查代理配置';
  return '代理速度很慢，建议不使用代理';
};

const loadSettings = async () => {
  try {
    // 获取代理设置
    const proxyResult = await getSystemSetting('proxy');
    if (proxyResult.code === 0 && proxyResult.data) {
      const proxyData = JSON.parse(proxyResult.data);
      formData.value.proxyEnabled = proxyData.enabled;
      formData.value.proxyUrl = proxyData.url;
    }

    // 获取镜像设置
    const mirrorResult = await getSystemSetting('mirror');
    if (mirrorResult.code === 0 && mirrorResult.data) {
      const mirrorData = JSON.parse(mirrorResult.data);
      formData.value.mirrorUrls = mirrorData;
    }
  } catch (error) {
    console.error('加载设置失败:', error);
    MessagePlugin.error('加载设置失败');
  }
};

onMounted(() => {
  loadSettings();
});
</script>

<style lang="less" scoped>
.proxy-setting {
  padding: 20px;
  height: 100%;

  .custom-tabs {
    :deep(.t-tabs__nav) {
      margin-bottom: 24px;
    }

    :deep(.t-tabs__nav-item) {
      padding: 0 24px;
      height: 48px;
      line-height: 48px;
      font-size: 16px;
    }

    :deep(.t-tabs__nav-item.t-is-active) {
      font-weight: 500;
    }
  }

  .custom-form {
    max-width: 800px;
    margin: 0 auto;
    padding: 24px;

    :deep(.t-form__item) {
      margin-bottom: 24px;
    }

    :deep(.t-form__label) {
      width: 120px;
      text-align: right;
    }
  }

  .form-actions {
    display: flex;
    justify-content: center;
    margin-top: 32px;
  }

  :deep(.t-alert) {
    max-width: 800px;
    margin: 0 auto 24px;
  }
}

/* 添加代理测试结果样式 */
.proxy-test-result {
  padding: 16px;
}

.test-metrics {
  margin: 16px 0;
  padding: 16px;
  background-color: var(--td-bg-color-container);
  border-radius: 8px;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.metric-label {
  color: var(--td-text-color-secondary);
}

.metric-value {
  font-weight: 500;
  color: var(--td-brand-color);
  font-size: 18px;
}

.proxy-test-loading {
  padding: 32px;
  text-align: center;
}
</style>
