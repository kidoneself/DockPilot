package com.dsm.pojo.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 容器信息V2
 */
@Data
@Builder
public class ContainerInfoV2 {
    /**
     * 容器名称
     */
    private String name;

    /**
     * 镜像名称
     */
    private String image;

    /**
     * 容器状态
     */
    private String status;

    /**
     * 创建时间
     */
    private String created;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 端口映射
     */
    private String[] ports;

    /**
     * 卷挂载
     */
    private String[] volumes;

    /**
     * 环境变量
     */
    private Map<String, String> environment;

    /**
     * 容器ID
     */
    private String id;

    /**
     * 容器短ID
     */
    private String shortId;

    /**
     * 容器标签
     */
    private Map<String, String> labels;

    /**
     * 容器配置
     */
    private Map<String, Object> config;

    /**
     * 容器主机配置
     */
    private Map<String, Object> hostConfig;

    /**
     * 容器网络设置
     */
    private Map<String, Object> networkSettings;

    /**
     * 容器状态详情
     */
    private Map<String, Object> state;

    /**
     * 容器资源使用情况
     */
    private Map<String, Object> resources;

    /**
     * 容器健康检查状态
     */
    private Map<String, Object> health;

    /**
     * 容器日志配置
     */
    private Map<String, Object> logConfig;

    /**
     * 容器重启策略
     */
    private Map<String, Object> restartPolicy;

    /**
     * 容器安全选项
     */
    private Map<String, Object> securityOpts;

    /**
     * 容器存储驱动
     */
    private String storageDriver;

    /**
     * 容器驱动程序
     */
    private String driver;

    /**
     * 容器平台
     */
    private String platform;

    /**
     * 容器架构
     */
    private String architecture;

    /**
     * 容器操作系统
     */
    private String os;

    /**
     * 容器版本
     */
    private String version;

    /**
     * 容器主机名
     */
    private String hostname;

    /**
     * 容器域名
     */
    private String domainname;

    /**
     * 容器用户
     */
    private String user;

    /**
     * 容器工作目录
     */
    private String workingDir;

    /**
     * 容器入口点
     */
    private String[] entrypoint;

    /**
     * 容器命令
     */
    private String[] cmd;

    /**
     * 容器环境变量
     */
    private String[] env;

    /**
     * 容器挂载点
     */
    private String[] mounts;

    /**
     * 容器网络模式
     */
    private String networkMode;

    /**
     * 容器端口绑定
     */
    private Map<String, Object> portBindings;

    /**
     * 容器链接
     */
    private String[] links;

    /**
     * 容器依赖
     */
    private String[] dependsOn;

    /**
     * 容器扩展配置
     */
    private Map<String, Object> extraConfig;
} 