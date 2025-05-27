import { ref } from 'vue'
import type { DockerWebSocketMessage } from '@/api/websocket/types'

// WebSocket 连接状态
export const wsStatus = ref<'connecting' | 'connected' | 'disconnected'>('disconnected')

// WebSocket 客户端类
class WebSocketClient {
  private static instance: WebSocketClient | null = null
  private ws: WebSocket | null = null
  private messageHandler: ((message: DockerWebSocketMessage) => void) | null = null
  private eventHandlers: Map<string, ((data: any) => void)[]> = new Map()
  private reconnectTimer: number | null = null
  private heartbeatTimer: number | null = null
  private heartbeatTimeoutTimer: number | null = null
  private readonly heartbeatInterval = 30000 // 30秒发送一次心跳
  private readonly heartbeatTimeout = 10000 // 10秒没收到响应就认为断开
  private isReconnecting = false
  private lastHeartbeatResponse = Date.now()
  private missedHeartbeats = 0
  private readonly maxMissedHeartbeats = 3 // 最多允许3次心跳未响应

  private constructor(private url: string) {}

  // 获取单例实例
  public static getInstance(url: string): WebSocketClient {
    if (!WebSocketClient.instance) {
      WebSocketClient.instance = new WebSocketClient(url)
    }
    return WebSocketClient.instance
  }

  // 检查是否已连接
  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }

  // 添加事件监听器
  on(eventType: string, handler: (data: any) => void) {
    if (!this.eventHandlers.has(eventType)) {
      this.eventHandlers.set(eventType, [])
    }
    this.eventHandlers.get(eventType)!.push(handler)
    console.log(`✅ 添加事件监听器: ${eventType}`)
  }

  // 移除事件监听器
  off(eventType: string, handler?: (data: any) => void) {
    if (handler) {
      const handlers = this.eventHandlers.get(eventType)
      if (handlers) {
        const index = handlers.indexOf(handler)
        if (index > -1) {
          handlers.splice(index, 1)
        }
      }
    } else {
      // 移除所有该类型的监听器
      this.eventHandlers.delete(eventType)
    }
    console.log(`✅ 移除事件监听器: ${eventType}`)
  }

  // 触发事件
  private emit(eventType: string, data: any) {
    const handlers = this.eventHandlers.get(eventType)
    if (handlers) {
      handlers.forEach(handler => {
        try {
          handler(data)
        } catch (error) {
          console.error(`事件处理器错误 [${eventType}]:`, error)
        }
      })
    }
  }

  // 设置消息处理器
  setMessageHandler(handler: (message: DockerWebSocketMessage) => void) {
    this.messageHandler = handler
  }

  // 连接服务器
  async connect() {
    if (this.ws?.readyState === WebSocket.OPEN || this.isReconnecting) {
      return
    }

    try {
      this.isReconnecting = true
      this.ws = new WebSocket(this.url)
      wsStatus.value = 'connecting'

      this.ws.onopen = () => {
        console.log('✅ WebSocket 连接成功')
        wsStatus.value = 'connected'
        this.isReconnecting = false
        this.lastHeartbeatResponse = Date.now()
        this.missedHeartbeats = 0
        this.startHeartbeat()
      }

      this.ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data) as DockerWebSocketMessage
          
          // 处理心跳响应
          if (message.type === 'HEARTBEAT') {
            this.lastHeartbeatResponse = Date.now()
            this.missedHeartbeats = 0
            if (this.heartbeatTimeoutTimer) {
              clearTimeout(this.heartbeatTimeoutTimer)
              this.heartbeatTimeoutTimer = null
            }
            return
          }

          // 触发对应的事件监听器
          this.emit(message.type, message)
          
          // 兼容旧的消息处理器
          this.messageHandler?.(message)
        } catch (error) {
          console.error('❌ 解析消息失败:', error)
        }
      }

      this.ws.onclose = () => {
        console.log('❌ WebSocket 连接关闭')
        wsStatus.value = 'disconnected'
        this.stopHeartbeat()
        this.isReconnecting = false
        this.reconnect()
      }

      this.ws.onerror = (error) => {
        console.error('❌ WebSocket 错误:', error)
        wsStatus.value = 'disconnected'
        this.stopHeartbeat()
        this.isReconnecting = false
      }
    } catch (error) {
      console.error('❌ WebSocket 连接失败:', error)
      this.isReconnecting = false
      this.reconnect()
    }
  }

  // 发送消息
  send(message: DockerWebSocketMessage) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    } else {
      console.warn('⚠️ WebSocket 未连接')
      this.connect() // 尝试重新连接
    }
  }

  // 断开连接
  disconnect() {
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.heartbeatTimeoutTimer) {
      clearTimeout(this.heartbeatTimeoutTimer)
      this.heartbeatTimeoutTimer = null
    }
    this.ws?.close()
    this.ws = null
    this.eventHandlers.clear()
    WebSocketClient.instance = null
  }

  // 重连
  private reconnect() {
    if (this.reconnectTimer || this.isReconnecting) {
      return
    }
    this.reconnectTimer = window.setTimeout(() => {
      console.log('🔄 尝试重新连接...')
      this.reconnectTimer = null
      this.connect()
    }, 3000)
  }

  // 开始心跳
  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        // 发送心跳
        this.send({ 
          type: 'HEARTBEAT', 
          taskId: 'heartbeat',
          timestamp: Date.now() 
        })
        
        // 设置心跳超时检测
        if (this.heartbeatTimeoutTimer) {
          clearTimeout(this.heartbeatTimeoutTimer)
        }
        this.heartbeatTimeoutTimer = window.setTimeout(() => {
          this.missedHeartbeats++
          console.log(
            `💔 心跳未响应 (${this.missedHeartbeats}/${this.maxMissedHeartbeats})`
          )
          
          if (this.missedHeartbeats >= this.maxMissedHeartbeats) {
            console.log('💀 心跳连续未响应，断开连接')
            this.ws?.close()
          }
        }, this.heartbeatTimeout)
      }
    }, this.heartbeatInterval)
  }

  // 停止心跳
  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
    if (this.heartbeatTimeoutTimer) {
      clearTimeout(this.heartbeatTimeoutTimer)
      this.heartbeatTimeoutTimer = null
    }
  }
}

// 创建 WebSocket 实例
const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${
  window.location.host
}/ws/docker`

// 导出单例实例
export const websocketClient = WebSocketClient.getInstance(wsUrl)

// 兼容旧导出
export const ws = websocketClient 