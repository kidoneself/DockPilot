package com.dockpilot.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 分类数据传输对象
 */
@Data
public class CategoryDTO {
    /**
     * 分类ID（更新时使用）
     */
    private Integer id;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    private String name;

    /**
     * 排序权重
     */
    private Integer sortOrder;
} 