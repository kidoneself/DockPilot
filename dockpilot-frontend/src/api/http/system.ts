import request from '@/utils/request'

// 系统配置
export interface SystemSetting {
  key: string;
  value: string;
}

// 路由配置
export interface Route {
  path: string;
  name: string;
  component: string;
  redirect?: string;
  meta?: Record<string, any>;
}

// 获取系统配置
export const getSetting = (key: string) => {
  return request.get<string>('/system/settings', { 
    params: { key },
    timeout: 10000  // 10秒超时，适合服务器环境
  })
}

// 更新系统配置
export const updateSetting = (data: SystemSetting) => {
  return request.put('/system/settings', data)
}

// 设置系统配置
export const setSetting = (data: SystemSetting) => {
  return request.post('/system/settings', data, {
    timeout: 10000  // 10秒超时
  })
}

// 删除系统配置
export const deleteSetting = (key: string) => {
  return request.delete(`/system/settings/${key}`)
}

// 测试代理延迟
export const testProxyLatency = () => {
  return request.get<Record<string, number>>('/system/proxy/test', {
    timeout: 15000  // 15秒超时，代理测试可能较慢
  })
}

// 测试指定代理URL的延迟
export const testProxyLatencyWithUrl = (proxyUrl: string) => {
  return request.post<Record<string, number>>('/system/proxy/test', null, {
    params: { proxyUrl },
    timeout: 15000  // 15秒超时，代理测试可能较慢
  })
}

// 获取授权菜单
export const getMenu = () => {
  return request.get<Route[]>('/system/getMenu')
}

// 获取网站 Logo
export const getFavicon = (url: string) => {
  return request.get<string>('/system/favicon', { params: { url } })
}

// 🎯 镜像检查间隔相关API (使用通用的key-value配置)

// 获取镜像检查间隔配置
export const getImageCheckInterval = () => {
  return getSetting('imageCheckInterval')
}

// 更新镜像检查间隔配置
export const updateImageCheckInterval = (intervalMinutes: number) => {
  return setSetting({ 
    key: 'imageCheckInterval', 
    value: intervalMinutes.toString() 
  })
} 