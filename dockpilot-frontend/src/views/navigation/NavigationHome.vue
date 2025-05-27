<template>
  <div class="navigation-home">
    <!-- 固定背景 -->
    <div class="navigation-background" ref="navigationBackgroundRef"></div>
    
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-gradient"></div>
    </div>

    <div class="main-content" v-show="!pageLoading">
      <!-- 顶部区域 -->
      <header class="top-header">
        <div class="header-left">
          <div class="logo-section">
            <div class="logo-icon">
              <n-icon size="32" :component="CubeOutline" />
            </div>
            <div class="logo-text">
              <h1>Dock Pilot</h1>
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
          <WeatherWidget />
          
          <!-- 网络模式切换按钮 -->
          <div class="network-mode-toggle">
            <n-button
              size="small"
              quaternary
              circle
              @click="toggleNetworkMode"
              :title="isInternalMode ? '当前：内网模式，点击切换到外网' : '当前：外网模式，点击切换到内网'"
              class="mode-toggle-btn"
            >
              <template #icon>
                <n-icon :size="16" :component="isInternalMode ? WifiOutline : GlobeOutline" />
              </template>
            </n-button>
          </div>
        </div>
      </header>

      <!-- 搜索区域 -->
      <SearchBar v-model="searchQuery" />

      <!-- 系统状态总览 - 紧凑版 -->
      <SystemStats ref="systemStatsRef" />

      <!-- 应用网格 -->
      <AppGrid 
        :categories="appCategories"
        :search-query="searchQuery"
        @app-click="handleAppClick"
        @image-error="handleImageError"
        @image-load="handleImageLoad"
        @edit-app="handleEditApp"
        @delete-app="handleDeleteApp"
        @sort-changed="handleSortChanged"
        @move-to-category="handleMoveToCategory"
      />

    </div>

    <!-- 加载状态 -->
    <div v-show="pageLoading" class="loading-overlay">
      <div class="loading-content">
        <n-spin size="large" />
        <p>加载中...</p>
      </div>
    </div>

    <!-- 浮动操作按钮 -->
    <FloatingActionButton @action="handleFabAction" />

    <!-- 添加应用弹窗 -->
    <AddAppModal 
      v-model="showAddApp"
      :category-options="categoryOptions"
      @save="saveApp"
    />

    <!-- 编辑应用弹窗 -->
    <AddAppModal 
      v-model="showEditApp"
      :category-options="categoryOptions"
      :edit-mode="true"
      :app-data="editingApp"
      @update="updateApp"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import { getCategories, getWebServers, createWebServer, updateWebServer, deleteWebServer, batchUpdateWebServerSort, type CategoryVO, type WebServerVO, type CreateWebServerRequest, type UpdateWebServerRequest } from '@/api/http/webserver'
import { getCurrentBackground } from '@/api/http/background'
import defaultBackgroundImg from '@/assets/background.png'
// 导入组件
import WeatherWidget from './components/WeatherWidget.vue'
import SystemStats from './components/SystemStats.vue'
import SearchBar from './components/SearchBar.vue'
import FloatingActionButton from './components/FloatingActionButton.vue'
import AddAppModal from './components/AddAppModal.vue'
import AppGrid from './components/AppGrid.vue'
import {
  CubeOutline,
  WifiOutline,
  GlobeOutline
} from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

// 时间状态
const currentTime = ref('')
const currentDate = ref('')

// 界面状态
const searchQuery = ref('')
const showAddApp = ref(false)
const showEditApp = ref(false)
const pageLoading = ref(true)

// 编辑应用相关状态
const editingApp = ref<any>(null)

// 简单防抖
let sortTimeout: NodeJS.Timeout | null = null
let moveTimeout: NodeJS.Timeout | null = null

// 内外网模式切换
const isInternalMode = ref(false) // false: 外网模式, true: 内网模式

// 组件引用
const systemStatsRef = ref()
const navigationBackgroundRef = ref<HTMLElement>()

// 应用分类和应用数据
const categories = ref<CategoryVO[]>([])
const webServers = ref<WebServerVO[]>([])
const dataLoading = ref(false)

// 图片加载错误状态
const imageErrors = ref<Record<string, boolean>>({})

// 应用分类数据（组织后的数据结构）
const appCategories = computed(() => {
  if (!categories.value.length || !webServers.value.length) return []
  
  return categories.value.map(category => ({
    id: category.id,
    name: category.name,
    sortOrder: category.sortOrder,
    appCount: category.appCount,
    color: '', // 不再使用背景色
    icon: markRaw(CubeOutline),
    expanded: true,
    apps: webServers.value
      .filter(app => app.categoryId === category.id)
      .map(app => ({
        id: app.id.toString(),
        name: app.name,
        description: app.description || '',
        iconUrl: app.icon || '',
        iconType: app.iconType || 'image',
        externalUrl: app.externalUrl || '',
        internalUrl: app.internalUrl || '',
        openType: app.openType || 'new',
        bgColor: app.bgColor || 'rgba(255, 255, 255, 0.15)',
        cardType: app.cardType || 'normal',
        categoryId: app.categoryId, // 添加 categoryId 字段
        icon: markRaw(CubeOutline),
        imageError: imageErrors.value[app.id.toString()] || false
      }))
      .sort((a, b) => {
        const aApp = webServers.value.find(ws => ws.id === a.id)
        const bApp = webServers.value.find(ws => ws.id === b.id)
        return (aApp?.itemSort || 0) - (bApp?.itemSort || 0)
      })
  }))
})

// 分组选项
const categoryOptions = computed(() => 
  categories.value.map(category => ({
    label: category.name,
    value: category.id
  }))
)

// 方法
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit'
    // 移除秒数显示以减少更新频率
  })
  currentDate.value = now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}

const handleAppClick = (app: any) => {
  let targetUrl = ''
  
  // 根据当前模式选择地址
  if (isInternalMode.value) {
    // 内网模式：优先使用内网地址
    targetUrl = app.internalUrl || app.externalUrl || ''
  } else {
    // 外网模式：优先使用外网地址
    targetUrl = app.externalUrl || app.internalUrl || ''
  }
  
  if (targetUrl) {
    if (app.openType === 'new') {
      window.open(targetUrl, '_blank')
    } else {
      window.location.href = targetUrl
    }
  } else if (app.route) {
    // 内部路由
    router.push(app.route)
  } else {
    // 没有可用地址时的提示
    const mode = isInternalMode.value ? '内网' : '外网'
    message.warning(`该应用暂无可用的${mode}地址`)
  }
}

const handleFabAction = (actionName: string) => {
  switch (actionName) {
    case 'refresh':
      // 刷新数据
      loadData()
      // 刷新系统状态（通过组件引用）
      if (systemStatsRef.value?.loadSystemStatus) {
        systemStatsRef.value.loadSystemStatus()
      }
      message.success('数据刷新中，网速需5-10秒显示准确值')
      break
    case 'add':
      // 打开新增应用弹窗
      showAddApp.value = true
      break
    case 'settings':
      router.push('/settings')
      break
  }
}

// 切换内外网模式
const toggleNetworkMode = () => {
  isInternalMode.value = !isInternalMode.value
  
  // 保存到localStorage
  localStorage.setItem('dockpilot-network-mode', isInternalMode.value ? 'internal' : 'external')
  
  const mode = isInternalMode.value ? '内网' : '外网'
  const description = isInternalMode.value ? '优先使用内网地址' : '优先使用外网地址'
  message.success(`${mode}模式 - ${description}`)
  
  console.log('🔄 网络模式切换:', mode)
}

// 从localStorage读取网络模式偏好
const loadNetworkModePreference = () => {
  const savedMode = localStorage.getItem('dockpilot-network-mode')
  if (savedMode === 'internal') {
    isInternalMode.value = true
  } else {
    isInternalMode.value = false
  }
  
  console.log('📖 加载网络模式偏好:', isInternalMode.value ? '内网' : '外网')
}

// 加载分类和应用数据
const loadData = async () => {
  if (dataLoading.value) return
  
  dataLoading.value = true
  try {
    // 并发加载分类和应用数据
    const [categoriesRes, webServersRes] = await Promise.all([
      getCategories(),
      getWebServers()
    ])
    
    categories.value = categoriesRes
    webServers.value = webServersRes
    
    console.log('数据加载成功:', { 
      categories: categories.value.length, 
      webServers: webServers.value.length 
    })
  } catch (error) {
    console.error('加载数据失败:', error)
    message.error('加载应用数据失败')
  } finally {
    dataLoading.value = false
  }
}

// 保存应用
const saveApp = async (appData: any) => {
  if (!appData.title || !appData.category) {
    message.error('请填写必填字段（标题和分组）')
    return
  }

  try {
    // 准备创建应用的数据
    let iconData = ''
    
    // 根据图标类型确定图标数据
    if (appData.iconType === 'text') {
      iconData = appData.textContent || appData.title.charAt(0).toUpperCase()
    } else if (appData.iconType === 'image' || appData.iconType === 'online') {
      iconData = appData.iconUrl || ''
    }
    
    const createData: CreateWebServerRequest = {
      name: appData.title,
      description: appData.description,
      categoryId: Number(appData.category), // category现在是ID
      icon: iconData,
      externalUrl: appData.url && appData.url.startsWith('http') ? appData.url : '',
      internalUrl: appData.internalUrl || '',
      bgColor: appData.bgColor,
      cardType: appData.cardType,
      iconType: appData.iconType,
      openType: appData.openType,
      itemSort: webServers.value.filter(ws => ws.categoryId === Number(appData.category)).length + 1
    }

    // 调用API创建应用
    const newAppId = await createWebServer(createData)
    
    // 重新加载数据以刷新界面
    await loadData()

    // 关闭弹窗
    showAddApp.value = false
  
    message.success('应用添加成功！')
    console.log('应用添加成功，ID:', newAppId)
  } catch (error) {
    console.error('保存应用失败:', error)
    message.error('保存应用失败')
  }
}

// 处理编辑应用
const handleEditApp = (app: any) => {
  console.log('编辑应用:', app)
  editingApp.value = app
  showEditApp.value = true
}

// 更新应用
const updateApp = async (appData: any) => {
  if (!appData.title || !appData.category || !appData.id) {
    message.error('请填写必填字段')
    return
  }

  try {
    // 准备更新应用的数据
    let iconData = ''
    
    // 根据图标类型确定图标数据
    if (appData.iconType === 'text') {
      iconData = appData.textContent || appData.title.charAt(0).toUpperCase()
    } else if (appData.iconType === 'image' || appData.iconType === 'online') {
      iconData = appData.iconUrl || ''
    }
    
    const updateData: UpdateWebServerRequest = {
      id: appData.id,
      name: appData.title,
      description: appData.description,
      categoryId: Number(appData.category),
      icon: iconData,
      externalUrl: appData.url && appData.url.startsWith('http') ? appData.url : '',
      internalUrl: appData.internalUrl || '',
      bgColor: appData.bgColor,
      cardType: appData.cardType,
      iconType: appData.iconType,
      openType: appData.openType,
      itemSort: 0 // 更新时保持原有排序
    }

    // 调用API更新应用
    await updateWebServer(appData.id, updateData)
    
    // 重新加载数据以刷新界面
    await loadData()

    // 关闭弹窗
    showEditApp.value = false
    editingApp.value = null
  
    message.success('应用更新成功！')
    console.log('应用更新成功，ID:', appData.id)
  } catch (error) {
    console.error('更新应用失败:', error)
    message.error('更新应用失败')
  }
}

// 处理删除应用
const handleDeleteApp = (app: any) => {
  console.log('删除应用:', app)
  
  dialog.warning({
    title: '确认删除',
    content: `确定要删除应用 "${app.name}" 吗？此操作不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => {
      deleteApp(app)
    }
  })
}

// 删除应用
const deleteApp = async (app: any) => {
  try {
    // 调用API删除应用
    await deleteWebServer(app.id)
    
    // 重新加载数据以刷新界面
    await loadData()
  
    message.success(`应用 "${app.name}" 删除成功！`)
    console.log('应用删除成功，ID:', app.id)
  } catch (error) {
    console.error('删除应用失败:', error)
    message.error('删除应用失败')
  }
}

// 处理拖拽排序变化
const handleSortChanged = async (categoryId: number, sortedApps: any[]) => {
  console.log('🔄 处理排序变化:', categoryId, sortedApps.map(app => app.name))
  
  // 简单防抖：清除之前的请求，500ms后执行
  if (sortTimeout) {
    clearTimeout(sortTimeout)
  }
  
  sortTimeout = setTimeout(async () => {
    try {
      // 准备批量更新数据
      const sortUpdateData = sortedApps.map((app, index) => ({
        id: app.id,
        name: app.name,
        description: app.description,
        categoryId: categoryId,
        icon: app.iconUrl,
        externalUrl: app.externalUrl || '',
        internalUrl: app.internalUrl || '',
        bgColor: app.bgColor,
        cardType: app.cardType,
        iconType: app.iconType,
        openType: app.openType,
        itemSort: index + 1
      }))

      console.log('📦 批量更新排序数据:', sortUpdateData)
      
      // 调用批量更新排序API
      await batchUpdateWebServerSort(sortUpdateData)
      
      // 重新加载数据
      await loadData()
      
      message.success('应用排序已更新')
      console.log('✅ 排序更新成功')
    } catch (error) {
      console.error('❌ 更新排序失败:', error)
      message.error('更新排序失败，请稍后重试')
      await loadData()
    }
  }, 500) // 500ms防抖
}

// 处理跨分类移动应用
const handleMoveToCategory = async (app: any, fromCategoryId: number, toCategoryId: number) => {
  console.log(`🚚 移动应用 "${app.name}" 从分类 ${fromCategoryId} 到分类 ${toCategoryId}`)
  
  // 简单防抖：清除之前的请求，300ms后执行
  if (moveTimeout) {
    clearTimeout(moveTimeout)
  }
  
  moveTimeout = setTimeout(async () => {
    try {
      // 准备更新应用数据
      const updateData: UpdateWebServerRequest = {
        id: app.id,
        name: app.name,
        description: app.description,
        categoryId: toCategoryId,
        icon: app.iconUrl,
        externalUrl: app.externalUrl || '',
        internalUrl: app.internalUrl || '',
        bgColor: app.bgColor,
        cardType: app.cardType,
        iconType: app.iconType,
        openType: app.openType,
        itemSort: 999
      }

      console.log('📦 更新应用分类数据:', updateData)
      
      // 调用更新API
      await updateWebServer(app.id, updateData)
      
      // 重新加载数据
      await loadData()
      
      // 获取分类名称用于提示
      const toCategory = categories.value.find(c => c.id === toCategoryId)
      
      message.success(`应用 "${app.name}" 已移动到 "${toCategory?.name}" 分类`)
      console.log('✅ 应用分类移动成功')
    } catch (error) {
      console.error('❌ 移动应用分类失败:', error)
      const errorMessage = error instanceof Error ? error.message : String(error)
      if (errorMessage.includes('database is locked') || errorMessage.includes('SQLITE_BUSY')) {
        message.error('操作过于频繁，请稍后重试')
      } else {
        message.error('移动应用失败')
      }
      await loadData()
    }
  }, 300) // 300ms防抖
}

// 处理图片加载错误
const handleImageError = (app: any) => {
  console.log(`图片加载失败: ${app.name} - ${app.iconUrl}`)
  imageErrors.value[app.id] = true
}

// 处理图片加载成功
const handleImageLoad = (app: any) => {
  if (imageErrors.value[app.id]) {
    console.log(`图片重新加载成功: ${app.name}`)
    imageErrors.value[app.id] = false
  }
}

// 预加载图片
const preloadImage = (url: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve()
    img.onerror = reject
    img.src = url
  })
}

// 设置导航页面背景
const setNavigationBackground = async () => {
  if (!navigationBackgroundRef.value) return
  
  try {
    const backgroundUrl = await getCurrentBackground()
    const finalUrl = backgroundUrl || defaultBackgroundImg
    
    // 预加载图片以避免闪烁
    await preloadImage(finalUrl)
    
    // 设置背景
    navigationBackgroundRef.value.style.backgroundImage = `url(${finalUrl})`
    navigationBackgroundRef.value.style.opacity = '1'
    
    console.log('✅ 导航页面背景已加载:', finalUrl)
  } catch (error) {
    // 降级到默认背景
    await preloadImage(defaultBackgroundImg)
    navigationBackgroundRef.value.style.backgroundImage = `url(${defaultBackgroundImg})`
    navigationBackgroundRef.value.style.opacity = '1'
    console.log('⚠️ 获取背景失败，使用默认背景:', error)
  }
}

let timeInterval: NodeJS.Timeout

onMounted(async () => {
  updateTime()
  // 改为每分钟更新一次时间，减少性能消耗
  timeInterval = setInterval(updateTime, 60000)
  
  // 加载网络模式偏好
  loadNetworkModePreference()
  
  try {
    // 并行加载背景和数据，提高加载速度
    await Promise.all([
      setNavigationBackground(),
      loadData()
    ])
    
    // 等待一个小的延迟确保渲染完成
    setTimeout(() => {
      pageLoading.value = false
    }, 100)
  } catch (error) {
    console.error('页面初始化失败:', error)
    pageLoading.value = false
  }
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
  
  // 清理防抖定时器
  if (sortTimeout) {
    clearTimeout(sortTimeout)
  }
  if (moveTimeout) {
    clearTimeout(moveTimeout)
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

/* 固定背景元素 */
.navigation-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  z-index: -999;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s ease-in-out;
  will-change: opacity;
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
  transform: translateZ(0); /* 启用GPU加速 */
  backface-visibility: hidden; /* 避免反面渲染 */
}

/* 顶部导航 */
.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 12px 20px;
  margin-bottom: 20px;
  width: 100%;
  max-width: 1200px;
  transition: background 0.2s ease;
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

/* 加载状态 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.8);
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #ffffff;
}

.loading-content p {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

/* 网络模式切换按钮 */
.network-mode-toggle {
  display: flex;
  align-items: center;
  margin-left: 8px;
}

.mode-toggle-btn {
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: rgba(255, 255, 255, 0.8) !important;
  transition: all 0.2s ease !important;
}

.mode-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.4) !important;
  color: #ffffff !important;
  transform: scale(1.05);
}





/* 组件相关样式已移至独立组件文件 */

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
    width: 36px;
    height: 36px;
  }
  
  .app-icon img {
    width: 28px;
    height: 28px;
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

  /* 移动端网络模式按钮 */
  .network-mode-toggle {
    margin-left: 4px;
  }
  
  .mode-toggle-btn {
    width: 28px !important;
    height: 28px !important;
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
    width: 32px;
    height: 32px;
  }
  
  .app-icon img {
    width: 24px;
    height: 24px;
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
    width: 28px;
    height: 28px;
  }
  
  .app-icon img {
    width: 20px;
    height: 20px;
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
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.preview-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-style: italic;
}

.preview-area {
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 120px;
  position: relative;
  overflow: hidden;
}

.preview-opacity-slider {
  position: absolute;
  top: 12px;
  right: 16px;
  z-index: 10;
  opacity: 0.9;
  transition: opacity 0.2s ease;
}

.preview-opacity-slider:hover {
  opacity: 1;
}

.preview-area.real-background {
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.preview-area.real-background::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  pointer-events: none;
  will-change: auto;
}

.preview-card {
  /* 移除默认背景色，让用户选择的颜色生效 */
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

/* 透明预览卡片样式 */
.preview-card.preview-transparent {
  background: transparent !important;
  backdrop-filter: none !important;
  border: none !important;
  box-shadow: none !important;
}

.preview-card.preview-transparent .preview-title,
.preview-card.preview-transparent .preview-desc {
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8);
}

.preview-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  font-size: 20px;
  font-weight: 700;
}

/* 透明卡片中的预览图标 */
.preview-card.preview-transparent .preview-icon {
  background: transparent;
}

.preview-icon img {
  width: 36px;
  height: 36px;
  border-radius: 8px;
}

/* 预览区域的降级图标样式 */
.preview-fallback-icon,
.preview-text-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

/* 预览区域的占位符图标样式 */
.preview-placeholder-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  border: 2px dashed rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
}

.preview-content {
  flex: 1;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 2px;
  line-height: 1.3;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.preview-desc {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.3;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}



/* 预览文字卡片样式 */
.preview-card.preview-text {
  min-width: 180px;
  padding: 12px 16px;
  border-radius: 8px;
}

.preview-card.preview-text .preview-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 2px;
}

.preview-card.preview-text .preview-desc {
  font-size: 11px;
  opacity: 0.8;
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

.color-picker-section {
  width: 100%;
}

.color-picker-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-value {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-family: 'SF Mono', 'Monaco', 'Cascadia Code', monospace;
  background: rgba(255, 255, 255, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
  min-width: 100px;
  width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.opacity-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.opacity-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  font-weight: 500;
}

.opacity-presets {
  display: flex;
  gap: 3px;
  flex-wrap: wrap;
}

.opacity-btn {
  padding: 3px 6px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
  border-radius: 3px;
  font-size: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 30px;
  text-align: center;
}

.opacity-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.4);
  color: #ffffff;
}

.opacity-btn.active {
  background: rgba(59, 130, 246, 0.3);
  border-color: rgba(59, 130, 246, 0.5);
  color: #ffffff;
  font-weight: 600;
}

.preset-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preset-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  font-weight: 500;
}

.color-presets {
  display: flex;
  gap: 6px;
}

.preset-color {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.2);
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.preset-color:hover {
  border-color: rgba(255, 255, 255, 0.4);
  transform: scale(1.1);
}

.transparent-preset {
  background: repeating-conic-gradient(#666 0% 25%, transparent 0% 50%) 50% / 8px 8px;
}

/* 统一的图标输入容器样式 */
.icon-input-container {
  display: flex;
  gap: 8px;
  width: 100%;
}

.icon-input-container .n-input {
  flex: 1;
}

.icon-input-container .n-button {
  flex-shrink: 0;
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