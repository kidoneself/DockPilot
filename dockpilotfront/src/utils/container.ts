import type {
  ContainerForm,
  PortMapping,
  VolumeMapping,
  DeviceMapping,
  Label
} from '@/types/container'

/**
 * 表单数据转为更新容器请求参数
 */
export function mapFormDataToUpdateRequest(formData: ContainerForm) {
    const updateParams: Record<string, any> = {}
    
    // 🔍 调试日志
    console.log('🔧 mapFormDataToUpdateRequest - 输入数据:', formData)
    
    // 基本信息 - 特别检查 image 字段
    if (!formData.image || formData.image.trim() === '') {
        throw new Error('镜像名称不能为空，请检查表单数据')
    }
    
    // 基本信息
    if (formData.image) updateParams.image = formData.image
    if (formData.tag) updateParams.tag = formData.tag
    if (formData.name) updateParams.name = formData.name
    if (formData.autoRemove !== undefined) updateParams.autoRemove = formData.autoRemove
    if (formData.restartPolicy) updateParams.restartPolicy = formData.restartPolicy
    
    // 网络配置
    if (formData.portMappings?.length) {
        console.log('🔧 处理端口映射数据:', formData.portMappings)
        
        // 过滤掉无效的端口映射
        const validPortMappings = formData.portMappings
            .filter((port: PortMapping) => {
                // 容器端口必须存在且为有效数字
                const isValidContainerPort = port.containerPort && 
                                           port.containerPort.trim() !== '' && 
                                           !isNaN(Number(port.containerPort))
                console.log(`🔧 端口验证: ${port.containerPort} -> ${isValidContainerPort}`)
                return isValidContainerPort
            })
            .map((port: PortMapping) => {
                const result = {
                    containerPort: parseInt(port.containerPort),
                    protocol: port.protocol || 'tcp',
                    ip: port.ip || ''
                } as any
                
                // 只有当主机端口有效时才添加
                if (port.hostPort && port.hostPort.trim() !== '' && !isNaN(Number(port.hostPort))) {
                    result.hostPort = parseInt(port.hostPort)
                }
                
                console.log('🔧 处理后的端口映射:', result)
                return result
            })
        
        if (validPortMappings.length > 0) {
            updateParams.portMappings = validPortMappings
            console.log('🔧 最终端口映射数据:', validPortMappings)
        } else {
            console.log('🔧 没有有效的端口映射数据')
        }
    }
    
    if (formData.networkMode) updateParams.networkMode = formData.networkMode
    if (formData.ipAddress) updateParams.ipAddress = formData.ipAddress
    
    // 存储配置
    if (formData.volumeMappings?.length) {
        updateParams.volumeMounts = formData.volumeMappings.map((volume: VolumeMapping) => ({
            hostPath: volume.hostPath,
            containerPath: volume.containerPath,
            readOnly: volume.readOnly
        }))
    }
    
    // 设备映射
    if (formData.devices?.length) {
        updateParams.devices = formData.devices.map(
          (d: DeviceMapping) => `${d.hostPath}:${d.containerPath}`
        )
    }
    
    // 环境变量
    if (formData.environmentVariables?.length) {
        updateParams.environmentVariables = formData.environmentVariables
    }
    
    // 安全配置
    if (formData.privileged !== undefined) updateParams.privileged = formData.privileged
    if (formData.capAdd?.length) updateParams.capAdd = formData.capAdd
    if (formData.capDrop?.length) updateParams.capDrop = formData.capDrop
    
    // 资源限制
    if (formData.memoryLimit) {
        updateParams.memory = Number(formData.memoryLimit)
    }
    if (formData.cpuLimit) {
        updateParams.cpuQuota = Number(formData.cpuLimit)
    }
    
    // 运行配置
    if (formData.entrypoint?.length) updateParams.entrypoint = formData.entrypoint
    if (formData.cmd?.length) updateParams.command = formData.cmd
    if (formData.workingDir) updateParams.workingDir = formData.workingDir
    if (formData.user) updateParams.user = formData.user
    
    // 标签
    if (formData.labels?.length) {
        updateParams.labels = formData.labels.reduce((acc: Record<string, string>, cur: Label) => {
            acc[cur.key] = cur.value
            return acc
        }, {})
    }
    
    // 健康检查
    if (formData.healthcheck) {
        updateParams.healthcheck = {
            test: formData.healthcheck.test,
            interval: formData.healthcheck.interval,
            timeout: formData.healthcheck.timeout,
            retries: formData.healthcheck.retries,
            startPeriod: formData.healthcheck.startPeriod
        }
    }
    
    // 🔧 重要：后端期望数据结构为 { containerId, config: { ... } }
    // 但容器ID需要在调用时单独传递，这里只返回配置部分
    return updateParams
} 