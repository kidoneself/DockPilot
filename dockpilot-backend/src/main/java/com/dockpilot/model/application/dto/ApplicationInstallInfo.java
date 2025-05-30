package com.dockpilot.model.application.dto;

import lombok.Data;
import java.util.List;

/**
 * 应用安装信息DTO
 */
@Data
public class ApplicationInstallInfo {
    
    /**
     * 应用基本信息
     */
    private AppBasicInfo app;
    
    /**
     * 镜像列表
     */
    private List<ImageStatusInfo> images;
    
    /**
     * 环境变量列表
     */
    private List<EnvVarInfo> envVars;
    
    /**
     * 服务列表
     */
    private List<ServiceInfo> services;
    
    /**
     * 应用基本信息
     */
    @Data
    public static class AppBasicInfo {
        private Long id;
        private String name;
        private String description;
        private String type;
        private String icon;
        private Integer deployCount;
    }
    
    /**
     * 镜像状态信息
     */
    @Data
    public static class ImageStatusInfo {
        private String name;
        private String size;
        private String status; // exists, missing, pulling, success, failed
    }
    
    /**
     * 环境变量信息
     */
    @Data
    public static class EnvVarInfo {
        private String name;
        private String description;
        private String value;
        private String defaultValue;
        private boolean required;
        private boolean sensitive;
    }
    
    /**
     * 服务信息
     */
    @Data
    public static class ServiceInfo {
        private String name;
        private String image;
        private String configUrl;
    }
} 