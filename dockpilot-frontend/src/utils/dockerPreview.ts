import type { ContainerFormData } from '@/types/container'
import yaml from 'js-yaml'

/**
 * 生成 Docker Run 命令
 * @param formData 容器表单数据
 * @returns Docker Run 命令字符串
 */
export function generateDockerRunCommand(formData: ContainerFormData): string {
  console.log('完整的 ContainerFormData:', JSON.stringify(formData, null, 2))
  const parts: string[] = ['docker run']

  // 添加容器名称
  if (formData.name) {
    parts.push(`  --name ${formData.name} \\`)
  }

  // 添加重启策略
  if (formData.restartPolicy && formData.restartPolicy !== 'no') {
    parts.push(`  --restart ${formData.restartPolicy} \\`)
  }

  // 添加工作目录
  // if (formData.workingDir) {
  //   parts.push(`  -w ${formData.workingDir} \\`)
  // }

  // 添加网络模式
  if (formData.networkMode) {
    parts.push(`  --network ${formData.networkMode} \\`)
  }

  // 添加端口映射
  console.log('Port mappings:', formData.portMappings)
  if (formData.portMappings && formData.portMappings.length > 0) {
    formData.portMappings.forEach(port => {
      console.log('Processing port:', port)
      if (port.hostPort && port.containerPort) {
        parts.push(`  -p ${port.hostPort}:${port.containerPort}/${port.protocol || 'tcp'} \\`)
      } else if (port.containerPort) {
        parts.push(`  -p ${port.containerPort}/${port.protocol || 'tcp'} \\`)
      }
    })
  } else {
    console.log('No port mappings available')
  }

  // 添加环境变量
  formData.environmentVariables?.forEach(env => {
    if (env.key && env.value) {
      parts.push(`  -e ${env.key}=${env.value} \\`)
    }
  })

  // 添加数据卷
  formData.volumeMappings?.forEach(volume => {
    if (volume.hostPath && volume.containerPath) {
      const readonly = volume.readOnly ? ':ro' : ''
      parts.push(`  -v ${volume.hostPath}:${volume.containerPath}${readonly} \\`)
    }
  })

  // 添加镜像
  if (formData.image) {
    parts.push(`  ${formData.image}`)
  }

  // 添加命令
  if (formData.cmd && formData.cmd.length > 0) {
    parts.push(`  ${formData.cmd.join(' ')}`)
  }

  return parts.join('\n')
}

/**
 * 生成 Docker Compose 配置
 * @param formData 容器表单数据
 * @returns Docker Compose YAML 配置字符串
 */
export function generateDockerComposeConfig(formData: ContainerFormData): string {
  console.log('完整的 ContainerFormData:', JSON.stringify(formData, null, 2))
  // 创建基础配置对象
  const serviceConfig: Record<string, any> = {}

  // 只添加非空字段
  if (formData.image) {
    serviceConfig.image = formData.image
  }
  if (formData.name) {
    serviceConfig.container_name = formData.name
  }
    // if (formData.workingDir) {
    //   serviceConfig.working_dir = formData.workingDir
    // }
  if (formData.restartPolicy && formData.restartPolicy !== 'no') {
    serviceConfig.restart = formData.restartPolicy
  }
  if (formData.networkMode) {
    serviceConfig.network_mode = formData.networkMode
  }

  // 处理端口映射
  const ports = formData.portMappings?.map(port => {
    if (port.hostPort && port.containerPort) {
      return `${port.hostPort}:${port.containerPort}/${port.protocol || 'tcp'}`
    }
    if (port.containerPort) {
      return `${port.containerPort}/${port.protocol || 'tcp'}`
    }
    return null
  }).filter(Boolean)
  if (ports && ports.length > 0) {
    serviceConfig.ports = ports
  }

  // 处理环境变量
  const environment = formData.environmentVariables?.reduce((acc, env) => {
    if (env.key && env.value) {
      acc[env.key] = env.value
    }
    return acc
  }, {} as Record<string, string>)
  if (environment && Object.keys(environment).length > 0) {
    serviceConfig.environment = environment
  }

  // 处理数据卷
  const volumes = formData.volumeMappings?.map(volume => {
    if (volume.hostPath && volume.containerPath) {
      return `${volume.hostPath}:${volume.containerPath}${volume.readOnly ? ':ro' : ''}`
    }
    return null
  }).filter(Boolean)
  if (volumes && volumes.length > 0) {
    serviceConfig.volumes = volumes
  }

  // 处理命令
  if (formData.cmd && formData.cmd.length > 0) {
    serviceConfig.command = formData.cmd
  }

  // 创建最终的配置对象
  const config = {
    version: '3',
    services: {
      [formData.name || 'app']: serviceConfig
    }
  }

  // 将配置转换为 YAML 格式
  return yaml.dump(config, {
    indent: 2,
    lineWidth: -1, // 不限制行宽
    noRefs: true, // 不生成引用
    sortKeys: false // 保持键的顺序
  })
} 