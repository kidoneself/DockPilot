/*
 * 日志管理相关API
 * 生成时间：2024-06-10 22:15:00
 */

import { request } from '@/utils/request';
import type { LogListResponse, LogQueryParams, LogCleanupParams } from '@/types/api/log';

/**
 * 获取日志列表
 * @param params 查询参数
 * @returns 日志列表响应
 */
export function getLogs(params?: LogQueryParams) {
  return request.get<LogListResponse>({
    url: '/logs',
    params: params || {},
  });
}

/**
 * 清理旧日志
 * @param params 清理参数
 * @returns 清理结果
 */
export function cleanupLogs(params: LogCleanupParams) {
  return request.delete({
    url: `/logs/cleanup?days=${params.days}`,
  });
} 
