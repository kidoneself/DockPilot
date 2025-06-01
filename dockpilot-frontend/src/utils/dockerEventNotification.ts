import { createDiscreteApi, darkTheme, lightTheme } from 'naive-ui'
import type { DockerEventNotification } from '@/api/websocket/types'
import { MessageType } from '@/api/websocket/types'
import { useThemeStore } from '@/store/theme'

/**
 * 获取当前主题
 */
function getCurrentTheme() {
  try {
    // 🔥 修复：使用项目的主题store获取当前主题
    const themeStore = useThemeStore()
    return themeStore.theme === 'dark' ? darkTheme : lightTheme
  } catch (error) {
    // 如果store不可用，回退到检测系统主题
    console.warn('无法获取主题store，使用系统主题:', error)
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    return prefersDark ? darkTheme : lightTheme
  }
}

// 创建独立的通知API（手动传递主题配置）
function createThemedNotificationApi() {
  const theme = getCurrentTheme()
  return createDiscreteApi(['notification'], {
    configProviderProps: {
      theme: theme
    }
  })
}

/**
 * Docker事件通知处理器
 */
export class DockerEventNotificationHandler {
  private static instance: DockerEventNotificationHandler | null = null
  
  private constructor() {}
  
  public static getInstance(): DockerEventNotificationHandler {
    if (!DockerEventNotificationHandler.instance) {
      DockerEventNotificationHandler.instance = new DockerEventNotificationHandler()
    }
    return DockerEventNotificationHandler.instance
  }

  /**
   * 处理Docker事件通知
   */
  public handleDockerEventNotification(data: DockerEventNotification) {
    try {
      const { eventType, message } = data
      
      // 🔥 修复：每次都重新创建API以获取最新主题
      const { notification } = createThemedNotificationApi()
      
      // 使用统一的info类型，通过图标区分，让主题自动适配颜色
      switch (eventType) {
        case 'create':
          notification.info({
            title: '📦 容器创建',
            content: message,
            duration: 3000,
            keepAliveOnHover: true
          })
          break
          
        case 'start':
          notification.info({
            title: '▶️ 容器启动',
            content: message,
            duration: 3000,
            keepAliveOnHover: true
          })
          break
          
        case 'stop':
        case 'kill':
        case 'die':
          notification.info({
            title: '⏹️ 容器停止',
            content: message,
            duration: 3000,
            keepAliveOnHover: true
          })
          break
          
        case 'destroy':
          notification.info({
            title: '🗑️ 容器删除',
            content: message,
            duration: 4000,
            keepAliveOnHover: true
          })
          break
          
        case 'rename':
          notification.info({
            title: '📝 容器重命名',
            content: message,
            duration: 3000,
            keepAliveOnHover: true
          })
          break
          
        default:
          notification.info({
            title: '🔔 Docker事件',
            content: message,
            duration: 3000,
            keepAliveOnHover: true
          })
          break
      }
      
      console.log('📢 Docker事件通知:', data)
      
    } catch (error) {
      console.error('处理Docker事件通知失败:', error)
    }
  }

  /**
   * 注册WebSocket消息监听器
   */
  public registerWebSocketListener() {
    // 监听全局WebSocket消息
    window.addEventListener('docker-websocket-message', (event: any) => {
      try {
        const message = event.detail
        if (message.type === MessageType.DOCKER_EVENT_NOTIFICATION) {
          this.handleDockerEventNotification(message.data)
        }
      } catch (error) {
        console.error('处理WebSocket消息失败:', error)
      }
    })
    
    console.log('🔧 Docker事件通知监听器已注册')
  }

  /**
   * 手动测试通知（调试用）
   */
  public testNotification() {
    this.handleDockerEventNotification({
      eventType: 'start',
      containerId: 'test123',
      containerName: 'test-container',
      message: '▶️ 容器 test-container 已启动',
      timestamp: Date.now()
    })
  }
}

// 导出单例实例
export const dockerEventNotificationHandler = DockerEventNotificationHandler.getInstance() 