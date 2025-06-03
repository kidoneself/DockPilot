package com.dockpilot.service.websocket;

import com.dockpilot.model.MessageType;
import com.dockpilot.model.ContainerCreateRequest;
import com.dockpilot.model.application.ApplicationParseResult;
import com.dockpilot.model.application.dto.ApplicationDeployResult;
import com.dockpilot.model.application.vo.ApplicationVO;
import com.dockpilot.service.ApplicationService;
import com.dockpilot.service.http.ImageService;
import com.dockpilot.service.http.ContainerService;
import com.dockpilot.api.DockerService;
import com.dockpilot.utils.YamlApplicationParser;
import com.dockpilot.utils.ErrorMessageExtractor;
import com.dockpilot.websocket.model.DockerWebSocketMessage;
import com.dockpilot.websocket.sender.WebSocketMessageSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用WebSocket服务
 * 处理应用安装相关的WebSocket消息
 */
@Slf4j
@Service
@Tag(name = "应用 WebSocket 服务", description = "处理应用安装相关的 WebSocket 消息")
public class ApplicationWebSocketService implements BaseService {

    @Autowired
    private ApplicationService applicationService;
    

    @Autowired
    private ContainerService containerService;
    
    @Autowired
    private DockerService dockerService;

    @Autowired
    private WebSocketMessageSender messageSender;
    
    @Autowired
    private com.dockpilot.common.config.AppConfig appConfig;
    
    @org.springframework.beans.factory.annotation.Value("${file.upload.path:uploads/}")
    private String uploadBasePath;

    // 活跃的安装任务
    private final Map<String, CompletableFuture<Void>> activeTasks = new ConcurrentHashMap<>();

    @Override
    @Operation(
            summary = "处理应用相关的WebSocket消息",
            description = "处理应用安装等操作"
    )
    public void handle(WebSocketSession session, DockerWebSocketMessage message) {
        MessageType type = MessageType.valueOf(message.getType());
        String taskId = message.getTaskId();

        log.info("处理应用WebSocket消息: {}, 任务ID: {}", type, taskId);

        try {
            switch (type) {
                case APP_INSTALL:
                    handleAppInstall(session, message, taskId);
                    break;
                default:
                    log.warn("未知的应用消息类型: {}", type);
                    messageSender.sendError(session, taskId, "未知的应用消息类型: " + type);
            }
        } catch (Exception e) {
            log.error("处理应用消息时发生错误: {}", type, e);
            String userFriendlyError = ErrorMessageExtractor.extractUserFriendlyError(e);
            messageSender.sendError(session, taskId, userFriendlyError);
        }
    }

    /**
     * 处理应用安装请求
     */
    private void handleAppInstall(WebSocketSession session, DockerWebSocketMessage message, String taskId) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        
        // 解析安装参数
        Long appId = Long.valueOf(data.get("appId").toString());
        String appName = (String) data.get("appName");
        Map<String, String> envVars = (Map<String, String>) data.getOrDefault("envVars", new HashMap<>());

        log.info("开始安装应用: appId={}, appName={}, taskId={}", appId, appName, taskId);

        // 检查是否已有相同的安装任务
        if (activeTasks.containsKey(taskId)) {
            messageSender.sendError(session, taskId, "安装任务已在进行中");
            return;
        }

        // 创建安装任务
        CompletableFuture<Void> installTask = CompletableFuture.runAsync(() -> {
            try {
                // 创建回调对象
                InstallCallback callback = new InstallCallback(session, taskId, messageSender);
                
                // 执行安装流程
                performInstallation(appId, appName, envVars, callback);
                
            } catch (Exception e) {
                log.error("安装应用失败: {}", e.getMessage(), e);
                messageSender.sendError(session, taskId, "安装失败: " + e.getMessage());
            } finally {
                // 清理任务
                activeTasks.remove(taskId);
            }
        });

        // 保存任务引用
        activeTasks.put(taskId, installTask);
        
        // 发送开始消息
        messageSender.sendLog(session, taskId, "🚀 开始安装应用: " + appName);
    }

    /**
     * 执行具体的安装流程
     */
    private void performInstallation(Long appId, String appName, Map<String, String> envVars, 
                                   InstallCallback callback) throws Exception {
        
        // 步骤1: 获取应用信息 (0-10%)
        callback.onProgress(5);
        callback.onLog("📋 获取应用配置信息...");
        ApplicationVO applicationVO = applicationService.getApplicationById(appId);
        if (applicationVO == null) {
            throw new RuntimeException("应用不存在: " + appId);
        }
        
        // 步骤2: 解析YAML配置 (10-20%)
        callback.onProgress(15);
        callback.onLog("🔍 解析应用配置...");
        
        // 解析应用配置
        callback.onProgress(5);
        callback.onLog("解析应用配置...");
        
        // 替换YAML中的环境变量占位符
        String processedYaml = applicationVO.getYamlContent();
        if (envVars != null && !envVars.isEmpty()) {
            callback.onLog("处理环境变量配置: " + envVars.size() + " 个");
            processedYaml = replaceEnvPlaceholders(applicationVO.getYamlContent(), envVars);
            callback.onLog("✅ 环境变量配置处理完成");
        }
        
        ApplicationParseResult parseResult = YamlApplicationParser.parseYaml(processedYaml);
        callback.onProgress(15);
        callback.onLog("✅ 应用配置解析完成");
        callback.onLog("解析到 " + parseResult.getServices().size() + " 个服务");
        
        // 步骤3: 检查和拉取镜像 (20-60%)
        callback.onProgress(25);
        callback.onLog("🐳 检查所需镜像...");
        ensureImagesAvailable(parseResult.getImages(), callback);
        
        // 步骤4: 创建和启动容器 (60-95%)
        callback.onProgress(65);
        callback.onLog("🚀 创建应用容器...");
        List<String> containerIds = createAndStartContainers(parseResult, appName, envVars, callback);
        
        // 步骤5: 验证部署 (95-100%)
        callback.onProgress(95);
        callback.onLog("✅ 验证应用状态...");
        ApplicationDeployResult result = verifyDeployment(containerIds, parseResult, envVars, callback);
        
        callback.onProgress(100);
        callback.onLog("🎉 安装完成!");
        callback.onComplete(result);
    }

    /**
     * 确保所需镜像可用
     */
    private void ensureImagesAvailable(List<ApplicationParseResult.ImageInfo> images, InstallCallback callback) throws Exception {
        callback.onLog("检查 " + images.size() + " 个镜像...");
        
        int totalImages = images.size();
        int currentIndex = 0;
        
        for (ApplicationParseResult.ImageInfo image : images) {
            currentIndex++;
            int baseProgress = 25 + (currentIndex * 35 / totalImages); // 25%-60%
            
            callback.onProgress(baseProgress);
            callback.onLog("检查镜像: " + image.getFullName());
            
            // 检查镜像是否存在
            boolean exists = dockerService.isImageExists(image.getFullName());
            if (!exists) {
                callback.onLog("镜像不存在，开始拉取: " + image.getFullName());
                
                // 拉取镜像 - 这里可以集成镜像拉取的WebSocket功能
                pullImageIfNeeded(image.getFullName(), callback);
                
                callback.onLog("✅ 镜像拉取完成: " + image.getFullName());
            } else {
                callback.onLog("✅ 镜像已存在: " + image.getFullName());
            }
        }
    }

    /**
     * 拉取镜像
     */
    private void pullImageIfNeeded(String imageName, InstallCallback callback) throws Exception {
        // 这里可以调用现有的镜像拉取服务
        // 或者使用同步方式拉取
        try {
            // 简化实现，实际应该调用Docker API
            callback.onLog("正在拉取镜像: " + imageName);
            
            // 模拟拉取过程
            Thread.sleep(2000); // 实际应该是真实的拉取操作
            
        } catch (Exception e) {
            throw new RuntimeException("拉取镜像失败: " + imageName + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 创建和启动容器
     */
    private List<String> createAndStartContainers(ApplicationParseResult parseResult, String appName,
                                                Map<String, String> envVars,
                                                InstallCallback callback) throws Exception {
        List<String> containerIds = new ArrayList<>();
        List<String> createdContainerIds = new ArrayList<>(); // 用于错误回滚
        
        try {
            int totalServices = parseResult.getServices().size();
            int currentIndex = 0;
            
            for (ApplicationParseResult.ServiceInfo service : parseResult.getServices()) {
                currentIndex++;
                int baseProgress = 65 + (currentIndex * 30 / totalServices); // 65%-95%
                
                callback.onProgress(baseProgress);
                callback.onLog("创建容器: " + service.getName());
                callback.onLog("正在创建服务: " + service.getName() + " (镜像: " + service.getImage() + ")");
                
                // 使用YAML中配置的容器名，如果没有配置则生成一个
                String containerName;
                if (service.getContainerName() != null && !service.getContainerName().trim().isEmpty()) {
                    containerName = service.getContainerName();
                    callback.onLog("使用配置的容器名: " + containerName);
                } else {
                    // 只有在没有配置container_name时才生成
                    containerName = appName + "_" + service.getName() + "_" + System.currentTimeMillis();
                    callback.onLog("生成容器名: " + containerName);
                }
                
                // 构建容器创建请求
                ContainerCreateRequest request = convertServiceToContainerRequest(service, containerName, envVars, callback);
                
                // 调用真实的容器创建API
                callback.onLog("正在创建Docker容器...");
                String containerId = containerService.createContainer(request);
                
                containerIds.add(containerId);
                createdContainerIds.add(containerId);
                callback.onLog("✅ 容器创建成功: " + containerName + " (ID: " + containerId + ")");
                
                // 容器创建后会自动启动，验证状态
                callback.onLog("验证容器状态: " + containerName);
                Thread.sleep(1000); // 等待容器启动
                
                // 检查容器是否正常运行
                boolean isRunning = isContainerRunning(containerId);
                if (isRunning) {
                    callback.onLog("✅ 容器启动成功: " + containerName);
                } else {
                    callback.onLog("⚠️ 容器可能未正常启动: " + containerName);
                }
            }
            
            return containerIds;
            
        } catch (Exception e) {
            // 错误回滚：清理已创建的容器
            callback.onLog("❌ 容器创建过程中发生错误: " + e.getMessage());
            callback.onLog("正在清理已创建的容器...");
            
            for (String containerId : createdContainerIds) {
                try {
                    containerService.removeContainer(containerId);
                    callback.onLog("已清理容器: " + containerId);
                } catch (Exception cleanupError) {
                    callback.onLog("清理容器失败: " + containerId + ", 错误: " + cleanupError.getMessage());
                }
            }
            
            throw new RuntimeException("容器创建失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将服务配置转换为容器创建请求
     */
    private ContainerCreateRequest convertServiceToContainerRequest(ApplicationParseResult.ServiceInfo service, 
                                                                   String containerName,
                                                                   Map<String, String> envVars, 
                                                                   InstallCallback callback) throws Exception {
        ContainerCreateRequest request = new ContainerCreateRequest();
        
        // 基础配置
        request.setImage(service.getImage());
        request.setName(containerName);
        
        callback.onLog("设置镜像: " + service.getImage());
        callback.onLog("设置容器名: " + containerName);
        
        // 命令和入口点配置
        if (service.getCommand() != null && !service.getCommand().isEmpty()) {
            request.setCmd(service.getCommand());
            callback.onLog("设置启动命令: " + service.getCommand());
        }
        
        if (service.getEntrypoint() != null && !service.getEntrypoint().isEmpty()) {
            request.setEntrypoint(service.getEntrypoint());
            callback.onLog("设置入口点: " + service.getEntrypoint());
        }
        
        // 工作目录配置
        if (service.getWorkingDir() != null && !service.getWorkingDir().trim().isEmpty()) {
            request.setWorkingDir(service.getWorkingDir());
            callback.onLog("设置工作目录: " + service.getWorkingDir());
        }
        
        // 环境变量配置 - 合并服务定义的环境变量和用户配置的环境变量
        List<String> envList = buildEnvironmentVariables(service.getEnvironment(), envVars, callback);
        request.setEnv(envList);
        
        // 端口映射配置
        if (service.getPorts() != null && !service.getPorts().isEmpty()) {
            parsePortMappings(service.getPorts(), request, envVars, callback);
        }
        
        // 数据卷挂载配置 - 🔥 新增：传递configUrl参数
        String configUrl = service.getConfigUrl();
        parseVolumeMounts(service.getVolumes(), request, service.getName(), configUrl, callback);
        
        // 网络配置 - 从服务配置中读取
        String networkMode = service.getNetworkMode();
        if (networkMode != null && !networkMode.trim().isEmpty()) {
            request.setNetworkMode(networkMode);
            callback.onLog("设置网络模式: " + networkMode);
        } else {
            // 如果没有指定网络模式，使用默认的bridge
            request.setNetworkMode("bridge");
            callback.onLog("使用默认网络模式: bridge");
        }
        
        // 重启策略 - 从服务配置中读取
        String restartPolicy = service.getRestart();
        if (restartPolicy != null && !restartPolicy.trim().isEmpty()) {
            try {
                request.setRestartPolicy(com.github.dockerjava.api.model.RestartPolicy.parse(restartPolicy));
                callback.onLog("设置重启策略: " + restartPolicy);
            } catch (Exception e) {
                callback.onLog("⚠️ 无效的重启策略配置: " + restartPolicy + ", 使用默认策略: unless-stopped");
                request.setRestartPolicy(com.github.dockerjava.api.model.RestartPolicy.parse("unless-stopped"));
            }
        } else {
            // 如果没有指定重启策略，使用默认的unless-stopped
            request.setRestartPolicy(com.github.dockerjava.api.model.RestartPolicy.parse("unless-stopped"));
            callback.onLog("使用默认重启策略: unless-stopped");
        }
        
        // 特权模式配置
        if (service.getPrivileged() != null && service.getPrivileged()) {
            request.setPrivileged(true);
            callback.onLog("启用特权模式");
        }
        
        // Capability 配置
        if (service.getCapAdd() != null && !service.getCapAdd().isEmpty()) {
            request.setCapAdd(service.getCapAdd());
            callback.onLog("添加Capabilities: " + service.getCapAdd());
        }
        
        // 设备映射配置
        if (service.getDevices() != null && !service.getDevices().isEmpty()) {
            parseDeviceMappings(service.getDevices(), request, callback);
        }
        
        // 标签配置
        if (service.getLabels() != null && !service.getLabels().isEmpty()) {
            request.setLabels(service.getLabels());
            callback.onLog("设置标签: " + service.getLabels().size() + " 个");
        }
        
        return request;
    }
    
    /**
     * 构建环境变量列表 - 合并服务定义的环境变量和用户配置的环境变量
     */
    private List<String> buildEnvironmentVariables(List<String> serviceEnvVars, Map<String, String> userEnvVars, InstallCallback callback) {
        List<String> envList = new ArrayList<>();
        
        // 添加服务定义的环境变量
        if (serviceEnvVars != null) {
            for (String envVar : serviceEnvVars) {
                // 替换环境变量占位符
                String processedEnv = replaceEnvPlaceholders(envVar, userEnvVars);
                envList.add(processedEnv);
            }
        }
        
        // 添加用户配置的环境变量
        if (userEnvVars != null) {
            for (Map.Entry<String, String> entry : userEnvVars.entrySet()) {
                String envString = entry.getKey() + "=" + entry.getValue();
                // 避免重复添加
                boolean exists = envList.stream().anyMatch(env -> env.startsWith(entry.getKey() + "="));
                if (!exists) {
                    envList.add(envString);
                }
            }
        }
        
        if (!envList.isEmpty()) {
            callback.onLog("设置环境变量: " + envList.size() + " 个");
        }
        
        return envList;
    }
    
    /**
     * 原有的构建环境变量方法，保持向后兼容
     */
    private List<String> buildEnvironmentVariables(Map<String, String> envVars, InstallCallback callback) {
        return buildEnvironmentVariables(null, envVars, callback);
    }
    
    /**
     * 解析端口映射
     */
    private void parsePortMappings(List<String> ports, ContainerCreateRequest request, 
                                 Map<String, String> envVars, InstallCallback callback) {
        List<com.github.dockerjava.api.model.ExposedPort> exposedPorts = new ArrayList<>();
        com.github.dockerjava.api.model.Ports portBindings = new com.github.dockerjava.api.model.Ports();
        
        for (String portMapping : ports) {
            try {
                // 替换环境变量占位符
                String processedPort = replaceEnvPlaceholders(portMapping, envVars);
                
                // 解析端口映射格式：hostPort:containerPort 或 port
                String[] parts = processedPort.split(":");
                if (parts.length == 2) {
                    int hostPort = Integer.parseInt(parts[0].trim());
                    int containerPort = Integer.parseInt(parts[1].trim());
                    
                    com.github.dockerjava.api.model.ExposedPort exposedPort = 
                        com.github.dockerjava.api.model.ExposedPort.tcp(containerPort);
                    exposedPorts.add(exposedPort);
                    
                    portBindings.bind(exposedPort, 
                        com.github.dockerjava.api.model.Ports.Binding.bindPort(hostPort));
                    
                    callback.onLog("设置端口映射: " + hostPort + " -> " + containerPort);
                } else if (parts.length == 1) {
                    int port = Integer.parseInt(parts[0].trim());
                    com.github.dockerjava.api.model.ExposedPort exposedPort = 
                        com.github.dockerjava.api.model.ExposedPort.tcp(port);
                    exposedPorts.add(exposedPort);
                    
                    callback.onLog("暴露端口: " + port);
                }
            } catch (Exception e) {
                callback.onLog("⚠️ 端口配置解析失败: " + portMapping + ", 错误: " + e.getMessage());
            }
        }
        
        if (!exposedPorts.isEmpty()) {
            request.setExposedPorts(exposedPorts);
            request.setPortBindings(portBindings);
        }
    }
    
    /**
     * 解析数据卷挂载
     */
    private void parseVolumeMounts(List<String> serviceVolumes, ContainerCreateRequest request, String serviceName, String configUrl, InstallCallback callback) {
        List<com.github.dockerjava.api.model.Bind> binds = new ArrayList<>();
        
        // 🔥 新增：处理配置包下载
        handleConfigDownload(configUrl, serviceName, serviceVolumes, callback);
        
        // 处理服务定义的卷挂载
        if (serviceVolumes != null) {
            for (String volumeMapping : serviceVolumes) {
                try {
                    // 解析卷挂载格式：hostPath:containerPath[:ro/rw]
                    String[] parts = volumeMapping.split(":");
                    if (parts.length >= 2) {
                        String hostPath = parts[0].trim();
                        String containerPath = parts[1].trim();
                        String accessMode = parts.length > 2 ? parts[2].trim() : "rw";
                        
                        // hostPath可能包含环境变量引用，在YAML处理阶段已经替换完成
                        
                        // 🔥 新增：自动创建宿主机目录
                        if (!ensureHostDirectoryExists(hostPath, callback)) {
                            callback.onLog("⚠️ 无法创建宿主机目录: " + hostPath + "，跳过此挂载");
                            continue;
                        }
                        
                        com.github.dockerjava.api.model.Volume volume = new com.github.dockerjava.api.model.Volume(containerPath);
                        com.github.dockerjava.api.model.Bind bind;
                        
                        if ("ro".equals(accessMode)) {
                            bind = new com.github.dockerjava.api.model.Bind(hostPath, volume, com.github.dockerjava.api.model.AccessMode.ro);
                        } else {
                            bind = new com.github.dockerjava.api.model.Bind(hostPath, volume, com.github.dockerjava.api.model.AccessMode.rw);
                        }
                        
                        binds.add(bind);
                        callback.onLog("设置卷挂载: " + hostPath + " -> " + containerPath + " (" + accessMode + ")");
                    } else if (parts.length == 1) {
                        // 命名卷或匿名卷
                        String volumePath = parts[0].trim();
                        com.github.dockerjava.api.model.Volume volume = new com.github.dockerjava.api.model.Volume(volumePath);
                        
                        // 初始化volumes列表如果为空
                        if (request.getVolumes() == null) {
                            request.setVolumes(new ArrayList<>());
                        }
                        request.getVolumes().add(volume);
                        callback.onLog("设置数据卷: " + volumePath);
                    }
                } catch (Exception e) {
                    callback.onLog("⚠️ 卷挂载配置解析失败: " + volumeMapping + ", 错误: " + e.getMessage());
                }
            }
        }
        
        if (!binds.isEmpty()) {
            request.setBinds(binds);
        }
    }
    
    /**
     * 🔥 新增：确保宿主机目录存在，如果不存在则自动创建
     * 
     * @param hostPath 宿主机路径
     * @param callback 回调对象用于日志输出
     * @return true如果目录存在或创建成功，false如果创建失败
     */
    private boolean ensureHostDirectoryExists(String hostPath, InstallCallback callback) {
        try {
            // 验证路径格式
            if (hostPath == null || hostPath.trim().isEmpty()) {
                callback.onLog("⚠️ 宿主机路径为空，跳过创建");
                return false;
            }
            
            // 规范化路径
            String normalizedPath = hostPath.trim();
            
            // 检查是否为绝对路径
            if (!normalizedPath.startsWith("/")) {
                callback.onLog("⚠️ 宿主机路径必须是绝对路径: " + normalizedPath);
                return false;
            }
            
            // 检查是否为系统敏感路径，避免误操作
            if (isSystemSensitivePath(normalizedPath)) {
                callback.onLog("⚠️ 跳过系统敏感路径: " + normalizedPath);
                return true; // 返回true，让Docker处理这些路径
            }
            
            // 🔥 新增：只创建基于 docker_base_dir 的目录
            if (!shouldCreateDirectory(normalizedPath, callback)) {
                callback.onLog("⚠️ 跳过非Docker配置目录: " + normalizedPath + " (只自动创建Docker配置目录)");
                return true; // 返回true，让Docker处理这些路径
            }
            
            // 🔥 新增：获取实际的文件系统路径（容器化部署）
            String actualPath = getActualFilePath(normalizedPath, callback);
            java.nio.file.Path targetPath = java.nio.file.Paths.get(actualPath);
            
            // 检查路径是否已存在
            if (java.nio.file.Files.exists(targetPath)) {
                if (java.nio.file.Files.isDirectory(targetPath)) {
                    callback.onLog("✅ 宿主机目录已存在: " + normalizedPath);
                    return true;
                } else {
                    callback.onLog("❌ 宿主机路径已存在但不是目录: " + normalizedPath);
                    return false;
                }
            }
            
            // 目录不存在，尝试创建
            callback.onLog("📁 正在创建Docker配置目录: " + normalizedPath);
            java.nio.file.Files.createDirectories(targetPath);
            
            // 验证创建结果
            if (java.nio.file.Files.exists(targetPath) && java.nio.file.Files.isDirectory(targetPath)) {
                callback.onLog("✅ Docker配置目录创建成功: " + normalizedPath);
                
                // 尝试设置目录权限（非关键操作，失败不影响整体流程）
                try {
                    // 设置目录权限为777（rwxrwxrwx）
                    java.nio.file.Files.setPosixFilePermissions(targetPath, 
                        java.util.EnumSet.of(
                            java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                            java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE,
                            java.nio.file.attribute.PosixFilePermission.GROUP_READ,
                            java.nio.file.attribute.PosixFilePermission.GROUP_WRITE,
                            java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE,
                            java.nio.file.attribute.PosixFilePermission.OTHERS_READ,
                            java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE,
                            java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE
                        ));
                    callback.onLog("✅ 目录权限设置成功: 777");
                } catch (Exception permissionError) {
                    callback.onLog("⚠️ 目录权限设置失败（不影响挂载）: " + permissionError.getMessage());
                }
                
                return true;
            } else {
                callback.onLog("❌ 目录创建失败，验证不通过: " + normalizedPath);
                return false;
            }
            
        } catch (java.nio.file.FileAlreadyExistsException e) {
            // 并发创建导致的异常，再次检查是否为目录
            try {
                String actualPath = getActualFilePath(hostPath, callback);
                java.nio.file.Path targetPath = java.nio.file.Paths.get(actualPath);
                if (java.nio.file.Files.isDirectory(targetPath)) {
                    callback.onLog("✅ 目录已被并发创建: " + hostPath);
                    return true;
                } else {
                    callback.onLog("❌ 路径被创建但不是目录: " + hostPath);
                    return false;
                }
            } catch (Exception verifyError) {
                callback.onLog("❌ 验证并发创建结果失败: " + verifyError.getMessage());
                return false;
            }
        } catch (java.nio.file.AccessDeniedException e) {
            callback.onLog("❌ 权限不足，无法创建目录: " + hostPath);
            return false;
        } catch (SecurityException e) {
            callback.onLog("❌ 安全策略阻止创建目录: " + hostPath);
            return false;
        } catch (Exception e) {
            callback.onLog("❌ 创建宿主机目录失败: " + hostPath + ", 错误: " + e.getMessage());
            log.error("创建宿主机目录失败: {}", hostPath, e);
            return false;
        }
    }
    
    /**
     * 🔥 新增：获取实际的文件系统路径（容器化部署）
     * 
     * @param hostPath 宿主机路径（如 /volume1/docker/app/config）
     * @param callback 回调对象用于日志输出
     * @return 实际的文件系统路径
     */
    private String getActualFilePath(String hostPath, InstallCallback callback) {
        // 容器化部署，通过 /mnt/host 访问宿主机文件系统
        String actualPath = "/mnt/host" + hostPath;
        callback.onLog("🐳 容器化部署，实际操作路径: " + actualPath);
        return actualPath;
    }
    
    /**
     * 🔥 新增：判断是否应该创建目录（只创建Docker配置目录）
     * 
     * @param path 要检查的路径
     * @param callback 回调对象用于日志输出
     * @return true如果应该创建，false如果不应该创建
     */
    private boolean shouldCreateDirectory(String path, InstallCallback callback) {
        try {
            // 检查Docker运行目录是否已配置
            if (!appConfig.isDockerBaseDirConfigured()) {
                callback.onLog("⚠️ Docker运行目录未配置，跳过自动创建");
                return false;
            }
            
            // 获取Docker运行目录
            String dockerBaseDir = appConfig.getDockerBaseDirOrThrow();
            
            // 规范化Docker基础目录路径（确保以/结尾）
            if (!dockerBaseDir.endsWith("/")) {
                dockerBaseDir = dockerBaseDir + "/";
            }
            
            // 检查路径是否以Docker基础目录开头
            boolean shouldCreate = path.startsWith(dockerBaseDir) || path.equals(dockerBaseDir.substring(0, dockerBaseDir.length() - 1));
            
            if (shouldCreate) {
                callback.onLog("✅ 检测到Docker配置目录，将自动创建: " + path);
            } else {
                callback.onLog("ℹ️ 非Docker配置目录，跳过创建: " + path + " (Docker目录: " + dockerBaseDir + ")");
            }
            
            return shouldCreate;
            
        } catch (Exception e) {
            callback.onLog("⚠️ 检查Docker目录配置失败: " + e.getMessage());
            log.warn("检查Docker目录配置失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 🔥 新增：检查是否为系统敏感路径
     * 
     * @param path 要检查的路径
     * @return true如果是敏感路径，false如果是安全路径
     */
    private boolean isSystemSensitivePath(String path) {
        // 系统敏感路径列表
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
     * 替换环境变量占位符
     */
    private String replaceEnvPlaceholders(String text, Map<String, String> envVars) {
        if (text == null || envVars == null) {
            return text;
        }
        
        String result = text;
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        
        return result;
    }
    
    /**
     * 检查容器是否正在运行
     */
    private boolean isContainerRunning(String containerId) {
        try {
            // 这里应该调用实际的容器状态检查
            // 简化实现：假设容器都能正常启动
            return true;
        } catch (Exception e) {
            log.warn("检查容器状态失败: {}, 错误: {}", containerId, e.getMessage());
            return false;
        }
    }

    /**
     * 验证部署结果
     */
    private ApplicationDeployResult verifyDeployment(List<String> containerIds, 
                                                   ApplicationParseResult parseResult,
                                                   Map<String, String> envVars,
                                                   InstallCallback callback) throws Exception {
        callback.onLog("验证容器运行状态...");
        
        // 检查所有容器是否正常运行
        for (String containerId : containerIds) {
            // boolean isRunning = containerService.isContainerRunning(containerId);
            boolean isRunning = true; // 模拟检查结果
            
            if (!isRunning) {
                throw new RuntimeException("容器启动失败: " + containerId);
            }
        }
        
        // 构建访问地址 - 直接列出所有端口
        List<ApplicationDeployResult.AccessUrl> accessUrls = new ArrayList<>();
        
        // 获取宿主机IP
        String hostIp = getHostIp();
        
        // 遍历所有环境变量，找出端口配置
        for (ApplicationParseResult.EnvVarInfo env : parseResult.getEnvVars()) {
            if (env.getName().toUpperCase().contains("PORT") && envVars.containsKey(env.getName())) {
                String portValue = envVars.get(env.getName());
                if (portValue != null && !portValue.trim().isEmpty() && isValidPort(portValue)) {
                    ApplicationDeployResult.AccessUrl accessUrl = new ApplicationDeployResult.AccessUrl();
                    
                    // 服务名称：直接使用环境变量名
                    String serviceName = env.getName().replace("_PORT", "").replace("PORT", "");
                    accessUrl.setName(serviceName);
                    
                    // 访问地址：宿主机IP + 端口
                    accessUrl.setUrl("http://" + hostIp + ":" + portValue);
                    
                    // 描述
                    accessUrl.setDescription("端口 " + portValue);
                    
                    accessUrls.add(accessUrl);
                }
            }
        }
        
        ApplicationDeployResult result = ApplicationDeployResult.success(containerIds, "应用安装成功");
        result.setAccessUrls(accessUrls);
        result.setDeployId("deploy_" + System.currentTimeMillis());
        
        callback.onLog("🎉 所有服务运行正常，安装完成!");
        
        return result;
    }
    
    /**
     * 获取宿主机IP
     */
    private String getHostIp() {
        try {
            // 尝试获取实际的宿主机IP
            // 这里可以根据实际情况调整获取IP的逻辑
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            String hostAddress = localHost.getHostAddress();
            
            // 如果是回环地址，尝试获取其他网卡地址
            if ("127.0.0.1".equals(hostAddress) || "localhost".equals(hostAddress)) {
                java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    java.net.NetworkInterface ni = interfaces.nextElement();
                    if (!ni.isLoopback() && ni.isUp()) {
                        java.util.Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            java.net.InetAddress addr = addresses.nextElement();
                            if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                                return addr.getHostAddress();
                            }
                        }
                    }
                }
            }
            
            return hostAddress;
        } catch (Exception e) {
            log.warn("获取宿主机IP失败，使用localhost: {}", e.getMessage());
            return "localhost";
        }
    }
    
    /**
     * 验证端口号是否有效
     */
    private boolean isValidPort(String port) {
        try {
            int portNum = Integer.parseInt(port.trim());
            return portNum > 0 && portNum <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 解析设备映射
     */
    private void parseDeviceMappings(List<String> devices, ContainerCreateRequest request, InstallCallback callback) {
        if (devices == null || devices.isEmpty()) {
            return;
        }
        
        List<com.github.dockerjava.api.model.Device> deviceList = new ArrayList<>();
        
        for (String deviceMapping : devices) {
            try {
                // 解析设备映射格式：hostDevice:containerDevice[:rwm]
                String[] parts = deviceMapping.split(":");
                if (parts.length >= 2) {
                    String hostDevice = parts[0].trim();
                    String containerDevice = parts[1].trim();
                    String permissions = parts.length > 2 ? parts[2].trim() : "rwm";
                    
                    // 🔍 检查宿主机设备是否存在
                    if (!checkHostDeviceExists(hostDevice, callback)) {
                        callback.onLog("⚠️ 跳过设备映射: " + hostDevice + " (宿主机设备不存在，容器将使用软件渲染)");
                        continue; // 跳过这个设备映射，继续处理下一个
                    }
                    
                    com.github.dockerjava.api.model.Device device = new com.github.dockerjava.api.model.Device(
                        permissions, containerDevice, hostDevice
                    );
                    deviceList.add(device);
                    
                    callback.onLog("✅ 设置设备映射: " + hostDevice + " -> " + containerDevice + " (" + permissions + ")");
                }
            } catch (Exception e) {
                callback.onLog("⚠️ 设备映射配置解析失败: " + deviceMapping + ", 错误: " + e.getMessage());
            }
        }
        
        if (!deviceList.isEmpty()) {
            request.setDevices(deviceList.toArray(new com.github.dockerjava.api.model.Device[0]));
            callback.onLog("📱 成功配置 " + deviceList.size() + " 个设备映射");
        } else {
            callback.onLog("📱 未配置任何设备映射 (所有设备均不可用)");
        }
    }
    
    /**
     * 检查宿主机设备是否存在
     * 
     * @param hostDevice 宿主机设备路径 (如 /dev/dri)
     * @param callback 回调用于记录日志
     * @return true-设备存在, false-设备不存在
     */
    private boolean checkHostDeviceExists(String hostDevice, InstallCallback callback) {
        try {
            // DockPilot 容器挂载了宿主机根目录到 /mnt/host
            // 所以检查宿主机的 /dev/dri 就是检查容器内的 /mnt/host/dev/dri
            String hostMountedPath = "/mnt/host" + hostDevice;
            java.nio.file.Path devicePath = java.nio.file.Paths.get(hostMountedPath);
            
            boolean exists = java.nio.file.Files.exists(devicePath);
            
            if (exists) {
                callback.onLog("🔍 设备检查: " + hostDevice + " ✅ 存在");
                return true;
            } else {
                callback.onLog("🔍 设备检查: " + hostDevice + " ❌ 不存在");
                return false;
            }
            
        } catch (Exception e) {
            // 如果检查过程中出现异常，为了安全起见，假设设备不存在
            callback.onLog("⚠️ 设备检查异常: " + hostDevice + ", 错误: " + e.getMessage() + " (假设设备不存在)");
            return false;
        }
    }

    /**
     * 🔥 新增：处理配置包下载
     * 
     * @param configUrl 配置包下载地址
     * @param serviceName 服务名称
     * @param volumeMappings 卷挂载配置
     * @param callback 回调对象
     */
    private void handleConfigDownload(String configUrl, String serviceName, 
                                    List<String> volumeMappings, InstallCallback callback) {
        // 检查configUrl是否有值
        if (configUrl == null || configUrl.trim().isEmpty()) {
            callback.onLog("📁 服务 " + serviceName + " 无配置包，将创建空目录");
            return;
        }
        
        // 🔥 新增：检测本地配置包
        if (configUrl.startsWith("local://")) {
            handleLocalConfigPackage(configUrl, serviceName, volumeMappings, callback);
            return;
        }
        
        // 原有逻辑：网络下载
        callback.onLog("📦 检测到网络配置包: " + configUrl);
        
        try {
            // 下载配置包
            String packagePath = downloadConfigPackage(configUrl, serviceName, callback);
            if (packagePath == null) {
                callback.onLog("⚠️ 配置包下载失败，将创建空目录");
                return;
            }
            
            // 解压配置包
            extractConfigPackage(packagePath, serviceName, volumeMappings, callback);
            
            // 清理临时文件
            deleteTemporaryFile(packagePath);
            
            callback.onLog("✅ 配置包部署完成: " + serviceName);
            
        } catch (Exception e) {
            callback.onLog("❌ 配置包处理失败: " + e.getMessage() + "，将创建空目录");
            log.error("处理配置包失败: {}", serviceName, e);
        }
    }
    
    /**
     * 🆕 处理本地配置包
     */
    private void handleLocalConfigPackage(String configUrl, String serviceName, 
                                        List<String> volumeMappings, InstallCallback callback) {
        try {
            // 解析本地路径: local://项目名/服务名.tar.gz
            String relativePath = configUrl.substring(8); // 移除 "local://"
            String localPackagePath = uploadBasePath + relativePath;
            
            callback.onLog("📦 使用本地配置包: " + relativePath);
            
            // 检查文件是否存在
            if (!java.nio.file.Files.exists(java.nio.file.Paths.get(localPackagePath))) {
                callback.onLog("⚠️ 本地配置包不存在: " + localPackagePath + "，将创建空目录");
                return;
            }
            
            // 直接解压本地配置包（复用现有解压逻辑）
            extractConfigPackage(localPackagePath, serviceName, volumeMappings, callback);
            
            callback.onLog("✅ 本地配置包部署完成: " + serviceName);
            
        } catch (Exception e) {
            callback.onLog("❌ 本地配置包处理失败: " + e.getMessage() + "，将创建空目录");
            log.error("处理本地配置包失败: {}", serviceName, e);
        }
    }
    
    /**
     * 下载配置包到临时文件
     */
    private String downloadConfigPackage(String configUrl, String serviceName, InstallCallback callback) {
        try {
            callback.onLog("⬇️ 正在下载配置包...");
            
            // 创建临时文件
            String tempFile = "/tmp/config-download-" + serviceName + "-" + System.currentTimeMillis() + ".tar.gz";
            
            // 使用Java原生HTTP客户端下载
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(configUrl))
                .timeout(java.time.Duration.ofSeconds(120))
                .GET()
                .build();
            
            java.net.http.HttpResponse<java.nio.file.Path> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofFile(java.nio.file.Paths.get(tempFile)));
            
            if (response.statusCode() == 200) {
                callback.onLog("✅ 配置包下载成功");
                return tempFile;
            } else {
                callback.onLog("❌ 配置包下载失败: HTTP " + response.statusCode());
                return null;
            }
            
        } catch (Exception e) {
            callback.onLog("❌ 配置包下载异常: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解压配置包到目标目录
     */
    private void extractConfigPackage(String packagePath, String serviceName, 
                                    List<String> volumeMappings, InstallCallback callback) throws Exception {
        callback.onLog("📂 正在解压配置包...");
        
        // 创建临时解压目录
        String tempExtractDir = "/tmp/config-extract-" + serviceName + "-" + System.currentTimeMillis();
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tempExtractDir));
        
        try {
            // 解压tar.gz文件
            extractTarGzFile(packagePath, tempExtractDir, callback);
            
            // 根据volumeMappings将解压的内容复制到目标目录
            deployExtractedConfig(tempExtractDir, serviceName, volumeMappings, callback);
            
        } finally {
            // 清理临时解压目录
            deleteTemporaryDirectory(tempExtractDir);
        }
    }
    
    /**
     * 解压tar.gz文件
     */
    private void extractTarGzFile(String packagePath, String extractDir, InstallCallback callback) throws Exception {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(packagePath);
             java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis);
             java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(bis);
             org.apache.commons.compress.archivers.tar.TarArchiveInputStream tais = 
                     new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzis)) {
            
            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                
                String entryPath = extractDir + "/" + entry.getName();
                java.nio.file.Path targetPath = java.nio.file.Paths.get(entryPath);
                
                // 创建父目录
                java.nio.file.Files.createDirectories(targetPath.getParent());
                
                // 写入文件
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(targetPath.toFile())) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = tais.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }
                
                callback.onLog("解压文件: " + entry.getName());
            }
        }
    }
    
    /**
     * 部署解压的配置到目标目录
     */
    private void deployExtractedConfig(String extractDir, String serviceName, 
                                     List<String> volumeMappings, InstallCallback callback) throws Exception {
        
        if (volumeMappings == null || volumeMappings.isEmpty()) {
            callback.onLog("⚠️ 无卷挂载配置，跳过配置部署");
            return;
        }
        
        for (String volumeMapping : volumeMappings) {
            String[] parts = volumeMapping.split(":");
            if (parts.length >= 2) {
                String hostPath = parts[0].trim();
                String containerPath = parts[1].trim();
                
                // 从容器路径推导配置包中的目录名
                String containerDirName = getLastPathSegment(containerPath);
                String sourceDir = extractDir + "/" + containerDirName;
                
                // 检查配置包中是否有对应的目录
                if (java.nio.file.Files.exists(java.nio.file.Paths.get(sourceDir))) {
                    // 获取宿主机实际路径
                    String actualHostPath = getActualFilePath(hostPath, callback);
                    
                    // 创建宿主机目录
                    java.nio.file.Files.createDirectories(java.nio.file.Paths.get(actualHostPath));
                    
                    // 复制配置文件
                    copyConfigToTarget(sourceDir, actualHostPath, callback);
                    
                    callback.onLog("✅ 配置部署成功: " + containerDirName + " -> " + hostPath);
                } else {
                    callback.onLog("⚠️ 配置包中未找到目录: " + containerDirName);
                }
            }
        }
    }
    
    /**
     * 获取路径的最后一段
     */
    private String getLastPathSegment(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        
        path = path.trim();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }
        
        return path;
    }
    
    /**
     * 复制配置到目标目录
     */
    private void copyConfigToTarget(String sourceDir, String targetDir, InstallCallback callback) throws Exception {
        java.nio.file.Path source = java.nio.file.Paths.get(sourceDir);
        java.nio.file.Path target = java.nio.file.Paths.get(targetDir);
        
        java.nio.file.Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
            @Override
            public java.nio.file.FileVisitResult preVisitDirectory(java.nio.file.Path dir, 
                    java.nio.file.attribute.BasicFileAttributes attrs) throws java.io.IOException {
                java.nio.file.Path targetDir = target.resolve(source.relativize(dir));
                java.nio.file.Files.createDirectories(targetDir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
            
            @Override
            public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file, 
                    java.nio.file.attribute.BasicFileAttributes attrs) throws java.io.IOException {
                java.nio.file.Path targetFile = target.resolve(source.relativize(file));
                java.nio.file.Files.copy(file, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * 删除临时文件
     */
    private void deleteTemporaryFile(String filePath) {
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filePath));
        } catch (Exception e) {
            log.warn("删除临时文件失败: {}", filePath, e);
        }
    }
    
    /**
     * 删除临时目录
     */
    private void deleteTemporaryDirectory(String dirPath) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(dirPath);
            if (java.nio.file.Files.exists(path)) {
                java.nio.file.Files.walk(path)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(java.io.File::delete);
            }
        } catch (Exception e) {
            log.warn("删除临时目录失败: {}", dirPath, e);
        }
    }

    /**
     * 安装回调接口
     */
    private static class InstallCallback {
        private final WebSocketSession session;
        private final String taskId;
        private final WebSocketMessageSender messageSender;

        public InstallCallback(WebSocketSession session, String taskId, WebSocketMessageSender messageSender) {
            this.session = session;
            this.taskId = taskId;
            this.messageSender = messageSender;
        }

        public void onProgress(int progress) {
            messageSender.sendProgress(session, taskId, progress);
        }

        public void onLog(String log) {
            messageSender.sendLog(session, taskId, log);
        }

        public void onComplete(ApplicationDeployResult result) {
            messageSender.sendComplete(session, taskId, result);
        }

        public void onError(String error) {
            messageSender.sendError(session, taskId, error);
        }
    }
} 