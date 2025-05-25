<template>
  <n-layout has-sider style="height: 100vh;">
    <!-- 顶部栏 -->
    <n-layout-header 
      bordered 
      class="header" 
      style="position: fixed; top: 0; left: 0; right: 0; z-index: 100; height: 50px;"
    >
      <div class="header-left">
        <div class="logo-container" @click="goToHome">
          <svg class="logo-icon" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#3b82f6"/>
                <stop offset="100%" style="stop-color:#8b5cf6"/>
              </linearGradient>
            </defs>
            
            <!-- 简洁的立方体容器 -->
            <path d="M8 12 L16 8 L24 12 L24 20 L16 24 L8 20 Z" 
                  fill="none" 
                  stroke="url(#logoGradient)" 
                  stroke-width="2" 
                  stroke-linejoin="round"/>
            
            <!-- 内部连接点 -->
            <circle cx="16" cy="16" r="2" fill="url(#logoGradient)"/>
            
            <!-- 顶部面 -->
            <path d="M8 12 L16 8 L24 12 L16 16 Z" 
                  fill="url(#logoGradient)" 
                  opacity="0.2"/>
          </svg>
          
          <div class="logo-text">
            <span class="brand-name">DockPilot</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <ThemeToggle style="margin-left: 16px;" />
        <n-dropdown
          :options="dropdownOptions"
          trigger="click"
          @select="handleDropdownSelect"
        >
          <div class="user-avatar">
            <n-avatar
              round
              size="small"
              :src="userAvatar"
              fallback-src="https://07akioni.oss-cn-beijing.aliyuncs.com/07akioni.jpeg"
            />
            <span class="username">{{ username }}</span>
          </div>
        </n-dropdown>
      </div>
    </n-layout-header>

    <!-- 左侧菜单 -->
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="240"
      :collapsed="collapsed"
      show-trigger
      :native-scrollbar="false"
      style="position: fixed; left: 0; top: 50px; bottom: 0; z-index: 99;"
      @collapse="collapsed = true"
      @expand="collapsed = false"
    >
      <n-menu
        :collapsed="collapsed"
        :options="menuOptions"
        :value="activeKey"
        @update:value="handleMenuUpdate"
      />
    </n-layout-sider>

    <!-- 右侧内容区域容器 -->
    <n-layout 
      style="
        margin-top: 50px; 
        padding-bottom: 50px; 
        margin-left: 240px; 
        transition: margin-left 0.3s; 
        height: calc(100vh - 50px - 50px); 
        overflow-y: auto;
      "
      :style="{ marginLeft: collapsed ? '64px' : '240px' }"
    >
      <!-- 内容区域 -->
      <n-layout-content content-style="padding: 24px;">
        <router-view />
      </n-layout-content>
    </n-layout>

    <!-- 底部栏 - 固定 -->
    <n-layout-footer
      bordered
      class="main-footer"
      style="
        position: fixed; 
        bottom: 0; 
        right: 0; 
        height: 50px; 
        display: flex; 
        align-items: center; 
        justify-content: center; 
        z-index: 99;
      "
      :style="{ left: collapsed ? '64px' : '240px' }"
    >
      © 2025 DockPilot Powered by KID
    </n-layout-footer>
  </n-layout>
</template>

<script setup lang="ts">
import { ref, h, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import { NIcon } from 'naive-ui'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import * as Icons from '@vicons/ionicons5'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()
const collapsed = ref(false)
const activeKey = computed(() => route.path)
const username = computed(() => userStore.userInfo?.username || 'Admin')
const userAvatar = ref('')

// 渲染图标
function renderIcon(icon: string) {
  // 将连字符格式转换为驼峰格式
  const iconName = icon
    .split('-')
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')
  return () => h(NIcon, null, { default: () => h(Icons[iconName as keyof typeof Icons]) })
}

// 下拉菜单选项
const dropdownOptions = [
  {
    label: '个人中心',
    key: 'profile',
    icon: renderIcon('PersonOutline')
  },
  {
    label: '退出登录',
    key: 'logout',
    icon: renderIcon('LogOutOutline')
  }
]

// 处理下拉菜单选择
const handleDropdownSelect = (key: string) => {
  switch (key) {
    case 'profile':
      // TODO: 实现个人中心功能
      message.info('个人中心功能开发中')
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 退出登录处理
const handleLogout = () => {
  userStore.logout()
  message.success('已退出登录')
}

// 将路由配置转换为菜单选项
function generateMenuOptions(routes: any[], parentPath: string = ''): MenuOption[] {
  return routes
    .filter(route => !route.meta?.hidden)
    .map(route => {
      const fullPath = parentPath ? `${parentPath}/${route.path}` : `/${route.path}`
      const menuOption: MenuOption = {
        label: route.meta?.title || route.name,
        key: fullPath,
        icon: route.meta?.icon ? renderIcon(route.meta.icon) : undefined
      }

      if (route.children) {
        menuOption.children = generateMenuOptions(route.children, fullPath)
      }

      return menuOption
    })
}

// 生成菜单选项
const menuOptions = computed(() => {
  const mainRoute = router.options.routes.find(route => route.path === '/')
  return mainRoute?.children ? generateMenuOptions(mainRoute.children) : []
})

// 处理菜单更新
const handleMenuUpdate = (key: string) => {
  router.push(key)
}

// 返回首页
const goToHome = () => {
  router.push('/navigation')
}

</script>

<style scoped>
.header {
  height: 50px;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--n-color);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-avatar {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-avatar:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.username {
  margin-left: 8px;
  font-size: 14px;
}

.main-footer {
  text-align: center;
  padding: 12px 0;
  font-size: 14px;
  color: #888;
  background: transparent;
  /* Removed fixed positioning */
}

:deep(.n-layout-scroll-container) {
  overflow-x: hidden;
}

/* Added style for the content area to take available space and allow scrolling */
.n-layout-content {
  /* flex: 1; Removed flex as we are calculating height explicitly */
  min-height: 0; /* Allow content to shrink below its default size */
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  padding: 4px 8px;
}

.logo-container:hover {
  background-color: rgba(59, 130, 246, 0.1);
}

.logo-icon {
  height: 24px;
  width: 24px;
  transition: all 0.3s ease;
}

.logo-icon:hover {
  transform: scale(1.1);
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
}
</style>