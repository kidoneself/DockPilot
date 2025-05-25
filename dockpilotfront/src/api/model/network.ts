/**
 * 网络信息接口
 */
export interface Network {
  /** 网络ID */
  id: string
  /** 网络名称 */
  name: string
  /** 驱动类型 */
  driver: string
  /** 作用域 */
  scope: string
  /** 是否启用IPv6 */
  enableIPv6: boolean
  /** 是否是内部网络 */
  internal: boolean
  /** 是否允许容器直接连接 */
  attachable: boolean
  /** 是否是ingress网络 */
  ingress: boolean
  /** 是否仅为配置网络 */
  configOnly: boolean
  /** IPAM配置 */
  ipamConfig: Array<{
    /** 子网 */
    subnet: string
    /** 网关 */
    gateway: string
  }>
  /** IPAM驱动类型 */
  ipamDriver: string
  /** IPAM选项 */
  ipamOptions: Record<string, string>
  /** 用户定义的标签 */
  labels: Record<string, string>
  /** 网络选项 */
  options: Record<string, string>
  /** 网络名称（中文） */
  nameStr: string
  /** 连接到该网络的容器 */
  containers?: Record<string, {
    /** 容器名称 */
    name: string
    /** MAC地址 */
    macAddress?: string
    /** IPv4地址 */
    ipv4Address?: string
    /** IPv6地址 */
    ipv6Address?: string
  }>
}

/**
 * 创建网络请求参数
 */
export interface CreateNetworkRequest {
  /** 网络名称 */
  name: string
  /** 驱动类型 */
  driver: string
  /** 子网 */
  subnet: string
  /** 网关 */
  gateway: string
}

/**
 * 删除网络请求参数
 */
export interface DeleteNetworkRequest {
  /** 网络ID */
  networkId: string
}

/**
 * 网络列表响应
 */
export type NetworkListResponse = Network[] 