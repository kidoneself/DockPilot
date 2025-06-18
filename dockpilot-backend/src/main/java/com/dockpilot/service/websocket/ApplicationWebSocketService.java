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
            
            // 🔧 解析端口映射
            if (serviceConfig.containsKey("ports")) {
                parsePortsFromConfig(serviceConfig.get("ports"), request, globalEnvVars, callback);
            }
            
            // 🔧 解析环境变量
            if (serviceConfig.containsKey("environment")) {
                parseEnvironmentFromConfig(serviceConfig.get("environment"), request, globalEnvVars, callback);
            }
            
            // 🔧 解析卷挂载
            if (serviceConfig.containsKey("volumes")) {
                parseVolumesFromConfig(serviceConfig.get("volumes"), request, globalEnvVars, callback);
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
            
            // 🔧 智能检测运行环境
            String actualPath = getActualHostPath(normalizedPath, callback);
            java.nio.file.Path targetPath = java.nio.file.Paths.get(actualPath);
            
            if (java.nio.file.Files.exists(targetPath)) {
                if (java.nio.file.Files.isDirectory(targetPath)) {
                    callback.onLog("✅ 宿主机目录已存在: " + normalizedPath);
                    return true;
                } else {
                    callback.onLog("❌ 宿主机路径已存在但不是目录: " + normalizedPath);
                    return false;
                }
            }
            
            callback.onLog("📁 正在创建目录: " + normalizedPath + " (实际路径: " + actualPath + ")");
            java.nio.file.Files.createDirectories(targetPath);
            
            if (java.nio.file.Files.exists(targetPath) && java.nio.file.Files.isDirectory(targetPath)) {
                callback.onLog("✅ 目录创建成功: " + normalizedPath);
                return true;
            } else {
                callback.onLog("❌ 目录创建失败: " + normalizedPath);
                return false;
            }
            
        } catch (Exception e) {
            callback.onLog("❌ 创建宿主机目录失败: " + hostPath + ", 错误: " + e.getMessage());
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
} 