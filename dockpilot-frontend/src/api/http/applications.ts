import request from '@/utils/request'
import { sendWebSocketMessage } from '@/api/websocket/websocketService'
import { MessageType } from '@/api/websocket/types'
import type { WebSocketCallbacks, DockerWebSocketMessage } from '@/api/websocket/types'

// 应用接口类型定义
export interface Application {
  id: number
  name: string
  description: string
  category: string
  iconUrl?: string
  icon?: string // 图标别名
  yamlContent: string
  fileHash: string
  envVars?: string
  createdAt: string
  updatedAt: string
  
  // 前端显示字段
  type: string
  deployCount: number
  services: number
  version: string
  author: string
}

// 应用保存请求
export interface ApplicationSaveRequest {
  name: string
  description?: string
  category?: string
  iconUrl?: string
  containerIds?: string[]
  yamlContent?: string
  envVars?: string
}

// 应用解析结果
export interface ApplicationParseResult {
  meta: {
    name: string
    description: string
    version: string
    author: string
    category: string
  }
  services: Array<{
    name: string
    image: string
    description: string
    configUrl: string
    ports: string[]
    volumes: string[]
  }>
  images: Array<{
    name: string
    tag: string
    fullName: string
    status: 'exists' | 'not_found' | 'unknown'
    size: string
  }>
  envVars: Array<{
    name: string
    defaultValue: string
    value: string
    description: string
    required: boolean
    sensitive: boolean
  }>
}

// YAML解析请求
export interface ParseRequest {
  yamlContent: string
}

/**
 * 获取应用列表
 */
export const getApplications = (params?: {
  category?: string
  keyword?: string
}) => {
  return request.get<Application[]>('/api/applications', { params })
}

/**
 * 获取应用详情
 */
export const getApplicationById = (id: number) => {
  return request.get<Application>(`/api/applications/${id}`)
}

/**
 * 保存应用
 */
export const saveApplication = (data: ApplicationSaveRequest) => {
  return request.post<Application>('/api/applications', data)
}

/**
 * 删除应用
 */
export const deleteApplication = (id: number) => {
  return request.delete(`/api/applications/${id}`)
}

/**
 * 分享应用 (获取YAML内容)
 */
export const shareApplication = (id: number) => {
  return request.get<string>(`/api/applications/${id}/share`)
}

/**
 * 获取分类列表
 */
export const getCategories = () => {
  return request.get<string[]>('/api/applications/categories')
}

/**
 * 解析YAML配置
 */
export const parseApplication = (data: ParseRequest) => {
  return request.post<ApplicationParseResult>('/api/applications/parse', data)
}

/**
 * 解析ZIP包并返回修改后的YAML内容
 */
export const parseZipPackage = (formData: FormData) => {
  return request.post<string>('/api/applications/parse-zip', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 应用部署相关接口

/**
 * 获取应用安装信息
 */
export const getInstallInfo = (id: number) => {
  return request.get<ApplicationInstallInfo>(`/api/applications/${id}/install-info`)
}

/**
 * 检查镜像状态
 */
export const checkImages = (imageNames: string[]) => {
  return request.post<ImageStatusInfo[]>('/api/applications/check-images', imageNames)
}

/**
 * 拉取镜像
 */
export const pullImage = (imageName: string) => {
  return request.post<string>('/api/applications/pull-image', { imageName })
}

/**
 * 部署应用
 */
export const deployApplication = (id: number, data: ApplicationDeployRequest) => {
  return request.post<ApplicationDeployResult>(`/api/applications/${id}/deploy`, data)
}

// 应用部署相关类型定义

export interface ApplicationInstallInfo {
  app: {
    id: number
    name: string
    description: string
    type: string
    icon: string
    deployCount: number
  }
  images: ImageStatusInfo[]
  envVars: EnvVarInfo[]
  services: ServiceInfo[]
}

export interface ImageStatusInfo {
  name: string
  size: string
  status: 'exists' | 'missing' | 'pulling' | 'success' | 'failed'
}

export interface EnvVarInfo {
  name: string
  description: string
  value: string | { value: string; description: string }
  defaultValue: string
  required: boolean
  sensitive: boolean
}

export interface ServiceInfo {
  name: string
  image: string
  configUrl: string
}

export interface ApplicationDeployRequest {
  appName: string
  envVars: Record<string, string | { value: string; description: string }>
  configs?: Record<string, any>
}

export interface ApplicationDeployResult {
  success: boolean
  message: string
  containerIds: string[]
  accessUrls: AccessUrl[]
  deployId: string
}

export interface AccessUrl {
  name: string
  url: string
  description: string
}

/**
 * 部署应用 - WebSocket版本
 */
export interface AppInstallParams {
  appId: number
  appName: string
  envVars: Record<string, string>
}

export interface AppInstallCallbacks {
  onProgress?: (progress: number, taskId: string) => void
  onLog?: (log: string, taskId: string) => void
  onComplete?: (result: ApplicationDeployResult) => void
  onError?: (error: string, taskId: string) => void
}

export function installApplicationWS(params: AppInstallParams, callbacks: AppInstallCallbacks) {
  return sendWebSocketMessage({
    type: MessageType.APP_INSTALL,
    data: params,
    callbacks: {
      onProgress: (progress: number, taskId: string) => {
        callbacks.onProgress?.(progress, taskId)
      },
      onLog: (log: string, taskId: string) => {
        callbacks.onLog?.(log, taskId)
      },
      onComplete: (data: DockerWebSocketMessage, taskId: string) => {
        console.log('应用安装完成，返回数据:', data)
        callbacks.onComplete?.(data.data as ApplicationDeployResult)
      },
      onError: (error: string, taskId: string) => callbacks.onError?.(error, taskId)
    },
    timeout: 600000 // 10分钟超时
  })
} 