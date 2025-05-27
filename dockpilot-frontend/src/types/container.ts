// 定义 VolumeMapping 接口
export interface VolumeMapping {
  containerPath: string
  hostPath: string
  readOnly: boolean
}

// 定义后端返回的容器详情接口
export interface ContainerDetail {
  containerId: string      // 容器ID
  containerName: string    // 容器名
  imageName: string        // 镜像名
  imageId: string          // 镜像ID
  createdTime: string      // 容器创建时间
  status: string           // 容器状态 (running, exited...) 后端返回的 state
  restartCount: number     // 重启次数
  command?: string[]       // 启动命令及其参数列表
  workingDir?: string      // 工作目录
  entrypoints?: string[]   // 入口点
  labels?: Record<string, string> // 标签
  envs?: string[]          // 环境变量
  volumes?: Array<{
    containerPath: string
    hostPath: string
    readOnly: boolean
  }>
  ports?: string[]         // 端口映射
  exposedPorts?: string[]  // 暴露端口
  devices?: string[]       // 设备映射
  networkMode?: string     // 网络模式
  ipAddress?: string       // 容器IP
  restartPolicyName?: string    // 重启策略名
  restartPolicyMaxRetry?: number // 重启策略最大重试次数
  privileged?: boolean          // 是否特权模式
  capAdd?: string[]            // 添加的 Linux 能力
  capDrop?: string[]           // 删除的 Linux 能力
  networkSettings?: {
    IPAddress: string
    Gateway: string
    NetworkMode: string
  }
  // 性能数据字段
  cpuPercent?: number
  memoryUsage?: number
  memoryLimit?: number
  upload?: string
  download?: string
}

// 定义表单数据类型
export interface ContainerFormData {
  name: string
  image: string
  tag: string
  autoPull: boolean
  autoRemove: boolean
  restartPolicy: string
  portMappings: Array<{
    hostPort: string
    containerPort: string
    protocol: string
    ip?: string
  }>
  networkMode: string
  ipAddress: string
  gateway: string
  volumeMappings: Array<{
    hostPath: string
    containerPath: string
    readOnly: boolean
  }>
  devices: Array<{
    hostPath: string
    containerPath: string
  }>
  environmentVariables: Array<{
    key: string
    value: string
  }>
  privileged: boolean
  capAdd: string[]
  capDrop: string[]
  memoryLimit: string
  cpuLimit: string
  entrypoint: string[]
  cmd: string[]
  workingDir: string
  user: string
  labels: Array<{
    key: string
    value: string
  }>
  healthcheck: {
    test: string[]
    interval: string
    timeout: string
    retries: number
    startPeriod: string
  }
}

// 定义用于前端展示的完整容器详情数据接口 (包含静态和动态信息)
export interface ContainerDetailData extends ContainerDetail {
  // 格式化后的动态数据
  memory: string // 格式化后的内存使用量
  upload: string
  download: string
  // 可能需要的其他前端展示字段
  iconUrl?: string
}

export interface PortMapping {
  hostPort: string
  containerPort: string
  protocol: string
  ip: string
}

export interface DeviceMapping {
  hostPath: string
  containerPath: string
}

export interface EnvironmentVariable {
  key: string
  value: string
}

export interface Label {
  key: string
  value: string
}

export interface HealthCheck {
  test: string[]
  interval: string
  timeout: string
  retries: number
  startPeriod: string
}

export interface ContainerForm {
  image: string
  tag: string
  autoPull: boolean
  name: string
  autoRemove: boolean
  restartPolicy: string
  portMappings: PortMapping[]
  networkMode: string
  ipAddress: string
  gateway: string
  volumeMappings: VolumeMapping[]
  devices: DeviceMapping[]
  environmentVariables: EnvironmentVariable[]
  privileged: boolean
  capAdd: string[]
  capDrop: string[]
  memoryLimit: string
  cpuLimit: string
  entrypoint: string[]
  cmd: string[]
  workingDir: string
  user: string
  labels: Label[]
  healthcheck: HealthCheck
} 