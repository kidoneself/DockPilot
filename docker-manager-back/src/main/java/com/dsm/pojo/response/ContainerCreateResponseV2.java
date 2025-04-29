package com.dsm.pojo.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 容器创建响应V2
 */
@Data
@Builder
public class ContainerCreateResponseV2 {
    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 容器ID
     */
    private String containerId;

    /**
     * 消息
     */
    private String message;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 容器信息
     */
    private ContainerInfoV2 containerInfo;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 警告信息
     */
    private String[] warnings;

    /**
     * 扩展信息
     */
    private Map<String, Object> extraInfo;
} 