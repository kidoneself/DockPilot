/**
 * 前端书签解析工具 - 使用 node-bookmarks-parser
 */
import parse from 'node-bookmarks-parser'

export interface BookmarkItem {
  title: string
  url: string
  groupName: string
  iconBase64?: string
  selected: boolean
}

export interface BookmarkGroup {
  name: string
  items: BookmarkItem[]
}

export interface BookmarkParseResult {
  groups: BookmarkGroup[]
  totalCount: number
  message: string
}



/**
 * 解析书签文件内容
 */
export function parseBookmarkContent(htmlContent: string): Promise<BookmarkParseResult> {
  return new Promise((resolve, reject) => {
    try {
      console.log('🔍 开始解析书签文件，内容长度:', htmlContent.length)
      
      // 使用 node-bookmarks-parser 解析
      const bookmarks = parse(htmlContent) as any[]
      console.log('📋 解析结果:', bookmarks)
      
      if (!bookmarks || !Array.isArray(bookmarks)) {
        throw new Error('解析结果格式无效')
      }
      
      // 转换为分组格式
      const groups = convertToGroups(bookmarks)
      const totalCount = groups.reduce((sum, group) => sum + group.items.length, 0)
      
      console.log('✅ 解析完成，共找到', groups.length, '个分组，', totalCount, '个书签')
      console.log('📁 分组详情:', groups)
      
      resolve({
        groups,
        totalCount,
        message: `成功解析 ${totalCount} 个书签，分布在 ${groups.length} 个分组中`
      })
      
    } catch (error: any) {
      console.error('❌ 解析过程出错:', error)
      reject(new Error('解析过程出错: ' + error.message))
    }
  })
}

/**
 * 转换解析结果为分组格式
 */
function convertToGroups(bookmarks: any[]): BookmarkGroup[] {
  const groups: BookmarkGroup[] = []
  
  function processBookmarks(items: any[], parentPath: string = '') {
    for (const item of items) {
      if (item.type === 'folder') {
        // 处理文件夹
        const folderName = item.title || '未命名文件夹'
        const fullFolderName = parentPath ? `${parentPath}/${folderName}` : folderName
        
        console.log('📂 处理文件夹:', fullFolderName)
        
        if (item.children && item.children.length > 0) {
          processBookmarks(item.children, fullFolderName)
        }
      } else if (item.type === 'bookmark') {
        // 处理书签
        if (!item.url || !item.title) {
          console.log('⚠️ 跳过无效书签:', item)
          continue
        }
        
        if (!isValidUrl(item.url)) {
          console.log('⚠️ 跳过无效URL:', item.title, '->', item.url)
          continue
        }
        
        const fullGroupName = parentPath || '其他书签'
        // 只取最后一级作为显示名称
        const displayGroupName = getLastPathSegment(fullGroupName)
        
        let group = groups.find(g => g.name === displayGroupName)
        
        if (!group) {
          group = {
            name: displayGroupName,
            items: []
          }
          groups.push(group)
          console.log('📁 创建新分组:', displayGroupName, '(完整路径:', fullGroupName, ')')
        }
        
        group.items.push({
          title: item.title,
          url: item.url,
          groupName: displayGroupName,
          iconBase64: item.icon || undefined,
          selected: true
        })
        
        console.log('🔗 添加书签:', item.title, '->', item.url)
      }
    }
  }
  
  processBookmarks(bookmarks)
  
  // 按分组名称排序 (基于最后一级名称)
  groups.sort((a, b) => {
    // 常见分组优先 (基于最后一级名称)
    const priority: { [key: string]: number } = {
      '书签栏': 1,
      '收藏夹栏': 1,
      'Bookmarks Bar': 2,
      'Favorites Bar': 2,
      'Bookmarks Toolbar': 2,
      '其他书签': 999,
      'Other Bookmarks': 999,
      // 一些常见的最后一级名称
      '自己的服务': 3,
      'pt': 4,
      '管理': 5,
      '工具': 6
    }
    
    const aScore = priority[a.name] || 500
    const bScore = priority[b.name] || 500
    
    if (aScore !== bScore) {
      return aScore - bScore
    }
    
    return a.name.localeCompare(b.name)
  })
  
  return groups
}

/**
 * 提取路径的最后一级
 */
function getLastPathSegment(path: string): string {
  if (!path) return '其他书签'
  
  // 分割路径并取最后一部分
  const segments = path.split('/').filter(segment => segment.trim())
  
  if (segments.length === 0) return '其他书签'
  
  return segments[segments.length - 1].trim()
}

/**
 * 验证URL是否有效
 */
function isValidUrl(url: string): boolean {
  return url.startsWith('http://') || 
         url.startsWith('https://') || 
         url.startsWith('ftp://') ||
         url.startsWith('chrome://') ||
         url.startsWith('edge://') ||
         url.startsWith('about:')
}

/**
 * 读取文件内容
 */
export function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    
    reader.onload = (e) => {
      const content = e.target?.result as string
      resolve(content)
    }
    
    reader.onerror = () => {
      reject(new Error('读取文件失败'))
    }
    
    reader.readAsText(file, 'UTF-8')
  })
} 