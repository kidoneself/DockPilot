<template>
  <div class="container-create">
    <NCard>
      <template #header>
        <div class="page-header">
          <div class="header-left">
            <NButton @click="router.back()">
              <template #icon>
                <NIcon><ArrowBackOutline /></NIcon>
              </template>
              返回
            </NButton>
            <span class="title">创建容器</span>
          </div>
        </div>
      </template>

      <n-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="left"
        label-width="120px"
        require-mark-placement="right-hanging"
      >
        <!-- 基本配置 -->
        <NCard title="基本配置" size="small" style="margin-bottom: 24px;">
          <n-form-item label="选择镜像" path="image">
            <n-select
              v-model:value="formData.image"
              placeholder="请选择镜像"
              :options="imageOptions"
              :loading="loadingImages"
              filterable
              style="width: 400px;"
              :disabled="isFromImagePage"
              @update:value="handleImageChange"
            />
            <template v-if="isFromImagePage" #feedback>
              <span style="color: var(--n-color-primary); font-size: 12px;">
                📋 已从镜像列表自动选择，如需更改请返回镜像列表页面
              </span>
            </template>
          </n-form-item>

          <n-form-item label="容器名称" path="name">
            <n-input v-model:value="formData.name" placeholder="选择镜像后自动生成" style="width: 300px;" />
            <template #feedback>
              <span style="color: var(--n-text-color-3); font-size: 12px;">
                系统已根据镜像名自动生成容器名称，您可以随时修改
              </span>
            </template>
          </n-form-item>

          <n-form-item label="重启策略" path="restartPolicy">
            <n-select
              v-model:value="formData.restartPolicy"
              :options="restartPolicyOptions"
              placeholder="选择重启策略"
              style="width: 200px;"
            />
          </n-form-item>
        </NCard>

        <!-- 网络配置 -->
        <NCard title="网络配置" size="small" style="margin-bottom: 24px;">
          <n-form-item label="端口映射" path="ports">
            <n-dynamic-input
              v-model:value="formData.ports"
              :on-create="onCreatePort"
              placeholder="端口映射"
            >
              <template #default="{ value }">
                <div style="display: flex; align-items: center; width: 100%; gap: 8px;">
                  <n-input
                    v-model:value="value.host"
                    placeholder="主机端口"
                    style="width: 120px;"
                    @blur="handlePortValidation"
                  />
                  <span>:</span>
                  <n-input
                    v-model:value="value.container"
                    placeholder="容器端口"
                    style="width: 120px;"
                    readonly
                  />
                  <n-select
                    v-model:value="value.protocol"
                    :options="protocolOptions"
                    style="width: 80px;"
                  />
                </div>
              </template>
              <template #create-button-default>
                添加端口映射
              </template>
            </n-dynamic-input>
            <template #feedback>
              <span style="color: var(--n-text-color-3); font-size: 12px;">
                容器端口已自动回填，您只需填写主机端口。格式：主机端口:容器端口
              </span>
            </template>
          </n-form-item>

          <n-form-item label="网络模式" path="networkMode">
            <n-select
              v-model:value="formData.networkMode"
              :options="networkOptions"
              :loading="loadingNetworks"
              placeholder="选择网络模式"
              style="width: 250px;"
            />
            <template #feedback>
              <span style="color: var(--n-text-color-3); font-size: 12px;">
                从现有Docker网络中选择，默认推荐使用bridge网络
              </span>
            </template>
          </n-form-item>
        </NCard>

        <!-- 存储配置 -->
        <NCard title="存储配置" size="small" style="margin-bottom: 24px;">
          <n-form-item label="挂载目录" path="volumes">
            <n-dynamic-input
              v-model:value="formData.volumes"
              :on-create="onCreateVolume"
              placeholder="挂载目录"
            >
              <template #default="{ value }">
                <div style="display: flex; align-items: center; width: 100%; gap: 8px;">
                  <div style="flex: 2;">
                    <PathSelector
                      v-model="value.hostPath"
                      placeholder="选择主机文件夹"
                      @update:model-value="handleVolumeValidation"
                    />
                  </div>
                  <span>:</span>
                  <n-input
                    v-model:value="value.containerPath"
                    placeholder="容器路径"
                    style="flex: 1;"
                  />
                  <n-checkbox v-model:checked="value.readOnly">只读</n-checkbox>
                </div>
              </template>
              <template #create-button-default>
                添加挂载目录
              </template>
            </n-dynamic-input>
            <template #feedback>
              <span style="color: var(--n-text-color-3); font-size: 12px;">
                容器路径支持自动回填和手动编辑，您可以选择主机路径并调整容器路径。格式：主机路径:容器路径
              </span>
            </template>
          </n-form-item>
        </NCard>

        <!-- 运行配置 -->
        <NCard title="运行配置" size="small" style="margin-bottom: 24px;">
          <n-form-item label="环境变量" path="env">
            <n-dynamic-input
              v-model:value="formData.env"
              :on-create="onCreateEnv"
              placeholder="环境变量"
            >
              <template #default="{ value }">
                <div style="display: flex; align-items: center; width: 500px; gap: 8px;">
                  <n-input
                    v-model:value="value.key"
                    placeholder="变量名"
                    style="width: 150px;"
                    @blur="handleEnvValidation"
                  />
                  <span>=</span>
                  <n-input
                    v-model:value="value.value"
                    placeholder="变量值"
                    style="width: 500px;"
                    @blur="handleEnvValidation"
                  />
                </div>
              </template>
              <template #create-button-default>
                添加环境变量
              </template>
            </n-dynamic-input>
          </n-form-item>

          <n-form-item label="CMD参数" path="cmd">
            <n-dynamic-input
              v-model:value="formData.cmd"
              :on-create="onCreateCommand"
              placeholder="启动命令的参数"
              style="width: 500px;"
              @update:value="handleCmdValidation"
            >
              <template #create-button-default>
                添加参数
              </template>
            </n-dynamic-input>
            <template #feedback>
              <span style="color: var(--n-text-color-3); font-size: 12px;">
                CMD参数会覆盖镜像默认的启动参数，例如：nginx -g "daemon off;"（无需要请勿填写）
              </span>
            </template>
          </n-form-item>

          <n-form-item label="特权模式" path="privileged">
            <div style="width: 100px;">
              <n-switch v-model:value="formData.privileged" />
            </div>
          </n-form-item>
        </NCard>
      </n-form>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="router.back()">取消</NButton>
          <NButton 
            type="primary" 
            :loading="creating"
            @click="handleSubmit"
          >
            创建容器
          </NButton>
        </NSpace>
      </template>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage, type FormInst } from 'naive-ui'
import { NButton, NCard, NIcon, NSpace } from 'naive-ui'
import { ArrowBackOutline } from '@vicons/ionicons5'
import { createContainer, getImageList, getImageDetail } from '@/api/container'
import { getNetworkList } from '@/api/network'
import type { Network } from '@/api/model/network'
import PathSelector from '@/components/common/PathSelector.vue'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const formRef = ref<FormInst | null>(null)

// 表单数据
const formData = reactive({
  name: '',
  image: '',
  restartPolicy: 'no',
  ports: [] as Array<{ host: string; container: string; protocol: string }>,
  networkMode: '',
  volumes: [] as Array<{ hostPath: string; containerPath: string; readOnly: boolean }>,
  env: [] as Array<{ key: string; value: string }>,
  cmd: [] as string[], // CMD参数，会覆盖镜像默认参数
  privileged: false
})

// 镜像选项
const imageOptions = ref<Array<{ label: string; value: string }>>([])
const loadingImages = ref(false)

// 网络选项
const networkOptions = ref<Array<{ label: string; value: string }>>([])
const loadingNetworks = ref(false)

// 选择选项
const restartPolicyOptions = [
  { label: '不重启', value: 'no' },
  { label: '总是重启', value: 'always' },
  { label: '除非手动停止', value: 'unless-stopped' },
  { label: '失败时重启', value: 'on-failure' }
]

const protocolOptions = [
  { label: 'TCP', value: 'tcp' },
  { label: 'UDP', value: 'udp' }
]

// 判断是否从镜像页面跳转过来
const isFromImagePage = computed(() => {
  return !!route.query.image
})

// 表单验证规则
const rules = {
  name: {
    required: true,
    message: '请输入容器名称',
    trigger: 'blur'
  },
  image: {
    required: true,
    message: '请选择镜像',
    trigger: 'change'
  },
  networkMode: {
    required: true,
    message: '请选择网络模式',
    trigger: 'change'
  },
  ports: {
    type: 'array',
    validator: (rule: any, value: any[]) => {
      if (!value || value.length === 0) return true // 端口映射可选
      
      const hostPorts = new Set()
      for (let i = 0; i < value.length; i++) {
        const port = value[i]
        
        // 检查主机端口
        if (port.host) {
          const hostPort = parseInt(port.host)
          if (isNaN(hostPort) || hostPort < 1 || hostPort > 65535) {
            return Promise.reject(`第${i + 1}个端口映射：主机端口必须是1-65535之间的数字`)
          }
          
          // 检查端口重复
          if (hostPorts.has(hostPort)) {
            return Promise.reject(`第${i + 1}个端口映射：主机端口${hostPort}重复`)
          }
          hostPorts.add(hostPort)
        }
        
        // 检查容器端口
        if (!port.container) {
          return Promise.reject(`第${i + 1}个端口映射：容器端口不能为空`)
        }
        const containerPort = parseInt(port.container)
        if (isNaN(containerPort) || containerPort < 1 || containerPort > 65535) {
          return Promise.reject(`第${i + 1}个端口映射：容器端口必须是1-65535之间的数字`)
        }
      }
      return true
    },
    trigger: 'blur'
  },
  volumes: {
    type: 'array',
    validator: (rule: any, value: any[]) => {
      if (!value || value.length === 0) return true // 挂载目录可选
      
      const containerPaths = new Set()
      for (let i = 0; i < value.length; i++) {
        const volume = value[i]
        
        // 检查主机路径
        if (!volume.hostPath) {
          return Promise.reject(`第${i + 1}个挂载目录：主机路径不能为空`)
        }
        
        // 检查主机路径格式（必须是绝对路径）
        if (!volume.hostPath.startsWith('/')) {
          return Promise.reject(`第${i + 1}个挂载目录：主机路径必须是绝对路径（以/开头）`)
        }
        
        // 检查容器路径
        if (!volume.containerPath) {
          return Promise.reject(`第${i + 1}个挂载目录：容器路径不能为空`)
        }
        
        // 检查容器路径格式（必须是绝对路径）
        if (!volume.containerPath.startsWith('/')) {
          return Promise.reject(`第${i + 1}个挂载目录：容器路径必须是绝对路径（以/开头）`)
        }
        
        // 检查容器路径重复
        if (containerPaths.has(volume.containerPath)) {
          return Promise.reject(`第${i + 1}个挂载目录：容器路径${volume.containerPath}重复`)
        }
        containerPaths.add(volume.containerPath)
      }
      return true
    },
    trigger: 'blur'
  },
  env: {
    type: 'array',
    validator: (rule: any, value: any[]) => {
      if (!value || value.length === 0) return true // 环境变量可选
      
      const envKeys = new Set()
      for (let i = 0; i < value.length; i++) {
        const env = value[i]
        
        // 检查变量名
        if (!env.key) {
          return Promise.reject(`第${i + 1}个环境变量：变量名不能为空`)
        }
        
        // 检查变量名格式（只能包含字母、数字、下划线）
        if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(env.key)) {
          return Promise.reject(`第${i + 1}个环境变量：变量名格式不正确（只能包含字母、数字、下划线，且不能以数字开头）`)
        }
        
        // 检查变量名重复
        if (envKeys.has(env.key)) {
          return Promise.reject(`第${i + 1}个环境变量：变量名${env.key}重复`)
        }
        envKeys.add(env.key)
        
        // 检查变量值（可以为空，但不能是undefined）
        if (env.value === undefined || env.value === null) {
          return Promise.reject(`第${i + 1}个环境变量：变量值不能为空`)
        }
      }
      return true
    },
    trigger: 'blur'
  },
  cmd: {
    type: 'array',
    validator: (rule: any, value: any[]) => {
      if (!value || value.length === 0) return true // CMD参数可选
      
      for (let i = 0; i < value.length; i++) {
        if (!value[i] || value[i].trim() === '') {
          return Promise.reject(`第${i + 1}个CMD参数不能为空`)
        }
      }
      return true
    },
    trigger: 'blur'
  }
}

// 获取镜像列表
const loadImageList = () => {
  loadingImages.value = true
  getImageList({
    onComplete: (msg) => {
      const images = msg.data
      if (Array.isArray(images)) {
        imageOptions.value = images.map((img: any) => ({
          label: `${img.name}:${img.tag}`,
          value: `${img.name}:${img.tag}`
        }))
      }
      loadingImages.value = false
      
      // 镜像列表加载完成后，检查路由参数
      checkRouteParams()
    },
    onError: (err) => {
      message.error('获取镜像列表失败: ' + err)
      loadingImages.value = false
    }
  })
}

// 检查路由参数并处理镜像自动填入
const checkRouteParams = () => {
  const imageParam = route.query.image as string
  if (imageParam) {
    // 设置镜像值
    formData.image = imageParam
    
    // 等待下一个tick确保DOM更新后再触发变化处理
    nextTick(() => {
      handleImageChange(imageParam)
    })
  }
}

// 获取网络列表
const loadNetworkList = () => {
  loadingNetworks.value = true
  getNetworkList({
    onComplete: (msg) => {
      const networks = msg.data as Network[]
      if (Array.isArray(networks)) {
        networkOptions.value = networks.map((network: Network) => {
          const nameStr = network.nameStr || network.name
          const displayLabel = nameStr === network.name 
            ? network.name 
            : `${nameStr} (${network.name})`
          
          return {
            label: displayLabel,
            value: network.name
          }
        })
        
        // 自动选择bridge网络作为默认值
        if (formData.networkMode === '' && networks.length > 0) {
          const bridgeNetwork = networks.find(n => n.name === 'bridge')
          if (bridgeNetwork) {
            formData.networkMode = bridgeNetwork.name
          } else {
            // 如果没有bridge网络，选择第一个网络
            formData.networkMode = networks[0].name
          }
        }
      }
      loadingNetworks.value = false
    },
    onError: (err) => {
      message.error('获取网络列表失败: ' + err)
      loadingNetworks.value = false
    }
  })
}

// 创建容器
const creating = ref(false)

// 自动生成容器名称
const generateContainerName = (imageName: string) => {
  // 提取镜像名（去掉registry和tag）
  let baseName = imageName
  
  // 去掉registry部分（如果存在）
  if (baseName.includes('/')) {
    baseName = baseName.split('/').pop() || baseName
  }
  
  // 去掉tag部分
  if (baseName.includes(':')) {
    baseName = baseName.split(':')[0]
  }
  
  // 清理名称，确保符合Docker命名规范
  baseName = baseName
    .toLowerCase() // 转为小写
    .replace(/[^a-z0-9-_.]/g, '-') // 非法字符替换为连字符
    .replace(/^[-.]+/, '') // 去掉开头的点和连字符
    .replace(/[-.]+$/, '') // 去掉结尾的点和连字符
    .replace(/[-_.]{2,}/g, '-') // 多个连续的分隔符替换为单个连字符
  
  // 确保名称不为空
  if (!baseName) {
    baseName = 'container'
  }
  
  // 生成3位随机数字
  const randomNum = Math.floor(Math.random() * 900) + 100 // 100-999
  
  // 组合生成最终名称
  const generatedName = `${baseName}-${randomNum}`
  
  // 设置到表单数据
  formData.name = generatedName
}

// 处理镜像选择变化
const handleImageChange = (selectedImage: string) => {
  if (!selectedImage) return
  
  // 自动生成容器名称
  generateContainerName(selectedImage)
  
  getImageDetail(selectedImage, {
    onComplete: (msg) => {
      const imageInfo = msg.data
      if (!imageInfo || !imageInfo.config) return

      // 自动回填端口映射
      if (imageInfo.config.exposedPorts) {
        const ports = Object.keys(imageInfo.config.exposedPorts).map(port => ({
          host: '', // 用户需要填写
          container: port.split('/')[0], // 自动填入容器端口
          protocol: port.split('/')[1] || 'tcp'
        }))
        formData.ports = ports
      }

      // 自动回填挂载目录
      if (imageInfo.config.volumes) {
        const volumes = Object.keys(imageInfo.config.volumes).map(vol => ({
          hostPath: '', // 用户需要填写
          containerPath: vol, // 自动填入容器路径
          readOnly: false
        }))
        formData.volumes = volumes
      }

      // 自动回填环境变量
      if (imageInfo.config.env) {
        const envVars = imageInfo.config.env.map((envStr: string) => {
          const [key, ...valueParts] = envStr.split('=')
          return {
            key,
            value: valueParts.join('=')
          }
        })
        formData.env = envVars
      }

      // 自动回填CMD参数
      if (imageInfo.config.cmd) {
        formData.cmd = [...imageInfo.config.cmd]
      }

      message.success(`镜像配置已自动回填，容器名称已设为: ${formData.name}`)
    },
    onError: (err) => {
      message.error('获取镜像详情失败: ' + err)
    }
  })
}

// 动态输入创建函数
const onCreatePort = () => ({
  host: '',
  container: '',
  protocol: 'tcp'
})

const onCreateVolume = () => ({
  hostPath: '',
  containerPath: '',
  readOnly: false
})

const onCreateEnv = () => ({
  key: '',
  value: ''
})

const onCreateCommand = () => ''

// 校验处理函数
const handlePortValidation = () => {
  formRef.value?.validate(['ports'])
}

const handleVolumeValidation = () => {
  formRef.value?.validate(['volumes'])
}

const handleEnvValidation = () => {
  formRef.value?.validate(['env'])
}

const handleCmdValidation = () => {
  formRef.value?.validate(['cmd'])
}

// 提交表单
const handleSubmit = async () => {
  try {
    // 表单验证
    await formRef.value?.validate()
    
    // 构建容器配置
    const containerConfig = {
      name: formData.name,
      image: formData.image,
      restartPolicy: formData.restartPolicy,
      networkMode: formData.networkMode,
      workingDir: '',
      privileged: formData.privileged,
      // 转换端口映射格式
      portMappings: formData.ports.map(p => ({
        hostPort: p.host,
        containerPort: p.container,
        protocol: p.protocol
      })),
      // 🔧 修复字段名：volumeMappings → volumeMounts（与后端模型保持一致）
      volumeMounts: formData.volumes.map(v => ({
        hostPath: v.hostPath,
        containerPath: v.containerPath,
        readOnly: v.readOnly
      })),
      // 转换环境变量格式
      environmentVariables: formData.env.map(e => ({
        key: e.key,
        value: e.value
      })),
      // CMD参数（覆盖镜像默认启动参数）
      command: formData.cmd
    }

    creating.value = true
    createContainer(containerConfig, {
      onComplete: () => {
        creating.value = false
        message.success('容器创建成功！')
        router.push('/containers')
      },
      onError: (err) => {
        creating.value = false
        
        // 检查是否是容器启动失败的情况（容器创建成功但启动失败）
        if (err.includes('容器创建成功但启动失败')) {
          // 尝试从错误信息中提取容器ID（如果可能的话）
          dialog.warning({
            title: '容器启动失败',
            content: `${err}\n\n容器已创建但启动失败，您可以前往编辑页面修改配置后重新启动。`,
            positiveText: '去编辑',
            negativeText: '稍后处理',
            onPositiveClick: () => {
              // 先跳转到容器列表，让用户找到失败的容器
              router.push('/containers')
              // 显示提示
              setTimeout(() => {
                message.info('请在列表中找到创建失败的容器，点击"编辑配置"来修改设置')
              }, 500)
            },
            onNegativeClick: () => {
              // 跳转到容器列表
              router.push('/containers')
            }
          })
        } else {
          // 普通的创建失败
          message.error('创建容器失败: ' + err)
        }
      }
    })
  } catch (validationErrors) {
    console.log('表单校验失败:', validationErrors)
    message.error('请检查表单输入，修正错误后重试')
  }
}

// 页面加载时获取镜像列表和网络列表
onMounted(() => {
  loadingImages.value = true
  loadingNetworks.value = true
  loadImageList()
  loadNetworkList()
})
</script>

<style scoped>
.container-create {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title {
  font-size: 18px;
  font-weight: bold;
  color: var(--n-text-color-base);
}

:deep(.n-card) {
  margin-bottom: 16px;
}

:deep(.n-card .n-card-header) {
  padding-bottom: 12px;
}

:deep(.n-form-item) {
  margin-bottom: 16px;
}
</style> 