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
          <!-- 新logo -->
          <img 
            src="/logo.svg" 
            class="logo-new" 
            alt="DockPilot New"
          />
          <!-- 原logo -->
          <img 
            :src="logo" 
            class="logo-old" 
            alt="DockPilot Classic"
          />
        </div>
      </div>
      <div class="header-right">
        <!-- 更新通知组件 -->
        <UpdateNotification style="margin-right: 16px;" />
        <ThemeToggle style="margin-left: 16px;" />
        <n-dropdown
          :options="dropdownOptions"
          trigger="click"
          @select="handleDropdownSelect"
        >
          <div class="user-info">
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

  <!-- 修改密码模态框 -->
  <ChangePasswordModal 
    v-model:show="showChangePasswordModal" 
    @success="handlePasswordChangeSuccess"
  />
</template>

<script setup lang="ts">
import { ref, h, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import { NIcon } from 'naive-ui'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import UpdateNotification from '@/components/UpdateNotification.vue'
import ChangePasswordModal from '@/components/common/ChangePasswordModal.vue'
import * as Icons from '@vicons/ionicons5'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'

// 导入 logo 图片
import logo from '@/assets/icons/logo.svg'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()
const themeStore = useThemeStore()
const collapsed = ref(false)
const activeKey = computed(() => route.path)
const username = computed(() => userStore.userInfo?.username || 'Admin')
const showChangePasswordModal = ref(false)

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
      showChangePasswordModal.value = true
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 处理密码修改成功
const handlePasswordChangeSuccess = () => {
  // 可以在这里添加额外的成功处理逻辑，比如重新获取用户信息等
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

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
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
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  padding: 4px 8px;
  gap: 2px;
}

.logo-container:hover {
  background-color: rgba(59, 130, 246, 0.1);
}

.logo-new {
  height: 28px;
  width: auto;
  transition: all 0.3s ease;
}

.logo-old {
  height: 24px;
  width: auto;
  transition: all 0.3s ease;
  /* 确保 logo 颜色跟随主题 */
  filter: var(--logo-filter, none);
}

.logo-container:hover .logo-new,
.logo-container:hover .logo-old {
  transform: scale(1.05);
}

/* 深色主题下的 logo 样式 */
:root[data-theme="dark"] .logo-old {
  --logo-filter: brightness(0) invert(1);
}

/* 保留原有样式以防万一 */
.logo-full {
  height: 32px;
  width: auto;
  transition: all 0.3s ease;
  /* 确保 logo 颜色跟随主题 */
  filter: var(--logo-filter, none);
}

.logo-full:hover {
  transform: scale(1.05);
}

/* 删除不需要的样式 */
.logo-icon,
.logo-text,
.brand-name {
  display: none;
}
</style>