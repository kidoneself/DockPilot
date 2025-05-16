/**
 * WebSocket业务相关类型定义
 */

/**
 * WebSocket消息类型
 */
export enum WebSocketMessageType {
  // 系统消息
  HEARTBEAT = 'HEARTBEAT',
  HEARTBEAT_RESPONSE = 'HEARTBEAT_RESPONSE',

  TEST_NOTIFY = 'TEST_NOTIFY',
  TEST_NOTIFY_RESPONSE = 'TEST_NOTIFY_RESPONSE',

  PROGRESS='PROGRESS',
  START='START',
  COMPLETE='COMPLETE',
  ERROR = 'ERROR',
  LOG = 'LOG',


  
  // 容器相关
  CONTAINER_LIST = 'CONTAINER_LIST',
  CONTAINER_DETAIL = 'CONTAINER_DETAIL',
  CONTAINER_START = 'CONTAINER_START',
  CONTAINER_STOP = 'CONTAINER_STOP',
  CONTAINER_RESTART = 'CONTAINER_RESTART',
  CONTAINER_DELETE = 'CONTAINER_DELETE',
  CONTAINER_UPDATE = 'CONTAINER_UPDATE',
  CONTAINER_CREATE = 'CONTAINER_CREATE',
  CONTAINER_LOGS = 'CONTAINER_LOGS',
  CONTAINER_STATS = 'CONTAINER_STATS',
  CONTAINER_STATE_CHANGE = 'CONTAINER_STATE_CHANGE',
  CONTAINER_JSON_CONFIG = 'CONTAINER_JSON_CONFIG',
  
  // 镜像相关
  PULL_IMAGE = 'PULL_IMAGE',
  PULL_START = 'PULL_START',
  PULL_PROGRESS = 'PULL_PROGRESS',
  PULL_COMPLETE = 'PULL_COMPLETE',
  CANCEL_PULL = 'CANCEL_PULL',
  IMAGE_LIST = 'IMAGE_LIST',
  IMAGE_DETAIL = 'IMAGE_DETAIL',
  IMAGE_DELETE = 'IMAGE_DELETE',
  IMAGE_UPDATE = 'IMAGE_UPDATE',
  IMAGE_BATCH_UPDATE = 'IMAGE_BATCH_UPDATE',
  IMAGE_CANCEL_PULL = 'IMAGE_CANCEL_PULL',
  IMAGE_CHECK_UPDATES = 'IMAGE_CHECK_UPDATES',
  CHECK_IMAGE_UPDATES = 'CHECK_IMAGE_UPDATES',
  CHECK_UPDATES_COMPLETE = 'CHECK_UPDATES_COMPLETE',
  
  // 网络相关
  NETWORK_LIST = 'NETWORK_LIST',
  NETWORK_CREATE = 'NETWORK_CREATE',
  NETWORK_DELETE = 'NETWORK_DELETE',
  NETWORK_OPERATION_RESULT = 'NETWORK_OPERATION_RESULT',
  
  // 安装相关
  INSTALL_CHECK_IMAGES = 'INSTALL_CHECK_IMAGES',
  INSTALL_CHECK_IMAGES_RESULT = 'INSTALL_CHECK_IMAGES_RESULT',
  INSTALL_VALIDATE = 'INSTALL_VALIDATE',
  INSTALL_VALIDATE_RESULT = 'INSTALL_VALIDATE_RESULT',
  INSTALL_START = 'INSTALL_START',
  INSTALL_START_RESULT = 'INSTALL_START_RESULT',
  INSTALL_LOG = 'INSTALL_LOG',
  INSTALL_PULL_IMAGE = 'INSTALL_PULL_IMAGE',
  IMPORT_TEMPLATE = 'IMPORT_TEMPLATE',
  IMPORT_TEMPLATE_RESULT = 'IMPORT_TEMPLATE_RESULT',
  DELETE_TEMPLATE = 'DELETE_TEMPLATE',
  OPERATION_RESULT = 'OPERATION_RESULT'




}

/**
 * WebSocket消息接口
 */
export interface WebSocketMessage {
  /** 消息类型 */
  type: WebSocketMessageType;
  /** 任务ID，用于关联请求和响应 */
  taskId: string;
  /** 消息数据 */
  data: any;
  /** 时间戳（毫秒） */
  timestamp?: number;
  /** 消息状态 */
  status?: 'PROCESSING' | 'SUCCESS' | 'ERROR';
  /** 错误信息 */
  errorMessage?: string;
  /** 进度信息（0-100） */
  progress?: number;
}

/**
 * Docker镜像拉取参数
 */
export interface PullImageParams {
  /** 镜像名称 */
  imageName: string;
}

/**
 * Docker镜像拉取进度
 */
export interface PullImageProgress {
  /** 拉取进度（0-100） */
  progress: number;
  /** 当前状态描述 */
  status: string;
  /** 分层下载信息 */
  layers?: {
    /** 层ID */
    id: string;
    /** 层状态 */
    status: string;
    /** 层进度 */
    progress: number;
  }[];
}

/**
 * Docker WebSocket回调接口
 */
export interface DockerWebSocketCallbacks {
  /** 任务开始回调 */
  onStart?: (taskId: string) => void;
  /** 进度更新回调 */
  onProgress?: (progress: number) => void;
  /** 日志回调 */
  onLog?: (data: any) => void;
  /** 任务完成回调 */
  onComplete?: () => void;
  /** 错误回调 */
  onError?: (error: string) => void;
}

/**
 * Docker镜像信息
 */
export interface DockerImage {
  /** 镜像名称 */
  name: string;
  /** 镜像标签 */
  tag: string;
  /** 是否需要更新 */
  needUpdate?: boolean;
  /** 最后检查时间 */
  lastChecked?: string;
}

/**
 * 容器操作结果
 */
export interface ContainerOperationResult {
  /** 操作是否成功 */
  success: boolean;
  /** 操作消息 */
  message: string;
  /** 容器ID */
  containerId: string;
  /** 操作类型 */
  operation: 'start' | 'stop' | 'restart' | 'delete' | 'update' | 'create';
}

/**
 * 网络操作结果
 */
export interface NetworkOperationResult {
  /** 操作是否成功 */
  success: boolean;
  /** 操作消息 */
  message: string;
  /** 网络ID */
  networkId: string;
  /** 操作类型 */
  operation: 'create' | 'delete';
}

/**
 * 镜像操作结果
 */
export interface ImageOperationResult {
  /** 操作是否成功 */
  success: boolean;
  /** 操作消息 */
  message: string;
  /** 镜像ID或名称 */
  imageId: string;
  /** 操作类型 */
  operation: 'delete' | 'update' | 'pull';
}

/**
 * WebSocket 请求选项定义
 */
export interface WebSocketRequestOptions {
  /** 消息类型（用于区分业务行为） */
  type: WebSocketMessageType;
  /** 要发送的数据 */
  data: any;
  /** 任务开始时的回调 */
  onStart?: (taskId: string) => void;
  /** 任务进度更新（0-100） */
  onProgress?: (progress: number) => void;
  /** 后端标记 status 为 PROCESSING 时的中间数据回调 */
  onLog?: (data: any) => void;
  /** 任务完成时回调（type 为 COMPLETE） */
  onComplete?: (data: any) => void;
  /** 任务失败或异常时的回调 */
  onError?: (error: string) => void;
  /** 超时触发时间（毫秒） */
  timeout?: number;
  /** 超时触发后的回调 */
  onTimeout?: () => void;
}

/**
 * WebSocket 响应消息结构
 */
export interface WebSocketResponse {
  type: WebSocketMessageType;
  taskId: string;
  data: any;
  timestamp: number;
  errorMessage?: string;
  progress?: number;
  /** 可选：处理中状态，如构建中、导入中等 */
  status?: string;
}