import { sendWebSocketMessage } from './websocket'
import type { WebSocketCallbacks } from './websocket/types'
import { MessageType } from './websocket/types'

// 定义超时时间常量（毫秒）
const TIMEOUT = {
  SYSTEM_STATUS: 10000,  // 系统状态请求可能需要较长时间
} as const

/**
 * 获取系统状态信息
 * @param callbacks WebSocket回调函数
 */
export function getSystemStatus(callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.SYSTEM_STATUS,
    data: {},
    callbacks,
    timeout: TIMEOUT.SYSTEM_STATUS
  })
}

/**
 * 获取Docker Events监听状态
 */
export async function getDockerEventsStatus() {
  try {
    const response = await fetch('/api/docker/events/status')
    return await response.json()
  } catch (error) {
    console.error('获取Docker Events状态失败:', error)
    throw error
  }
}

/**
 * 启动Docker Events监听
 */
export async function startDockerEvents() {
  try {
    const response = await fetch('/api/docker/events/start', {
      method: 'POST'
    })
    return await response.json()
  } catch (error) {
    console.error('启动Docker Events失败:', error)
    throw error
  }
}

/**
 * 停止Docker Events监听
 */
export async function stopDockerEvents() {
  try {
    const response = await fetch('/api/docker/events/stop', {
      method: 'POST'
    })
    return await response.json()
  } catch (error) {
    console.error('停止Docker Events失败:', error)
    throw error
  }
}

/**
 * 重启Docker Events监听
 */
export async function restartDockerEvents() {
  try {
    const response = await fetch('/api/docker/events/restart', {
      method: 'POST'
    })
    return await response.json()
  } catch (error) {
    console.error('重启Docker Events失败:', error)
    throw error
  }
} 