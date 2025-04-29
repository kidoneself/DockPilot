/*
 * 容器表单数据转换工具函数
 * 生成时间：2024-06-09 21:15:00
 */

import type { ContainerForm, CreateContainerParams } from '@/types/container.d.ts';

/**
 * 表单数据转为创建容器请求参数
 */
export function mapFormDataToRequest(formData: ContainerForm): CreateContainerParams {
    return {
        image: formData.image,
        tag: formData.tag,
        autoPull: formData.autoPull,
        name: formData.name,
        autoRemove: formData.autoRemove,
        restartPolicy: formData.restartPolicy,
        portMappings: formData.portMappings,
        networkMode: formData.networkMode,
        ipAddress: formData.ipAddress,
        volumeMounts: formData.volumeMappings,
        devices: formData.devices?.map(d => `${d.hostPath}:${d.containerPath}`),
        environmentVariables: formData.environmentVariables,
        privileged: formData.privileged,
        capAdd: formData.capAdd,
        capDrop: formData.capDrop,
        memory: formData.memoryLimit ? Number(formData.memoryLimit) : undefined,
        cpuQuota: formData.cpuLimit ? Number(formData.cpuLimit) : undefined,
        entrypoint: formData.entrypoint,
        command: formData.cmd?.join(' '),
        workingDir: formData.workingDir,
        user: formData.user,
        labels: formData.labels?.reduce((acc, cur) => {
            acc[cur.key] = cur.value;
            return acc;
        }, {} as Record<string, string>),
        healthcheck: formData.healthcheck,
    };
}

/**
 * 容器详情转为表单数据
 */
export function mapContainerDetailToForm(detail: any): ContainerForm {
    return {
        image: detail.imageName?.split(':')[0] || '',
        tag: detail.imageName?.split(':')[1] || 'latest',
        autoPull: false,
        name: detail.containerName || '',
        autoRemove: false,
        restartPolicy: detail.restartPolicyName || '',
        portMappings: detail.ports?.map((port: string) => {
            const [containerPort, hostPort] = port.split(':');
            const [portNumber, protocol] = containerPort.split('/');
            return {
                hostPort,
                containerPort: portNumber,
                protocol: protocol || 'tcp',
                ip: ''
            };
        }) || [],
        networkMode: detail.networkMode || '',
        ipAddress: detail.ipAddress || '',
        gateway: detail.networkSettings?.Gateway || '',
        volumeMappings: detail.volumes?.map((volume: any) => {
            if (volume && typeof volume === 'object') {
                return {
                    hostPath: volume.hostPath || '',
                    containerPath: volume.containerPath || '',
                    readOnly: volume.readOnly || false
                };
            }
            return {
                hostPath: '',
                containerPath: '',
                readOnly: false
            };
        }) || [],
        devices: detail.devices?.map((device: string) => {
            const [hostPath, containerPath] = device.split(':');
            return {
                hostPath,
                containerPath
            };
        }) || [],
        environmentVariables: detail.envs?.map((env: string) => {
            const [key, value] = env.split('=');
            return { key, value };
        }) || [],
        privileged: detail.privileged || false,
        capAdd: detail.capAdd || [],
        capDrop: detail.capDrop || [],
        memoryLimit: '',
        cpuLimit: '',
        entrypoint: detail.entrypoints || [],
        cmd: typeof detail.command === 'string' ? detail.command.split(' ').filter(Boolean) : [],
        workingDir: detail.workingDir || '',
        user: '',
        labels: Object.entries(detail.labels || {}).map(([key, value]) => ({
            key,
            value: value as string
        })),
        healthcheck: {
            test: [],
            interval: '',
            timeout: '',
            retries: 0,
            startPeriod: ''
        }
    };
} 