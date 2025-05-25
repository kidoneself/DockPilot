package com.dsm.service.http;

import com.dsm.model.ContainerCreateRequest;
import com.dsm.model.ContainerDTO;
import com.dsm.model.ContainerStaticInfoDTO;
import com.dsm.model.ResourceUsageDTO;
import com.dsm.utils.MessageCallback;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 容器服务接口
 * 定义容器管理的业务逻辑
 */
public interface ContainerService {
    /**
     * 获取所有容器列表
     *
     * @return 容器列表
     */
    List<ContainerDTO> listContainers();


    /**
     * 删除容器
     *
     * @param containerId 容器ID
     */
    void removeContainer(String containerId);

    /**
     * 获取容器统计信息
     *
     * @param containerId 容器ID
     * @return 容器统计信息
     */
    ResourceUsageDTO getContainerStats(String containerId);

    /**
     * 获取容器配置信息
     *
     * @param containerId 容器ID
     * @return 容器配置信息
     */
    ContainerStaticInfoDTO getContainerConfig(String containerId);

    /**
     * 启动容器
     *
     * @param containerId 容器ID
     */
    void startContainer(String containerId);

    /**
     * 停止容器
     *
     * @param containerId 容器ID
     */
    void stopContainer(String containerId);


    /**
     * 更新容器配置并重新创建
     *
     * @param containerId 原容器ID
     * @param request     新的容器配置请求
     * @param callback
     * @return 新容器的ID
     */
    CompletableFuture<String> updateContainer(String containerId, ContainerCreateRequest request, MessageCallback callback);

    /**
     * 更新容器镜像
     *
     * @param containerId 容器ID
     * @return 新容器的ID
     */
    CompletableFuture<String> updateContainerImage(String containerId, MessageCallback callback);


    void restartContainer(String id);

    String getContainerLogs(String id, int tail, boolean follow, boolean timestamps);

    /**
     * 创建新容器
     *
     * @param request 容器创建请求
     * @return 容器创建响应
     */
    String createContainer(ContainerCreateRequest request);


}