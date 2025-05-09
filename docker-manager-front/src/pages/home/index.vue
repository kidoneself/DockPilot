<template>
  <div class="home-container" @contextmenu.prevent="homeStore.handlePageContextMenu">
    <!-- 背景图片和遮罩 -->
    <div
      class="cover"
      :style="{
        filter: `blur(${homeStore.backgroundBlur}px)`,
        background: `url(${backgroundImage}) no-repeat`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }"
    />
    <div class="mask" :style="{ backgroundColor: `rgba(0,0,0,${homeStore.backgroundMask})` }" />

    <main class="main-content new-layout">
      <transition name="fade-cross" mode="out-in">
        <div v-if="homeStore.currentTab === 'dashboard'" key="dashboard" class="dashboard-container">
          <!-- 仪表盘视图 -->
          <DashboardView :dashboardVisible="homeStore.dashboardVisible" />
        </div>
        <div v-else key="applist" class="applist-container">
          <!-- 应用面板内容 -->
          <div class="app-content-wrapper">
            <!-- 顶部大标题和时间 -->
            <HeaderClock />
            <!-- 搜索框 -->
            <SearchBar />
            <!-- APP 列表 -->
            <AppList :animateIn="homeStore.webServerListVisible" />
          </div>
        </div>
      </transition>
    </main>
    
    <!-- 悬浮按钮组 -->
    <FloatingButtons />

    <!-- 内嵌窗口 -->
    <EmbeddedWindow
      v-model:visible="homeStore.windowVisible"
      :title="homeStore.windowTitle"
      :url="homeStore.windowUrl"
    />

    <!-- 新增/编辑应用对话框 -->
    <AppEditDialog
      v-model:visible="homeStore.addAppVisible"
      :isEdit="homeStore.isEdit"
      :appData="homeStore.newApp"
    />

    <!-- 右键菜单 -->
    <ContextMenu
      :visible="homeStore.contextMenuVisible"
      :menuStyle="homeStore.contextMenuStyle"
      :contextItem="homeStore.currentContextItem"
    />

    <!-- 确认对话框 -->
    <ConfirmDialog
      v-model:visible="homeStore.confirmVisible"
      :title="homeStore.confirmConfig.title"
      :content="homeStore.confirmConfig.content"
      @confirm="homeStore.handleConfirmConfirm"
      @cancel="homeStore.handleConfirmCancel"
    />

    <!-- 后台进入过渡动画 -->
    <transition name="fade-zoom">
      <div v-if="homeStore.enteringBackend" class="backend-transition">
        <div class="transition-content">
          <t-loading theme="dots" size="large" />
          <div class="transition-text">正在进入管理后台...</div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { useHomeStore } from '@/store/modules/home';

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

// 初始化 store
const homeStore = useHomeStore();

onMounted(() => {
  // 添加全局点击事件监听
  document.addEventListener('click', () => {
    homeStore.contextMenuVisible = false;
  });
  
  // 初始化动画状态
  homeStore.initAnimations();

  // 获取 Web 服务器列表
  homeStore.fetchWebServers().catch(error => {
    console.error('获取 Web 服务器列表失败:', error);
  });
});

onUnmounted(() => {
  // 移除全局点击事件监听
  document.removeEventListener('click', () => {
    homeStore.contextMenuVisible = false;
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