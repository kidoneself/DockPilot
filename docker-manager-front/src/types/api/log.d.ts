/*
 * 日志相关类型定义
 * 生成时间：2024-06-10 22:10:00
 */

export interface Log {
  id: number;
  type: string;      // 日志类型：OPERATION-操作日志，SYSTEM-系统日志
  level: string;     // 日志级别：INFO, ERROR, WARN
  content: string;   // 日志内容
  createTime: string;  // 创建时间
}

export interface LogListResponse {
  code: number;
  data: Log[];
  message: string;
}

export interface LogQueryParams {
  type?: string;    // 日志类型筛选
  level?: string;   // 日志级别筛选
  limit?: number;   // 限制返回数量
}

export interface LogCleanupParams {
  days: number;     // 清理指定天数前的日志
} 