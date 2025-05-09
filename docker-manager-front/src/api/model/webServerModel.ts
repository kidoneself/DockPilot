// Web 服务器基础信息
export interface WebServer {
  id: string;
  name: string;
  icon: string;
  internalUrl: string;
  externalUrl: string;
  description: string;
  category: string;
  itemSort: number;
  createdAt: string;
  updatedAt: string;
}

// API 响应类型
export interface ApiResponse<T> {
  code: number;
  data: T;
  message: string;
}

// 创建 Web 服务器的请求参数
export interface CreateWebServerRequest {
  name: string;
  icon: string;
  internalUrl: string;
  externalUrl: string;
  description: string;
  category: string;
  itemSort: number;
}

// 更新 Web 服务器的请求参数
export interface UpdateWebServerRequest extends CreateWebServerRequest {
  id: string;
}

// 更新排序的请求参数
export interface UpdateSortRequest {
  id: string;
  itemSort: number;
}

// 批量更新排序的请求参数
export interface BatchUpdateSortRequest {
  id: string;
  itemSort: number;
}[] 