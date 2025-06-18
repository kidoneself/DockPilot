<template>
  <div class="simple-editor">
    <div class="config-grid">
      <!-- 端口配置区域 -->
      <div v-if="portEnvs.length > 0" class="config-group">
        <h4 class="group-title">
                     <n-icon size="16" color="#2563eb"><GlobeOutline /></n-icon>
          端口配置
        </h4>
        <div class="port-grid">
          <div 
            v-for="env in portEnvs" 
            :key="env.name"
            class="config-item port-item"
          >
            <div class="config-header">
              <span class="config-label">{{ env.description || env.name }}</span>
              <div class="config-tags">
                <n-tag v-if="env.required" type="error" size="tiny">必填</n-tag>
                <n-tag type="info" size="tiny">端口</n-tag>
              </div>
            </div>
            
            <div class="config-input">
              <n-input 
                :value="env.value"
                :placeholder="env.defaultValue || '请输入端口号'"
                size="small"
                clearable
                @update:value="(value: string) => handleEnvVarChange(env, value)"
                @blur="handlePortCheck(env)"
              />
              <!-- 端口状态显示 -->
              <div v-if="portCheckStates[env.name]" class="port-status">
                <n-spin v-if="portCheckStates[env.name].checking" size="small" />
                <n-tag 
                  v-else-if="portCheckStates[env.name].available === false" 
                  type="error" 
                  size="tiny"
                >
                  端口被占用
                </n-tag>
                <n-tag 
                  v-else-if="portCheckStates[env.name].available === true" 
                  type="success" 
                  size="tiny"
                >
                  端口可用
                </n-tag>
              </div>
              <!-- 寻找可用端口按钮 -->
              <n-button 
                v-if="portCheckStates[env.name]?.available === false"
                size="tiny"
                type="primary"
                text
                :loading="findingPort[env.name]"
                @click="findAvailablePort(env)"
              >
                寻找可用端口
              </n-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 其他配置区域 -->
      <div v-if="otherEnvs.length > 0" class="config-group">
        <h4 class="group-title">
          <n-icon size="16" color="#059669"><SettingsOutline /></n-icon>
          其他配置
        </h4>
        <div class="other-grid">
          <div 
            v-for="env in otherEnvs" 
            :key="env.name"
            class="config-item"
            :class="{ 'other-item': !isPathEnv(env), 'path-item': isPathEnv(env) }"
          >
            <div class="config-header">
              <span class="config-label">{{ env.description || env.name }}</span>
              <div class="config-tags">
                <n-tag v-if="env.required" type="error" size="tiny">必填</n-tag>
                <n-tag v-if="isPathEnv(env)" type="warning" size="tiny">路径</n-tag>
              </div>
            </div>
            
            <div class="config-input">
              <PathSelector
                v-if="isPathEnv(env)"
                :model-value="env.value"
                :placeholder="env.defaultValue || '点击选择文件夹路径'"
                @update:model-value="(value: string) => handleEnvVarChange(env, value)"
              />
              <n-input 
                v-else
                :value="env.value"
                :placeholder="env.defaultValue || '请输入值'"
                :type="env.sensitive ? 'password' : 'text'"
                size="small"
                clearable
                @update:value="(value: string) => handleEnvVarChange(env, value)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { GlobeOutline, SettingsOutline } from '@vicons/ionicons5'
import PathSelector from '@/components/common/PathSelector.vue'
import { checkPort, findAvailablePorts } from '@/api/http/port'
import { useMessage } from 'naive-ui'

// 环境变量类型定义
interface LocalEnvVarInfo {
  name: string
  description: string
  value: string
  defaultValue: string
  required: boolean
  sensitive: boolean
}

// 组件属性定义
interface Props {
  envVars: LocalEnvVarInfo[]
}

// 组件事件定义
interface Emits {
  'update-env': [name: string, value: string]
}

// 定义props和emits
const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 组件状态
const message = useMessage()
const portCheckStates = ref<Record<string, { checking: boolean; available: boolean | null }>>({})
const findingPort = ref<Record<string, boolean>>({})

// 端口检测防抖
let portCheckTimeout: number | null = null

// 智能分组计算属性
const portEnvs = computed(() => {
  return props.envVars.filter(env => 
    env.name.toLowerCase().includes('port') || 
    env.description?.toLowerCase().includes('端口') ||
    env.description?.toLowerCase().includes('port')
  )
})

const otherEnvs = computed(() => {
  return props.envVars.filter(env => !portEnvs.value.includes(env))
})

// 判断是否为路径类型的环境变量
const isPathEnv = (env: LocalEnvVarInfo) => {
  const pathKeywords = ['path', 'dir', 'directory', 'folder', 'data', 'config', 'log', 'storage', 'volume', 'mount']
  const pathDescriptions = ['路径', '目录', '文件夹', '存储', '挂载', '地址']
  
  const nameMatch = pathKeywords.some(keyword => 
    env.name.toLowerCase().includes(keyword)
  )
  
  const descMatch = pathDescriptions.some(keyword =>
    env.description?.toLowerCase().includes(keyword)
  ) || pathKeywords.some(keyword =>
    env.description?.toLowerCase().includes(keyword)
  )
  
  return nameMatch || descMatch
}

// 验证端口号是否有效
const isValidPort = (port: string) => {
  const portNum = parseInt(port.trim())
  return !isNaN(portNum) && portNum > 0 && portNum <= 65535
}

// 处理端口检查
const handlePortCheck = async (env: LocalEnvVarInfo) => {
  if (!env.value || !isValidPort(env.value)) {
    delete portCheckStates.value[env.name]
    return
  }
  
  // 防抖处理
  if (portCheckTimeout) {
    clearTimeout(portCheckTimeout)
  }
  
  portCheckTimeout = window.setTimeout(async () => {
    await checkPortAvailability(env.name, parseInt(env.value))
  }, 500)
}

// 检查端口可用性
const checkPortAvailability = async (envName: string, port: number) => {
  try {
    portCheckStates.value[envName] = { checking: true, available: null }
    
    const result = await checkPort(port)
    
    portCheckStates.value[envName] = {
      checking: false,
      available: result
    }
  } catch (error) {
    console.error('端口检查失败:', error)
    portCheckStates.value[envName] = { checking: false, available: null }
  }
}

// 寻找可用端口
const findAvailablePort = async (env: LocalEnvVarInfo) => {
  try {
    findingPort.value[env.name] = true
    
    const currentPort = parseInt(env.value) || 8080
    const result = await findAvailablePorts(currentPort, currentPort + 100, 1)
    
    if (result.length > 0) {
      env.value = result[0].toString()
      message.success(`已为您找到可用端口: ${result[0]}`)
      
      // 更新端口状态
      portCheckStates.value[env.name] = {
        checking: false,
        available: true
      }
    } else {
      message.warning('未找到可用端口，请手动输入')
    }
  } catch (error) {
    console.error('寻找可用端口失败:', error)
    message.error('寻找可用端口失败')
  } finally {
    findingPort.value[env.name] = false
  }
}

// 处理环境变量值变化
const handleEnvVarChange = (envVar: LocalEnvVarInfo, newValue: string) => {
  // 🎯 直接发出更新事件，让父组件处理YAML更新
  emit('update-env', envVar.name, newValue)
  
  // 🔧 不再直接修改 envVar.value，因为它来自computed属性，是只读的
  // 父组件会更新YAML，然后computed会自动重新计算，界面会自动更新
}
</script>

<style scoped>
.simple-editor {
  padding: 20px 24px;
}

.config-grid {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.config-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color-1);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.port-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.other-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.config-item {
  padding: 20px;
  background: var(--card-color-hover);
  border-radius: 8px;
  border: 1px solid var(--border-color);
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.config-item:hover {
  border-color: #3b82f6;
  background: var(--bg-color-2);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.port-item {
  border-left: 4px solid #2563eb;
}

.path-item {
  border-left: 4px solid #f59e0b;
}

.other-item {
  border-left: 4px solid #059669;
}

.config-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.config-label {
  font-weight: 600;
  color: var(--text-color-1);
  font-size: 14px;
  line-height: 1.4;
  flex: 1;
}

.config-tags {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.config-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.port-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

/* 深色模式增强 */
[data-theme="dark"] .config-item {
  background: rgba(255, 255, 255, 0.02);
  border-color: rgba(255, 255, 255, 0.06);
}

[data-theme="dark"] .config-item:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

[data-theme="dark"] .group-title {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .port-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 1000px) {
  .other-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .simple-editor {
    padding: 20px;
  }
  
  .port-grid,
  .other-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .config-grid {
    gap: 20px;
  }
}

@media (max-width: 640px) {
  .simple-editor {
    padding: 16px;
  }
  
  .port-grid,
  .other-grid {
    gap: 12px;
  }
  
  .config-item {
    padding: 16px;
  }
}
</style> 