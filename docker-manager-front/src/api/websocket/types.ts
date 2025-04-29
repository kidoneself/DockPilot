// WebSocket消息类型
export type WebSocketMessageType = 
  | 'PULL_IMAGE'
  | 'PULL_START'
  | 'PULL_PROGRESS'
  | 'PULL_COMPLETE'
  | 'CANCEL_PULL'
  | 'CHECK_IMAGE_UPDATES'
  | 'CHECK_UPDATES_COMPLETE'
  | 'ERROR'
  | 'INSTALL_CHECK_IMAGES'
  | 'INSTALL_CHECK_IMAGES_RESULT'
  | 'INSTALL_VALIDATE'
  | 'INSTALL_PULL_IMAGE'
  | 'UPDATE_IMAGE'
  | 'INSTALL_START'
  | 'INSTALL_LOG'
  | 'INSTALL_START_RESULT';

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
  onError?: (error: any) => void;
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