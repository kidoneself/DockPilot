import { request } from '@/utils/request';
import type {
  Container,
  ContainerDetail,
  ContainerListResponse,
  ImageListResponse,
  NetworkListResponse,
  ContainerStatsResponse,
  ContainerLogsParams,
  ContainerUpdateParams,
  CreateContainerParams
} from '@/types/api/container.d.ts';

export type {
  Container,
  ContainerDetail,
  ContainerListResponse,
  ImageListResponse,
  NetworkListResponse,
  ContainerStatsResponse,
  ContainerLogsParams,
  ContainerUpdateParams,
  CreateContainerParams
};

/**
 * API 函数
 */

/**
 * 容器相关 API
 */

/**
 * 获取容器列表
 * @returns 容器列表
 */
export const getContainerList = async (): Promise<Container[]> => {
  const response = await request.get<ContainerListResponse>({
    url: '/containers',
  });
  return response.data || [];
};

/**
 * 创建容器
 * @param data 容器创建参数
 * @returns 创建结果
 */
export const createContainer = async (data: CreateContainerParams): Promise<any> => {
  return request.post({
    url: '/containers',
    data,
  });
};

/**
 * 启动容器
 * @param id 容器ID
 * @returns 启动结果
 */
export const startContainer = async (id: string): Promise<any> => {
  return request.post({
    url: `/containers/start/${id}`,
  });
};

/**
 * 停止容器
 * @param id 容器ID
 * @returns 停止结果
 */
export const stopContainer = async (id: string): Promise<any> => {
  return request.post({
    url: `/containers/stop/${id}`,
  });
};

/**
 * 重启容器
 * @param id 容器ID
 * @returns 重启结果
 */
export const restartContainer = async (id: string): Promise<any> => {
  return request.post({
    url: `/containers/restart/${id}`,
  });
};

/**
 * 获取容器日志
 * @param id 容器ID
 * @param params 日志参数
 * @returns 日志内容
 */
export const getContainerLogs = async (
  id: string,
  params: ContainerLogsParams = {},
): Promise<{ data: string }> => {
  return request.get({
    url: `/containers/${id}/logs`,
    params,
  });
};

/**
 * 删除容器
 * @param id 容器ID
 * @returns 删除结果
 */
export const deleteContainer = async (id: string): Promise<any> => {
  return request.delete({
    url: `/containers/${id}`,
  });
};

/**
 * 获取容器详情
 * @param id 容器ID
 * @returns 容器详情
 */
export const getContainerDetail = async (id: string): Promise<ContainerDetail> => {
  return request.get<ContainerDetail>({
    url: `/containers/${id}/config`,
  });
};

/**
 * 更新容器
 * @param id 容器ID
 * @param data 更新数据
 * @returns 更新结果
 */
export const updateContainer = async (id: string, data: ContainerUpdateParams): Promise<any> => {
  return request.post({
    url: `/containers/update/${id}`,
    data,
  });
};

/**
 * 镜像相关 API
 */

/**
 * 获取镜像列表
 * @returns 镜像列表
 */
export const getImageList = async (): Promise<ImageListResponse> => {
  return request.get<ImageListResponse>({
    url: '/images',
  });
};

/**
 * 获取镜像详情
 * @param imageName 镜像名称
 * @returns 镜像详情
 */
export const getImageDetail = async (imageName: string): Promise<any> => {
  return request.post({
    url: '/images/detail',
    data: {
      imageName,
    },
  });
};

/**
 * 批量更新镜像
 * @param params 更新参数
 * @returns 更新结果
 */
export const batchUpdateImages = async (params: { useProxy: boolean }): Promise<any> => {
  return request.post({
    url: '/images/batch-update',
    data: params,
  });
};

/**
 * 取消镜像拉取
 * @param taskId 任务ID
 * @returns 取消结果
 */
export const cancelImagePull = async (taskId: string): Promise<any> => {
  return request.post({
    url: '/images/cancel-pull',
    data: { taskId },
  });
};

/**
 * 检查镜像更新
 * @returns 检查结果
 */
export const checkImageUpdates = async (): Promise<any> => {
  return request.post({
    url: '/images/check-updates',
  });
};

/**
 * 删除镜像
 * @param imageId 镜像ID
 * @returns 删除结果
 */
export const deleteImage = async (imageId: string): Promise<any> => {
  return request.delete({
    url: `/images/${imageId}`,
  });
};

/**
 * 更新镜像
 * @param params 更新参数
 * @returns 更新结果
 */
export const updateImage = async (params: {
  image: string;
  tag: string;
  id?: string;
}): Promise<any> => {
  return request.post({
    url: '/images/update',
    data: params,
  });
};

/**
 * 网络相关 API
 */

/**
 * 获取网络列表
 * @returns 网络列表
 */
export const getNetworkList = async (): Promise<NetworkListResponse> => {
  return request.get<NetworkListResponse>({
    url: '/networks/list',
  });
};

/**
 * 获取容器资源使用情况
 * @param id 容器ID
 * @returns 容器资源使用情况
 */
export const getContainerStats = async (id: string): Promise<ContainerStatsResponse> => {
  return request.get<ContainerStatsResponse>({
    url: `/containers/${id}/stats`,
  });
};
