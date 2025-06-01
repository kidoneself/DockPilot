package com.dockpilot.utils;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Docker Compose 生成器
 * 用于将 Docker 容器配置转换为 Docker Compose 格式
 * 支持多容器配置、端口映射、网络设置、环境变量等功能
 * 
 * 🌐 网络处理策略（已优化）：
 * - Host 模式：保留 network_mode: host 配置
 * - 桥接模式：使用 Docker Compose 默认网络（不设置 networks 配置）
 * - 其他网络：统一处理为桥接模式，避免自定义网络不存在导致的启动失败
 * - 移除了复杂的外部网络定义，确保导出的 compose 文件能够正常导入和启动
 * 
 * 🔥 路径处理策略（新增）：
 * - Docker专用目录：标准化到 ${DOCKER_BASE}/service_name/container_path
 * - 共享资源目录：智能分组抽取为 ${BASE_X} 环境变量
 * - 系统目录：保持原样不动
 */
@Component
public class ComposeGenerator {

    @Autowired
    private com.dockpilot.api.DockerService dockerService;
    
    // 🔥 新增：注入AppConfig获取docker_base_dir配置
    @Autowired
    private com.dockpilot.common.config.AppConfig appConfig;

    // 🔥 新增：Docker专用路径模式
    private static final Set<String> DOCKER_SPECIFIC_PATTERNS = Set.of(
        "/config", "/configuration", "/settings",
        "/data", "/database", "/storage", 
        "/logs", "/log",
        "/cache", "/tmp", "/temp",
        "/app", "/application", "/workspace",
        "/plugins", "/extensions", "/themes",
        "/uploads", "/scripts",
        "/var/lib", "/opt"
    );
    
    // 🔥 新增：系统路径模式（补充更完整的系统路径）
    private static final Set<String> SYSTEM_PATTERNS = Set.of(
        "/var/run", "/dev", "/sys", "/proc", "/run",
        "/etc/timezone", "/etc/localtime", "/etc/passwd", "/etc/group", 
        "/etc/hosts", "/etc/hostname", "/etc/resolv.conf", "/etc/nsswitch.conf",
        "/lib", "/lib64", "/usr/lib", "/usr/share/zoneinfo",
        "/bin", "/sbin", "/usr/bin", "/usr/sbin"
    );

    /**
     * 🔥 新增：获取Docker基础目录
     */
    private String getDockerBaseDir() {
        try {
            if (appConfig == null) {
                throw new IllegalStateException("系统配置未初始化，请重启应用");
            }
            if (!appConfig.isDockerBaseDirConfigured()) {
                throw new IllegalStateException("Docker运行目录未配置，请在系统设置中配置Docker运行目录");
            }
            return appConfig.getDockerBaseDirOrThrow();
        } catch (Exception e) {
            throw new IllegalStateException("获取Docker运行目录失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 🔥 新增：判断是否为Docker专用路径
     */
    private boolean isDockerSpecific(String containerPath) {
        if (containerPath == null || containerPath.trim().isEmpty()) {
            return false;
        }
        return DOCKER_SPECIFIC_PATTERNS.stream()
                .anyMatch(containerPath::startsWith);
    }
    
    /**
     * 🔥 新增：判断是否为系统路径
     */
    private boolean isSystemPath(String hostPath) {
        if (hostPath == null || hostPath.trim().isEmpty()) {
            return false;
        }
        return SYSTEM_PATTERNS.stream()
                .anyMatch(hostPath::startsWith);
    }

    /**
     * 生成 Docker Compose 文件
     *
     * @param containers    容器信息列表
     * @param outputPath    输出文件路径
     * @param templateFile  模板文件路径（可选）
     * @param excludeFields 需要排除的字段集合
     * @throws IOException 文件操作异常
     */
    public void generateComposeFile(List<InspectContainerResponse> containers, String outputPath, String templateFile, Set<String> excludeFields) throws IOException {
        // 初始化 Compose 配置结构
        Map<String, Object> compose = new LinkedHashMap<>();
        Map<String, Object> services = new LinkedHashMap<>();
        // 🚫 移除networks配置 - 不需要定义外部桥接网络
        // Map<String, Object> networks = new LinkedHashMap<>();

        // 收集所有容器的端口映射和路径
        Map<String, String> portMappings = new LinkedHashMap<>();
        Set<String> allPaths = new HashSet<>();

        // 处理每个容器
        for (InspectContainerResponse container : containers) {
            String serviceName = getServiceName(container);
            Map<String, Object> service = convertContainerToService(container, excludeFields);
            service.put("container_name", serviceName);

            // 处理端口映射
            if (service.containsKey("ports")) {
                @SuppressWarnings("unchecked")
                List<String> ports = (List<String>) service.get("ports");
                List<String> newPorts = new ArrayList<>();
                for (String portMapping : ports) {
                    String[] parts = portMapping.split(":");
                    if (parts.length == 2) {
                        String hostPort = parts[0];
                        String containerPort = parts[1];
                        // 从镜像名称中提取最后一个部分
                        String imageName = container.getConfig().getImage();
                        String shortName = imageName;
                        if (imageName.contains("/")) {
                            String[] imageParts = imageName.split("/");
                            shortName = imageParts[imageParts.length - 1];
                        }
                        // 移除版本标签
                        if (shortName.contains(":")) {
                            shortName = shortName.split(":")[0];
                        }
                        // 标准化服务名（替换 - 为 _ 并转大写）
                        String normalizedName = shortName.replace("-", "_").toUpperCase();
                        String portKey = normalizedName + "_PORT_" + containerPort;
                        portMappings.put(portKey, hostPort);
                        // 使用环境变量引用替换端口映射
                        newPorts.add("${" + portKey + "}:" + containerPort);
                    }
                }
                service.put("ports", newPorts);
            }

            // 收集路径
            if (service.containsKey("volumes")) {
                @SuppressWarnings("unchecked")
                List<String> volumes = (List<String>) service.get("volumes");
                for (String volume : volumes) {
                    String[] parts = volume.split(":");
                    if (parts.length >= 1) {
                        allPaths.add(parts[0]);
                    }
                }
            }

            // 添加服务级元数据配置
            Map<String, Object> serviceMeta = new LinkedHashMap<>();
            serviceMeta.put("name", serviceName);
            serviceMeta.put("description", "容器服务");
            serviceMeta.put("configUrl", "");  // 直接的一级配置
            service.put("x-meta", serviceMeta);

            services.put(serviceName, service);
        }

        // 处理路径映射 - 使用智能分组算法
        Map<String, List<String>> basePathGroups = analyzePathGroups(allPaths);
        Map<String, String> baseEnv = new LinkedHashMap<>();
        Map<String, String> pathToEnv = new LinkedHashMap<>();
        int baseCount = 1;

        // 根据分组结果生成BASE环境变量
        for (Map.Entry<String, List<String>> entry : basePathGroups.entrySet()) {
            String basePath = entry.getKey();
            List<String> pathsInGroup = entry.getValue();
            
                String envName = "BASE_" + baseCount++;
            baseEnv.put(envName, basePath);

            // 为组内每个路径生成环境变量引用
            for (String path : pathsInGroup) {
                String relative = path.substring(basePath.length());
                if (relative.startsWith("/")) {
                    relative = relative.substring(1);
                }
                // 如果相对路径为空，直接使用环境变量，不添加斜杠
                pathToEnv.put(path, relative.isEmpty() ? "${" + envName + "}" : "${" + envName + "}/" + relative);
            }
        }

        // 更新服务的卷映射
        for (Map.Entry<String, Object> serviceEntry : services.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> service = (Map<String, Object>) serviceEntry.getValue();
            if (service.containsKey("volumes")) {
                @SuppressWarnings("unchecked")
                List<String> volumes = (List<String>) service.get("volumes");
                List<String> newVolumes = new ArrayList<>();
                for (String volume : volumes) {
                    String[] parts = volume.split(":");
                    if (parts.length >= 1) {
                        String hostPath = parts[0];
                        String containerPath = parts.length > 1 ? parts[1] : "";
                        String newHostPath = pathToEnv.getOrDefault(hostPath, hostPath);
                        newVolumes.add(newHostPath + (containerPath.isEmpty() ? "" : ":" + containerPath));
                    }
                }
                service.put("volumes", newVolumes);
            }
        }

        // 添加项目级元数据配置
        Map<String, Object> projectMeta = new LinkedHashMap<>();
        projectMeta.put("name", "Docker容器项目");
        projectMeta.put("description", "Docker容器管理项目");
        projectMeta.put("version", "1.0.0");
        projectMeta.put("author", "System");
        projectMeta.put("category", "container");  // 添加到顶级配置

        // 🔥 配置环境变量 - 只添加基础环境变量，不添加具体路径映射
        Map<String, Object> envVars = new LinkedHashMap<>();
        
        // 🔥 优先添加Docker基础目录环境变量
        Map<String, Object> dockerBaseEnv = new LinkedHashMap<>();
        dockerBaseEnv.put("value", getDockerBaseDir());
        dockerBaseEnv.put("description", "Docker容器基础目录");
        envVars.put("DOCKER_BASE", dockerBaseEnv);
        
        // 🔥 添加语义化的基础目录环境变量（只添加BASE类型的变量）
        for (Map.Entry<String, String> entry : baseEnv.entrySet()) {
            Map<String, Object> envObj = new LinkedHashMap<>();
            envObj.put("value", entry.getValue());
            envObj.put("description", getEnvDescription(entry.getKey()));
            envVars.put(entry.getKey(), envObj);
        }
        
        // 🔥 添加端口环境变量
        for (Map.Entry<String, String> entry : portMappings.entrySet()) {
            Map<String, Object> envObj = new LinkedHashMap<>();
            envObj.put("value", entry.getValue());
            envObj.put("description", getEnvDescription(entry.getKey()));
            envVars.put(entry.getKey(), envObj);
        }
        
        projectMeta.put("env", envVars);

        // 组装最终的 Compose 配置
        compose.put("x-meta", projectMeta);
        compose.put("services", services);

        // 🚫 移除networks配置 - 使用Docker Compose默认网络
        // 只有当有网络配置时才添加 networks 部分
        // if (!networks.isEmpty()) {
        //     compose.put("networks", networks);
        // }

        // 配置 YAML 输出选项
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);  // 使用块样式
        options.setPrettyFlow(true);  // 美化输出
        options.setIndent(2);  // 设置缩进为2空格
        options.setIndicatorIndent(2);  // 设置指示符缩进
        options.setWidth(120);  // 设置行宽
        options.setLineBreak(DumperOptions.LineBreak.UNIX);  // 使用UNIX换行符
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);  // 使用普通标量样式
        options.setIndentWithIndicator(true);  // 使用指示符缩进
        options.setNonPrintableStyle(DumperOptions.NonPrintableStyle.BINARY);  // 处理非打印字符

        // 生成并写入 YAML 文件
        Yaml yaml = new Yaml(options);
        String yamlContent = yaml.dump(compose);
        // 在根节点之间添加双换行
        yamlContent = yamlContent.replace("\nx-meta:", "\n\nx-meta:")
                .replace("\nservices:", "\n\nservices:")
                .replace("\nnetworks:", "\n\nnetworks:");

        // 在每个服务配置块之间添加换行
        StringBuilder formattedContent = new StringBuilder();
        String[] lines = yamlContent.split("\n");
        boolean inService = false;
        boolean firstService = true;
        int currentIndent = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            // 计算当前行的缩进级别
            int indent = line.length() - line.replaceAll("^\\s+", "").length();

            // 检查是否是新服务的开始（缩进为2且以冒号结尾）
            if (indent == 2 && trimmedLine.endsWith(":") && !trimmedLine.startsWith("x-meta") && !trimmedLine.startsWith("services") && !trimmedLine.startsWith("networks")) {
                if (inService && !firstService) {
                    formattedContent.append("\n");  // 在服务之间添加空行
                }
                inService = true;
                firstService = false;
            }

            formattedContent.append(line).append("\n");
        }

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(formattedContent.toString());
        }
    }

    /**
     * 生成 Docker Compose YAML 内容，使用自定义项目信息
     *
     * @param containers    容器信息列表
     * @param excludeFields 需要排除的字段集合
     * @param projectName 项目名称
     * @param projectDescription 项目描述
     * @return YAML 格式的字符串
     */
    public String generateComposeContent(List<InspectContainerResponse> containers, Set<String> excludeFields, String projectName, String projectDescription) {
        
        // 🔥 检查Docker基础目录配置
        String dockerBaseDir;
        try {
            dockerBaseDir = getDockerBaseDir();
        } catch (IllegalStateException e) {
            throw new RuntimeException("无法生成配置：" + e.getMessage());
        }
        
        Map<String, Object> compose = new LinkedHashMap<>();
        Map<String, Object> services = new LinkedHashMap<>();
        
        // 🔥 使用语义分组和收集端口
        Map<String, String> pathToEnvMapping = analyzePathsByContainerSemantics(containers);
        Map<String, String> portMappings = new HashMap<>();

        // 处理每个容器
        for (InspectContainerResponse container : containers) {
            String serviceName = getServiceName(container);
            Map<String, Object> service = convertContainerToService(container, excludeFields);
            service.put("container_name", serviceName);
            
            // 🔥 处理端口映射
            processPortMappings(container, service, portMappings);
            
            // 🔥 处理volumes路径映射
            processVolumeMappings(container, service, serviceName, dockerBaseDir, pathToEnvMapping);
            
            // 添加服务元数据
            Map<String, Object> serviceMeta = new LinkedHashMap<>();
            serviceMeta.put("name", serviceName);
            serviceMeta.put("description", "容器服务");
            serviceMeta.put("configUrl", "");
            service.put("x-meta", serviceMeta);

            services.put(serviceName, service);
        }
        
        compose.put("services", services);

        // 🔥 添加项目元数据和环境变量
        addProjectMetadata(compose, projectName, projectDescription, dockerBaseDir, portMappings);

        return generateYamlString(compose);
    }
    
    /**
     * 🔥 简化：处理端口映射
     */
    private void processPortMappings(InspectContainerResponse container, Map<String, Object> service, Map<String, String> portMappings) {
        if (!service.containsKey("ports")) return;
        
        @SuppressWarnings("unchecked")
        List<String> ports = (List<String>) service.get("ports");
        List<String> newPorts = new ArrayList<>();
        
        String imageName = container.getConfig().getImage();
        String shortName = extractImageName(imageName);
        
        for (String portMapping : ports) {
            String[] parts = portMapping.split(":");
            if (parts.length == 2) {
                String hostPort = parts[0];
                String containerPort = parts[1];
                String portKey = shortName.toUpperCase() + "_PORT_" + containerPort;
                portMappings.put(portKey, hostPort);
                newPorts.add("${" + portKey + "}:" + containerPort);
            }
        }
        service.put("ports", newPorts);
    }
    
    /**
     * 🔥 简化：处理卷映射
     */
    private void processVolumeMappings(InspectContainerResponse container, Map<String, Object> service, 
                                     String serviceName, String dockerBaseDir, Map<String, String> pathToEnvMapping) {
        if (!service.containsKey("volumes")) return;
        
        @SuppressWarnings("unchecked")
        List<String> volumes = (List<String>) service.get("volumes");
        List<String> newVolumes = new ArrayList<>();
        
        for (String volume : volumes) {
            String[] parts = volume.split(":");
            if (parts.length >= 2) {
                String hostPath = parts[0];
                String containerPath = parts[1];
                String newHostPath = transformPath(hostPath, containerPath, serviceName, dockerBaseDir, pathToEnvMapping);
                
                if (parts.length == 3) {
                    newVolumes.add(newHostPath + ":" + containerPath + ":" + parts[2]);
                } else {
                    newVolumes.add(newHostPath + ":" + containerPath);
                }
            } else {
                newVolumes.add(volume);
            }
        }
        service.put("volumes", newVolumes);
    }
    
    /**
     * 🔥 简化：路径转换逻辑
     */
    private String transformPath(String hostPath, String containerPath, String serviceName, 
                                String dockerBaseDir, Map<String, String> pathToEnvMapping) {
        if (isSystemPath(hostPath)) {
            return hostPath; // 系统路径保持原样
        } else if (isDockerSpecific(containerPath)) {
            return "${DOCKER_BASE}/" + serviceName + "/" + containerPath.substring(1); // Docker专用路径
        } else {
            return pathToEnvMapping.getOrDefault(hostPath, hostPath); // 其他路径使用语义映射
        }
    }
    
    /**
     * 🔥 简化：提取镜像名称
     */
    private String extractImageName(String imageName) {
        String shortName = imageName;
        if (imageName.contains("/")) {
            String[] parts = imageName.split("/");
            shortName = parts[parts.length - 1];
        }
        if (shortName.contains(":")) {
            shortName = shortName.split(":")[0];
        }
        return shortName.replace("-", "_");
    }
    
    /**
     * 🔥 简化：添加项目元数据
     */
    private void addProjectMetadata(Map<String, Object> compose, String projectName, String projectDescription, 
                                   String dockerBaseDir, Map<String, String> portMappings) {
        Map<String, Object> projectMeta = new LinkedHashMap<>();
        projectMeta.put("name", projectName != null && !projectName.trim().isEmpty() ? projectName.trim() : "Docker容器项目");
        projectMeta.put("description", projectDescription != null && !projectDescription.trim().isEmpty() ? projectDescription.trim() : "Docker容器管理项目");
        projectMeta.put("version", "1.0.0");
        projectMeta.put("author", "System");
        projectMeta.put("category", "container");

        // 🔥 简化环境变量处理
        Map<String, Object> envVars = new LinkedHashMap<>();
        
        // Docker基础目录
        Map<String, Object> dockerBaseEnv = new LinkedHashMap<>();
        dockerBaseEnv.put("value", dockerBaseDir);
        dockerBaseEnv.put("description", "Docker容器基础目录");
        envVars.put("DOCKER_BASE", dockerBaseEnv);
        
        // 语义化基础目录
        for (Map.Entry<String, String> entry : semanticBaseEnvs.entrySet()) {
            Map<String, Object> envObj = new LinkedHashMap<>();
            envObj.put("value", entry.getValue());
            envObj.put("description", getEnvDescription(entry.getKey()));
            envVars.put(entry.getKey(), envObj);
        }
        
        // 端口环境变量
        for (Map.Entry<String, String> entry : portMappings.entrySet()) {
            Map<String, Object> envObj = new LinkedHashMap<>();
            envObj.put("value", entry.getValue());
            envObj.put("description", "服务端口映射");
            envVars.put(entry.getKey(), envObj);
        }
        
        projectMeta.put("env", envVars);
        compose.put("x-meta", projectMeta);
    }
    
    /**
     * 🔥 简化：生成YAML字符串
     */
    private String generateYamlString(Map<String, Object> compose) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setWidth(120);
        options.setLineBreak(DumperOptions.LineBreak.UNIX);

        Yaml yaml = new Yaml(options);
        String yamlContent = yaml.dump(compose);
        
        // 格式化输出
        yamlContent = yamlContent.replace("\nx-meta:", "\n\nx-meta:")
                .replace("\nservices:", "\n\nservices:");

        return formatServiceBlocks(yamlContent);
    }
    
    /**
     * 🔥 简化：格式化服务块
     */
    private String formatServiceBlocks(String yamlContent) {
        StringBuilder formatted = new StringBuilder();
        String[] lines = yamlContent.split("\n");
        boolean firstService = true;

        for (String line : lines) {
            String trimmed = line.trim();
            int indent = line.length() - line.replaceAll("^\\s+", "").length();

            if (indent == 2 && trimmed.endsWith(":") && 
                !trimmed.startsWith("x-meta") && !trimmed.startsWith("services")) {
                if (!firstService) {
                    formatted.append("\n");
                }
                firstService = false;
            }
            formatted.append(line).append("\n");
        }
        return formatted.toString();
    }

    /**
     * 获取服务名称
     *
     * @param container 容器信息
     * @return 服务名称
     */
    String getServiceName(InspectContainerResponse container) {
        String name = container.getName();
        if (name != null && !name.isEmpty()) {
            return name.startsWith("/") ? name.substring(1) : name;
        }
        return "container_" + container.getId().substring(0, 8) + "_" + System.currentTimeMillis();
    }

    /**
     * 检查字段是否应该被包含
     *
     * @param fieldName     字段名
     * @param excludeFields 排除字段集合
     * @return 是否应该包含该字段
     */
    private boolean shouldIncludeField(String fieldName, Set<String> excludeFields) {
        return excludeFields == null || !excludeFields.contains(fieldName);
    }

    /**
     * 如果字段未被排除，则添加到映射中
     *
     * @param map           目标映射
     * @param key           键
     * @param value         值
     * @param excludeFields 排除字段集合
     */
    private void addFieldIfNotExcluded(Map<String, Object> map, String key, Object value, Set<String> excludeFields) {
        if (shouldIncludeField(key, excludeFields)) {
            if (value instanceof Collection) {
                if (!((Collection<?>) value).isEmpty()) {
                    map.put(key, value);
                }
            } else if (value != null) {
                map.put(key, value);
            }
        }
    }

    /**
     * 将容器信息转换为服务配置
     *
     * @param container     容器信息
     * @param excludeFields 需要排除的字段集合
     * @return 服务配置映射
     */
    Map<String, Object> convertContainerToService(InspectContainerResponse container, Set<String> excludeFields) {
        Map<String, Object> service = new LinkedHashMap<>();
        ContainerConfig config = container.getConfig();
        HostConfig hostConfig = container.getHostConfig();
        // 先获取格式化后的容器名称（去除开头的 "/"）
        String name = container.getName().replaceFirst("^/", "");

        // ✅ 加载 container_name 字段
        addFieldIfNotExcluded(service, "container_name", name, excludeFields);
        addFieldIfNotExcluded(service, "image", config.getImage(), excludeFields);
        // command, entrypoint
        addFieldIfNotExcluded(service, "command", arrayToList(config.getCmd()), excludeFields);
        // addFieldIfNotExcluded(service, "entrypoint", arrayToList(config.getEntrypoint()), excludeFields);
        // 🔥 使用过滤后的环境变量
        addFieldIfNotExcluded(service, "environment", filterContainerEnvironment(config.getEnv()), excludeFields);

        // ports
        if (shouldIncludeField("ports", excludeFields)) {
            List<String> ports = new ArrayList<>();
            if (hostConfig != null && hostConfig.getPortBindings() != null) {
                hostConfig.getPortBindings().getBindings().forEach((key, bindings) -> {
                    if (bindings != null) {
                        for (Ports.Binding binding : bindings) {
                            if (binding != null && binding.getHostPortSpec() != null) {
                                ports.add(binding.getHostPortSpec() + ":" + key.getPort());
                            }
                        }
                    }
                });
            }
            if (!ports.isEmpty()) service.put("ports", ports);
        }

        // volumes
        if (shouldIncludeField("volumes", excludeFields)) {
            List<String> volumes = new ArrayList<>();
            if (container.getMounts() != null) {
                for (InspectContainerResponse.Mount mount : container.getMounts()) {
                    if (mount.getSource() != null && mount.getDestination() != null) {
                        volumes.add(mount.getSource() + ":" + mount.getDestination().getPath());
                    }
                }
            }
            if (!volumes.isEmpty()) service.put("volumes", volumes);
        }

        // networks - 🎯 简化网络处理逻辑
        if (shouldIncludeField("networks", excludeFields) && container.getNetworkSettings() != null) {
            Map<String, ContainerNetwork> networksMap = container.getNetworkSettings().getNetworks();
            if (networksMap != null && !networksMap.isEmpty()) {
                // 🔍 检查是否有host网络模式
                boolean hasHostNetwork = false;
                for (Map.Entry<String, ContainerNetwork> entry : networksMap.entrySet()) {
                    String networkName = entry.getKey();
                    if ("host".equals(networkName)) {
                        hasHostNetwork = true;
                        break;
                    }
                }
                
                // ✅ 只有host网络才设置network_mode，其他网络都使用默认桥接
                if (hasHostNetwork) {
                    service.put("network_mode", "host");
                }
                // 🚫 不再设置networks配置，让Docker Compose使用默认桥接网络
                // 这样可以避免自定义网络不存在的问题，确保导入后能正常启动
            }
        }

        // restart
        if (shouldIncludeField("restart", excludeFields) && hostConfig != null && hostConfig.getRestartPolicy() != null) {
            RestartPolicy restartPolicy = hostConfig.getRestartPolicy();
            if (restartPolicy.getName() != null) {
                service.put("restart", restartPolicy.getName());
            }
        }

        // cap_add
        if (shouldIncludeField("cap_add", excludeFields) && hostConfig != null && hostConfig.getCapAdd() != null) {
            service.put("cap_add", hostConfig.getCapAdd());
        }

        // devices
        if (shouldIncludeField("devices", excludeFields) && hostConfig != null && hostConfig.getDevices() != null) {
            List<String> devices = Arrays.stream(hostConfig.getDevices()).map(d -> d.getPathOnHost() + ":" + d.getPathInContainer()).collect(Collectors.toList());
            if (!devices.isEmpty()) {
                service.put("devices", devices);
            }
        }

        return service;
    }

    /**
     * 将字符串数组转换为列表
     *
     * @param arr 字符串数组
     * @return 字符串列表
     */
    List<String> arrayToList(String[] arr) {
        return arr == null ? null : Arrays.asList(arr);
    }

    /**
     * 根据容器ID列表生成Compose配置文件，使用自定义项目信息
     *
     * @param containerIds 容器ID列表
     * @param projectName 项目名称
     * @param projectDescription 项目描述
     * @return 生成的YAML内容
     */
    public String generateFromContainerIds(List<String> containerIds, String projectName, String projectDescription) {
        try {
            // 获取容器详细信息
            List<InspectContainerResponse> containers = dockerService.listContainers().stream()
                    .filter(container -> containerIds.contains(container.getId()))
                    .map(container -> dockerService.inspectContainerCmd(container.getId()))
                    .collect(Collectors.toList());

            return generateComposeContent(containers, new HashSet<>(), projectName, projectDescription);
        } catch (Exception e) {
            throw new RuntimeException("生成YAML配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据容器ID列表生成Compose配置文件（使用默认项目信息）
     *
     * @param containerIds 容器ID列表  
     * @return 生成的YAML内容
     */
    public String generateFromContainerIds(List<String> containerIds) {
        return generateFromContainerIds(containerIds, "Docker容器项目", "Docker容器管理项目");
    }

    /**
     * 🔥 新增：过滤容器环境变量，移除Docker Compose级别的变量
     */
    private List<String> filterContainerEnvironment(String[] envArray) {
        if (envArray == null) {
            return null;
        }
        
        List<String> filteredEnv = new ArrayList<>();
        for (String env : envArray) {
            // 🚫 过滤掉Docker Compose级别的环境变量
            if (shouldExcludeFromContainerEnv(env)) {
                continue;
            }
            filteredEnv.add(env);
        }
        
        return filteredEnv.isEmpty() ? null : filteredEnv;
    }
    
    /**
     * 🔥 新增：判断环境变量是否应该从容器环境中排除
     */
    private boolean shouldExcludeFromContainerEnv(String env) {
        if (env == null || env.trim().isEmpty()) {
            return true;
        }
        
        String[] parts = env.split("=", 2);
        if (parts.length < 1) {
            return true;
        }
        
        String varName = parts[0].trim();
        
        // 🚫 排除BASE类型的变量
        if (varName.startsWith("BASE_")) {
            return true;
        }
        
        // 🚫 排除端口类型的变量（格式：*_PORT_*）
        if (varName.contains("_PORT_")) {
            return true;
        }
        
        // 🚫 排除Docker基础目录变量
        if ("DOCKER_BASE".equals(varName)) {
            return true;
        }
        
        return false;
    }

    /**
     * 🔥 新增：基于容器路径语义的智能分组
     */
    private Map<String, String> analyzePathsByContainerSemantics(List<InspectContainerResponse> containers) {
        // 按容器路径语义分组收集宿主机路径
        List<String> mediaHosts = new ArrayList<>();
        List<String> downloadHosts = new ArrayList<>();
        List<String> documentHosts = new ArrayList<>();
        List<String> otherHosts = new ArrayList<>();
        
        for (InspectContainerResponse container : containers) {
            if (container.getMounts() == null) continue;
            
            for (InspectContainerResponse.Mount mount : container.getMounts()) {
                // 🔥 增强空值检查
                if (mount.getSource() == null || mount.getDestination() == null) {
                    continue;
                }
                
                String hostPath = mount.getSource();
                String containerPath = mount.getDestination().getPath();
                
                // 🔥 检查路径是否有效
                if (hostPath.trim().isEmpty() || containerPath.trim().isEmpty()) {
                    continue;
                }
                
                // 跳过系统路径和Docker专用路径
                if (isSystemPath(hostPath) || isDockerSpecific(containerPath)) {
                    continue;
                }
                
                // 🎯 按容器路径语义分类
                if (isMediaPath(containerPath)) {
                    mediaHosts.add(hostPath);
                } else if (isDownloadPath(containerPath)) {
                    downloadHosts.add(hostPath);
                } else if (isDocumentPath(containerPath)) {
                    documentHosts.add(hostPath);
                } else {
                    otherHosts.add(hostPath);
                }
            }
        }
        
        // 🔥 生成语义化环境变量映射
        Map<String, String> pathToEnv = new HashMap<>();
        Map<String, String> baseEnvs = new HashMap<>();
        
        // 处理媒体路径
        if (!mediaHosts.isEmpty()) {
            String mediaBase = findCommonBase(mediaHosts);
            baseEnvs.put("MEDIA_BASE", mediaBase);
            for (String hostPath : mediaHosts) {
                String relative = getRelativePath(hostPath, mediaBase);
                pathToEnv.put(hostPath, relative.isEmpty() ? "${MEDIA_BASE}" : "${MEDIA_BASE}/" + relative);
            }
        }
        
        // 处理下载路径
        if (!downloadHosts.isEmpty()) {
            String downloadBase = findCommonBase(downloadHosts);
            baseEnvs.put("DOWNLOAD_BASE", downloadBase);
            for (String hostPath : downloadHosts) {
                String relative = getRelativePath(hostPath, downloadBase);
                pathToEnv.put(hostPath, relative.isEmpty() ? "${DOWNLOAD_BASE}" : "${DOWNLOAD_BASE}/" + relative);
            }
        }
        
        // 处理文档路径
        if (!documentHosts.isEmpty()) {
            String documentBase = findCommonBase(documentHosts);
            baseEnvs.put("DOCUMENT_BASE", documentBase);
            for (String hostPath : documentHosts) {
                String relative = getRelativePath(hostPath, documentBase);
                pathToEnv.put(hostPath, relative.isEmpty() ? "${DOCUMENT_BASE}" : "${DOCUMENT_BASE}/" + relative);
            }
        }
        
        // 处理其他路径（使用原有的智能分组）
        if (!otherHosts.isEmpty()) {
            Map<String, List<String>> otherGroups = analyzePathGroups(new HashSet<>(otherHosts));
            int baseCount = 1;
            for (Map.Entry<String, List<String>> entry : otherGroups.entrySet()) {
                String basePath = entry.getKey();
                List<String> pathsInGroup = entry.getValue();
                
                String envName = "BASE_" + baseCount++;
                baseEnvs.put(envName, basePath);
                
                for (String path : pathsInGroup) {
                    String relative = getRelativePath(path, basePath);
                    pathToEnv.put(path, relative.isEmpty() ? "${" + envName + "}" : "${" + envName + "}/" + relative);
                }
            }
        }
        
        // 将baseEnvs信息存储，供后续使用
        this.semanticBaseEnvs = baseEnvs;
        
        return pathToEnv;
    }
    
    /**
     * 🔥 新增：判断是否为媒体路径
     */
    private boolean isMediaPath(String containerPath) {
        return containerPath.contains("/media") || 
               containerPath.contains("/movies") || 
               containerPath.contains("/tv") ||
               containerPath.contains("/music") ||
               containerPath.contains("/video") ||
               containerPath.contains("/photos") ||
               containerPath.contains("/pictures");
    }
    
    /**
     * 🔥 新增：判断是否为下载路径
     */
    private boolean isDownloadPath(String containerPath) {
        return containerPath.contains("/downloads") || 
               containerPath.contains("/download") ||
               containerPath.contains("/torrents");
    }
    
    /**
     * 🔥 新增：判断是否为文档路径
     */
    private boolean isDocumentPath(String containerPath) {
        return containerPath.contains("/documents") || 
               containerPath.contains("/books") ||
               containerPath.contains("/ebooks") ||
               containerPath.contains("/files");
    }
    
    /**
     * 🔥 新增：找到路径列表的公共基础路径
     */
    private String findCommonBase(List<String> paths) {
        if (paths.isEmpty()) {
            return "";
        }
        
        if (paths.size() == 1) {
            return findReasonableBase(paths.get(0));
        }
        
        // 找到所有路径的公共前缀
        String commonPrefix = paths.get(0);
        for (int i = 1; i < paths.size(); i++) {
            commonPrefix = getCommonPrefix(commonPrefix, paths.get(i));
        }
        
        // 确保公共前缀以完整的目录结束
        if (commonPrefix.isEmpty() || commonPrefix.equals("/")) {
            // 如果没有公共前缀，使用第一个路径的合理基础
            return findReasonableBase(paths.get(0));
        }
        
        // 确保不以/结尾，除非是根目录
        if (commonPrefix.endsWith("/") && !commonPrefix.equals("/")) {
            commonPrefix = commonPrefix.substring(0, commonPrefix.length() - 1);
        }
        
        return commonPrefix;
    }
    
    /**
     * 🔥 新增：获取两个路径的公共前缀
     */
    private String getCommonPrefix(String path1, String path2) {
        int minLength = Math.min(path1.length(), path2.length());
        int commonLength = 0;
        
        for (int i = 0; i < minLength; i++) {
            if (path1.charAt(i) == path2.charAt(i)) {
                commonLength = i + 1;
            } else {
                break;
            }
        }
        
        if (commonLength == 0) {
            return "";
        }
        
        String common = path1.substring(0, commonLength);
        
        // 确保公共前缀在路径分隔符处结束
        int lastSlash = common.lastIndexOf('/');
        if (lastSlash > 0) {
            return common.substring(0, lastSlash);
        } else if (lastSlash == 0) {
            return "/";
        }
        
        return common;
    }
    
    /**
     * 🔥 新增：获取相对路径
     */
    private String getRelativePath(String fullPath, String basePath) {
        if (!fullPath.startsWith(basePath)) {
            return "";
        }
        
        String relative = fullPath.substring(basePath.length());
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        
        return relative;
    }
    
    /**
     * 🔥 新增：获取环境变量描述
     */
    private String getEnvDescription(String envKey) {
        if (envKey == null) return "";
        
        if (envKey.equals("DOCKER_BASE")) {
            return "Docker容器基础目录";
        } else if (envKey.equals("MEDIA_BASE")) {
            return "媒体文件存储目录";
        } else if (envKey.equals("DOWNLOAD_BASE")) {
            return "下载文件存储目录";
        } else if (envKey.equals("DOCUMENT_BASE")) {
            return "文档文件存储目录";
        } else if (envKey.startsWith("BASE_")) {
            return "通用存储目录";
        } else if (envKey.contains("_PORT_")) {
            return "服务端口映射";
        } else {
            return "配置目录";
        }
    }
    
    /**
     * 🔥 简化：智能分析路径分组
     */
    private Map<String, List<String>> analyzePathGroups(Set<String> allPaths) {
        Map<String, List<String>> groups = new HashMap<>();
        
        // 简单分组：按父目录分组
        Map<String, List<String>> parentGroups = new HashMap<>();
        for (String path : allPaths) {
            String parent = findReasonableBase(path);
            parentGroups.computeIfAbsent(parent, k -> new ArrayList<>()).add(path);
        }
        
        // 只保留有多个路径的分组
        for (Map.Entry<String, List<String>> entry : parentGroups.entrySet()) {
            if (entry.getValue().size() >= 1) { // 简化：保留所有分组
                groups.put(entry.getKey(), entry.getValue());
            }
        }
        
        return groups;
    }
    
    /**
     * 🔥 简化：找到合理的基础路径
     */
    private String findReasonableBase(String path) {
        String[] segments = path.split("/");
        
        // 简化逻辑：取前3-4层目录
        int targetDepth = Math.min(4, Math.max(2, segments.length - 1));
        
        if (segments.length > targetDepth) {
            StringBuilder base = new StringBuilder();
            for (int i = 1; i <= targetDepth; i++) {
                if (i < segments.length) {
                    base.append("/").append(segments[i]);
                }
            }
            return base.toString();
        }
        
        return path;
    }
    
    // 🔥 新增：存储语义化的基础环境变量
    private Map<String, String> semanticBaseEnvs = new HashMap<>();

    /**
     * 生成 Docker Compose YAML 内容（使用默认项目信息）
     *
     * @param containers    容器信息列表
     * @param excludeFields 需要排除的字段集合
     * @return YAML 格式的字符串
     */
    public String generateComposeContent(List<InspectContainerResponse> containers, Set<String> excludeFields) {
        return generateComposeContent(containers, excludeFields, "Docker容器项目", "Docker容器管理项目");
    }

    /**
     * 🔥 简化：拆分compose内容为单服务
     */
    public Map<String, String> splitComposeContent(String composeContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> originalCompose = yaml.load(composeContent);
        Map<String, String> result = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) originalCompose.get("services");
        if (services == null || services.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, Object> serviceEntry : services.entrySet()) {
            String serviceName = serviceEntry.getKey();
            
            Map<String, Object> singleServiceCompose = new LinkedHashMap<>(originalCompose);
            Map<String, Object> singleService = new LinkedHashMap<>();
            singleService.put(serviceName, serviceEntry.getValue());
            singleServiceCompose.put("services", singleService);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml singleYaml = new Yaml(options);
            result.put(serviceName, singleYaml.dump(singleServiceCompose));
        }

        return result;
    }

}