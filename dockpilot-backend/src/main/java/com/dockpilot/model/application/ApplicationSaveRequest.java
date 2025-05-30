package com.dockpilot.model.application;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 保存应用请求
 */
@Data
public class ApplicationSaveRequest {
    
    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空")
    private String name;
    
    /**
     * 应用描述
     */
    private String description;
    
    /**
     * 应用分类
     */
    private String category = "容器应用";
    
    /**
     * 应用图标URL
     */
    private String iconUrl;
    
    /**
     * 容器ID列表 (用于从容器保存)
     */
    private List<String> containerIds;
    
    /**
     * YAML内容 (用于导入)
     */
    private String yamlContent;
} 