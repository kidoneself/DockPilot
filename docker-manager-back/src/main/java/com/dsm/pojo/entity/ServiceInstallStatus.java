package com.dsm.pojo.entity;

import lombok.Data;

@Data
public class ServiceInstallStatus {
    private String serviceName;
    private String status; // waiting, running, completed, failed
    private int progress; // 0-100
    private String containerId; // 关联的容器ID
}
