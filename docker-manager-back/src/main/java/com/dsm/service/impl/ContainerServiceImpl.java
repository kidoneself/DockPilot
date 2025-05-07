package com.dsm.service.impl;

import com.dsm.api.DockerService;
import com.dsm.exception.BusinessException;
import com.dsm.model.ContainerCreateRequest;
import com.dsm.model.ContainerDTO;
import com.dsm.model.ContainerStaticInfoDTO;
import com.dsm.model.ResourceUsageDTO;
import com.dsm.service.ContainerService;
import com.dsm.utils.ContainerStaticInfoConverter;
import com.dsm.utils.LogUtil;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
        // 获取所有镜像列表
        List<Image> images = dockerService.listImages();
        // 创建镜像ID映射
        Map<String, String> imageIdMap = new HashMap<>();
        for (Image image : images) {
            if (image.getRepoTags() != null) {
                for (String tag : image.getRepoTags()) {
                    imageIdMap.put(tag, image.getId());
                }
            }
        }

        List<ContainerDTO> containerDTOS = new ArrayList<>();
        for (Container container : containers) {
            ContainerDTO dto = ContainerDTO.convertToDTO(container);
            String latestImageId = imageIdMap.get(container.getImage());
            dto.setNeedUpdate(latestImageId != null && !latestImageId.equals(container.getImageId()));
            //暂时先设置的需要更新
//            dto.setNeedUpdate(true);
            containerDTOS.add(dto);
        }
        return containerDTOS;
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
    public String updateContainer(String containerId, ContainerCreateRequest request) {
        ContainerStaticInfoDTO originalConfig = null;
        String newContainerId = null;
        String backupContainerName = null;

        try {
            // 1. 停止原容器
            stopContainer(containerId);

            // 2. 获取原容器配置
            originalConfig = getContainerConfig(containerId);

            // 3. 生成唯一的备份容器名称
            backupContainerName = generateBackupName(originalConfig.getContainerName());
            dockerService.renameContainer(containerId, backupContainerName);

            // 4. 创建新容器
            newContainerId = createContainer(request);

            // 5. 验证新容器状态
            if (!isContainerRunning(newContainerId)) {
                throw new BusinessException("新容器未正常启动");
            }

            // 6. 删除原容器
            removeContainer(containerId);
            LogUtil.logSysInfo("原容器已删除: " + containerId);

            // 7. 返回新容器ID
            return newContainerId;

        } catch (Exception e) {
            LogUtil.logSysError("更新容器失败: " + e.getMessage());
            if (newContainerId != null) {
                try {
                    removeContainer(newContainerId);
                    LogUtil.logSysInfo("失败的新容器已删除: " + newContainerId);
                } catch (Exception ex) {
                    LogUtil.logSysError("删除失败的新容器时出错: " + ex.getMessage());
                }
            }
            throw new RuntimeException("更新容器失败: " + e.getMessage());
        } finally {
            // 恢复原容器状态（如果已重命名）
            if (originalConfig != null && backupContainerName != null) {
                restoreOriginalContainerByName(backupContainerName, originalConfig.getContainerName());
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


    /**
     * 验证容器是否运行中
     */
    public boolean isContainerRunning(String containerId) {
        Container container = findContainerByIdOrName(containerId);
        return container != null && "running".equalsIgnoreCase(container.getState());
    }

    /**
     * 生成不重复的容器备份名
     */
    private String generateBackupName(String baseName) {
        String backupName = baseName + "_backup";
        int i = 1;
        while (containerExists(backupName)) {
            backupName = baseName + "_backup_" + (i++);
        }
        return backupName;
    }

    /**
     * 恢复原容器
     */
    private void restoreOriginalContainerByName(String backupName, String originalName) {
        try {
            Container container = findContainerByIdOrName(backupName);
            if (container != null) {
                dockerService.renameContainer(container.getId(), originalName);
                startContainer(container.getId());
                LogUtil.logSysInfo("已恢复原容器: " + originalName);
            }
        } catch (Exception e) {
            LogUtil.logSysError("恢复原容器失败: " + e.getMessage());
        }
    }

    /**
     * 判断指定名称的容器是否存在
     *
     * @param containerName 容器名称
     * @return 存在返回 true，不存在返回 false
     */
    private boolean containerExists(String containerName) {
        try {
            Container container = findContainerByIdOrName(containerName);
            return container != null;
        } catch (Exception e) {
            LogUtil.logSysError("检查容器存在失败: " + e.getMessage());
            return false;
        }
    }
}