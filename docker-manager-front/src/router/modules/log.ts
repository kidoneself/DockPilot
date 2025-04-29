import { shallowRef } from 'vue';
import Layout from '@/layouts/index.vue';

export default [
  {
    path: '/log',
    name: 'log',
    component: Layout,
    redirect: '/log/index',
    meta: {
      title: '日志管理',
      icon: 'file',
    },
    children: [
      {
        path: 'index',
        name: 'LogIndex',
        component: () => import('@/pages/log/index.vue'),
        meta: {
          title: '日志列表',
        },
      },
    ],
  },
]; 