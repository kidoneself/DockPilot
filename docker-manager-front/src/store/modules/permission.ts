import { defineStore } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';
import router, { homepageRouterList, fixedRouterList } from '@/router';
import { store } from '@/store';

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    whiteListRouters: ['/login', '/'] as string[],
    routers: [] as RouteRecordRaw[],
  }),
  actions: {
    initRoutes() {
      // 只使用非登录页面的路由
      this.routers = [...homepageRouterList, ...fixedRouterList] as RouteRecordRaw[];
    },
  },
});

export function getPermissionStore() {
  return usePermissionStore(store);
}
