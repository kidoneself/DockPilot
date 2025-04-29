package com.dsm.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ContainerStaticInfoDTO {
    private String containerId;      // 容器ID
    private String containerName;    // 容器名
    private String imageName;        // 镜像名
    private String imageId;          // 镜像ID
    private String createdTime;      // 容器创建时间
    private String status;           // 容器状态 (running, exited...)
    private Integer restartCount;    // 重启次数
    private String command;          // 启动命令
    private String workingDir;       // 工作目录
    private List<String> entrypoints; // 入口点
    private Map<String, String> labels; // 标签
    private List<String> envs;        // 环境变量
    private List<VolumeMapping> volumes;     // 挂载卷路径
    private List<String> ports;       // 端口映射
    private List<String> exposedPorts; // 暴露端口
    private List<String> devices;     // 设备映射
    private String networkMode;       // 网络模式
    private String ipAddress;         // 容器IP
    private String restartPolicyName;    // 重启策略名 (always, on-failure, unless-stopped, no)
    private Integer restartPolicyMaxRetry; // 重启策略最大重试次数
    private Boolean privileged;          // 是否特权模式
}