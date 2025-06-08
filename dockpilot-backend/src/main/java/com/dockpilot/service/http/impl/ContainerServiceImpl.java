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
import com.github.dockerjava.api.model.Bind;
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

    @Autowired
    private com.dockpilot.common.config.AppConfig appConfig;

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
                // 🔄 直接传递三状态值：0=正常，1=需要更新，2=老版本
                dto.setNeedUpdate(containerInfo.getNeedUpdate() != null ? containerInfo.getNeedUpdate() : 0);
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

                    // 📌 重要：保存用户配置字段，防止丢失
                    String preservedWebUrl = originalContainerInfo.getWebUrl();
                    String preservedIconUrl = originalContainerInfo.getIconUrl();
                    
                    callback.onLog("【数据库】保留用户配置 - WebURL: " + preservedWebUrl + ", IconURL: " + preservedIconUrl);

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
                    
                    // 🔒 强制保留用户配置字段，即使为空也要显式设置
                    originalContainerInfo.setWebUrl(preservedWebUrl);
                    originalContainerInfo.setIconUrl(preservedIconUrl);
                    
                    callback.onLog("【数据库】用户配置已强制保留 - WebURL: " + preservedWebUrl + ", IconURL: " + preservedIconUrl);

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
                    newContainerInfo.setNeedUpdate(0); // 0=正常状态，无需更新
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
            String originalContainerName = null; // 添加：保存原始容器名称用于回滚

            // 保存原有的容器信息（包括用户配置的iconUrl和webUrl）
            ContainerInfo originalContainerInfo = containerInfoService.getContainerInfoByContainerId(containerId);

            try {
                callback.onLog("【开始更新镜像】容器: " + containerId);

                // 1. 检查容器状态
                InspectContainerResponse originalConfig = dockerService.inspectContainerCmd(containerId);
                originalContainerName = originalConfig.getName().replaceFirst("/", ""); // 保存原始名称
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

                // 3. 生成备份名称（基于原始名称而不是当前名称，避免时间戳叠加）
                callback.onLog("【原容器】正在生成备份名称...");
                backupContainerName = generateBackupName(originalContainerName);
                callback.onLog("【原容器】备份名称生成: " + backupContainerName);
                callback.onLog("【原容器】正在重命名为备份...");
                dockerService.renameContainer(containerId, backupContainerName);
                callback.onLog("【原容器】已重命名为备份：" + backupContainerName);

                try {
                    // 4. 检查本地镜像是否存在
                    callback.onLog("【镜像检查】检查本地镜像: " + originalImageName);
                    if (!dockerService.isImageExists(originalImageName)) {
                        throw new BusinessException("本地镜像不存在: " + originalImageName + "，请先在镜像管理页面拉取最新镜像");
                    }
                    callback.onLog("【镜像检查】本地镜像可用: " + originalImageName);

                    // 5. 创建新容器请求（使用原始名称）
                    callback.onLog("【新容器】开始创建...");
                    CreateContainerCmd createContainerCmd = dockerService.createContainerCmd(originalImageName)
                            .withName(originalContainerName); // 使用原始名称

                    // 6. 复制主机配置
                    HostConfig hostConfig = originalConfig.getHostConfig();
                    if (hostConfig != null) {
                        createContainerCmd.withHostConfig(hostConfig);
                        callback.onLog("【新容器】已复制主机配置（挂载、网络、端口等）");
                    }

                    // 7. 复制环境变量
                    String[] env = originalConfig.getConfig().getEnv();
                    if (env != null && env.length > 0) {
                        createContainerCmd.withEnv(env);
                        callback.onLog("【新容器】已复制环境变量配置");
                    }

                    // 8. 复制命令配置
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

                    // 9. 创建新容器
                    CreateContainerResponse response = createContainerCmd.exec();
                    newContainerId = response.getId();
                    callback.onLog("【新容器】创建完成，ID：" + newContainerId);

                    // 10. 如果原容器在运行，启动新容器
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

                    // 11. 更新数据库中的容器信息记录
                    if (originalContainerInfo != null) {
                        callback.onLog("【数据库】正在处理容器信息记录...");

                        // 📌 重要：保存用户配置字段，防止丢失
                        String preservedWebUrl = originalContainerInfo.getWebUrl();
                        String preservedIconUrl = originalContainerInfo.getIconUrl();
                        
                        callback.onLog("【数据库】保留用户配置 - WebURL: " + preservedWebUrl + ", IconURL: " + preservedIconUrl);

                        // 获取新容器的详细信息
                        InspectContainerResponse newInspect = dockerService.inspectContainerCmd(newContainerId);

                        // ✅ 正确逻辑：为新容器创建新记录，继承用户配置
                        ContainerInfo newContainerInfo = new ContainerInfo();
                        newContainerInfo.setContainerId(newContainerId);  // 新容器ID
                        newContainerInfo.setName(newInspect.getName().replaceFirst("/", ""));
                        newContainerInfo.setImage(newInspect.getConfig().getImage());
                        newContainerInfo.setStatus(wasRunning ? "running" : "created");
                        newContainerInfo.setOperationStatus("success");
                        newContainerInfo.setLastError(null);
                        newContainerInfo.setNeedUpdate(0); // 0=正常状态，无需更新
                        newContainerInfo.setCreatedAt(new Date());
                        newContainerInfo.setUpdatedAt(new Date());
                        
                        // 🔒 继承原容器的用户配置
                        newContainerInfo.setWebUrl(preservedWebUrl);
                        newContainerInfo.setIconUrl(preservedIconUrl);
                        
                        containerInfoService.createContainerInfo(newContainerInfo);
                        callback.onLog("【数据库】新容器记录已创建，继承了用户配置 - WebURL: " + preservedWebUrl + ", IconURL: " + preservedIconUrl);
                        
                        // ✅ 正确逻辑：原容器记录只更新名称为备份名称，保持原容器ID不变
                        originalContainerInfo.setName(backupContainerName);  // 只更新名称
                        originalContainerInfo.setNeedUpdate(2); // 🔘 标记为老版本容器（可删除的备份）
                        originalContainerInfo.setUpdatedAt(new Date());
                        
                        containerInfoService.updateContainerInfo(originalContainerInfo);
                        callback.onLog("【数据库】原容器记录已更新为备份名称: " + backupContainerName + "，标记为老版本（可删除）");
                    } else {
                        callback.onLog("【数据库】原容器信息不存在，为新容器创建记录...");
                        // 如果原记录不存在，创建新记录
                        InspectContainerResponse newInspect = dockerService.inspectContainerCmd(newContainerId);
                        ContainerInfo newContainerInfo = new ContainerInfo();
                        newContainerInfo.setContainerId(newContainerId);
                        newContainerInfo.setName(newInspect.getName().replaceFirst("/", ""));
                        newContainerInfo.setImage(newInspect.getConfig().getImage());
                        newContainerInfo.setStatus(wasRunning ? "running" : "created");
                        newContainerInfo.setOperationStatus("success");
                        newContainerInfo.setNeedUpdate(0); // 0=正常状态，无需更新
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
                    
                    // 🚨 关键：更新失败时回滚容器名称
                    try {
                        if (backupContainerName != null && originalContainerName != null) {
                            callback.onLog("【回滚操作】正在恢复容器名称...");
                            // 将备份容器重命名回原始名称
                            Container backupContainer = findContainerByIdOrName(backupContainerName);
                            if (backupContainer != null) {
                                dockerService.renameContainer(backupContainer.getId(), originalContainerName);
                                callback.onLog("【回滚完成】容器名称已恢复为: " + originalContainerName);
                                
                                // 如果新容器创建成功但启动失败，清理新容器
                                if (newContainerId != null) {
                                    try {
                                        dockerService.removeContainer(newContainerId);
                                        callback.onLog("【清理完成】已删除失败的新容器: " + newContainerId);
                                    } catch (Exception cleanupEx) {
                                        callback.onLog("【清理失败】无法删除新容器，请手动清理: " + newContainerId);
                                    }
                                }
                            } else {
                                callback.onLog("【回滚失败】找不到备份容器: " + backupContainerName);
                            }
                        }
                    } catch (Exception rollbackEx) {
                        callback.onLog("【回滚异常】回滚操作失败: " + rollbackEx.getMessage());
                        LogUtil.logSysError("回滚容器名称失败: " + rollbackEx.getMessage());
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
            // 🚀 新增：在创建容器前自动创建挂载目录
            log.info("📁 开始检查和创建挂载目录...");
            ensureVolumeMountDirectoriesExist(request);

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

                // 🔧 改进：使用DockerErrorResolver解析启动失败的原因
                DockerOperationException dockerEx;
                if (startEx instanceof DockerOperationException) {
                    dockerEx = (DockerOperationException) startEx;
                } else {
                    dockerEx = DockerErrorResolver.resolve("启动容器", containerId, startEx);
                }

                // 获取容器详细信息
                InspectContainerResponse inspect = dockerService.inspectContainerCmd(containerId);

                // 🎯 改进：获取用户友好的错误信息
                String userFriendlyError = dockerEx.getDetail() != null ? dockerEx.getDetail() : dockerEx.getMessage();
                
                // 在独立事务中保存启动失败的错误记录，不会被回滚
                saveContainerInfoInNewTransaction(containerId, inspect, "created", "failed", userFriendlyError);

                // 记录系统日志
                LogUtil.logSysError("容器启动失败: " + containerId + ", 错误: " + userFriendlyError);

                // 抛出业务异常，包含用户友好的错误信息
                throw new BusinessException("容器创建成功但启动失败: " + userFriendlyError);
            }

        } catch (Exception e) {
            // 容器创建失败的处理逻辑
            if (!dockerContainerCreated) {
                // 容器都没创建成功，直接抛出异常
                log.error("容器创建失败: {}", e.getMessage(), e);

                // 🔧 改进：使用DockerErrorResolver解析创建失败的原因
                DockerOperationException dockerEx;
                if (e instanceof DockerOperationException) {
                    dockerEx = (DockerOperationException) e;
                } else {
                    dockerEx = DockerErrorResolver.resolve("创建容器", "unknown", e);
                }

                // 🎯 改进：获取用户友好的错误信息，确保不返回null
                String userFriendlyError = dockerEx.getDetail() != null ? dockerEx.getDetail() : dockerEx.getMessage();
                if (userFriendlyError == null || userFriendlyError.trim().isEmpty()) {
                    userFriendlyError = "容器创建失败，请检查配置是否正确";
                }

                throw new BusinessException("创建容器失败: " + userFriendlyError);
            } else {
                // 容器创建成功但后续处理失败，使用DockerErrorResolver解析异常
                log.error("容器创建成功但后续处理失败: {}", e.getMessage(), e);
                
                DockerOperationException dockerEx;
                if (e instanceof DockerOperationException) {
                    dockerEx = (DockerOperationException) e;
                } else {
                    dockerEx = DockerErrorResolver.resolve("容器后续处理", containerId, e);
                }
                
                String userFriendlyError = dockerEx.getDetail() != null ? dockerEx.getDetail() : dockerEx.getMessage();
                if (userFriendlyError == null || userFriendlyError.trim().isEmpty()) {
                    userFriendlyError = "容器创建成功但初始化失败";
                }
                
                throw new BusinessException("容器处理失败: " + userFriendlyError);
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
                containerInfo.setNeedUpdate(0); // 0=正常状态，无需更新
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

    // =============== 🚀 新增：目录自动创建功能 ===============

    /**
     * 确保卷挂载目录存在，自动创建不存在的目录
     * 
     * @param request 容器创建请求
     * @throws BusinessException 如果关键目录创建失败
     */
    private void ensureVolumeMountDirectoriesExist(ContainerCreateRequest request) {
        if (request.getBinds() == null || request.getBinds().isEmpty()) {
            log.info("📁 无卷挂载配置，跳过目录创建");
            return;
        }

        log.info("📁 检测到 {} 个卷挂载配置，开始检查目录", request.getBinds().size());

        StringBuilder criticalErrors = new StringBuilder();
        int failedCount = 0;

        for (Bind bind : request.getBinds()) {
            try {
                String hostPath = bind.getPath();
                String containerPath = bind.getVolume().getPath();
                String accessMode = bind.getAccessMode() != null ? bind.getAccessMode().toString() : "rw";

                log.info("📂 处理卷挂载: {} -> {} ({})", hostPath, containerPath, accessMode);

                // 自动创建宿主机目录
                if (ensureHostDirectoryExists(hostPath)) {
                    log.info("✅ 宿主机目录处理成功: {}", hostPath);
                } else {
                    failedCount++;
                    String errorMsg = "无法创建宿主机目录: " + hostPath;
                    log.warn("⚠️ {}", errorMsg);
                    
                    // 🎯 检查是否为关键目录（Docker配置目录内的）
                    if (shouldCreateDirectory(hostPath)) {
                        if (criticalErrors.length() > 0) {
                            criticalErrors.append("; ");
                        }
                        criticalErrors.append(errorMsg);
                    }
                }

            } catch (Exception e) {
                failedCount++;
                String errorMsg = String.format("处理卷挂载失败: %s -> %s, 错误: %s", 
                    bind.getPath(), bind.getVolume().getPath(), e.getMessage());
                log.error("❌ {}", errorMsg, e);
                
                // 🎯 对于Docker配置目录，记录为关键错误
                if (shouldCreateDirectory(bind.getPath())) {
                    if (criticalErrors.length() > 0) {
                        criticalErrors.append("; ");
                    }
                    criticalErrors.append(errorMsg);
                }
            }
        }

        log.info("📁 卷挂载目录检查完成，成功: {}, 失败: {}", 
            request.getBinds().size() - failedCount, failedCount);

        // 🚨 如果有关键目录创建失败，抛出异常
        if (criticalErrors.length() > 0) {
            throw new BusinessException("Docker配置目录创建失败: " + criticalErrors.toString());
        }
    }

    /**
     * 确保宿主机目录存在，如果不存在则自动创建
     * 
     * @param hostPath 宿主机路径
     * @return 是否成功创建或已存在
     */
    private boolean ensureHostDirectoryExists(String hostPath) {
        try {
            if (hostPath == null || hostPath.trim().isEmpty()) {
                log.warn("⚠️ 宿主机路径为空，跳过创建");
                return false;
            }

            String normalizedPath = hostPath.trim();

            if (!normalizedPath.startsWith("/")) {
                log.warn("⚠️ 宿主机路径必须是绝对路径: {}", normalizedPath);
                return false;
            }

            if (isSystemSensitivePath(normalizedPath)) {
                log.info("⚠️ 跳过系统敏感路径: {}", normalizedPath);
                return true;
            }

            if (!shouldCreateDirectory(normalizedPath)) {
                log.info("ℹ️ 跳过非Docker配置目录: {} (只自动创建Docker配置目录)", normalizedPath);
                return true;
            }

            String actualPath = getActualFilePath(normalizedPath);
            java.nio.file.Path targetPath = java.nio.file.Paths.get(actualPath);

            if (java.nio.file.Files.exists(targetPath)) {
                if (java.nio.file.Files.isDirectory(targetPath)) {
                    log.info("✅ 宿主机目录已存在: {}", normalizedPath);
                    return true;
                } else {
                    log.error("❌ 宿主机路径已存在但不是目录: {}", normalizedPath);
                    return false;
                }
            }

            log.info("📁 正在创建Docker配置目录: {}", normalizedPath);
            java.nio.file.Files.createDirectories(targetPath);

            if (java.nio.file.Files.exists(targetPath) && java.nio.file.Files.isDirectory(targetPath)) {
                log.info("✅ Docker配置目录创建成功: {}", normalizedPath);
                return true;
            } else {
                log.error("❌ 目录创建失败，验证不通过: {}", normalizedPath);
                return false;
            }

        } catch (Exception e) {
            log.error("❌ 创建宿主机目录失败: {}, 错误: {}", hostPath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 判断是否应该创建目录（只创建Docker配置目录）
     * 
     * @param path 目录路径
     * @return 是否应该创建
     */
    private boolean shouldCreateDirectory(String path) {
        try {
            if (!appConfig.isDockerBaseDirConfigured()) {
                log.info("⚠️ Docker运行目录未配置，跳过自动创建");
                return false;
            }

            String dockerBaseDir = appConfig.getDockerBaseDirOrThrow();

            if (!dockerBaseDir.endsWith("/")) {
                dockerBaseDir = dockerBaseDir + "/";
            }

            boolean shouldCreate = path.startsWith(dockerBaseDir) || path.equals(dockerBaseDir.substring(0, dockerBaseDir.length() - 1));

            if (shouldCreate) {
                log.info("✅ 检测到Docker配置目录，将自动创建: {}", path);
            } else {
                log.info("ℹ️ 非Docker配置目录，跳过创建: {} (Docker目录: {})", path, dockerBaseDir);
            }

            return shouldCreate;

        } catch (Exception e) {
            log.warn("⚠️ 检查Docker目录配置失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否为系统敏感路径
     * 
     * @param path 路径
     * @return 是否为敏感路径
     */
    private boolean isSystemSensitivePath(String path) {
        String[] sensitivePaths = {
            "/", "/bin", "/sbin", "/usr/bin", "/usr/sbin",
            "/etc", "/boot", "/dev", "/proc", "/sys", "/run",
            "/lib", "/lib64", "/usr/lib", "/usr/lib64",
            "/var/run", "/var/log/system", "/tmp"
        };

        for (String sensitivePath : sensitivePaths) {
            if (path.equals(sensitivePath) || path.startsWith(sensitivePath + "/")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取实际的文件系统路径（适配容器化部署）
     * 
     * @param hostPath 宿主机路径
     * @return 实际的文件系统路径
     */
    private String getActualFilePath(String hostPath) {
        // 容器化部署，通过 /mnt/host 访问宿主机文件系统
        String actualPath = "/mnt/host" + hostPath;
        log.debug("🐳 容器化部署，实际操作路径: {} -> {}", hostPath, actualPath);
        return actualPath;
    }

}