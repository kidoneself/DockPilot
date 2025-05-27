package com.dockpilot.model.vo;

import lombok.Data;

/**
 * Web服务视图对象
 */
@Data
public class WebServerVO {
    /**
     * 主键ID（UUID）
     */
    private String id;

    /**
     * 服务器名称
     */
    private String name;

    /**
     * 图标URL
     */
    private String icon;

    /**
     * 内网访问地址
     */
    private String internalUrl;

    /**
     * 外网访问地址
     */
    private String externalUrl;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 分类名称
     */
    private String category;

    /**
     * 应用排序
     */
    private Integer itemSort;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
} 