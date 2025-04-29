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
  state: string;
  status: string;
  ports: Port[];
  created: number;
  labels: Record<string, string>;
  sizeRw: number;
  sizeRootFs: number;
  hostConfig: {
    networkMode: string;
    restartPolicy: string;
  };
  networkSettings: {
    ipAddress: string;
  };
  mounts: {
    source: string;
    destination: string;
    readOnly: boolean;
  }[];
  [key: string]: any;
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
  command: string;
  workingDir: string;
  entrypoints: string[];
  labels: Record<string, string>;
  envs: string[];
  volumes: string[];
  ports: string[];
  exposedPorts: string[];
  devices: string[];
  networkMode: string;
  ipAddress: string;
  state: string;
  startedAt: string;
  privileged: boolean;
  capAdd: string[];
  capDrop: string[];
  networkSettings: {
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
  code?: number;
  data?: any;
}

export interface ContainerListResponse {
  code: number;
  data: Container[];
  message: string;
}

export interface ImageListResponse {
  code: number;
  data: Array<{
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
  }>;
  message: string;
}

export interface NetworkListResponse {
  code: number;
  data: Array<{
    Name: string;
    nameStr: string;
    Id: string;
    Created: string;
    Scope: string;
    Driver: string;
    EnableIPv6: boolean;
    IPAM: {
      Driver: string;
      Options: any;
      Config: Array<{
        Subnet: string;
        Gateway: string;
      }>;
    };
    Internal: boolean;
    Attachable: boolean;
    Ingress: boolean;
    ConfigFrom: any;
    ConfigOnly: boolean;
    Containers: any;
    Options: any;
    Labels: any;
  }>;
  message: string;
}

export interface ContainerStatsResponse {
  code: number;
  message: string;
  data: {
    cpuPercent: number;
    memoryUsage: number;
    memoryLimit: number;
    networkRx: number;
    networkTx: number;
    running: boolean;
  };
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