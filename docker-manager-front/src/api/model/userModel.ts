/**
 * 用户等级
 */
export type UserLevel = 'free' | 'pro';

/**
 * 登录请求参数
 */
export interface LoginRequest {
  /** 用户名 */
  username: string;
  /** 密码 */
  password: string;
}

/**
 * 用户信息
 */
export interface UserInfo {
  /** 用户ID */
  id: number;
  /** 用户名 */
  username: string;
  /** 用户等级 */
  level: UserLevel;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
} 