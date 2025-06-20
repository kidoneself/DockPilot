export enum MessageType {
  // 镜像相关
  PULL_IMAGE = 'PULL_IMAGE',
  PUSH_IMAGE = 'PUSH_IMAGE',
  BUILD_IMAGE = 'BUILD_IMAGE',
  IMAGE_LIST = 'IMAGE_LIST',
  IMAGE_DETAIL = 'IMAGE_DETAIL',
  IMAGE_DELETE = 'IMAGE_DELETE',
  IMAGE_UPDATE = 'IMAGE_UPDATE',
  IMAGE_BATCH_UPDATE = 'IMAGE_BATCH_UPDATE',
  IMAGE_CANCEL_PULL = 'IMAGE_CANCEL_PULL',
  IMAGE_CHECK_UPDATES = 'IMAGE_CHECK_UPDATES',

  // 容器相关
  CONTAINER_CREATE = 'CONTAINER_CREATE',
  CONTAINER_START = 'CONTAINER_START',
  CONTAINER_STOP = 'CONTAINER_STOP',
  CONTAINER_RESTART = 'CONTAINER_RESTART',
  CONTAINER_DELETE = 'CONTAINER_DELETE',
  CONTAINER_LIST = 'CONTAINER_LIST',
  CONTAINER_STATS = 'CONTAINER_STATS',
  CONTAINER_DETAIL = 'CONTAINER_DETAIL',
  CONTAINER_UPDATE = 'CONTAINER_UPDATE',
  CONTAINER_UPDATE_INFO = 'CONTAINER_UPDATE_INFO',

  // 应用相关
  APP_INSTALL = 'APP_INSTALL',

  // 网络相关
  NETWORK_LIST = 'NETWORK_LIST',
  NETWORK_CREATE = 'NETWORK_CREATE',
  NETWORK_DELETE = 'NETWORK_DELETE',
  NETWORK_DETAIL = 'NETWORK_DETAIL',

  // 系统状态相关
  SYSTEM_STATUS = 'SYSTEM_STATUS',

  // Docker事件通知
  DOCKER_EVENT_NOTIFICATION = 'DOCKER_EVENT_NOTIFICATION',

  // 系统消息
  COMPLETE = 'COMPLETE',
  ERROR = 'ERROR',
  PROGRESS = 'PROGRESS',
  LOG = 'LOG'
}

export interface DockerWebSocketMessage {
  type: string;
  taskId: string;
  data?: any;
  timestamp: number;
  errorMessage?: string;
  progress?: number;
}

export interface DockerEventNotification {
  eventType: string; // create, start, stop, destroy, rename
  containerId: string;
  containerName: string;
  message: string;
  timestamp: number;
}

export interface WebSocketCallbacks {
  onProgress?: (progress: number, taskId: string, imageName?: string) => void;
  onLog?: (log: string, taskId: string, imageName?: string) => void;
  onComplete?: (data: DockerWebSocketMessage, taskId: string) => void;
  onError?: (error: string, taskId: string) => void;
}

export interface WebSocketRequestOptions {
  type: string;
  data?: any;
  callbacks: WebSocketCallbacks;
  timeout?: number;
}

export interface Container {
  id: string
  names: string[]
  image: string
  imageId: string
  command: string
  created: number
  state: string
  status: string
  ports: {
    privatePort: string
    publicPort: string
    type: string
  }[]
  mounts: {
    source: string
    destination: string
    readOnly: boolean
  }[]
  labels: Record<string, string>
  hostConfig: {
    networkMode: string
  }
  lastError?: string
  // 性能数据，初始为空
  stats?: {
    cpu: number
    memory: string
    network: {
      upload: string
      download: string
    }
  }
}

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