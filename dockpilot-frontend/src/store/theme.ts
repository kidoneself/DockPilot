import { defineStore } from 'pinia'
import { ref } from 'vue'

type ThemeMode = 'light' | 'dark'

// 检测系统主题
function getSystemTheme(): ThemeMode {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

// 从 localStorage 获取保存的主题设置
const savedTheme = localStorage.getItem('theme') as ThemeMode | null
const initialTheme = savedTheme || getSystemTheme()

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<ThemeMode>(initialTheme)
  const isAuto = ref(!savedTheme)

  // 监听系统主题变化
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const handleSystemThemeChange = (e: MediaQueryListEvent) => {
    if (isAuto.value) {
      theme.value = e.matches ? 'dark' : 'light'
    }
  }
  mediaQuery.addEventListener('change', handleSystemThemeChange)

  // 切换主题
  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    isAuto.value = false
    localStorage.setItem('theme', theme.value)
  }

  // 切换自动/手动模式
  function toggleAuto() {
    isAuto.value = !isAuto.value
    if (isAuto.value) {
      theme.value = getSystemTheme()
      localStorage.removeItem('theme')
    } else {
      localStorage.setItem('theme', theme.value)
    }
  }

  return {
    theme,
    isAuto,
    toggleTheme,
    toggleAuto
  }
}) 