package com.dockpilot.model.entity;

import lombok.Data;

/**
 * 分类实体类
 */
@Data
public class Category {
    /**
     * 分类ID
     */
    private Integer id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
} 