import { DashboardIcon } from 'tdesign-icons-vue-next';
import { shallowRef } from 'vue';
import Layout from '@/layouts/index.vue';
import IFrame from '@/layouts/frame/index.vue';

export default [

  {
    path: '/dashboard',
    component: Layout,
    redirect: '/dashboard/base',
    name: 'dashboard',
    meta: {
      title: '仪表盘',
      icon: shallowRef(DashboardIcon),
      orderNo: 0,
      hidden: true,
    },
    children: [
      {
        path: 'base',
        name: 'DashboardBase',
        component: () => import('@/pages/dashboard/base/index.vue'),
        meta: {
          title: '概览仪表盘',
        },
      },
    ],
  },
  {
    path: '/docker',
    name: 'docker',
    component: Layout,
    redirect: '/docker/containers',
    meta: {
      title: '容器管理',
      icon: 'server',
    },
    children: [
      {
        path: 'containers',
        name: 'DockerContainers',
        component: () => import('@/pages/docker/containers/index.vue'),
        meta: {
          title: '容器管理',
        },
      },
      {
        path: 'create',
        name: 'ContainerCreate',
        component: () => import('@/pages/docker/containers/container-create/index.vue'),
        meta: {
          title: '创建容器',
          // hidden: true,
        },
      },
      {
        path: 'containers/edit',
        name: 'ContainerEdit',
        component: () => import('@/pages/docker/containers/container-edit/index.vue'),
        meta: {
          title: '编辑容器',
          hidden: true,
        },
      },
      {
        path: 'containers/detail',
        name: 'ContainerDetail',
        component: () => import('@/pages/docker/containers/container-detail/index.vue'),
        meta: {
          title: '容器详情',
          hidden: true,
        },
      },
      {
        path: 'images',
        name: 'DockerImages',
        component: () => import('@/pages/docker/images/index.vue'),
        meta: {
          title: '镜像列表',
        },
      },
      // {
      //   path: 'networks',
      //   name: 'DockerNetworks',
      //   component: () => import('@/pages/docker/networks/index.vue'),
      //   meta: {
      //     title: {
      //       zh_CN: '网络管理',
      //       en_US: 'Network Management',
      //     },
      //   },
      // },
      // {
      //   path: 'volumes',
      //   name: 'DockerVolumes',
      //   component: () => import('@/pages/docker/volumes/index.vue'),
      //   meta: {
      //     title: {
      //       zh_CN: '存储卷管理',
      //       en_US: 'Volume Management',
      //     },
      //   },
      // },
    ],
  },
  {
    path: '/system',
    name: 'System',
    component: () => import('@/layouts/index.vue'),
    redirect: '/system/proxy',
    meta: {
      title: '系统设置',
      icon: 'system-setting',
    },
    children: [
      {
        path: 'proxy',
        name: 'ProxySetting',
        component: () => import('@/pages/system/proxy.vue'),
        meta: {
          title: '加速设置',
        },
      },
      {
        path: 'xml-editor',
        name: 'XmlEditor',
        component: () => import('@/pages/system/xml-editor/index.vue'),
        meta: {
          title: 'XML编辑器',
          hidden: true,
        },
      },
    ],
  },
  {
    path: '/frame',
    name: 'Frame',
    component: Layout,
    redirect: '/frame/doc',
    meta: {
      icon: 'internet',
      title: '外部页面',
      orderNo: 10,
      hidden: true,
    },
    children: [
      {
        path: 'doc',
        name: 'Doc',
        component: IFrame,
        meta: {
          frameSrc: 'https://naspt.vip',
          title: '使用文档（内嵌）',
        },
      },
      {
        path: 'TDesign',
        name: 'TDesign',
        component: IFrame,
        meta: {
          frameSrc: 'https://pan.naspt.vip',
          title: '资源下载网盘（内嵌）',
        },
      },
      {
        path: 'TDesign2',
        name: 'TDesign2',
        component: IFrame,
        meta: {
          frameSrc: 'https://pan.naspt.vip',
          frameBlank: true,
          title: '资源下载网盘（外链）',
        },
      },
    ],
  },
  {
    path: '/store',
    name: 'Store',
    component: Layout,
    redirect: '/store/index',
    meta: {
      title: '应用商店',
      icon: 'app',
    },
    children: [
      {
        path: 'index',
        name: 'StoreIndex',
        component: () => import('@/pages/store/index.vue'),
        meta: {
          title: '应用商店',
        },
      },
      {
        path: 'detail/:id',
        name: 'StoreDetail',
        component: () => import('@/pages/store/detail.vue'),
        meta: {
          title: '应用详情',
          hidden: true,
        },
      },
      {
        path: 'install/:id',
        name: 'StoreInstall',
        component: () => import('@/pages/store/install.vue'),
        meta: {
          title: '安装应用',
          hidden: true,
        },
      },
    ],
  },
];
