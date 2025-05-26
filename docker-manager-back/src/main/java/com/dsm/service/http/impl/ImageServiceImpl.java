package com.dsm.service.http.impl;

import com.dsm.api.DockerService;
import com.dsm.common.config.AppConfig;
import com.dsm.common.exception.BusinessException;
import com.dsm.mapper.ImageStatusMapper;
import com.dsm.model.*;
import com.dsm.service.http.ImageService;
import com.dsm.service.http.SystemSettingService;
import com.dsm.utils.LogUtil;
import com.dsm.utils.MessageCallback;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.GraphDriver;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 容器服务实现类
 * 实现容器管理的具体业务逻辑
 */
@Service
@DependsOn("databaseConfig") // 确保数据库配置完成后再初始化
public class ImageServiceImpl implements ImageService {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Resource
    private DockerService dockerService;
    @Resource
    private ImageStatusMapper imageStatusMapper;
    @Autowired
    private AppConfig appConfig;
    @Autowired
    private SystemSettingService systemSettingService;

    // 🎯 缓存相关字段
    private final Map<String, CachedImageInfo> remoteImageCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 30 * 60 * 1000; // 30分钟缓存

    // 🎯 动态任务调度相关字段
    private TaskScheduler taskScheduler;
    private ScheduledFuture<?> imageCheckTask;
    private long currentCheckInterval = 60 * 60 * 1000; // 默认1小时，单位毫秒

    /**
     * 缓存镜像信息内部类
     */
    private static class CachedImageInfo {
        final String createTime;
        final long timestamp;

        CachedImageInfo(String createTime, long timestamp) {
            this.createTime = createTime;
            this.timestamp = timestamp;
        }
    }

    @PostConstruct
    public void init() {
        // 在服务启动时清理所有拉取中的状态
        cleanAllPullingImages();
        
        // 🎯 初始化任务调度器
        initTaskScheduler();
        
        // 🎯 从数据库加载检查间隔配置并启动定时任务
        loadAndStartImageCheckTask();
    }

    /**
     * 初始化任务调度器
     */
    private void initTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("image-check-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        this.taskScheduler = scheduler;
        LogUtil.logSysInfo("镜像检查任务调度器初始化完成");
    }

    /**
     * 加载配置并启动镜像检查任务
     */
    private void loadAndStartImageCheckTask() {
        try {
            // 从数据库加载检查间隔配置
            String intervalStr = systemSettingService.get("imageCheckInterval");
            if (intervalStr != null && !intervalStr.isEmpty()) {
                try {
                    long interval = Long.parseLong(intervalStr) * 60 * 1000; // 配置以分钟为单位，转换为毫秒
                    if (interval >= 10 * 60 * 1000) { // 最小10分钟
                        this.currentCheckInterval = interval;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.logSysError("解析镜像检查间隔配置失败，使用默认值: " + e.getMessage());
                }
            }
            
            // 启动定时任务
            scheduleImageCheckTask();
            LogUtil.logSysInfo("镜像检查任务已启动，间隔: " + (currentCheckInterval / 60000) + " 分钟");
        } catch (Exception e) {
            LogUtil.logSysError("启动镜像检查任务失败: " + e.getMessage());
            // 使用默认配置启动
            scheduleImageCheckTask();
        }
    }

    /**
     * 🎯 调度镜像检查任务
     */
    private void scheduleImageCheckTask() {
        // 取消现有任务
        if (imageCheckTask != null && !imageCheckTask.isCancelled()) {
            imageCheckTask.cancel(false);
            LogUtil.logSysInfo("已取消现有的镜像检查任务");
        }
        
        // 启动新任务 - 使用固定延迟调度
        imageCheckTask = taskScheduler.scheduleWithFixedDelay(
            this::checkAllImagesStatus,
            java.time.Instant.now().plusMillis(60000), // 1分钟后开始
            java.time.Duration.ofMillis(currentCheckInterval) // 使用配置的间隔
        );
        
        LogUtil.logSysInfo("镜像检查任务已调度，间隔: " + (currentCheckInterval / 60000) + " 分钟");
    }

    /**
     * 🎯 通过事件监听器更新检查间隔配置
     * 供SystemSettingChangedListener调用
     */
    public void updateImageCheckIntervalFromEvent(String intervalValue) {
        try {
            // 验证和解析参数
            int intervalMinutes = Integer.parseInt(intervalValue);
            
            if (intervalMinutes < 10) {
                LogUtil.logSysError("镜像检查间隔配置无效，小于10分钟: " + intervalMinutes);
                return;
            }
            
            if (intervalMinutes > 24 * 60) { // 最大24小时
                LogUtil.logSysError("镜像检查间隔配置无效，超过24小时: " + intervalMinutes);
                return;
            }
            
            // 更新当前间隔
            this.currentCheckInterval = intervalMinutes * 60 * 1000L;
            
            // 重新调度任务
            scheduleImageCheckTask();
            
            LogUtil.logSysInfo("✅ 镜像检查间隔已热更新: " + intervalMinutes + " 分钟");
            
        } catch (NumberFormatException e) {
            LogUtil.logSysError("解析镜像检查间隔配置失败: " + intervalValue + ", 错误: " + e.getMessage());
        } catch (Exception e) {
            LogUtil.logSysError("更新镜像检查间隔失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void removeImage(String imageId, boolean removeStatus) {
        LogUtil.logSysInfo("删除镜像: " + imageId + ", 同时删除状态记录: " + removeStatus);

        boolean dockerImageExists = false;
        String[] repoTags = null;

        try {
            // 尝试获取镜像详情
            InspectImageResponse imageInfo = dockerService.getInspectImage(imageId);
            repoTags = imageInfo.getRepoTags().toArray(new String[0]);
            dockerImageExists = true;

            // 删除Docker镜像
            dockerService.removeImage(imageId);
            LogUtil.logSysInfo("已删除Docker镜像: " + imageId);
        } catch (Exception e) {
            // Docker镜像不存在或删除失败，记录日志但继续执行删除数据库记录的操作
            LogUtil.logSysInfo("Docker镜像不存在或删除失败: " + imageId + ", 错误: " + e.getMessage());

            // 如果Docker中不存在镜像，尝试从imageId解析name和tag
            if (imageId.contains(":")) {
                String[] parts = imageId.split(":", 2);
                repoTags = new String[]{imageId}; // 直接使用imageId作为repoTag
            }
        }

        // 删除数据库记录
        if (removeStatus) {
            if (repoTags != null && repoTags.length > 0) {
                // 从repoTags中解析并删除记录
                for (String repoTag : repoTags) {
                    String[] parts = repoTag.split(":", 2);
                    if (parts.length >= 2) {
                        String name = parts[0];
                        String tag = parts[1];
                        // 删除数据库记录
                        int deletedRows = imageStatusMapper.deleteByNameAndTag(name, tag);
                        if (deletedRows > 0) {
                            LogUtil.logSysInfo("已删除镜像状态记录: " + name + ":" + tag);
                        } else {
                            LogUtil.logSysInfo("未找到要删除的镜像状态记录: " + name + ":" + tag);
                        }
                    }
                }
            } else {
                // 如果无法解析repoTags，尝试直接从imageId解析
                if (imageId.contains(":")) {
                    String[] parts = imageId.split(":", 2);
                    String name = parts[0];
                    String tag = parts[1];
                    int deletedRows = imageStatusMapper.deleteByNameAndTag(name, tag);
                    if (deletedRows > 0) {
                        LogUtil.logSysInfo("已删除镜像状态记录: " + name + ":" + tag);
                    } else {
                        LogUtil.logSysInfo("未找到要删除的镜像状态记录: " + name + ":" + tag);
                    }
                }
            }
        }

        String successMessage = dockerImageExists
                ? "成功删除镜像: " + imageId
                : "成功删除镜像记录: " + imageId;
        if (removeStatus) {
            successMessage += " (已删除状态记录)";
        }
        LogUtil.logOpe(successMessage);
    }


    /**
     * 🎯 检查所有镜像更新状态（动态调度）
     */
    @Override
    public void checkAllImagesStatus() {
        LogUtil.logSysInfo("开始定时检查所有镜像更新状态...");
        try {
            // 获取Docker中真实存在的镜像
            List<Image> dockerImages = dockerService.listImages();
            Map<String, Image> dockerImageMap = new HashMap<>();

            for (Image dockerImage : dockerImages) {
                if (dockerImage.getRepoTags() != null) {
                    for (String repoTag : dockerImage.getRepoTags()) {
                        if (!"<none>:<none>".equals(repoTag)) {
                            dockerImageMap.put(repoTag, dockerImage);
                        }
                    }
                }
            }

            // 同步真实存在的镜像到数据库
            syncExistingImagesToDb(dockerImageMap);

            // 只检查真实存在镜像的远程更新状态
            List<ImageStatus> imageRecords = imageStatusMapper.selectAll();
            for (ImageStatus record : imageRecords) {
                // 只检查Docker中真实存在的镜像，跳过拉取记录
                String fullName = record.getName() + ":" + record.getTag();
                if (!dockerImageMap.containsKey(fullName)) {
                    continue; // 跳过不存在的镜像（拉取失败或拉取中的记录）
                }

                try {
                    String name = record.getName();
                    String tag = record.getTag();
                    String storedLocalCreateTime = record.getLocalCreateTime();
                    Long id = record.getId();

                    // 获取远程镜像创建时间进行比较
                    String remoteCreateTime = getRemoteImageCreateTime(name, tag);
                    Instant localInstant = parseToInstant(storedLocalCreateTime);
                    Instant remoteInstant = parseToInstant(remoteCreateTime);

                    // 检查时间解析是否成功
                    if (localInstant == null || remoteInstant == null) {
                        LogUtil.logSysError("时间解析失败，跳过镜像更新检查: " + name + ":" + tag + 
                            " (本地时间: " + storedLocalCreateTime + ", 远程时间: " + remoteCreateTime + ")");
                        continue;
                    }

                    // 如果远程时间晚于本地时间，说明需要更新
                    boolean needUpdate = remoteInstant.isAfter(localInstant);

                    // 更新数据库记录
                    String currentTime = getCurrentIsoDateTime();
                    imageStatusMapper.updateRemoteCreateTime(id, remoteCreateTime, needUpdate, currentTime);
                } catch (Exception e) {
                    LogUtil.logSysError("检查镜像状态异常: " + record.getName() + ":" + record.getTag() + ", 错误: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LogUtil.logSysError("检查镜像更新状态失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> updateImage(String imageName, String tag) {
        Map<String, Object> result = new HashMap<>();

        try {
//            // 获取远程镜像创建时间
//            String remoteCreateTime = getRemoteImageCreateTime(imageName, tag);
//
//            // 拉取镜像
////            StringBuilder pullOutput = new StringBuilder();
//            dockerService.pullImage(imageName, tag, new PullImageCallback() {
//                @Override
//                public void onProgress(int progress) {
//                    super();
//                }
//
//                @Override
//                public void onLog(String log) {
//                    super
//                }
//
//                @Override
//                public void onComplete() {
//                    super
//                }
//
//                @Override
//                public void onError(String error) {
//                    super
//                }
//            });
//
//            // 同步本地镜像信息到数据库，
//            syncLocalImageToDb(imageName, tag);

//            result.put("success", true);
//            result.put("message", "镜像开始更新");
//            result.put("remoteCreateTime", remoteCreateTime);
//            result.put("pull_output", pullOutput.toString());

            return result;
        } catch (Exception e) {
            LogUtil.logSysError("更新镜像失败: " + e.getMessage());
            result.put("success", false);
            result.put("message", "更新镜像失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public List<ImageStatusDTO> listImages() {
        LogUtil.logSysInfo("获取镜像状态列表");
        try {
            // 第一步：获取Docker中真实存在的所有镜像
            List<Image> dockerImages = dockerService.listImages();
            Map<String, Image> dockerImageMap = new HashMap<>();

            for (Image dockerImage : dockerImages) {
                if (dockerImage.getRepoTags() != null) {
                    for (String repoTag : dockerImage.getRepoTags()) {
                        if (!"<none>:<none>".equals(repoTag)) {
                            dockerImageMap.put(repoTag, dockerImage);
                        }
                    }
                }
            }

            // 第二步：同步真实存在的镜像到数据库
            syncExistingImagesToDb(dockerImageMap);

            // 第三步：获取所有数据库记录（包括拉取成功、失败、进行中的）
            List<ImageStatus> dbRecords = imageStatusMapper.selectAll();

            // 第四步：转换为DTO并分类
            List<ImageStatusDTO> result = new ArrayList<>();
            for (ImageStatus record : dbRecords) {
                String fullName = record.getName() + ":" + record.getTag();
                Image dockerImage = dockerImageMap.get(fullName);

                ImageStatusDTO dto = ImageStatusDTO.builder()
                        .id(record.getId() != null ? record.getId().toString() : "unknown")
                        .name(record.getName())
                        .tag(record.getTag())
                        .needUpdate(record.getNeedUpdate() != null ? record.getNeedUpdate() : false)
                        .statusId(record.getId())
                        .localCreateTime(record.getLocalCreateTime())
                        .remoteCreateTime(record.getRemoteCreateTime())
                        .pulling(record.getPulling() != null ? record.getPulling() : false)
                        .progress(record.getProgress())
                        .build();

                // 判断镜像类型并设置相应信息
                if (dockerImage != null) {
                    // 真实存在的镜像（拉取成功）
                    dto.setSize(dockerImage.getSize());
                    dto.setCreated(new Date(dockerImage.getCreated() * 1000L));
                } else {
                    // Docker中不存在的记录（拉取失败或拉取中）
                    dto.setSize(0L);
                    dto.setCreated(new Date());
                }

                // 处理时间格式转换
                String lastCheckedStr = record.getLastChecked();
                if (lastCheckedStr != null && !lastCheckedStr.isEmpty()) {
                    dto.setLastChecked(parseIsoDate(lastCheckedStr));
                }

                result.add(dto);
            }

//            LogUtil.logSysInfo("获取镜像列表完成，总计: " + result.size() + " 条记录");
            return result;
        } catch (Exception e) {
            LogUtil.logSysError("获取镜像状态列表失败: " + e.getMessage());
            throw new BusinessException("获取镜像状态列表失败");
        }
    }

    /**
     * 同步Docker中真实存在的镜像到数据库
     * 只处理已经存在的镜像，不影响拉取记录
     */
    private void syncExistingImagesToDb(Map<String, Image> dockerImageMap) {
//        LogUtil.logSysInfo("开始同步Docker中真实存在的镜像到数据库...");

        int syncCount = 0;
        int skipCount = 0;

        for (Map.Entry<String, Image> entry : dockerImageMap.entrySet()) {
            String fullName = entry.getKey();
            Image dockerImage = entry.getValue();

            String[] parts = fullName.split(":");
            String name = parts[0];
            String tag = parts.length > 1 ? parts[1] : "latest";

            try {
                // 获取本地镜像创建时间
                String localCreateTime = dockerService.getLocalImageCreateTime(name, tag);
                if (localCreateTime == null || localCreateTime.isEmpty()) {
                    skipCount++;
                    continue;
                }

                // 检查数据库是否已有记录
                ImageStatus existingRecord = imageStatusMapper.selectByNameAndTag(name, tag);
                String currentTime = getCurrentIsoDateTime();

                if (existingRecord == null) {
                    // 新镜像，插入记录
                    ImageStatus imageStatus = ImageStatus.builder()
                            .name(name)
                            .tag(tag)
                            .localCreateTime(localCreateTime)
                            .remoteCreateTime(localCreateTime) // 初始设置与本地相同
                            .needUpdate(false)
                            .lastChecked(currentTime)
                            .pulling(false) // 已存在的镜像肯定不在拉取中
                            .progress(null) // 已存在的镜像没有拉取进度
                            .build();

                    imageStatusMapper.insert(imageStatus);
                    syncCount++;
                } else {
                    // 已有记录，只更新必要字段
                    boolean needUpdate = false;

                    // 如果本地创建时间变化了，说明镜像被更新过
                    if (!localCreateTime.equals(existingRecord.getLocalCreateTime())) {
                        existingRecord.setLocalCreateTime(localCreateTime);
                        needUpdate = true;
                    }

                    // 如果之前是拉取失败或拉取中，现在Docker中存在了，说明拉取成功了
                    if (Boolean.TRUE.equals(existingRecord.getPulling()) ||
                            (existingRecord.getProgress() != null && existingRecord.getProgress().contains("\"status\":\"failed\""))) {
                        existingRecord.setPulling(false);
                        existingRecord.setProgress(String.format(
                                "{\"status\":\"success\",\"percentage\":100,\"message\":\"拉取完成\",\"end_time\":\"%s\"}",
                                java.time.Instant.now().toString()
                        ));
                        needUpdate = true;
                    }

                    if (needUpdate) {
                        existingRecord.setLastChecked(currentTime);
                        imageStatusMapper.update(existingRecord);
                        syncCount++;
                    } else {
                        skipCount++;
                    }
                }
            } catch (Exception e) {
                LogUtil.logSysError("同步镜像 " + name + ":" + tag + " 失败: " + e.getMessage());
            }
        }

//        LogUtil.logSysInfo("同步真实镜像完成 - 处理: " + syncCount + ", 跳过: " + skipCount);
    }

    @Transactional
    public Map<String, Object> syncLocalImageToDb(String imageName, String tag) {
        LogUtil.logSysInfo("同步特定镜像到数据库: " + imageName + ":" + tag);
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取本地镜像创建时间
            //如果是更新镜像到此步骤，则这里获取镜像本的的时间一定是最新的
            String localCreateTime = getLocalImageCreateTime(imageName, tag);
            if (localCreateTime == null || localCreateTime.isEmpty()) {
                result.put("success", false);
                result.put("message", "未找到本地镜像");
                return result;
            }
            //更新镜像:这里一定是数据库有数据的，并且是需要更新的
            // 检查数据库是否已有记录
            ImageStatus existingRecord = imageStatusMapper.selectByNameAndTag(imageName, tag);

            // 当前ISO格式日期
            String currentTime = getCurrentIsoDateTime();

            if (existingRecord == null) {
                // 插入新记录
                ImageStatus imageStatus = ImageStatus.builder().name(imageName).tag(tag).localCreateTime(localCreateTime).remoteCreateTime(localCreateTime).needUpdate(false).lastChecked(currentTime).build();

                imageStatusMapper.insert(imageStatus);
                LogUtil.logSysInfo("已创建镜像状态记录: " + imageName + ":" + tag);
            } else {
                // 更新现有记录
                existingRecord.setLocalCreateTime(localCreateTime);
                existingRecord.setNeedUpdate(false);
                existingRecord.setLastChecked(currentTime);
                imageStatusMapper.update(existingRecord);
                LogUtil.logSysInfo("已更新镜像状态记录: " + imageName + ":" + tag);
            }

            result.put("success", true);
            result.put("message", "成功同步镜像信息");
            result.put("localCreateTime", localCreateTime);

            return result;
        } catch (Exception e) {
            LogUtil.logSysError("同步本地镜像到数据库失败: " + e.getMessage());
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 同步宿主机所有镜像到数据库
     * 保证数据库记录与宿主机镜像同步
     */
    public void syncAllLocalImagesToDb() {
        LogUtil.logSysInfo("开始同步宿主机所有镜像到数据库...");
        try {
            // 获取所有本地镜像
            List<Image> images = dockerService.listImages();
            int syncCount = 0;
            int skipCount = 0;
            for (Image image : images) {
                String[] repoTags = image.getRepoTags();
                if (repoTags != null) {
                    for (String repoTag : repoTags) {
                        // 跳过<none>:<none>这样的镜像
                        if (!"<none>:<none>".equals(repoTag)) {
                            String[] parts = repoTag.split(":");
                            String name = parts[0];
                            String tag = parts.length > 1 ? parts[1] : "latest";

                            try {
                                // 检查数据库是否已有记录
                                ImageStatus existingRecord = imageStatusMapper.selectByNameAndTag(name, tag);

                                // 获取本地镜像创建时间
                                String localCreateTime = dockerService.getLocalImageCreateTime(name, tag);

                                if (localCreateTime == null || localCreateTime.isEmpty()) {
//                                    LogUtil.logSysInfo("镜像 " + name + ":" + tag + " 无法获取有效创建时间，跳过同步");
                                    skipCount++;
                                    continue;
                                }

                                // 当前ISO格式日期
                                String currentTime = getCurrentIsoDateTime();

                                if (existingRecord == null) {
                                    // 插入新记录
                                    ImageStatus imageStatus = ImageStatus.builder().name(name).tag(tag).localCreateTime(localCreateTime).remoteCreateTime(localCreateTime) // 初始设置与本地相同，表示不需要更新
                                            .needUpdate(false).lastChecked(currentTime).build();

                                    imageStatusMapper.insert(imageStatus);
//                                    LogUtil.logSysInfo("已创建镜像状态记录: " + name + ":" + tag);
                                    syncCount++;
                                } else if (!localCreateTime.equals(existingRecord.getLocalCreateTime())) {
                                    // 仅当创建时间不同时更新记录，避免不必要的数据库操作
                                    existingRecord.setLocalCreateTime(localCreateTime);
                                    existingRecord.setLastChecked(currentTime);
                                    imageStatusMapper.update(existingRecord);
//                                    LogUtil.logSysInfo("已更新镜像状态记录: " + name + ":" + tag);
                                    syncCount++;
                                } else {
//                                    LogUtil.logSysInfo("镜像 " + name + ":" + tag + " 无变化，跳过更新");
                                    skipCount++;
                                }
                            } catch (Exception e) {
                                LogUtil.logSysError("同步镜像 " + name + ":" + tag + " 失败: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            LogUtil.logSysInfo("同步宿主机镜像完成 - 同步: " + syncCount + ", 跳过: " + skipCount);
        } catch (Exception e) {
            LogUtil.logSysError("同步宿主机镜像失败: " + e.getMessage());
        }
    }

    /**
     * 🎯 带缓存的远程镜像创建时间获取方法
     * 优先使用缓存，缓存过期或失败时调用远程API
     */
    private String getRemoteImageCreateTime(String imageName, String tag) {
        String fullName = imageName + ":" + tag;
        CachedImageInfo cached = remoteImageCache.get(fullName);
        
        // 检查缓存是否有效
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_DURATION) {
            LogUtil.logSysInfo("使用缓存的远程镜像信息: " + fullName);
            return cached.createTime;
        }
        
        try {
            // 调用原有的远程获取方法
            String remoteCreateTime = getRemoteImageCreateTimeFromApi(imageName, tag);
            
            // 成功获取后更新缓存
            remoteImageCache.put(fullName, new CachedImageInfo(remoteCreateTime, System.currentTimeMillis()));
            LogUtil.logSysInfo("已缓存远程镜像信息: " + fullName);
            
            return remoteCreateTime;
        } catch (Exception e) {
            // 如果有缓存（即使过期），在网络失败时也可以使用
            if (cached != null) {
                LogUtil.logSysInfo("网络失败，使用过期缓存: " + fullName + " (缓存时间: " + 
                    (System.currentTimeMillis() - cached.timestamp) / 1000 + "秒前)");
                return cached.createTime;
            }
            throw e;
        }
    }

    /**
     * 从远程API获取镜像创建时间（原有逻辑）
     */
    private String getRemoteImageCreateTimeFromApi(String imageName, String tag) {
        try {
            List<String> command = new ArrayList<>();
            command.add("skopeo");
            command.add("inspect");
            // 检查当前系统架构
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            // 只有在Mac的ARM架构(M系列芯片)上才需要指定架构参数
            if (osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64"))) {
                LogUtil.logSysInfo("检测到Mac ARM架构，强制指定arm64/linux架构参数");
                command.add("--override-arch");
                command.add("arm64");
                command.add("--override-os");
                command.add("linux");
            }
            // 添加安全策略参数 (移除旧版本skopeo不支持的TLS参数)
            command.add("--insecure-policy");
            // 注释掉不兼容的参数，--insecure-policy 已经能处理大部分TLS问题
            // command.add("--src-tls-verify=false");  // 旧版本skopeo不支持
            // command.add("--dest-tls-verify=false"); // 旧版本skopeo不支持
            command.add("docker://" + imageName + ":" + tag);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 设置代理（如果启用）
            String proxyUrl = appConfig.getProxyUrl();
            boolean useProxy = proxyUrl != null && !proxyUrl.isEmpty();
            if (useProxy) {

                Map<String, String> env = processBuilder.environment();
                env.put("HTTP_PROXY", proxyUrl);
                env.put("HTTPS_PROXY", proxyUrl);
            }

            // 打印完整命令行
            LogUtil.logSysInfo("执行镜像检查命令: " + String.join(" ", command));
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            // 读取标准输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 读取错误输出
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            // 等待命令完成，最多等待30秒
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("获取远程镜像创建时间超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                LogUtil.logSysError("skopeo命令执行失败，退出码: " + exitCode + ", 错误输出: " + errorOutput);
                throw new RuntimeException("获取远程镜像创建时间失败，退出码: " + exitCode + ", 错误输出: " + errorOutput);
            }

            // 解析JSON输出以提取创建时间
            String outputStr = output.toString();
            int createTimeIndex = outputStr.indexOf("\"Created\":");
            if (createTimeIndex >= 0) {
                int startPos = outputStr.indexOf("\"", createTimeIndex + 10) + 1;
                int endPos = outputStr.indexOf("\"", startPos);
                if (startPos > 0 && endPos > startPos) {
                    String createTime = outputStr.substring(startPos, endPos);
                    return createTime;
                }
            }
            throw new RuntimeException("无法从输出中解析镜像创建时间");
        } catch (Exception e) {
            LogUtil.logSysError("获取远程镜像创建时间失败: " + e.getMessage());
            throw new RuntimeException("获取远程镜像创建时间失败: " + e.getMessage());
        }
    }

    /**
     * 将多种格式的日期字符串转换为Instant对象
     * 支持格式：
     * 1. yyyy-MM-dd HH:mm:ss
     * 2. yyyy-MM-dd HH:mm:ss.nnnnnnnnn +0000 UTC
     * 3. ISO8601标准格式
     *
     * @param dateString 日期字符串
     * @return Instant对象
     */
    private Instant parseToInstant(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        
        try {
            // 尝试多种时间格式解析
            String cleanedDateString = dateString.trim();
            
            // 格式1: 处理包含纳秒和时区的格式 (如: 2025-05-26 05:48:36.357380367 +0000 UTC)
            if (cleanedDateString.contains(".") && cleanedDateString.contains("UTC")) {
                try {
                    // 移除 UTC 后缀，替换空格为 T，处理时区格式
                    String processedString = cleanedDateString.replace(" UTC", "")
                                                            .replaceFirst(" ", "T");
                    
                    // 如果时区是 +0000 格式，转换为 Z
                    if (processedString.endsWith("+0000")) {
                        processedString = processedString.replace("+0000", "Z");
                    }
                    
                    return Instant.parse(processedString);
                } catch (Exception ex) {
                    LogUtil.logSysInfo("格式1解析失败，尝试其他格式: " + dateString);
                }
            }
            
            // 格式2: 尝试标准ISO8601格式
            try {
                if (cleanedDateString.contains("T")) {
                    return Instant.parse(cleanedDateString);
                }
            } catch (Exception ex) {
                LogUtil.logSysInfo("ISO8601格式解析失败，尝试其他格式: " + dateString);
            }
            
            // 格式3: 尝试ISO_DATE_TIME格式
            try {
                if (cleanedDateString.contains("T")) {
                    LocalDateTime localDateTime = LocalDateTime.parse(cleanedDateString, DateTimeFormatter.ISO_DATE_TIME);
                    return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
                }
            } catch (Exception ex) {
                LogUtil.logSysInfo("ISO_DATE_TIME格式解析失败，尝试其他格式: " + dateString);
            }
            
            // 格式4: 原有的简单格式 (yyyy-MM-dd HH:mm:ss)
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(cleanedDateString, ISO_FORMATTER);
                return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception ex) {
                LogUtil.logSysInfo("简单格式解析失败: " + dateString);
            }
            
            LogUtil.logSysError("所有日期格式解析均失败: " + dateString);
            return null;
            
        } catch (Exception e) {
            LogUtil.logSysError("解析日期时发生异常: " + dateString + ", 错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 将多种格式的日期字符串转换为Date对象
     * 支持格式：
     * 1. yyyy-MM-dd HH:mm:ss
     * 2. yyyy-MM-dd HH:mm:ss.nnnnnnnnn +0000 UTC
     * 3. ISO8601标准格式
     *
     * @param dateString 日期字符串
     * @return Date对象
     */
    private Date parseIsoDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        
        try {
            Instant instant = parseToInstant(dateString);
            return instant != null ? Date.from(instant) : null;
        } catch (Exception e) {
            LogUtil.logSysError("解析日期时发生异常: " + dateString + ", 错误: " + e.getMessage());
            return null;
        }
    }

    public String getLocalImageCreateTime(String imageName, String tag) {
        LogUtil.logSysInfo("获取本地镜像创建时间: " + imageName + ":" + tag);
        try {
            // 找到镜像
            List<Image> images = dockerService.listImages();
            String fullName = imageName + ":" + tag;

            Optional<Image> targetImage = images.stream().filter(image -> image.getRepoTags() != null && Arrays.asList(image.getRepoTags()).contains(fullName)).findFirst();

            if (!targetImage.isPresent()) {
                LogUtil.logSysInfo("未找到本地镜像: " + fullName);
                return null;
            }

            // 获取镜像详情
            InspectImageResponse imageInfo = dockerService.getInspectImage(targetImage.get().getId());
            String createTime = imageInfo.getCreated();

            // 如果创建时间包含 "T"，则使用该值作为创建时间
            if (createTime != null && !createTime.isEmpty()) {
                LogUtil.logSysInfo("获取到本地镜像创建时间: " + createTime);
                return createTime;
            }

            LogUtil.logSysInfo("无法从镜像中提取有效创建时间");
            return null;
        } catch (Exception e) {
            LogUtil.logSysError("获取本地镜像创建时间失败: " + e.getMessage());
            throw new RuntimeException("获取本地镜像创建时间失败: " + e.getMessage());
        }
    }

    // 获取本地镜像的 CreateTime（Docker API）
    public String getDockerApiCreateTime(String imageName) {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        List<Image> images = dockerClient.listImagesCmd().exec();
        dockerClient.createContainerCmd(imageName).exec();
        String targetRepo = imageName.split(":")[0];
        for (Image image : images) {
            String[] repoDigests = image.getRepoDigests();
            if (repoDigests != null) {
                for (String digest : repoDigests) {
                    if (digest.startsWith(targetRepo + "@")) {
                        return digest.split("@")[1]; // 返回 T
                    }
                }
            }
        }
        return "❌ 未找到本地镜像（或无 createTime）";
    }

    private String getCurrentIsoDateTime() {
        return LocalDateTime.now().format(ISO_FORMATTER);
    }

    @Override
    public ImageInspectDTO getImageDetail(String imageId) {
        InspectImageResponse response = dockerService.getInspectImage(imageId);
        ImageInspectDTO imageInspectDTO = new ImageInspectDTO();
        imageInspectDTO.setId(response.getId());
        imageInspectDTO.setParent(response.getParent());
        imageInspectDTO.setComment(response.getComment());
        // 解析ISO 8601格式的日期字符串
        String createdStr = response.getCreated();
        if (createdStr != null && !createdStr.isEmpty()) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(createdStr, DateTimeFormatter.ISO_DATE_TIME);
                imageInspectDTO.setCreated(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()));
            } catch (Exception e) {
                LogUtil.logSysInfo("解析创建时间失败: " + createdStr + ", 使用当前时间");
                imageInspectDTO.setCreated(new Date());
            }
        } else {
            imageInspectDTO.setCreated(new Date());
        }
        imageInspectDTO.setContainer(response.getContainer());
        imageInspectDTO.setDockerVersion(response.getDockerVersion());
        imageInspectDTO.setAuthor(response.getAuthor());
        imageInspectDTO.setOs(response.getOs());
        imageInspectDTO.setOsVersion(response.getOsVersion());
        imageInspectDTO.setSize(response.getSize());
        imageInspectDTO.setVirtualSize(response.getVirtualSize());
        imageInspectDTO.setRepoDigests(response.getRepoDigests());
        imageInspectDTO.setRepoTags(response.getRepoTags());

        // 设置ContainerConfig
        ContainerConfig containerConfig = response.getContainerConfig();
        if (containerConfig != null) {
            ContainerConfigDTO containerConfigDTO = new ContainerConfigDTO();
            containerConfigDTO.setUser(containerConfig.getUser());
            containerConfigDTO.setAttachStdin(containerConfig.getAttachStdin());
            containerConfigDTO.setAttachStdout(containerConfig.getAttachStdout());
            containerConfigDTO.setAttachStderr(containerConfig.getAttachStderr());
            containerConfigDTO.setTty(containerConfig.getTty());
            containerConfigDTO.setEnv(containerConfig.getEnv() != null ? Arrays.asList(containerConfig.getEnv()) : null);
            containerConfigDTO.setCmd(containerConfig.getCmd() != null ? Arrays.asList(containerConfig.getCmd()) : null);
            containerConfigDTO.setEntrypoint(containerConfig.getEntrypoint() != null ? Arrays.asList(containerConfig.getEntrypoint()) : null);
            containerConfigDTO.setImage(containerConfig.getImage());
            containerConfigDTO.setLabels(containerConfig.getLabels());
            containerConfigDTO.setVolumes(containerConfig.getVolumes());
            containerConfigDTO.setWorkingDir(containerConfig.getWorkingDir());
            containerConfigDTO.setOnBuild(containerConfig.getOnBuild() != null ? Arrays.asList(containerConfig.getOnBuild()) : null);

            // 处理暴露的端口
            ExposedPort[] exposedPorts = containerConfig.getExposedPorts();
            if (exposedPorts != null) {
                Map<String, Object> portsMap = new HashMap<>();
                for (ExposedPort port : exposedPorts) {
                    if (port != null) {
                        String portStr = port.getPort() + "/" + port.getProtocol().name().toLowerCase();
                        portsMap.put(portStr, new HashMap<>());
                    }
                }
                containerConfigDTO.setExposedPorts(portsMap);
            } else {
                containerConfigDTO.setExposedPorts(new HashMap<>());
            }

            // 设置健康检查
            if (containerConfig.getHealthcheck() != null) {
                HealthcheckDTO healthcheckDTO = new HealthcheckDTO();
                healthcheckDTO.setTest(containerConfig.getHealthcheck().getTest());
                healthcheckDTO.setInterval(containerConfig.getHealthcheck().getInterval());
                healthcheckDTO.setTimeout(containerConfig.getHealthcheck().getTimeout());
                healthcheckDTO.setRetries(containerConfig.getHealthcheck().getRetries());
                healthcheckDTO.setStartPeriod(containerConfig.getHealthcheck().getStartPeriod());
                containerConfigDTO.setHealthcheck(healthcheckDTO);
            }

            imageInspectDTO.setContainerConfig(containerConfigDTO);
        }

        // 设置Config
        ContainerConfig config = response.getConfig();
        if (config != null) {
            ConfigDTO configDTO = new ConfigDTO();
            configDTO.setUser(config.getUser());
            configDTO.setAttachStdin(config.getAttachStdin());
            configDTO.setAttachStdout(config.getAttachStdout());
            configDTO.setAttachStderr(config.getAttachStderr());
            configDTO.setTty(config.getTty());
            configDTO.setEnv(config.getEnv() != null ? Arrays.asList(config.getEnv()) : null);
            configDTO.setCmd(config.getCmd() != null ? Arrays.asList(config.getCmd()) : null);
            configDTO.setEntrypoint(config.getEntrypoint() != null ? Arrays.asList(config.getEntrypoint()) : null);
            configDTO.setImage(config.getImage());
            configDTO.setLabels(config.getLabels());
            configDTO.setVolumes(config.getVolumes());
            configDTO.setWorkingDir(config.getWorkingDir());
            configDTO.setOnBuild(config.getOnBuild() != null ? config.getOnBuild().toString() : null);

            // 处理暴露的端口
            ExposedPort[] exposedPorts = config.getExposedPorts();
            if (exposedPorts != null) {
                Map<String, Object> portsMap = new HashMap<>();
                for (ExposedPort port : exposedPorts) {
                    if (port != null) {
                        String portStr = port.getPort() + "/" + port.getProtocol().name().toLowerCase();
                        portsMap.put(portStr, new HashMap<>());
                    }
                }
                configDTO.setExposedPorts(portsMap);
            } else {
                configDTO.setExposedPorts(new HashMap<>());
            }

            // 设置健康检查
            if (config.getHealthcheck() != null) {
                HealthcheckDTO healthcheckDTO = new HealthcheckDTO();
                healthcheckDTO.setTest(config.getHealthcheck().getTest());
                healthcheckDTO.setInterval(config.getHealthcheck().getInterval());
                healthcheckDTO.setTimeout(config.getHealthcheck().getTimeout());
                healthcheckDTO.setRetries(config.getHealthcheck().getRetries());
                healthcheckDTO.setStartPeriod(config.getHealthcheck().getStartPeriod());
                configDTO.setHealthcheck(healthcheckDTO);
            }

            imageInspectDTO.setConfig(configDTO);
        }

        // 设置GraphDriver
        GraphDriver graphDriver = response.getGraphDriver();
        if (graphDriver != null) {
            GraphDriverDTO graphDriverDTO = new GraphDriverDTO();
            graphDriverDTO.setName(graphDriver.getName());
//                graphDriverDTO.setData(graphDriver.getData() != null ? graphDriver.getData() : new HashMap<>());
            imageInspectDTO.setGraphDriver(graphDriverDTO);
        }

//        // 设置RootFS
//        RootFS rootFS = response.getRootFS();
//        if (rootFS != null) {
//            RootFSDTO rootFSDTO = new RootFSDTO();
//            rootFSDTO.setType(rootFS.getType());
//            rootFSDTO.setLayers(rootFS.getLayers() != null ? rootFS.getLayers() : null);
//            imageInspectDTO.setRootFS(rootFSDTO);
//        }

        return imageInspectDTO;
    }

    @Override
    public CompletableFuture<Void> pullImage(String image, String tag, MessageCallback callback) {
        return dockerService.pullImageWithSkopeo(image, tag, callback);
    }

    @Override
    public void startPullImage(String imageName, String tag) {
        // 检查是否已经在拉取中
        if (isPulling(imageName, tag)) {
            LogUtil.logSysInfo("镜像 " + imageName + ":" + tag + " 已经在拉取中，跳过");
            return;
        }

        // 构建进度JSON
        String progressJson = String.format(
                "{\"status\":\"pulling\",\"percentage\":0,\"message\":\"开始拉取镜像\",\"start_time\":\"%s\"}",
                java.time.Instant.now().toString()
        );

        // 创建或更新镜像状态记录
        ImageStatus status = ImageStatus.builder()
                .name(imageName)
                .tag(tag)
                .pulling(true)
                .progress(progressJson)
                .needUpdate(false)
                .build();

        // 尝试插入或更新
        imageStatusMapper.insertOrUpdate(status);
        LogUtil.logSysInfo("开始拉取镜像，记录状态: " + imageName + ":" + tag);
    }

    @Override
    public void updatePullProgress(String imageName, String tag, int percentage, String message) {
        ImageStatus existing = imageStatusMapper.selectByNameAndTag(imageName, tag);
        if (existing == null) {
            LogUtil.logSysError("尝试更新不存在的镜像进度: " + imageName + ":" + tag);
            return;
        }

        // 如果percentage为-1，保留原有进度百分比
        int currentPercentage = percentage;
        if (percentage == -1 && existing.getProgress() != null) {
            try {
                String progressStr = existing.getProgress();
                if (progressStr.contains("\"percentage\":")) {
                    int start = progressStr.indexOf("\"percentage\":") + 13;
                    int end = progressStr.indexOf(",", start);
                    if (end == -1) end = progressStr.indexOf("}", start);
                    if (end > start) {
                        currentPercentage = Integer.parseInt(progressStr.substring(start, end).trim());
                    }
                }
            } catch (Exception e) {
                LogUtil.logSysError("解析现有进度失败: " + e.getMessage());
                currentPercentage = 0;
            }
        }

        String progressJson = String.format(
                "{\"status\":\"pulling\",\"percentage\":%d,\"message\":\"%s\",\"update_time\":\"%s\"}",
                currentPercentage, message.replace("\"", "\\\""), java.time.Instant.now().toString()
        );

        existing.setPulling(true);
        existing.setProgress(progressJson);
        imageStatusMapper.update(existing);

        LogUtil.logSysInfo("更新拉取进度 " + imageName + ":" + tag + " - " + currentPercentage + "%: " + message);
    }

    @Override
    public void completePullImage(String imageName, String tag, String imageId) {
        ImageStatus existing = imageStatusMapper.selectByNameAndTag(imageName, tag);
        if (existing == null) {
            LogUtil.logSysError("尝试完成不存在的镜像拉取: " + imageName + ":" + tag);
            return;
        }

        // 获取拉取成功后的本地镜像创建时间
        String localCreateTime = null;
        try {
            localCreateTime = getLocalImageCreateTime(imageName, tag);
        } catch (Exception e) {
            LogUtil.logSysError("获取本地镜像创建时间失败: " + e.getMessage());
        }

        String progressJson = String.format(
                "{\"status\":\"success\",\"percentage\":100,\"message\":\"拉取完成\",\"end_time\":\"%s\"}",
                java.time.Instant.now().toString()
        );

        existing.setPulling(false);
        existing.setProgress(progressJson);
        existing.setImageId(imageId);
        existing.setLocalCreateTime(localCreateTime);
        existing.setNeedUpdate(false);
        imageStatusMapper.update(existing);

        LogUtil.logOpe("镜像拉取成功: " + imageName + ":" + tag + (imageId != null ? " (ID: " + imageId + ")" : ""));
    }

    @Override
    public void failPullImage(String imageName, String tag, String error) {
        ImageStatus existing = imageStatusMapper.selectByNameAndTag(imageName, tag);
        if (existing == null) {
            LogUtil.logSysError("尝试标记不存在的镜像拉取失败: " + imageName + ":" + tag);
            return;
        }

        // 将原始错误信息转换为用户友好的错误信息
        String userFriendlyError = parseUserFriendlyError(error);

        String progressJson = String.format(
                "{\"status\":\"failed\",\"percentage\":0,\"message\":\"拉取失败\",\"error\":\"%s\",\"end_time\":\"%s\"}",
                userFriendlyError.replace("\"", "\\\""), java.time.Instant.now().toString()
        );

        existing.setPulling(false);
        existing.setProgress(progressJson);
        imageStatusMapper.update(existing);

        // 记录日志时使用原始错误信息，给开发者看详细信息
        LogUtil.logSysError("镜像拉取失败: " + imageName + ":" + tag + " - 原始错误: " + error + " | 用户友好错误: " + userFriendlyError);
    }

    @Override
    public boolean isPulling(String imageName, String tag) {
        ImageStatus status = imageStatusMapper.selectByNameAndTag(imageName, tag);
        return status != null && Boolean.TRUE.equals(status.getPulling());
    }

    /**
     * 在服务启动时清理所有拉取中的状态
     * 将所有正在拉取的镜像状态标记为失败，避免服务重启后状态不一致
     */
    private void cleanAllPullingImages() {
        LogUtil.logSysInfo("🔧 服务启动 - 开始清理所有拉取中的状态...");

        try {
            // 获取所有镜像状态记录
            List<ImageStatus> allStatuses = imageStatusMapper.selectAll();
            int cleanedCount = 0;

            for (ImageStatus status : allStatuses) {
                // 检查是否正在拉取中
                if (Boolean.TRUE.equals(status.getPulling())) {
                    // 构建服务重启导致拉取失败的进度JSON - 使用用户友好的错误信息
                    String userFriendlyError = "后端服务重启，拉取进程被中断";
                    String failureProgressJson = String.format(
                            "{\"status\":\"failed\",\"percentage\":0,\"message\":\"服务重启导致拉取中断\",\"error\":\"%s\",\"end_time\":\"%s\"}",
                            userFriendlyError, java.time.Instant.now().toString()
                    );

                    // 更新状态为失败
                    status.setPulling(false);
                    status.setProgress(failureProgressJson);

                    imageStatusMapper.update(status);
                    cleanedCount++;

                    LogUtil.logSysInfo("已清理拉取中断的镜像: " + status.getName() + ":" + status.getTag());
                }
            }

            if (cleanedCount > 0) {
                LogUtil.logSysInfo("✅ 清理完成 - 共处理 " + cleanedCount + " 个拉取中断的镜像状态");
            } else {
                LogUtil.logSysInfo("✅ 无需清理 - 没有发现拉取中的镜像状态");
            }
        } catch (Exception e) {
            LogUtil.logSysError("❌ 清理拉取中状态失败: " + e.getMessage());
        }
    }

    /**
     * 将技术错误信息转换为用户友好的错误信息
     *
     * @param rawError 原始错误信息
     * @return 用户友好的错误信息
     */
    private String parseUserFriendlyError(String rawError) {
        if (rawError == null || rawError.isEmpty()) {
            return "镜像拉取失败";
        }

        String lowerError = rawError.toLowerCase();

        // 解析常见的错误类型，返回用户友好的信息
        if (lowerError.contains("requested access to the resource is denied")) {
            return "镜像访问被拒绝，可能不存在或需要认证";
        }

        if (lowerError.contains("not found") || lowerError.contains("manifest unknown")) {
            return "镜像未找到，请检查名称和标签是否正确";
        }

        if (lowerError.contains("unauthorized") || lowerError.contains("401")) {
            return "认证失败，需要登录认证";
        }

        if (lowerError.contains("timeout") || lowerError.contains("deadline exceeded")) {
            return "网络超时，请检查网络连接";
        }

        if (lowerError.contains("connection refused") || lowerError.contains("connection reset")) {
            return "网络连接被拒绝，请检查网络设置";
        }

        if (lowerError.contains("no such host") || lowerError.contains("name resolution")) {
            return "域名解析失败，请检查网络连接";
        }

        if (lowerError.contains("certificate") || lowerError.contains("tls") || lowerError.contains("ssl")) {
            return "证书验证失败，请检查网络设置";
        }

        if (lowerError.contains("too many requests") || lowerError.contains("rate limit")) {
            return "请求过于频繁，请稍后重试";
        }

        if (lowerError.contains("disk") || lowerError.contains("space")) {
            return "磁盘空间不足";
        }

        if (lowerError.contains("interrupted") || lowerError.contains("中断")) {
            return "操作被中断";
        }

        if (lowerError.contains("skopeo") && lowerError.contains("127")) {
            return "skopeo工具未安装，请联系管理员";
        }

        if (lowerError.contains("skopeo 命令执行失败")) {
            return "镜像拉取失败，请检查镜像名称或网络连接";
        }

        // 如果是其他类型的错误，尽量简化显示
        if (rawError.length() > 100) {
            return "镜像拉取失败，请重试或联系管理员";
        }

        return rawError;
    }

    /**
     * 🎯 缓存管理方法
     */
    
    /**
     * 清理过期的缓存条目
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 每小时清理一次过期缓存
    void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        
        Iterator<Map.Entry<String, CachedImageInfo>> iterator = remoteImageCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CachedImageInfo> entry = iterator.next();
            if ((currentTime - entry.getValue().timestamp) > CACHE_DURATION * 2) { // 超过2倍缓存时间才清理
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            LogUtil.logSysInfo("清理过期缓存条目: " + removedCount + " 个");
        }
    }
    
    /**
     * 手动清理特定镜像的缓存
     */
    public void clearImageCache(String imageName, String tag) {
        String fullName = imageName + ":" + tag;
        CachedImageInfo removed = remoteImageCache.remove(fullName);
        if (removed != null) {
            LogUtil.logSysInfo("已清理镜像缓存: " + fullName);
        }
    }
    
    /**
     * 清理所有缓存
     */
    public void clearAllCache() {
        int size = remoteImageCache.size();
        remoteImageCache.clear();
        LogUtil.logSysInfo("已清理所有镜像缓存，共 " + size + " 个条目");
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        long currentTime = System.currentTimeMillis();
        int validCacheCount = 0;
        int expiredCacheCount = 0;
        
        for (CachedImageInfo info : remoteImageCache.values()) {
            if ((currentTime - info.timestamp) < CACHE_DURATION) {
                validCacheCount++;
            } else {
                expiredCacheCount++;
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCacheCount", remoteImageCache.size());
        stats.put("validCacheCount", validCacheCount);
        stats.put("expiredCacheCount", expiredCacheCount);
        stats.put("cacheDurationMinutes", CACHE_DURATION / (60 * 1000));
        
        return stats;
    }

}