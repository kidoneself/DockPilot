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
  uploadIcon(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<IconInfo>('/icons/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 上传多个图标文件
   */
  uploadIcons(files: File[]) {
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
    return request.post<IconInfo[]>('/icons/upload-multiple', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 上传ZIP压缩包图标
   */
  uploadIconsFromZip(zipFile: File) {
    const formData = new FormData()
    formData.append('file', zipFile)
    return request.post<IconInfo[]>('/icons/upload-zip', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
} 