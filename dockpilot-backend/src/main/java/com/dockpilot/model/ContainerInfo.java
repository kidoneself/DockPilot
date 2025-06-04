package com.dockpilot.model;

import lombok.Data;

import java.util.Date;

@Data
public class ContainerInfo {
    private Integer id;
    private String containerId;
    private String name;
    private String image;
    private String status;
    private String operationStatus;
    private String lastError;
    private Integer needUpdate;  // 0=正常状态，1=需要更新，2=老版本（可删除的备份）
    private String iconUrl;
    private String webUrl;
    private Date createdAt;
    private Date updatedAt;
} 