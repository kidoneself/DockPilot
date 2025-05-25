import request from '@/utils/request'

// 文件节点接口
export interface FileNode {
  name: string
  path: string
  directory: boolean
  children?: FileNode[]
}

// 获取文件树列表
export function getFileTree(path: string = '/', maxFilesPerDir: number = 100): Promise<FileNode[]> {
  return request.get('/api/file-tree/list', {
    params: {
      path,
      maxFilesPerDir
    }
  })
} 