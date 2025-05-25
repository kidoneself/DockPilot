package com.dsm.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Web服务数据传输对象
 */
@Data
public class WebServerDTO {
    /**
     * 主键ID（UUID）
     */
    private String id;

    /**
     * 服务名称
     */
    @NotBlank(message = "服务名称不能为空")
    @Size(max = 100, message = "服务名称长度不能超过100个字符")
    private String name;

    /**
     * 图标URL
     */
    @Pattern(regexp = "^$|^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$", message = "图标URL格式不正确")
    private String icon;

    /**
     * 内网访问地址
     */
    @NotBlank(message = "内网访问地址不能为空")
    @Pattern(regexp = "^https?://[\\w.-]+(:\\d+)?(/[\\w.-]*)*$", message = "内网访问地址格式不正确")
    private String internalUrl;

    /**
     * 外网访问地址
     */
    @Pattern(regexp = "^$|^https?://[\\w.-]+(:\\d+)?(/[\\w.-]*)*$", message = "外网访问地址格式不正确")
    private String externalUrl;

    /**
     * 描述信息
     */
    @Size(max = 500, message = "描述信息长度不能超过500个字符")
    private String description;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    private String category;

    /**
     * 应用排序
     */
    @NotNull(message = "应用排序不能为空")
    private Integer itemSort;
} 