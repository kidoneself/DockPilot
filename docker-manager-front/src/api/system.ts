import type { AxiosRequestConfig } from 'axios';

import { request } from '@/utils/request';

export interface SystemSettingResponse {
  data: string;
  code: number;
  message: string;
}

/**
 * 获取系统设置
 * @param key 设置项的键
 */
export function getSystemSetting(key: string) {
  return request.get<SystemSettingResponse>({
    url: `/system/settings?key=${key}`,
  } as AxiosRequestConfig);
}

/**
 * 设置系统设置
 * @param key 设置项的键
 * @param value 设置项的值
 */
export function setSystemSetting(key: string, value: string) {
  return request.post<SystemSettingResponse>({
    url: '/system/settings',
    data: {
      key,
      value
    }
  } as AxiosRequestConfig);
}

/**
 * 更新系统设置
 * @param key 设置项的键
 * @param value 设置项的值
 */
export function updateSystemSetting(key: string, value: string) {
  return request.put<SystemSettingResponse>({
    url: '/system/settings',
    data: {
      key,
      value
    }
  } as AxiosRequestConfig);
}

/**
 * 删除系统设置
 * @param key 设置项的键
 */
export function deleteSystemSetting(key: string) {
  return request.delete<SystemSettingResponse>({
    url: `/system/settings?key=${key}`,
  } as AxiosRequestConfig);
}

export interface ProxySettingRequest {
  enabled: boolean;
  url: string;
}

export interface MirrorSettingRequest {
  urls: {
    url: string;
    enabled: boolean;
  }[];
}

export interface SettingResponse {
  code: number;
  message: string;
  data: any;
}

export function setProxySetting(data: ProxySettingRequest) {
  return request.post<SettingResponse>({
    url: '/system/settings',
    data: {
      key: 'proxy',
      value: JSON.stringify({
        enabled: data.enabled,
        url: data.url
      })
    }
  });
}

export function setMirrorSetting(data: MirrorSettingRequest) {
  return request.post<SettingResponse>({
    url: '/system/settings',
    data: {
      key: 'mirror',
      value: JSON.stringify(data.urls)
    }
  });
}

/**
 * 测试代理延迟
 * @returns 代理延迟测试结果
 */
export function testProxyLatency() {
  return request.get<SettingResponse>({
    url: '/system/proxy/test',
  });
}
