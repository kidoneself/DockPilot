/*
 * 容器相关API类型定义
 * 生成时间：2024-06-09 21:40:00
 */

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
    networks: Record<
      string,
      {
        IPAddress: string;
        Gateway: string;
        NetworkID: string;
      }
    >;
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

export interface ContainerStats {
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

/*
 * 容器相关类型定义
 * 生成时间：2024-06-09 21:00:00
 */

export interface ValidationResult {
  valid: boolean;
  message: string;
}

export interface PortMapping {
  hostPort: string;
  containerPort: string;
  protocol: string;
  ip?: string;
  validationResult?: ValidationResult;
}

export interface VolumeMapping {
  hostPath: string;
  containerPath: string;
  readOnly: boolean;
  isDefaultVolume?: boolean;
}

export interface DeviceMapping {
  hostPath: string;
  containerPath: string;
}

export interface EnvironmentVariable {
  key: string;
  value: string;
}

export interface Label {
  key: string;
  value: string;
}

export interface HealthCheck {
  test: string[];
  interval: string;
  timeout: string;
  retries: number;
  startPeriod: string;
}

export interface CreateContainerParams {
  image: string;
  tag: string;
  autoPull?: boolean;
  name: string;
  autoRemove?: boolean;
  restartPolicy?: string;
  portMappings: Array<{
    ip?: string;
    hostPort: string;
    containerPort: string;
    protocol?: string;
  }>;
  networkMode?: string;
  dns?: string[];
  dnsSearch?: string[];
  extraHosts?: string[];
  ipAddress?: string;
  macAddress?: string;
  volumeMounts: Array<{
    hostPath: string;
    containerPath: string;
    readOnly: boolean;
  }>;
  tmpfs?: string[];
  shmSize?: string;
  environmentVariables: Array<{
    key: string;
    value: string;
  }>;
  memory?: number;
  memorySwap?: number;
  cpuShares?: number;
  cpuPeriod?: number;
  cpuQuota?: number;
  cpusetCpus?: string;
  privileged?: boolean;
  capAdd?: string[];
  capDrop?: string[];
  securityOpt?: string[];
  devices?: string[];
  user?: string;
  groupAdd?: string[];
  workingDir?: string;
  command?: string[];
  entrypoint?: string[];
  stopSignal?: string;
  stopTimeout?: number;
  labels?: Record<string, string>;
  ipcMode?: string;
  pidMode?: string;
  utsMode?: string;
  sysctls?: Record<string, string>;
  healthcheck?: HealthCheck;
}

export interface ContainerForm {
  image: string;
  tag: string;
  autoPull: boolean;
  name: string;
  autoRemove: boolean;
  restartPolicy: string;
  portMappings: PortMapping[];
  networkMode: string;
  ipAddress: string;
  gateway: string;
  volumeMappings: VolumeMapping[];
  devices: DeviceMapping[];
  environmentVariables: EnvironmentVariable[];
  privileged: boolean;
  capAdd: string[];
  capDrop: string[];
  memoryLimit: string;
  cpuLimit: string;
  entrypoint: string[];
  cmd: string[];
  workingDir: string;
  user: string;
  labels: Label[];
  healthcheck: HealthCheck;
}
