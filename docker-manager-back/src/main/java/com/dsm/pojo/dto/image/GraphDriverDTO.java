package com.dsm.pojo.dto.image;

import lombok.Data;

import java.util.Map;

/**
 * 镜像存储驱动信息DTO
 * 包含镜像使用的存储驱动类型和相关数据
 */
@Data
public class GraphDriverDTO {
    /**
     * 存储驱动名称，如overlay2、aufs等
     */
    private String name;
    /**
     * 存储驱动相关数据
     */
    private Map<String, String> data;
} 