package com.dockpilot.model.application;

import lombok.Data;

import java.util.List;

/**
 * 应用配置解析结果
 */
@Data
public class ApplicationParseResult {
    
    /**
     * 应用元数据
     */
    private ApplicationMeta meta;
    
    /**
     * 服务列表
     */
    private List<ServiceInfo> services;
    
    /**
     * 镜像列表
     */
    private List<ImageInfo> images;
    
    /**
     * 环境变量列表
     */
    private List<EnvVarInfo> envVars;
    
    /**
     * 应用元数据
     */
    @Data
    public static class ApplicationMeta {
        private String name;
        private String description;
        private String version;
        private String author;
        private String category;
    }
    
    /**
     * 服务信息
     */
    @Data
    public static class ServiceInfo {
        private String name;
        private String image;
        private String description;
        private String configUrl;
        private List<String> ports;
        private List<String> volumes;
    }
    
    /**
     * 镜像信息
     */
    @Data
    public static class ImageInfo {
        private String name;
        private String tag;
        private String fullName;
        private String status;  // exists, not_found, unknown
        private String size;
    }
    
    /**
     * 环境变量信息
     */
    @Data
    public static class EnvVarInfo {
        private String name;
        private String defaultValue;
        private String value;
        private String description;
        private boolean required;
        private boolean sensitive;
    }
} 