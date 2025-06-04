package com.dockpilot.service.http;

import com.dockpilot.model.ContainerInfo;

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

    /**
     * 检查使用指定镜像的容器更新状态
     * 
     * @param imageName 镜像名称，如 nginx
     * @param tag 镜像标签，如 latest
     */
    void checkContainersUsingImage(String imageName, String tag);
} 