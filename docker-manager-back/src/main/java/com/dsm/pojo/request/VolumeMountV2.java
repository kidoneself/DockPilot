package com.dsm.pojo.request;

import lombok.Data;

/**
 * 卷挂载V2
 */
@Data
public class VolumeMountV2 {
    /**
     * 主机路径
     */
    private String hostPath;

    /**
     * 容器路径
     */
    private String containerPath;

    /**
     * 挂载模式（ro/rw）
     */
    private String mode = "rw";

    /**
     * 是否只读
     */
    private boolean readOnly = false;

    /**
     * 是否创建目录
     */
    private boolean createDirectory = false;

    /**
     * 卷名称
     */
    private String volumeName;

    /**
     * 卷驱动
     */
    private String volumeDriver;

    /**
     * 卷选项
     */
    private String volumeOptions;

    /**
     * 挂载标签
     */
    private String mountLabel;

    /**
     * 挂载传播模式
     */
    private String propagation;

    /**
     * 挂载描述
     */
    private String description;
} 