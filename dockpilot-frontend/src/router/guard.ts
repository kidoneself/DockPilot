import type { Router } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/auth'

// 白名单路由
const whiteList = ['/login']

export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore()
    const hasToken = getToken()

    if (hasToken) {
      if (to.path === '/login') {
        // 已登录且要跳转的页面是登录页
        next({ path: '/' })
      } else {
        // 已登录访问其他页面
        if (!userStore.isLoggedIn) {
          try {
            // 获取用户信息
            await userStore.fetchUserInfo()
            next()
          } catch {
            // 获取用户信息失败，清除token并跳转登录页
            userStore.clearUserInfo()
            next(`/login?redirect=${to.path}`)
          }
        } else {
          next()
        }
      }
    } else {
      // 未登录
      if (whiteList.includes(to.path)) {
        // 在免登录白名单中，直接进入
        next()
      } else {
        // 其他没有访问权限的页面将被重定向到登录页面
        next(`/login?redirect=${to.path}`)
      }
    }
  })
} 