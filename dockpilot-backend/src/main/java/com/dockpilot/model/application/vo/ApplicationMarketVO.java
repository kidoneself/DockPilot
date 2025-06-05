package com.dockpilot.model.application.vo;

import lombok.Data;

/**
 * 应用市场展示VO
 */
@Data
public class ApplicationMarketVO {
    
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
     * 下载URL
     */
    private String downloadUrl;
    
    /**
     * 版本号
     */
    private String version;
    
    /**
     * 作者
     */
    private String author;
    
    // ========== 前端适配字段 ==========
    
    /**
     * 应用类型
     */
    private String type = "应用市场";
    
    /**
     * 部署次数 (随机数)
     */
    private Integer deployCount = (int)(Math.random() * 1000) + 100;
    
    /**
     * 服务数量 (默认1)
     */
    private Integer services = 1;
    
    /**
     * 图标字段别名
     */
    public String getIcon() {
        return iconUrl;
    }
    
    public void setIcon(String icon) {
        this.iconUrl = icon;
    }
} 