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
 * 检查是否有新版本
 */
export const checkUpdate = () => {
  return request.get<UpdateInfo>('/api/update/check')
}

/**
 * 获取当前版本信息
 */
export const getCurrentVersion = () => {
  return request.get<VersionInfo>('/api/update/version')
}

/**
 * 执行热更新
 */
export const applyHotUpdate = (version?: string) => {
  const params = version ? { version } : {}
  return request.post<string>('/api/update/apply', null, { params })
}

/**
 * 获取更新进度
 */
export const getUpdateProgress = () => {
  return request.get<UpdateProgress>('/api/update/progress')
}

/**
 * 取消更新
 */
export const cancelUpdate = () => {
  return request.post<string>('/api/update/cancel')
}

/**
 * 设置自动检查更新
 */
export const setAutoCheck = (enabled: boolean) => {
  return request.post<void>('/api/update/auto-check', null, {
    params: { enabled }
  })
}

/**
 * 获取更新历史
 */
export const getUpdateHistory = () => {
  return request.get<UpdateHistory>('/api/update/history')
} 