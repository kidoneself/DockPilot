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
import { computed } from 'vue'
import { darkTheme } from 'naive-ui'
import { useThemeStore } from '@/store/theme'
import WebSocketError from '@/components/WebSocketError.vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

// 配置 highlight.js
hljs.configure({
  ignoreUnescapedHTML: true
})

const themeStore = useThemeStore()
const theme = computed(() => themeStore.theme === 'dark' ? darkTheme : null)
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