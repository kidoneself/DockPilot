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