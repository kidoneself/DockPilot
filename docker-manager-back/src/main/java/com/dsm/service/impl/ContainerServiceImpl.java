package com.dsm.service.impl;

import com.dsm.api.DockerService;
import com.dsm.exception.BusinessException;
import com.dsm.model.dockerApi.ContainerCreateRequest;
import com.dsm.model.dto.ContainerDTO;
import com.dsm.model.dto.ContainerStaticInfoDTO;
import com.dsm.model.dto.ResourceUsageDTO;
import com.dsm.pojo.request.ContainerUpdateRequest;
import com.dsm.service.ContainerService;
import com.dsm.utils.ContainerStaticInfoConverter;
import com.dsm.utils.LogUtil;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 容器服务实现类
 * 实现容器管理的具体业务逻辑
 */
@Slf4j
@Service
public class ContainerServiceImpl implements ContainerService {

    @Autowired
    private DockerService dockerService;

    /**
     * 获取容器列表
     *
     * @return
     */
    @Override
    public List<ContainerDTO> listContainers() {
        // 获取 Docker 容器列表
        List<Container> containers = dockerService.listContainers();
        // 转换成 ContainerDTO 列表
        return containers.stream().map(ContainerDTO::convertToDTO).collect(Collectors.toList());
    }


    @Override
    public void removeContainer(String containerId) {
        dockerService.removeContainer(containerId);
    }

    @Override
    public ResourceUsageDTO getContainerStats(String containerId) {
        return dockerService.getContainerStats(containerId);

    }

    @Override
    public ContainerStaticInfoDTO getContainerConfig(String containerId) {
        // 获取容器详细信息
        InspectContainerResponse inspect = dockerService.inspectContainerCmd(containerId);
        return ContainerStaticInfoConverter.convert(inspect);
    }

    @Override
    public void startContainer(String containerId) {
        dockerService.startContainer(containerId);
    }

    @Override
    public void stopContainer(String containerId) {
        ResourceUsageDTO containerStats = dockerService.getContainerStats(containerId);
        Boolean running = containerStats.getRunning();
        if (running) {
            dockerService.stopContainer(containerId);
        }
    }

    @Override
    public void updateContainer(String containerId, ContainerCreateRequest request) {
        ContainerStaticInfoDTO originalConfig = null;
        String newContainerId = null;
        
        try {
            // 1. 停止原容器
            stopContainer(containerId);

            // 2. 备份原容器配置
            originalConfig = getContainerConfig(containerId);
            dockerService.renameContainer(containerId, originalConfig.getContainerName() + "_backup");

            // 3. 创建新容器
            newContainerId = createContainer(request);

            // 4. 验证新容器状态
            if (!isContainerRunning(newContainerId)) {
                throw new BusinessException("创建新容器失败");
            }

            // 5. 删除原容器
            removeContainer(containerId);
            LogUtil.logSysInfo("原容器已删除: " + containerId);

        } catch (Exception e) {
            LogUtil.logSysError("更新容器失败: " + e.getMessage());
            // 如果新容器创建成功但启动失败，删除新容器
            if (newContainerId != null) {
                try {
                    removeContainer(newContainerId);
                } catch (Exception ex) {
                    LogUtil.logSysError("删除失败的新容器时出错: " + ex.getMessage());
                }
            }
            throw new RuntimeException("更新容器失败: " + e.getMessage());
        } finally {
            // 确保原容器状态恢复
            if (originalConfig != null) {
                restoreOriginalContainer(containerId, originalConfig);
            }
        }
    }



    /**
     * 根据容器ID或名称查找容器
     *
     * @param containerIdOrName 容器ID或名称
     * @return 容器对象，如果未找到则返回null
     */
    private Container findContainerByIdOrName(String containerIdOrName) {
        List<Container> containers = dockerService.listContainers();
        return containers.stream().filter(container -> container.getId().equals(containerIdOrName) || Arrays.asList(container.getNames()).contains(containerIdOrName) || Arrays.asList(container.getNames()).contains("/" + containerIdOrName)).findFirst().orElse(null);
    }

    @Override
    public void restartContainer(String id) {
        dockerService.restartContainer(id);
    }

    @Override
    public String getContainerLogs(String containerId, int tail, boolean follow, boolean timestamps) {
        return dockerService.getContainerLogs(containerId, tail, follow, timestamps);
    }

    @Override
    public String createContainer(ContainerCreateRequest request) {
        CreateContainerResponse createContainerResponse = dockerService.configureContainerCmd(request);
        String containerId = createContainerResponse.getId();
        dockerService.startContainer(containerId);
        return containerId;

    }


    public boolean isContainerRunning(String containerId) {
        Container container = findContainerByIdOrName(containerId);
        return container != null && "running".equals(container.getState());
    }

    /**
     * 恢复原容器状态
     * @param containerId 原容器ID
     * @param originalConfig 原容器配置
     */
    private void restoreOriginalContainer(String containerId, ContainerStaticInfoDTO originalConfig) {
        try {
            // 检查原容器是否存在且被重命名
            Container container = findContainerByIdOrName(containerId);
            if (container != null && container.getNames()[0].endsWith("_backup")) {
                // 恢复原容器名称
                dockerService.renameContainer(containerId, originalConfig.getContainerName());
                // 启动原容器
                startContainer(containerId);
                LogUtil.logSysInfo("原容器状态已恢复: " + containerId);
            }
        } catch (Exception e) {
            LogUtil.logSysError("恢复原容器状态失败: " + e.getMessage());
        }
    }
}