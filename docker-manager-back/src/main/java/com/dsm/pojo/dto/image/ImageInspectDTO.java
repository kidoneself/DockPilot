package com.dsm.pojo.dto.image;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 镜像详细信息DTO
 * 包含镜像的完整信息，如ID、创建时间、配置等，返回给前端使用
 * ？？？是否太复杂不需要这么多。待商榷
 */
@Data
public class ImageInspectDTO {
    /**
     * 镜像ID
     */
    private String id;
    /**
     * 父镜像ID
     */
    private String parent;
    /**
     * 镜像注释
     */
    private String comment;
    /**
     * 镜像创建时间
     */
    private Date created;
    /**
     * 创建该镜像的容器ID
     */
    private String container;
    /**
     * 容器配置信息
     */
    private ContainerConfigDTO containerConfig;
    /**
     * Docker版本
     */
    private String dockerVersion;
    /**
     * 镜像作者
     */
    private String author;
    /**
     * 镜像配置信息
     */
    private ConfigDTO config;
    /**
     * 操作系统类型
     */
    private String os;
    /**
     * 操作系统版本
     */
    private String osVersion;
    /**
     * 镜像大小（字节）
     */
    private Long size;
    /**
     * 虚拟大小（字节）
     */
    private Long virtualSize;
    /**
     * 存储驱动信息
     */
    private GraphDriverDTO graphDriver;
    /**
     * 根文件系统信息
     */
    private RootFSDTO rootFS;
    /**
     * 镜像标签列表
     */
    private List<String> repoTags;
    /**
     * 镜像摘要列表
     */
    private List<String> repoDigests;
} 