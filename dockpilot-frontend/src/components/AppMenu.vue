<template>
  <n-menu
    :value="activeKey"
    :options="menuOptions"
    :collapsed="collapsed"
    :collapsed-width="64"
    :collapsed-icon-size="22"
    @update:value="handleMenuClick"
  />
</template>

<script setup lang="ts">
import { h, computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NIcon } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import * as Icons from '@vicons/ionicons5'

const router = useRouter()
const route = useRoute()

// 控制菜单折叠状态
const collapsed = ref(false)

// 当前激活的菜单项
const activeKey = computed(() => route.path)

// 渲染图标
function renderIcon(icon: string) {
  return () => h(NIcon, null, { default: () => h(Icons[icon as keyof typeof Icons]) })
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

// 处理菜单点击
function handleMenuClick(key: string) {
  router.push(key)
}
</script> 