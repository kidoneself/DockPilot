package com.dsm.service.impl;

import com.dsm.api.DockerService;
import com.dsm.config.AppConfig;
import com.dsm.exception.BusinessException;
import com.dsm.mapper.ImageStatusMapper;
import com.dsm.model.dto.ImageStatusDTO;
import com.dsm.pojo.dto.image.*;
import com.dsm.pojo.entity.ImageStatus;
import com.dsm.service.ImageService;
import com.dsm.utils.LogUtil;
import com.dsm.websocket.callback.PullImageCallback;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.GraphDriver;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 容器服务实现类
 * 实现容器管理的具体业务逻辑
 */
@Service
public class ImageServiceImpl implements ImageService {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // 存储镜像拉取任务和进度信息
    @Resource
    private DockerService dockerService;
    @Resource
    private ImageStatusMapper imageStatusMapper;
    @Autowired
    private AppConfig appConfig;


    @Override
    @Transactional
    public void removeImage(String imageId, boolean removeStatus) {
        LogUtil.logSysInfo("删除镜像: " + imageId + ", 同时删除状态记录: " + removeStatus);
        try {
            // 获取镜像详情
            InspectImageResponse imageInfo = dockerService.getInspectImage(imageId);
            String[] repoTags = imageInfo.getRepoTags().toArray(new String[0]);
            // 删除Docker镜像
            dockerService.removeImage(imageId);
            // 如果需要同时删除状态记录
            if (removeStatus) {
                for (String repoTag : repoTags) {
                    String[] parts = repoTag.split(":");
                    if (parts.length >= 2) {
                        String name = parts[0];
                        String tag = parts[1];
                        // 删除数据库记录
                        imageStatusMapper.deleteByNameAndTag(name, tag);
                        LogUtil.logSysInfo("已删除镜像状态记录: " + name + ":" + tag);
                    }
                }
            }
            LogUtil.log("成功删除镜像: " + imageId + (removeStatus ? " (已删除状态记录)" : ""));
        } catch (Exception e) {
            LogUtil.logSysError("删除镜像失败: " + e.getMessage());
            throw new RuntimeException("删除镜像失败: " + e.getMessage());
        }
    }


    /**
     * 每小时定时检查所有镜像更新状态
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Override
    public void checkAllImagesStatus() {
        LogUtil.logSysInfo("开始定时检查所有镜像更新状态...");
        try {
            // 首先同步宿主机所有镜像到数据库
            syncAllLocalImagesToDb();

            // 然后查询所有镜像记录进行更新检查
            List<ImageStatus> imageRecords = imageStatusMapper.selectAll();

            LogUtil.logSysInfo("找到 " + imageRecords.size() + " 条镜像记录需要检查");
            for (ImageStatus record : imageRecords) {
                try {
                    String name = record.getName();
                    String tag = record.getTag();
                    String storedLocalCreateTime = record.getLocalCreateTime();
                    Long id = record.getId();
                    String remoteCreateTime = dockerService.getRemoteImageCreateTime(name, tag);
                    Instant localInstant = Instant.parse(storedLocalCreateTime);
                    Instant remoteInstant = Instant.parse(remoteCreateTime);

                    LogUtil.logSysInfo("通过HTTP获取到远程镜像创建时间: " + remoteCreateTime);
                    LogUtil.logSysInfo("通过DOKCER取到本地镜像创建时间: " + storedLocalCreateTime);
                    // 如果远程时间晚于本地时间，说明需要更新
                    boolean needUpdate = remoteInstant.isAfter(localInstant);
                    // 更新数据库记录 - 使用ISO格式日期
                    String currentTime = getCurrentIsoDateTime();
                    imageStatusMapper.updateRemoteCreateTime(id, remoteCreateTime, needUpdate, currentTime);
                    LogUtil.logSysInfo("镜像 " + name + ":" + tag + " 检查完成 - 需要更新: " + needUpdate);
                } catch (Exception e) {
                    LogUtil.logSysError("检查镜像状态异常: " + record.getName() + ":" + record.getTag() + ", 错误: " + e.getMessage());
                }
            }
            LogUtil.logSysInfo("所有镜像更新状态检查完成");
        } catch (Exception e) {
            LogUtil.logSysError("检查镜像更新状态失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> updateImage(String imageName, String tag) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取远程镜像创建时间
            String remoteCreateTime = getRemoteImageCreateTime(imageName, tag);

            // 拉取镜像
            StringBuilder pullOutput = new StringBuilder();
            dockerService.pullImage(imageName, tag, new PullImageCallback() {
                @Override
                public void onProgress(int progress, String status) {
                    pullOutput.append(status).append("\n");
                    LogUtil.logSysInfo("拉取进度: " + status);
                }

                @Override
                public void onComplete() {
                    LogUtil.logSysInfo("镜像拉取完成");
                }

                @Override
                public void onError(String error) {
                    LogUtil.logSysError("镜像拉取失败: " + error);
                }
            });

            // 同步本地镜像信息到数据库
            Map<String, Object> syncResult = syncLocalImageToDb(imageName, tag);

            result.put("success", true);
            result.put("message", "镜像更新成功");
            result.put("remoteCreateTime", remoteCreateTime);
            result.put("pull_output", pullOutput.toString());

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
            // 先同步宿主机镜像到数据库，确保显示最新数据
            syncAllLocalImagesToDb();

            // 获取所有本地镜像
            List<Image> images = dockerService.listImages();

            // 获取数据库中的所有镜像状态记录
            List<ImageStatus> dbRecords = imageStatusMapper.selectAll();

            // 构建数据库记录的映射表，键为"name:tag"
            Map<String, ImageStatus> dbRecordsMap = dbRecords.stream().collect(Collectors.toMap(record -> record.getName() + ":" + record.getTag(), record -> record, (existing, replacement) -> existing // 如果有重复，保留第一个
            ));

            // 合并本地镜像和数据库记录
            List<ImageStatusDTO> result = new ArrayList<>();

            for (Image image : images) {
                String[] repoTags = image.getRepoTags();
                if (repoTags != null) {
                    for (String repoTag : repoTags) {
                        if (!"<none>:<none>".equals(repoTag)) {
                            String[] parts = repoTag.split(":");
                            String name = parts[0];
                            String tag = parts.length > 1 ? parts[1] : "latest";

                            ImageStatusDTO imageStatusDTO = ImageStatusDTO.builder().id(image.getId()).name(name).tag(tag).size(image.getSize()).created(new Date(image.getCreated() * 1000L)).build();

                            // 添加状态信息
                            ImageStatus statusRecord = dbRecordsMap.get(name + ":" + tag);
                            if (statusRecord != null) {
                                imageStatusDTO.setNeedUpdate(statusRecord.getNeedUpdate());
                                imageStatusDTO.setStatusId(statusRecord.getId());
                                imageStatusDTO.setLocalCreateTime(statusRecord.getLocalCreateTime());
                                imageStatusDTO.setRemoteCreateTime(statusRecord.getRemoteCreateTime());

                                // 将ISO格式日期字符串转换为Date对象
                                String lastCheckedStr = statusRecord.getLastChecked();
                                if (lastCheckedStr != null && !lastCheckedStr.isEmpty()) {
                                    imageStatusDTO.setLastChecked(parseIsoDate(lastCheckedStr));
                                }
                            }
                            result.add(imageStatusDTO);
                        }
                    }
                }
            }

            return result;
        } catch (Exception e) {
            LogUtil.logSysError("获取镜像状态列表失败: " + e.getMessage());
            throw new BusinessException("获取镜像状态列表失败");
        }
    }

    @Transactional
    public Map<String, Object> syncLocalImageToDb(String imageName, String tag) {
        LogUtil.logSysInfo("同步特定镜像到数据库: " + imageName + ":" + tag);
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取本地镜像创建时间
            String localCreateTime = getLocalImageCreateTime(imageName, tag);
            if (localCreateTime == null || localCreateTime.isEmpty()) {
                result.put("success", false);
                result.put("message", "未找到本地镜像");
                return result;
            }

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
                                    LogUtil.logSysInfo("镜像 " + name + ":" + tag + " 无法获取有效创建时间，跳过同步");
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
                                    LogUtil.logSysInfo("已创建镜像状态记录: " + name + ":" + tag);
                                    syncCount++;
                                } else if (!localCreateTime.equals(existingRecord.getLocalCreateTime())) {
                                    // 仅当创建时间不同时更新记录，避免不必要的数据库操作
                                    existingRecord.setLocalCreateTime(localCreateTime);
                                    existingRecord.setLastChecked(currentTime);
                                    imageStatusMapper.update(existingRecord);
                                    LogUtil.logSysInfo("已更新镜像状态记录: " + name + ":" + tag);
                                    syncCount++;
                                } else {
                                    LogUtil.logSysInfo("镜像 " + name + ":" + tag + " 无变化，跳过更新");
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

    public String getRemoteImageCreateTime(String imageName, String tag) {
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
            // 添加 --insecure-policy 和 --tls-verify=false 参数
            command.add("--insecure-policy");
            command.add("--tls-verify=false");
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
     * 将ISO8601格式的日期字符串转换为Date对象
     *
     * @param isoDateString ISO格式的日期字符串
     * @return Date对象
     */
    private Date parseIsoDate(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) {
            return null;
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(isoDateString, ISO_FORMATTER);
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            LogUtil.logSysInfo("解析ISO日期失败: " + isoDateString);
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
    public ImageInspectDTO getImageDetail(String imageName) {
        InspectImageResponse response = dockerService.getInspectImage(imageName);


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


    /**
     * 通过WebSocket发送进度消息
     *
     * @param taskId       任务ID
     * @param progressData 进度数据
     */
    private void sendPullProgressMessage(String taskId, Map<String, Object> progressData) {
        try {
            // 创建一个副本，避免ConcurrentModificationException
            Map<String, Object> dataCopy = new HashMap<>(progressData);

            // 添加时间戳，确保每次消息都有变化
            dataCopy.put("timestamp", System.currentTimeMillis());

            // 发送到特定任务的WebSocket主题
//            messagingTemplate.convertAndSend("/topic/image/pull/" + taskId, dataCopy);

            LogUtil.logSysInfo("已发送进度消息: 任务=" + taskId + ", 进度=" + dataCopy.get("progress") + "%, 状态=" + dataCopy.get("status"));
        } catch (Exception e) {
            LogUtil.logSysError("发送WebSocket消息失败: " + e.getMessage());
        }
    }
}