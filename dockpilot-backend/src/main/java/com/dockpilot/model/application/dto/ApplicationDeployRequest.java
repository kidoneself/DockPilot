package com.dockpilot.model.application.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 应用部署请求DTO
 */
@Data
public class ApplicationDeployRequest {
    
    /**
     * 应用名称（用户自定义）
     */
    private String appName;
    
    /**
     * 环境变量配置
     */
    private Map<String, String> envVars;
    
    /**
     * 数据目录配置
     */
    private String dataDir;
    
    /**
     * 其他配置参数
     */
    private Map<String, Object> configs;
} 