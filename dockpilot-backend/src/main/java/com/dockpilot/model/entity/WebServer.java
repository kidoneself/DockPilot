package com.dockpilot.model.entity;

import lombok.Data;

/**
 * Web服务实体类
 */
@Data
public class WebServer {
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
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 应用排序
     */
    private Integer itemSort;

    /**
     * 背景色（支持transparent、rgba、渐变等）
     */
    private String bgColor;

    /**
     * 卡片类型（normal、text）
     */
    private String cardType;

    /**
     * 图标类型（image、text、icon）
     */
    private String iconType;

    /**
     * 打开方式（current、new）
     */
    private String openType;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
} 