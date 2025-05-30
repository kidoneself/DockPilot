package com.dockpilot.model.application.dto;

import lombok.Data;
import java.util.List;

/**
 * 应用部署结果DTO
 */
@Data
public class ApplicationDeployResult {
    
    /**
     * 部署是否成功
     */
    private boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 创建的容器ID列表
     */
    private List<String> containerIds;
    
    /**
     * 访问地址列表
     */
    private List<AccessUrl> accessUrls;
    
    /**
     * 部署ID（用于跟踪部署状态）
     */
    private String deployId;
    
    /**
     * 访问地址信息
     */
    @Data
    public static class AccessUrl {
        private String name;
        private String url;
        private String description;
    }
    
    /**
     * 创建成功结果
     */
    public static ApplicationDeployResult success(List<String> containerIds, String message) {
        ApplicationDeployResult result = new ApplicationDeployResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setContainerIds(containerIds);
        return result;
    }
    
    /**
     * 创建失败结果
     */
    public static ApplicationDeployResult failure(String message) {
        ApplicationDeployResult result = new ApplicationDeployResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
} 