<template>
  <div class="template-store">
    <!-- 顶部标题栏 -->
    <div class="store-header">
      <div class="header-left">
        <h1>应用中心</h1>
        <span class="template-count">{{ templates.length }} 个应用</span>
      </div>
      <div class="header-actions">
        <n-input 
          v-model:value="searchText" 
          placeholder="搜索应用..."
          style="width: 280px;"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <n-icon><SearchOutline /></n-icon>
          </template>
        </n-input>
        <n-button type="primary" size="medium" @click="handleImport">
          <template #icon>
            <n-icon><AddOutline /></n-icon>
          </template>
          导入应用
        </n-button>
      </div>
    </div>

    <!-- 分类标签 -->
    <div class="category-tabs">
      <n-button 
        :type="selectedCategory === null ? 'primary' : 'tertiary'"
        @click="() => { selectedCategory = null; handleCategoryChange() }"
        size="small"
      >
        全部
      </n-button>
      <n-button 
        v-for="category in categories"
        :key="category"
        :type="selectedCategory === category ? 'primary' : 'tertiary'"
        @click="() => { selectedCategory = category; handleCategoryChange() }"
        size="small"
      >
        {{ category }}
      </n-button>
    </div>

    <!-- 模板列表 -->
    <div class="template-list">
      <div 
        v-for="template in filteredTemplates" 
        :key="template.id"
        class="template-item"
      >
        <div class="template-icon">
          <img :src="template.iconUrl || template.icon" :alt="template.name" />
        </div>
        <div class="template-content">
          <div class="template-header">
            <h3 class="template-name">{{ template.name }}</h3>
            <div class="template-badges">
              <n-tag 
                :type="template.type === '官方模板' ? 'success' : 'info'" 
                size="small"
              >
                {{ template.type }}
              </n-tag>
              <n-tag type="default" size="small">
                {{ template.deployCount }}次部署
              </n-tag>
            </div>
          </div>
          <p class="template-description">{{ template.description }}</p>
          <div class="template-meta">
            <span class="meta-item">
              <n-icon><CubeOutline /></n-icon>
              {{ template.services }}个服务
            </span>
            <span class="meta-item">
              <n-icon><CalendarOutline /></n-icon>
              {{ template.createdAt }}
            </span>
            <span class="meta-item">
              <n-icon><FolderOutline /></n-icon>
              {{ template.category }}
            </span>
          </div>
        </div>
        <div class="template-actions">
          <n-button type="primary" @click="handleDeploy(template)">
            立即部署
          </n-button>
          <n-dropdown trigger="click" :options="getActionOptions(template)" @select="(key: any) => handleAction(key, template)">
            <n-button quaternary circle>
              <template #icon>
                <n-icon><EllipsisVerticalOutline /></n-icon>
              </template>
            </n-button>
          </n-dropdown>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="filteredTemplates.length === 0" class="empty-state">
      <n-empty description="没有找到匹配的应用" />
    </div>

    <!-- 模板详情弹窗 -->
    <n-modal v-model:show="showDetail" style="width: 90%; max-width: 600px;">
      <n-card 
        :bordered="false"
        size="huge"
        role="dialog"
        aria-modal="true"
        closable
        @close="showDetail = false"
      >
        <div v-if="selectedTemplate" class="template-detail">
          <!-- 模板头部 -->
          <div class="detail-header">
            <img :src="selectedTemplate.iconUrl || selectedTemplate.icon" :alt="selectedTemplate.name" class="detail-icon" />
            <div class="detail-info">
              <h2 class="detail-title">{{ selectedTemplate.name }}</h2>
              <p class="detail-desc">{{ selectedTemplate.description }}</p>
              <div class="detail-meta">
                <n-tag :type="selectedTemplate.type === '官方模板' ? 'success' : 'info'" size="small">
                  {{ selectedTemplate.type }}
                </n-tag>
                <span class="meta-text">{{ selectedTemplate.deployCount }}人使用过</span>
                <span class="meta-text">{{ selectedTemplate.category }}</span>
              </div>
            </div>
          </div>

          <!-- 主要信息 -->
          <div class="detail-content">
            <!-- 包含的服务 -->
            <div class="info-section">
              <h3><n-icon><CubeOutline /></n-icon>包含服务 ({{ selectedTemplate.services }}个)</h3>
              <div class="services-list">
                <div v-for="service in getSimpleServices(selectedTemplate)" :key="service.name" class="service-row">
                  <img :src="service.icon" :alt="service.name" class="service-icon-small" />
                  <div class="service-content">
                    <span class="service-name">{{ service.name }}</span>
                    <span class="service-image">{{ service.image }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 端口配置 -->
            <div class="info-section">
              <h3><n-icon><ServerOutline /></n-icon>使用端口</h3>
              <div class="ports-list">
                <div v-for="port in getSimplePorts(selectedTemplate)" :key="port.internal" class="port-row">
                  <span class="port-number">{{ port.internal }}</span>
                  <span class="port-service">{{ port.service }}</span>
                  <span class="port-desc">{{ port.description }}</span>
                </div>
              </div>
            </div>

            <!-- 环境变量 -->
            <div class="info-section" v-if="getSimpleEnvs(selectedTemplate).length > 0">
              <h3><n-icon><SettingsOutline /></n-icon>可配置项</h3>
              <div class="envs-list">
                <div v-for="env in getSimpleEnvs(selectedTemplate)" :key="env.name" class="env-row">
                  <code class="env-name">{{ env.name }}</code>
                  <span class="env-default">{{ env.default || '需要配置' }}</span>
                </div>
              </div>
            </div>

            <!-- 数据存储 -->
            <div class="info-section" v-if="getSimpleVolumes(selectedTemplate).length > 0">
              <h3><n-icon><FolderOutline /></n-icon>数据存储</h3>
              <div class="volumes-list">
                <div v-for="volume in getSimpleVolumes(selectedTemplate)" :key="volume.container" class="volume-row">
                  <span class="volume-path">{{ volume.container }}</span>
                  <span class="volume-desc">{{ volume.service }}数据目录</span>
                </div>
              </div>
            </div>

            <!-- 部署提醒 -->
            <div class="info-section">
              <n-alert type="info" style="margin-bottom: 0;">
                <template #header>
                  <n-icon><InformationCircleOutline /></n-icon>
                  部署提醒
                </template>
                部署后将创建{{ selectedTemplate.services }}个容器，请确保所需端口未被占用
              </n-alert>
            </div>
          </div>

          <!-- 底部操作 -->
          <div class="detail-actions">
            <n-button size="large" @click="closeModal">取消</n-button>
            <n-button type="primary" size="large" @click="handleDeployFromDetail">
              <template #icon>
                <n-icon><PlayOutline /></n-icon>
          </template>
              立即安装
            </n-button>
          </div>
        </div>
        </n-card>
    </n-modal>

    <!-- 导入应用弹窗 -->
    <n-modal v-model:show="showImportModal" style="width: 650px">
      <n-card title="导入应用" :bordered="false" closable @close="showImportModal = false" style="border-radius: 12px;">
        <template #header>
          <div style="display: flex; align-items: center; gap: 12px;">
            <div style="width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #18a058, #36ad6a); display: flex; align-items: center; justify-content: center;">
              <n-icon size="20" color="white"><AddOutline /></n-icon>
            </div>
            <div>
              <h3 style="margin: 0; font-size: 20px; font-weight: 600;">导入应用</h3>
              <p style="margin: 0; color: #666; font-size: 14px;">从YAML文件导入新的应用配置</p>
            </div>
          </div>
        </template>
        
        <n-form :model="importData" label-placement="top" style="margin-top: 8px;">
          <!-- 使用提示 -->
          <n-alert type="info" style="margin-bottom: 20px;">
            <template #header>💡 使用提示</template>
            支持导入标准的Docker Compose YAML文件，系统会自动解析并填充应用信息
          </n-alert>
          
          <n-form-item label="应用名称" required>
            <n-input v-model:value="importData.name" placeholder="请输入应用名称" clearable />
          </n-form-item>
          <n-form-item label="应用描述">
            <n-input 
              v-model:value="importData.description" 
              type="textarea" 
              placeholder="请输入应用描述（可选）"
              :rows="3"
              clearable
            />
          </n-form-item>
          <n-form-item label="应用分类">
            <n-select 
              v-model:value="importData.category" 
              :options="categoryOptions"
              placeholder="选择或输入分类"
              filterable
              tag
              clearable
            />
          </n-form-item>
          
          <!-- 导入方式选择 -->
          <n-form-item label="导入方式">
            <n-radio-group v-model:value="importMethod">
              <n-radio value="file">上传文件</n-radio>
              <n-radio value="content">粘贴内容</n-radio>
            </n-radio-group>
          </n-form-item>
          
          <!-- 文件上传方式 -->
          <n-form-item v-if="importMethod === 'file'" label="YAML文件" required>
            <n-upload
              :max="1"
              accept=".yml,.yaml"
              :show-file-list="false"
              :before-upload="handleBeforeUpload"
              @change="handleFileChange"
              @remove="handleFileRemove"
            >
              <n-button type="primary" dashed style="width: 100%; height: 80px; border-style: dashed; border-width: 2px;">
                <template #icon>
                  <n-icon size="24"><CloudUploadOutline /></n-icon>
                </template>
                <div style="display: flex; flex-direction: column; gap: 4px; align-items: center;">
                  <span style="font-size: 16px; font-weight: 500;">选择YAML文件</span>
                  <span style="font-size: 12px; color: #999;">支持 .yml 和 .yaml 格式，最大1MB</span>
                </div>
              </n-button>
            </n-upload>
            
            <!-- 文件状态显示 -->
            <div v-if="uploadedFileName" style="margin-top: 12px;">
              <n-alert type="success" style="margin-bottom: 8px;">
                <template #header>
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <n-icon><DocumentTextOutline /></n-icon>
                    <span>已选择文件</span>
                  </div>
                </template>
                <div style="font-family: monospace; color: #2080f0;">{{ uploadedFileName }}</div>
              </n-alert>
              
              <!-- 解析状态 -->
              <div v-if="parsing" style="display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #f0f7ff; border-radius: 6px; border-left: 3px solid #2080f0;">
                <n-spin size="small" />
                <span style="color: #2080f0; font-size: 14px;">正在解析配置文件...</span>
              </div>
              
              <div v-else-if="parseSuccess" style="display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #f0fff4; border-radius: 6px; border-left: 3px solid #18a058;">
                <n-icon color="#18a058"><CheckmarkCircleOutline /></n-icon>
                <span style="color: #18a058; font-size: 14px; font-weight: 500;">✓ 配置解析成功，应用信息已自动填充</span>
              </div>
              
              <div v-else-if="!parsing && importData.yamlContent?.trim()" style="display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fffbf0; border-radius: 6px; border-left: 3px solid #f0a020;">
                <n-icon color="#f0a020"><WarningOutline /></n-icon>
                <span style="color: #f0a020; font-size: 14px;">⚠ 配置解析失败，请手动填写应用信息</span>
              </div>
            </div>
          </n-form-item>
          
          <!-- 内容粘贴方式 -->
          <n-form-item v-if="importMethod === 'content'" label="YAML配置" required>
            <n-input 
              v-model:value="importData.yamlContent" 
              type="textarea" 
              placeholder="请粘贴YAML配置内容"
              :rows="10"
            />
            <div style="margin-top: 8px; display: flex; gap: 8px; align-items: center;">
              <n-button 
                size="small" 
                @click="() => parseYamlContent(importData.yamlContent || '')"
                :loading="parsing"
                :disabled="!importData.yamlContent?.trim()"
              >
                <template #icon>
                  <n-icon><DocumentTextOutline /></n-icon>
                </template>
                解析配置
              </n-button>
              <div v-if="parsing" style="color: #2080f0; font-size: 12px;">
                正在解析...
              </div>
              <div v-else-if="parseSuccess" style="color: #18a058; font-size: 12px;">
                ✓ 解析成功，已自动填充
              </div>
            </div>
          </n-form-item>
        </n-form>
        <template #footer>
          <div style="display: flex; justify-content: flex-end; gap: 12px; padding-top: 8px;">
            <n-button size="large" @click="showImportModal = false">
              取消
            </n-button>
            <n-button 
              type="primary" 
              size="large"
              @click="handleImportSubmit" 
              :loading="importing"
              :disabled="!importData.name.trim() || !importData.yamlContent?.trim()"
            >
              <template #icon>
                <n-icon><CloudUploadOutline /></n-icon>
              </template>
              {{ importing ? '导入中...' : '导入应用' }}
            </n-button>
          </div>
        </template>
      </n-card>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, NUpload, NRadioGroup, NRadio, NAlert, NSpin } from 'naive-ui'
import {
  SearchOutline,
  AddOutline,
  DocumentTextOutline,
  ShareOutline,
  TrashOutline,
  RocketOutline,
  CloudDownloadOutline,
  CubeOutline,
  FolderOutline,
  CalendarOutline,
  EllipsisVerticalOutline,
  InformationCircleOutline,
  ServerOutline,
  SettingsOutline,
  PlayOutline,
  CloudUploadOutline,
  CheckmarkCircleOutline,
  WarningOutline
} from '@vicons/ionicons5'

// 导入真实API
import { 
  getApplications, 
  saveApplication, 
  deleteApplication,
  shareApplication,
  getCategories,
  parseApplication,
  type Application,
  type ApplicationSaveRequest,
  type ApplicationParseResult
} from '@/api/http/applications'

const router = useRouter()
const message = useMessage()

// 状态
const searchText = ref('')
const selectedCategory = ref<string | null>(null)
const showDetail = ref(false)
const selectedTemplate = ref<Application | null>(null)
const showImportModal = ref(false)
const importing = ref(false)
const importMethod = ref('file')
const uploadedFileName = ref('')
const parsing = ref(false)
const parseSuccess = ref(false)

// 真实数据
const templates = ref<Application[]>([])
const categories = ref<string[]>([])

// 导入表单数据
const importData = ref<ApplicationSaveRequest>({
  name: '',
  description: '',
  category: '',
  yamlContent: ''
})

// 加载应用列表
const loadApplications = async () => {
  try {
    const params = {
      category: selectedCategory.value || undefined,
      keyword: searchText.value || undefined
    }
    const data = await getApplications(params)
    templates.value = data
  } catch (error) {
    console.error('加载应用列表失败:', error)
    message.error('加载应用列表失败')
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const data = await getCategories()
    categories.value = data
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 过滤后的模板
const filteredTemplates = computed(() => {
  return templates.value.filter(template => {
    const matchSearch = !searchText.value || 
      template.name.toLowerCase().includes(searchText.value.toLowerCase()) ||
      template.description.toLowerCase().includes(searchText.value.toLowerCase())
    
    const matchCategory = !selectedCategory.value || template.category === selectedCategory.value
    
    return matchSearch && matchCategory
  })
})

// 分类选项
const categoryOptions = computed(() => {
  return categories.value.map(cat => ({
    label: cat,
    value: cat
  }))
})

// 操作菜单
const getActionOptions = (template: Application) => [
  { label: '查看详情', key: 'detail', icon: DocumentTextOutline },
  { label: '分享应用', key: 'share', icon: ShareOutline },
  { type: 'divider' },
  { label: '删除应用', key: 'delete', icon: TrashOutline }
]

// 事件处理
const handleDeploy = (template: Application) => {
  router.push({
    path: '/appcenter/install',
    query: { 
      id: template.id.toString(),
      name: template.name 
    }
  })
}

const handleImport = () => {
  // 重置表单状态
  importData.value = {
    name: '',
    description: '',
    category: '',
    yamlContent: ''
  }
  importMethod.value = 'file'
  uploadedFileName.value = ''
  parsing.value = false
  parseSuccess.value = false
  
  showImportModal.value = true
}

const handleAction = async (key: string, template: Application) => {
  if (key === 'detail') {
    selectedTemplate.value = template
    showDetail.value = true
  } else if (key === 'share') {
    try {
      const yamlContent = await shareApplication(template.id)
      await navigator.clipboard.writeText(yamlContent)
      message.success('YAML配置已复制到剪贴板')
    } catch (error) {
      console.error('分享应用失败:', error)
      message.error('分享应用失败')
    }
  } else if (key === 'delete') {
    try {
      await deleteApplication(template.id)
      message.success('删除成功')
      loadApplications()
    } catch (error) {
      console.error('删除应用失败:', error)
      message.error('删除应用失败')
    }
  }
}

const handleDeployFromDetail = () => {
  if (selectedTemplate.value) {
    router.push({
      path: '/appcenter/install',
      query: { 
        id: selectedTemplate.value.id.toString(),
        name: selectedTemplate.value.name 
      }
    })
    showDetail.value = false
  }
}

const handleImportSubmit = async () => {
  importing.value = true
  try {
    // 验证必填字段
    if (!importData.value.name.trim()) {
      message.error('应用名称不能为空')
      return
    }
    
    if (!importData.value.yamlContent || !importData.value.yamlContent.trim()) {
      message.error('YAML配置不能为空')
      return
    }
    
    await saveApplication(importData.value)
    message.success('导入成功')
    showImportModal.value = false
    
    // 重置表单数据
    importData.value = {
      name: '',
      description: '',
      category: '',
      yamlContent: ''
    }
    importMethod.value = 'file'
    uploadedFileName.value = ''
    parsing.value = false
    parseSuccess.value = false
    
    loadApplications()
  } catch (error) {
    console.error('导入应用失败:', error)
    message.error('导入应用失败')
  } finally {
    importing.value = false
  }
}

const closeModal = () => {
  showDetail.value = false
  selectedTemplate.value = null
}

// 从YAML解析服务信息的辅助函数
const getTemplateServices = (template: Application) => {
  // 从YAML中解析services，这里先返回简化版本
    return [
      {
      name: template.name,
      description: template.description,
      image: 'unknown',
      icon: template.iconUrl || template.icon,
      ports: []
    }
  ]
}

const getTemplateEnvs = (template: Application): Array<{name: string, default: string}> => {
  // 这里可以解析YAML中的环境变量，暂时返回空数组
  return []
}

const getTemplatePorts = (template: Application): Array<{internal: number, service: string, description: string}> => {
  // 这里可以解析YAML中的端口映射，暂时返回空数组
  return []
}

const getTemplateVolumes = (template: Application): Array<{container: string, service: string}> => {
  // 这里可以解析YAML中的卷挂载，暂时返回空数组
  return []
}

const getTemplateYaml = (template: Application) => {
  return template.yamlContent || '# 暂无配置预览'
}

const getTemplateReadme = (template: Application) => {
    return `
<h3>应用信息</h3>
<ul>
  <li>应用名称: ${template.name}</li>
  <li>应用分类: ${template.category}</li>
  <li>服务数量: ${template.services}个</li>
  <li>部署次数: ${template.deployCount}次</li>
</ul>

<h3>部署步骤</h3>
<ol>
  <li>点击"立即部署"按钮开始部署</li>
  <li>确认配置参数</li>
  <li>等待镜像拉取和容器启动</li>
  <li>根据提示访问应用</li>
</ol>

<h3>注意事项</h3>
<ul>
  <li>请确保端口未被占用</li>
  <li>确保有足够的磁盘空间</li>
  <li>首次启动可能需要几分钟时间</li>
</ul>
    `
}

const getSimpleServices = (template: Application) => {
  return getTemplateServices(template)
}

const getUseCases = (template: Application) => {
  // 根据分类返回适用场景
  switch (template.category) {
    case 'Web开发':
      return ['Web项目开发', '前端调试', '后端API开发', '全栈开发环境']
    case '媒体娱乐':
    return ['家庭影音中心', '个人视频库', '在线视频播放', '媒体文件管理']
    case '文件存储':
    return ['个人网盘', '文件同步', '团队协作', '数据备份']
    case '运维监控':
    return ['服务器监控', '性能分析', '故障告警', '运维管理']
    default:
      return ['通用用途', '学习环境', '测试部署']
  }
}

const getRequirements = (template: Application) => {
  // 根据服务数量估算资源需求
  const serviceCount = template.services || 1
    return {
    memory: serviceCount > 3 ? '最低4GB' : serviceCount > 1 ? '最低2GB' : '最低1GB',
    storage: serviceCount > 3 ? '20GB起' : serviceCount > 1 ? '10GB起' : '5GB起',
    ports: '根据配置而定'
    }
}

const getDeployTips = (template: Application) => {
  return `部署 ${template.name} 前请确保系统满足资源需求，首次启动可能需要拉取镜像。`
}

const getSimplePorts = (template: Application) => {
  return getTemplatePorts(template)
}

const getSimpleEnvs = (template: Application) => {
  return getTemplateEnvs(template)
}

const getSimpleVolumes = (template: Application) => {
  return getTemplateVolumes(template)
}

// 搜索处理
const handleSearch = () => {
  loadApplications()
}

// 分类选择处理
const handleCategoryChange = () => {
  loadApplications()
}

// 文件上传处理
const handleFileChange = async (data: { file: any; fileList: any[] }) => {
  console.log('文件选择事件触发:', data)
  
  if (!data.file) {
    console.log('没有选择文件')
    return
  }
  
  // 获取原始文件对象
  const file = data.file.file as File
  console.log('选择的文件:', file?.name, file?.size, file?.type)
  
  if (!file) {
    console.log('文件对象为空')
    return
  }
  
  // 立即显示文件名反馈
  uploadedFileName.value = file.name
  message.info(`正在处理文件: ${file.name}`)
  
  // 检查文件类型
  if (!file.name.toLowerCase().endsWith('.yml') && !file.name.toLowerCase().endsWith('.yaml')) {
    message.error('请选择YAML文件（.yml或.yaml格式）')
    uploadedFileName.value = ''
    return
  }
  
  // 检查文件大小（限制为1MB）
  if (file.size > 1024 * 1024) {
    message.error('文件大小不能超过1MB')
    uploadedFileName.value = ''
    return
  }
  
  // 读取文件内容
  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      const content = e.target?.result as string
      console.log('文件内容读取成功，长度:', content.length)
      
      importData.value.yamlContent = content
      
      // 自动解析YAML内容
      await parseYamlContent(content)
      
      // 如果解析失败且应用名称为空，尝试从文件名推断
      if (!parseSuccess.value && !importData.value.name) {
        const nameWithoutExt = file.name.replace(/\.(yml|yaml)$/i, '')
        importData.value.name = nameWithoutExt
      }
      
      message.success('文件读取成功')
    } catch (error) {
      console.error('文件处理出错:', error)
      message.error('文件处理失败')
  }
}
  reader.onerror = () => {
    console.error('文件读取失败')
    message.error('文件读取失败')
  }
  reader.readAsText(file)
}

// 移除文件处理
const handleFileRemove = () => {
  importData.value.yamlContent = ''
  uploadedFileName.value = ''
  message.info('已移除文件')
}

// before-upload处理（阻止自动上传并手动处理文件）
const handleBeforeUpload = async (data: { file: any }) => {
  console.log('before-upload触发:', data)
  
  const file = data.file.file as File
  if (file) {
    await processSelectedFile(file)
  }
  
  return false // 阻止自动上传
}

// 统一的文件处理逻辑
const processSelectedFile = async (file: File) => {
  console.log('处理文件:', file.name, file.size, file.type)
  
  // 立即显示文件名反馈
  uploadedFileName.value = file.name
  message.info(`正在处理文件: ${file.name}`)
  
  // 检查文件类型
  if (!file.name.toLowerCase().endsWith('.yml') && !file.name.toLowerCase().endsWith('.yaml')) {
    message.error('请选择YAML文件（.yml或.yaml格式）')
    uploadedFileName.value = ''
    return
  }
  
  // 检查文件大小（限制为1MB）
  if (file.size > 1024 * 1024) {
    message.error('文件大小不能超过1MB')
    uploadedFileName.value = ''
    return
  }
  
  // 读取文件内容
  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      const content = e.target?.result as string
      console.log('文件内容读取成功，长度:', content.length)
      
      importData.value.yamlContent = content
      
      // 自动解析YAML内容
      await parseYamlContent(content)
      
      // 如果解析失败且应用名称为空，尝试从文件名推断
      if (!parseSuccess.value && !importData.value.name) {
        const nameWithoutExt = file.name.replace(/\.(yml|yaml)$/i, '')
        importData.value.name = nameWithoutExt
      }
      
      message.success('文件读取成功')
    } catch (error) {
      console.error('文件处理出错:', error)
      message.error('文件处理失败')
    }
  }
  reader.onerror = () => {
    console.error('文件读取失败')
    message.error('文件读取失败')
  }
  reader.readAsText(file)
}

// YAML解析功能
const parseYamlContent = async (yamlContent: string) => {
  if (!yamlContent.trim()) {
    parseSuccess.value = false
    return
  }
  
  try {
    parsing.value = true
    const result: ApplicationParseResult = await parseApplication({ yamlContent })
    
    // 自动填充表单字段
    if (result.meta) {
      if (result.meta.name && !importData.value.name) {
        importData.value.name = result.meta.name
      }
      if (result.meta.description && !importData.value.description) {
        importData.value.description = result.meta.description
      }
      if (result.meta.category && !importData.value.category) {
        importData.value.category = result.meta.category
      }
    }
    
    parseSuccess.value = true
    message.success('YAML配置解析成功，已自动填充应用信息')
    
  } catch (error: any) {
    parseSuccess.value = false
    console.warn('YAML解析失败:', error)
    // 不显示错误消息，允许用户手动填写
  } finally {
    parsing.value = false
  }
}

// 页面挂载时的处理
onMounted(async () => {
  loadApplications()
  loadCategories()
  
  // 检查是否有来自URL的导入参数
  const route = router.currentRoute.value
  if (route.query.mode === 'import') {
    const yamlContent = route.query.yaml ? decodeURIComponent(route.query.yaml as string) : ''
    const name = route.query.name ? decodeURIComponent(route.query.name as string) : ''
    
    if (yamlContent) {
      importData.value = {
        name: name || '容器应用',
        description: '',
        category: '容器应用',
        yamlContent: yamlContent
      }
      importMethod.value = 'content' // 自动切换到粘贴内容模式
      showImportModal.value = true
      
      // 自动解析YAML内容
      await parseYamlContent(yamlContent)
      
      // 清除URL参数，避免刷新页面时重复触发
      router.replace({ path: '/appcenter' })
}
  }
})
</script>

<style scoped>
.template-store {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.store-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: #2c3e50;
}

.template-count {
  margin-left: 12px;
  color: #7f8c8d;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.template-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.template-item {
  display: flex;
  align-items: center;
  padding: 20px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e9ecef;
  transition: all 0.2s ease;
}

.template-item:hover {
  border-color: #18a058;
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.1);
}

.template-icon {
  width: 64px;
  height: 64px;
  margin-right: 20px;
  flex-shrink: 0;
}

.template-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.template-content {
  flex: 1;
  min-width: 0;
}

.template-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.template-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.template-badges {
  display: flex;
  gap: 6px;
}

.template-description {
  margin: 0 0 12px 0;
  color: #5a6c7d;
  line-height: 1.5;
  font-size: 14px;
}

.template-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #8492a6;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.template-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .template-store {
    padding: 16px;
  }
  
  .store-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .header-actions {
    justify-content: space-between;
  }
  
  .template-item {
    flex-direction: column;
    align-items: stretch;
    text-align: center;
  }
  
  .template-icon {
    margin: 0 auto 16px auto;
  }
  
  .template-header {
    flex-direction: column;
    gap: 8px;
  }
  
  .template-meta {
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .template-actions {
    margin-left: 0;
    margin-top: 16px;
    justify-content: center;
  }
}

.template-detail {
  padding: 24px;
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.detail-icon {
  width: 80px;
  height: 80px;
  margin-right: 20px;
  flex-shrink: 0;
  border-radius: 16px;
  object-fit: contain;
}

.detail-info {
  flex: 1;
  min-width: 0;
}

.detail-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
}

.detail-desc {
  margin: 0 0 12px 0;
  color: #5a6c7d;
  line-height: 1.5;
  font-size: 14px;
}

.detail-meta {
  display: flex;
  gap: 6px;
}

.meta-text {
  color: #7f8c8d;
  font-size: 14px;
}

.detail-content {
  margin-bottom: 24px;
}

.info-section {
  margin-bottom: 24px;
}

.info-section h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.services-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.service-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #36ad6a;
}

.service-icon-small {
  width: 24px;
  height: 24px;
  margin-right: 0;
  flex-shrink: 0;
}

.service-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.service-name {
  color: #2c3e50;
  font-size: 14px;
  font-weight: 600;
}

.service-image {
  color: #7f8c8d;
  font-size: 12px;
  font-family: monospace;
}

.ports-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.port-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #18a058;
}

.port-number {
  font-weight: 600;
  color: #18a058;
  min-width: 60px;
}

.port-service {
  color: #2c3e50;
  font-weight: 500;
  min-width: 80px;
}

.port-desc {
  color: #7f8c8d;
  font-size: 12px;
  flex: 1;
}

.envs-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.env-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #2080f0;
}

.env-name {
  font-weight: 600;
  color: #2080f0;
  min-width: 120px;
}

.env-default {
  color: #7f8c8d;
  font-size: 12px;
  flex: 1;
}

.volumes-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.volume-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #f0a020;
}

.volume-path {
  font-weight: 600;
  color: #f0a020;
  font-family: monospace;
  min-width: 150px;
}

.volume-desc {
  color: #7f8c8d;
  font-size: 12px;
  flex: 1;
}

.deploy-docs {
  margin-bottom: 24px;
}

.detail-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 32px;
  gap: 16px;
}

.detail-actions .n-button {
  flex: 1;
}
</style> 