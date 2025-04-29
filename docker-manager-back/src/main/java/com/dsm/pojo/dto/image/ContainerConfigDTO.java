package com.dsm.pojo.dto.image;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 容器配置信息DTO
 * 包含容器的运行时配置信息，如用户、环境变量、命令等
 */
@Data
public class ContainerConfigDTO {
    /**
     * 运行容器的用户
     */
    private String user;
    /**
     * 是否附加标准输入
     */
    private Boolean attachStdin;
    /**
     * 是否附加标准输出
     */
    private Boolean attachStdout;
    /**
     * 是否附加标准错误
     */
    private Boolean attachStderr;
    /**
     * 是否分配TTY
     */
    private Boolean tty;
    /**
     * 环境变量列表
     */
    private List<String> env;
    /**
     * 容器启动命令
     */
    private List<String> cmd;
    /**
     * 容器入口点
     */
    private List<String> entrypoint;
    /**
     * 镜像名称
     */
    private String image;
    /**
     * 容器标签
     */
    private Map<String, String> labels;
    /**
     * 数据卷配置
     */
    private Map<String, ?> volumes;
    /**
     * 工作目录
     */
    private String workingDir;
    /**
     * 构建触发器列表
     */
    private List<String> onBuild;
    /**
     * 暴露的端口
     */
    private Map<String, Object> exposedPorts;
    /**
     * 健康检查配置
     */
    private HealthcheckDTO healthcheck;
} 