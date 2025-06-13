import request from '@/utils/request'
import type { IconInfo } from '@/types/icon'

export interface IconListParams {
  search?: string
  type?: string
}

export const IconApi = {
  /**
   * 获取图标列表
   */
  getIconList(params?: IconListParams) {
    return request.get<IconInfo[]>('/icons/list', { params })
  },

  /**
   * 刷新图标缓存
   */
  refreshIconCache() {
    return request.post<string>('/icons/refresh')
  },

  /**
   * 获取图标URL
   */
  getIconUrl(iconName: string): string {
    return `/api/icons/${iconName}`
  },

  /**
   * 上传单个图标文件
   */
  uploadIcon(file: File, onProgress?: (progress: number) => void) {
    const formData = new FormData()
    formData.append('file', file)
    return request.upload<IconInfo>('/icons/upload', formData, {
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      }
    })
  },

  /**
   * 上传多个图标文件
   */
  uploadIcons(files: File[], onProgress?: (progress: number) => void) {
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
    return request.upload<IconInfo[]>('/icons/upload-multiple', formData, {
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      }
    })
  },

  /**
   * 上传ZIP压缩包图标（大文件专用）
   */
  uploadIconsFromZip(zipFile: File, onProgress?: (progress: number) => void) {
    const formData = new FormData()
    formData.append('file', zipFile)
    return request.upload<IconInfo[]>('/icons/upload-zip', formData, {
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      }
    })
  }
} 