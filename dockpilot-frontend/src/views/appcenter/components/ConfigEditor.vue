<template>
  <div class="config-editor">    
    <!-- 配置编辑区域 -->
    <div class="section">
      <!-- 编辑模式切换头部 -->
      <div class="section-header">
        <div class="header-left">
          <h3>应用配置</h3>
          <n-tag v-if="editMode === 'yaml'" type="warning" size="small">
            <template #icon><n-icon><CodeOutline /></n-icon></template>
            高级模式
          </n-tag>
        </div>
        
        <div class="header-right">
          <n-button-group>
            <n-button 
              :type="editMode === 'simple' ? 'primary' : 'default'"
              size="small"
              @click="switchMode('simple')"
            >
              <template #icon><n-icon><SettingsOutline /></n-icon></template>
              简单模式
            </n-button>
            <n-button 
              :type="editMode === 'yaml' ? 'primary' : 'default'"
              size="small"
              @click="switchMode('yaml')"
            >
              <template #icon><n-icon><CodeOutline /></n-icon></template>
              YAML编辑
            </n-button>
          </n-button-group>
          
          <!-- 展开/收起按钮 -->
          <n-button quaternary @click="toggleConfig">
            <n-icon size="20" :class="{ 'rotate-180': !configExpanded }">
              <ChevronDownOutline />
            </n-icon>
          </n-button>
        </div>
      </div>
      
      <!-- 配置内容 -->
      <div v-show="configExpanded" class="config-content">
        <!-- 简单模式编辑器 -->
        <SimpleEditor 
          v-show="editMode === 'simple'"
          :env-vars="envVarsFromYaml"
          @update-env="handleUpdateEnvVar"
        />
        
        <!-- YAML编辑器 -->
        <div v-show="editMode === 'yaml'" class="yaml-editor-container">
          <YamlEditor
            v-model:content="yamlContent"
            :original-yaml="originalYaml"
            height="650px"
            @change="handleYamlChange"
            @syntax-error="handleYamlError"
            @syntax-valid="handleYamlValid"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { 
  ChevronDownOutline, 
  CodeOutline, 
  SettingsOutline 
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import yaml from 'js-yaml'
// 组件导入
import SimpleEditor from './SimpleEditor.vue'
import YamlEditor from './YamlEditor.vue'

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
  yamlContent?: string
  originalYaml?: string
}

// 组件事件定义
interface Emits {
  'update:yamlContent': [content: string]
  yamlSyncError: [error: string]
}

// 定义props和emits
const props = withDefaults(defineProps<Props>(), {
  yamlContent: '',
  originalYaml: ''
})

const emit = defineEmits<Emits>()

// 组件状态
const message = useMessage()
const configExpanded = ref(true)
const editMode = ref<'simple' | 'yaml'>('simple')

// YAML内容的响应式绑定
const yamlContent = computed({
  get: () => props.yamlContent,
  set: (value) => emit('update:yamlContent', value)
})

// 🎯 核心功能：从YAML实时解析环境变量
const envVarsFromYaml = computed(() => {
  if (!yamlContent.value.trim()) {
    return []
  }
  
  try {
    const parsed = yaml.load(yamlContent.value) as any
    
    if (!parsed || !parsed.services) {
      return []
    }
    
    // 🎯 获取顶级 x-meta 元数据
    const meta = parsed['x-meta'] || {}
    const envMeta = meta.envVars || meta.env || {}
    
    // 如果没有定义 x-meta 中的环境变量，返回空数组
    if (Object.keys(envMeta).length === 0) {
      return []
    }
    
    // 获取第一个服务的环境变量配置
    const serviceKey = Object.keys(parsed.services)[0]
    const service = parsed.services[serviceKey]
    const environment = service?.environment || {}
    
    // 🔧 只解析 x-meta 中定义的环境变量
    const envVars: LocalEnvVarInfo[] = []
    
    Object.entries(envMeta).forEach(([key, metaInfo]: [string, any]) => {
      // 优先从 x-meta 中的 value 字段获取值，其次从 environment，最后使用默认值
      const actualValue = metaInfo.value || environment[key] || metaInfo.defaultValue || metaInfo.default || ''
      
      envVars.push({
        name: key,
        description: metaInfo.description || metaInfo.desc || key,
        value: String(actualValue),
        defaultValue: metaInfo.defaultValue || metaInfo.default || metaInfo.value || '',
        required: metaInfo.required || false,
        sensitive: metaInfo.sensitive || metaInfo.secret || false
      })
    })
    
    // 🎯 按 x-meta 中定义的顺序排序，如果没有定义则按字母顺序
    if (meta.envOrder && Array.isArray(meta.envOrder)) {
      envVars.sort((a, b) => {
        const aIndex = meta.envOrder.indexOf(a.name)
        const bIndex = meta.envOrder.indexOf(b.name)
        
        // 如果都在排序列表中，按列表顺序
        if (aIndex !== -1 && bIndex !== -1) {
          return aIndex - bIndex
        }
        // 如果只有一个在列表中，列表中的排在前面
        if (aIndex !== -1) return -1
        if (bIndex !== -1) return 1
        // 都不在列表中，按字母顺序
        return a.name.localeCompare(b.name)
      })
    } else {
      // 没有定义顺序，按字母顺序排序
      envVars.sort((a, b) => a.name.localeCompare(b.name))
    }
    
    return envVars
    
  } catch (error) {
    console.warn('⚠️ YAML解析失败，无法获取环境变量:', error)
    return []
  }
})

// 🎯 核心功能：更新YAML中的环境变量
const handleUpdateEnvVar = (name: string, value: string) => {
  if (!yamlContent.value.trim()) {
    console.warn('⚠️ YAML内容为空，无法更新环境变量')
    return
  }
  
  try {
    const parsed = yaml.load(yamlContent.value) as any
    
    if (!parsed || !parsed.services) {
      console.warn('⚠️ YAML格式错误，无法更新环境变量')
      return
    }
    
    // 获取第一个服务
    const serviceKey = Object.keys(parsed.services)[0]
    const service = parsed.services[serviceKey]
    
    if (!service) {
      console.warn('⚠️ 服务配置不存在')
      return
    }
    
    // 确保environment字段存在
    if (!service.environment) {
      service.environment = {}
    }
    
    // 🔧 更新service中的环境变量
    if (value !== undefined && value !== null && value !== '') {
      service.environment[name] = value
    } else {
      // 如果值为空，则删除该环境变量
      delete service.environment[name]
    }
    
    // 🎯 同时更新x-meta中的value字段（如果存在）
    if (parsed['x-meta'] && parsed['x-meta'].envVars && parsed['x-meta'].envVars[name]) {
      if (value !== undefined && value !== null && value !== '') {
        parsed['x-meta'].envVars[name].value = value
      } else {
        // 如果值为空，删除value字段或设为空
        delete parsed['x-meta'].envVars[name].value
      }
    } else if (parsed['x-meta'] && parsed['x-meta'].env && parsed['x-meta'].env[name]) {
      // 兼容env字段
      if (value !== undefined && value !== null && value !== '') {
        parsed['x-meta'].env[name].value = value
      } else {
        delete parsed['x-meta'].env[name].value
      }
    }
    
    // 转换回YAML字符串
    const updatedYaml = yaml.dump(parsed, {
      indent: 2,
      lineWidth: 120,
      noRefs: true,
      sortKeys: false
    })
    
    yamlContent.value = updatedYaml
    console.log(`✅ 环境变量 ${name} 已更新为: ${value}`)
    
  } catch (error) {
    console.error('❌ 更新环境变量失败:', error)
    emit('yamlSyncError', `更新失败: ${error}`)
  }
}

// 展开/收起切换
const toggleConfig = () => {
  configExpanded.value = !configExpanded.value
}

// 编辑模式切换
const switchMode = (mode: 'simple' | 'yaml') => {
  if (mode === editMode.value) return
  
  editMode.value = mode
  
  // 🎯 简化：不需要同步逻辑，因为都操作同一个yamlContent
  message.info(`已切换到${mode === 'simple' ? '简单' : 'YAML编辑'}模式`)
}

// 处理YAML内容变化
const handleYamlChange = (content: string) => {
  yamlContent.value = content
  // 🎯 不需要同步逻辑，envVarsFromYaml会自动响应变化
}

// 处理YAML语法错误
const handleYamlError = (error: any) => {
  console.warn('⚠️ YAML语法错误:', error)
  emit('yamlSyncError', `YAML语法错误: ${error.message}`)
}

// 处理YAML语法正确
const handleYamlValid = () => {
  // YAML语法正确，不需要特殊处理
}

// 监听模式变化，自动调整高度
watch(editMode, () => {
  nextTick(() => {
    // 触发布局更新
    window.dispatchEvent(new Event('resize'))
  })
})
</script>

<style scoped>
.config-editor {
  margin-bottom: 24px;
}

.section {
  background: var(--card-color);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 深色模式下的增强效果 */
[data-theme="dark"] .section {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.05);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--card-color-hover);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.header-left h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color-1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.section-header .n-icon {
  transition: transform 0.3s ease;
}

.section-header .n-icon.rotate-180 {
  transform: rotate(180deg);
}

.config-content {
  transition: all 0.3s ease;
  min-height: 200px;
}

.yaml-editor-container {
  padding: 20px 24px;
  min-height: 700px;
}

/* 模式切换按钮样式增强 */
.header-right .n-button-group {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  overflow: hidden;
}

[data-theme="dark"] .header-right .n-button-group {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .section-header {
    padding: 16px 20px;
    flex-direction: column;
    gap: 16px;
  }
  
  .header-left,
  .header-right {
    width: 100%;
    justify-content: center;
  }
  
  .yaml-editor-container {
    padding: 16px 20px;
    min-height: 550px;
  }
}

@media (max-width: 640px) {
  .section-header {
    padding: 12px 16px;
  }
  
  .header-left h3 {
    font-size: 16px;
  }
  
  .yaml-editor-container {
    padding: 12px 16px;
    min-height: 450px;
  }
  
  .header-right .n-button-group .n-button {
    font-size: 12px;
    padding: 0 8px;
  }
}
</style> 