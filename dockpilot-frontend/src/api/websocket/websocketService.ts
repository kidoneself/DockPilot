import { ws } from '@/utils/websocketClient'
import type { DockerWebSocketMessage, WebSocketCallbacks, WebSocketRequestOptions } from './types'

// 任务管理器
class TaskManager {
  private taskHandlers = new Map<string, WebSocketCallbacks>()
  private taskTimeouts = new Map<string, number>()

  // 注册任务处理器
  registerTask(taskId: string, callbacks: WebSocketCallbacks, timeout: number = 30000) {
    this.taskHandlers.set(taskId, callbacks)
    
    // 设置超时处理
    const timeoutId = window.setTimeout(() => {
      const handler = this.taskHandlers.get(taskId)
      if (handler) {
        handler.onError?.('操作超时', taskId)
        this.removeTask(taskId)
      }
    }, timeout)
    
    this.taskTimeouts.set(taskId, timeoutId)
  }

  // 处理消息
  handleMessage(message: DockerWebSocketMessage) {
    const handler = this.taskHandlers.get(message.taskId)
    if (!handler) return

    switch (message.type) {
      case 'PROGRESS':
        if (typeof message.progress === 'number') {
          handler.onProgress?.(message.progress, message.taskId)
        }
        break
      case 'LOG':
        if (message.data) {
          handler.onLog?.(message.data, message.taskId)
        }
        break
      case 'COMPLETE':
        handler.onComplete?.(message, message.taskId)
        this.removeTask(message.taskId)
        break
      case 'ERROR':
        handler.onError?.(message.errorMessage || '操作失败', message.taskId)
        this.removeTask(message.taskId)
        break
    }
  }

  // 移除任务
  private removeTask(taskId: string) {
    const timeoutId = this.taskTimeouts.get(taskId)
    if (timeoutId) {
      clearTimeout(timeoutId)
      this.taskTimeouts.delete(taskId)
    }
    this.taskHandlers.delete(taskId)
  }
}

// 创建任务管理器实例
const taskManager = new TaskManager()

// 设置消息处理器
ws.setMessageHandler((message: DockerWebSocketMessage) => {
  taskManager.handleMessage(message)
})

// 生成任务ID
function generateTaskId(type: string): string {
  return `${type}_${Date.now()}_${Math.random().toString(36).slice(2)}`
}

// 发送消息
export async function sendWebSocketMessage(options: WebSocketRequestOptions): Promise<void> {
  const taskId = generateTaskId(options.type)

  // 注册任务处理器
  taskManager.registerTask(taskId, options.callbacks, options.timeout ?? 30000)

  try {
    // 确保连接
    await ws.connect()
    
    // 为不同类型的消息准备不同的数据格式
    let messageData = options.data
    
    // 特殊处理 CONTAINER_UPDATE 消息
    if (options.type === 'CONTAINER_UPDATE' && options.data) {
      // 添加调试日志
      console.log('🚀 发送 CONTAINER_UPDATE 消息')
      console.log('📊 原始数据:', JSON.stringify(options.data, null, 2))
      
      // 确保数据结构正确 - 现在数据结构是 { containerId, config: { image, ... } }
      if (!options.data.containerId) {
        throw new Error('缺少 containerId 参数')
      }
      
      // 检查 config 对象是否存在
      if (!options.data.config) {
        throw new Error('缺少 config 参数')
      }
      
      // 检查 config 中的 image 字段
      if (!options.data.config.image) {
        throw new Error('缺少 image 参数')
      }
      
      console.log('🔍 关键字段检查:')
      console.log('  - containerId:', options.data.containerId)
      console.log('  - config.image:', options.data.config.image)
      console.log('  - config.name:', options.data.config.name)
      
      messageData = options.data
    }
    
    // 发送消息
    const message = {
      type: options.type,
      taskId,
      data: messageData,
      timestamp: Date.now()
    }
    
    console.log('发送WebSocket消息:', message)
    ws.send(message)
  } catch (error) {
    console.error('发送WebSocket消息失败:', error)
    taskManager.handleMessage({
      type: 'ERROR',
      taskId,
      errorMessage: '发送消息失败: ' + (error instanceof Error ? error.message : '未知错误'),
      timestamp: Date.now()
    })
    throw error
  }
} 