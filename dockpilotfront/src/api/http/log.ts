import request from '@/utils/request'

// 日志记录
export interface Log {
  id: number;
  type: string;
  level: string;
  content: string;
  createTime: string;
}

// 获取日志列表
export const getLogs = (params: {
  type?: string;
  level?: string;
  limit?: number;
}) => {
  return request.get<Log[]>('/logs', { params })
}

// 清理旧日志
export const cleanupOldLogs = (days: number) => {
  return request.delete('/logs/cleanup', { params: { days } })
} 