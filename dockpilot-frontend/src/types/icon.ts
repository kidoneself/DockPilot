export interface IconInfo {
  name: string        // 图标名称：Docker_A
  displayName: string // 显示名称：Docker
  type: string        // 类型：border-radius/circle/svg
  url: string         // 访问URL
  fileSize: number    // 文件大小
  extension: string   // 文件扩展名
}

export interface IconSelectData {
  value: string
  mode: 'url' | 'local'
  isLocal: boolean
  displayUrl: string
} 