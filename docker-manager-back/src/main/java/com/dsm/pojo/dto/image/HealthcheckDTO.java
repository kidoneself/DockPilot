package com.dsm.pojo.dto.image;

import lombok.Data;

import java.util.List;

/**
 * 容器健康检查配置DTO
 * 包含容器的健康检查相关配置信息
 */
@Data
public class HealthcheckDTO {
    /**
     * 健康检查命令列表
     */
    private List<String> test;
    /**
     * 健康检查间隔时间（毫秒）
     */
    private Long interval;
    /**
     * 健康检查超时时间（毫秒）
     */
    private Long timeout;
    /**
     * 健康检查重试次数
     */
    private Integer retries;
    /**
     * 健康检查启动等待时间（毫秒）
     */
    private Long startPeriod;
} 