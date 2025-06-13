import request from '@/utils/request'

/**
 * 书签导入相关API
 */

// 解析书签文件
export const parseBookmarkFile = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  
  return request.post('/api/bookmark/parse', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 导入选中的书签
export const importSelectedBookmarks = (bookmarks: any[]) => {
  return request.post('/api/bookmark/import', {
    bookmarks
  })
} 