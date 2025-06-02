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
  includeConfigPackages?: boolean  // 是否包含配置包（可选）
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
 * 项目导出请求参数（继承YAML请求参数）
 */
export interface ProjectExportRequest extends ContainerYamlRequest {
  includeConfigPackages: boolean  // 是否包含配置包
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

/**
 * 导出项目包（YAML + 可选配置文件）
 * @param params 导出请求参数
 * @returns 导出的文件Blob
 */
export const exportProject = async (params: ProjectExportRequest): Promise<{ blob: Blob, filename: string }> => {
  const response = await fetch('/api/api/containers/export-project', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(params)
  })

  if (!response.ok) {
    throw new Error(`导出失败: ${response.status} ${response.statusText}`)
  }

  // 🔥 改进文件名解析，支持UTF-8编码
  const contentDisposition = response.headers.get('Content-Disposition')
  let filename = 'docker-project.yml'
  
  if (contentDisposition) {
    // 优先尝试解析UTF-8编码的文件名（RFC 5987标准）
    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/)
    if (utf8Match) {
      try {
        filename = decodeURIComponent(utf8Match[1])
      } catch (e) {
        console.warn('UTF-8文件名解码失败:', e)
        // 降级到基本文件名解析
        const basicMatch = contentDisposition.match(/filename="?([^"]+)"?/)
        if (basicMatch) {
          filename = basicMatch[1]
        }
      }
    } else {
      // 如果没有UTF-8编码版本，使用基本版本
      const basicMatch = contentDisposition.match(/filename="?([^"]+)"?/)
      if (basicMatch) {
        filename = basicMatch[1]
      }
    }
  }

  const blob = await response.blob()
  return { blob, filename }
}

/**
 * 只导出YAML文件
 * @param params 导出请求参数
 * @returns 导出的YAML文件Blob
 */
export const exportYamlOnly = async (params: ContainerYamlRequest): Promise<{ blob: Blob, filename: string }> => {
  const exportParams: ProjectExportRequest = {
    ...params,
    includeConfigPackages: false
  }
  
  return exportProject(exportParams)
} 