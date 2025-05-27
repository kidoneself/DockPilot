import request from '@/utils/request'

// 分类信息接口
export interface CategoryVO {
  id: number
  name: string
  sortOrder: number
  appCount: number
  createdAt: string
  updatedAt: string
}

// 应用信息接口
export interface WebServerVO {
  id: string
  name: string
  icon?: string
  internalUrl?: string
  externalUrl?: string
  description?: string
  categoryId: number
  categoryName: string
  itemSort: number
  bgColor?: string
  cardType?: string
  iconType?: string
  openType?: string
  createdAt: string
  updatedAt: string
}

// 创建应用请求接口
export interface CreateWebServerRequest {
  name: string
  icon?: string
  internalUrl?: string
  externalUrl?: string
  description?: string
  categoryId: number
  itemSort: number
  bgColor?: string
  cardType?: string
  iconType?: string
  openType?: string
}

// 更新应用请求接口
export interface UpdateWebServerRequest extends CreateWebServerRequest {
  id: string
}

// 获取所有分类（包含应用数量）
export const getCategories = () => {
  return request.get<CategoryVO[]>('/web-servers/categories')
}

// 获取分类详情
export const getCategoryById = (id: number) => {
  return request.get<CategoryVO>(`/web-servers/categories/${id}`)
}

// 获取所有应用（包含分类信息）
export const getWebServers = () => {
  return request.get<WebServerVO[]>('/web-servers')
}

// 根据分类ID获取应用
export const getWebServersByCategory = (categoryId: number) => {
  return request.get<WebServerVO[]>(`/web-servers/category/${categoryId}`)
}

// 根据分类名称获取应用
export const getWebServersByCategoryName = (categoryName: string) => {
  return request.get<WebServerVO[]>(`/web-servers/category/name/${encodeURIComponent(categoryName)}`)
}

// 获取应用详情
export const getWebServerById = (id: string) => {
  return request.get<WebServerVO>(`/web-servers/${id}`)
}

// 创建应用
export const createWebServer = (data: CreateWebServerRequest) => {
  return request.post<string>('/web-servers', data)
}

// 更新应用
export const updateWebServer = (id: string, data: UpdateWebServerRequest) => {
  return request.put<void>(`/web-servers/${id}`, data)
}

// 删除应用
export const deleteWebServer = (id: string) => {
  return request.delete<void>(`/web-servers/${id}`)
}

// 更新应用排序
export const updateWebServerSort = (id: string, itemSort: number) => {
  return request.put<void>(`/web-servers/${id}/sort`, null, {
    params: { itemSort }
  })
}

// 批量更新应用排序
export const batchUpdateWebServerSort = (servers: UpdateWebServerRequest[]) => {
  return request.put<void>('/web-servers/batch-sort', servers)
} 