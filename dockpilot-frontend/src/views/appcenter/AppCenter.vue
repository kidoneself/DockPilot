<template>
  <div class="template-store">
    <!-- 顶部标题栏 -->
    <div class="store-header">
      <div class="header-left">
        <h1>应用中心</h1>
      </div>
      <div class="header-actions">
        <n-button type="primary" size="medium" @click="handleImport">
          <template #icon>
            <n-icon><AddOutline /></n-icon>
          </template>
          导入应用
        </n-button>
      </div>
    </div>

    <!-- 应用类型切换 -->
    <div class="app-type-tabs">
      <n-button-group>
        <n-button 
          :type="appType === 'local' ? 'primary' : 'default'"
          @click="() => { appType = 'local'; handleAppTypeChange() }"
        >
          📱 本地应用
        </n-button>
        <n-button 
          :type="appType === 'market' ? 'primary' : 'default'"
          @click="() => { appType = 'market'; handleAppTypeChange() }"
        >
          🏪 应用市场
        </n-button>
      </n-button-group>
    </div>

    <!-- 模板列表 -->
    <div class="template-list">
      <div 
        v-for="(template, index) in filteredTemplates" 
        :key="template.id || `market-${index}`"
        class="plugin-card"
        :style="{ background: getCardGradient(template.id || index, template.name) }"
        @click="handleDeploy(template)"
      >
        <!-- 标题和版本 -->
        <h3 class="plugin-title">{{ template.name }} {{ template.version || 'v1.0.0' }}</h3>
        
        <!-- 描述文字 -->
        <p class="plugin-description">{{ template.description }}</p>
        
        <!-- 右上角圆形图标 -->
        <div class="plugin-icon">
          <img :src="template.iconUrl || template.icon" :alt="template.name" />
        </div>
        
        <!-- 底部信息栏 -->
        <div class="plugin-footer">
          <div class="author-info">
            <n-icon size="12" color="rgba(255,255,255,0.7)"><PersonOutline /></n-icon>
            <span>{{ template.author || 'thsrite' }}</span>
          </div>
          <div class="more-actions">
            <!-- 市场应用显示安装按钮 -->
            <n-button 
              v-if="appType === 'market'"
              size="tiny" 
              type="primary"
              @click.stop="handleInstallFromMarket(template)"
            >
              安装
            </n-button>
            <!-- 本地应用显示更多操作菜单 -->
            <n-dropdown 
              v-else
              trigger="click" 
              :options="getActionOptions(template)" 
              @select="(key: any) => handleAction(key, template)"
            >
              <n-button size="tiny" text @click.stop>
                <template #icon>
                  <n-icon size="16" color="rgba(255,255,255,0.6)"><EllipsisVerticalOutline /></n-icon>
                </template>
              </n-button>
            </n-dropdown>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="filteredTemplates.length === 0" class="empty-state">
      <n-empty description="没有找到匹配的应用" />
    </div>

    <!-- 导入应用弹窗 -->
    <n-modal v-model:show="showImportModal" style="width: 650px">
      <n-card
title="导入应用"
:bordered="false"
closable
style="border-radius: 12px;"
@close="showImportModal = false">
        <template #header>
          <div style="display: flex; align-items: center; gap: 12px;">
            <div style="width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #18a058, #36ad6a); display: flex; align-items: center; justify-content: center;">
              <n-icon size="20" color="white"><AddOutline /></n-icon>
            </div>
            <div>
              <h3 style="margin: 0; font-size: 20px; font-weight: 600;">导入应用</h3>
              <p style="margin: 0; color: #666; font-size: 14px;">选择导入方式，系统将自动解析配置</p>
            </div>
          </div>
        </template>
        
        <div style="margin-top: 8px;">
          <!-- 使用提示 -->
          <NAlert type="info" style="margin-bottom: 20px;">
            <template #header>💡 使用说明</template>
            支持 Docker Compose YAML 文件和项目 ZIP 包，系统会自动解析并填充应用信息
          </NAlert>
          
          <!-- 步骤1：选择导入方式 -->
          <div style="margin-bottom: 24px;">
            <h4 style="margin: 0 0 12px 0; font-size: 16px; font-weight: 600; color: var(--text-color-1);">
              📂 选择导入方式
            </h4>
            <NRadioGroup v-model:value="importMethod">
              <n-space vertical>
                <NRadio value="file">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <n-icon><DocumentTextOutline /></n-icon>
                    <span>上传YAML文件</span>
                    <span style="color: #999; font-size: 12px;">(docker-compose.yml)</span>
                  </div>
                </NRadio>
                <NRadio value="zip">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <n-icon><FolderOutline /></n-icon>
                    <span>上传ZIP包</span>
                    <span style="color: #999; font-size: 12px;">(项目导出包)</span>
                  </div>
                </NRadio>
                <NRadio value="url">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <n-icon><CloudDownloadOutline /></n-icon>
                    <span>在线URL</span>
                    <span style="color: #999; font-size: 12px;">(网络地址下载)</span>
                  </div>
                </NRadio>
                <NRadio value="content">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <n-icon><DocumentTextOutline /></n-icon>
                    <span>粘贴内容</span>
                    <span style="color: #999; font-size: 12px;">(直接粘贴YAML)</span>
                  </div>
                </NRadio>
              </n-space>
            </NRadioGroup>
          </div>

          <!-- 步骤2：上传/输入文件 -->
          <div v-if="importMethod" style="margin-bottom: 24px;">
            <h4 style="margin: 0 0 12px 0; font-size: 16px; font-weight: 600; color: var(--text-color-1);">
              📤 {{ getStepTitle }}
            </h4>
            
            <!-- 文件上传方式 -->
            <div v-if="importMethod === 'file'">
              <NUpload
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
                    <span style="font-size: 16px; font-weight: 500;">点击选择YAML文件</span>
                    <span style="font-size: 12px; color: #999;">支持 .yml 和 .yaml 格式，最大1MB</span>
                  </div>
                </n-button>
              </NUpload>
              
              <!-- 文件状态显示 -->
              <div v-if="uploadedFileName" style="margin-top: 12px;">
                <NAlert type="success" style="margin-bottom: 8px;">
                  <template #header>
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <n-icon><DocumentTextOutline /></n-icon>
                      <span>已选择文件</span>
                    </div>
                  </template>
                  <div style="font-family: monospace; color: #2080f0;">{{ uploadedFileName }}</div>
                </NAlert>
                
                <!-- 解析状态 -->
                <div v-if="parsing" style="display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #f0f7ff; border-radius: 6px; border-left: 3px solid #2080f0;">
                  <NSpin size="small" />
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
            </div>
            
            <!-- ZIP包上传方式 -->
            <div v-if="importMethod === 'zip'">
              <NUpload
                :max="1"
                accept=".zip"
                :show-file-list="false"
                :before-upload="handleZipBeforeUpload"
                @change="handleZipFileChange"
                @remove="handleZipFileRemove"
              >
                <n-button type="primary" dashed style="width: 100%; height: 80px; border-style: dashed; border-width: 2px;">
                  <template #icon>
                    <n-icon size="24"><CloudUploadOutline /></n-icon>
                  </template>
                  <div style="display: flex; flex-direction: column; gap: 4px; align-items: center;">
                    <span style="font-size: 16px; font-weight: 500;">点击选择ZIP包</span>
                    <span style="font-size: 12px; color: #999;">导出的项目ZIP包</span>
                  </div>
                </n-button>
              </NUpload>
              
              <!-- ZIP文件状态显示 -->
              <div v-if="uploadedZipName" style="margin-top: 12px;">
                <NAlert type="success" style="margin-bottom: 8px;">
                  <template #header>
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <n-icon><FolderOutline /></n-icon>
                      <span>已选择ZIP包</span>
                    </div>
                  </template>
                  <div style="font-family: monospace; color: #2080f0;">{{ uploadedZipName }}</div>
                </NAlert>
                
                <!-- ZIP解析状态 -->
                <div v-if="zipProcessing" style="display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #f0f7ff; border-radius: 6px; border-left: 3px solid #2080f0;">
                  <NSpin size="small" />
                  <span style="color: #2080f0; font-size: 14px;">正在解析ZIP包和配置文件...</span>
                </div>
                
                <div v-else-if="zipParseSuccess" style="display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #f0fff4; border-radius: 6px; border-left: 3px solid #18a058;">
                  <n-icon color="#18a058"><CheckmarkCircleOutline /></n-icon>
                  <span style="color: #18a058; font-size: 14px; font-weight: 500;">✓ ZIP包解析成功，配置包已自动关联</span>
                </div>
                
                <div v-else-if="!zipProcessing && uploadedZipName" style="display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fffbf0; border-radius: 6px; border-left: 3px solid #f0a020;">
                  <n-icon color="#f0a020"><WarningOutline /></n-icon>
                  <span style="color: #f0a020; font-size: 14px;">⚠ ZIP包解析失败，请检查包格式</span>
                </div>
              </div>
            </div>

            <!-- 在线URL方式 -->
            <div v-if="importMethod === 'url'">
              <n-input 
                v-model:value="urlInput" 
                placeholder="请输入YAML或ZIP文件的URL地址，例如：https://example.com/app.yml"
                clearable
                size="large"
                style="margin-bottom: 12px;"
              />
              <div style="display: flex; gap: 8px; align-items: center; margin-bottom: 12px;">
                <n-button 
                  :loading="urlFetching"
                  :disabled="!urlInput?.trim()"
                  type="primary"
                  size="large"
                  style="flex-shrink: 0;"
                  @click="handleFetchFromUrl"
                >
                  <template #icon>
                    <n-icon><CloudDownloadOutline /></n-icon>
                  </template>
                  获取文件
                </n-button>
                <div v-if="urlFetching" style="color: #2080f0; font-size: 14px;">
                  正在下载文件...
                </div>
                <div v-else-if="urlFetchSuccess" style="color: #18a058; font-size: 14px;">
                  ✓ 文件获取成功，已自动解析
                </div>
              </div>
              
              <!-- URL文件状态显示 -->
              <div v-if="urlInput && !urlFetching && importData.yamlContent" style="margin-top: 12px;">
                <NAlert type="success" style="margin-bottom: 8px;">
                  <template #header>
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <n-icon><CheckmarkCircleOutline /></n-icon>
                      <span>文件获取成功</span>
                    </div>
                  </template>
                  <div style="font-family: monospace; color: #2080f0; word-break: break-all;">{{ urlInput }}</div>
                </NAlert>
              </div>
              
              <!-- 支持格式提示 -->
              <div style="padding: 12px; background: #f6f9ff; border-radius: 6px; border-left: 3px solid #2080f0;">
                <div style="color: #2080f0; font-size: 12px; font-weight: 500; margin-bottom: 4px;">💡 支持的格式：</div>
                <div style="color: #666; font-size: 12px;">
                  • YAML文件：.yml、.yaml<br>
                  • ZIP包：.zip<br>
                  • GitHub Raw链接、CDN链接等
                </div>
              </div>
            </div>
            
            <!-- 内容粘贴方式 -->
            <div v-if="importMethod === 'content'">
              <n-input 
                v-model:value="importData.yamlContent" 
                type="textarea" 
                placeholder="请粘贴YAML配置内容"
                :rows="8"
                style="margin-bottom: 12px;"
              />
              <div style="display: flex; gap: 8px; align-items: center;">
                <n-button 
                  size="medium" 
                  :loading="parsing"
                  :disabled="!importData.yamlContent?.trim()"
                  type="primary"
                  @click="() => parseYamlContent(importData.yamlContent || '')"
                >
                  <template #icon>
                    <n-icon><DocumentTextOutline /></n-icon>
                  </template>
                  解析配置
                </n-button>
                <div v-if="parsing" style="color: #2080f0; font-size: 14px;">
                  正在解析...
                </div>
                <div v-else-if="parseSuccess" style="color: #18a058; font-size: 14px;">
                  ✓ 解析成功，已自动填充
                </div>
              </div>
            </div>
          </div>

          <!-- 步骤3：应用信息确认（只有解析成功后才显示） -->
          <div v-if="parseSuccess || zipParseSuccess || urlFetchSuccess || (importData.yamlContent && importMethod === 'content')" style="margin-bottom: 24px;">
            <h4 style="margin: 0 0 12px 0; font-size: 16px; font-weight: 600; color: var(--text-color-1);">
              ✏️ 确认应用信息
            </h4>
            <n-form
:model="importData"
label-placement="left"
label-width="80px"
style="background: #fafafa; padding: 16px; border-radius: 8px;">
              <n-form-item label="应用名称" required>
                <n-input v-model:value="importData.name" placeholder="请输入应用名称" clearable />
              </n-form-item>
              <n-form-item label="应用描述">
                <n-input 
                  v-model:value="importData.description" 
                  type="textarea" 
                  placeholder="请输入应用描述（可选）"
                  :rows="2"
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
              <n-form-item label="图标URL">
                <n-input 
                  v-model:value="importData.iconUrl" 
                  placeholder="请输入图标链接（可选）"
                  clearable
                />
              </n-form-item>
            </n-form>
          </div>
        </div>
        
        <template #footer>
          <div style="display: flex; justify-content: space-between; align-items: center; padding-top: 8px;">
            <div style="color: #999; font-size: 12px;">
              {{ getFooterText }}
            </div>
            <div style="display: flex; gap: 12px;">
              <n-button size="large" @click="showImportModal = false">
                取消
              </n-button>
              <n-button 
                type="primary" 
                size="large"
                :loading="importing" 
                :disabled="!canSubmit"
                @click="handleImportSubmit"
              >
                <template #icon>
                  <n-icon><CloudUploadOutline /></n-icon>
                </template>
                {{ importing ? '导入中...' : '导入应用' }}
              </n-button>
            </div>
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
  AddOutline,
  DocumentTextOutline,
  TrashOutline,
  CloudDownloadOutline,
  FolderOutline,
  EllipsisVerticalOutline,
  CloudUploadOutline,
  CheckmarkCircleOutline,
  WarningOutline,
  PersonOutline
} from '@vicons/ionicons5'

// 导入真实API
import { 
  getApplications, 
  saveApplication, 
  deleteApplication,
  getCategories,
  parseApplication,
  parseZipPackage,
  fetchFromUrl,
  getMarketApplications,
  type Application,
  type ApplicationSaveRequest,
  type ApplicationParseResult
} from '@/api/http/applications'

const router = useRouter()
const message = useMessage()

// 状态
const searchText = ref('')
const selectedCategory = ref<string | null>(null)
const showImportModal = ref(false)
const importing = ref(false)
const importMethod = ref('')
const uploadedFileName = ref('')
const parsing = ref(false)
const parseSuccess = ref(false)

// 应用类型切换相关状态
const appType = ref<'local' | 'market'>('local')
const marketTemplates = ref<Application[]>([])
const marketSourcesLoading = ref(false)

// ZIP上传相关状态
const uploadedZipName = ref('')
const zipProcessing = ref(false)
const zipParseSuccess = ref(false)

// URL导入相关状态
const urlInput = ref('')
const urlFetching = ref(false)
const urlFetchSuccess = ref(false)

// 真实数据
const templates = ref<Application[]>([])
const categories = ref<string[]>([])

// 导入表单数据
const importData = ref<ApplicationSaveRequest>({
  name: '',
  description: '',
  category: '',
  iconUrl: '',
  yamlContent: ''
})

// 计算属性：本地应用
const localTemplates = computed(() => templates.value)

// 加载应用列表
const loadApplications = async () => {
  try {
    const params = {
      category: selectedCategory.value || undefined,
      keyword: searchText.value || undefined
    }
    const data = await getApplications(params)
    templates.value = data
  } catch (error: any) {
    console.error('加载应用列表失败:', error)
    message.error(error.message || '加载应用列表失败')
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const data = await getCategories()
    categories.value = data
  } catch (error: any) {
    console.error('加载分类失败:', error)
    message.error(error.message || '加载分类失败')
  }
}

// 加载市场应用列表
const loadMarketApplications = async () => {
  if (marketSourcesLoading.value) return
  
  try {
    marketSourcesLoading.value = true
    message.info('正在获取应用市场数据...')
    
    // 调用API获取市场应用数据
    const data = await getMarketApplications()
    marketTemplates.value = data
    
    message.success(`已获取 ${data.length} 个市场应用`)
    
  } catch (error: any) {
    console.error('加载市场应用失败:', error)
    message.error(error.message || '加载市场应用失败')
    marketTemplates.value = []
  } finally {
    marketSourcesLoading.value = false
  }
}

// 过滤后的模板
const filteredTemplates = computed(() => {
  const currentTemplates = appType.value === 'local' ? localTemplates.value : marketTemplates.value
  return currentTemplates.filter(template => {
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
  { label: '删除应用', key: 'delete', icon: TrashOutline }
]

// 事件处理
const handleDeploy = (template: Application) => {
  // 应用市场的应用没有id，直接触发安装
  if (appType.value === 'market') {
    handleInstallFromMarket(template)
    return
  }
  
  // 本地应用有id，跳转到安装页面
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
    iconUrl: '',
    yamlContent: ''
  }
  importMethod.value = '' // 重置导入方式，让用户重新选择
  uploadedFileName.value = ''
  parsing.value = false
  parseSuccess.value = false
  
  // 重置ZIP状态
  uploadedZipName.value = ''
  zipProcessing.value = false
  zipParseSuccess.value = false
  
  // 重置URL状态
  urlInput.value = ''
  urlFetching.value = false
  urlFetchSuccess.value = false
  
  showImportModal.value = true
}

const handleAction = async (key: string, template: Application) => {
  if (key === 'delete') {
    try {
      await deleteApplication(template.id)
      message.success('删除成功')
      loadApplications()
    } catch (error: any) {
      console.error('删除应用失败:', error)
      message.error(error.message || '删除应用失败')
    }
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
      iconUrl: '',
      yamlContent: ''
    }
    importMethod.value = ''
    uploadedFileName.value = ''
    parsing.value = false
    parseSuccess.value = false
    
    // 重置ZIP状态
    uploadedZipName.value = ''
    zipProcessing.value = false
    zipParseSuccess.value = false
    
    // 重置URL状态
    urlInput.value = ''
    urlFetching.value = false
    urlFetchSuccess.value = false
    
    loadApplications()
  } catch (error: any) {
    console.error('导入应用失败:', error)
    message.error(error.message || '导入应用失败')
  } finally {
    importing.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  loadApplications()
}


// 应用类型切换处理
const handleAppTypeChange = () => {
  if (appType.value === 'market') {
    loadMarketApplications()
  }
}

// 从市场安装应用
const handleInstallFromMarket = async (marketApp: Application) => {
  const marketAppWithUrl = marketApp as Application & { downloadUrl?: string }
  
  if (!marketAppWithUrl.downloadUrl) {
    message.error('该应用没有提供下载链接')
    return
  }
  
  try {
    message.info(`正在获取 ${marketApp.name} 的配置文件...`)
    
    // 调用现有的URL导入功能
    urlInput.value = marketAppWithUrl.downloadUrl
    await handleFetchFromUrl()
    
    // 自动填充应用信息
    importData.value.name = marketApp.name
    importData.value.description = marketApp.description
    importData.value.category = marketApp.category
    importData.value.iconUrl = marketApp.iconUrl || marketApp.icon // 添加图标URL
    
    // 显示导入弹窗，让用户确认
    importMethod.value = 'url'
    showImportModal.value = true
    
  } catch (error: any) {
    console.error('从市场安装应用失败:', error)
    message.error('安装失败: ' + (error.message || '网络错误'))
  }
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
    } catch (error: any) {
      console.error('文件处理出错:', error)
      message.error(error.message || '文件处理失败')
    }
  }
  reader.onerror = (e) => {
    console.error('文件读取失败:', e)
    message.error('文件读取失败，请重试')
    uploadedFileName.value = ''
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
    } catch (error: any) {
      console.error('文件处理出错:', error)
      message.error(error.message || '文件处理失败')
    }
  }
  reader.onerror = (e) => {
    console.error('文件读取失败:', e)
    message.error('文件读取失败，请重试')
    uploadedFileName.value = ''
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

// ZIP包处理功能
const handleZipFileChange = async (data: { file: any; fileList: any[] }) => {
  console.log('ZIP文件选择事件触发:', data)
  
  if (!data.file) {
    console.log('没有选择ZIP文件')
    return
  }
  
  const file = data.file.file as File
  console.log('选择的ZIP文件:', file?.name, file?.size, file?.type)
  
  if (!file) {
    console.log('ZIP文件对象为空')
    return
  }
  
  await processSelectedZipFile(file)
}

const handleZipBeforeUpload = async (data: { file: any }) => {
  console.log('ZIP before-upload触发:', data)
  
  const file = data.file.file as File
  if (file) {
    await processSelectedZipFile(file)
  }
  
  return false // 阻止自动上传
}

const handleZipFileRemove = () => {
  importData.value.yamlContent = ''
  uploadedZipName.value = ''
  zipProcessing.value = false
  zipParseSuccess.value = false
  message.info('已移除ZIP文件')
}

const processSelectedZipFile = async (file: File) => {
  console.log('处理ZIP文件:', file.name, file.size, file.type)
  
  // 立即显示文件名反馈
  uploadedZipName.value = file.name
  message.info(`正在处理ZIP包: ${file.name}`)
  
  // 检查文件类型
  if (!file.name.toLowerCase().endsWith('.zip')) {
    message.error('请选择ZIP文件')
    uploadedZipName.value = ''
    return
  }
  
  try {
    zipProcessing.value = true
    zipParseSuccess.value = false
    
    // 创建FormData
    const formData = new FormData()
    formData.append('file', file)
    
    // 调用ZIP解析API
    const modifiedYaml = await parseZipPackage(formData)
    
    // 设置YAML内容
    importData.value.yamlContent = modifiedYaml
    
    // 解析修改后的YAML内容以填充表单
    await parseYamlContent(modifiedYaml)
    
    zipParseSuccess.value = true
    message.success('ZIP包解析成功，配置包已自动关联为本地路径')
    
  } catch (error: any) {
    zipParseSuccess.value = false
    console.error('ZIP包处理失败:', error)
    message.error('ZIP包处理失败: ' + (error.message || '未知错误'))
  } finally {
    zipProcessing.value = false
  }
}

// 从URL获取文件
const handleFetchFromUrl = async () => {
  if (!urlInput.value?.trim()) {
    message.error('请输入文件URL地址')
    return
  }
  
  try {
    urlFetching.value = true
    urlFetchSuccess.value = false
    
    // 调用API获取文件内容
    const yamlContent = await fetchFromUrl(urlInput.value)
    
    // 设置YAML内容
    importData.value.yamlContent = yamlContent
    
    // 自动解析YAML内容以填充表单
    await parseYamlContent(yamlContent)
    
    urlFetchSuccess.value = true
    message.success('文件获取成功')
    
  } catch (error: any) {
    urlFetchSuccess.value = false
    console.error('URL文件获取失败:', error)
    message.error('文件获取失败: ' + (error.message || '网络错误'))
  } finally {
    urlFetching.value = false
  }
}

// UI辅助方法
const getStepTitle = computed(() => {
  switch (importMethod.value) {
    case 'file': return '上传YAML文件'
    case 'zip': return '上传ZIP包'
    case 'url': return '输入文件URL'
    case 'content': return '粘贴YAML内容'
    default: return '选择文件'
  }
})

const getFooterText = computed(() => {
  if (!importMethod.value) {
    return '请先选择导入方式'
  }
  if (!importData.value.yamlContent?.trim()) {
    return '请先上传或输入配置文件'
  }
  if (!importData.value.name?.trim()) {
    return '请填写应用名称'
  }
  return '准备就绪，可以导入应用'
})

// 计算属性：是否可以提交
const canSubmit = computed(() => {
  return importData.value.name?.trim() && importData.value.yamlContent?.trim()
})

// 生成卡片渐变背景
const getCardGradient = (id: number | undefined, appName?: string) => {
  const gradients = [
    'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', // 深紫蓝
    'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', // 清新蓝
    'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', // 薄荷绿
    'linear-gradient(135deg, #667db6 0%, #0082c8 100%)', // 深海蓝
    'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)', // 天空蓝
    'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)', // 薄荷粉
    'linear-gradient(135deg, #8fd3f4 0%, #84fab0 100%)', // 青绿色
    'linear-gradient(135deg, #74b9ff 0%, #0984e3 100%)', // 蓝色系
    'linear-gradient(135deg, #6c5ce7 0%, #a29bfe 100%)', // 淡紫色
    'linear-gradient(135deg, #00b894 0%, #00cec9 100%)', // 青蓝色
    'linear-gradient(135deg, #5f72bd 0%, #9b23ea 100%)', // 深紫色
    'linear-gradient(135deg, #2193b0 0%, #6dd5ed 100%)', // 清水蓝
    'linear-gradient(135deg, #3b82f6 0%, #1e40af 100%)', // 经典蓝
    'linear-gradient(135deg, #06b6d4 0%, #0891b2 100%)', // 青色系
    'linear-gradient(135deg, #10b981 0%, #059669 100%)', // 翠绿色
    'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)', // 紫罗兰
    'linear-gradient(135deg, #64748b 0%, #475569 100%)', // 石墨灰
    'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', // 湖水蓝
    'linear-gradient(135deg, #22c55e 0%, #16a34a 100%)', // 森林绿
    'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)'  // 靛蓝色
  ]
  
  // 如果有ID，优先使用ID
  if (id !== undefined && id !== null && !isNaN(id)) {
    return gradients[id % gradients.length]
  }
  
  // 如果没有ID但有应用名称，基于名称生成哈希
  if (appName) {
    let hash = 0
    for (let i = 0; i < appName.length; i++) {
      const char = appName.charCodeAt(i)
      hash = ((hash << 5) - hash) + char
      hash = hash & hash // 转换为32位整数
    }
    return gradients[Math.abs(hash) % gradients.length]
  }
  
  // 默认返回第一个渐变
  return gradients[0]
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
        iconUrl: '',
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
  padding: 24px 16px;
  max-width: 100%;
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
  color: var(--text-color-1);
}

.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.app-type-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.template-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  max-width: 1400px;
  margin: 0 auto;
}

.plugin-card {
  position: relative;
  padding: 16px;
  border-radius: 12px;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  aspect-ratio: 2/1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.plugin-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

/* 深色模式下的增强效果 */
[data-theme="dark"] .plugin-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}

[data-theme="dark"] .plugin-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
  transform: translateY(-2px);
}

.plugin-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 700;
  color: white;
  line-height: 1.3;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  padding-right: 60px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: calc(100% - 60px);
}

.plugin-description {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.4;
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.1);
  padding-right: 60px;
  flex: 1;
  word-break: break-word;
}

.plugin-icon {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.plugin-icon img {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  object-fit: cover;
}

.plugin-footer {
  position: absolute;
  bottom: 8px;
  left: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 400;
  z-index: 2;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
}

.author-info .n-icon {
  font-size: 12px;
  opacity: 0.8;
}

.more-actions {
  display: flex;
  align-items: center;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

/* 响应式 */
@media (max-width: 1600px) {
  .template-list {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 10px;
  }
}

@media (max-width: 1200px) {
  .template-list {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 10px;
  }
}

@media (max-width: 768px) {
  .template-store {
    padding: 16px 12px;
  }
  
  .store-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .header-actions {
    justify-content: space-between;
  }
  
  .template-list {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 8px;
  }
  
  .plugin-card {
    padding: 12px;
  }
  
  .plugin-title {
    font-size: 14px;
    padding-right: 50px;
    max-width: calc(100% - 50px);
  }
  
  .plugin-description {
    font-size: 12px;
    padding-right: 50px;
  }
  
  .plugin-icon {
    width: 40px;
    height: 40px;
    top: 12px;
    right: 12px;
  }
  
  .plugin-icon img {
    width: 28px;
    height: 28px;
  }
  
  .plugin-footer {
    bottom: 8px;
    left: 12px;
    right: 12px;
    font-size: 10px;
  }
}

@media (max-width: 480px) {
  .template-list {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 6px;
  }
  
  .plugin-card {
    padding: 10px;
  }
  
  .plugin-title {
    font-size: 12px;
    margin-bottom: 6px;
    max-width: calc(100% - 40px);
  }
  
  .plugin-description {
    font-size: 11px;
  }
  
  .plugin-icon {
    width: 36px;
    height: 36px;
    top: 8px;
    right: 8px;
  }
  
  .plugin-icon img {
    width: 24px;
    height: 24px;
  }
  
  .plugin-footer {
    font-size: 9px;
    bottom: 6px;
    left: 10px;
    right: 10px;
  }
}
</style> 