<template>
  <!-- 应用网格 - 分类容器布局 -->
  <div class="apps-container">
    <div 
      v-for="category in filteredCategories" 
      :key="category.id"
      class="category-container"
    >
      <!-- 分类标题 -->
      <div class="category-header">
        <span class="category-name">{{ category.name }}</span>
      </div>
      
      <!-- 该分类下的应用 - 使用拖拽容器 -->
      <draggable
        v-model="category.apps"
        item-key="id"
        class="category-apps-grid"
        ghost-class="app-card-ghost"
        chosen-class="app-card-chosen"
        drag-class="app-card-drag"
        :animation="200"
        :delay="100"
        :delay-on-touch-start="true"
        :touch-start-threshold="3"
        group="apps"
        @start="handleDragStart($event, category)"
        @end="handleDragEnd($event, category)"
        @change="handleDragChange($event, category)"
      >
        <template #item="{ element: app }">
          <div
            class="app-card"
            :class="{ 
              'card-text': app.cardType === 'text',
              'card-transparent': app.bgColor === 'transparent' || app.bgColor === 'rgba(0, 0, 0, 0)'
            }"
            :style="{ backgroundColor: app.bgColor || 'rgba(255, 255, 255, 0.15)' }"
            @click="handleAppClick(app)"
            @contextmenu.prevent="handleContextMenu($event, app)"
          >
            <div class="app-icon">
              <!-- 文字图标 -->
              <span v-if="app.iconType === 'text'" class="text-icon">
                {{ app.iconUrl || app.name.charAt(0).toUpperCase() }}
              </span>
              <!-- 图片图标或在线图标 -->
              <img 
                v-else-if="(app.iconType === 'image' || app.iconType === 'online') && app.iconUrl && !app.imageError" 
                :src="app.iconUrl" 
                :alt="app.name"
                @error="handleImageError(app)"
                @load="handleImageLoad(app)"
              >
              <div 
                v-else-if="(app.iconType === 'image' || app.iconType === 'online') && app.iconUrl && app.imageError" 
                class="fallback-icon"
                :title="`${app.name} - 图标加载失败，显示文字图标`"
              >
                {{ app.name.charAt(0).toUpperCase() }}
              </div>
              <!-- 默认图标（兼容旧数据） -->
              <n-icon 
                v-else
                :size="24" 
                :component="(app as any).icon || CubeOutline" 
              />
            </div>
            <div class="app-content">
              <div class="app-name">{{ app.name }}</div>
              <div class="app-desc">{{ app.description }}</div>
            </div>
            <div v-if="(app as any).status" class="app-status">
              <div 
                class="status-dot" 
                :class="(app as any).status === 'running' ? 'running' : 'stopped'"
              ></div>
            </div>
            <!-- 收藏按钮 -->
            <div class="favorite-button" @click.stop="handleFavoriteClick(app)">
              <n-icon 
                :size="18" 
                :component="app.isFavorite ? Heart : HeartOutline"
                :class="{ 'favorited': app.isFavorite }"
              />
            </div>
            <!-- 拖拽提示图标 -->
            <div class="drag-handle">
              <n-icon :size="16" :component="ReorderThreeOutline" />
            </div>
          </div>
        </template>
      </draggable>

      <!-- 空分类提示 -->
      <div 
        v-if="category.apps.length === 0"
        class="empty-category-drop"
      >
        <p>暂无应用，拖拽应用到此处</p>
      </div>
    </div>

    <!-- 右键菜单 -->
    <n-dropdown
      placement="bottom-start"
      trigger="manual"
      :x="contextMenuPosition.x"
      :y="contextMenuPosition.y"
      :options="contextMenuOptions"
      :show="showContextMenu"
      @clickoutside="handleClickOutside"
      @select="handleContextMenuSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, markRaw, nextTick, h } from 'vue'
import { CubeOutline, CreateOutline, TrashOutline, ReorderThreeOutline, HeartOutline, Heart } from '@vicons/ionicons5'
import { NIcon } from 'naive-ui'
import draggable from 'vuedraggable'

// Props
interface App {
  id: string
  name: string
  description: string
  iconUrl: string
  iconType?: string
  externalUrl?: string
  internalUrl?: string
  openType?: string
  bgColor?: string
  cardType?: string
  icon?: any
  imageError?: boolean
  categoryId?: number
  isFavorite?: boolean
}

interface Category {
  id: number
  name: string
  sortOrder: number
  appCount: number
  color?: string
  icon?: any
  expanded: boolean
  apps: App[]
}

interface Props {
  categories?: Category[]
  searchQuery?: string
}

const props = withDefaults(defineProps<Props>(), {
  categories: () => [],
  searchQuery: ''
})

// Emits
const emit = defineEmits<{
  'app-click': [app: App]
  'image-error': [app: App]
  'image-load': [app: App]
  'edit-app': [app: App]
  'delete-app': [app: App]
  'sort-changed': [categoryId: number, apps: App[]]
  'move-to-category': [app: App, fromCategoryId: number, toCategoryId: number]
  'toggle-favorite': [app: App]
}>()

// 图片加载错误状态
const imageErrors = ref<Record<string, boolean>>({})

// 右键菜单相关状态
const showContextMenu = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const currentApp = ref<App | null>(null)

// 拖拽状态
const dragStartInfo = ref<{ app: App, categoryId: number } | null>(null)

// 右键菜单选项
const contextMenuOptions = computed(() => [
  {
    label: '编辑应用',
    key: 'edit',
    icon: () => h(NIcon, null, { default: () => h(CreateOutline) })
  },
  {
    type: 'divider',
    key: 'divider'
  },
  {
    label: '删除应用',
    key: 'delete',
    icon: () => h(NIcon, null, { default: () => h(TrashOutline) })
  }
])

// 过滤后的分类
const filteredCategories = computed(() => {
  if (!props.searchQuery) return props.categories
  
  return props.categories
    .map(category => ({
      ...category,
      apps: category.apps.filter(app =>
        app.name.toLowerCase().includes(props.searchQuery.toLowerCase()) ||
        app.description.toLowerCase().includes(props.searchQuery.toLowerCase())
      )
    }))
    .filter(category => category.apps.length > 0)
})

// 处理拖拽开始
const handleDragStart = (event: any, category: Category) => {
  console.log('🔄 开始拖拽:', event.item.innerText, '从分类:', category.name)
  
  // 记录拖拽开始的信息
  const appElement = event.item
  const appId = appElement.getAttribute('data-app-id') || category.apps[event.oldIndex]?.id
  const app = category.apps.find(a => a.id === appId) || category.apps[event.oldIndex]
  
  if (app) {
    dragStartInfo.value = {
      app: app,
      categoryId: category.id
    }
    console.log('📝 记录拖拽信息:', dragStartInfo.value)
  }
}

// 处理拖拽变化（跨分类移动时触发）
const handleDragChange = (event: any, category: Category) => {
  console.log('🔄 拖拽变化事件:', event, '目标分类:', category.name)
  
  if (event.added) {
    // 有应用被添加到这个分类
    const addedApp = event.added.element
    console.log('➕ 应用被添加到分类:', addedApp.name, '→', category.name)
    
    // 检查是否是跨分类移动
    if (dragStartInfo.value && dragStartInfo.value.categoryId !== category.id) {
      console.log('🚚 检测到跨分类移动!')
      
      // 更新应用的分类ID
      addedApp.categoryId = category.id
      
      // 发出跨分类移动事件
      emit('move-to-category', addedApp, dragStartInfo.value.categoryId, category.id)
      return // 跨分类移动不需要发送排序事件
    }
  }
}

// 处理拖拽结束事件
const handleDragEnd = (event: any, category: Category) => {
  console.log('🔄 拖拽结束:', category.name, '事件:', event)
  
  // 如果不是跨分类移动，则是同分类排序
  if (dragStartInfo.value && dragStartInfo.value.categoryId === category.id) {
    console.log('📊 同分类排序:', category.name)
    emit('sort-changed', category.id, category.apps)
  }
  
  // 清除拖拽信息
  dragStartInfo.value = null
}

// 处理应用点击
const handleAppClick = (app: App) => {
  // 如果右键菜单正在显示，则不触发点击事件
  if (showContextMenu.value) {
    return
  }
  emit('app-click', app)
}

// 处理右键菜单
const handleContextMenu = (event: MouseEvent, app: App) => {
  console.log('右键菜单触发:', app.name)
  
  // 阻止默认行为
  event.preventDefault()
  event.stopPropagation()
  
  // 先关闭之前的菜单
  showContextMenu.value = false
  
  // 设置当前应用和菜单位置
  currentApp.value = app
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  
  // 使用 nextTick 确保菜单重新打开
  nextTick(() => {
    showContextMenu.value = true
  })
}

// 处理右键菜单选择
const handleContextMenuSelect = (key: string) => {
  console.log('菜单选择:', key, currentApp.value?.name)
  
  if (!currentApp.value) return
  
  switch (key) {
    case 'edit':
      emit('edit-app', currentApp.value)
      break
    case 'delete':
      emit('delete-app', currentApp.value)
      break
  }
  
  // 关闭菜单
  showContextMenu.value = false
  currentApp.value = null
}

// 处理点击外部关闭菜单
const handleClickOutside = () => {
  console.log('点击外部，关闭菜单')
  showContextMenu.value = false
  currentApp.value = null
}

// 处理图片加载错误
const handleImageError = (app: App) => {
  console.log(`图片加载失败: ${app.name} - ${app.iconUrl}`)
  imageErrors.value[app.id] = true
  emit('image-error', app)
}

// 处理图片加载成功
const handleImageLoad = (app: App) => {
  if (imageErrors.value[app.id]) {
    console.log(`图片重新加载成功: ${app.name}`)
    imageErrors.value[app.id] = false
  }
  emit('image-load', app)
}

// 处理收藏按钮点击
const handleFavoriteClick = (app: App) => {
  console.log('收藏按钮点击:', app.name, '当前状态:', app.isFavorite)
  emit('toggle-favorite', app)
}
</script>

<style scoped>
/* 应用网格 - 分类容器布局 */
.apps-container {
  margin-bottom: 24px;
  width: 100%;
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 分类容器样式 */
.category-container {
  padding: 16px 0;
}

/* 分类标题样式 */
.category-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.category-name {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.drag-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-style: italic;
  transition: all 0.2s ease;
}

/* 空分类提示区域 */
.empty-category-drop {
  min-height: 80px;
  border: 2px dashed rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.02);
}

.empty-category-drop p {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  font-style: italic;
}

/* 拖拽时空分类区域的视觉反馈 */
.empty-category-drop:hover {
  border-color: rgba(59, 130, 246, 0.4);
  background: rgba(59, 130, 246, 0.05);
}

/* 分类下的应用网格 */
.category-apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.app-card {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(4px);
  min-height: 50px;
  position: relative;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.03);
}

/* 完全透明的卡片样式 */
.app-card.card-transparent {
  background: transparent !important;
  backdrop-filter: none !important;
  box-shadow: none !important;
  border: none !important;
}

.app-card:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.app-card:hover .drag-handle,
.app-card:hover .favorite-button {
  opacity: 1;
}

/* 文字卡片类型 */
.app-card.card-text {
  padding: 12px 16px;
  min-height: 40px;
  border-radius: 8px;
  backdrop-filter: blur(10px);
}

.app-card.card-text .app-icon {
  display: none;
}

.app-card.card-text .app-content {
  gap: 0;
}

.app-card.card-text .app-name {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 2px;
}

.app-card.card-text .app-desc {
  font-size: 11px;
  opacity: 0.8;
}

.app-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  flex-shrink: 0;
}

.app-icon img {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: contain;
}

.app-icon span.text-icon,
.app-icon .fallback-icon {
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
  text-transform: uppercase;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

/* 图片加载失败的降级图标样式 */
.fallback-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: all 0.2s ease;
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
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.app-desc {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
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

/* 拖拽手柄样式 */
.drag-handle {
  position: absolute;
  top: 8px;
  right: 36px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  cursor: grab;
  opacity: 0;
  transition: all 0.2s ease;
  color: rgba(255, 255, 255, 0.8);
}

.drag-handle:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.drag-handle:active {
  cursor: grabbing;
}

/* 拖拽状态样式 */
.app-card-ghost {
  opacity: 0.4;
  background: rgba(59, 130, 246, 0.2) !important;
  border: 2px dashed rgba(59, 130, 246, 0.5) !important;
}

.app-card-chosen {
  transform: rotate(3deg) scale(1.02);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3) !important;
  border-color: rgba(59, 130, 246, 0.8) !important;
  z-index: 999;
}

.app-card-drag {
  opacity: 0.8;
  transform: rotate(5deg);
}

/* 收藏按钮样式 */
.favorite-button {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s ease;
  color: rgba(255, 255, 255, 0.8);
}

.favorite-button:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.favorite-button:active {
  cursor: grabbing;
}

/* 收藏按钮选中样式 */
.favorited {
  color: #ef4444;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .category-apps-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 10px;
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
  .category-apps-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
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
  
  /* 移动端拖拽手柄和收藏按钮调整 */
  .drag-handle {
    opacity: 0.6;
    right: 32px; /* 移动端稍微调整间距 */
    width: 20px;
    height: 20px;
  }
  
  .favorite-button {
    opacity: 0.6;
    width: 20px;
    height: 20px;
  }
  
  .drag-hint {
    display: none;
  }
}

@media (max-width: 480px) {
  .category-apps-grid {
    grid-template-columns: 1fr;
    gap: 8px;
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
  
  /* 小屏幕拖拽手柄优化 */
  .drag-handle {
    width: 20px;
    height: 20px;
    opacity: 0.7;
  }
}
</style> 