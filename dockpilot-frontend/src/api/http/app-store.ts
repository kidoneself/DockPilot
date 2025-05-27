import request from '@/utils/request'

// 服务模板
export interface ServiceTemplate {
  name: string;
  image: string;
  env: Record<string, string>;
  ports: Record<string, string>;
  volumes: Record<string, string>;
  restartPolicy: string;
  cmd: string[];
  networkMode: string;
  privileged: boolean;
}

// 服务配置
export interface ServiceConfig {
  id: string;
  name: string;
  template: ServiceTemplate;
}

// 参数配置
export interface ParameterConfig {
  key: string;
  name: string;
  value: string;
}

// 应用商店应用
export interface AppStoreAppDTO {
  id: string;
  name: string;
  category: string;
  version: string;
  description: string;
  iconUrl: string;
  createdAt: string;
  updatedAt: string;
  services: ServiceConfig[];
  parameters: ParameterConfig[];
}

// 分页结果
export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// 获取应用列表
export const getAppList = (params: {
  page?: number;
  pageSize?: number;
  category?: string;
}) => {
  return request.get<PageResult<AppStoreAppDTO>>('/api/app-store/apps', { params })
}

// 获取应用详情
export const getAppDetail = (id: string) => {
  return request.get<AppStoreAppDTO>(`/api/app-store/apps/${id}`)
} 