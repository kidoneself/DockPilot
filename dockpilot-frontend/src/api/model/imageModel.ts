/**
 * 镜像相关的类型定义
 */

import type { DockerWebSocketMessage } from '../websocket/types'

/**
 * 镜像拉取状态
 */
export interface PullStatus {
  status: 'pulling' | 'success' | 'failed' | 'idle'
  percentage: number
  message: string
  error?: string
  start_time?: string
  end_time?: string
}

/**
 * 镜像状态信息
 */
export interface ImageStatus {
  id: number
  name: string
  tag: string
  localCreateTime?: string
  remoteCreateTime?: string
  needUpdate: boolean
  lastChecked?: string
  createdAt?: string
  updatedAt?: string
  imageId?: string
  pulling: boolean
  progress?: string // JSON格式的拉取进度信息
}

/**
 * 镜像信息
 */
export interface Image {
  id: string
  name: string
  tag: string
  size: number
  created: number
  needUpdate: boolean
  statusId: number
  // 扩展字段
  pullStatus?: PullStatus
  canRetry?: boolean
  isRealImage?: boolean // 是否是Docker中真实存在的镜像
}

/**
 * 镜像列表响应
 */
export interface ImageListResponse {
  data: Image[]
  progress: number
  taskId: string
  timestamp: number
  type: string
}

/**
 * 拉取镜像参数
 */
export interface PullImageParams {
  imageName: string
}

/**
 * 拉取镜像回调
 */
export interface PullImageCallbacks {
  onProgress?: (progress: number, taskId: string) => void
  onLog?: (log: string, taskId: string) => void
  onComplete?: (data: DockerWebSocketMessage) => void
  onError?: (error: string, taskId: string) => void
} 