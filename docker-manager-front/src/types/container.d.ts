/*
 * 容器相关类型定义
 * 生成时间：2024-06-09 21:00:00
 */

export interface ValidationResult {
    valid: boolean;
    message: string;
}

export interface PortMapping {
    hostPort: string;
    containerPort: string;
    protocol: string;
    ip?: string;
    validationResult?: ValidationResult;
}

export interface VolumeMapping {
    hostPath: string;
    containerPath: string;
    readOnly: boolean;
    isDefaultVolume?: boolean;
}

export interface DeviceMapping {
    hostPath: string;
    containerPath: string;
}

export interface EnvironmentVariable {
    key: string;
    value: string;
}

export interface Label {
    key: string;
    value: string;
}

export interface HealthCheck {
    test: string[];
    interval: string;
    timeout: string;
    retries: number;
    startPeriod: string;
}

export interface CreateContainerParams {
    image: string;
    tag: string;
    autoPull?: boolean;
    name: string;
    autoRemove?: boolean;
    restartPolicy?: string;
    portMappings: Array<{
        ip?: string;
        hostPort: string;
        containerPort: string;
        protocol?: string;
    }>;
    networkMode?: string;
    dns?: string[];
    dnsSearch?: string[];
    extraHosts?: string[];
    ipAddress?: string;
    macAddress?: string;
    volumeMounts: Array<{
        hostPath: string;
        containerPath: string;
        readOnly: boolean;
    }>;
    tmpfs?: string[];
    shmSize?: string;
    environmentVariables: Array<{
        key: string;
        value: string;
    }>;
    memory?: number;
    memorySwap?: number;
    cpuShares?: number;
    cpuPeriod?: number;
    cpuQuota?: number;
    cpusetCpus?: string;
    privileged?: boolean;
    capAdd?: string[];
    capDrop?: string[];
    securityOpt?: string[];
    devices?: string[];
    user?: string;
    groupAdd?: string[];
    workingDir?: string;
    command?: string;
    entrypoint?: string[];
    stopSignal?: string;
    stopTimeout?: number;
    labels?: Record<string, string>;
    ipcMode?: string;
    pidMode?: string;
    utsMode?: string;
    sysctls?: Record<string, string>;
    healthcheck?: HealthCheck;
}

export interface ContainerForm {
    image: string;
    tag: string;
    autoPull: boolean;
    name: string;
    autoRemove: boolean;
    restartPolicy: string;
    portMappings: PortMapping[];
    networkMode: string;
    ipAddress: string;
    gateway: string;
    volumeMappings: VolumeMapping[];
    devices: DeviceMapping[];
    environmentVariables: EnvironmentVariable[];
    privileged: boolean;
    capAdd: string[];
    capDrop: string[];
    memoryLimit: string;
    cpuLimit: string;
    entrypoint: string[];
    cmd: string[];
    workingDir: string;
    user: string;
    labels: Label[];
    healthcheck: HealthCheck;
} 