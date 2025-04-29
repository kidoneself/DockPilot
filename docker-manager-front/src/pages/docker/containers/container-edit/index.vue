<template>
  <div>
    <div class="form-step-container">
      <!-- 步骤条：显示当前创建容器的进度 -->
      <t-card :bordered="false">
        <t-steps :current="activeForm" class="step-container" status="process">
          <t-step-item content="基础配置" title="基础配置" />
          <t-step-item content="网络设置" title="网络配置" />
          <t-step-item content="存储设置" title="存储配置" />
          <t-step-item content="高级设置" title="高级配置" />
        </t-steps>
      </t-card>

      <!-- 第一步：基础配置表单 -->
      <div v-show="activeForm === 0" class="rule-tips">
        <t-alert :close="true" theme="info" title="配置说明">
          <template #message>
            <p>1. 选择要使用的容器镜像，注意版本的选择，镜像来自镜像列表，如果镜像列表中没有，请先添加镜像！</p>
            <p>2. 容器名和重启策略都为默认，如果有需要请自行设置容器名称和重启策略！</p>
          </template>
        </t-alert>
      </div>
      <t-form
        v-show="activeForm === 0"
        :data="formData"
        :rules="FORM_RULES"
        class="step-form"
        label-align="right"
        @submit="(result: SubmitContext) => onSubmit(result, 1)"
      >
        <!-- 镜像选择：从镜像列表中选择要使用的镜像 -->
        <t-form-item label="容器镜像" name="image">
          <t-tooltip content="选择要使用的容器镜像，会自动解析镜像配置" trigger="focus">
            <t-select
              v-model="formData.image"
              :loading="loading"
              :options="imageOptions"
              :style="{ width: '480px' }"
              class="demo-select-base"
              :disabled="hasImageParams || !!route.query.id"
              clearable
              @change="handleImageChange"
            >
              <template #option="{ option }">
                {{ option.label }}
              </template>
              <template #value="{ value }">
                {{ currentImageDisplay }}
              </template>
            </t-select>
          </t-tooltip>
        </t-form-item>
        <!-- 容器名称：设置容器的名称 -->
        <t-form-item label="容器名称" name="name">
          <t-tooltip content="设置容器名称，注意不要重复" trigger="focus">
            <t-input v-model="formData.name" :style="{ width: '480px' }" />
          </t-tooltip>
        </t-form-item>

        <!-- 重启策略：设置容器退出后的重启策略 -->
        <t-form-item label="重启策略" name="restartPolicy">
          <t-tooltip content="默认自动重启" trigger="focus">
            <t-select v-model="formData.restartPolicy" :style="{ width: '480px' }" class="demo-select-base" clearable>
              <t-option
                v-for="(item, index) in RESTART_POLICY_OPTIONS"
                :key="index"
                :value="item.value"
                :label="item.label"
              >
                {{ item.label }}
              </t-option>
            </t-select>
          </t-tooltip>
        </t-form-item>
        <t-form-item label="命令" name="cmd">
          <t-tooltip content="容器启动命令，小白不要做任何操作" trigger="focus">
            <t-textarea v-model="cmdText" placeholder="请输入命令，默认为空" :style="{ width: '480px' }" />
          </t-tooltip>
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" type="submit">下一步</t-button>
        </t-form-item>
      </t-form>

      <!-- 第二步：网络配置表单 -->
      <div v-show="activeForm === 1" class="rule-tips">
        <t-alert :close="true" theme="info" title="配置说明">
          <template #message>
            <p>1. 选择网络模式，默认是桥接模式，如果需要其他模式请自行选择！</p>
            <p>2. 配置网络参数，如果需要其他参数请自行设置！</p>
            <p>3. 设置端口映射，如果需要其他端口映射请自行设置！</p>
          </template>
        </t-alert>
      </div>
      <t-form
        v-show="activeForm === 1"
        :data="formData"
        :rules="FORM_RULES"
        class="step-form"
        label-align="left"
        @reset="onReset(0)"
        @submit="(result: SubmitContext) => onSubmit(result, 2)"
      >
        <!-- 网络模式：选择容器的网络模式 -->
        <t-form-item label="网络模式" name="networkMode">
          <t-tooltip content="默认使用桥接模式，可以自定义端口映射" trigger="focus">
            <t-select
              v-model="formData.networkMode"
              :style="{ width: '480px' }"
              class="demo-select-base"
              clearable
              @change="handleNetworkModeChange"
            >
              <t-option v-for="(item, index) in networkOptions" :key="index" :label="item.label" :value="item.value">
                {{ item.label }}
              </t-option>
            </t-select>
          </t-tooltip>
        </t-form-item>
        <!-- 容器内IP：设置容器的IP地址（仅在自定义网络模式下可用） -->
        <t-form-item v-if="showNetworkConfig" label="容器内IP" name="ipAddress">
          <t-input
            v-model="formData.ipAddress"
            :style="{ width: '480px' }"
            :disabled="formData.networkMode === 'bridge'"
            placeholder="可选，仅在自定义网络模式下需要"
          />
        </t-form-item>
        <!-- 网关：设置容器的网关（仅在自定义网络模式下可用） -->
        <t-form-item v-if="showNetworkConfig" label="网关" name="gateway">
          <t-input
            v-model="formData.gateway"
            :style="{ width: '480px' }"
            :disabled="formData.networkMode === 'bridge'"
            placeholder="可选，仅在自定义网络模式下需要"
          />
        </t-form-item>
        <!-- 端口映射：设置容器端口与主机端口的映射关系 -->
        <t-form-item label="端口映射">
          <t-space direction="vertical" style="width: 100%">
            <div v-for="(port, index) in formData.portMappings" :key="index" class="port-mapping">
              <t-space>
                <t-input-number
                  v-model="port.hostPort"
                  theme="normal"
                  align="right"
                  :disabled="disablePortMappings"
                  label="主机端口："
                ></t-input-number>
                <t-input-number
                  v-model="port.containerPort"
                  theme="normal"
                  align="right"
                  :disabled="disablePortMappings"
                  label="容器端口："
                ></t-input-number>
                <t-select v-model="port.protocol" style="width: 100px" :disabled="disablePortMappings">
                  <t-option value="tcp">TCP</t-option>
                  <t-option value="udp">UDP</t-option>
                </t-select>
                <t-button theme="danger" variant="text" :disabled="disablePortMappings" @click="removePort(index)">
                  <template #icon>
                    <t-icon name="delete" />
                  </template>
                </t-button>
              </t-space>
            </div>
            <t-button theme="default" variant="dashed" :disabled="disablePortMappings" @click="addPort">
              <template #icon>
                <t-icon name="add" />
              </template>
              添加端口映射
            </t-button>
          </t-space>
        </t-form-item>
        <t-form-item>
          <t-button theme="default" type="reset" variant="base">上一步</t-button>
          <t-button theme="primary" type="submit">下一步</t-button>
        </t-form-item>
      </t-form>

      <!-- 第三步：存储配置表单 -->
      <div v-show="activeForm === 2" class="rule-tips">
        <t-alert :close="true" theme="info" title="配置说明">
          <template #message>
            <p>1. 配置数据卷映射</p>
            <p>2. 设置挂载权限</p>
          </template>
        </t-alert>
      </div>
      <t-form
        v-show="activeForm === 2"
        :data="formData"
        :rules="FORM_RULES"
        class="step-form"
        label-align="left"
        @reset="onReset(1)"
        @submit="(result: SubmitContext) => onSubmit(result, 3)"
      >
        <!-- 数据卷映射：设置主机目录与容器目录的映射关系 -->
        <t-form-item label="数据卷映射">
          <t-space direction="vertical" style="width: 100%">
            <div v-for="(volume, index) in formData.volumeMappings" :key="index" class="volume-mapping">
              <t-space>
                <t-tooltip content="设置主机真实路径" trigger="focus">
                  <t-input v-model="volume.hostPath" placeholder="主机路径" style="width: 300px" />
                </t-tooltip>
                <t-tooltip content="设置容器内路径一般默认，可以自行增加" trigger="focus">
                  <t-input v-model="volume.containerPath" placeholder="容器路径" style="width: 200px" />
                </t-tooltip>
                <t-select v-model="volume.readOnly" style="width: 100px">
                  <t-option
                    v-for="option in VOLUME_PERMISSION_OPTIONS"
                    :key="String(option.value)"
                    :label="option.label"
                    :value="option.value"
                  />
                </t-select>
                <t-button theme="danger" variant="text" @click="removeVolume(index)">
                  <template #icon>
                    <t-icon name="delete" />
                  </template>
                </t-button>
              </t-space>
            </div>
            <t-button theme="default" variant="dashed" @click="addVolume">
              <template #icon>
                <t-icon name="add" />
              </template>
              添加路径映射
            </t-button>
          </t-space>
        </t-form-item>
        <t-form-item>
          <t-button theme="default" type="reset" variant="base">上一步</t-button>
          <t-button theme="primary" type="submit">下一步</t-button>
        </t-form-item>
      </t-form>

      <!-- 第四步：高级配置表单 -->
      <div v-show="activeForm === 3" class="rule-tips">
        <t-alert :close="true" theme="info" title="配置说明">
          <template #message>
            <p>1. 配置环境变量</p>
            <p>2. 设置资源限制</p>
            <p>3. 配置特权模式</p>
          </template>
        </t-alert>
      </div>
      <t-form
        v-show="activeForm === 3"
        :data="formData"
        :rules="FORM_RULES"
        class="step-form"
        label-align="left"
        @reset="onReset(2)"
        @submit="(result: SubmitContext) => onSubmit(result, 6)"
      >
        <!-- 环境变量：设置容器的环境变量 -->
        <t-form-item label="环境变量">
          <t-space direction="vertical" style="width: 100%">
            <div v-for="(env, index) in formData.environmentVariables" :key="index" class="env-variable">
              <t-space>
                <t-input v-model="env.key" placeholder="变量名" style="width: 200px" />
                <t-input v-model="env.value" placeholder="变量值" style="width: 200px" />
                <t-button theme="danger" variant="text" @click="removeEnv(index)">
                  <template #icon>
                    <t-icon name="delete" />
                  </template>
                </t-button>
              </t-space>
            </div>
            <t-button theme="default" variant="dashed" @click="addEnv">
              <template #icon>
                <t-icon name="add" />
              </template>
              添加环境变量
            </t-button>
          </t-space>
        </t-form-item>
        <!-- 特权模式：设置容器是否以特权模式运行 -->
        <t-form-item label="特权模式" name="privileged">
          <t-switch v-model="formData.privileged" />
        </t-form-item>
        <t-form-item>
          <t-button theme="default" type="reset" variant="base">上一步</t-button>
          <t-button :loading="creating" theme="primary" type="submit">更新容器</t-button>
        </t-form-item>
      </t-form>

      <!-- 完成页面：显示创建成功的提示 -->
      <div v-show="activeForm === 6" class="step-form-4">
        <t-space direction="vertical" style="align-items: center">
          <t-icon name="check-circle-filled" size="52px" style="color: green" />
          <p class="text">更新成功</p>
          <p class="tips">容器配置已更新，您可以查看容器详情或继续编辑</p>
          <div class="button-group">
            <t-button theme="primary" @click="onReset(0)">继续编辑</t-button>
            <t-button theme="default" variant="base" @click="complete">查看详情</t-button>
          </div>
        </t-space>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'ContainerConfig',
};
</script>

<script lang="ts" setup>
// 导入所需的类型和组件
import type { SelectOption, SelectValue, SelectValueChangeTrigger } from 'tdesign-vue-next';
import { MessagePlugin, SubmitContext } from 'tdesign-vue-next';
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

// 导入API和工具函数
import { createContainer, getImageDetail, getImageList, getNetworkList, getContainerDetail, updateContainer } from '@/api/container';
import router from '@/router';

// 导入常量和类型定义
import { IMAGE_OPTIONS, TYPE_OPTIONS, NETWORK_OPTIONS, RESTART_POLICY_OPTIONS, FORM_RULES } from '@/constants/container';
import type { ContainerForm, CreateContainerParams } from '@/types/container.d.ts';
import { mapFormDataToRequest, mapContainerDetailToForm } from '@/utils/container';

// 权限选项配置
const VOLUME_PERMISSION_OPTIONS = [
  { label: '只读', value: true },
  { label: '读写', value: false },
];

// 表单数据：使用ref创建响应式数据
const formData = ref<ContainerForm>({
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
});
const activeForm = ref(0); // 当前步骤
const loading = ref(false); // 加载状态
const imageOptions = ref<{ label: string; value: string }[]>([]); // 镜像选项
const networkOptions = ref<{ label: string; value: string; gateway?: string }[]>([]); // 网络选项
const creating = ref(false); // 创建状态

// 网络模式相关状态
const showNetworkConfig = ref(false); // 是否显示网络配置
const disablePortMappings = ref(false); // 是否禁用端口映射
const tempPortMappings = ref<Array<{ hostPort: string; containerPort: string; protocol: string; ip: string }>>([]); // 临时存储端口映射

// 路由相关
const route = useRoute();
const hasImageParams = computed(() => !!route.query.image && !!route.query.tag); // 是否有镜像参数
const containerId = ref<string>(''); // 容器ID

// 获取镜像列表
const fetchImageList = async () => {
  loading.value = true;
  try {
    const res = await getImageList();
    console.log('获取镜像列表响应:', res);
    if (res.code === 0) {
      imageOptions.value = res.data.map((img) => {
        const imageName = img.name || '未命名镜像';
        const imageTag = img.tag || 'latest';
        return {
          label: `${imageName}:${imageTag}`,
          value: `${imageName}:${imageTag}`,
        };
      });
    } else {
      MessagePlugin.error(String(res.message));
    }
  } catch (error) {
    console.error('获取镜像列表失败:', error);
    MessagePlugin.error('获取镜像列表失败');
  } finally {
    loading.value = false;
  }
};

// 获取网络列表
const fetchNetworkList = async () => {
  try {
    const res = await getNetworkList();
    if (res.code === 0) {
      networkOptions.value = res.data.map((network) => {
        const gateway = network.IPAM?.Config?.[0]?.Gateway || '';
        return {
          label: network.nameStr || '未知网络',
          value: network.Name || '未知网络',
          gateway,
        };
      });
    } else {
      MessagePlugin.error(String(res.message));
    }
  } catch (error) {
    console.error('获取网络列表错误:', error);
    MessagePlugin.error('获取网络列表失败');
  }
};

const fetchContainerDetail = async (id: string) => {
  try {
    console.log('开始获取容器详情，ID:', id);
    const res = await getContainerDetail(id);
    console.log('获取容器详情响应:', res);
    if (res.code === 0) {
      console.log('获取容器详情数据:', res.data);
      if (!res.data) {
        MessagePlugin.error('容器详情数据为空');
        return;
      }
      const mappedData = mapContainerDetailToForm(res.data);
      console.log('转换后的表单数据:', mappedData);
      formData.value = mappedData;
      console.log('设置后的表单数据:', formData.value);
    } else {
      MessagePlugin.error('获取容器详情失败');
    }
  } catch (error) {
    console.error('获取容器详情错误:', error);
    MessagePlugin.error(String(error instanceof Error ? error.message : '未知错误'));
  }
};

// 处理镜像选择变化
const handleImageChange = async (
  value: SelectValue<SelectOption>,
  context: {
    option?: SelectOption;
    selectedOptions: SelectOption[];
    trigger: SelectValueChangeTrigger;
    e?: KeyboardEvent | MouseEvent;
  },
) => {
  // 编辑模式下不允许修改镜像
  if (route.query.id) {
    MessagePlugin.warning('编辑模式下不允许修改镜像');
    return;
  }

  if (!value) {
    formData.value = { ...formData.value };
    return;
  }
  try {
    loading.value = true;
    const imageName = value as string;
    const res = await getImageDetail(imageName);
    if (res.code === 0) {
      const detail = res.data;
      formData.value = { ...formData.value };

      // 分割镜像名称和标签
      const [image, tag = 'latest'] = imageName.split(':');
      formData.value.image = image;
      formData.value.tag = tag;

      // 生成随机容器名称
      const randomNum = Math.floor(Math.random() * 90) + 10;
      formData.value.name = `${image.replace(/[^a-zA-Z0-9]/g, '-')}-${randomNum}`;
      formData.value.restartPolicy = 'always';

      if (detail) {
        const { config } = detail;

        // 设置入口点
        if (config.entrypoint) {
          formData.value.entrypoint = config.entrypoint;
        }

        // 设置默认命令
        if (config.cmd) {
          formData.value.cmd = config.cmd;
        }

        // 设置暴露端口
        if (config.exposedPorts) {
          formData.value.portMappings = Object.keys(config.exposedPorts).map((port) => {
            const [portNumber, protocol] = port.split('/');
            return {
              hostPort: portNumber,
              containerPort: portNumber,
              protocol: protocol || 'tcp',
              ip: '',
            };
          });
        }

        // 设置数据卷
        if (config.volumes) {
          formData.value.volumeMappings = Object.keys(config.volumes).map((volume) => ({
            hostPath: '',
            containerPath: volume,
            readOnly: false,
          }));
        }

        // 设置环境变量
        if (config.env) {
          formData.value.environmentVariables = config.env.map((env: string) => {
            const [key, value] = env.split('=');
            return { key, value };
          });
        }

        // 设置工作目录
        if (config.workingDir) {
          formData.value.workingDir = config.workingDir;
        }

        // 设置用户
        if (config.user) {
          formData.value.user = config.user;
        }

        // 设置健康检查
        if (config.healthcheck) {
          formData.value.healthcheck = {
            test: config.healthcheck.test || [],
            interval: config.healthcheck.interval || '30s',
            timeout: config.healthcheck.timeout || '10s',
            retries: config.healthcheck.retries || 3,
            startPeriod: config.healthcheck.startPeriod || '0s',
          };
        }

        // 显示镜像信息提示
        MessagePlugin.success(`已加载镜像 ${imageName} 的配置信息`);
      }
    } else {
      MessagePlugin.error(String(res.message));
    }
  } catch (error) {
    console.error('获取镜像详情失败:', error);
    MessagePlugin.error('获取镜像详情失败');
  } finally {
    loading.value = false;
  }
};

// 处理网络模式变化
const handleNetworkModeChange = async (
  value: SelectValue<SelectOption>,
  context: {
    option?: SelectOption;
    selectedOptions: SelectOption[];
    trigger: SelectValueChangeTrigger;
    e?: KeyboardEvent | MouseEvent;
  },
) => {
  const mode = value as string;
  formData.value.ipAddress = '';
  formData.value.gateway = '';

  if (mode === 'bridge') {
    disablePortMappings.value = false;
    showNetworkConfig.value = false;
    // 如果之前保存了端口映射，则恢复
    if (tempPortMappings.value.length > 0) {
      formData.value.portMappings = [...tempPortMappings.value];
      tempPortMappings.value = [];
    }
  } else if (mode === 'host' || mode === 'none') {
    // 保存当前的端口映射配置
    if (formData.value.portMappings.length > 0) {
      tempPortMappings.value = formData.value.portMappings.map(port => ({
        ...port,
        ip: port.ip ?? ''
      }));
    }
    disablePortMappings.value = true;
    showNetworkConfig.value = false;
    formData.value.portMappings = [];
  } else {
    disablePortMappings.value = false;
    showNetworkConfig.value = true;
    // 如果之前保存了端口映射，则恢复
    if (tempPortMappings.value.length > 0) {
      formData.value.portMappings = [...tempPortMappings.value];
      tempPortMappings.value = [];
    }
    const selectedNetwork = networkOptions.value.find((n) => n.value === mode);
    if (selectedNetwork?.gateway) {
      formData.value.gateway = selectedNetwork.gateway;
    }
  }
};

// 添加端口映射
const addPort = () => {
  formData.value.portMappings.push({
    hostPort: '',
    containerPort: '',
    protocol: 'tcp',
    ip: '',
  });
};

// 移除端口映射
const removePort = (index: number) => {
  formData.value.portMappings.splice(index, 1);
};

// 添加数据卷映射
const addVolume = () => {
  formData.value.volumeMappings.push({
    hostPath: '',
    containerPath: '',
    readOnly: false,
  });
};

// 移除数据卷映射
const removeVolume = (index: number) => {
  formData.value.volumeMappings.splice(index, 1);
};

// 添加环境变量
const addEnv = () => {
  formData.value.environmentVariables.push({
    key: '',
    value: '',
  });
};

// 移除环境变量
const removeEnv = (index: number) => {
  formData.value.environmentVariables.splice(index, 1);
};

// 更新容器
const handleUpdateContainer = async () => {
  try {
    creating.value = true;
    const request = mapFormDataToRequest(formData.value);
    const res = await updateContainer(route.query.id as string, request);
    if (res.code !== 0) {
      MessagePlugin.error('更新容器失败');
      return;
    }
    MessagePlugin.success('更新容器成功');
    containerId.value = route.query.id as string;
    activeForm.value = 6;
    setTimeout(() => {
      router.push(`/docker/containers/detail?id=${containerId.value}`);
    }, 2000);
  } catch (error) {
    MessagePlugin.error('更新容器失败');
  } finally {
    creating.value = false;
  }
};

// 表单提交处理
const onSubmit = (result: SubmitContext, val: number) => {
  if (result.validateResult === true) {
    if (val === 6) {
      handleUpdateContainer();
    } else {
      // 如果是从基础配置进入网络配置，且网络模式未设置，则使用默认值
      if (val === 1 && !formData.value.networkMode) {
        formData.value.networkMode = 'bridge';
      }
      activeForm.value = val;
    }
  }
};

// 重置表单
const onReset = (val: number) => {
  activeForm.value = val;
};

// 完成创建
const complete = () => {
  if (containerId.value) {
    router.push(`/docker/containers/detail?id=${containerId.value}`);
  } else {
    router.push('/docker/containers');
  }
};

// 组件挂载时初始化
onMounted(async () => {
  if (route.query.id) {
    // 如果有容器ID，获取容器详情
    await fetchContainerDetail(route.query.id as string);
  } else {
    // 否则加载镜像列表（这种情况应该不会发生，因为编辑页面必须要有容器ID）
    MessagePlugin.error('缺少容器ID参数');
    router.push('/docker/containers');
  }
  fetchNetworkList();
});

const cmdText = computed({
  get: () => (formData.value.cmd || []).join(' '),
  set: (value: string) => {
    formData.value.cmd = value.split(' ').filter(Boolean);
  }
});

// 添加一个计算属性，用于显示当前选中的镜像
const currentImageDisplay = computed(() => {
  if (!formData.value.image) return '';
  return formData.value.tag ? `${formData.value.image}:${formData.value.tag}` : formData.value.image;
});
</script>

<style lang="less" scoped>
@import './index.less';

// 端口映射和数据卷映射的样式
.port-mapping,
.volume-mapping {
  position: relative;
  margin-bottom: 16px;
}
</style>
