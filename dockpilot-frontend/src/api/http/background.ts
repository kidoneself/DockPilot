import { getSetting, setSetting } from './system'

// 配置键名
const BACKGROUND_URL_KEY = 'current_background_url'

/**
 * 获取当前背景URL
 */
export const getCurrentBackground = async (): Promise<string> => {
  try {
    const backgroundUrl = await getSetting(BACKGROUND_URL_KEY)
    
    if (!backgroundUrl) return ''
    
    // 兼容处理：如果是完整URL，转换为相对路径
    if (backgroundUrl.startsWith('http')) {
      // 提取相对路径部分，如 http://localhost:8080/upload/image/img_xxx.jpg -> /upload/image/img_xxx.jpg
      const url = new URL(backgroundUrl)
      return url.pathname
    }
    
    // 如果已经是相对路径，直接返回
    return backgroundUrl
  } catch (error) {
    console.warn('获取背景配置失败，使用默认配置:', error)
    return ''
  }
}

/**
 * 设置当前背景URL
 */
export const setCurrentBackground = async (backgroundUrl: string): Promise<void> => {
  try {
    let relativePath = backgroundUrl
    
    // 确保存储的总是相对路径
    if (backgroundUrl.startsWith('http')) {
      // 如果传入的是完整URL，转换为相对路径存储
      const url = new URL(backgroundUrl)
      relativePath = url.pathname
    }
    
    await setSetting({
      key: BACKGROUND_URL_KEY,
      value: relativePath
    })
    console.log('✅ 背景已保存（相对路径）:', relativePath)
  } catch (error) {
    console.error('❌ 保存背景失败:', error)
    throw error
  }
} 