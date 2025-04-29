package com.dsm.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 镜像状态实体类
 * 用于存储镜像的更新状态信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageStatus {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 仓库名称，如 jxxghp/moviepilot-v2
     */
    private String name;

    /**
     * 镜像标签，如 latest、v1.0.0
     */
    private String tag;

    /**
     * 本地镜像创建时间
     */
    private String localCreateTime;

    /**
     * 远程镜像创建时间
     */
    private String remoteCreateTime;

    /**
     * 是否需要更新
     */
    private Boolean needUpdate;

    /**
     * 上次检查时间 - 存储为ISO8601格式日期字符串
     */
    private String lastChecked;

    /**
     * 创建时间 - 存储为ISO8601格式日期字符串
     */
    private String createdAt;

    /**
     * 更新时间 - 存储为ISO8601格式日期字符串
     */
    private String updatedAt;
} 