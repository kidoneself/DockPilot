package com.dockpilot.service.http.impl;

import com.dockpilot.api.DockerService;
import com.dockpilot.common.exception.BusinessException;
import com.dockpilot.common.exception.DockerErrorResolver;
import com.dockpilot.common.exception.DockerOperationException;
import com.dockpilot.model.*;
import com.dockpilot.service.http.ContainerInfoService;
import com.dockpilot.service.http.ContainerService;
import com.dockpilot.service.http.ContainerSyncService;
import com.dockpilot.utils.ContainerStaticInfoConverter;
import com.dockpilot.utils.LogUtil;
import com.dockpilot.utils.MessageCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 容器服务实现类
 * 实现容器管理的具体业务逻辑
 */
@Slf4j
@Service
public class ContainerServiceImpl implements ContainerService {

    @Autowired
    private DockerService dockerService;

    @Autowired
    private ContainerSyncService containerSyncService;

    @Autowired
    private ContainerInfoService containerInfoService;

    /**
     * 获取容器列表
     *
     * @return
     */
    @Override
    public List<ContainerDTO> listContainers() {
        // 先同步容器数据到数据库
        containerSyncService.syncContainers();
        List<ContainerInfo> allContainerInfo = containerInfoService.getAllContainerInfo();

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
            // dto.setNeedUpdate(allContainerInfo.stream().anyMatch(containerInfo -> containerInfo.getContainerId().equals(container.getId())));

            // 这里allContainerInfo也要转成容器id和容器信息的map
            Map<String, ContainerInfo> containerInfoMap = new HashMap<>();
            for (ContainerInfo containerInfo : allContainerInfo) {
                containerInfoMap.put(containerInfo.getContainerId(), containerInfo);
            }
            // 然后通过容器id信息比较
            ContainerInfo containerInfo = containerInfoMap.get(container.getId());
            if (containerInfo != null) {
                dto.setNeedUpdate(containerInfo.getNeedUpdate() != null && containerInfo.getNeedUpdate());
                dto.setLastError(containerInfo.getLastError());
                dto.setOperationStatus(containerInfo.getOperationStatus());
                dto.setWebUrl(containerInfo.getWebUrl());
                dto.setIconUrl(containerInfo.getIconUrl());
            }

            containerDTOS.add(dto);
        }
        return containerDTOS;
    }


    @Override
    public void removeContainer(String containerId) {
        // 先查找并记录数据库信息，避免删除后找不到记录
        ContainerInfo containerInfo = containerInfoService.getContainerInfoByContainerId(containerId);

        try {
            // 删除Docker容器
            dockerService.removeContainer(containerId);

            // Docker删除成功后，再删除数据库记录
            if (containerInfo != null) {
                containerInfoService.deleteContainerInfo(containerInfo.getId());
                log.info("容器删除完成，已清理数据库记录: {}", containerInfo.getName());
            } else {
                log.warn("容器 {} 在数据库中不存在记录，仅删除了Docker容器", containerId);
            }
        } catch (Exception e) {
            // 如果Docker删除失败，保留数据库记录，记录错误信息
            if (containerInfo != null) {
                containerInfoService.updateContainerError(containerId,
                        "删除容器失败: " + e.getMessage());
                log.error("删除Docker容器失败，已更新数据库错误信息: " + containerId, e);
            }
            throw e;
        }
    }

    @Override
    public ResourceUsageDTO getContainerStats(String containerId) {
        return dockerService.getContainerStats(containerId);

    }

    @Override
    public ContainerStaticInfoDTO getContainerConfig(String containerId) {
        // 获取容器详细信息
        InspectContainerResponse inspect = dockerService.inspectContainerCmd(containerId);
        ContainerStaticInfoDTO dto = ContainerStaticInfoConverter.convert(inspect);

        // 从数据库获取错误信息和操作状态
        ContainerInfo containerInfo = containerInfoService.getContainerInfoByContainerId(containerId);
        if (containerInfo != null) {
            dto.setLastError(containerInfo.getLastError());
            dto.setOperationStatus(containerInfo.getOperationStatus());
        }

        return dto;
    }

    @Override
    public void startContainer(String containerId) {
        try {
            dockerService.startContainer(containerId);
            // 更新容器状态为运行中
//            containerInfoService.updateContainerStatus(containerId, "running", "success");
            // 清除之前的错误信息
            containerInfoService.updateContainerError(containerId, null);
        } catch (Exception e) {
            // 获取错误信息
            String errorMessage;
            if (e instanceof DockerOperationException) {
                DockerOperationException dockerEx = (DockerOperationException) e;
                errorMessage = dockerEx.getMessage();
            } else {
                errorMessage = e.getMessage();
            }

            // 记录错误信息到容器表
            containerInfoService.updateContainerError(containerId, errorMessage);
            // 更新容器状态为错误
//            containerInfoService.updateContainerStatus(containerId, "error", "failed");
            // 记录系统日志
            LogUtil.logSysError("启动容器失败: " + containerId + ", 错误: " + errorMessage);
            throw e;
        }
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
    public CompletableFuture<String> updateContainer(String containerId, ContainerCreateRequest request, MessageCallback callback) {
        return CompletableFuture.supplyAsync(() -> {
            ContainerStaticInfoDTO originalConfig = null;
            String newContainerId = null;
            String backupContainerName = null;
            String originalState;

            // 保存原有的容器信息（包括用户配置的iconUrl和webUrl）
            ContainerInfo originalContainerInfo = containerInfoService.getContainerInfoByContainerId(containerId);

            try {
                callback.onLog("【开始更新】容器: " + containerId);

                // 1. 停止原容器
                Container container = findContainerByIdOrName(containerId);
                if (container != null) {
                    originalState = container.getState();
                    callback.onLog("【原容器】当前状态: " + originalState);
                    if ("running".equalsIgnoreCase(originalState)) {
                        callback.onLog("【原容器】正在停止...");
                        stopContainer(containerId);
                        callback.onLog("【原容器】已停止");
                    } else {
                        callback.onLog("【原容器】当前未运行，无需停止");
                    }
                } else {
                    callback.onLog("【警告】未找到原容器: " + containerId);
                }

                // 2. 获取原容器配置
                callback.onLog("【原容器】正在获取配置信息...");
                originalConfig = getContainerConfig(containerId);
                callback.onLog("【原容器】配置已获取，容器名称: " + originalConfig.getContainerName());

                // 3. 生成唯一的备份容器名称
                callback.onLog("【原容器】正在生成备份名称...");
                backupContainerName = generateBackupName(originalConfig.getContainerName());
                callback.onLog("【原容器】备份名称生成: " + backupContainerName);
                callback.onLog("【原容器】正在重命名为备份...");
                dockerService.renameContainer(containerId, backupContainerName);
                callback.onLog("【原容器】已重命名为备份：" + backupContainerName);

                // 4. 创建新容器
                callback.onLog("【新容器】开始创建...");
                // 直接调用dockerService创建容器，避免重复创建容器信息记录
                CreateContainerResponse createContainerResponse = dockerService.configureContainerCmd(request);
                newContainerId = createContainerResponse.getId();

                // 启动新容器
                startContainer(newContainerId);
                callback.onLog("【新容器】创建完成，ID：" + newContainerId);

                // 5. 验证新容器状态
                callback.onLog("【新容器】正在验证运行状态...");
                if (!isContainerRunning(newContainerId)) {
                    callback.onLog("【新容器】未正常启动，准备回滚...");
                    throw new BusinessException("新容器未正常启动");
                }
                callback.onLog("【新容器】状态正常，运行中...");

                // 6. 更新数据库中的容器信息记录
                if (originalContainerInfo != null) {
                    callback.onLog("【数据库】正在更新容器信息记录...");

                    // 获取新容器的详细信息
                    InspectContainerResponse newInspect = dockerService.inspectContainerCmd(newContainerId);

                    // 更新容器信息，保留用户配置的iconUrl和webUrl
                    originalContainerInfo.setContainerId(newContainerId);
                    originalContainerInfo.setName(newInspect.getName().replaceFirst("/", ""));
                    originalContainerInfo.setImage(newInspect.getConfig().getImage());
                    originalContainerInfo.setStatus("running");
                    originalContainerInfo.setOperationStatus("success");
                    originalContainerInfo.setLastError(null); // 清除之前的错误
                    originalContainerInfo.setUpdatedAt(new Date());
                    // iconUrl和webUrl保持不变，继承原有值

                    containerInfoService.updateContainerInfo(originalContainerInfo);
                    callback.onLog("【数据库】容器信息记录已更新，保留了原有的图标和网址配置");
                } else {
                    callback.onLog("【数据库】原容器信息不存在，创建新记录...");
                    // 如果原记录不存在，创建新记录
                    InspectContainerResponse newInspect = dockerService.inspectContainerCmd(newContainerId);
                    ContainerInfo newContainerInfo = new ContainerInfo();
                    newContainerInfo.setContainerId(newContainerId);
                    newContainerInfo.setName(newInspect.getName().replaceFirst("/", ""));
                    newContainerInfo.setImage(newInspect.getConfig().getImage());
                    newContainerInfo.setStatus("running");
                    newContainerInfo.setOperationStatus("success");
                    newContainerInfo.setNeedUpdate(false);
                    newContainerInfo.setCreatedAt(new Date());
                    newContainerInfo.setUpdatedAt(new Date());

                    containerInfoService.createContainerInfo(newContainerInfo);
                    callback.onLog("【数据库】新容器信息记录已创建");
                }

                // 7. 返回新容器ID
                callback.onLog("【操作完成】更新成功，新容器ID：" + newContainerId);
                callback.onLog("【提示】原容器已保留为备份：" + backupContainerName + "，如需删除请手动操作");
                return newContainerId;

            } catch (Exception e) {
                LogUtil.logSysError(e.getMessage());
                callback.onLog("【操作异常】发生错误: " + e.getMessage());
                if (newContainerId != null) {
                    callback.onLog("【新容器】创建失败，ID：" + newContainerId);
                    callback.onLog("【提示】原容器已保留为备份：" + backupContainerName + "，新容器已保留，如需删除请手动操作");
                }
                throw new RuntimeException(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<String> updateContainerImage(String containerId, MessageCallback callback) {
        return CompletableFuture.supplyAsync(() -> {
            String newContainerId = null;
            String backupContainerName = null;

            // 保存原有的容器信息（包括用户配置的iconUrl和webUrl）
            ContainerInfo originalContainerInfo = containerInfoService.getContainerInfoByContainerId(containerId);

            try {
                callback.onLog("【开始更新镜像】容器: " + containerId);

                // 1. 检查容器状态
                InspectContainerResponse originalConfig = dockerService.inspectContainerCmd(containerId);
                boolean wasRunning = "running".equals(originalConfig.getState().getStatus());
                if (wasRunning) {
                    callback.onLog("【原容器】正在停止...");
                    dockerService.stopContainer(containerId);
                    callback.onLog("【原容器】已停止");
                } else {
                    callback.onLog("【原容器】当前未运行，无需停止");
                }

                // 2. 获取原容器配置
                String originalImageName = originalConfig.getConfig().getImage();
                callback.onLog("【原容器】配置已获取，镜像: " + originalImageName);

                // 3. 生成备份名称
                callback.onLog("【原容器】正在生成备份名称...");
                backupContainerName = generateBackupName(originalConfig.getName());
                callback.onLog("【原容器】备份名称生成: " + backupContainerName);
                callback.onLog("【原容器】正在重命名为备份...");
                dockerService.renameContainer(containerId, backupContainerName);
                callback.onLog("【原容器】已重命名为备份：" + backupContainerName);

                try {
                    // 4. 创建新容器请求
                    callback.onLog("【新容器】开始创建...");
                    CreateContainerCmd createContainerCmd = dockerService.createContainerCmd(originalImageName)
                            .withName(originalConfig.getName());

                    // 5. 复制主机配置
                    HostConfig hostConfig = originalConfig.getHostConfig();
                    if (hostConfig != null) {
                        createContainerCmd.withHostConfig(hostConfig);
                        callback.onLog("【新容器】已复制主机配置（挂载、网络、端口等）");
                    }

                    // 6. 复制环境变量
                    String[] env = originalConfig.getConfig().getEnv();
                    if (env != null && env.length > 0) {
                        createContainerCmd.withEnv(env);
                        callback.onLog("【新容器】已复制环境变量配置");
                    }

                    // 7. 复制命令配置
                    String[] cmd = originalConfig.getConfig().getCmd();
                    String[] entrypoint = originalConfig.getConfig().getEntrypoint();
                    if (cmd != null && cmd.length > 0) {
                        createContainerCmd.withCmd(cmd);
                        callback.onLog("【新容器】已复制CMD配置");
                    }
                    if (entrypoint != null && entrypoint.length > 0) {
                        createContainerCmd.withEntrypoint(entrypoint);
                        callback.onLog("【新容器】已复制Entrypoint配置");
                    }

                    // 8. 创建新容器
                    CreateContainerResponse response = createContainerCmd.exec();
                    newContainerId = response.getId();
                    callback.onLog("【新容器】创建完成，ID：" + newContainerId);

                    // 9. 如果原容器在运行，启动新容器
                    if (wasRunning) {
                        callback.onLog("【新容器】正在启动...");
                        startContainer(newContainerId);
                        InspectContainerResponse newOriginalConfig = dockerService.inspectContainerCmd(newContainerId);
                        boolean newWasRunning = "running".equals(newOriginalConfig.getState().getStatus());
                        if (newWasRunning) {
                            callback.onLog("【新容器】状态正常，运行中...");
                        } else {
                            throw new BusinessException("新容器未正常启动");
                        }
                    }

                    // 10. 更新数据库中的容器信息记录
                    if (originalContainerInfo != null) {
                        callback.onLog("【数据库】正在更新容器信息记录...");

                        // 获取新容器的详细信息
                        InspectContainerResponse newInspect = dockerService.inspectContainerCmd(newContainerId);

                        // 更新容器信息，保留用户配置的iconUrl和webUrl
                        originalContainerInfo.setContainerId(newContainerId);
                        originalContainerInfo.setName(newInspect.getName().replaceFirst("/", ""));
                        originalContainerInfo.setImage(newInspect.getConfig().getImage());
                        originalContainerInfo.setStatus(wasRunning ? "running" : "created");
                        originalContainerInfo.setOperationStatus("success");
                        originalContainerInfo.setLastError(null); // 清除之前的错误
                        originalContainerInfo.setUpdatedAt(new Date());
                        // iconUrl和webUrl保持不变，继承原有值

                        containerInfoService.updateContainerInfo(originalContainerInfo);
                        callback.onLog("【数据库】容器信息记录已更新，保留了原有的图标和网址配置");
                    } else {
                        callback.onLog("【数据库】原容器信息不存在，创建新记录...");
                        // 如果原记录不存在，创建新记录
                        InspectContainerResponse newInspect = dockerService.inspectContainerCmd(newContainerId);
                        ContainerInfo newContainerInfo = new ContainerInfo();
                        newContainerInfo.setContainerId(newContainerId);
                        newContainerInfo.setName(newInspect.getName().replaceFirst("/", ""));
                        newContainerInfo.setImage(newInspect.getConfig().getImage());
                        newContainerInfo.setStatus(wasRunning ? "running" : "created");
                        newContainerInfo.setOperationStatus("success");
                        newContainerInfo.setNeedUpdate(false);
                        newContainerInfo.setCreatedAt(new Date());
                        newContainerInfo.setUpdatedAt(new Date());

                        containerInfoService.createContainerInfo(newContainerInfo);
                        callback.onLog("【数据库】新容器信息记录已创建");
                    }

                    callback.onLog("【操作完成】更新成功，新容器ID：" + newContainerId);
                    callback.onLog("【提示】原容器已保留为备份：" + backupContainerName + "，如需删除请手动操作");

                } catch (Exception e) {
                    LogUtil.logSysError(e.getMessage());
                    callback.onLog("【操作异常】发生错误: " + e.getMessage());
                    if (newContainerId != null) {
                        callback.onLog("【新容器】创建失败，ID：" + newContainerId);
                        callback.onLog("【提示】原容器已保留为备份：" + backupContainerName + "，新容器已保留，如需删除请手动操作");
                    }
                    throw new RuntimeException("Failed to update container image: " + e.getMessage(), e);
                }
                return newContainerId;
            } catch (Exception e) {
                LogUtil.logSysError(e.getMessage());
                callback.onLog("【操作异常】发生错误: " + e.getMessage());
                throw new RuntimeException("Failed to update container image: " + e.getMessage(), e);
            }
        });
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
        String containerId = null;
        boolean dockerContainerCreated = false;

        try {
            // 1. 创建容器（非事务操作）
            CreateContainerResponse createContainerResponse = dockerService.configureContainerCmd(request);
            containerId = createContainerResponse.getId();
            dockerContainerCreated = true;
            log.info("Docker容器创建成功: {}", containerId);

            // 2. 尝试启动容器
            try {
                startContainer(containerId);
                log.info("Docker容器启动成功: {}", containerId);

                // 3. 获取容器详细信息
                InspectContainerResponse inspect = dockerService.inspectContainerCmd(containerId);

                // 4. 检查是否已存在记录，避免重复创建
                ContainerInfo existingInfo = containerInfoService.getContainerInfoByContainerId(containerId);
                if (existingInfo != null) {
                    log.warn("容器 {} 的数据库记录已存在，跳过创建。可能是同步服务已经创建", containerId);
                    return containerId;
                }

                // 5. 创建成功的容器信息记录（独立事务）
                saveContainerInfoInNewTransaction(containerId, inspect, "running", "success", null);
                log.info("容器创建和启动完成，已添加数据库记录: {}", inspect.getName().replaceFirst("/", ""));

                return containerId;

            } catch (Exception startEx) {
                // 启动失败，但容器已创建，保留容器并记录启动失败原因
                log.warn("容器创建成功但启动失败: {}, 错误: {}", containerId, startEx.getMessage());

                // 使用DockerErrorResolver解析启动失败的原因
                DockerOperationException dockerEx;
                if (startEx instanceof DockerOperationException) {
                    dockerEx = (DockerOperationException) startEx;
                } else {
                    dockerEx = DockerErrorResolver.resolve("启动容器", containerId, startEx);
                }

                // 获取容器详细信息
                InspectContainerResponse inspect = dockerService.inspectContainerCmd(containerId);

                // 在独立事务中保存启动失败的错误记录，不会被回滚
                saveContainerInfoInNewTransaction(containerId, inspect, "created", "failed", dockerEx.getDetail());

                // 记录系统日志
                LogUtil.logSysError("容器启动失败: " + containerId + ", 错误: " + dockerEx.getDetail());

                // 抛出业务异常，包含用户友好的错误信息
                throw new BusinessException("容器创建成功但启动失败: " + dockerEx.getDetail());
            }

        } catch (Exception e) {
            // 容器创建失败的处理逻辑
            if (!dockerContainerCreated) {
                // 容器都没创建成功，直接抛出异常
                log.error("容器创建失败: {}", e.getMessage(), e);

                // 使用DockerErrorResolver解析创建失败的原因
                DockerOperationException dockerEx;
                if (e instanceof DockerOperationException) {
                    dockerEx = (DockerOperationException) e;
                } else {
                    dockerEx = DockerErrorResolver.resolve("创建容器", "unknown", e);
                }

                throw new BusinessException("创建容器失败: " + dockerEx.getDetail());
            } else {
                // 容器创建成功但后续处理失败，重新抛出异常
                throw e;
            }
        }
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
        // 生成时间戳格式的备份名称
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        return baseName + "_" + timestamp;
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

    /**
     * 在独立事务中保存容器信息，避免被主事务回滚
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveContainerInfoInNewTransaction(String containerId, InspectContainerResponse inspect, String status, String operationStatus, String lastError) {
        try {
            // 检查是否已存在记录
            ContainerInfo existingInfo = containerInfoService.getContainerInfoByContainerId(containerId);
            if (existingInfo != null) {
                // 更新已有记录
                existingInfo.setStatus(status);
                existingInfo.setOperationStatus(operationStatus);
                existingInfo.setLastError(lastError);
                existingInfo.setUpdatedAt(new Date());
                containerInfoService.updateContainerInfo(existingInfo);
                log.info("已更新容器信息记录: {}, 状态: {}, 错误: {}", existingInfo.getName(), operationStatus, lastError);
            } else {
                // 创建新记录
                ContainerInfo containerInfo = new ContainerInfo();
                containerInfo.setContainerId(containerId);
                containerInfo.setName(inspect.getName().replaceFirst("/", ""));
                containerInfo.setImage(inspect.getConfig().getImage());
                containerInfo.setStatus(status);
                containerInfo.setOperationStatus(operationStatus);
                containerInfo.setLastError(lastError);
                containerInfo.setNeedUpdate(false);
                containerInfo.setCreatedAt(new Date());
                containerInfo.setUpdatedAt(new Date());

                containerInfoService.createContainerInfo(containerInfo);
                log.info("已创建容器信息记录: {}, 状态: {}, 错误: {}", containerInfo.getName(), operationStatus, lastError);
            }
        } catch (Exception e) {
            log.error("保存容器信息到数据库失败: " + containerId, e);
            // 不重新抛出异常，避免影响主流程
        }
    }

}