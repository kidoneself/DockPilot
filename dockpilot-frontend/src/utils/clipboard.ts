/**
 * 复制文本到剪贴板 - 健壮版本
 * 支持多种复制方案，兼容各种浏览器环境
 */

export interface CopyResult {
  success: boolean
  method: 'clipboard' | 'execCommand' | 'manual'
  error?: string
}

/**
 * 复制文本到剪贴板
 * @param text 要复制的文本
 * @param options 选项配置
 * @returns Promise<CopyResult>
 */
export async function copyToClipboard(
  text: string, 
  options: {
    showMessage?: boolean
    messageApi?: any
  } = {}
): Promise<CopyResult> {
  const { showMessage = true, messageApi } = options

  // 检查文本是否为空
  if (!text || text.trim() === '') {
    const error = '复制内容不能为空'
    if (showMessage && messageApi) {
      messageApi.error(error)
    }
    return { success: false, method: 'manual', error }
  }

  // 方案1: 使用现代 navigator.clipboard API
  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(text)
      if (showMessage && messageApi) {
        messageApi.success('复制成功')
      }
      return { success: true, method: 'clipboard' }
    } catch (error) {
      console.warn('navigator.clipboard 复制失败:', error)
      // 继续尝试fallback方案
    }
  }

  // 方案2: 使用传统 document.execCommand fallback
  try {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.style.top = '-9999px'
    textarea.style.opacity = '0'
    textarea.style.pointerEvents = 'none'
    textarea.setAttribute('readonly', '')
    
    document.body.appendChild(textarea)
    
    // 选择文本
    textarea.select()
    textarea.setSelectionRange(0, text.length)
    
    // 执行复制
    const successful = document.execCommand('copy')
    document.body.removeChild(textarea)
    
    if (successful) {
      if (showMessage && messageApi) {
        messageApi.success('复制成功')
      }
      return { success: true, method: 'execCommand' }
    } else {
      throw new Error('execCommand copy failed')
    }
  } catch (error) {
    console.warn('execCommand 复制失败:', error)
  }

  // 方案3: 手动提示用户复制
  const errorMsg = '自动复制失败，请手动复制'
  if (showMessage && messageApi) {
    messageApi.warning(errorMsg)
    // 可以在这里显示一个包含文本的模态框让用户手动复制
  }
  
  return { 
    success: false, 
    method: 'manual', 
    error: errorMsg 
  }
}

/**
 * 检查复制功能是否可用
 */
export function isClipboardSupported(): boolean {
  // 检查现代API
  if (navigator.clipboard && window.isSecureContext) {
    return true
  }
  
  // 检查传统API
  if (document.queryCommandSupported && document.queryCommandSupported('copy')) {
    return true
  }
  
  return false
}

/**
 * 获取复制功能状态信息
 */
export function getClipboardStatus(): {
  supported: boolean
  method: string
  secure: boolean
} {
  const secure = window.isSecureContext
  
  if (navigator.clipboard && secure) {
    return {
      supported: true,
      method: 'navigator.clipboard (现代API)',
      secure: true
    }
  }
  
  if (document.queryCommandSupported && document.queryCommandSupported('copy')) {
    return {
      supported: true,
      method: 'document.execCommand (兼容API)',
      secure: false
    }
  }
  
  return {
    supported: false,
    method: '不支持自动复制',
    secure: false
  }
}

/**
 * 复制并显示详细结果（调试用）
 */
export async function copyToClipboardDebug(text: string, messageApi?: any): Promise<CopyResult> {
  const status = getClipboardStatus()
  console.log('📋 复制功能状态:', status)
  
  const result = await copyToClipboard(text, { showMessage: true, messageApi })
  console.log('📋 复制结果:', result)
  
  if (!result.success && messageApi) {
    messageApi.info(`复制环境: ${status.method}`)
  }
  
  return result
} 