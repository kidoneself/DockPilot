package com.dsm.pojo.dto.image;

import lombok.Data;

import java.util.List;

/**
 * 镜像根文件系统信息DTO
 * 包含镜像的根文件系统相关信息
 */
@Data
public class RootFSDTO {
    /**
     * 文件系统类型
     */
    private String type;
    /**
     * 镜像层列表
     */
    private List<String> layers;
    /**
     * 差异ID列表
     */
    private List<String> diffIds;
} 