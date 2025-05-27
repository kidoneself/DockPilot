import { sendWebSocketMessage } from './websocket'
import type { WebSocketCallbacks } from './websocket/types'
import { MessageType } from './websocket/types'
import type {
  CreateNetworkRequest,
  DeleteNetworkRequest
} from './model/network'

// 定义超时时间常量（毫秒）
const TIMEOUT = {
  LIST: 5000,      // 列表请求超时时间较短
  CREATE: 30000,   // 创建网络可能需要较长时间
  DELETE: 30000,   // 删除网络可能需要较长时间
  DETAIL: 5000     // 获取详情超时时间较短
} as const

/**
 * 获取网络列表
 * @param callbacks WebSocket回调函数
 */
export function getNetworkList(callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.NETWORK_LIST,
    data: {},
    callbacks,
    timeout: TIMEOUT.LIST
  })
}

/**
 * 创建网络
 * @param data 网络配置数据
 * @param callbacks WebSocket回调函数
 */
export function createNetwork(data: CreateNetworkRequest, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.NETWORK_CREATE,
    data,
    callbacks,
    timeout: TIMEOUT.CREATE
  })
}

/**
 * 删除网络
 * @param networkId 网络ID
 * @param callbacks WebSocket回调函数
 */
export function deleteNetwork(networkId: string, callbacks: WebSocketCallbacks) {
  const data: DeleteNetworkRequest = { networkId }
  return sendWebSocketMessage({
    type: MessageType.NETWORK_DELETE,
    data,
    callbacks,
    timeout: TIMEOUT.DELETE
  })
}

/**
 * 获取网络详情
 * @param networkId 网络ID
 * @param callbacks WebSocket回调函数
 */
export function getNetworkDetail(networkId: string, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.NETWORK_DETAIL,
    data: { networkId },
    callbacks,
    timeout: TIMEOUT.DETAIL
  })
} 