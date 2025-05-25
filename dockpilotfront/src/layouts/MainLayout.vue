<template>
  <n-layout has-sider style="height: 100vh;">
    <!-- 顶部栏 -->
    <n-layout-header 
      bordered 
      class="header" 
      style="position: fixed; top: 0; left: 0; right: 0; z-index: 100; height: 50px;"
    >
      <div class="header-left">
        <h2>Docker 管理系统</h2>
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

</style>