package com.dockpilot.model.application.dto;

import lombok.Data;

import java.util.List;

/**
 * 应用保存请求DTO
 */
@Data
public class ApplicationSaveRequest {
    
    /**
     * 应用名称
     */
    private String name;
    
    /**
     * 应用描述
     */
    private String description;
    
    /**
     * 应用分类
     */
    private String category;
    
    /**
     * 图标URL
     */
    private String iconUrl;
    
    /**
     * 容器ID列表（从容器创建应用时使用）
     */
    private List<String> containerIds;
    
    /**
     * YAML配置内容（直接导入时使用）
     */
    private String yamlContent;
    
    /**
     * 环境变量配置
     */
    private String envVars;
} 