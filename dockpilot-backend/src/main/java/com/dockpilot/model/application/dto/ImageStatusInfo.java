package com.dockpilot.model.application.dto;

import lombok.Data;

/**
 * 镜像状态信息DTO
 */
@Data
public class ImageStatusInfo {
    
    /**
     * 镜像名称
     */
    private String name;
    
    /**
     * 镜像大小
     */
    private String size;
    
    /**
     * 镜像状态
     * exists - 已存在
     * missing - 需要拉取
     * pulling - 拉取中
     * success - 拉取成功
     * failed - 拉取失败
     */
    private String status;
    
    public static ImageStatusInfo exists(String name, String size) {
        ImageStatusInfo info = new ImageStatusInfo();
        info.setName(name);
        info.setSize(size);
        info.setStatus("exists");
        return info;
    }
    
    public static ImageStatusInfo missing(String name) {
        ImageStatusInfo info = new ImageStatusInfo();
        info.setName(name);
        info.setSize("");
        info.setStatus("missing");
        return info;
    }
} 