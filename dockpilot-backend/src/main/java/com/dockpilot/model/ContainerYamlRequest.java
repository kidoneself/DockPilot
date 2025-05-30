package com.dockpilot.model;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * 容器生成YAML请求DTO
 */
@Data
public class ContainerYamlRequest {
    
    /**
     * 容器ID列表
     */
    @NotEmpty(message = "容器ID列表不能为空")
    private List<String> containerIds;
    
    /**
     * 项目名称（可选）
     */
    private String projectName;
    
    /**
     * 项目描述（可选）
     */
    private String description;
    
    /**
     * 排除字段列表（可选）
     */
    private List<String> excludeFields;
    
    /**
     * 环境变量配置（可选）
     * key: 环境变量名称
     * value: 用户配置的描述
     */
    private Map<String, String> envDescriptions;
} 