<template>
  <div class="home-container" @contextmenu.prevent="handlePageContextMenu">
    <!-- 背景图片和遮罩 -->
    <div
      class="cover"
      :style="{
        filter: `blur(${backgroundBlur}px)`,
        background: `url(${backgroundImage}) no-repeat`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }"
    />
    <div class="mask" :style="{ backgroundColor: `rgba(0,0,0,${backgroundMask})` }" />

    <main class="main-content new-layout">
      <transition name="fade-cross" mode="out-in">
        <div v-if="currentTab === 'dashboard'" key="dashboard" class="dashboard-container">
          <!-- 仪表盘视图 -->
          <DashboardView :dashboardVisible="dashboardVisible" />
        </div>
        <div v-else key="applist" class="applist-container">
          <!-- 应用面板内容 -->
          <div class="app-content-wrapper">
            <!-- 顶部大标题和时间 -->
            <HeaderClock />
            <!-- 搜索框 -->
            <SearchBar />
            <!-- APP 列表 -->
            <AppList 
              :appList="appList"
              :isInternalNetwork="isInternalNetwork"
              :animateIn="appListVisible"
              @openApp="handleAppClick"
              @addApp="handleAddApp"
              @contextMenu="handleContextMenu"
            />
          </div>
        </div>
      </transition>
    </main>
    
    <!-- 悬浮按钮组 -->
    <FloatingButtons
      :currentTab="currentTab"
      :isInternalNetwork="isInternalNetwork"
      @addApp="handleAddApp"
      @openSettings="openSettings"
      @toggleView="toggleView"
      @toggleNetworkMode="toggleNetworkMode"
    />

    <!-- 内嵌窗口 -->
    <EmbeddedWindow
      v-model:visible="windowVisible"
      :title="windowTitle"
      :url="windowUrl"
    />

    <!-- 新增/编辑应用对话框 -->
    <AppEditDialog
      v-model:visible="addAppVisible"
      :isEdit="isEdit"
      :appData="newApp"
      @submit="onSubmitApp"
      @delete="handleDeleteApp"
      @cancel="addAppVisible = false"
    />

    <!-- 右键菜单 -->
    <ContextMenu
      :visible="contextMenuVisible"
      :menuStyle="contextMenuStyle"
      :contextItem="currentContextItem"
      @open-app="handleOpenAppFromMenu"
      @menu-click="handleContextMenuClick"
    />

    <!-- 确认对话框 -->
    <ConfirmDialog
      v-model:visible="confirmVisible"
      :title="confirmConfig.title"
      :content="confirmConfig.content"
      @confirm="handleConfirmConfirm"
      @cancel="handleConfirmCancel"
    />

    <!-- 后台进入过渡动画 -->
    <transition name="fade-zoom">
      <div v-if="enteringBackend" class="backend-transition">
        <div class="transition-content">
          <t-loading theme="dots" size="large" />
          <div class="transition-text">正在进入管理后台...</div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { MessagePlugin, FormRules, SubmitContext, UploadFile, DialogPlugin } from 'tdesign-vue-next';
import { useRouter } from 'vue-router';

// 导入背景图片
import backgroundImage from '/src/assets/background.png';

// 导入拆分的组件
import HeaderClock from './components/HeaderClock.vue';
import SearchBar from './components/SearchBar.vue';
import AppList from './components/AppList.vue';
import DashboardView from './components/DashboardView.vue';
import FloatingButtons from './components/FloatingButtons.vue';
import ContextMenu from './components/ContextMenu.vue';
import AppEditDialog from './components/AppEditDialog.vue';
import ConfirmDialog from './components/ConfirmDialog.vue';
import EmbeddedWindow from './components/EmbeddedWindow.vue';

// 定义应用项接口
interface AppItem {
  name: string;
  icon: string;
  internalUrl: string;
  externalUrl: string;
  desc: string;
}

const router = useRouter();

// 背景配置
const backgroundBlur = ref(0.5);
const backgroundMask = ref(0.3);

// 仪表盘动画控制
const dashboardVisible = ref(false);
// 应用列表动画控制
const appListVisible = ref(true);

// 内嵌窗口
const windowVisible = ref(false);
const windowTitle = ref('');
const windowUrl = ref('');

// 后台过渡动画控制
const enteringBackend = ref(false);

// 应用列表
const appList = ref<AppItem[]>([
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/11emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能Web服务器'
  },
  {
    name: 'MySQL',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '流行的关系型数据库'
  },
  {
    name: 'Redis',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能缓存数据库'
  },
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能Web服务器'
  },
  {
    name: 'MySQL',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '流行的关系型数据库'
  },
  {
    name: 'Redis',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能缓存数据库'
  }
]);

// 应用点击处理
function handleAppClick(item: AppItem) {
  const url = isInternalNetwork.value ? item.internalUrl : item.externalUrl;
  if (!url) {
    MessagePlugin.error(`${isInternalNetwork.value ? '内网' : '外网'}地址未设置`);
    return;
  }
  windowTitle.value = item.name;
  windowUrl.value = url;
  windowVisible.value = true;
}

// 新增应用相关
const addAppVisible = ref(false);
const newApp = ref<Partial<AppItem>>({
  name: '',
  icon: '',
  internalUrl: '',
  externalUrl: '',
  desc: ''
});

// 编辑状态
const isEdit = ref(false);

function handleEditApp(item: AppItem) {
  isEdit.value = true;
  newApp.value = { ...item };
  addAppVisible.value = true;
}

function handleAddApp() {
  isEdit.value = false;
  newApp.value = {
    name: '',
    icon: '',
    internalUrl: '',
    externalUrl: '',
    desc: ''
  };
  addAppVisible.value = true;
}

// 确认对话框相关
const confirmVisible = ref(false);
const confirmConfig = ref({
  title: '',
  content: '',
  onConfirm: () => {}
});

function handleDeleteApp(item: AppItem) {
  confirmConfig.value = {
    title: '确认删除',
    content: `确定要删除应用 "${item.name}" 吗？`,
    onConfirm: () => {
      // TODO: 实现删除逻辑
      MessagePlugin.info('删除功能开发中...');
      addAppVisible.value = false;
      confirmVisible.value = false;
    }
  };
  confirmVisible.value = true;
}

function handleConfirmCancel() {
  confirmVisible.value = false;
}

function handleConfirmConfirm() {
  confirmConfig.value.onConfirm();
}

function onSubmitApp(formData: AppItem) {
  MessagePlugin.info('保存功能开发中...');
  addAppVisible.value = false;
}

// 其他功能
function openSettings() {
  // 显示过渡动画
  enteringBackend.value = true;
  
  // 添加延迟，让过渡动画有足够的显示时间
  setTimeout(() => {
    router.push('/docker/containers');
  }, 800);
}

// 网络模式
const isInternalNetwork = ref(true);

function toggleNetworkMode() {
  isInternalNetwork.value = !isInternalNetwork.value;
  MessagePlugin.info(`已切换到${isInternalNetwork.value ? '内网' : '外网'}模式`);
}

// 右键菜单相关
const contextMenuVisible = ref(false);
const contextMenuStyle = ref({
  position: 'fixed' as const,
  left: '0px',
  top: '0px',
  zIndex: 1000
});
const currentContextItem = ref<AppItem | null>(null);

function handleContextMenu(payload: { event: MouseEvent, item: AppItem }) {
  const { event, item } = payload;
  event.preventDefault();
  currentContextItem.value = item;
  
  // 获取视口尺寸
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  
  // 计算菜单位置,确保不会超出视口
  const menuWidth = 120; // 预估菜单宽度
  const menuHeight = 160; // 预估菜单高度
  
  let left = event.clientX;
  let top = event.clientY;
  
  // 如果菜单会超出右边界,则向左偏移
  if (left + menuWidth > viewportWidth) {
    left = viewportWidth - menuWidth;
  }
  
  // 如果菜单会超出下边界,则向上偏移
  if (top + menuHeight > viewportHeight) {
    top = viewportHeight - menuHeight;
  }
  
  contextMenuStyle.value = {
    position: 'fixed' as const,
    left: `${left}px`,
    top: `${top}px`,
    zIndex: 1000
  };
  contextMenuVisible.value = true;
}

function handlePageContextMenu(event: MouseEvent) {
  // 如果点击的是应用卡片，不处理
  if ((event.target as HTMLElement).closest('.app-item')) {
    return;
  }
  
  currentContextItem.value = null;
  
  // 获取视口尺寸
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  
  // 计算菜单位置
  const menuWidth = 120;
  const menuHeight = 40;
  
  let left = event.clientX;
  let top = event.clientY;
  
  if (left + menuWidth > viewportWidth) {
    left = viewportWidth - menuWidth;
  }
  
  if (top + menuHeight > viewportHeight) {
    top = viewportHeight - menuHeight;
  }
  
  contextMenuStyle.value = {
    position: 'fixed' as const,
    left: `${left}px`,
    top: `${top}px`,
    zIndex: 1000
  };
  contextMenuVisible.value = true;
}

function handleOpenAppFromMenu(payload: { type: 'internal' | 'external', item: AppItem }) {
  if (!payload.item) return;
  
  const url = payload.type === 'internal' 
    ? payload.item.internalUrl 
    : payload.item.externalUrl;
    
  if (!url) {
    MessagePlugin.error(`${payload.type === 'internal' ? '内网' : '外网'}地址未设置`);
    return;
  }
  
  windowTitle.value = payload.item.name;
  windowUrl.value = url;
  windowVisible.value = true;
  contextMenuVisible.value = false;
}

function handleRefresh() {
  window.location.reload();
}

function handleContextMenuClick(payload: { action: string, item: AppItem | null }) {
  const { action, item } = payload;
  
  switch (action) {
    case 'edit':
      if (item) handleEditApp(item);
      break;
    case 'refresh':
      handleRefresh();
      break;
  }
  
  contextMenuVisible.value = false;
}

const currentTab = ref('applist');

// 视图切换
function toggleView() {
  if (currentTab.value === 'dashboard') {
    // 从仪表盘切换到应用列表
    dashboardVisible.value = false;
    
    // 直接切换视图，由transition组件处理动画效果
    currentTab.value = 'applist';
    
    // 延迟显示应用列表，让其有独立的入场动画
    setTimeout(() => {
      appListVisible.value = true;
    }, 300);
  } else {
    // 从应用列表切换到仪表盘
    appListVisible.value = false;
    
    // 直接切换视图，由transition组件处理动画效果
    currentTab.value = 'dashboard';
    
    // 延迟显示仪表盘，让其有独立的入场动画
    setTimeout(() => {
      dashboardVisible.value = true;
    }, 300);
  }
}

onMounted(() => {
  // 添加全局点击事件监听
  document.addEventListener('click', () => {
    contextMenuVisible.value = false;
  });
  
  // 初始化动画状态
  if (currentTab.value === 'dashboard') {
    setTimeout(() => {
      dashboardVisible.value = true;
    }, 300);
  } else {
    setTimeout(() => {
      appListVisible.value = true;
    }, 300);
  }
});

onUnmounted(() => {
  // 移除全局点击事件监听
  document.removeEventListener('click', () => {
    contextMenuVisible.value = false;
  });
});
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.cover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transform: scale(1.05);
}

.mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.main-content {
  position: relative;
  z-index: 1;
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.new-layout {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  min-height: 100vh;
  padding-top: 5vh;
}

/* 交叉淡入淡出切换效果 */
.fade-cross-enter-active,
.fade-cross-leave-active {
  transition: all 0.6s cubic-bezier(0.23, 1, 0.32, 1);
}

.fade-cross-enter-from {
  opacity: 0;
  transform: scale(0.9);
}

.fade-cross-leave-to {
  opacity: 0;
  transform: scale(1.1);
}

/* 容器样式 */
.applist-container,
.dashboard-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 应用内容包装器 */
.app-content-wrapper {
  width: 100%;
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 应用列表动画 */
.app-content-wrapper .new-app-grid {
  opacity: 0;
  transform: translateY(20px);
}

.app-content-wrapper .new-app-grid.animate-in {
  animation: cardFadeIn 0.6s ease forwards;
}

/* 后台过渡动画效果 */
.backend-transition {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(8px);
  z-index: 9999;
}

.transition-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

.transition-text {
  color: white;
  font-size: 1.2rem;
  font-weight: 500;
  margin-top: 1rem;
}

/* 淡入缩放动画 */
.fade-zoom-enter-active {
  animation: fadeInZoom 0.3s ease-out;
}

.fade-zoom-leave-active {
  animation: fadeOutZoom 0.5s ease-in;
}

@keyframes fadeInZoom {
  from {
    opacity: 0;
    transform: scale(1.1);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes fadeOutZoom {
  from {
    opacity: 1;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(0.9);
  }
}

@keyframes cardFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }
}
</style> 