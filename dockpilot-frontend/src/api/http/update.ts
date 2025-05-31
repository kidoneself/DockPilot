// 前端更新API
import request from '@/utils/request'

// 更新信息接口
export interface UpdateInfo {
  currentVersion: string
  latestVersion: string
  hasUpdate: boolean
  releaseNotes?: string
  releaseTime?: string
  downloadUrl?: string
  fileSize?: number
  forceUpdate: boolean
  lastCheckTime: string
  status: string // 'available' | 'up-to-date' | 'downloading' | 'applying' | 'completed' | 'failed'
  progress?: number
  errorMessage?: string
}

// 版本信息接口
export interface VersionInfo {
  currentVersion: string
  buildTime: string
  updateMethod: string
  frontendPath: string
  backendJar: string
}

// 下载状态接口
export interface DownloadStatus {
  status: string // 'idle' | 'downloading' | 'verifying' | 'completed' | 'failed' | 'cancelled'
  progress: number // 0-100
  message: string
  version: string
  timestamp: string
}

// 更新进度接口
export interface UpdateProgress {
  status: string
  progress: number
  message: string
  targetVersion?: string
  isUpdating: boolean
  timestamp: string
  error?: string
}

// 更新历史接口
export interface UpdateHistory {
  currentVersion: string
  lastCheckTime: string
  autoCheckEnabled: boolean
  updateMethod: string
}

/**
 * 健康检查
 */
export const healthCheck = () => {
  return request.get<{ status: string; service: string; timestamp: string }>('/update/health')
}

/**
 * 检查是否有新版本
 */
export const checkUpdate = () => {
  return request.get<UpdateInfo>('/update/check')
}

/**
 * 获取当前版本信息
 */
export const getCurrentVersion = () => {
  return request.get<VersionInfo>('/update/version')
}

/**
 * 开始下载新版本
 */
export const startDownload = (version?: string) => {
  const params = version ? { version } : {}
  return request.post<string>('/update/download', null, { params })
}

/**
 * 获取下载状态
 */
export const getDownloadStatus = () => {
  return request.get<DownloadStatus>('/update/download/status')
}

/**
 * 确认重启更新
 */
export const confirmRestart = () => {
  return request.post<string>('/update/restart')
}

/**
 * 取消下载
 */
export const cancelDownload = () => {
  return request.post<string>('/update/download/cancel')
}

/**
 * 获取更新进度
 */
export const getUpdateProgress = () => {
  return request.get<UpdateProgress>('/update/progress')
}

/**
 * 取消更新
 */
export const cancelUpdate = () => {
  return request.post<string>('/update/cancel')
}

/**
 * 设置自动检查更新
 */
export const setAutoCheck = (enabled: boolean) => {
  return request.post<void>('/update/auto-check', null, {
    params: { enabled }
  })
}

/**
 * 获取更新历史
 */
export const getUpdateHistory = () => {
  return request.get<UpdateHistory>('/update/history')
} 