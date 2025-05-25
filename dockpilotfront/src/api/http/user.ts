import request from '@/utils/request'

// 用户登录
export interface LoginRequest {
  username: string;
  password: string;
}

// 修改密码
export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

// 用户信息
export interface User {
  id: number;
  username: string;
  level: 'free' | 'pro';
  createdAt: string;
  updatedAt: string;
}

// 登录
export const login = (data: LoginRequest) => {
  return request.post<string>('/users/login', data)
}

// 修改密码
export const changePassword = (data: ChangePasswordRequest) => {
  return request.post('/users/change-password', data)
}

// 获取用户信息
export const getUserInfo = () => {
  return request.get<User>('/users/info')
}

// 获取免费版功能列表
export const getFreeFeatures = () => {
  return request.get<string[]>('/users/free-features')
}

// 获取专业版功能列表
export const getProFeatures = () => {
  return request.get<string[]>('/users/pro-features')
} 