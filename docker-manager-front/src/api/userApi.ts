import { request } from '@/utils/request';
import type { UserInfo } from './model/userModel';
import { ApiResponse } from './model/commonModel';

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

/**
 * 用户登录
 * @param data 登录请求参数
 */
export const login = (data: { username: string; password: string }) => {
  return request.post<ApiResponse<string>>({
    url: '/users/login',
    data,
  });
};

/**
 * 获取用户信息
 */
export const getUserInfo = () => {
  return request.get<ApiResponse<UserInfo>>({
    url: '/users/info',
  });
};

/**
 * 修改密码
 * @param data 修改密码请求参数
 */
export const changePassword = (data: { oldPassword: string; newPassword: string }) => {
  return request.post<void>({
    url: '/users/change-password',
    data,
  });
};

export const changePasswordApi = (data: ChangePasswordRequest) => {
  return request.post<ApiResponse<void>>({
    url: '/users/change-password',
    data,
  });
}; 