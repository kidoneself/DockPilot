package com.dockpilot.model.application;

import lombok.Data;

/**
 * 应用实体类
 * 存储应用中心的应用配置信息
 */
@Data
public class Application {
    
    /**
     * 应用ID
     */
    private Long id;
    
    /**
     * 应用名称
     */
    private String name;
    
    /**
     * 应用描述
     */
    private String description;
    
    /**
     * 应用分类 (如：Web开发、媒体娱乐等)
     */
    private String category;
    
    /**
     * 应用类型 (官方应用、用户分享)
     */
    private String type = "用户分享";
    
    /**
     * 应用图标URL
     */
    private String iconUrl;
    
    /**
     * 应用版本
     */
    private String version = "1.0.0";
    
    /**
     * 作者
     */
    private String author = "System";
    
    /**
     * YAML配置内容 (完整的应用配置)
     */
    private String yamlContent;
    
    /**
     * 配置文件哈希值 (用于去重检查)
     */
    private String fileHash;
    
    /**
     * 使用次数统计
     */
    private Integer deployCount = 0;
    
    /**
     * 是否为官方应用
     */
    private Boolean isOfficial = false;
    
    /**
     * 是否启用
     */
    private Boolean isEnabled = true;
    
    /**
     * 标签 (逗号分隔)
     */
    private String tags;
    
    /**
     * 用户安装时填写的变量 (JSON格式存储)
     */
    private String envVars;
    
    /**
     * 创建时间
     */
    private String createdAt;
    
    /**
     * 更新时间
     */
    private String updatedAt;
} 