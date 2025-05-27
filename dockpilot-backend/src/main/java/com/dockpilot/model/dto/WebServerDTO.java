package com.dockpilot.model.dto;

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
    @Pattern(regexp = "^$|^https?://[\\w.-]+(:\\d+)?(/[\\w.-]*)*$", message = "内网访问地址格式不正确")
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
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Integer categoryId;

    /**
     * 应用排序
     */
    @NotNull(message = "应用排序不能为空")
    private Integer itemSort;

    /**
     * 背景色（支持transparent、rgba、渐变等）
     */
    @Size(max = 200, message = "背景色设置长度不能超过200个字符")
    private String bgColor;

    /**
     * 卡片类型（normal、text）
     */
    @Pattern(regexp = "^(normal|text)$", message = "卡片类型只能是 normal 或 text")
    private String cardType;

    /**
     * 图标类型（image、text、icon）
     */
    @Pattern(regexp = "^(image|text|icon)$", message = "图标类型只能是 image、text 或 icon")
    private String iconType;

    /**
     * 打开方式（current、new）
     */
    @Pattern(regexp = "^(current|new)$", message = "打开方式只能是 current 或 new")
    private String openType;
} 