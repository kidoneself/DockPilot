package com.dsm.pojo.dto;

import lombok.Data;

/**
 * 系统设置 DTO
 */
@Data
public class SystemSettingDTO {
    /**
     * 设置项的键
     */
    private String key;

    /**
     * 设置项的值
     */
    private String value;
} 