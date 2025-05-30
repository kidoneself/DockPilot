import request from '@/utils/request'

/**
 * 容器YAML生成请求参数
 */
export interface ContainerYamlRequest {
  containerIds: string[]     // 容器ID列表
  projectName?: string       // 项目名称（可选）
  description?: string       // 项目描述（可选）
  excludeFields?: string[]   // 排除字段列表（可选）
  envDescriptions?: Record<string, string>  // 环境变量描述配置（可选）
}

/**
 * 容器YAML生成响应
 */
export interface ContainerYamlResponse {
  yamlContent: string       // 生成的YAML内容
  containerCount: number    // 容器数量
  projectName: string       // 项目名称
  generateTime: string      // 生成时间
  success: boolean          // 成功标识
  message: string           // 消息
}

/**
 * 根据容器ID列表生成YAML配置
 * @param params 请求参数
 * @returns YAML配置响应
 */
export const generateContainerYaml = (params: ContainerYamlRequest): Promise<ContainerYamlResponse> => {
  return request.post('/api/containers/generate-yaml', params)
}

/**
 * 预览容器YAML配置（不包含敏感信息）
 * @param params 请求参数
 * @returns YAML配置响应
 */
export const previewContainerYaml = (params: ContainerYamlRequest): Promise<ContainerYamlResponse> => {
  return request.post('/api/containers/preview-yaml', params)
} 