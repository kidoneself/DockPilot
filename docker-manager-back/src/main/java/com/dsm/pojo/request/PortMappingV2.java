package com.dsm.pojo.request;

import lombok.Data;

/**
 * 端口映射V2
 */
@Data
public class PortMappingV2 {
    /**
     * 主机IP地址
     */
    private String ip;

    /**
     * 主机端口
     */
    private String hostPort;

    /**
     * 容器端口
     */
    private String containerPort;

    /**
     * 协议类型（tcp/udp）
     */
    private String protocol = "tcp";

    /**
     * 是否发布所有端口
     */
    private boolean publishAllPorts = false;

    /**
     * 是否随机端口
     */
    private boolean randomPort = false;

    /**
     * 端口范围
     */
    private String portRange;

    /**
     * 端口描述
     */
    private String description;
} 