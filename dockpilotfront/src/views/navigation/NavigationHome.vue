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
          <div class="weather-widget">
            <n-icon size="20" :component="SunnyOutline" />
            <span>22°C</span>
          </div>
          <n-button quaternary circle @click="showSettings = true">
            <n-icon size="18" :component="SettingsOutline" />
          </n-button>
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

      <!-- 快速统计卡片 -->
      <div class="stats-section">
        <div class="section-header">
          <h3>系统状态</h3>
          <n-button quaternary size="tiny" @click="showAddApp = true">
            <n-icon :component="AddOutline" />
          </n-button>
        </div>
        <div class="stats-grid">
          <div 
            v-for="stat in systemStats" 
            :key="stat.title"
            class="stat-card"
          >
            <div class="stat-header">
              <div class="stat-icon">
                <n-icon :size="16" :component="stat.icon" />
              </div>
              <div class="stat-info">
                <div class="stat-title">{{ stat.title }}</div>
                <div class="stat-value">{{ stat.value }}</div>
              </div>
            </div>
            <div class="stat-progress">
              <n-progress
                type="line"
                :percentage="stat.percentage"
                :height="3"
                :show-indicator="false"
                :color="stat.color"
                :rail-color="'rgba(255,255,255,0.08)'"
              />
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
              <n-icon v-else-if="app.iconType === 'icon' || !app.iconType" :size="24" :component="app.icon" />
              <span v-else-if="app.iconType === 'text'">{{ app.name.charAt(0) }}</span>
            </div>
            <div class="app-content">
              <div class="app-name">{{ app.name }}</div>
              <div class="app-desc">{{ app.description }}</div>
            </div>
            <div class="app-status" v-if="app.status">
              <div class="status-dot" :class="app.status === 'running' ? 'running' : 'stopped'"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 快速操作按钮 -->
      <div class="quick-actions">
        <n-button 
          type="primary" 
          size="large" 
          round
          @click="router.push('/containers/create')"
        >
          <template #icon>
            <n-icon :component="AddOutline" />
          </template>
          创建容器
        </n-button>
        <n-button 
          type="success" 
          size="large" 
          round
          @click="router.push('/images')"
        >
          <template #icon>
            <n-icon :component="CloudDownloadOutline" />
          </template>
          拉取镜像
        </n-button>
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
          v-show="showFabMenu"
          v-for="(action, index) in fabActions"
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
                <img v-if="newApp.iconType === 'image' && newApp.iconUrl" :src="newApp.iconUrl" alt="">
                <n-icon v-else-if="newApp.iconType === 'icon'" :size="24" :component="CubeOutline" />
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

          <n-form-item label="图像地址" v-if="newApp.iconType === 'image'">
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
import {
  CubeOutline,
  SearchOutline,
  SettingsOutline,
  SunnyOutline,
  EllipsisHorizontalOutline,
  AddOutline,
  CloudDownloadOutline,
  MenuOutline,
  CloseOutline,
  ImageOutline,
  GlobeOutline,
  TerminalOutline,
  StatsChartOutline,
  RefreshOutline
} from '@vicons/ionicons5'

const router = useRouter()

// 时间状态
const currentTime = ref('')
const currentDate = ref('')

// 界面状态
const searchQuery = ref('')
const showSettings = ref(false)
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
    title: '运行容器',
    value: '12',
    trend: '+8.5%',
    percentage: 75,
    color: '#10b981',
    icon: markRaw(CubeOutline)
  },
  {
    title: '镜像数量',
    value: '28',
    trend: '+12.3%',
    percentage: 60,
    color: '#3b82f6',
    icon: markRaw(ImageOutline)
  },
  {
    title: '网络连接',
    value: '5',
    trend: '+2.1%',
    percentage: 40,
    color: '#8b5cf6',
    icon: markRaw(GlobeOutline)
  },
  {
    title: '内存使用',
    value: '6.2GB',
    trend: '+5.3%',
    percentage: 68,
    color: '#f59e0b',
    icon: markRaw(StatsChartOutline)
  },
  {
    title: 'CPU使用率',
    value: '45%',
    trend: '-3.2%',
    percentage: 45,
    color: '#ef4444',
    icon: markRaw(StatsChartOutline)
  }
])

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
  const apps = []
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
      console.log('刷新数据')
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
    } catch (error) {
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
    } catch (error) {
      console.log('无效的内网URL')
    }
  }
}

let timeInterval: NodeJS.Timeout

onMounted(async () => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
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
  gap: 6px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  padding: 6px 10px;
  color: #3b82f6;
  font-weight: 500;
  font-size: 14px;
}

/* 搜索区域 */
.search-section {
  margin-bottom: 20px;
}

.search-container {
  max-width: 500px;
  margin: 0 auto;
}

.search-container :deep(.n-input) {
  background: transparent;
  backdrop-filter: blur(10px);
  border: none;
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
}

.search-container :deep(.n-input--focus) {
  background: rgba(255, 255, 255, 0.08) !important;
  box-shadow: none !important;
}

/* 统计卡片网格 */
.stats-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0px;
  margin-bottom: 8px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #f8fafc;
  opacity: 0.9;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 8px;
  margin-bottom: 20px;
  background: transparent;
  border: none;
  padding: 16px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.03);
  border: none;
  border-radius: 8px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: all 0.3s ease;
  min-height: 50px;
  backdrop-filter: blur(10px);
}

.stat-card:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.stat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.stat-icon {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: rgba(59, 130, 246, 0.15);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-title {
  font-size: 10px;
  color: #94a3b8;
  margin-bottom: 1px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 500;
}

.stat-value {
  font-size: 13px;
  font-weight: 600;
  color: #f8fafc;
  line-height: 1;
}

.stat-progress {
  width: 100%;
  margin-top: 2px;
}

.stat-progress :deep(.n-progress-line) {
  height: 3px !important;
  border-radius: 2px !important;
}

.stat-progress :deep(.n-progress-line__fill) {
  border-radius: 2px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stat-progress :deep(.n-progress-line__rail) {
  background: rgba(255, 255, 255, 0.08) !important;
  border-radius: 2px !important;
}

/* 应用网格 - 简洁布局 */
.apps-container {
  margin-bottom: 24px;
}

.all-apps-grid {
  padding: 0 0 18px;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.app-card {
  background: rgba(255, 255, 255, 0.15);
  border: none;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(20px);
  min-height: 80px;
  position: relative;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.app-card:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-3px);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.2);
}

.app-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  flex-shrink: 0;
}

.app-icon img {
  width: 28px;
  height: 28px;
  border-radius: 6px;
}

.app-icon span {
  font-size: 16px;
  font-weight: 600;
  color: #3b82f6;
  text-transform: uppercase;
}

.app-content {
  flex: 1;
  min-width: 0;
}

.app-name {
  font-size: 14px;
  font-weight: 600;
  color: #f8fafc;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-desc {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.3;
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

/* 快速操作 */
.quick-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}

.quick-actions :deep(.n-button) {
  font-size: 14px;
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
  
  .stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
    gap: 6px;
    padding: 12px;
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
  
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 4px;
    padding: 10px;
  }
  
  .stat-card {
    padding: 8px;
    gap: 4px;
    min-height: 40px;
  }
  
  .stat-icon {
    width: 16px;
    height: 16px;
  }
  
  .stat-value {
    font-size: 11px;
  }
  
  .stat-title {
    font-size: 8px;
  }
  
  .stat-progress :deep(.n-progress-line) {
    height: 2px !important;
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
  
  .quick-actions {
    flex-direction: column;
    align-items: center;
    gap: 12px;
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
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 3px;
    padding: 8px;
  }
  
  .stat-card {
    padding: 6px;
    gap: 3px;
    min-height: 35px;
  }
  
  .stat-icon {
    width: 14px;
    height: 14px;
  }
  
  .stat-value {
    font-size: 10px;
  }
  
  .stat-title {
    font-size: 7px;
  }
  
  .stat-progress :deep(.n-progress-line) {
    height: 2px !important;
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