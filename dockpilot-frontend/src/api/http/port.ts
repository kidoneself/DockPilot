import request from '@/utils/request'

/**
 * 端口检测相关API
 */

/**
 * 检测单个端口是否可用
 * @param port 端口号
 * @returns 是否可用
 */
export const checkPort = (port: number): Promise<boolean> => {
  return request.get(`/api/port/check/${port}`)
}

/**
 * 批量检测端口
 * @param ports 端口数组
 * @returns 端口可用性映射
 */
export const checkMultiplePorts = (ports: number[]): Promise<Record<number, boolean>> => {
  return request.post('/api/port/check-batch', ports)
}

/**
 * 查找可用端口
 * @param startPort 起始端口
 * @param endPort 结束端口
 * @param count 需要的数量
 * @returns 可用端口数组
 */
export const findAvailablePorts = (
  startPort: number, 
  endPort: number, 
  count: number = 1
): Promise<number[]> => {
  return request.get('/api/port/find-available', {
    params: { startPort, endPort, count }
  })
}

/**
 * 检测常用端口
 * @returns 常用端口检测结果
 */
export const checkCommonPorts = (): Promise<{
  ports: Record<number, boolean>
  availableCount: number
  totalCount: number
}> => {
  return request.get('/api/port/check-common')
} 