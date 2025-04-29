import { LogoutIcon } from 'tdesign-icons-vue-next';
import { shallowRef } from 'vue';

import Layout from '@/layouts/index.vue';

export default [
  {
    path: '/user',
    name: 'user',
    component: Layout,
    redirect: '/user/index',
    meta: {
      title: '用户中心',
      icon: 'user',
    },
    children: [
      {
        path: 'index',
        name: 'User',
        component: () => import('@/pages/user/index.vue'),
        meta: {
          title: '用户中心',
        },
      },
    ],
  },
  {
    path: '/loginRedirect',
    name: 'loginRedirect',
    redirect: '/login',
    meta: { title: '登录页', hidden: true,icon: shallowRef(LogoutIcon) },
    component: () => import('@/layouts/blank.vue'),
    children: [
      {
        path: 'index',
        redirect: '/login',
        component: () => import('@/layouts/blank.vue'),
        meta: { title: '登录页' },
      },
    ],
  },
];
