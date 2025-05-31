<template>
  <div class="side-drawer-container" :class="{ 'expanded': showDrawer }">
    <!-- 鼠标感应区域 -->
    <div 
      class="mouse-area"
      :class="{ 'expanded': showDrawer }"
      @mouseenter="handleContainerEnter"
      @mouseleave="handleContainerLeave"
    >
      <!-- 触发区域 -->
      <div class="drawer-trigger">
        <div class="trigger-indicator">
          <n-icon :component="HeartOutline" class="trigger-icon" />
        </div>
      </div>

      <!-- 抽屉面板 -->
      <transition name="drawer-slide">
        <div 
          v-if="showDrawer"
          class="drawer-panel"
        >
          <div class="drawer-content">
            <!-- 抽屉标题 -->
            <div class="drawer-header">
              <h4>
                <n-icon :component="Heart" style="margin-right: 8px; color: #ef4444;" />
                我的收藏
              </h4>
            </div>

            <!-- 抽屉主要内容 - 收藏列表 -->
            <div class="drawer-body">
              <div v-if="loading" class="loading-state">
                <n-spin size="small" />
                <span>加载中...</span>
              </div>
              
              <div v-else-if="favorites.length === 0" class="empty-state">
                <n-icon :component="HeartOutline" style="font-size: 32px; opacity: 0.4; margin-bottom: 8px;" />
                <p>暂无收藏应用</p>
                <span>点击应用卡片右上角的 ♡ 图标收藏</span>
              </div>
              
              <div v-else class="favorites-list">
                <div 
                  v-for="app in favorites" 
                  :key="app.id"
                  class="favorite-item"
                  @click="handleFavoriteAppClick(app)"
                >
                  <div class="favorite-text">
                    <span class="favorite-name">{{ app.name }}</span>
                    <span class="favorite-separator"> - </span>
                    <span class="favorite-desc">{{ app.description || '暂无描述' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { Heart, HeartOutline } from '@vicons/ionicons5'
import { getFavorites } from '@/api/http/webserver'

// 定义 App 接口
interface App {
  id: string
  name: string
  description?: string
  iconUrl?: string
  iconType?: string
  externalUrl?: string
  internalUrl?: string
  openType?: string
  isFavorite?: boolean
  imageError?: boolean
}

const message = useMessage()

// 抽屉显示状态
const showDrawer = ref(false)
// 收藏列表
const favorites = ref<App[]>([])
// 加载状态
const loading = ref(false)

// 延迟隐藏定时器
let hideTimer: NodeJS.Timeout | null = null

// 处理鼠标进入
const handleContainerEnter = () => {
  clearHideTimer()
  showDrawer.value = true
  // 每次显示时刷新收藏列表
  loadFavorites()
}

// 处理鼠标离开
const handleContainerLeave = () => {
  hideTimer = setTimeout(() => {
    showDrawer.value = false
  }, 150)
}

// 加载收藏列表
const loadFavorites = async () => {
  try {
    loading.value = true
    const data = await getFavorites()
    // 映射后端数据到前端接口
    favorites.value = (data || []).map(item => ({
      id: item.id,
      name: item.name,
      description: item.description || '暂无描述',
      iconUrl: item.icon,
      iconType: item.iconType,
      externalUrl: item.externalUrl,
      internalUrl: item.internalUrl,
      openType: item.openType,
      isFavorite: true,
      imageError: false
    }))
  } catch (error) {
    console.error('加载收藏列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理收藏应用点击
const handleFavoriteAppClick = (app: App) => {
  showDrawer.value = false
  
  // 获取网络模式
  const isInternalMode = localStorage.getItem('networkMode') === 'internal'
  
  // 确定要打开的URL
  let targetUrl = ''
  if (isInternalMode && app.internalUrl) {
    targetUrl = app.internalUrl
  } else if (app.externalUrl) {
    targetUrl = app.externalUrl
  } else if (app.internalUrl) {
    targetUrl = app.internalUrl
  }
  
  if (!targetUrl) {
    message.warning('该应用暂无可用访问地址')
    return
  }
  
  // 确保URL包含协议
  if (!targetUrl.startsWith('http://') && !targetUrl.startsWith('https://')) {
    targetUrl = 'http://' + targetUrl
  }
  
  // 根据打开方式决定如何打开
  if (app.openType === 'current') {
    window.location.href = targetUrl
  } else {
    window.open(targetUrl, '_blank')
  }
  
  console.log('打开收藏应用:', app.name, '地址:', targetUrl, '模式:', isInternalMode ? '内网' : '外网')
}

// 处理图片加载错误
const handleImageError = (app: App) => {
  app.imageError = true
}

// 清理定时器
const clearHideTimer = () => {
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
}

onMounted(() => {
  // 初始加载收藏列表
  loadFavorites()
})
</script>

<style scoped>
.side-drawer-container {
  position: fixed;
  left: 0;
  top: 0;
  width: 25px; /* 默认与感应区域同宽 */
  height: 100vh;
  z-index: 100;
  pointer-events: none; /* 容器默认不响应事件 */
  transition: width 0.1s ease;
}

.side-drawer-container.expanded {
  width: 240px; /* 从285px缩小到240px */
}

/* 鼠标感应区域 */
.mouse-area {
  position: absolute;
  left: 0;
  top: 0;
  width: 25px; /* 稍微增大感应区域 */
  height: 100vh;
  pointer-events: all; /* 允许鼠标事件 */
  transition: width 0.1s ease;
}

/* 当抽屉展开时，扩大感应区域 */
.mouse-area.expanded {
  width: 240px; /* 从285px缩小到240px */
}

/* 触发区域 */
.drawer-trigger {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 100px;
  background: rgba(255, 255, 255, 0.08); /* 调整为更透明的背景 */
  border-radius: 0 10px 10px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  pointer-events: all; /* 恢复事件响应用于视觉反馈 */
  backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-left: none;
  z-index: 101;
}

.drawer-trigger:hover {
  width: 25px;
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
}

.trigger-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
}

.trigger-icon {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  transition: all 0.3s ease;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

.drawer-trigger:hover .trigger-icon {
  color: #ffffff;
  transform: translateX(2px);
}

/* 抽屉面板 */
.drawer-panel {
  position: absolute;
  left: 0;
  top: 0;
  width: 220px; /* 从280px缩小到220px */
  height: 100vh;
  background: linear-gradient(135deg, 
    rgba(0, 0, 0, 0.35) 0%, 
    rgba(0, 0, 0, 0.25) 50%, 
    rgba(0, 0, 0, 0.3) 100%
  ); /* 微妙的渐变透明背景 */
  backdrop-filter: blur(20px);
  border-right: 1px solid rgba(255, 255, 255, 0.15);
  pointer-events: all; /* 抽屉面板内容需要响应事件 */
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.2);
}

.drawer-content {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* 抽屉头部 */
.drawer-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}

.drawer-header h4 {
  margin: 0;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8); /* 增加文字阴影提升可读性 */
}

/* 抽屉主体 */
.drawer-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 20px;
  color: rgba(255, 255, 255, 0.6);
}

.empty-state p {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 500;
}

.empty-state span {
  font-size: 12px;
  opacity: 0.7;
  line-height: 1.4;
}

/* 收藏列表 */
.favorites-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.favorite-item {
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid transparent;
}

.favorite-item:hover {
  color: #3b82f6;
  transform: translateX(4px);
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(59, 130, 246, 0.05);
}

.favorite-text {
  display: inline;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

.favorite-name {
  font-weight: 600;
  color: #ffffff;
}

.favorite-separator {
  color: rgba(255, 255, 255, 0.5);
}

.favorite-desc {
  color: rgba(255, 255, 255, 0.7);
  font-style: italic;
}

/* 章节标题 */
.section-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

/* 导航项目 */
.nav-items, .tool-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item, .tool-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08); /* 稍微增加透明度 */
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.nav-item:hover, .tool-item:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  border-color: rgba(59, 130, 246, 0.4);
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.nav-item span, .tool-item span {
  font-size: 13px;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

/* 状态项目 */
.status-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.status-item .n-icon {
  margin-top: 2px;
  font-size: 14px;
}

/* CPU 状态项图标颜色 */
.status-item:nth-child(1) .n-icon {
  color: #22d3ee;
}

/* 内存状态项图标颜色 */
.status-item:nth-child(2) .n-icon {
  color: #a78bfa;
}

/* 磁盘状态项图标颜色 */
.status-item:nth-child(3) .n-icon {
  color: #fbbf24;
}

.status-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.status-content span {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

.progress-bar {
  width: 100%;
  height: 3px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-fill.cpu {
  background: linear-gradient(90deg, #22d3ee, #06b6d4);
}

.progress-fill.memory {
  background: linear-gradient(90deg, #a78bfa, #8b5cf6);
}

.progress-fill.disk {
  background: linear-gradient(90deg, #fbbf24, #f59e0b);
}

/* 抽屉动画 */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.drawer-slide-enter-from {
  transform: translateX(-100%);
}

.drawer-slide-leave-to {
  transform: translateX(-100%);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .drawer-panel {
    width: 200px; /* 从250px缩小到200px */
  }
  
  .drawer-content {
    padding: 16px;
  }
  
  .drawer-trigger {
    width: 16px;
    height: 80px;
  }
  
  .drawer-trigger:hover {
    width: 20px;
  }
}

@media (max-width: 480px) {
  .drawer-panel {
    width: 180px; /* 从220px缩小到180px */
  }
  
  .nav-item, .tool-item {
    padding: 10px 12px;
  }
  
  .nav-item span, .tool-item span {
    font-size: 12px;
  }
}

/* 滚动条样式 */
.drawer-content::-webkit-scrollbar {
  width: 4px;
}

.drawer-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.drawer-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

.drawer-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}
</style> 