import {
  createRouter,
  createWebHistory,
  RouteRecordRaw,
  NavigationGuardNext,
  RouteLocationNormalized
} from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { setupRouterGuard } from './guard'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'UserLogin',
    component: () => import('@/views/user/UserLogin.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/navigation',
    name: 'NavigationHome',
    component: () => import('@/views/navigation/NavigationHome.vue'),
    meta: {
      title: 'DockPilot - 智能容器管理平台',
      requiresAuth: false
    }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/navigation',
    children: [
      {
        path: 'containers',
        name: 'containers',
        component: () => import('../views/containers/ContainerList.vue'),
        meta: {
          title: '容器管理',
          icon: 'cube-outline'
        }
      },
      {
        path: 'containers/:id',
        name: 'container-detail',
        component: () => import('../views/containers/ContainerDetail.vue'),
        meta: {
          title: '容器详情',
          icon: 'cube-outline',
          hidden: true
        }
      },
      {
        path: 'containers/:id/edit',
        name: 'ContainerEdit',
        component: () => import('@/views/containers/ContainerEdit.vue'),
        meta: {
          title: '编辑容器',
          hidden: true
        }
      },
      {
        path: 'containers/create',
        name: 'ContainerCreate',
        component: () => import('@/views/containers/ContainerCreate.vue'),
        meta: {
          title: '创建容器',
          hidden: true
        }
      },
      {
        path: 'images',
        name: 'Images',
        component: () => import('../views/images/ImageList.vue'),
        meta: {
          title: '镜像管理',
          icon: 'ImageOutline'
        }
      },
      {
        path: 'networks',
        name: 'Networks',
        component: () => import('../views/networks/NetworkList.vue'),
        meta: {
          title: '网络管理',
          icon: 'GlobeOutline'
        }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('../views/settings/SystemSettings.vue'),
        meta: {
          title: '系统设置',
          icon: 'SettingsOutline'
        }
      },
      {
        path: 'appcenter',
        name: 'AppCenter',
        component: () => import('../views/appcenter/AppCenter.vue'),
        meta: {
          title: '应用中心',
          icon: 'AppsOutline'
        }
      },
      {
        path: 'appcenter/install',
        name: 'AppInstall',
        component: () => import('../views/appcenter/AppInstall.vue'),
        meta: {
          title: '安装应用',
          hidden: true
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 设置路由守卫
setupRouterGuard(router)

// 路由守卫
router.beforeEach((
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  // 设置页面标题
  document.title = `${to.meta.title || 'Docker 管理系统'}`

  // 检查是否需要登录
  if (to.matched.some((record: RouteRecordRaw) => record.meta?.requiresAuth)) {
    // TODO: 检查用户是否已登录
    const isAuthenticated = localStorage.getItem('token')
    if (!isAuthenticated) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router 