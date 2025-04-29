package com.dsm.pojo.entity;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AppInstallTask {
    private String taskId;
    private String appId;
    private String status; // waiting, running, completed, failed, cancelled
    private List<String> selectedServices;
    private Map<String, String> envValues;
    private List<ServiceInstallStatus> serviceStatuses;
    private List<InstallLog> logs;
    private List<DockerResource> createdResources; // 已创建的Docker资源
}



