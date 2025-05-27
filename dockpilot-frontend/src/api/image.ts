import { sendWebSocketMessage } from './websocket'
import type { WebSocketCallbacks } from './websocket/types'
import { MessageType } from './websocket/types'
import type { PullImageParams, PullImageCallbacks } from './model/imageModel'

// 定义超时时间常量（毫秒）
const TIMEOUT = {
  LIST: 5000,      // 列表请求超时时间较短
  PULL: 300000,    // 拉取镜像可能需要较长时间
  DELETE: 30000,   // 删除镜像操作超时时间
} as const

/**
 * 获取镜像列表
 * @param callbacks WebSocket回调函数
 */
export function getImageList(callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.IMAGE_LIST,
    data: {},
    callbacks: {
      onComplete: (data, taskId) => {
        console.log('WebSocket 返回的原始数据:', data)
        // 如果返回的是对象且包含 data 字段，使用 data 字段
        if (data && typeof data === 'object' && 'data' in data) {
          callbacks.onComplete?.(data.data, taskId)
        } else {
          callbacks.onComplete?.(data, taskId)
        }
      },
      onError: callbacks.onError,
      onProgress: callbacks.onProgress,
      onLog: callbacks.onLog
    },
    timeout: TIMEOUT.LIST
  })
}

/**
 * 拉取镜像
 * @param params 拉取参数
 * @param callbacks 回调函数
 */
export function pullImage(params: PullImageParams, callbacks: PullImageCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.PULL_IMAGE,
    data: params,
    callbacks: {
      onProgress: (progress: number, taskId: string) => {
        callbacks.onProgress?.(progress, taskId)
      },
      onLog: (log: string, taskId: string) => {
        callbacks.onLog?.(log, taskId)
      },
      onComplete: (data, taskId) => {
        console.log('WebSocket 返回的原始数据:', data)
        // 如果返回的是对象且包含 data 字段，使用 data 字段
        if (data && typeof data === 'object' && 'data' in data) {
          callbacks.onComplete?.(data.data)
        } else {
          callbacks.onComplete?.(data)
        }
      },
      onError: (error, taskId) => callbacks.onError?.(error, taskId)
    },
    timeout: TIMEOUT.PULL
  })
}

/**
 * 删除镜像
 * @param imageId 镜像ID或名称:标签
 * @param callbacks WebSocket回调函数
 */
export function deleteImage(imageId: string, callbacks: WebSocketCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.IMAGE_DELETE,
    data: { imageId },
    callbacks: {
      onComplete: (data, taskId) => {
        console.log('删除镜像成功:', data)
        callbacks.onComplete?.(data, taskId)
      },
      onError: callbacks.onError,
      onProgress: callbacks.onProgress,
      onLog: callbacks.onLog
    },
    timeout: TIMEOUT.DELETE
  })
} 