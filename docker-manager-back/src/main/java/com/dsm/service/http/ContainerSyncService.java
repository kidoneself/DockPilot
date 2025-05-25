package com.dsm.service.http;

import com.dsm.model.ContainerInfo;

import java.util.List;

public interface ContainerSyncService {
    /**
     * 同步容器数据
     */
    void syncContainers();

    /**
     * 获取容器列表（会先同步数据）
     */
    List<ContainerInfo> getContainerList();

    /**
     * 清理重复的容器记录
     */
    void cleanupDuplicateRecords();
} 