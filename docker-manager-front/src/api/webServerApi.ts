import { request } from '@/utils/request';
import { WebServer, ApiResponse, CreateWebServerRequest } from './model/webServerModel';

/**
 * 获取所有 Web 服务器列表
 * @returns Promise<ApiResponse<WebServer[]>>
 */
export const getWebServerListApi = () => {
  return request.get<ApiResponse<WebServer[]>>({
    url: '/web-servers',
    params: {}
  });
};

/**
 * 创建 Web 服务器
 * @param data 创建参数
 * @returns Promise<ApiResponse<WebServer>>
 */
export const createWebServerApi = (data: CreateWebServerRequest) => {
  return request.post<ApiResponse<WebServer>>({
    url: '/web-servers',
    data
  });
};

/**
 * 更新 Web 服务器
 * @param id Web 服务器ID
 * @param data 更新参数
 * @returns Promise<ApiResponse<void>>
 */
export const updateWebServerApi = (id: string, data: CreateWebServerRequest) => {
  return request.put<ApiResponse<void>>({
    url: `/web-servers/${id}`,
    data
  });
};

/**
 * 删除 Web 服务器
 * @param id Web 服务器ID
 * @returns Promise<ApiResponse<void>>
 */
export const deleteWebServerApi = (id: string) => {
  return request.delete<ApiResponse<void>>({
    url: `/web-servers/${id}`
  });
}; 