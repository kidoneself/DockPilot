package com.dockpilot.model.application.dto;

import lombok.Data;

/**
 * 拉取镜像请求DTO
 */
@Data
public class PullImageRequest {
    
    /**
     * 镜像名称
     */
    private String imageName;
} 