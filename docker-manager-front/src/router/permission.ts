import { useUserStore } from '@/store';
import { MessagePlugin } from 'tdesign-vue-next';
import type { RouteLocationNormalized } from 'vue-router';
import router from './index';

const whiteList = ['/login', '/loginRedirect', '/'];

router.beforeEach(async (to: RouteLocationNormalized, from: RouteLocationNormalized, next) => {
  const userStore = useUserStore();
  const token = userStore.token;

  if (token) {
    if (to.path === '/login') {
      next({ path: '/' });
    } else {
      if (!userStore.userInfo.id) {
        try {
          await userStore.getUserInfo();
        } catch (error) {
          userStore.logout();
          MessagePlugin.error('获取用户信息失败，请重新登录');
          next(`/login?redirect=${to.path}`);
          return;
        }
      }

      // 检查是否需要 PRO 权限
      if (to.meta.requiresPro && !userStore.isPro) {
        MessagePlugin.error('需要 PRO 订阅才能访问此功能');
        next(false);
        return;
      }

      next();
    }
  } else {
    if (whiteList.includes(to.path)) {
      next();
    } else {
      next(`/login?redirect=${to.path}`);
    }
  }
}); 