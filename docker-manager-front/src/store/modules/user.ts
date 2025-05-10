import { defineStore } from 'pinia';
import { usePermissionStore } from '@/store';
import { login, getUserInfo } from '@/api/userApi';
import type { UserInfo } from '@/api/model/userModel';
import { ws as wsClient } from '@/utils/websocket';

const InitUserInfo: UserInfo = {
  id: 0,
  username: '',
  level: 'free',
  createdAt: '',
  updatedAt: '',
};

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '', // JWT token
    userInfo: { ...InitUserInfo },
  }),
  getters: {
    isPro: (state) => state.userInfo?.level === 'pro',
  },
  actions: {
    async login(userInfo: { username: string; password: string }) {
      try {
        const response = await login(userInfo);
        if (response.code !== 0) {
          throw new Error(response.message || '登录失败');
        }
        this.token = response.data;
        // 登录成功后获取用户信息
        await this.getUserInfo();
        // 初始化WebSocket连接
        try {
          await wsClient.connect();
        } catch (error) {
          console.error('WebSocket 连接失败:', error);
        }
        return response.data;
      } catch (error) {
        console.error('登录或获取用户信息失败:', error);
        throw error;
      }
    },
    async getUserInfo() {
      try {
        const response = await getUserInfo();
        if (!response.data) {
          throw new Error('获取用户信息失败');
        }
        this.userInfo = response.data;
        // 获取用户信息后初始化路由
        const permissionStore = usePermissionStore();
        permissionStore.initRoutes();
        return response.data;
      } catch (error) {
        console.error('获取用户信息失败:', error);
        throw error;
      }
    },
    async logout() {
      this.token = '';
      this.userInfo = { ...InitUserInfo };
      // 断开WebSocket连接
      wsClient.disconnect();
    },
  },
  persist: {
    key: 'user',
    paths: ['token'],
  },
});
