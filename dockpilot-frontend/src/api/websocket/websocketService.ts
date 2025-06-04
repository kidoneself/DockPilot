import { ws } from '@/utils/websocketClient'
import type { DockerWebSocketMessage, WebSocketCallbacks, WebSocketRequestOptions } from './types'

// 任务管理器
class TaskManager {
  private taskHandlers = new Map<string, WebSocketCallbacks>()
  private taskTimeouts = new Map<string, number>()
  private globalHandler: WebSocketCallbacks | null = null

  // 设置全局消息处理器（用于处理未注册的任务）
  setGlobalHandler(handler: WebSocketCallbacks) {
    this.globalHandler = handler
    console.log('📝 设置全局消息处理器')
  }

  // 注册任务处理器
  registerTask(taskId: string, callbacks: WebSocketCallbacks, timeout: number = 30000) {
    console.log(`📝 注册任务处理器: ${taskId}`)
    this.taskHandlers.set(taskId, callbacks)
    
    // 设置超时处理
    const timeoutId = window.setTimeout(() => {
      const handler = this.taskHandlers.get(taskId)
      if (handler) {
        console.log(`⏰ 任务超时: ${taskId}`)
        handler.onError?.('操作超时', taskId)
        this.removeTask(taskId)
      }
    }, timeout)
    
    this.taskTimeouts.set(taskId, timeoutId)
  }

  // 处理消息
  handleMessage(message: DockerWebSocketMessage) {
    console.log('🔍 TaskManager 收到消息:', message.type, 'taskId:', message.taskId, 'progress:', message.progress)
    
    // 🔥 特殊处理：Docker事件通知（无需注册任务，直接广播）
    if (message.type === 'DOCKER_EVENT_NOTIFICATION') {
      console.log('🔔 处理Docker事件通知:', message.data)
      // 触发自定义事件，让通知处理器接收
      window.dispatchEvent(new CustomEvent('docker-websocket-message', {
        detail: message
      }))
      return
    }
    
    let handler = this.taskHandlers.get(message.taskId)
    
    if (!handler) {
      console.warn('⚠️ 未找到对应的任务处理器:', message.taskId)
      console.log('📋 当前注册的任务:', Array.from(this.taskHandlers.keys()))
      
      // 🔧 关键修复：如果没有找到特定任务的处理器，尝试使用全局处理器
      if (this.globalHandler) {
        console.log('🌐 使用全局处理器处理消息:', message.type)
        handler = this.globalHandler
      } else {
        console.warn('❌ 没有全局处理器，跳过消息')
        return
      }
    } else {
      console.log('✅ 找到任务处理器，开始处理消息:', message.type)
    }

    switch (message.type) {
      case 'PROGRESS':
        // 🔧 修复：处理新的进度消息格式
        let progress = null
        let imageName = null
        
        if (message.data && typeof message.data === 'object') {
          // 新格式：进度和镜像名称都在 data 中
          if ('progress' in message.data && typeof message.data.progress === 'number') {
            progress = message.data.progress
            imageName = message.data.imageName
            console.log(`📈 处理进度消息 (新格式): ${progress}% (taskId: ${message.taskId})`)
            console.log(`📊 消息包含镜像名称: ${imageName}`)
          }
        } else if (typeof message.progress === 'number') {
          // 旧格式：进度直接在 message.progress 中
          progress = message.progress
          console.log(`📈 处理进度消息 (旧格式): ${progress}% (taskId: ${message.taskId})`)
        }
        
        if (progress !== null) {
          if (imageName) {
            handler.onProgress?.(progress, message.taskId, imageName)
          } else {
            handler.onProgress?.(progress, message.taskId)
          }
        } else {
          console.warn('⚠️ 进度消息格式错误:', message)
        }
        break
      case 'LOG':
        if (message.data) {
          console.log(`📝 处理日志消息: ${message.data} (taskId: ${message.taskId})`)
          
          // 🔧 修复：处理日志消息的镜像名称
          let logMessage = message.data
          let imageName = null
          
          if (typeof message.data === 'object' && 'message' in message.data) {
            logMessage = message.data.message
            imageName = message.data.imageName
            console.log(`📊 日志消息包含镜像名称: ${imageName}`)
          }
          
          if (imageName) {
            handler.onLog?.(logMessage, message.taskId, imageName)
          } else {
            handler.onLog?.(logMessage, message.taskId)
          }
        }
        break
      case 'COMPLETE':
        console.log(`✅ 处理完成消息 (taskId: ${message.taskId})`)
        handler.onComplete?.(message, message.taskId)
        // 只有注册的任务才移除，全局处理器不移除
        if (this.taskHandlers.has(message.taskId)) {
          this.removeTask(message.taskId)
        }
        break
      case 'ERROR':
        console.log(`❌ 处理错误消息: ${message.errorMessage} (taskId: ${message.taskId})`)
        handler.onError?.(message.errorMessage || '操作失败', message.taskId)
        // 只有注册的任务才移除，全局处理器不移除
        if (this.taskHandlers.has(message.taskId)) {
          this.removeTask(message.taskId)
        }
        break
      default:
        console.log(`🔍 未处理的消息类型: ${message.type} (taskId: ${message.taskId})`)
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
      
      // 确保数据结构正确
      if (!options.data.containerId) {
        throw new Error('缺少 containerId 参数')
      }
      
      // 🎯 智能判断：如果只有containerId，说明是镜像更新；如果有config，说明是完整配置更新
      const dataKeys = Object.keys(options.data)
      const isImageUpdate = dataKeys.length === 1 && dataKeys[0] === 'containerId'
      
      if (isImageUpdate) {
        console.log('🔍 检测到镜像更新模式（只有containerId）')
      } else {
        console.log('🔍 检测到配置更新模式（包含config）')
        
        // 只有在配置更新模式下才检查 config 参数
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
      }
      
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

// 导出全局任务管理器，供其他模块使用
export { taskManager } 