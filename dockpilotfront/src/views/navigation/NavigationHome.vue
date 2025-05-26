<template>
  <div class="navigation-home">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-gradient"></div>
      <div class="bg-pattern"></div>
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
    </div>

    <div class="main-content">
      <!-- 顶部区域 -->
      <header class="top-header">
        <div class="header-left">
          <div class="logo-section">
            <div class="logo-icon">
              <n-icon size="32" :component="CubeOutline" />
            </div>
            <div class="logo-text">
              <h1>Docker Pilot</h1>
              <p>智能容器管理平台</p>
            </div>
          </div>
        </div>
        
        <div class="header-center">
          <div class="datetime-display">
            <div class="time">{{ currentTime }}</div>
            <div class="date">{{ currentDate }}</div>
          </div>
        </div>

        <div class="header-right">
          <div 
            class="weather-widget" 
            :title="`${weatherData.location} - 点击查看详情`" 
            @click="showWeatherDetails"
          >
            <n-icon size="20" :component="weatherData.icon" />
            <span>{{ weatherData.temperature }}</span>
            <span class="weather-location">{{ weatherData.location }}</span>
          </div>
        </div>
      </header>

      <!-- 搜索区域 -->
      <div class="search-section">
        <div class="search-container">
          <n-input
            v-model:value="searchQuery"
            size="large"
            round
            placeholder="搜索应用、容器或服务..."
            clearable
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>
        </div>
      </div>

      <!-- 系统状态总览 - 紧凑版 -->
      <div class="stats-section">
        <div class="section-header">
          <h3>系统状态</h3>
          <div class="header-actions">
            <n-button text size="tiny" @click="showDetailedInfo = !showDetailedInfo">
              {{ showDetailedInfo ? '收起详情' : '查看详情' }}
            </n-button>
          </div>
        </div>
        
        <!-- 紧凑型性能卡片 -->
        <div class="compact-stats-grid">
          <div 
            v-for="stat in systemStats" 
            :key="stat.title"
            class="compact-stat-card"
          >
            <div class="compact-stat-content">
              <div class="stat-icon-small" :style="{ color: stat.color }">
                <n-icon :size="14" :component="stat.icon" />
              </div>
              <div class="stat-main">
                <div class="stat-title-small">{{ stat.title }}</div>
                <div class="stat-value-large">{{ stat.value }}</div>
                <div v-if="stat.total || stat.free" class="stat-extra-small">
                  <span v-if="stat.total">/ {{ stat.total }}</span>
                  <span v-if="stat.free" class="free-space-small">剩余 {{ stat.free }}</span>
                </div>
              </div>
              
              <!-- 网速卡片：显示实时指示器而不是百分比 -->
              <div v-if="stat.title === '网络速度'" class="stat-network-indicator">
                <div v-if="stat.value.includes('未知') || stat.value.includes('初始化') || stat.value.includes('计算中') || stat.value.includes('获取失败')" class="network-status-text">
                  <span class="status-text">{{ stat.value.includes('未知') ? '检测中' : stat.value.includes('初始化') ? '准备中' : stat.value.includes('计算中') ? '计算中' : '获取失败' }}</span>
                </div>
                <div v-else class="network-status" :class="{ active: stat.percentage > 0 }">
                  <div class="signal-bars">
                    <div class="bar" :class="{ active: stat.percentage >= 25 }"></div>
                    <div class="bar" :class="{ active: stat.percentage >= 50 }"></div>
                    <div class="bar" :class="{ active: stat.percentage >= 75 }"></div>
                    <div class="bar" :class="{ active: stat.percentage >= 100 }"></div>
                  </div>
                </div>
              </div>
              
              <!-- 其他卡片：显示百分比圆形进度条 -->
              <div v-else class="stat-percentage">
                <div class="percentage-text">{{ stat.percentage.toFixed(0) }}%</div>
                <n-progress
                  type="circle"
                  :percentage="stat.percentage"
                  :stroke-width="8"
                  :show-indicator="false"
                  :color="stat.color"
                  :rail-color="'rgba(255,255,255,0.1)'"
                  style="width: 32px; height: 32px;"
                />
              </div>
            </div>
          </div>
        </div>
        
        <!-- 详细系统信息 - 可折叠 -->
        <div v-show="showDetailedInfo" class="detailed-info-section">
          <div class="info-summary">
            <div class="summary-item">
              <span class="summary-label">主机</span>
              <span class="summary-value">{{ systemInfo.hostname }} ({{ systemInfo.os }})</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">硬件</span>
              <span class="summary-value">{{ systemInfo.cpuCores }}核心 {{ systemInfo.cpuModel }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">运行</span>
              <span class="summary-value">{{ systemInfo.uptime }}</span>
            </div>
                         <div class="summary-item">
               <span class="summary-label">网络</span>
               <span class="summary-value">{{ systemInfo.ipAddress }}</span>
             </div>
             <div class="summary-item">
               <span class="summary-label">网速</span>
               <span class="summary-value">{{ systemStats[4]?.value || '初始化中...' }}</span>
             </div>
             <div class="summary-item">
               <span class="summary-label">Docker</span>
               <span class="summary-value">{{ systemInfo.dockerVersion }}</span>
             </div>
            <div class="summary-item">
              <span class="summary-label">内核</span>
              <span class="summary-value">{{ systemInfo.kernel }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 应用网格 - 简洁布局 -->
      <div class="apps-container">
        <div class="all-apps-grid">
          <div
            v-for="app in allApps"
            :key="app.id"
            class="app-card"
            @click="handleAppClick(app)"
          >
            <div class="app-icon">
              <img v-if="app.iconUrl" :src="app.iconUrl" alt="">
              <n-icon 
                v-else-if="app.iconType === 'icon' || !app.iconType" 
                :size="24" 
                :component="app.icon" 
              />
              <span v-else-if="app.iconType === 'text'">{{ app.name.charAt(0) }}</span>
            </div>
            <div class="app-content">
              <div class="app-name">{{ app.name }}</div>
              <div class="app-desc">{{ app.description }}</div>
            </div>
            <div v-if="app.status" class="app-status">
              <div 
                class="status-dot" 
                :class="app.status === 'running' ? 'running' : 'stopped'"
              ></div>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- 浮动操作按钮 -->
    <div class="fab-container">
      <n-button
        type="primary"
        circle
        size="large"
        class="fab-main"
        @click="showFabMenu = !showFabMenu"
      >
        <n-icon :component="showFabMenu ? CloseOutline : MenuOutline" />
      </n-button>
      
      <transition-group name="fab" tag="div" class="fab-menu">
        <n-button
          v-for="(action, index) in fabActions"
          v-show="showFabMenu"
          :key="action.name"
          :type="action.type"
          circle
          class="fab-item"
          :style="{ '--delay': index * 0.1 + 's' }"
          @click="handleFabAction(action)"
        >
          <n-icon :component="action.icon" />
        </n-button>
      </transition-group>
    </div>

    <!-- 添加应用弹窗 -->
    <n-modal 
      v-model:show="showAddApp" 
      preset="card" 
      style="max-width: 600px;"
      title="添加项目"
      size="huge"
      :bordered="false"
      :segmented="false"
    >
      <template #header-extra>
        <n-button quaternary circle @click="showAddApp = false">
          <n-icon :component="CloseOutline" />
        </n-button>
      </template>

      <div class="add-app-form">
        <!-- 效果预览 -->
        <div class="preview-section">
          <div class="preview-options">
            <n-checkbox v-model:checked="previewSettings.showPreview" size="small">
              效果预览（仅供参考）
            </n-checkbox>
            <n-checkbox v-model:checked="previewSettings.transparent" size="small">
              画布透明
            </n-checkbox>
          </div>
          
          <!-- 预览区域 -->
          <div class="preview-area" :class="{ transparent: previewSettings.transparent }">
            <div class="preview-card" :style="{ backgroundColor: newApp.bgColor }">
              <div class="preview-icon">
                <img 
                  v-if="newApp.iconType === 'image' && newApp.iconUrl" 
                  :src="newApp.iconUrl" 
                  alt=""
                >
                <n-icon 
                  v-else-if="newApp.iconType === 'icon'" 
                  :size="24" 
                  :component="CubeOutline" 
                />
                <span v-else-if="newApp.iconType === 'text'">{{ newApp.title.charAt(0) }}</span>
              </div>
              <div class="preview-content">
                <div class="preview-title">{{ newApp.title || '应用标题' }}</div>
                <div class="preview-desc">{{ newApp.description || '应用描述' }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 表单内容 -->
        <n-form :model="newApp" label-placement="top">
          <div class="form-row">
            <div class="form-item">
              <n-form-item label="分组" required>
                <n-select 
                  v-model:value="newApp.category" 
                  :options="categoryOptions"
                  placeholder="选择分组"
                />
              </n-form-item>
            </div>
            <div class="form-item">
              <n-form-item label="卡片类型">
                <n-select 
                  v-model:value="newApp.cardType" 
                  :options="cardTypeOptions"
                  placeholder="选择卡片类型"
                />
              </n-form-item>
            </div>
            <div class="form-item">
              <n-form-item label="卡片背景色">
                <n-color-picker v-model:value="newApp.bgColor" :show-alpha="true" />
              </n-form-item>
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <n-form-item label="标题" required>
                <n-input 
                  v-model:value="newApp.title" 
                  placeholder="请输入标题"
                  :maxlength="20"
                  show-count
                />
              </n-form-item>
            </div>
            <div class="form-item">
              <n-form-item label="描述信息">
                <n-input 
                  v-model:value="newApp.description" 
                  placeholder="请输入描述"
                  :maxlength="100"
                  show-count
                />
              </n-form-item>
            </div>
          </div>

          <n-form-item label="图标风格">
            <n-radio-group v-model:value="newApp.iconType">
              <n-radio value="text">文字</n-radio>
              <n-radio value="image">图片</n-radio>
              <n-radio value="icon">在线图标</n-radio>
            </n-radio-group>
          </n-form-item>

          <n-form-item v-if="newApp.iconType === 'image'" label="图像地址">
            <div class="icon-upload">
              <n-input 
                v-model:value="newApp.iconUrl" 
                placeholder="输入图标地址或上传"
              />
              <n-button>本地上传</n-button>
            </div>
          </n-form-item>

          <n-form-item label="地址" required>
            <div class="url-input">
              <n-input 
                v-model:value="newApp.url" 
                placeholder="http(s)://"
              />
              <n-button quaternary @click="getUrlIcon">获取图标</n-button>
            </div>
          </n-form-item>

          <n-form-item label="内网地址">
            <div class="url-input">
              <n-input 
                v-model:value="newApp.internalUrl" 
                placeholder="http(s):// (内网环境，会跳转该地址)"
              />
              <n-button quaternary @click="getInternalUrlIcon">获取图标</n-button>
            </div>
          </n-form-item>

          <n-form-item label="打开方式">
            <n-radio-group v-model:value="newApp.openType">
              <n-radio value="current">当前窗口</n-radio>
              <n-radio value="new">新窗口</n-radio>
            </n-radio-group>
          </n-form-item>
        </n-form>
      </div>

      <template #action>
        <div class="modal-actions">
          <n-button @click="showAddApp = false">取消</n-button>
          <n-button type="primary" @click="saveApp">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { getSystemStatus } from '@/api/system'
import {
  CubeOutline,
  SearchOutline,
  SettingsOutline,
  SunnyOutline,
  AddOutline,
  MenuOutline,
  CloseOutline,
  ImageOutline,
  GlobeOutline,
  TerminalOutline,
  StatsChartOutline,
  RefreshOutline,
  CloudyOutline,
  RainyOutline,
  SnowOutline,
  ThunderstormOutline,
  PartlySunnyOutline
} from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()

// 时间状态
const currentTime = ref('')
const currentDate = ref('')

// 天气状态
const weatherData = ref({
  temperature: '22°C',
  location: '获取中...',
  icon: markRaw(SunnyOutline),
  loading: true
})

// 界面状态
const searchQuery = ref('')
const showFabMenu = ref(false)
const showAddApp = ref(false)

// 添加应用相关数据
const previewSettings = ref({
  showPreview: true,
  transparent: false
})

const newApp = ref({
  category: '',
  cardType: 'normal',
  bgColor: '#2a2a2a6b',
  title: '',
  description: '',
  iconType: 'icon', // 'text', 'image', 'icon'
  iconUrl: '',
  url: '',
  internalUrl: '',
  openType: 'new' // 'current', 'new'
})

// 系统统计数据
const systemStats = ref([
  {
    title: 'CPU使用率',
    value: '0%',
    percentage: 0,
    color: '#ef4444',
    icon: markRaw(StatsChartOutline),
    description: '处理器负载'
  },
  {
    title: '内存使用',
    value: '0MB',
    total: '0MB',
    percentage: 0,
    color: '#f59e0b',
    icon: markRaw(StatsChartOutline),
    description: '系统内存'
  },
  {
    title: '磁盘使用',
    value: '0%',
    free: '0GB',
    percentage: 0,
    color: '#8b5cf6',
    icon: markRaw(StatsChartOutline),
    description: '存储空间'
  },
  {
    title: '运行容器',
    value: '0',
    total: '0',
    percentage: 0,
    color: '#10b981',
    icon: markRaw(CubeOutline),
    description: 'Docker容器'
  },
  {
    title: '网络速度',
    value: '初始化中...',
    percentage: 0,
    color: '#06b6d4',
    icon: markRaw(GlobeOutline),
    description: '实时网速'
  }
])

// 系统基础信息
const systemInfo = ref({
  hostname: '获取中...',
  os: '获取中...',
  kernel: '获取中...',
  uptime: '获取中...',
  cpuModel: '获取中...',
  cpuCores: 0,
  ipAddress: '获取中...',
  gateway: '获取中...',
  dockerVersion: '获取中...'
})

// 系统状态加载状态
const systemStatusLoading = ref(false)

// 详细信息显示状态
const showDetailedInfo = ref(false)

// 系统状态定时器
let systemStatusTimer: NodeJS.Timeout | null = null

// 应用分类数据 - 外部应用假数据
const appCategories = ref([
  {
    name: '开发工具',
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    icon: markRaw(CubeOutline),
    expanded: true,
    apps: [
      {
        id: 1,
        name: 'GitHub',
        description: '全球最大的代码托管平台',
        iconUrl: 'https://github.githubassets.com/favicons/favicon.svg',
        iconType: 'image',
        externalUrl: 'https://github.com',
        openType: 'new',
        bgColor: '#24292f',
        status: 'running'
      },
      {
        id: 2,
        name: 'VS Code Web',
        description: '在线代码编辑器',
        iconUrl: 'https://code.visualstudio.com/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://vscode.dev',
        openType: 'new',
        bgColor: '#007acc'
      },
      {
        id: 3,
        name: 'Docker Hub',
        description: 'Docker镜像仓库',
        iconUrl: 'https://www.docker.com/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://hub.docker.com',
        openType: 'new',
        bgColor: '#2496ed'
      },
      {
        id: 4,
        name: 'Stack Overflow',
        description: '程序员问答社区',
        iconUrl: 'https://stackoverflow.com/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://stackoverflow.com',
        openType: 'new',
        bgColor: '#f48024'
      },
      {
        id: 5,
        name: 'Postman',
        description: 'API开发测试工具',
        iconType: 'text',
        externalUrl: 'https://www.postman.com',
        openType: 'new',
        bgColor: '#ff6c37'
      },
      {
        id: 6,
        name: 'Figma',
        description: '在线设计协作工具',
        iconUrl: 'https://www.figma.com/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://www.figma.com',
        openType: 'new',
        bgColor: '#f24e1e'
      }
    ]
  },
  {
    name: '效率工具',
    color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    icon: markRaw(ImageOutline),
    expanded: true,
    apps: [
      {
        id: 7,
        name: 'Notion',
        description: '全能工作空间',
        iconUrl: 'https://www.notion.so/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://www.notion.so',
        openType: 'new',
        bgColor: '#000000'
      },
      {
        id: 8,
        name: 'Trello',
        description: '项目管理看板工具',
        iconUrl: 'https://trello.com/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://trello.com',
        openType: 'new',
        bgColor: '#0079bf'
      },
      {
        id: 9,
        name: 'Slack',
        description: '团队协作通讯工具',
        iconUrl: 'https://slack.com/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://slack.com',
        openType: 'new',
        bgColor: '#4a154b'
      },
      {
        id: 10,
        name: 'Zoom',
        description: '视频会议平台',
        iconType: 'text',
        externalUrl: 'https://zoom.us',
        openType: 'new',
        bgColor: '#2d8cff'
      },
      {
        id: 11,
        name: 'Google Drive',
        description: '云端存储与协作',
        iconUrl: 'https://ssl.gstatic.com/docs/doclist/images/drive_2022q3_32dp.png',
        iconType: 'image',
        externalUrl: 'https://drive.google.com',
        openType: 'new',
        bgColor: '#4285f4'
      },
      {
        id: 12,
        name: 'Canva',
        description: '在线设计工具',
        iconUrl: 'https://www.canva.com/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://www.canva.com',
        openType: 'new',
        bgColor: '#00c4cc'
      }
    ]
  },
  {
    name: '监控运维',
    color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    icon: markRaw(GlobeOutline),
    expanded: true,
    apps: [
      {
        id: 13,
        name: 'Grafana',
        description: '数据可视化监控平台',
        iconUrl: 'https://grafana.com/static/img/menu/grafana2.svg',
        iconType: 'image',
        externalUrl: 'http://localhost:3000',
        internalUrl: 'http://192.168.1.100:3000',
        openType: 'new',
        bgColor: '#f46800'
      },
      {
        id: 14,
        name: 'Prometheus',
        description: '系统监控和告警工具',
        iconType: 'text',
        externalUrl: 'http://localhost:9090',
        internalUrl: 'http://192.168.1.100:9090',
        openType: 'new',
        bgColor: '#e6522c'
      },
      {
        id: 15,
        name: 'Portainer',
        description: 'Docker容器管理界面',
        iconUrl: 'https://www.portainer.io/hubfs/portainer-logo-black.svg',
        iconType: 'image',
        externalUrl: 'http://localhost:9443',
        internalUrl: 'http://192.168.1.100:9443',
        openType: 'new',
        bgColor: '#13bef9'
      },
      {
        id: 16,
        name: 'Nginx Proxy',
        description: '反向代理管理界面',
        iconType: 'text',
        externalUrl: 'http://localhost:81',
        internalUrl: 'http://192.168.1.100:81',
        openType: 'new',
        bgColor: '#009639'
      },
      {
        id: 17,
        name: 'Uptime Kuma',
        description: '服务可用性监控',
        iconType: 'text',
        externalUrl: 'http://localhost:3001',
        internalUrl: 'http://192.168.1.100:3001',
        openType: 'new',
        bgColor: '#5cdd8b'
      },
      {
        id: 18,
        name: 'NetData',
        description: '实时性能监控',
        iconUrl: 'https://www.netdata.cloud/img/favicon/favicon.ico',
        iconType: 'image',
        externalUrl: 'http://localhost:19999',
        internalUrl: 'http://192.168.1.100:19999',
        openType: 'new',
        bgColor: '#00ab44'
      }
    ]
  },
  {
    name: '媒体娱乐',
    color: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    icon: markRaw(TerminalOutline),
    expanded: false,
    apps: [
      {
        id: 19,
        name: 'Plex',
        description: '个人媒体服务器',
        iconUrl: 'https://www.plex.tv/favicon.ico',
        iconType: 'image',
        externalUrl: 'http://localhost:32400',
        internalUrl: 'http://192.168.1.100:32400',
        openType: 'new',
        bgColor: '#e5a00d'
      },
      {
        id: 20,
        name: 'Jellyfin',
        description: '开源媒体系统',
        iconType: 'text',
        externalUrl: 'http://localhost:8096',
        internalUrl: 'http://192.168.1.100:8096',
        openType: 'new',
        bgColor: '#00a4dc'
      },
      {
        id: 21,
        name: 'qBittorrent',
        description: 'BT下载客户端',
        iconType: 'text',
        externalUrl: 'http://localhost:8080',
        internalUrl: 'http://192.168.1.100:8080',
        openType: 'new',
        bgColor: '#3daee9'
      },
      {
        id: 22,
        name: 'Spotify',
        description: '音乐流媒体服务',
        iconUrl: 'https://open.spotify.com/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://open.spotify.com',
        openType: 'new',
        bgColor: '#1db954'
      },
      {
        id: 23,
        name: 'YouTube',
        description: '视频分享平台',
        iconUrl: 'https://www.youtube.com/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://www.youtube.com',
        openType: 'new',
        bgColor: '#ff0000'
      },
      {
        id: 24,
        name: 'Twitch',
        description: '游戏直播平台',
        iconUrl: 'https://www.twitch.tv/favicon.ico',
        iconType: 'image',
        externalUrl: 'https://www.twitch.tv',
        openType: 'new',
        bgColor: '#9146ff'
      }
    ]
  }
])

// 浮动操作按钮
const fabActions = ref([
  { name: 'refresh', icon: markRaw(RefreshOutline), type: 'info' },
  { name: 'terminal', icon: markRaw(TerminalOutline), type: 'warning' },
  { name: 'settings', icon: markRaw(SettingsOutline), type: 'default' }
])

// 计算属性
const filteredCategories = computed(() => {
  if (!searchQuery.value) return appCategories.value
  
  return appCategories.value
    .map(category => ({
      ...category,
      apps: category.apps.filter(app =>
        app.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
        app.description.toLowerCase().includes(searchQuery.value.toLowerCase())
      )
    }))
    .filter(category => category.apps.length > 0)
})

// 获取所有应用（不分组）
const allApps = computed(() => {
  const apps: any[] = []
  if (!searchQuery.value) {
    // 没有搜索时，显示所有应用
    appCategories.value.forEach(category => {
      apps.push(...category.apps)
    })
  } else {
    // 有搜索时，只显示匹配的应用
    filteredCategories.value.forEach(category => {
      apps.push(...category.apps)
    })
  }
  return apps
})

// 分组选项
const categoryOptions = computed(() => 
  appCategories.value.map(category => ({
    label: category.name,
    value: category.name
  }))
)

// 卡片类型选项
const cardTypeOptions = ref([
  { label: '普通图标', value: 'normal' },
  { label: '大图标', value: 'large' },
  { label: '文字卡片', value: 'text' }
])

// 方法
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit',
    second: '2-digit'
  })
  currentDate.value = now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}

const handleAppClick = (app: any) => {
  if (app.externalUrl) {
    // 外部链接
    if (app.openType === 'new') {
      window.open(app.externalUrl, '_blank')
    } else {
      window.location.href = app.externalUrl
    }
  } else if (app.internalUrl) {
    // 内网链接
    if (app.openType === 'new') {
      window.open(app.internalUrl, '_blank')
    } else {
      window.location.href = app.internalUrl
    }
  } else if (app.route) {
    // 内部路由
    router.push(app.route)
  }
}

const handleFabAction = (action: any) => {
  showFabMenu.value = false
  switch (action.name) {
    case 'refresh':
      // 刷新数据
      loadSystemStatus()
      getLocationAndWeather()
      message.success('数据刷新中，网速需5-10秒显示准确值')
      break
    case 'terminal':
      // 打开终端
      console.log('打开终端')
      break
    case 'settings':
      router.push('/settings')
      break
  }
}

// 保存应用
const saveApp = () => {
  if (!newApp.value.title || !newApp.value.category || !newApp.value.url) {
    // 这里应该显示错误提示
    console.log('请填写必填字段')
    return
  }

  // 生成新的应用ID
  const newId = Date.now()
  
  // 创建新应用对象
  const appToAdd = {
    id: newId,
    name: newApp.value.title,
    description: newApp.value.description,
    icon: markRaw(CubeOutline), // 默认图标
    iconUrl: newApp.value.iconType === 'image' ? newApp.value.iconUrl : '',
    route: newApp.value.url.startsWith('http') ? '' : newApp.value.url,
    externalUrl: newApp.value.url.startsWith('http') ? newApp.value.url : '',
    internalUrl: newApp.value.internalUrl,
    bgColor: newApp.value.bgColor,
    iconType: newApp.value.iconType,
    openType: newApp.value.openType
  }

  // 找到对应的分组并添加应用
  const categoryIndex = appCategories.value.findIndex(cat => cat.name === newApp.value.category)
  if (categoryIndex !== -1) {
    appCategories.value[categoryIndex].apps.push(appToAdd)
  }

  // 重置表单并关闭弹窗
  resetForm()
  showAddApp.value = false
  
  console.log('应用添加成功:', appToAdd)
}

// 重置表单
const resetForm = () => {
  newApp.value = {
    category: '',
    cardType: 'normal',
    bgColor: '#2a2a2a6b',
    title: '',
    description: '',
    iconType: 'icon',
    iconUrl: '',
    url: '',
    internalUrl: '',
    openType: 'new'
  }
}

// 获取图标
const getUrlIcon = () => {
  if (newApp.value.url) {
    // 模拟获取网站图标
    try {
      const url = new URL(newApp.value.url)
      newApp.value.iconUrl = `${url.origin}/favicon.ico`
      newApp.value.iconType = 'image'
      console.log('获取图标:', newApp.value.iconUrl)
    } catch {
      console.log('无效的URL')
    }
  }
}

// 获取内网图标
const getInternalUrlIcon = () => {
  if (newApp.value.internalUrl) {
    // 模拟获取内网图标
    try {
      const url = new URL(newApp.value.internalUrl)
      newApp.value.iconUrl = `${url.origin}/favicon.ico`
      newApp.value.iconType = 'image'
      console.log('获取内网图标:', newApp.value.iconUrl)
    } catch {
      console.log('无效的内网URL')
    }
  }
}

// 获取系统状态
const loadSystemStatus = async () => {
  if (systemStatusLoading.value) return
  
  systemStatusLoading.value = true
  
  try {
    await getSystemStatus({
      onComplete: (data) => {
        const status = data.data
        if (status) {
          // 更新系统统计数据
          // 0. CPU使用率
          if (status.cpuUsage !== undefined) {
            systemStats.value[0].value = `${status.cpuUsage.toFixed(1)}%`
            systemStats.value[0].percentage = status.cpuUsage
          }
          
          // 1. 内存使用
          if (status.memoryTotal && status.memoryUsed) {
            const memoryUsedGB = (status.memoryUsed / 1024).toFixed(1)
            const memoryTotalGB = (status.memoryTotal / 1024).toFixed(1)
            systemStats.value[1].value = `${memoryUsedGB}GB`
            systemStats.value[1].total = `${memoryTotalGB}GB`
            systemStats.value[1].percentage = status.memoryUsage || 0
          }
          
          // 2. 磁盘使用
          if (status.diskUsage && status.diskFree) {
            systemStats.value[2].value = status.diskUsage
            systemStats.value[2].free = status.diskFree
            // 从百分比字符串中提取数字
            const diskPercent = parseInt(status.diskUsage.replace('%', ''))
            systemStats.value[2].percentage = isNaN(diskPercent) ? 0 : diskPercent
          }
          
          // 3. 运行容器
          systemStats.value[3].value = status.runningContainers?.toString() || '0'
          systemStats.value[3].total = status.totalContainers?.toString() || '0'
          systemStats.value[3].percentage = status.totalContainers > 0 
            ? Math.round((status.runningContainers / status.totalContainers) * 100) 
            : 0
          
          // 4. 网络速度
          if (status.networkDownloadSpeed && status.networkUploadSpeed) {
            // 检查是否为特殊状态
            if (status.networkDownloadSpeed.includes('初始化') || status.networkDownloadSpeed.includes('计算中') || status.networkDownloadSpeed.includes('未知') || status.networkDownloadSpeed.includes('获取失败')) {
              systemStats.value[4].value = status.networkDownloadSpeed
              systemStats.value[4].percentage = 0
            } else {
              systemStats.value[4].value = `↓${status.networkDownloadSpeed} ↑${status.networkUploadSpeed}`
              // 根据网速设置信号强度（用下载速度计算）
              const downloadSpeedRaw = status.networkDownloadSpeedRaw || 0
              
              // 信号强度分级：
              // 0: 无网络 (0KB/s)
              // 25: 低速 (0-100KB/s) 
              // 50: 中速 (100KB/s-1MB/s)
              // 75: 高速 (1MB/s-10MB/s)
              // 100: 极速 (>10MB/s)
              let signalStrength = 0
              if (downloadSpeedRaw > 0) {
                if (downloadSpeedRaw < 100 * 1024) {        // < 100KB/s
                  signalStrength = 25
                } else if (downloadSpeedRaw < 1024 * 1024) { // < 1MB/s
                  signalStrength = 50
                } else if (downloadSpeedRaw < 10 * 1024 * 1024) { // < 10MB/s
                  signalStrength = 75
                } else {                                      // >= 10MB/s
                  signalStrength = 100
                }
              }
              systemStats.value[4].percentage = signalStrength
            }
          } else {
            systemStats.value[4].value = '0KB/s'
            systemStats.value[4].percentage = 0
          }
          
          // 更新系统基础信息
          systemInfo.value = {
            hostname: status.hostname || '未知',
            os: status.os || '未知',
            kernel: status.kernel || '未知',
            uptime: status.uptime || '未知',
            cpuModel: status.cpuModel || '未知',
            cpuCores: status.cpuCores || 0,
            ipAddress: status.ipAddress || '未知',
            gateway: status.gateway || '未知',
            dockerVersion: status.dockerVersion || '未知'
          }
        }
      },
      onError: (error) => {
        console.error('获取系统状态失败:', error)
        message.error('获取系统状态失败: ' + error)
      }
    })
  } catch (error) {
    console.error('获取系统状态失败:', error)
    message.error('获取系统状态失败')
  } finally {
    systemStatusLoading.value = false
  }
}

// 显示天气详情
const showWeatherDetails = () => {
  // 可以跳转到天气详情页面或显示弹窗
  console.log('天气详情:', weatherData.value)
  // 这里可以添加更多功能，比如刷新天气或显示详细预报
  getLocationAndWeather() // 刷新天气
}

// 获取用户位置和天气
const getLocationAndWeather = async () => {
  try {
    // 获取用户位置
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const { latitude, longitude } = position.coords
          
          // 获取天气数据
          const weatherResponse = await fetch(
            'https://api.open-meteo.com/v1/forecast?' +
            `latitude=${latitude}&longitude=${longitude}&` +
            'current=temperature_2m,weather_code&timezone=auto'
          )
          const weatherResult = await weatherResponse.json()
          
          // 更新天气信息
          const temp = Math.round(weatherResult.current.temperature_2m)
          const weatherCode = weatherResult.current.weather_code
          
          weatherData.value = {
            temperature: `${temp}°C`,
            location: getLocationFromTimezone(weatherResult.timezone),
            icon: getWeatherIcon(weatherCode),
            loading: false
          }
        },
        () => {
          // 位置获取失败，使用默认位置（北京）
          getDefaultWeather()
        }
      )
    } else {
      // 不支持地理位置，使用默认
      getDefaultWeather()
    }
  } catch (error) {
    console.error('获取天气失败:', error)
    getDefaultWeather()
  }
}

// 获取默认天气（北京）
const getDefaultWeather = async () => {
  try {
    const response = await fetch(
      'https://api.open-meteo.com/v1/forecast?' +
      'latitude=39.9042&longitude=116.4074&' +
      'current=temperature_2m,weather_code&' +
      'timezone=Asia/Shanghai'
    )
    const result = await response.json()
    
    const temp = Math.round(result.current.temperature_2m)
    const weatherCode = result.current.weather_code
    
    weatherData.value = {
      temperature: `${temp}°C`,
      location: '北京',
      icon: getWeatherIcon(weatherCode),
      loading: false
    }
  } catch (error) {
    console.error('获取默认天气失败:', error)
    weatherData.value = {
      temperature: '22°C',
      location: '位置未知',
      icon: markRaw(SunnyOutline),
      loading: false
    }
  }
}

// 从时区获取位置名称
const getLocationFromTimezone = (timezone: string) => {
  const cityMap: Record<string, string> = {
    'Asia/Shanghai': '上海',
    'Asia/Beijing': '北京',
    'Asia/Tokyo': '东京',
    'America/New_York': '纽约',
    'America/Los_Angeles': '洛杉矶',
    'Europe/London': '伦敦',
    'Europe/Paris': '巴黎',
    'Australia/Sydney': '悉尼'
  }
  
  return cityMap[timezone] || timezone.split('/').pop()?.replace('_', ' ') || timezone
}

// 根据天气代码获取图标
const getWeatherIcon = (weatherCode: number) => {
  // WMO Weather interpretation codes (WW)
  if (weatherCode === 0) return markRaw(SunnyOutline) // 晴天
  if (weatherCode <= 3) return markRaw(PartlySunnyOutline) // 晴到多云
  if (weatherCode <= 48) return markRaw(CloudyOutline) // 雾
  if (weatherCode <= 67) return markRaw(RainyOutline) // 雨
  if (weatherCode <= 77) return markRaw(SnowOutline) // 雪
  if (weatherCode <= 82) return markRaw(RainyOutline) // 阵雨
  if (weatherCode <= 86) return markRaw(SnowOutline) // 阵雪
  if (weatherCode <= 99) return markRaw(ThunderstormOutline) // 雷暴
  
  return markRaw(SunnyOutline)
}

let timeInterval: NodeJS.Timeout

onMounted(async () => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  getLocationAndWeather()
  
  // 立即加载一次系统状态
  await loadSystemStatus()
  
  // 启动系统状态定时刷新（5秒间隔）
  systemStatusTimer = setInterval(() => {
    loadSystemStatus()
  }, 5000)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
  if (systemStatusTimer) {
    clearInterval(systemStatusTimer)
  }
})
</script>

<style scoped>
.navigation-home {
  min-height: 100vh;
  position: relative;
  background: transparent;
  overflow-x: hidden;
}

/* 背景装饰 - 简化版本 */
.bg-decoration {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.15);
}

.bg-pattern {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: none;
}

.floating-shapes {
  display: none;
}

/* 主要内容区域 */
.main-content {
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 顶部导航 */
.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: transparent;
  backdrop-filter: blur(20px);
  border: none;
  border-radius: 16px;
  padding: 12px 20px;
  margin-bottom: 20px;
  width: 100%;
  max-width: 1200px;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.logo-text h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #f8fafc;
  line-height: 1.2;
}

.logo-text p {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.2;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.datetime-display {
  text-align: center;
}

.time {
  font-size: 24px;
  font-weight: 700;
  color: #f8fafc;
  line-height: 1;
  margin-bottom: 2px;
  font-family: 'SF Mono', 'Monaco', 'Cascadia Code', monospace;
}

.date {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.weather-widget {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 12px;
  padding: 8px 12px;
  color: #ffffff;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.weather-widget:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.weather-location {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* 搜索区域 */
.search-section {
  margin-bottom: 20px;
  width: 100%;
  display: flex;
  justify-content: center;
}

.search-container {
  max-width: 500px;
  width: 100%;
}

.search-container :deep(.n-input) {
  background: transparent;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.search-container :deep(.n-input__input-el) {
  background: transparent;
  color: #f8fafc;
  font-size: 14px;
}

.search-container :deep(.n-input__placeholder) {
  color: #64748b;
}

.search-container :deep(.n-input):hover {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.search-container :deep(.n-input--focus) {
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(59, 130, 246, 0.3) !important;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1) !important;
}

/* 系统状态总览 - 紧凑版 */
.stats-section {
  margin-bottom: 16px;
  width: 100%;
  max-width: 1200px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  margin-bottom: 12px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 紧凑型性能卡片网格 */
.compact-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.compact-stat-card {
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  padding: 12px;
  transition: all 0.3s ease;
  backdrop-filter: blur(20px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.compact-stat-card:hover {
  background: rgba(0, 0, 0, 0.35);
  border-color: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.4);
}

.compact-stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon-small {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.5));
}

.stat-main {
  flex: 1;
  min-width: 0;
}

.stat-title-small {
  font-size: 10px;
  color: #e2e8f0;
  margin-bottom: 2px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.stat-value-large {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.2;
  margin-bottom: 1px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.7);
}

.stat-extra-small {
  font-size: 9px;
  color: #cbd5e1;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.free-space-small {
  color: #34d399;
  font-weight: 500;
}

.stat-percentage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.percentage-text {
  font-size: 9px;
  color: #e2e8f0;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

/* 网络速度指示器样式 */
.stat-network-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.network-status-text {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.status-text {
  font-size: 8px;
  color: #94a3b8;
  text-align: center;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.network-status {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.signal-bars {
  display: flex;
  align-items: end;
  gap: 2px;
  height: 16px;
}

.signal-bars .bar {
  width: 3px;
  border-radius: 1px;
  background: rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.signal-bars .bar:nth-child(1) { height: 4px; }
.signal-bars .bar:nth-child(2) { height: 7px; }
.signal-bars .bar:nth-child(3) { height: 10px; }
.signal-bars .bar:nth-child(4) { height: 13px; }

.signal-bars .bar.active {
  background: #06b6d4;
  box-shadow: 0 0 4px rgba(6, 182, 212, 0.4);
}

.network-status.active .signal-bars .bar.active {
  animation: pulse-bar 2s ease-in-out infinite;
}

@keyframes pulse-bar {
  0%, 100% {
    opacity: 1;
    transform: scaleY(1);
  }
  50% {
    opacity: 0.7;
    transform: scaleY(1.2);
  }
}

/* 详细系统信息 - 可折叠 */
.detailed-info-section {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.info-summary {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px;
  backdrop-filter: blur(20px);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 0;
}

.summary-label {
  font-size: 9px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.summary-value {
  font-size: 11px;
  color: #ffffff;
  font-weight: 500;
  word-break: break-all;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.7);
}

/* 应用网格 - 简洁布局 */
.apps-container {
  margin-bottom: 24px;
  width: 100%;
  display: flex;
  justify-content: center;
}

.all-apps-grid {
  padding: 0 0 18px;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
  max-width: 1200px;
  width: 100%;
  justify-content: center;
}

.app-card {
  background: rgba(255, 255, 255, 0.15);
  border: none;
  border-radius: 16px;
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(20px);
  min-height: 50px;
  position: relative;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.app-card:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-3px);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.2);
}

.app-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  flex-shrink: 0;
}

.app-icon img {
  width: 24px;
  height: 24px;
  border-radius: 4px;
}

.app-icon span {
  font-size: 14px;
  font-weight: 600;
  color: #3b82f6;
  text-transform: uppercase;
}

.app-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
}

.app-name {
  font-size: 13px;
  font-weight: 600;
  color: #f8fafc;
  margin-bottom: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.2;
}

.app-desc {
  font-size: 10px;
  color: #94a3b8;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-status {
  position: absolute;
  top: 8px;
  right: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.running {
  background-color: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
}

.stopped {
  background-color: #ef4444;
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.5);
}

/* 浮动操作按钮 */
.fab-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 100;
}

.fab-main {
  width: 50px;
  height: 50px;
  box-shadow: 0 6px 24px rgba(59, 130, 246, 0.3);
}

.fab-menu {
  position: absolute;
  bottom: 60px;
  right: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fab-item {
  width: 42px;
  height: 42px;
  animation: fabIn 0.3s ease var(--delay) both;
}

/* 动画 */
.fab-enter-active,
.fab-leave-active {
  transition: all 0.3s ease;
}

.fab-enter-from,
.fab-leave-to {
  opacity: 0;
  transform: scale(0.5) translateX(20px);
}

@keyframes fabIn {
  from {
    opacity: 0;
    transform: scale(0.5) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .all-apps-grid {
    grid-template-columns: repeat(5, 1fr);
    gap: 18px;
  }
}

@media (max-width: 1200px) {
  .all-apps-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }
}

@media (max-width: 1024px) {
  .main-content {
    padding: 12px;
  }
  
  .compact-stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 6px;
  }
  
  .compact-stat-card {
    padding: 10px;
  }
  
  .stat-value-large {
    font-size: 14px;
  }
  
  .info-summary {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 6px;
  }
  
  .all-apps-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 14px;
    padding: 0 0 16px;
  }
  
  .app-card {
    padding: 16px;
    min-height: 70px;
    gap: 12px;
  }
  
  .app-icon {
    width: 32px;
    height: 32px;
  }
  
  .app-icon img {
    width: 24px;
    height: 24px;
  }
  
  .app-name {
    font-size: 12px;
  }
  
  .app-desc {
    font-size: 10px;
  }
}

@media (max-width: 768px) {
  .top-header {
    flex-direction: column;
    gap: 12px;
    text-align: center;
    padding: 10px 16px;
  }
  
  .header-left,
  .header-right {
    order: 2;
  }
  
  .header-center {
    order: 1;
  }
  
  .time {
    font-size: 20px;
  }
  
  .compact-stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 6px;
  }
  
  .compact-stat-card {
    padding: 10px;
  }
  
  .compact-stat-content {
    gap: 8px;
  }
  
  .stat-icon-small {
    width: 20px;
    height: 20px;
  }
  
  .stat-value-large {
    font-size: 13px;
  }
  
  .stat-title-small {
    font-size: 9px;
  }
  
  .stat-extra-small {
    font-size: 8px;
  }
  
  .percentage-text {
    font-size: 8px;
  }
  
  .info-summary {
    grid-template-columns: 1fr;
    gap: 6px;
    padding: 10px;
  }
  
  .summary-item {
    padding: 4px 0;
  }
  
  .summary-label {
    font-size: 8px;
  }
  
  .summary-value {
    font-size: 10px;
  }
  
  .all-apps-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 0 0 16px;
  }
  
  .app-card {
    padding: 12px;
    min-height: 60px;
    gap: 10px;
  }
  
  .app-icon {
    width: 28px;
    height: 28px;
  }
  
  .app-icon img {
    width: 20px;
    height: 20px;
  }
  
  .app-name {
    font-size: 11px;
  }
  
  .app-desc {
    font-size: 9px;
  }
  
  .fab-container {
    bottom: 16px;
    right: 16px;
  }
}

@media (max-width: 480px) {
  .main-content {
    padding: 8px;
  }
  
  .logo-section {
    gap: 8px;
  }
  
  .logo-icon {
    width: 32px;
    height: 32px;
  }
  
  .logo-text h1 {
    font-size: 16px;
  }
  
  .logo-text p {
    font-size: 10px;
  }
  
  .time {
    font-size: 18px;
  }
  
  .date {
    font-size: 10px;
  }
  
  .section-header h3 {
    font-size: 14px;
  }
  
  .compact-stats-grid {
    grid-template-columns: 1fr;
    gap: 4px;
  }
  
  .compact-stat-card {
    padding: 8px;
  }
  
  .compact-stat-content {
    gap: 6px;
  }
  
  .stat-icon-small {
    width: 18px;
    height: 18px;
  }
  
  .stat-value-large {
    font-size: 11px;
  }
  
  .stat-title-small {
    font-size: 8px;
  }
  
  .stat-extra-small {
    font-size: 7px;
  }
  
  .percentage-text {
    font-size: 7px;
  }
  
  .info-summary {
    grid-template-columns: 1fr;
    gap: 4px;
    padding: 8px;
  }
  
  .summary-item {
    padding: 3px 0;
  }
  
  .summary-label {
    font-size: 7px;
  }
  
  .summary-value {
    font-size: 9px;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 4px;
  }
  
  .all-apps-grid {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 0 0 8px;
  }
  
  .app-card {
    padding: 10px;
    min-height: 50px;
    gap: 8px;
  }
  
  .app-icon {
    width: 24px;
    height: 24px;
  }
  
  .app-icon img {
    width: 18px;
    height: 18px;
  }
  
  .app-name {
    font-size: 10px;
  }
  
  .app-desc {
    font-size: 8px;
  }
  
  .search-container {
    max-width: 100%;
  }
}

/* 添加应用弹窗样式 */
.add-app-form {
  padding: 20px 0;
}

.preview-section {
  margin-bottom: 24px;
}

.preview-options {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.preview-area {
  background: repeating-conic-gradient(#808080 0% 25%, transparent 0% 50%) 50% / 20px 20px;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 120px;
}

.preview-area.transparent {
  background: transparent;
}

.preview-card {
  background: #2a2a2a6b;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
  backdrop-filter: blur(10px);
}

.preview-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  font-size: 18px;
  font-weight: 600;
}

.preview-icon img {
  width: 28px;
  height: 28px;
  border-radius: 6px;
}

.preview-content {
  flex: 1;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #f8fafc;
  margin-bottom: 4px;
}

.preview-desc {
  font-size: 12px;
  color: #94a3b8;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.form-item {
  min-width: 0;
}

.icon-upload {
  display: flex;
  gap: 8px;
}

.icon-upload .n-input {
  flex: 1;
}

.url-input {
  display: flex;
  gap: 8px;
}

.url-input .n-input {
  flex: 1;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 弹窗响应式 */
@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .preview-card {
    min-width: 160px;
    padding: 12px;
  }
  
  .preview-icon {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }
  
  .preview-icon img {
    width: 24px;
    height: 24px;
  }
  
  .icon-upload,
  .url-input {
    flex-direction: column;
    gap: 8px;
  }
}
</style> 