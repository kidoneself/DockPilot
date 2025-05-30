package com.dockpilot.model;

import lombok.Data;

/**
 * 容器生成YAML响应DTO
 */
@Data
public class ContainerYamlResponse {
    
    /**
     * 生成的YAML内容
     */
    private String yamlContent;
    
    /**
     * 容器数量
     */
    private Integer containerCount;
    
    /**
     * 项目名称
     */
    private String projectName;
    
    /**
     * 生成时间
     */
    private String generateTime;
    
    /**
     * 成功标识
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    public static ContainerYamlResponse success(String yamlContent, Integer containerCount, String projectName) {
        ContainerYamlResponse response = new ContainerYamlResponse();
        response.setYamlContent(yamlContent);
        response.setContainerCount(containerCount);
        response.setProjectName(projectName);
        response.setSuccess(true);
        response.setMessage("YAML生成成功");
        response.setGenerateTime(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return response;
    }
    
    public static ContainerYamlResponse error(String message) {
        ContainerYamlResponse response = new ContainerYamlResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setGenerateTime(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return response;
    }
} 