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

// 分类请求接口
export interface CategoryDTO {
  id?: number
  name: string
  sortOrder?: number
}

// 获取所有分类（包含应用数量）- 使用WebServerController，只返回有应用的分类
export const getCategories = () => {
  return request.get<CategoryVO[]>('/web-servers/categories')
}

// 获取所有分类（包括空分类）- 使用CategoryController，用于分类管理界面
export const getAllCategoriesForManage = () => {
  return request.get<CategoryVO[]>('/categories')
}

// 获取分类详情 - 使用CategoryController
export const getCategoryById = (id: number) => {
  return request.get<CategoryVO>(`/categories/${id}`)
}

// 创建分类 - 使用CategoryController
export const createCategory = (data: CategoryDTO) => {
  return request.post<number>('/categories', data)
}

// 更新分类 - 使用CategoryController
export const updateCategory = (id: number, data: CategoryDTO) => {
  return request.put<void>(`/categories/${id}`, data)
}

// 删除分类 - 使用CategoryController
export const deleteCategory = (id: number) => {
  return request.delete<void>(`/categories/${id}`)
}

// 更新分类排序 - 使用CategoryController
export const updateCategorySort = (id: number, sortOrder: number) => {
  return request.put<void>(`/categories/${id}/sort`, null, {
    params: { sortOrder }
  })
}

// 批量更新分类排序 - 使用CategoryController
export const batchUpdateCategorySort = (categories: CategoryDTO[]) => {
  return request.put<void>('/categories/batch-sort', categories)
} 