<template>
  <n-dropdown
    :options="themeOptions"
    trigger="click"
    placement="bottom"
    @select="handleThemeSelect"
    
  >
    <n-button
      quaternary
      circle
      :title="themeStore.theme === 'light' ? '切换到暗黑模式' : '切换到明亮模式'"
    >
      <NIcon>
        <SunnyOutline v-if="themeStore.theme === 'light'" />
        <MoonOutline v-else />
      </NIcon>
    </n-button>
  </n-dropdown>
</template>

<script setup lang="ts">
import { h } from 'vue'
import { NIcon } from 'naive-ui'
import type { DropdownOption } from 'naive-ui'
import {
  SunnyOutline,
  MoonOutline,
  DesktopOutline
} from '@vicons/ionicons5'
import { useThemeStore } from '@/store/theme'

const themeStore = useThemeStore()

// 渲染图标
function renderIcon(icon: any) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

// 主题切换选项
const themeOptions: DropdownOption[] = [
  {
    label: '浅色模式',
    key: 'light',
    icon: renderIcon(SunnyOutline)
  },
  {
    label: '深色模式',
    key: 'dark',
    icon: renderIcon(MoonOutline)
  },
  {
    label: '跟随系统',
    key: 'auto',
    icon: renderIcon(DesktopOutline)
  }
]

// 处理主题切换
function handleThemeSelect(key: string) {
  if (key === 'auto') {
    themeStore.toggleAuto()
  } else {
    themeStore.theme = key as 'light' | 'dark'
    themeStore.isAuto = false
    localStorage.setItem('theme', key)
  }
}
</script> 