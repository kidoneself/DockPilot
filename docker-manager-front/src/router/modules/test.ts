import { RouteRecordRaw } from 'vue-router';
import { BrowseIcon } from 'tdesign-icons-vue-next';
import Layout from '@/layouts/index.vue';

const test: RouteRecordRaw = {
  path: '/test',
  name: 'test',
  component: Layout,
  meta: { title: '测试', icon: BrowseIcon },
  children: [
    {
      path: 'websocket',
      name: 'TestWebSocket',
      component: () => import('@/pages/test/websocket.vue'),
      meta: { title: 'WebSocket测试' },
    },
  ],
};

export default test; 