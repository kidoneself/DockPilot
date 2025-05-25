import request from '@/utils/request'

// 文件上传响应
export interface FileUploadResponse {
  filename: string;
  originalName: string;
  url?: string;  // 可选字段，由前端生成
  size: string;
}

// 上传进度回调类型
export type UploadProgressCallback = (progress: number) => void;

/**
 * 上传图片文件
 * @param file 图片文件
 * @param onProgress 上传进度回调（可选）
 * @returns Promise<FileUploadResponse>
 */
export const uploadImage = (
  file: File, 
  onProgress?: UploadProgressCallback
): Promise<FileUploadResponse> => {
  const formData = new FormData()
  formData.append('file', file)

  return request.post<FileUploadResponse>('/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    // 上传进度监听
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(progress)
      }
    },
    // 上传文件时可能需要更长的超时时间
    timeout: 60000,
  })
}

/**
 * 删除文件
 * @param filename 文件名
 * @returns Promise<void>
 */
export const deleteFile = async (filename: string): Promise<void> => {
  await request.delete(`/upload/file/${filename}`)
}

/**
 * 获取图片URL（返回相对路径，浏览器自动补全域名）
 * @param filename 文件名
 * @returns 图片访问相对路径
 */
export const getImageUrl = (filename: string): string => {
  // 返回相对路径，浏览器会自动补全当前域名
  // 这样在任何环境下都能正确访问（nginx代理、CDN等）
  return `/upload/image/${filename}`
}

/**
 * 获取所有上传的图片
 */
export interface ImageInfo {
  filename: string
  name: string
  size?: string
  lastModified?: string
}

export const getAllImages = async (): Promise<ImageInfo[]> => {
  const response = await request.get('/upload/images')
  return response || []
}

/**
 * 从URL下载图片
 */
export interface DownloadImageRequest {
  url: string
  name?: string  // 可选的自定义名称
}

export const downloadImageFromUrl = async (
  requestData: DownloadImageRequest
): Promise<FileUploadResponse> => {
  const response = await request.post<FileUploadResponse>('/upload/image/download', requestData)
  return response
}

/**
 * 验证文件是否为图片（更新支持SVG）
 * @param file 文件对象
 * @returns boolean
 */
export const isImageFile = (file: File): boolean => {
  const imageTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml']
  return imageTypes.includes(file.type)
}

/**
 * 验证文件大小
 * @param file 文件对象
 * @param maxSizeInMB 最大大小（MB）
 * @returns boolean
 */
export const isValidFileSize = (file: File, maxSizeInMB: number = 10): boolean => {
  const maxSizeInBytes = maxSizeInMB * 1024 * 1024
  return file.size <= maxSizeInBytes
} 