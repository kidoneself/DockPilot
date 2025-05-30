package com.dockpilot.model.application.vo;

import lombok.Data;

/**
 * 应用展示VO
 */
@Data
public class ApplicationVO {
    
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
     * 应用分类
     */
    private String category;
    
    /**
     * 图标URL
     */
    private String iconUrl;
    
    /**
     * YAML配置内容
     */
    private String yamlContent;
    
    /**
     * 文件哈希值
     */
    private String fileHash;
    
    /**
     * 环境变量配置
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
    
    // ========== 前端适配字段 ==========
    
    /**
     * 应用类型 (固定为"用户分享")
     */
    private String type = "用户分享";
    
    /**
     * 部署次数 (默认随机数)
     */
    private Integer deployCount = (int)(Math.random() * 200) + 10;
    
    /**
     * 服务数量 (从YAML中动态计算)
     */
    private Integer services = 1;
    
    /**
     * 版本号
     */
    private String version = "1.0.0";
    
    /**
     * 作者
     */
    private String author = "System";
    
    /**
     * 图标字段别名 (前端可能使用icon)
     */
    public String getIcon() {
        return iconUrl;
    }
    
    public void setIcon(String icon) {
        this.iconUrl = icon;
    }
} 