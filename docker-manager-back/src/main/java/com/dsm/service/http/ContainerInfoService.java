package com.dsm.service.http;

import com.dsm.model.ContainerInfo;

import java.util.List;

public interface ContainerInfoService {
    /**
     * 创建容器信息
     */
    void createContainerInfo(ContainerInfo containerInfo);

    /**
     * 更新容器信息
     */
    void updateContainerInfo(ContainerInfo containerInfo);

    /**
     * 删除容器信息
     */
    void deleteContainerInfo(Integer id);

    /**
     * 根据ID获取容器信息
     */
    ContainerInfo getContainerInfoById(Integer id);

    /**
     * 根据容器ID获取容器信息
     */
    ContainerInfo getContainerInfoByContainerId(String containerId);

    /**
     * 获取所有容器信息
     */
    List<ContainerInfo> getAllContainerInfo();

    /**
     * 更新容器状态
     */
    void updateContainerStatus(String containerId, String status, String operationStatus);

    /**
     * 更新容器错误信息
     */
    void updateContainerError(String containerId, String error);
} 