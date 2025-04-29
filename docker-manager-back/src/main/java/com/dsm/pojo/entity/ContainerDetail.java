package com.dsm.pojo.entity;

import lombok.Data;

import java.util.List;

/**
 * 前后端详情，编辑，创建用的对象
 */
@Data
public class ContainerDetail {
    private String image;
    private String name;
    private String restartPolicy;
    private String networkMode;

    private List<PortMapping> portMappings;
    private List<VolumeMapping> volumeMappings;
    private List<EnvironmentVariable> environmentVariables;

    private Boolean privileged;

    // 将环境变量列表转换为字符串列表
    public List<String> getEnvList() {
        return environmentVariables.stream().map(env -> env.getKey() + "=" + env.getValue()).collect(java.util.stream.Collectors.toList());
    }

    // 将卷映射列表转换为Docker绑定字符串列表
    public List<String> getBinds() {
        return volumeMappings.stream().map(vol -> vol.getHostPath() + ":" + vol.getContainerPath() + (vol.getReadOnly() ? ":ro" : ":rw")).collect(java.util.stream.Collectors.toList());
    }

    @Data
    public static class PortMapping {
        private Integer hostPort;
        private Integer containerPort;
        private String protocol;
    }

    @Data
    public static class VolumeMapping {
        private String hostPath;
        private String containerPath;
        private Boolean readOnly;
    }

    @Data
    public static class EnvironmentVariable {
        private String key;
        private String value;
    }
} 