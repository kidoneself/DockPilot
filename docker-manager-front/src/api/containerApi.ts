/**
 * 容器相关API
 * 此文件仅导出类型定义和WebSocket实现的API函数
 */

// 从WebSocket实现中导入API函数
import {
  getContainerList,
  startContainer,
  stopContainer,
  restartContainer,
  getContainerDetail
} from './websocket/container';

// 直接导出类型定义
export type {
  Container,
  ContainerDetail,
  ContainerListResponse,
  ImageListResponse,
  NetworkListResponse,
  ContainerStats,
  ContainerLogsParams,
  ContainerUpdateParams,
  CreateContainerParams
} from '@/api/model/containerModel.ts';

// 导出WebSocket实现的API函数
export {
  getContainerList,
  startContainer,
  stopContainer,
  restartContainer,
  getContainerDetail
};
