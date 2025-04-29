package com.dsm.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 镜像状态DTO
 * 用于前端展示镜像状态信息（是否需要更新）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageStatusDTO {

    /**
     * 镜像ID
     */
    private String id;

    /**
     * 镜像状态记录ID
     */
    private Long statusId;

    /**
     * 仓库名称
     */
    private String name;

    /**
     * 镜像标签
     */
    private String tag;

    /**
     * 镜像大小
     */
    private Long size;

    /**
     * 创建时间
     */
    private Date created;

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
     * 上次检查时间
     */
    private Date lastChecked;
} 