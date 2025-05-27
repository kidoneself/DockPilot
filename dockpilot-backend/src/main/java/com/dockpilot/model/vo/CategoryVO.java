package com.dockpilot.model.vo;

import lombok.Data;

/**
 * 分类展示对象
 */
@Data
public class CategoryVO {
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
     * 该分类下的应用数量
     */
    private Integer appCount;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
} 