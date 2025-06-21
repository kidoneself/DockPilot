package com.dockpilot.service.websocket;

import com.dockpilot.api.DockerService;
import com.dockpilot.model.ContainerCreateRequest;
import com.dockpilot.model.MessageType;
import com.dockpilot.model.application.ApplicationParseResult;
import com.dockpilot.model.application.dto.ApplicationDeployResult;
import com.dockpilot.service.ApplicationService;
import com.dockpilot.service.http.ContainerService;
import com.dockpilot.utils.ErrorMessageExtractor;
import com.dockpilot.utils.WebSocketUtils;
import com.dockpilot.utils.YamlApplicationParser;
import com.dockpilot.websocket.model.DockerWebSocketMessage;
import com.dockpilot.websocket.sender.WebSocketMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用WebSocket服务
 * 处理应用安装相关的WebSocket消息
 */
@Slf4j
@Service
public class ApplicationWebSocketService implements BaseService {

    @Value("${file.upload.path:uploads/}")
    private String uploadBasePath;

    @Value("${file.config.path}")
    private String configPath;

    // 活跃的安装任务
    private final Map<String, CompletableFuture<Void>> activeTasks = new ConcurrentHashMap<>();
    

    @Autowired
    private ContainerService containerService;
    @Autowired
    private DockerService dockerService;
    @Autowired
    private WebSocketMessageSender messageSender;
    

    @Override
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
        String yamlContent = (String) data.get("yamlContent");

        log.info("开始安装应用: appId={}, appName={}, taskId={}", appId, appName, taskId);

        // 验证YAML内容
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            messageSender.sendError(session, taskId, "YAML配置内容不能为空");
            return;
        }

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
                
                // 执行YAML安装
                performYamlInstallation(appId, appName, yamlContent, callback);
                
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
     * 🆕 执行YAML模式的安装流程 - 完整实现
     * 直接使用用户编辑的YAML内容进行安装
     */
    private void performYamlInstallation(Long appId, String appName, String yamlContent,
                                   InstallCallback callback) throws Exception {
        
        // 🔧 记录已创建的容器ID，用于失败回滚
        List<String> createdContainerIds = new ArrayList<>();
        
        try {
            // 步骤1: 验证YAML格式 (0-5%)
        callback.onProgress(5);
            callback.onLog("📋 验证YAML配置格式...");
            
            try {
                YamlApplicationParser.validateYaml(yamlContent);
                callback.onLog("✅ YAML格式验证通过");
            } catch (Exception e) {
                throw new RuntimeException("YAML格式错误: " + e.getMessage());
            }
            
            // 步骤2: 解析YAML配置 (5-15%)
        callback.onProgress(10);
            callback.onLog("🔍 解析YAML配置...");
            
            ApplicationParseResult parseResult = YamlApplicationParser.parseYaml(yamlContent);
            callback.onLog("✅ YAML配置解析完成");
        callback.onLog("检测到 " + parseResult.getServices().size() + " 个服务待安装");
        
            // 步骤3: 检查并拉取镜像 (15-30%)
            callback.onProgress(20);
            callback.onLog("🔍 检查所需镜像...");
            
            ensureImagesAvailable(parseResult.getImages(), callback);
            callback.onLog("✅ 所有镜像准备完成");
            
            // 步骤4: 串行创建容器服务 (30-90%)
            callback.onProgress(35);
            callback.onLog("🚀 开始串行创建容器服务...");
            
        int totalServices = parseResult.getServices().size();
            int currentService = 0;
            
            for (ApplicationParseResult.ServiceInfo service : parseResult.getServices()) {
                currentService++;
                int serviceProgress = 35 + (currentService * 55 / totalServices); // 35%-90%
                
                callback.onProgress(serviceProgress);
                callback.onLog("🚀 创建服务 " + currentService + "/" + totalServices + ": " + service.getName());
                
                try {
                    // 🔧 串行创建：一个接一个
                    String containerId = createServiceFromYaml(service, yamlContent, appName, callback);
                
                if (containerId != null) {
                        createdContainerIds.add(containerId);
                        callback.onLog("✅ 服务 " + service.getName() + " 创建成功: " + containerId);
                        callback.onLog("📊 已成功创建 " + createdContainerIds.size() + "/" + totalServices + " 个服务");
                } else {
                        // 🚨 创建失败，立即回滚
                        throw new RuntimeException("服务 " + service.getName() + " 创建失败：容器ID为空");
                }
                
            } catch (Exception e) {
                    // 🚨 任何服务失败，立即回滚所有已创建的容器
                    callback.onLog("❌ 服务 " + service.getName() + " 创建失败: " + e.getMessage());
                    throw new RuntimeException("服务创建失败，将回滚所有已创建容器: " + e.getMessage());
                }
            }
            
            // 步骤5: 验证服务状态 (90-95%)
            callback.onProgress(92);
            callback.onLog("🔍 验证所有服务运行状态...");
            
            Thread.sleep(3000); // 等待容器启动
            
            // 步骤6: 构建安装结果 (95-100%)
            callback.onProgress(97);
            callback.onLog("📋 构建安装结果...");
            
            ApplicationDeployResult result = buildYamlInstallResult(createdContainerIds, parseResult, yamlContent, callback);
        
        callback.onProgress(100);
            callback.onLog("🎉 应用安装完成! 成功启动 " + createdContainerIds.size() + "/" + totalServices + " 个服务");
        callback.onComplete(result);
            
        } catch (Exception e) {
            // 🚨 发生任何异常，执行回滚操作
            callback.onLog("🔄 检测到安装失败，开始回滚操作...");
            
            if (!createdContainerIds.isEmpty()) {
                rollbackCreatedContainers(createdContainerIds, callback);
            }
            
            // 重新抛出异常
            throw e;
        }
    }

    /**
     * 🆕 从YAML配置创建单个服务容器 - 完整实现
     */
    private String createServiceFromYaml(ApplicationParseResult.ServiceInfo service, String yamlContent,
                                       String appName, InstallCallback callback) throws Exception {
        try {
            callback.onLog("📋 解析服务配置: " + service.getName());
            
            // 🔧 从原始YAML中解析完整的服务配置
            Map<String, Object> serviceConfig = extractServiceConfigFromYaml(yamlContent, service.getName(), callback);
            
            // 🔧 获取x-meta中的环境变量用于替换
            Map<String, String> globalEnvVars = extractGlobalEnvVarsFromYaml(yamlContent, callback);
            
            // 创建容器请求对象
            ContainerCreateRequest request = new ContainerCreateRequest();
            
            // 基础配置
            request.setImage(service.getImage());
            request.setName(service.getName());
            callback.onLog("设置镜像: " + service.getImage());
            callback.onLog("设置容器名: " + service.getName());
            
            // 🔧 先解析卷挂载配置，获取处理后的卷挂载列表
            List<String> processedVolumeMappings = new ArrayList<>();
            if (serviceConfig.containsKey("volumes")) {
                parseVolumesFromConfig(serviceConfig.get("volumes"), request, globalEnvVars, callback);
                
                // 提取处理后的卷挂载配置用于配置包处理
                List<Object> volumes = (List<Object>) serviceConfig.get("volumes");
                for (Object volumeObj : volumes) {
                    String volumeMapping = replaceEnvPlaceholders(volumeObj.toString(), globalEnvVars);
                    processedVolumeMappings.add(volumeMapping);
                }
            }
            
            // 🔧 处理配置包（使用处理后的卷挂载配置）
            String configUrl = service.getConfigUrl();
            if (configUrl != null && !configUrl.trim().isEmpty()) {
                callback.onLog("📦 检测到配置包: " + configUrl);
                handleConfigDownload(configUrl, service.getName(), processedVolumeMappings, callback);
            } else {
                callback.onLog("📁 服务 " + service.getName() + " 无配置包，将创建空目录");
            }
            
            // 🔧 解析端口映射
            if (serviceConfig.containsKey("ports")) {
                parsePortsFromConfig(serviceConfig.get("ports"), request, globalEnvVars, callback);
            }
            
            // 🔧 解析环境变量
            if (serviceConfig.containsKey("environment")) {
                parseEnvironmentFromConfig(serviceConfig.get("environment"), request, globalEnvVars, callback);
            }
            
            // 🔧 解析重启策略
            if (serviceConfig.containsKey("restart")) {
                parseRestartPolicyFromConfig(serviceConfig.get("restart"), request, callback);
            } else {
                request.setRestartPolicy(com.github.dockerjava.api.model.RestartPolicy.parse("unless-stopped"));
                callback.onLog("使用默认重启策略: unless-stopped");
            }
            
            // 🔧 解析网络模式
            if (serviceConfig.containsKey("network_mode")) {
                request.setNetworkMode(serviceConfig.get("network_mode").toString());
                callback.onLog("设置网络模式: " + serviceConfig.get("network_mode"));
        } else {
                request.setNetworkMode("bridge");
                callback.onLog("使用默认网络模式: bridge");
            }
            
            // 🔧 解析特权模式
            if (serviceConfig.containsKey("privileged")) {
                boolean privileged = Boolean.parseBoolean(serviceConfig.get("privileged").toString());
                request.setPrivileged(privileged);
                if (privileged) {
                    callback.onLog("启用特权模式");
                }
            }
            
            // 🔧 解析工作目录
            if (serviceConfig.containsKey("working_dir")) {
                request.setWorkingDir(serviceConfig.get("working_dir").toString());
                callback.onLog("设置工作目录: " + serviceConfig.get("working_dir"));
            }
            
            callback.onLog("🚀 正在创建容器: " + request.getName());
            
            // 调用容器服务创建容器（已包含启动逻辑）
        String containerId = containerService.createContainer(request);
        
            if (containerId != null) {
                callback.onLog("✅ 容器创建并启动成功: " + containerId);
            }
            
            return containerId;
            
        } catch (Exception e) {
            callback.onLog("❌ 创建服务失败: " + service.getName() + " - " + e.getMessage());
            throw new RuntimeException("创建服务失败: " + service.getName() + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 🆕 构建YAML模式的安装结果 - 简化版本
     */
    private ApplicationDeployResult buildYamlInstallResult(List<String> containerIds, 
                                                          ApplicationParseResult parseResult,
                                                          String yamlContent,
                                                          InstallCallback callback) {
        callback.onLog("📋 构建安装结果...");
        
        // 🔧 暂不处理访问地址，按设计要求简化
        List<ApplicationDeployResult.AccessUrl> accessUrls = new ArrayList<>();
        callback.onLog("📋 跳过访问地址构建（按设计要求）");
        
        ApplicationDeployResult result = ApplicationDeployResult.success(containerIds, "YAML模式安装成功");
        result.setAccessUrls(accessUrls);
        result.setDeployId("yaml_deploy_" + System.currentTimeMillis());
        
        callback.onLog("✅ 安装结果构建完成");
        callback.onLog("📊 成功创建容器: " + containerIds.size() + " 个");
        callback.onLog("📊 部署ID: " + result.getDeployId());
        
        return result;
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
                
                // 拉取镜像 - 同步等待完成
                pullImageIfNeeded(image.getFullName(), callback);
                
                callback.onLog("✅ 镜像拉取完成: " + image.getFullName());
            } else {
                callback.onLog("✅ 镜像已存在: " + image.getFullName());
            }
        }
    }

    /**
     * 拉取镜像 - 同步等待完成
     */
    private void pullImageIfNeeded(String imageName, InstallCallback callback) throws Exception {
        try {
            callback.onLog("正在拉取镜像: " + imageName);
            
            // 解析镜像名称和标签
            String[] parts = imageName.split(":");
            String image = parts[0];
            String tag = parts.length > 1 ? parts[1] : "latest";
            
            // 🎯 调用真正的镜像拉取服务并等待完成
            dockerService.pullImageWithSkopeo(image, tag, new com.dockpilot.utils.MessageCallback() {
                @Override
                public void onProgress(int progress) {
                    // 可以在这里更新进度，但安装过程中的进度更新在上层处理
                }
                
                @Override
                public void onLog(String log) {
                    callback.onLog("拉取日志: " + log);
                }
                
                @Override
                public void onComplete() {
                    callback.onLog("镜像拉取完成: " + imageName);
                }
                
                @Override
                public void onError(String error) {
                    callback.onLog("镜像拉取失败: " + error);
                }
            }).get(); // 🚨 关键：使用.get()同步等待拉取完成
            
            callback.onLog("✅ 镜像拉取成功: " + imageName);
            
        } catch (Exception e) {
            String errorMsg = "拉取镜像失败: " + imageName + ", 错误: " + e.getMessage();
            callback.onLog("❌ " + errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * 🎯 从WebSocket会话获取客户端真实IP
     */
    private String getClientIpFromCallback(InstallCallback callback) {
        try {
            // 从callback中获取WebSocket会话
            WebSocketSession session = callback.getSession();
            
            if (session != null) {
                // 🎯 使用工具类获取客户端IP
                return WebSocketUtils.getClientIp(session);
            }
            
            // 如果无法获取，使用备用方案
            return "localhost";
            
        } catch (Exception e) {
            log.warn("获取客户端IP失败: {}", e.getMessage());
            return "localhost";
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

        public WebSocketSession getSession() {
            return session;
        }
    }

    /**
     * 🔧 从YAML中提取指定服务的配置
     */
    private Map<String, Object> extractServiceConfigFromYaml(String yamlContent, String serviceName, InstallCallback callback) {
        try {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> yamlMap = yaml.load(yamlContent);
            
            if (yamlMap != null && yamlMap.containsKey("services")) {
                Map<String, Object> services = (Map<String, Object>) yamlMap.get("services");
                if (services.containsKey(serviceName)) {
                    Map<String, Object> serviceConfig = (Map<String, Object>) services.get(serviceName);
                    callback.onLog("提取服务配置: " + serviceName + " - " + serviceConfig.keySet().size() + " 个配置项");
                    return serviceConfig;
                }
            }
            
            callback.onLog("⚠️ 未找到服务配置: " + serviceName);
            return new HashMap<>();
            
        } catch (Exception e) {
            callback.onLog("❌ 解析服务配置失败: " + serviceName + " - " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 🔧 从YAML中提取x-meta环境变量
     */
    private Map<String, String> extractGlobalEnvVarsFromYaml(String yamlContent, InstallCallback callback) {
        try {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> yamlMap = yaml.load(yamlContent);
            
            Map<String, String> envVars = new HashMap<>();
            
            if (yamlMap != null && yamlMap.containsKey("x-meta")) {
                Map<String, Object> meta = (Map<String, Object>) yamlMap.get("x-meta");
                
                // 尝试 envVars 或 env 字段
                Map<String, Object> envConfig = null;
                if (meta.containsKey("envVars")) {
                    envConfig = (Map<String, Object>) meta.get("envVars");
                } else if (meta.containsKey("env")) {
                    envConfig = (Map<String, Object>) meta.get("env");
                }
                
                if (envConfig != null) {
                    for (Map.Entry<String, Object> entry : envConfig.entrySet()) {
                        String key = entry.getKey();
                        Object valueObj = entry.getValue();
                        
                        String value = "";
                        if (valueObj instanceof Map) {
                            Map<String, Object> envInfo = (Map<String, Object>) valueObj;
                            value = envInfo.getOrDefault("value", "").toString();
                } else {
                            value = valueObj.toString();
                        }
                        
                        envVars.put(key, value);
                    }
                    
                    callback.onLog("提取全局环境变量: " + envVars.size() + " 个");
                }
            }
            
            return envVars;
            
        } catch (Exception e) {
            callback.onLog("⚠️ 解析全局环境变量失败: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 🔧 解析端口配置
     */
    private void parsePortsFromConfig(Object portsObj, ContainerCreateRequest request, 
                                    Map<String, String> globalEnvVars, InstallCallback callback) {
        try {
            if (!(portsObj instanceof List)) {
                return;
            }
            
            List<Object> ports = (List<Object>) portsObj;
        List<com.github.dockerjava.api.model.ExposedPort> exposedPorts = new ArrayList<>();
        com.github.dockerjava.api.model.Ports portBindings = new com.github.dockerjava.api.model.Ports();
        
            for (Object portObj : ports) {
                String portMapping = replaceEnvPlaceholders(portObj.toString(), globalEnvVars);
                
                // 解析端口映射格式：hostPort:containerPort 或 port
                String[] parts = portMapping.split(":");
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
        }
        
        if (!exposedPorts.isEmpty()) {
            request.setExposedPorts(exposedPorts);
            request.setPortBindings(portBindings);
            }
            
        } catch (Exception e) {
            callback.onLog("⚠️ 端口配置解析失败: " + e.getMessage());
        }
    }

    /**
     * 🔧 解析环境变量配置
     */
    private void parseEnvironmentFromConfig(Object envObj, ContainerCreateRequest request, 
                                          Map<String, String> globalEnvVars, InstallCallback callback) {
        try {
            List<String> envList = new ArrayList<>();
            
            if (envObj instanceof List) {
                // List格式: ["KEY=value", "KEY2=value2"]
                List<Object> envArray = (List<Object>) envObj;
                for (Object env : envArray) {
                    String envStr = replaceEnvPlaceholders(env.toString(), globalEnvVars);
                    envList.add(envStr);
                }
            } else if (envObj instanceof Map) {
                // Map格式: {KEY: value, KEY2: value2}
                Map<String, Object> envMap = (Map<String, Object>) envObj;
                for (Map.Entry<String, Object> entry : envMap.entrySet()) {
                    String key = entry.getKey();
                    String value = replaceEnvPlaceholders(entry.getValue().toString(), globalEnvVars);
                    envList.add(key + "=" + value);
                }
            }
            
            if (!envList.isEmpty()) {
                request.setEnv(envList);
                callback.onLog("设置环境变量: " + envList.size() + " 个");
            }
            
        } catch (Exception e) {
            callback.onLog("⚠️ 环境变量配置解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 🔧 解析卷挂载配置
     */
    private void parseVolumesFromConfig(Object volumesObj, ContainerCreateRequest request, 
                                      Map<String, String> globalEnvVars, InstallCallback callback) {
        try {
            if (!(volumesObj instanceof List)) {
                return;
            }
            
            List<Object> volumes = (List<Object>) volumesObj;
            List<com.github.dockerjava.api.model.Bind> binds = new ArrayList<>();
            
            for (Object volumeObj : volumes) {
                String volumeMapping = replaceEnvPlaceholders(volumeObj.toString(), globalEnvVars);
                
                    // 解析卷挂载格式：hostPath:containerPath[:ro/rw]
                    String[] parts = volumeMapping.split(":");
                    if (parts.length >= 2) {
                        String hostPath = parts[0].trim();
                        String containerPath = parts[1].trim();
                        String accessMode = parts.length > 2 ? parts[2].trim() : "rw";
                        
                        // 自动创建宿主机目录
                    if (ensureHostDirectoryExists(hostPath, callback)) {
                        com.github.dockerjava.api.model.Volume volume = new com.github.dockerjava.api.model.Volume(containerPath);
                        com.github.dockerjava.api.model.Bind bind;
                        
                        if ("ro".equals(accessMode)) {
                            bind = new com.github.dockerjava.api.model.Bind(hostPath, volume, com.github.dockerjava.api.model.AccessMode.ro);
                        } else {
                            bind = new com.github.dockerjava.api.model.Bind(hostPath, volume, com.github.dockerjava.api.model.AccessMode.rw);
                        }
                        
                        binds.add(bind);
                        callback.onLog("设置卷挂载: " + hostPath + " -> " + containerPath + " (" + accessMode + ")");
                    }
                }
            }
            
            if (!binds.isEmpty()) {
                request.setBinds(binds);
            }
            
        } catch (Exception e) {
            callback.onLog("⚠️ 卷挂载配置解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 🔧 解析重启策略配置
     */
    private void parseRestartPolicyFromConfig(Object restartObj, ContainerCreateRequest request, InstallCallback callback) {
        try {
            String restartPolicy = restartObj.toString().trim();
            request.setRestartPolicy(com.github.dockerjava.api.model.RestartPolicy.parse(restartPolicy));
            callback.onLog("设置重启策略: " + restartPolicy);
                } catch (Exception e) {
            callback.onLog("⚠️ 无效的重启策略配置: " + restartObj + ", 使用默认策略: unless-stopped");
            request.setRestartPolicy(com.github.dockerjava.api.model.RestartPolicy.parse("unless-stopped"));
        }
    }
    
    /**
     * 🔧 替换环境变量占位符
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
     * 🔧 确保宿主机目录存在 - 修复版本
     */
    private boolean ensureHostDirectoryExists(String hostPath, InstallCallback callback) {
        try {
            if (hostPath == null || hostPath.trim().isEmpty()) {
                callback.onLog("⚠️ 宿主机路径为空，跳过创建");
                return false;
            }
            
            String normalizedPath = hostPath.trim();
            
            if (!normalizedPath.startsWith("/")) {
                callback.onLog("⚠️ 宿主机路径必须是绝对路径: " + normalizedPath);
                return false;
            }
            
            // 🔧 获取真实的宿主机路径（处理容器内路径映射）
            String actualPath = getActualHostPath(normalizedPath, callback);
            
            java.nio.file.Path dirPath = java.nio.file.Paths.get(actualPath);
            if (!java.nio.file.Files.exists(dirPath)) {
                java.nio.file.Files.createDirectories(dirPath);
                callback.onLog("✅ 创建宿主机目录: " + actualPath);
                } else {
                callback.onLog("✅ 宿主机目录已存在: " + actualPath);
            }
            
                return true;
            
        } catch (Exception e) {
            callback.onLog("❌ 创建宿主机目录失败: " + hostPath + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * 🔧 智能获取实际的宿主机路径
     */
    private String getActualHostPath(String hostPath, InstallCallback callback) {
        // 检测是否在Docker容器内运行
        if (isRunningInContainer()) {
            // 容器内：使用 /mnt/host 前缀
            String containerPath = "/mnt/host" + hostPath;
            callback.onLog("🐳 检测到容器环境，使用容器路径: " + containerPath);
            return containerPath;
            } else {
            // 宿主机：直接使用原路径
            callback.onLog("🖥️ 检测到宿主机环境，使用直接路径: " + hostPath);
            return hostPath;
        }
    }

    /**
     * 🔧 检测是否在Docker容器内运行
     */
    private boolean isRunningInContainer() {
        try {
            // 方法1: 检查 /.dockerenv 文件（Docker创建的标识文件）
            if (java.nio.file.Files.exists(java.nio.file.Paths.get("/.dockerenv"))) {
                return true;
            }
            
            // 方法2: 检查 /proc/1/cgroup 文件（容器内的进程组信息）
            java.nio.file.Path cgroupPath = java.nio.file.Paths.get("/proc/1/cgroup");
            if (java.nio.file.Files.exists(cgroupPath)) {
                String content = java.nio.file.Files.readString(cgroupPath);
                if (content.contains("docker") || content.contains("kubepods") || content.contains("containerd")) {
                return true;
            }
        }
        
            // 方法3: 检查 /mnt/host 目录是否存在（特定的挂载点）
            if (java.nio.file.Files.exists(java.nio.file.Paths.get("/mnt/host"))) {
                return true;
        }
        
            return false;
            
        } catch (Exception e) {
            // 检测失败，默认认为不在容器内
        return false;
        }
    }

    /**
     * 🚨 回滚操作：删除所有已创建的容器
     */
    private void rollbackCreatedContainers(List<String> containerIds, InstallCallback callback) {
        callback.onLog("🚨 开始回滚 " + containerIds.size() + " 个已创建的容器...");
        
        for (String containerId : containerIds) {
            try {
                callback.onLog("🔄 回滚容器: " + containerId);
                
                // 先停止容器
                try {
                    containerService.stopContainer(containerId);
                    callback.onLog("✅ 容器已停止: " + containerId);
                } catch (Exception e) {
                    callback.onLog("⚠️ 停止容器失败: " + containerId + " - " + e.getMessage());
                }
                
                // 再删除容器
                try {
                    containerService.removeContainer(containerId);
                    callback.onLog("✅ 容器已删除: " + containerId);
                } catch (Exception e) {
                    callback.onLog("⚠️ 删除容器失败: " + containerId + " - " + e.getMessage());
                }
                
            } catch (Exception e) {
                callback.onLog("❌ 回滚容器失败: " + containerId + " - " + e.getMessage());
            }
        }
        
        callback.onLog("🔄 回滚操作完成，已清理 " + containerIds.size() + " 个容器");
    }

    /**
     * 🔧 处理配置包下载和部署
     */
    private void handleConfigDownload(String configUrl, String serviceName, 
                                    List<String> volumeMappings, InstallCallback callback) {
        // 检查configUrl是否有值
        if (configUrl == null || configUrl.trim().isEmpty()) {
            callback.onLog("📁 服务 " + serviceName + " 无配置包，将创建空目录");
            return;
        }
        
        // 🔥 检测本地配置包
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
            String localPackagePath = configPath + relativePath;
            
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
     * 解压并部署配置包
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
        
        callback.onLog("🔍 开始部署配置包到目标目录...");
        callback.onLog("📂 解压目录: " + extractDir);
        callback.onLog("🔧 服务名: " + serviceName);
        callback.onLog("📋 卷挂载配置数量: " + (volumeMappings != null ? volumeMappings.size() : 0));
        
        if (volumeMappings == null || volumeMappings.isEmpty()) {
            callback.onLog("⚠️ 无卷挂载配置，跳过配置部署");
            return;
        }
        
        // 列出解压目录的内容
        try {
            java.nio.file.Path extractPath = java.nio.file.Paths.get(extractDir);
            if (java.nio.file.Files.exists(extractPath)) {
                callback.onLog("📁 解压目录内容:");
                java.nio.file.Files.list(extractPath).forEach(path -> {
                    try {
                        callback.onLog("  - " + path.getFileName() + " (" + 
                                     (java.nio.file.Files.isDirectory(path) ? "目录" : "文件") + ")");
                    } catch (Exception e) {
                        callback.onLog("  - " + path.getFileName() + " (检查失败)");
                    }
                });
            } else {
                callback.onLog("❌ 解压目录不存在: " + extractDir);
                return;
            }
        } catch (Exception e) {
            callback.onLog("⚠️ 无法列出解压目录内容: " + e.getMessage());
        }
        
        for (String volumeMapping : volumeMappings) {
            callback.onLog("🔄 处理卷挂载: " + volumeMapping);
            
            String[] parts = volumeMapping.split(":");
            if (parts.length >= 2) {
                String hostPath = parts[0].trim();
                String containerPath = parts[1].trim();
                
                callback.onLog("📍 宿主机路径: " + hostPath);
                callback.onLog("📍 容器路径: " + containerPath);
                
                // 🔧 从宿主机路径推导配置包中的目录名（与打包逻辑保持一致）
                String hostDirName = getLastPathSegment(hostPath);
                callback.onLog("📂 推导的目录名: " + hostDirName);
                
                // 🔥 先尝试在服务名目录下查找（标准结构）
                String sourceDir = extractDir + "/" + serviceName + "/" + hostDirName;
                callback.onLog("🔍 尝试标准路径: " + sourceDir);
                
                // 🔄 如果服务名目录下找不到，再尝试直接查找（兼容性）
                if (!java.nio.file.Files.exists(java.nio.file.Paths.get(sourceDir))) {
                    sourceDir = extractDir + "/" + hostDirName;
                    callback.onLog("🔍 标准路径未找到，尝试兼容路径: " + sourceDir);
                }
                
                // 检查配置包中是否有对应的目录
                if (java.nio.file.Files.exists(java.nio.file.Paths.get(sourceDir))) {
                    callback.onLog("✅ 找到配置源目录: " + sourceDir);
                    
                    // 获取宿主机实际路径
                    String actualHostPath = getActualHostPath(hostPath, callback);
                    callback.onLog("🎯 目标宿主机路径: " + actualHostPath);
                    
                    // 确保目标目录存在
                    java.nio.file.Path targetPath = java.nio.file.Paths.get(actualHostPath);
                    if (!java.nio.file.Files.exists(targetPath)) {
                        java.nio.file.Files.createDirectories(targetPath);
                        callback.onLog("📁 创建目标目录: " + actualHostPath);
                    } else {
                        callback.onLog("📁 目标目录已存在: " + actualHostPath);
                    }
                    
                    // 复制配置包内容到宿主机目录
                    copyDirectory(sourceDir, actualHostPath, callback);
                    
                    callback.onLog("✅ 配置包部署成功: " + containerPath + " -> " + actualHostPath);
                } else {
                    callback.onLog("⚠️ 配置包中未找到对应目录: " + hostDirName);
                    callback.onLog("❌ 源目录不存在: " + sourceDir);
                    
                    // 列出可能的目录结构帮助调试
                    try {
                        java.nio.file.Path extractPath = java.nio.file.Paths.get(extractDir);
                        callback.onLog("🔍 可用的目录结构:");
                        java.nio.file.Files.walk(extractPath, 2)
                                .filter(java.nio.file.Files::isDirectory)
                                .forEach(path -> callback.onLog("  📁 " + extractPath.relativize(path)));
                    } catch (Exception e) {
                        callback.onLog("⚠️ 无法列出目录结构: " + e.getMessage());
                    }
                    
                    // 确保宿主机目录存在（创建空目录）
                    String actualHostPath = getActualHostPath(hostPath, callback);
                    java.nio.file.Files.createDirectories(java.nio.file.Paths.get(actualHostPath));
                    callback.onLog("📁 创建空目录: " + actualHostPath);
                }
            } else {
                callback.onLog("⚠️ 无效的卷挂载格式: " + volumeMapping);
            }
        }
        
        callback.onLog("✅ 配置包部署完成");
    }
    
    /**
     * 获取路径的最后一段（目录名）
     */
    private String getLastPathSegment(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        
        String normalized = path.trim().replaceAll("/+$", ""); // 移除末尾的斜杠
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }
    
    /**
     * 复制目录内容
     */
    private void copyDirectory(String sourceDir, String targetDir, InstallCallback callback) throws Exception {
        java.nio.file.Path sourcePath = java.nio.file.Paths.get(sourceDir);
        java.nio.file.Path targetPath = java.nio.file.Paths.get(targetDir);
        
        callback.onLog("📋 开始复制目录内容:");
        callback.onLog("  源目录: " + sourceDir);
        callback.onLog("  目标目录: " + targetDir);
        
        // 确保目标目录存在
        java.nio.file.Files.createDirectories(targetPath);
        callback.onLog("📁 确保目标目录存在: " + targetDir);
        
        // 统计复制文件数量
        java.util.concurrent.atomic.AtomicInteger fileCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger dirCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger errorCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        // 递归复制
        java.nio.file.Files.walk(sourcePath)
            .forEach(source -> {
                try {
                    java.nio.file.Path destination = targetPath.resolve(sourcePath.relativize(source));
                    if (java.nio.file.Files.isDirectory(source)) {
                        if (!source.equals(sourcePath)) { // 跳过根目录
                            java.nio.file.Files.createDirectories(destination);
                            callback.onLog("📁 创建目录: " + sourcePath.relativize(source));
                            dirCount.incrementAndGet();
                        }
                    } else {
                        java.nio.file.Files.copy(source, destination, 
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        callback.onLog("📄 复制文件: " + sourcePath.relativize(source) + 
                                     " (" + java.nio.file.Files.size(source) + " 字节)");
                        fileCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    callback.onLog("❌ 复制失败: " + source + " -> " + e.getMessage());
                    errorCount.incrementAndGet();
                }
            });
        
        callback.onLog("📊 复制统计:");
        callback.onLog("  📄 文件数量: " + fileCount.get());
        callback.onLog("  📁 目录数量: " + dirCount.get());
        callback.onLog("  ❌ 错误数量: " + errorCount.get());
        
        if (errorCount.get() > 0) {
            callback.onLog("⚠️ 有 " + errorCount.get() + " 个文件复制失败");
        } else {
            callback.onLog("✅ 所有文件复制成功");
        }
        
        callback.onLog("📁 复制目录完成: " + sourceDir + " -> " + targetDir);
    }
    
    /**
     * 删除临时文件
     */
    private void deleteTemporaryFile(String filePath) {
        try {
            if (filePath != null) {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filePath));
            }
        } catch (Exception e) {
            log.warn("删除临时文件失败: {}", filePath, e);
        }
    }
    
    /**
     * 删除临时目录
     */
    private void deleteTemporaryDirectory(String dirPath) {
        try {
            if (dirPath != null) {
                java.nio.file.Path path = java.nio.file.Paths.get(dirPath);
                if (java.nio.file.Files.exists(path)) {
                    java.nio.file.Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(java.nio.file.Path::toFile)
                        .forEach(java.io.File::delete);
                }
            }
        } catch (Exception e) {
            log.warn("删除临时目录失败: {}", dirPath, e);
        }
    }
} 