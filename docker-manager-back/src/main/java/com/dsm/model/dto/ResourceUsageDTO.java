package com.dsm.model.dto;

import lombok.Data;

/**
 * DTO (Data Transfer Object) 用于传输 Docker 容器资源监控信息。
 * 容器详情的监控信息
 */
@Data
public class ResourceUsageDTO {

    /**
     * CPU 使用率（百分比）。
     * 表示当前容器使用的 CPU 相对于系统 CPU 的百分比，值范围通常为 0-100。
     */
    private Double cpuPercent;

    /**
     * 当前内存使用量（单位：字节）。
     * 表示当前容器已使用的内存量。
     */
    private Long memoryUsage;

    /**
     * 容器内存限制（单位：字节）。
     * 表示容器的内存上限，超过该值容器可能会被 OOM（内存溢出）杀死。
     */
    private Long memoryLimit;

    /**
     * 网络接收总量（单位：字节）。
     * 表示容器通过网络接收的总字节数。
     */
    private Long networkRx;

    /**
     * 网络发送总量（单位：字节）。
     * 表示容器通过网络发送的总字节数。
     */
    private Long networkTx;

    /**
     * 容器是否在运行中。
     * 如果 Docker 返回的时间戳存在，则容器正在运行。
     */
    private Boolean running;
}