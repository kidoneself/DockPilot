package com.dockpilot.model.application;

import lombok.Data;

/**
 * 应用响应VO
 */
@Data
public class ApplicationVO {
    
    private Long id;
    private String name;
    private String description;
    private String category;
    private String iconUrl;
    private String createdAt;
    private String updatedAt;
    
    /**
     * 从Application实体转换为VO
     */
    public static ApplicationVO fromEntity(Application application) {
        if (application == null) {
            return null;
        }
        
        ApplicationVO vo = new ApplicationVO();
        vo.setId(application.getId());
        vo.setName(application.getName());
        vo.setDescription(application.getDescription());
        vo.setCategory(application.getCategory());
        vo.setIconUrl(application.getIconUrl());
        vo.setCreatedAt(application.getCreatedAt());
        vo.setUpdatedAt(application.getUpdatedAt());
        return vo;
    }
} 