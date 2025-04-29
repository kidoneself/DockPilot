package com.dsm.pojo.request;

import lombok.Data;

/**
 * 环境变量V2
 */
@Data
public class EnvironmentVariable {
    /**
     * 环境变量键
     */
    private String key;

    /**
     * 环境变量值
     */
    private String value;

    /**
     * 环境变量类型
     */
    private String type;

    /**
     * 是否加密
     */
    private boolean encrypted = false;

    /**
     * 加密算法
     */
    private String encryptionAlgorithm;

    /**
     * 环境变量描述
     */
    private String description;

    /**
     * 是否必需
     */
    private boolean required = false;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 验证规则
     */
    private String validationRule;

    /**
     * 环境变量组
     */
    private String group;

    /**
     * 环境变量标签
     */
    private String[] tags;
} 