/*
 * 容器相关API类型定义
 * 生成时间：2024-06-09 21:40:00
 */

export interface Port {
  IP: string;
  PrivatePort: number;
  PublicPort: number;
  Type: string;
}

export interface Container {
  id: string;
  names: string[];
  image: string;
  imageId: string;
  command: string;
  created: number;
  state: string;
  status: string;
  ports: {
    IP: string;
    PrivatePort: number;
    PublicPort: number;
    Type: string;
  }[];
  labels: Record<string, string>;
  networkSettings: {
    networks: Record<string, {
      IPAddress: string;
      Gateway: string;
      NetworkID: string;
    }>;
  };
  mounts: {
    Type: string;
    Source: string;
    Destination: string;
    Mode: string;
    RW: boolean;
    Propagation: string;
  }[];
  needUpdate?: boolean;
}

export interface ContainerDetail {
  containerId: string;
  containerName: string;
  name: string;
  imageId: string;
  imageName: string;
  createdTime: string;
  status: string;
  restartCount: number;
  restartPolicyName: string;
  restartPolicyMaxRetry: number;
  privileged: boolean;
  networkMode: string;
  ipAddress: string;
  ports: Array<{
    containerPort: number;
    hostPort: number;
    protocol: string;
  }>;
  volumes: Array<{
    containerPath: string;
    hostPath: string;
    mode: string;
  }>;
  environment: Array<{
    key: string;
    value: string;
  }>;
  command: string;
  entrypoints: string[];
  labels: Record<string, string>;
  needUpdate?: boolean;
  envs?: string[];
  code?: number;
  networkSettings?: {
    IPAddress: string;
    Gateway: string;
    NetworkMode: string;
  };
  stats?: {
    cpuPercent: number;
    memoryUsage: number;
    memoryLimit: number;
    networkRx: number;
    networkTx: number;
    running: boolean;
  };
  data?: any;
}

export interface ContainerListResponse {
  data: Container[];
}

export interface ImageInfo {
  id: string;
  statusId: number;
  name: string;
  tag: string;
  size: number;
  created: Date;
  localCreateTime: string;
  remoteCreateTime: string;
  needUpdate: boolean;
  lastChecked: Date;
}

export type ImageListResponse = ImageInfo[];

export interface NetworkInfo {
  id: string;
  name: string;
  nameStr: string;
  driver: string;
  scope: string;
  enableIPv6: boolean;
  internal: boolean;
  attachable: boolean;
  ingress: boolean;
  configOnly: boolean;
  ipamDriver: string;
  ipamConfig: Array<{
    subnet?: string;
    gateway?: string;
  }>;
  labels: Record<string, string>;
  options: Record<string, string>;
}

export type NetworkListResponse = NetworkInfo[];

export interface ContainerStatsResponse {
  cpuPercent: number;
  memoryUsage: number;
  memoryLimit: number;
  networkRx: number;
  networkTx: number;
  running: boolean;
}

export interface ContainerLogsParams {
  tail?: number;
  follow?: boolean;
  timestamps?: boolean;
}

export interface ContainerUpdateParams {
  [key: string]: any;
}

export interface CreateContainerParams {
  // 这里可根据实际需要补充字段
  [key: string]: any;
} 