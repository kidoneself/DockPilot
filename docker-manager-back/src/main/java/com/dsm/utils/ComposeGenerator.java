package com.dsm.utils;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Docker Compose 生成器
 * 用于将 Docker 容器配置转换为 Docker Compose 格式
 * 支持多容器配置、端口映射、网络设置、环境变量等功能
 */
public class ComposeGenerator {

    /**
     * 生成 Docker Compose 文件
     * @param containers 容器信息列表
     * @param outputPath 输出文件路径
     * @param templateFile 模板文件路径（可选）
     * @param excludeFields 需要排除的字段集合
     * @throws IOException 文件操作异常
     */
    public void generateComposeFile(List<InspectContainerResponse> containers, String outputPath, String templateFile, Set<String> excludeFields) throws IOException {
        // 初始化 Compose 配置结构
        Map<String, Object> compose = new LinkedHashMap<>();
        Map<String, Object> services = new LinkedHashMap<>();
        Map<String, Object> networks = new LinkedHashMap<>();
        
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

        // 处理路径映射
        Map<String, String> baseEnv = new LinkedHashMap<>();
        Map<String, String> pathToEnv = new LinkedHashMap<>();
        int baseCount = 1;

        for (String path : allPaths) {
            boolean matched = false;
            for (Map.Entry<String, String> entry : baseEnv.entrySet()) {
                String base = entry.getValue();
                if (path.equals(base) || path.startsWith(base + "/")) {
                    String envName = entry.getKey();
                    String relative = path.substring(base.length());
                    if (relative.startsWith("/")) {
                        relative = relative.substring(1);
                    }
                    // 如果相对路径为空，直接使用环境变量，不添加斜杠
                    pathToEnv.put(path, relative.isEmpty() ? "${" + envName + "}" : "${" + envName + "}/" + relative);
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                String base;
                if (path.equals("/")) {
                    base = "/";
                } else {
                    String[] segments = path.split("/");
                    int endIndex = Math.min(4, segments.length);
                    if (endIndex <= 1) {
                        endIndex = 2;
                    }
                    base = "/" + String.join("/", Arrays.asList(segments).subList(1, endIndex));
                }
                String envName = "BASE_" + baseCount++;
                baseEnv.put(envName, base);

                String relative = path.substring(base.length());
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
        
        // 配置环境变量
        Map<String, String> envVars = new LinkedHashMap<>();
        envVars.putAll(portMappings);
        envVars.putAll(baseEnv);  // 添加路径变量
        projectMeta.put("env", envVars);

        // 组装最终的 Compose 配置
        compose.put("x-meta", projectMeta);
        compose.put("services", services);
        
        // 只有当有网络配置时才添加 networks 部分
        if (!networks.isEmpty()) {
            compose.put("networks", networks);
        }

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
     * 生成 Docker Compose YAML 内容
     * @param containers 容器信息列表
     * @param excludeFields 需要排除的字段集合
     * @return YAML 格式的字符串
     */
    public String generateComposeContent(List<InspectContainerResponse> containers, Set<String> excludeFields) {
        Map<String, Object> compose = new LinkedHashMap<>();
        Map<String, Object> services = new LinkedHashMap<>();
        Map<String, Object> networks = new LinkedHashMap<>();
        Map<String, Object> defaultNetwork = new LinkedHashMap<>();
        defaultNetwork.put("external", true);
        defaultNetwork.put("name", "bridge");
        networks.put("default", defaultNetwork);

        // 收集所有容器的端口映射
        Map<String, String> portMappings = new LinkedHashMap<>();

        for (InspectContainerResponse container : containers) {
            String serviceName = getServiceName(container);
            Map<String, Object> service = convertContainerToService(container, excludeFields);
            service.put("container_name", serviceName);

            // 收集端口映射并替换为环境变量引用
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
                        // 替换为环境变量引用
                        newPorts.add("${" + portKey + "}:" + containerPort);
                    }
                }
                service.put("ports", newPorts);
            }

            // 添加服务级元数据配置
            Map<String, Object> serviceMeta = new LinkedHashMap<>();
            serviceMeta.put("name", serviceName);
            serviceMeta.put("description", "容器服务");
            serviceMeta.put("configUrl", "");  // 直接的一级配置
            service.put("x-meta", serviceMeta);

            services.put(serviceName, service);
        }

        // 添加项目级元数据配置
        Map<String, Object> projectMeta = new LinkedHashMap<>();
        projectMeta.put("name", "Docker容器项目");
        projectMeta.put("description", "Docker容器管理项目");
        projectMeta.put("version", "1.0.0");
        projectMeta.put("author", "System");
        projectMeta.put("category", "container");  // 添加到顶级配置
        
        // 配置环境变量
        Map<String, String> envVars = new LinkedHashMap<>();
        envVars.putAll(portMappings);
        projectMeta.put("env", envVars);

        compose.put("x-meta", projectMeta);
        compose.put("services", services);
        
        // 只有当有网络配置时才添加 networks 部分
        if (!networks.isEmpty()) {
            compose.put("networks", networks);
        }

        // 配置 YAML 输出选项
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(2);
        options.setWidth(120);
        options.setLineBreak(DumperOptions.LineBreak.UNIX);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setIndentWithIndicator(true);
        options.setNonPrintableStyle(DumperOptions.NonPrintableStyle.BINARY);

        // 生成 YAML 字符串
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
        
        return formattedContent.toString();
    }

    /**
     * 将多服务的 compose 内容拆分成多个单服务的 compose 内容
     * @param composeContent 原始的 compose 内容
     * @return 拆分后的 compose 内容列表，key 为服务名，value 为对应的 compose 内容
     */
    public Map<String, String> splitComposeContent(String composeContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> originalCompose = yaml.load(composeContent);
        Map<String, String> result = new LinkedHashMap<>();

        // 获取所有服务
        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) originalCompose.get("services");
        if (services == null || services.isEmpty()) {
            return result;
        }

        // 获取原始环境变量
        @SuppressWarnings("unchecked")
        Map<String, Object> xMeta = (Map<String, Object>) originalCompose.get("x-meta");
        @SuppressWarnings("unchecked")
        Map<String, String> originalEnv = xMeta != null ? (Map<String, String>) xMeta.get("env") : new LinkedHashMap<>();

        // 为每个服务创建独立的 compose 内容
        for (Map.Entry<String, Object> serviceEntry : services.entrySet()) {
            String serviceName = serviceEntry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> serviceConfig = (Map<String, Object>) serviceEntry.getValue();

            // 创建新的 compose 配置
            Map<String, Object> newCompose = new LinkedHashMap<>(originalCompose);
            Map<String, Object> newServices = new LinkedHashMap<>();
            newServices.put(serviceName, serviceConfig);
            newCompose.put("services", newServices);

            // 收集服务使用的环境变量
            Set<String> usedEnvVars = new HashSet<>();
            
            // 检查端口映射
            if (serviceConfig.containsKey("ports")) {
                @SuppressWarnings("unchecked")
                List<String> ports = (List<String>) serviceConfig.get("ports");
                for (String port : ports) {
                    if (port.startsWith("${") && port.contains("}")) {
                        String envVar = port.substring(2, port.indexOf("}"));
                        usedEnvVars.add(envVar);
                    }
                }
            }

            // 检查卷挂载
            if (serviceConfig.containsKey("volumes")) {
                @SuppressWarnings("unchecked")
                List<String> volumes = (List<String>) serviceConfig.get("volumes");
                for (String volume : volumes) {
                    if (volume.startsWith("${") && volume.contains("}")) {
                        String envVar = volume.substring(2, volume.indexOf("}"));
                        usedEnvVars.add(envVar);
                    }
                }
            }

            // 过滤环境变量
            Map<String, String> filteredEnv = new LinkedHashMap<>();
            for (Map.Entry<String, String> envEntry : originalEnv.entrySet()) {
                if (usedEnvVars.contains(envEntry.getKey())) {
                    filteredEnv.put(envEntry.getKey(), envEntry.getValue());
                }
            }

            // 更新 x-meta 中的环境变量
            @SuppressWarnings("unchecked")
            Map<String, Object> newXMeta = (Map<String, Object>) newCompose.get("x-meta");
            if (newXMeta != null) {
                newXMeta.put("env", filteredEnv);
            }

            // 配置 YAML 输出选项
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            options.setIndent(2);
            options.setIndicatorIndent(2);
            options.setWidth(120);
            options.setLineBreak(DumperOptions.LineBreak.UNIX);
            options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
            options.setIndentWithIndicator(true);
            options.setNonPrintableStyle(DumperOptions.NonPrintableStyle.BINARY);

            // 生成 YAML 字符串
            Yaml newYaml = new Yaml(options);
            result.put(serviceName, newYaml.dump(newCompose));
        }

        return result;
    }

    /**
     * 获取服务名称
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
     * @param fieldName 字段名
     * @param excludeFields 排除字段集合
     * @return 是否应该包含该字段
     */
    private boolean shouldIncludeField(String fieldName, Set<String> excludeFields) {
        return excludeFields == null || !excludeFields.contains(fieldName);
    }

    /**
     * 如果字段未被排除，则添加到映射中
     * @param map 目标映射
     * @param key 键
     * @param value 值
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
     * @param container 容器信息
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
        // env
        addFieldIfNotExcluded(service, "environment", arrayToList(config.getEnv()), excludeFields);

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
                        volumes.add(mount.getSource() + ":" + mount.getDestination());
                    }
                }
            }
            if (!volumes.isEmpty()) service.put("volumes", volumes);
        }

        // networks
        if (shouldIncludeField("networks", excludeFields) && container.getNetworkSettings() != null) {
            Map<String, ContainerNetwork> networksMap = container.getNetworkSettings().getNetworks();
            if (networksMap != null && !networksMap.isEmpty()) {
                List<String> networks = new ArrayList<>();
                for (Map.Entry<String, ContainerNetwork> entry : networksMap.entrySet()) {
                    String networkName = entry.getKey();
                    // 如果是 host 网络，使用 network_mode: host
                    if ("host".equals(networkName)) {
                        service.put("network_mode", "host");
                    } else {
                        // 对于其他网络（包括桥接网络），添加到 networks 列表
                        networks.add(networkName);
                    }
                }
                if (!networks.isEmpty()) {
                    service.put("networks", networks);
                }
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
     * @param arr 字符串数组
     * @return 字符串列表
     */
    List<String> arrayToList(String[] arr) {
        return arr == null ? null : Arrays.asList(arr);
    }
} 