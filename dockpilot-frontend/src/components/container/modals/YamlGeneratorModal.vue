<template>
  <NModal
    v-model:show="showModal"
    preset="card"
    title="生成YAML配置"
    class="yaml-modal"
    style="width: 90%; max-width: 1000px;"
  >
    <!-- 基础信息配置 -->
    <NForm
      ref="yamlFormRef"
      :model="yamlForm"
      label-placement="left"
      label-width="100px"
      style="margin-bottom: 16px;"
    >
      <NFormItem label="项目名称">
        <NInput
          v-model:value="yamlForm.projectName"
          placeholder="容器项目名称"
          style="width: 300px;"
        />
      </NFormItem>
      <NFormItem label="项目描述">
        <NInput
          v-model:value="yamlForm.description"
          placeholder="项目描述（可选）"
          style="width: 300px;"
        />
      </NFormItem>
    </NForm>

    <!-- 环境变量配置 -->
    <div v-if="previewEnvVars.length > 0" style="margin-bottom: 20px;">
      <h3>📝 环境变量说明配置</h3>
      <NAlert type="info" style="margin-bottom: 16px;">
        为环境变量添加说明，方便以后使用时理解每个配置的作用（可选）
      </NAlert>
      <div class="env-config-list">
        <div v-for="env in previewEnvVars" :key="env.name" class="env-config-item">
          <div class="env-config-info">
            <code class="env-name">{{ env.name }}</code>
            <span class="env-value">{{ env.value }}</span>
          </div>
          <NInput
            v-model:value="env.description"
            placeholder="可选：添加说明，例如'Emby访问端口'、'数据存储目录'等"
            style="flex: 1; margin-left: 12px;"
            size="small"
          />
        </div>
      </div>
    </div>

    <NSpace style="margin-bottom: 16px;">
      <NButton type="primary" :loading="generatingYaml" @click="generateYamlContent">
        生成完整YAML
      </NButton>
    </NSpace>

    <div v-if="yamlResult">
      <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
        <NText strong>YAML 配置内容：</NText>
        <NSpace>
          <NButton 
            size="small" 
            :type="editableMode ? 'warning' : 'default'"
            @click="editableMode = !editableMode"
          >
            <template #icon>
              <n-icon><CreateOutline /></n-icon>
            </template>
            {{ editableMode ? '退出编辑' : '编辑YAML' }}
          </NButton>
          <NButton size="small" @click="copyYamlContent">
            <template #icon>
              <n-icon><CopyOutline /></n-icon>
            </template>
            复制
          </NButton>
          <NDropdown
            trigger="click"
            :options="downloadOptions"
            @select="handleDownloadSelect"
          >
            <NButton size="small">
              <template #icon>
                <n-icon><DownloadOutline /></n-icon>
              </template>
              下载 ▼
            </NButton>
          </NDropdown>
        </NSpace>
      </div>
      
      <!-- 可编辑模式 -->
      <div v-if="editableMode" style="margin-bottom: 16px;">
        <NAlert type="info" style="margin-bottom: 12px;">
          <template #icon>
            <n-icon><InformationCircleOutline /></n-icon>
          </template>
          编辑模式：您可以直接修改YAML内容。请注意保持正确的YAML语法格式。
        </NAlert>
        
        <!-- 编辑状态指示 -->
        <div 
          class="yaml-edit-status" 
          :class="hasUnsavedChanges ? 'has-changes' : 'no-changes'"
        >
          <n-icon v-if="hasUnsavedChanges">
            <RefreshOutline />
          </n-icon>
          <n-icon v-else>
            <CheckmarkCircleOutline />
          </n-icon>
          <span>
            {{ hasUnsavedChanges ? '有未保存的修改' : '内容已同步' }}
          </span>
          <span style="margin-left: auto; color: #999;">
            字符数: {{ editableYamlContent.length }}
          </span>
        </div>
        
        <NInput
          v-model:value="editableYamlContent"
          type="textarea"
          placeholder="请输入YAML内容..."
          :rows="20"
          style="font-family: 'Monaco', 'Consolas', monospace; font-size: 13px;"
          show-count
        />
        
        <div style="margin-top: 8px; display: flex; gap: 8px; flex-wrap: wrap;">
          <NButton
size="small"
type="primary"
:disabled="!hasUnsavedChanges"
@click="applyYamlChanges">
            <template #icon>
              <n-icon><CheckmarkOutline /></n-icon>
            </template>
            应用修改
          </NButton>
          <NButton size="small" :disabled="!hasUnsavedChanges" @click="resetYamlChanges">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
            重置修改
          </NButton>
          <NButton size="small" @click="validateYamlSyntax">
            <template #icon>
              <n-icon><CheckmarkCircleOutline /></n-icon>
            </template>
            验证语法
          </NButton>
          <NButton size="small" type="default" @click="insertTemplate">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            插入模板
          </NButton>
        </div>
        
        <!-- 编辑提示 -->
        <div class="yaml-edit-tips">
          <h5>💡 编辑提示</h5>
          <ul>
            <li>使用2个空格进行缩进，不要使用Tab键</li>
            <li>冒号后面必须有空格: <code>key: value</code></li>
            <li>字符串值建议用双引号包围: <code>"值"</code></li>
            <li>列表项前面用破折号和空格: <code>- item</code></li>
            <li>可以使用Ctrl+Z撤销，Ctrl+Y重做</li>
          </ul>
        </div>
      </div>
      
      <!-- 只读模式 -->
      <NCode 
        v-else
        :code="yamlResult.yamlContent" 
        language="yaml"
        style="max-height: 400px; overflow-y: auto;"
      />
      
      <div style="margin-top: 16px;">
        <NText depth="3">
          包含容器数量：{{ yamlResult.containerCount }} | 
          生成时间：{{ yamlResult.generateTime }}
          <span v-if="editableMode && hasUnsavedChanges" style="color: orange; margin-left: 8px;">
            • 有未保存的修改
          </span>
        </NText>
      </div>
    </div>

    <template #action>
      <NSpace>
        <NButton @click="handleCloseModal">关闭</NButton>
        <NButton v-if="yamlResult" type="primary" @click="saveAsApplication">
          保存为应用
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick, h } from 'vue'
import { NModal, NForm, NFormItem, NInput, NButton, NSpace, NText, NAlert, NCode, NDropdown, useMessage, useDialog, type FormInst } from 'naive-ui'
import {
  CreateOutline,
  CopyOutline,
  DownloadOutline,
  InformationCircleOutline,
  CheckmarkOutline,
  RefreshOutline,
  CheckmarkCircleOutline,
  AddOutline
} from '@vicons/ionicons5'
import { useRouter } from 'vue-router'
import { generateContainerYaml, previewContainerYaml, type ContainerYamlResponse } from '@/api/containerYaml'

// Props & Emits
interface Props {
  show: boolean
  selectedContainers: Set<string>
}

interface Emits {
  (e: 'update:show', value: boolean): void
  (e: 'download-project', yamlResult: ContainerYamlResponse): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

// 状态管理
const showModal = computed({
  get: () => props.show,
  set: (value) => emit('update:show', value)
})

const generatingYaml = ref(false)
const yamlResult = ref<ContainerYamlResponse | null>(null)
const yamlForm = reactive({
  projectName: '',
  description: ''
})
const yamlFormRef = ref<FormInst | null>(null)

// YAML编辑相关状态
const editableMode = ref(false)
const editableYamlContent = ref('')
const hasUnsavedChanges = ref(false)
const originalYamlContent = ref('')

// 环境变量预览状态
const previewEnvVars = ref<Array<{
  name: string
  value: string
  description: string
}>>([])

// 监听模态框打开
watch(() => props.show, async (newVal) => {
  if (newVal) {
    // 重置表单和结果
    yamlForm.projectName = `容器项目-${new Date().getTime()}`
    yamlForm.description = ''
    yamlResult.value = null
    previewEnvVars.value = []
    
    // 重置编辑模式状态
    editableMode.value = false
    editableYamlContent.value = ''
    hasUnsavedChanges.value = false
    originalYamlContent.value = ''
    
    // 加载环境变量预览
    await loadPreviewEnvVars()
  }
})

// 获取环境变量预览数据
async function loadPreviewEnvVars() {
  try {
    // 先生成一个预览YAML来获取环境变量
    const response = await previewContainerYaml({
      containerIds: Array.from(props.selectedContainers),
      projectName: yamlForm.projectName,
      description: yamlForm.description,
      excludeFields: ['environment'] // 排除敏感信息但保留环境变量结构
    })
    
    if (response.success) {
      // 解析YAML内容，提取环境变量
      const yamlLines = response.yamlContent.split('\n')
      const envVars: Array<{name: string, value: string, description: string}> = []
      let inEnvSection = false
      
      for (let i = 0; i < yamlLines.length; i++) {
        const line = yamlLines[i].trim()
        if (line === 'env:') {
          inEnvSection = true
          continue
        }
        if (inEnvSection && line && !line.startsWith(' ')) {
          inEnvSection = false
        }
        if (inEnvSection && line.includes(':')) {
          const envName = line.split(':')[0].trim()
          const envValue = line.split(':')[1]?.trim()?.replace(/"/g, '') || ''
          if (envName && !envVars.find(e => e.name === envName)) {
            envVars.push({
              name: envName,
              value: envValue,
              description: ''
            })
          }
        }
      }
      
      previewEnvVars.value = envVars
    }
  } catch (error) {
    console.warn('预览环境变量失败:', error)
    previewEnvVars.value = []
  }
}

// 生成完整YAML
async function generateYamlContent() {
  try {
    generatingYaml.value = true
    
    // 收集用户配置的环境变量描述
    const envDescriptions: Record<string, string> = {}
    previewEnvVars.value.forEach(env => {
      if (env.description && env.description.trim()) {
        envDescriptions[env.name] = env.description
      }
    })
    
    const response = await generateContainerYaml({
      containerIds: Array.from(props.selectedContainers),
      projectName: yamlForm.projectName,
      description: yamlForm.description,
      envDescriptions: envDescriptions
    })
    
    if (response.success) {
      yamlResult.value = response
      // 初始化编辑状态
      originalYamlContent.value = response.yamlContent
      message.success('YAML生成成功')
    } else {
      message.error(response.message || 'YAML生成失败')
    }
  } catch (error: any) {
    message.error('生成YAML失败: ' + (error.message || error))
  } finally {
    generatingYaml.value = false
  }
}

// 复制YAML内容
function copyYamlContent() {
  if (!yamlResult.value) return
  
  navigator.clipboard.writeText(yamlResult.value.yamlContent).then(() => {
    message.success('YAML内容已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败，请手动复制')
  })
}

// 下载YAML文件
function downloadYamlFile() {
  if (!yamlResult.value) return
  
  const blob = new Blob([yamlResult.value.yamlContent], { type: 'text/yaml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${yamlResult.value.projectName || 'docker-compose'}.yml`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  message.success('YAML文件下载完成')
}

// 下载项目包
function downloadProjectPackage() {
  if (!yamlResult.value) return
  emit('download-project', yamlResult.value)
}

// 保存为应用（跳转到应用中心）
function saveAsApplication() {
  if (!yamlResult.value) return
  
  // 跳转到应用中心并预填充导入表单
  router.push({
    path: '/appcenter',
    query: {
      mode: 'import',
      yaml: encodeURIComponent(yamlResult.value.yamlContent),
      name: encodeURIComponent(yamlResult.value.projectName)
    }
  })
  
  // 关闭当前弹窗
  showModal.value = false
  message.success('已跳转到应用中心，请完善应用信息后导入')
}

// 监听编辑内容变化
watch(editableYamlContent, (newValue) => {
  if (originalYamlContent.value && newValue !== originalYamlContent.value) {
    hasUnsavedChanges.value = true
  } else {
    hasUnsavedChanges.value = false
  }
})

// 监听编辑模式切换
watch(editableMode, (newMode) => {
  if (newMode && yamlResult.value) {
    // 进入编辑模式，初始化编辑内容
    editableYamlContent.value = yamlResult.value.yamlContent
    originalYamlContent.value = yamlResult.value.yamlContent
    hasUnsavedChanges.value = false
  }
})

// YAML编辑相关方法
function applyYamlChanges() {
  if (!yamlResult.value) return
  
  // 应用修改
  yamlResult.value.yamlContent = editableYamlContent.value
  originalYamlContent.value = editableYamlContent.value
  hasUnsavedChanges.value = false
  message.success('YAML修改已应用')
}

function resetYamlChanges() {
  if (!originalYamlContent.value) return
  
  // 重置为原始内容
  editableYamlContent.value = originalYamlContent.value
  hasUnsavedChanges.value = false
  message.success('YAML已重置为原始内容')
}

function validateYamlSyntax() {
  if (!editableYamlContent.value) {
    message.warning('请输入YAML内容')
    return
  }
  
  try {
    // 基本的YAML语法检查
    const lines = editableYamlContent.value.split('\n')
    let hasErrors = false
    const errors: string[] = []
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i]
      const lineNum = i + 1
      
      // 检查缩进（应该是2或4的倍数）
      if (line.length > 0 && line[0] === ' ') {
        const leadingSpaces = line.match(/^ */)?.[0].length || 0
        if (leadingSpaces % 2 !== 0) {
          errors.push(`第${lineNum}行: 缩进应该是2的倍数`)
          hasErrors = true
        }
      }
      
      // 检查冒号后是否有空格
      if (line.includes(':') && !line.includes(': ') && !line.endsWith(':')) {
        const colonIndex = line.indexOf(':')
        if (colonIndex < line.length - 1 && line[colonIndex + 1] !== ' ') {
          errors.push(`第${lineNum}行: 冒号后应该有空格`)
          hasErrors = true
        }
      }
    }
    
    if (hasErrors) {
      message.error('YAML语法检查发现问题:\n' + errors.join('\n'))
    } else {
      message.success('YAML语法验证通过')
    }
  } catch (error) {
    message.error('YAML语法验证失败: ' + (error as Error).message)
  }
}

function handleCloseModal() {
  if (hasUnsavedChanges.value) {
    dialog.warning({
      title: '未保存的修改',
      content: '您有未保存的修改，确定要关闭吗？',
      positiveText: '确定',
      negativeText: '取消',
      maskClosable: false,
      closeOnEsc: false,
      onPositiveClick: () => {
        showModal.value = false
      }
    })
  } else {
    showModal.value = false
  }
}

function insertTemplate() {
  const templates = [
    {
      name: '新服务模板',
      content: `
  new-service:
    image: nginx:latest
    ports:
      - "8080:80"
    environment:
      - ENV_VAR=value
    volumes:
      - ./data:/app/data
    restart: unless-stopped`
    },
    {
      name: '数据库服务',
      content: `
  database:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: "password"
      MYSQL_DATABASE: "app_db"
    volumes:
      - db_data:/var/lib/mysql
    restart: unless-stopped`
    },
    {
      name: '环境变量配置',
      content: `
    environment:
      - NEW_VAR=value
      - ANOTHER_VAR=another_value`
    }
  ]
  
  // 简化版本，使用字符串选择
  const templateNames = templates.map(t => t.name).join('\n')
  
  dialog.info({
    title: '插入YAML模板',
    content: `可用的模板：\n\n${templateNames}\n\n请在下面的按钮中选择要插入的模板。`,
    action: () => {
      return h('div', { style: 'display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px;' }, 
        templates.map(template => 
          h('button', {
            style: 'padding: 6px 12px; border: 1px solid #ddd; border-radius: 4px; background: #f8f9fa; cursor: pointer;',
            onClick: () => {
              editableYamlContent.value += template.content
              message.success(`已插入${template.name}模板`)
            }
          }, template.name)
        )
      )
    }
  })
}

// 下载选项
const downloadOptions = [
  {
    label: '只下载YAML',
    key: 'yaml'
  },
  {
    label: '同时打包配置文件',
    key: 'all'
  }
]

// 处理下载选择
function handleDownloadSelect(key: string) {
  if (key === 'yaml') {
    downloadYamlFile()
  } else if (key === 'all') {
    downloadProjectPackage()
  }
}
</script>

<style scoped>
/* YAML模态框样式 */
:deep(.yaml-modal .n-card) {
  max-height: 80vh;
  overflow-y: auto;
}

.yaml-edit-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 12px;
}

.yaml-edit-status.has-changes {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  color: #856404;
}

.yaml-edit-status.no-changes {
  background: #d4edda;
  border: 1px solid #c3e6cb;
  color: #155724;
}

/* 编辑提示样式 */
.yaml-edit-tips {
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid #007bff;
}

.yaml-edit-tips h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #495057;
}

.yaml-edit-tips ul {
  margin: 0;
  padding-left: 20px;
  color: #6c757d;
  font-size: 12px;
}

.yaml-edit-tips li {
  margin-bottom: 4px;
}

/* 环境变量配置样式 */
.env-config-list {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e0e0e6;
  border-radius: 6px;
  padding: 12px;
  background-color: #fafafa;
}

.env-config-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.env-config-item:last-child {
  border-bottom: none;
}

.env-config-info {
  min-width: 300px;
  margin-right: 12px;
}

.env-name {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  color: #666;
  display: inline-block;
  margin-right: 8px;
}

.env-value {
  color: #2080f0;
  font-size: 12px;
  font-weight: 500;
}
</style> 