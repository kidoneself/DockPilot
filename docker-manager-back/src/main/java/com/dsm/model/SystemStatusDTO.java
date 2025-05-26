package com.dsm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统状态DTO
 * 用于返回宿主机的系统状态信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStatusDTO {

    /**
     * 主机名
     */
    private String hostname;

    /**
     * 操作系统信息
     */
    private String os;

    /**
     * 内核版本
     */
    private String kernel;

    /**
     * 系统运行时间
     */
    private String uptime;

    /**
     * CPU核心数
     */
    private Integer cpuCores;

    /**
     * CPU型号
     */
    private String cpuModel;

    /**
     * CPU使用率 (百分比)
     */
    private Double cpuUsage;

    /**
     * 内存总量 (MB)
     */
    private Long memoryTotal;

    /**
     * 内存使用量 (MB)
     */
    private Long memoryUsed;

    /**
     * 内存使用百分比
     */
    private Double memoryUsage;

    /**
     * 磁盘使用百分比
     */
    private String diskUsage;

    /**
     * 磁盘剩余空间
     */
    private String diskFree;

    /**
     * 主机IP地址
     */
    private String ipAddress;

    /**
     * 默认网关
     */
    private String gateway;

    /**
     * 运行中的容器数量
     */
    private Integer runningContainers;

    /**
     * 总容器数量
     */
    private Integer totalContainers;

    /**
     * 镜像数量
     */
    private Integer totalImages;

    /**
     * 网络数量
     */
    private Integer totalNetworks;

    /**
     * Docker版本
     */
    private String dockerVersion;

    /**
     * 网络下载速度（格式化显示）
     */
    private String networkDownloadSpeed;

    /**
     * 网络上传速度（格式化显示）
     */
    private String networkUploadSpeed;

    /**
     * 网络下载速度（字节/秒）
     */
    private Double networkDownloadSpeedRaw;

    /**
     * 网络上传速度（字节/秒）
     */
    private Double networkUploadSpeedRaw;
} 