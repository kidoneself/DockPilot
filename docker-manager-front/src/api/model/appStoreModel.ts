/**
 * 应用商店相关类型定义
 */
export interface AppStoreApp {
  /** 应用ID */
  id: string;
  /** 应用名称 */
  name: string;
  /** 应用描述 */
  description: string;
  /** 应用图标URL */
  icon: string;
  /** 应用分类 */
  category: string;
  /** 应用版本 */
  version: string;
  iconUrl: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 服务信息
 */
export interface AppStoreService {
  /** 服务标识符 */
  name: string;
  /** 服务显示名称 */
  displayName: string;
  /** 服务描述 */
  description: string;
  /** 依赖的其他服务 */
  dependencies: string[];
}

/**
 * 环境变量配置项
 */
export interface AppStoreEnvField {
  /** 配置项键名 */
  key: string;
  /** 配置项显示名称 */
  label: string;
  /** 配置项描述 */
  description: string;
  /** 默认值 */
  default: string;
  /** 是否必填 */
  required: boolean;
  /** 依赖的服务 */
  serviceDependencies: string[];
}

/**
 * 应用详情信息
 */
export interface AppStoreAppDetail extends AppStoreApp {
  /** 包含的服务列表 */
  services: ServiceConfig[];
  /** 环境变量配置项列表 */
  envFields: AppStoreEnvField[];
  parameters: ParameterConfig[];
}

/**
 * 安装日志
 */
export interface AppStoreInstallLog {
  /** 日志类型：信息/成功/警告/错误 */
  type: 'info' | 'success' | 'warning' | 'error';
  /** 日志内容 */
  message: string;
  /** 日志时间 */
  time: string;
}

/**
 * 服务安装状态
 */
export interface AppStoreServiceStatus {
  /** 服务名称 */
  name: string;
  /** 安装状态：等待安装/安装中/已完成/失败 */
  status: 'waiting' | 'running' | 'completed' | 'failed';
  /** 安装进度(0-100) */
  progress: number;
  message?: string;
}

export interface ServiceConfig {
  id: string;
  name: string;
  template: ServiceTemplate;
}

export interface ServiceTemplate {
  name: string;
  image: string;
  env: Record<string, string>;
  ports: Record<string, string>;
  volumes: Record<string, string>;
  restartPolicy: string;
}

export interface HostConfig {
  PortBindings: Record<string, string>;
  Binds: string[];
  RestartPolicy: string;
  Privileged: boolean;
  NetworkMode: string;
}

export interface ParameterConfig {
  key: string;
  name: string;
  value: string;
} 