import { createApp } from 'vue'
import { createPinia } from 'pinia'
import naive from 'naive-ui'
import App from './App.vue'
import router from './router'
import { ws } from './utils/websocketClient'

// 创建应用实例
const app = createApp(App)

// 使用插件
app.use(createPinia())
app.use(router)
app.use(naive)

// 初始化 WebSocket 连接
ws.connect().catch(error => {
  console.error('WebSocket 初始化失败:', error)
})

// 挂载应用
app.mount('#app') 