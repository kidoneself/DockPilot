package com.dsm.model;

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
    private Boolean needUpdate;
    private String iconUrl;
    private String webUrl;
    private Date createdAt;
    private Date updatedAt;
} 