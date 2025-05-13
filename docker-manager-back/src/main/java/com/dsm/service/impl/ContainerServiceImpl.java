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
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
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

            //这里如果没获取到镜像名字，可能是镜像被强制删除了，或者镜像更新了，要么通过为null直接判断容器需要更新
            //另外一种方式，是通过容器的镜像ID去获取镜像名字，如果不存在说明容器使用的镜像已经老了
            dto.setNeedUpdate(latestImageId == null/* && !latestImageId.equals(container.getImageId())*/);
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
        String originalState;

        try {
            // 1. 停止原容器
            // 1. 检查容器状态
            Container container = findContainerByIdOrName(containerId);
            if (container != null) {
                originalState = container.getState();
                if ("running".equalsIgnoreCase(originalState)) {
                    stopContainer(containerId);
                }
            }

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

    @Override
    public String updateContainerImage(String containerId) {
        // 1. 检查容器状态
        InspectContainerResponse originalConfig = dockerService.inspectContainerCmd(containerId);
        boolean wasRunning = "running".equals(originalConfig.getState().getStatus());

        if (wasRunning) {
            dockerService.stopContainer(containerId);
        }

        // 2. 获取原容器配置
        String originalImageName = originalConfig.getConfig().getImage();

        // 3. 生成备份名称
        String backupName = originalConfig.getName() + "_backup_" + System.currentTimeMillis();

        // 4. 重命名原容器
        dockerService.renameContainer(containerId, backupName);

        try {
            // 5. 创建新容器请求
            CreateContainerCmd createContainerCmd = dockerService.createContainerCmd(originalImageName).withName(originalConfig.getName());

            // 6. 复制主机配置
            HostConfig hostConfig = originalConfig.getHostConfig();
            if (hostConfig != null) {
                createContainerCmd.withHostConfig(hostConfig);
            }

            // 7. 复制基本配置
            ContainerConfig config = originalConfig.getConfig();
            if (config != null) {
                if (config.getEnv() != null) {
                    createContainerCmd.withEnv(config.getEnv());
                }
                if (config.getCmd() != null) {
                    createContainerCmd.withCmd(config.getCmd());
                }
                if (config.getEntrypoint() != null) {
                    createContainerCmd.withEntrypoint(config.getEntrypoint());
                }
                if (config.getWorkingDir() != null) {
                    createContainerCmd.withWorkingDir(config.getWorkingDir());
                }
                if (config.getLabels() != null) {
                    createContainerCmd.withLabels(config.getLabels());
                }
                if (config.getExposedPorts() != null) {
                    createContainerCmd.withExposedPorts(config.getExposedPorts());
                }
                if (config.getUser() != null) {
                    createContainerCmd.withUser(config.getUser());
                }
                if (config.getTty() != null) {
                    createContainerCmd.withTty(config.getTty());
                }
                if (config.getAttachStdout() != null) {
                    createContainerCmd.withAttachStdout(config.getAttachStdout());
                }
                if (config.getAttachStderr() != null) {
                    createContainerCmd.withAttachStderr(config.getAttachStderr());
                }
                if (config.getStdinOpen() != null) {
                    createContainerCmd.withStdinOpen(config.getStdinOpen());
                }
                if (config.getHealthcheck() != null) {
                    createContainerCmd.withHealthcheck(config.getHealthcheck());
                }
            }

            // 8. 复制高级配置
            if (hostConfig != null) {
                // 设备相关
                if (hostConfig.getDevices() != null) {
                    createContainerCmd.withDevices(hostConfig.getDevices());
                }

                // 资源限制相关
                if (hostConfig.getMemorySwap() != null) {
                    createContainerCmd.withMemorySwap(hostConfig.getMemorySwap());
                }
                if (hostConfig.getCpusetCpus() != null) {
                    createContainerCmd.withCpusetCpus(hostConfig.getCpusetCpus());
                }
                if (hostConfig.getCpusetMems() != null) {
                    createContainerCmd.withCpusetMems(hostConfig.getCpusetMems());
                }

                // 网络相关
                if (hostConfig.getDns() != null) {
                    createContainerCmd.withDns(hostConfig.getDns());
                }
                if (hostConfig.getDnsSearch() != null) {
                    createContainerCmd.withDnsSearch(hostConfig.getDnsSearch());
                }
                if (hostConfig.getExtraHosts() != null) {
                    createContainerCmd.withExtraHosts(hostConfig.getExtraHosts());
                }
                if (hostConfig.getLinks() != null) {
                    createContainerCmd.withLinks(hostConfig.getLinks());
                }

                // 安全相关
                if (hostConfig.getCapAdd() != null) {
                    createContainerCmd.withCapAdd(hostConfig.getCapAdd());
                }
                if (hostConfig.getCapDrop() != null) {
                    createContainerCmd.withCapDrop(hostConfig.getCapDrop());
                }
                if (hostConfig.getPrivileged() != null) {
                    createContainerCmd.withPrivileged(hostConfig.getPrivileged());
                }
                if (hostConfig.getReadonlyRootfs() != null) {
                    createContainerCmd.withReadonlyRootfs(hostConfig.getReadonlyRootfs());
                }

                // 其他
                if (hostConfig.getUlimits() != null) {
                    createContainerCmd.withUlimits(hostConfig.getUlimits());
                }
                if (hostConfig.getOomKillDisable() != null) {
                    createContainerCmd.withOomKillDisable(hostConfig.getOomKillDisable());
                }
                if (hostConfig.getPublishAllPorts() != null) {
                    createContainerCmd.withPublishAllPorts(hostConfig.getPublishAllPorts());
                }
            }

            // 9. 创建新容器
            CreateContainerResponse response = createContainerCmd.exec();
            String newContainerId = response.getId();


            // 10. 验证新容器状态
            if (newContainerId != null) {
                // 11. 如果原容器在运行，启动新容器
                if (wasRunning) {
                    dockerService.startContainer(newContainerId);
                    InspectContainerResponse newOriginalConfig = dockerService.inspectContainerCmd(newContainerId);
                    boolean newWasRunning = "running".equals(newOriginalConfig.getState().getStatus());
                    if (newWasRunning) {
                        // 12. 删除原容器
                        dockerService.removeContainer(backupName);
                        return newContainerId;
                    }
                }
                return newContainerId;
            } else {
                throw new BusinessException("Failed to create new container");
            }
        } catch (Exception e) {
            // 13. 发生错误时恢复原容器
            dockerService.renameContainer(backupName, originalConfig.getName());
            if (wasRunning) {
                dockerService.startContainer(backupName);
            }
            throw new RuntimeException("Failed to update container image: " + e.getMessage(), e);
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