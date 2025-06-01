package com.dockpilot.service.http.impl;

import com.dockpilot.api.DockerService;
import com.dockpilot.mapper.ContainerInfoMapper;
import com.dockpilot.model.ContainerInfo;
import com.dockpilot.service.http.ContainerSyncService;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@DependsOn("databaseConfig") // 确保数据库配置完成后再初始化
public class ContainerSyncServiceImpl implements ContainerSyncService {

    private final AtomicBoolean isSyncing = new AtomicBoolean(false);
    @Autowired
    private DockerService dockerService;
    @Autowired
    private ContainerInfoMapper containerInfoMapper;

    @Override
    public void syncContainers() {
        if (isSyncing.compareAndSet(false, true)) {
            try {
                doSync();
            } finally {
                isSyncing.set(false);
            }
        } else {
            log.info("同步任务正在执行中，跳过本次同步");
        }
    }

    @Override
    public List<ContainerInfo> getContainerList() {
        syncContainers();
        return containerInfoMapper.selectAll();
    }

    @Scheduled(fixedRate = 300000, initialDelay = 30000) // 🔥 降低频率：5分钟执行一次，延迟30秒启动
    public void scheduledSync() {
        log.info("开始执行定时容器同步任务（兜底机制）...");
        try {
            syncContainers();
            log.info("定时容器同步任务执行完成");
        } catch (Exception e) {
            log.error("定时容器同步任务执行失败", e);
        }
    }

    @Transactional
    public void doSync() {
        try {
            // 0. 首次同步时清理重复记录
            cleanupDuplicateRecords();

            // 1. 获取Docker容器列表
            List<Container> dockerContainers = dockerService.listContainers();

            // 2. 获取数据库容器列表
            List<ContainerInfo> dbContainers = containerInfoMapper.selectAll();

            // 3. 同步处理
            syncContainerData(dockerContainers, dbContainers);

            log.info("容器数据同步完成，Docker容器数量: {}, 数据库容器数量: {}", dockerContainers.size(), dbContainers.size());

        } catch (Exception e) {
            log.error("容器数据同步失败", e);
            throw e;
        }
    }

    /**
     * 清理重复的容器记录
     */
    @Transactional
    public void cleanupDuplicateRecords() {
        try {
            List<ContainerInfo> allContainers = containerInfoMapper.selectAll();
            Map<String, List<ContainerInfo>> containerGroups = new HashMap<>();

            // 按containerId分组
            for (ContainerInfo container : allContainers) {
                String containerId = container.getContainerId();
                containerGroups.computeIfAbsent(containerId, k -> new java.util.ArrayList<>()).add(container);
            }

            // 处理重复记录
            int duplicatesFound = 0;
            int duplicatesRemoved = 0;

            for (Map.Entry<String, List<ContainerInfo>> entry : containerGroups.entrySet()) {
                List<ContainerInfo> duplicates = entry.getValue();
                if (duplicates.size() > 1) {
                    duplicatesFound += duplicates.size() - 1;
                    log.warn("发现容器 {} 有 {} 条重复记录", entry.getKey(), duplicates.size());

                    // 保留最新的记录（优先级：更新时间 > 创建时间 > ID）
                    ContainerInfo keepRecord = duplicates.stream()
                            .max((a, b) -> {
                                if (a.getUpdatedAt() != null && b.getUpdatedAt() != null) {
                                    int updateCompare = a.getUpdatedAt().compareTo(b.getUpdatedAt());
                                    if (updateCompare != 0) return updateCompare;
                                }
                                if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                                    int createCompare = a.getCreatedAt().compareTo(b.getCreatedAt());
                                    if (createCompare != 0) return createCompare;
                                }
                                if (a.getId() != null && b.getId() != null) {
                                    return a.getId().compareTo(b.getId());
                                }
                                return 0;
                            })
                            .orElse(duplicates.get(0));

                    // 删除其他重复记录
                    for (ContainerInfo duplicate : duplicates) {
                        if (!duplicate.getId().equals(keepRecord.getId())) {
                            try {
                                containerInfoMapper.deleteById(duplicate.getId());
                                duplicatesRemoved++;
                                log.info("删除重复容器记录: {} (ID: {}, 创建时间: {})",
                                        duplicate.getName(), duplicate.getId(), duplicate.getCreatedAt());
                            } catch (Exception e) {
                                log.error("删除重复记录失败: ID {}", duplicate.getId(), e);
                            }
                        }
                    }

                    log.info("保留容器 {} 的记录 ID: {} (创建时间: {}, 更新时间: {})",
                            entry.getKey(), keepRecord.getId(), keepRecord.getCreatedAt(), keepRecord.getUpdatedAt());
                }
            }

            if (duplicatesFound > 0) {
                log.info("重复记录清理完成：发现 {} 条重复记录，成功删除 {} 条", duplicatesFound, duplicatesRemoved);
            } else {
                log.debug("未发现重复的容器记录");
            }

        } catch (Exception e) {
            log.error("清理重复记录失败", e);
            throw e;
        }
    }

    private void syncContainerData(List<Container> dockerContainers, List<ContainerInfo> dbContainers) {
        // 1. 处理Docker中存在的容器
        for (Container dockerContainer : dockerContainers) {
            String containerName = dockerContainer.getNames()[0].replaceFirst("/", "");
            String containerId = dockerContainer.getId();

            // 先按容器ID查找（更精确）
            ContainerInfo dbContainerById = findContainerById(dbContainers, containerId);
            // 再按名称查找（兼容性）
            ContainerInfo dbContainerByName = findContainerByName(dbContainers, containerName);

            if (dbContainerById != null) {
                // 找到匹配的记录，只更新基本状态信息
                updateContainerBasicInfo(dbContainerById, dockerContainer);
            } else if (dbContainerByName != null) {
                // 按名称找到但ID不同，可能是容器更新操作产生的
                if (!dbContainerByName.getContainerId().equals(containerId)) {
                    log.debug("发现容器ID变化: {} 从 {} 变为 {}",
                            containerName, dbContainerByName.getContainerId(), containerId);
                    // 更新容器ID和基本信息
                    updateContainerIdAndBasicInfo(dbContainerByName, dockerContainer);
                } else {
                    // ID相同，只更新基本状态
                    updateContainerBasicInfo(dbContainerByName, dockerContainer);
                }
            } else {
                // 数据库中没有记录，检查是否是重复检查
                boolean isDuplicate = dbContainers.stream()
                        .anyMatch(info -> info.getContainerId().equals(containerId));

                if (!isDuplicate) {
                    // 确认没有重复记录才创建新的
                    createNewContainer(dockerContainer);
                } else {
                    log.debug("跳过重复的容器记录创建: {}", containerId);
                }
            }
        }

        // 2. 处理数据库中存在但Docker中不存在的容器（可能已被删除）
        for (ContainerInfo dbContainer : dbContainers) {
            if (!existsInDockerById(dockerContainers, dbContainer.getContainerId()) &&
                    !existsInDockerByName(dockerContainers, dbContainer.getName())) {
                // 只有在Docker中完全找不到时才删除记录
                containerInfoMapper.deleteById(dbContainer.getId());
                log.info("删除不存在的容器记录: {} ({})", dbContainer.getName(), dbContainer.getContainerId());
            }
        }
    }

    private ContainerInfo findContainerById(List<ContainerInfo> containers, String containerId) {
        return containers.stream()
                .filter(c -> c.getContainerId().equals(containerId))
                .findFirst()
                .orElse(null);
    }

    private ContainerInfo findContainerByName(List<ContainerInfo> containers, String name) {
        return containers.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private boolean existsInDockerById(List<Container> dockerContainers, String containerId) {
        return dockerContainers.stream()
                .anyMatch(c -> c.getId().equals(containerId));
    }

    private boolean existsInDockerByName(List<Container> dockerContainers, String name) {
        return dockerContainers.stream()
                .anyMatch(c -> c.getNames()[0].replaceFirst("/", "").equals(name));
    }

    private void updateContainerBasicInfo(ContainerInfo dbContainer, Container dockerContainer) {
        try {
            // 只更新基本状态信息，保留用户配置
            boolean needUpdate = false;

            if (!dbContainer.getStatus().equals(dockerContainer.getState())) {
                dbContainer.setStatus(dockerContainer.getState());
                needUpdate = true;
            }

            if (!dbContainer.getImage().equals(dockerContainer.getImage())) {
                dbContainer.setImage(dockerContainer.getImage());
                needUpdate = true;
            }

            if (needUpdate) {
                dbContainer.setUpdatedAt(new java.util.Date());
                containerInfoMapper.update(dbContainer);
                log.debug("更新容器基本信息: {}", dbContainer.getName());
            }
        } catch (Exception e) {
            log.error("更新容器基本信息失败: {}", dbContainer.getName(), e);
        }
    }

    private void updateContainerIdAndBasicInfo(ContainerInfo dbContainer, Container dockerContainer) {
        try {
            // 更新容器ID和基本信息
            dbContainer.setContainerId(dockerContainer.getId());
            dbContainer.setStatus(dockerContainer.getState());
            dbContainer.setImage(dockerContainer.getImage());
            dbContainer.setUpdatedAt(new java.util.Date());
            // 保留其他字段如iconUrl、webUrl、operationStatus等
            containerInfoMapper.update(dbContainer);
            log.debug("更新容器ID和基本信息: {} -> {}", dbContainer.getName(), dockerContainer.getId());
        } catch (Exception e) {
            log.error("更新容器ID和基本信息失败: {}", dbContainer.getName(), e);
        }
    }

    private void createNewContainer(Container dockerContainer) {
        try {
            ContainerInfo containerInfo = new ContainerInfo();
            containerInfo.setContainerId(dockerContainer.getId());
            containerInfo.setName(dockerContainer.getNames()[0].replaceFirst("/", ""));
            containerInfo.setImage(dockerContainer.getImage());
            containerInfo.setStatus(dockerContainer.getState());
            containerInfo.setOperationStatus("success"); // 设置操作状态
            containerInfo.setNeedUpdate(false); // 新容器默认不需要更新
            containerInfo.setCreatedAt(new java.util.Date()); // 设置创建时间
            containerInfo.setUpdatedAt(new java.util.Date()); // 设置更新时间
            containerInfoMapper.insert(containerInfo);
            log.debug("新增容器记录: {}", containerInfo.getName());
        } catch (Exception e) {
            log.error("创建容器记录失败: {}", dockerContainer.getNames()[0], e);
        }
    }

    @Scheduled(fixedRate = 21600000, initialDelay = 60000) // 🔥 降低频率：6小时执行一次，延迟1分钟启动
    public void checkContainerUpdates() {
        log.info("开始检查容器更新状态（兜底检查）...");
        try {
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

            for (Container container : containers) {

                String currentImageName = container.getImage();
                String imageIdToQuery = currentImageName;

                // If currentImageName does not contain a colon (no explicit tag),
                // and is not a digest, try to append ":latest" for the lookup.
                if (!currentImageName.contains(":") && !currentImageName.contains("@")) {
                    String potentialLatestKey = currentImageName + ":latest";
                    if (imageIdMap.containsKey(potentialLatestKey)) {
                        imageIdToQuery = potentialLatestKey;
                    }
                }

                String latestImageId = imageIdMap.get(imageIdToQuery);
                String actualContainerImageId = container.getImageId();

                Boolean needUpdate;
                if (latestImageId == null) {
                    // If we can't find the image (e.g., "image_name:latest" or "image_name:specific_tag" is not in local cache,
                    // or container.getImage() was an ID not present as a key in imageIdMap),
                    // consider it as needing an update or attention.
                    needUpdate = true;
                } else {
                    // If we found the latestImageId for the resolved tag,
                    // check if the container is running that specific image ID.
                    needUpdate = !latestImageId.equals(actualContainerImageId);
                }

                // 处理重复记录问题：使用 selectAll 然后过滤
                List<ContainerInfo> allContainerInfos = containerInfoMapper.selectAll();
                List<ContainerInfo> matchingContainers = allContainerInfos.stream()
                        .filter(info -> info.getContainerId().equals(container.getId()))
                        .collect(java.util.stream.Collectors.toList());

                if (matchingContainers.isEmpty()) {
                    // 如果数据库中不存在该容器记录，创建新记录
                    ContainerInfo newContainerInfo = new ContainerInfo();
                    newContainerInfo.setContainerId(container.getId());
                    newContainerInfo.setName(container.getNames()[0].replaceFirst("/", ""));
                    newContainerInfo.setImage(container.getImage());
                    newContainerInfo.setStatus(container.getState());
                    newContainerInfo.setOperationStatus("success");
                    newContainerInfo.setNeedUpdate(needUpdate);
                    newContainerInfo.setCreatedAt(new java.util.Date());
                    newContainerInfo.setUpdatedAt(new java.util.Date());
                    containerInfoMapper.insert(newContainerInfo);
                    log.debug("创建新容器记录: {}", newContainerInfo.getName());
                } else if (matchingContainers.size() == 1) {
                    // 正常情况：只有一条记录，更新needUpdate字段
                    ContainerInfo containerInfo = matchingContainers.get(0);
                    containerInfo.setNeedUpdate(needUpdate);
                    containerInfo.setUpdatedAt(new java.util.Date());
                    containerInfoMapper.update(containerInfo);
                } else {
                    // 发现重复记录，进行清理
                    log.warn("发现容器 {} 有 {} 条重复记录，正在清理...", container.getId(), matchingContainers.size());

                    // 保留最新的记录（根据ID或更新时间）
                    ContainerInfo keepRecord = matchingContainers.stream()
                            .max((a, b) -> {
                                if (a.getUpdatedAt() != null && b.getUpdatedAt() != null) {
                                    return a.getUpdatedAt().compareTo(b.getUpdatedAt());
                                } else if (a.getId() != null && b.getId() != null) {
                                    return a.getId().compareTo(b.getId());
                                }
                                return 0;
                            })
                            .orElse(matchingContainers.get(0));

                    // 删除其他重复记录
                    for (ContainerInfo containerInfo : matchingContainers) {
                        if (!containerInfo.getId().equals(keepRecord.getId())) {
                            containerInfoMapper.deleteById(containerInfo.getId());
                            log.info("删除重复的容器记录: {} (ID: {})", containerInfo.getName(), containerInfo.getId());
                        }
                    }

                    // 更新保留的记录
                    keepRecord.setNeedUpdate(needUpdate);
                    keepRecord.setUpdatedAt(new java.util.Date());
                    // 确保保留记录中的基本信息是最新的
                    keepRecord.setName(container.getNames()[0].replaceFirst("/", ""));
                    keepRecord.setImage(container.getImage());
                    keepRecord.setStatus(container.getState());
                    containerInfoMapper.update(keepRecord);
                    log.info("已清理容器 {} 的重复记录，保留记录ID: {}", container.getId(), keepRecord.getId());
                }
            }
            log.info("容器更新状态检查完成");
        } catch (Exception e) {
            log.error("检查容器更新状态时出错", e);
        }
    }
}