package com.dockpilot.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Docker Events 监听配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "docker.events")
public class DockerEventsConfig {
    
    /**
     * 是否启用Docker Events监听
     */
    private boolean enabled = true;
    
    /**
     * 监听异常时是否自动重启
     */
    private boolean autoRestart = true;
    
    /**
     * 重启延迟时间（毫秒）
     */
    private long restartDelay = 5000;
} 