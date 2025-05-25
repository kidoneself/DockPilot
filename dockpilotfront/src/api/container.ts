import { sendWebSocketMessage } from './websocket'
import type { WebSocketCallbacks } from './websocket/types'
import { MessageType } from './websocket/types'

// 定义超时时间常量（毫秒）
const TIMEOUT = {
  LIST: 5000,      // 列表请求超时时间较短
  STATS: 3000,     // 性能数据请求超时时间较短
  START: 30000,    // 启动容器可能需要较长时间
  STOP: 30000,     // 停止容器可能需要较长时间
  RESTART: 30000,   // 重启容器可能需要较长时间
  CREATE: 60000     // 创建容器可能需要较长时间
} as const

/**
 * 获取容器列表
 * @param callbacks WebSocket回调函数
 */
export function getContainerList(callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.CONTAINER_LIST,
    callbacks,
    timeout: TIMEOUT.LIST
  })
}

/**
 * 获取容器性能数据
 * @param containerId 容器ID
 * @param callbacks WebSocket回调函数
 */
export function getContainerStats(containerId: string, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.CONTAINER_STATS,
    data: { containerId },
    callbacks,
    timeout: TIMEOUT.STATS
  })
}

/**
 * 启动容器
 * @param containerId 容器ID
 * @param callbacks WebSocket回调函数
 */
export function startContainer(containerId: string, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.CONTAINER_START,
    data: { containerId },
    callbacks,
    timeout: TIMEOUT.START
  })
}

/**
 * 停止容器
 * @param containerId 容器ID
 * @param callbacks WebSocket回调函数
 */
export function stopContainer(containerId: string, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.CONTAINER_STOP,
    data: { containerId },
    callbacks,
    timeout: TIMEOUT.STOP
  })
}

/**
 * 重启容器
 * @param containerId 容器ID
 * @param callbacks WebSocket回调函数
 */
export function restartContainer(containerId: string, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.CONTAINER_RESTART,
    data: { containerId },
    callbacks,
    timeout: TIMEOUT.RESTART
  })
}

/**
 * 删除容器
 * @param containerId 容器ID
 * @param callbacks WebSocket回调函数
 */
export function removeContainer(containerId: string, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.CONTAINER_DELETE,
    data: { containerId },
    callbacks,
    timeout: TIMEOUT.STOP
  })
} 

// 获取容器详情
export const getContainerDetail = async (containerId: string) => {
  return new Promise((resolve, reject) => {
    sendWebSocketMessage({
      type: MessageType.CONTAINER_DETAIL,
      data: { containerId },
      callbacks: {
        onComplete: (data) => {
          resolve(data)
        },
        onError: (error) => {
          reject(error)
        }
      },
      timeout: TIMEOUT.LIST
    })
  })
}

// 更新容器配置
export const updateContainer = async (
  containerId: string,
  config: any,
  callbacks?: {
    onLog?: (msg: string) => void
    onComplete?: (data: any) => void
    onError?: (err: any) => void
  }
) => {
  return new Promise((resolve, reject) => {
    sendWebSocketMessage({
      type: MessageType.CONTAINER_UPDATE,
      data: { containerId, config },
      callbacks: {
        onLog: callbacks?.onLog,
        onComplete: (data) => {
          callbacks?.onComplete?.(data)
          resolve(data)
        },
        onError: (error) => {
          callbacks?.onError?.(error)
          reject(error)
        }
      }
    })
  })
}

/**
 * 获取容器静态详情
 * @param containerId 容器ID
 * @param callbacks WebSocket回调函数
 */
export function getContainerStaticDetail(containerId: string, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.CONTAINER_DETAIL,
    data: { containerId },
    callbacks,
    timeout: TIMEOUT.LIST
  })
}

/**
 * 创建容器
 * @param containerConfig 容器配置
 * @param callbacks WebSocket回调函数
 */
export function createContainer(containerConfig: any, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.CONTAINER_CREATE,
    data: containerConfig,
    callbacks,
    timeout: TIMEOUT.CREATE
  })
}

/**
 * 获取镜像详情
 * @param imageId 镜像ID或镜像名称
 * @param callbacks WebSocket回调函数
 */
export function getImageDetail(imageId: string, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.IMAGE_DETAIL,
    data: { imageId },
    callbacks,
    timeout: TIMEOUT.LIST
  })
}

/**
 * 获取镜像列表
 * @param callbacks WebSocket回调函数
 */
export function getImageList(callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.IMAGE_LIST,
    callbacks,
    timeout: TIMEOUT.LIST
  })
}

/**
 * 更新容器信息（webUrl, iconUrl）
 * @param containerId 容器ID
 * @param callbacks WebSocket回调函数
 * @param webUrl WebUI地址
 * @param iconUrl 图标地址
 */
export function updateContainerInfo(
  containerId: string, 
  callbacks: WebSocketCallbacks, 
  webUrl?: string, 
  iconUrl?: string
) {
  return sendWebSocketMessage({
    type: MessageType.CONTAINER_UPDATE_INFO,
    data: { containerId, webUrl, iconUrl },
    callbacks,
    timeout: TIMEOUT.LIST
  })
} 