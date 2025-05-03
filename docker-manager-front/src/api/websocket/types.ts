// WebSocket消息类型
export type WebSocketMessageType =
  | 'HEARTBEAT'
  | 'HEARTBEAT_RESPONSE'
  | 'TEST_NOTIFY'
  | 'TEST_NOTIFY_RESPONSE'
  | 'INSTALL_CHECK_IMAGES'
  | 'INSTALL_VALIDATE'
  | 'PULL_IMAGE'
  | 'PULL_START'
  | 'PULL_PROGRESS'
  | 'PULL_COMPLETE'
  | 'CANCEL_PULL'
  | 'CONTAINER_LIST'
  | 'CONTAINER_DETAIL'
  | 'CONTAINER_START'
  | 'CONTAINER_STOP'
  | 'CONTAINER_RESTART'
  | 'CONTAINER_DELETE'
  | 'CONTAINER_UPDATE'
  | 'CONTAINER_CREATE'
  | 'CONTAINER_LOGS'
  | 'CONTAINER_STATS'
  | 'CONTAINER_OPERATION_RESULT'
  | 'NETWORK_LIST'
  | 'NETWORK_CREATE'
  | 'NETWORK_DELETE'
  | 'NETWORK_OPERATION_RESULT'
  | 'IMAGE_LIST'
  | 'IMAGE_DETAIL'
  | 'IMAGE_DELETE'
  | 'IMAGE_UPDATE'
  | 'IMAGE_BATCH_UPDATE'
  | 'IMAGE_CANCEL_PULL'
  | 'IMAGE_CHECK_UPDATES'
  | 'IMAGE_OPERATION_RESULT'
  | 'ERROR';

// WebSocket消息接口
export interface WebSocketMessage {
  type: WebSocketMessageType;
  taskId: string;
  data: any;
}

// WebSocket配置选项
export interface WebSocketOptions {
  url: string;
  onMessage?: (message: WebSocketMessage) => void;
  onError?: (error: Error) => void;
  onClose?: (event: CloseEvent) => void;
  maxReconnectAttempts?: number;
  reconnectInterval?: number;
  connectionTimeout?: number;
}

// Docker镜像拉取参数
export interface PullImageParams {
  imageName: string;
}

// Docker镜像拉取进度
export interface PullImageProgress {
  progress: number;
  status: string;
  layers?: any[];
}

// Docker WebSocket回调
export interface DockerWebSocketCallbacks {
  onStart?: (taskId: string) => void;
  onProgress?: (progress: PullImageProgress) => void;
  onComplete?: () => void;
  onError?: (error: string) => void;
}

// Docker镜像信息
export interface DockerImage {
  name: string;
  tag: string;
  needUpdate?: boolean;
  lastChecked?: string;
} 