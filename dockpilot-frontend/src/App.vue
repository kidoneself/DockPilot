<template>
  <n-config-provider :theme="theme" :hljs="hljs">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <n-loading-bar-provider>
            <WebSocketError />
            <router-view />
          </n-loading-bar-provider>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { darkTheme } from 'naive-ui'
import { useThemeStore } from '@/store/theme'
import { getCurrentBackground } from '@/api/http/background'
import WebSocketError from '@/components/WebSocketError.vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
// 导入默认背景图片
import defaultBackgroundImg from '@/assets/background.png'

// 配置 highlight.js
hljs.configure({
  ignoreUnescapedHTML: true
})

const themeStore = useThemeStore()
const theme = computed(() => themeStore.theme === 'dark' ? darkTheme : null)

// 应用背景
const applyBackground = (backgroundUrl: string) => {
  document.body.style.backgroundImage = `url(${backgroundUrl}?t=${Date.now()})`
  document.body.style.backgroundSize = 'cover'
  document.body.style.backgroundPosition = 'center'
  document.body.style.backgroundAttachment = 'fixed'
  document.body.style.backgroundRepeat = 'no-repeat'
}

onMounted(async () => {
  try {
    const backgroundUrl = await getCurrentBackground()
    if (backgroundUrl) {
      // 使用后端配置的背景
      applyBackground(backgroundUrl)
      console.log('✅ 后端背景已应用:', backgroundUrl)
    } else {
      // 使用默认背景图片
      applyBackground(defaultBackgroundImg)
      console.log('✅ 默认背景已应用:', defaultBackgroundImg)
    }
  } catch (error) {
    // 如果获取后端背景失败，也使用默认背景
    applyBackground(defaultBackgroundImg)
    console.log('⚠️ 获取后端背景失败，使用默认背景:', error)
  }
})
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen,
    Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

#app {
  height: 100%;
}
</style> 