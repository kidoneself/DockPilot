package com.dsm.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ComposeGenerator {

    public void generateComposeFile(List<InspectContainerResponse> containers, String outputPath, String templateFile, Set<String> excludeFields) throws IOException {
        Map<String, Object> compose = new LinkedHashMap<>();
        Map<String, Object> services = new LinkedHashMap<>();
        Map<String, Object> networks = new LinkedHashMap<>();
        Map<String, Object> defaultNetwork = new LinkedHashMap<>();
        defaultNetwork.put("external", true);
        defaultNetwork.put("name", "bridge");
        networks.put("default", defaultNetwork);

        for (InspectContainerResponse container : containers) {
            String serviceName = getServiceName(container);
            Map<String, Object> service = convertContainerToService(container, excludeFields);
            service.put("container_name", serviceName);

            // 添加 x-meta 配置
            Map<String, Object> xMeta = new LinkedHashMap<>();
            xMeta.put("config_url", "https://example.com/configs/" + serviceName + ".tgz");
            xMeta.put("priority", 1);
            xMeta.put("group", "video");
            service.put("x-meta", xMeta);

            services.put(serviceName, service);
        }

        compose.put("services", services);
        compose.put("networks", networks);

        // 配置 YAML 输出选项
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);  // 使用块样式
        options.setPrettyFlow(true);
        options.setIndent(2);  // 设置缩进
        options.setIndicatorIndent(2);  // 设置指示符缩进为2
        options.setWidth(120);  // 设置行宽
        options.setLineBreak(DumperOptions.LineBreak.UNIX);  // 设置换行符
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);  // 使用普通标量样式
        options.setIndentWithIndicator(true);  // 使用指示符缩进
        options.setNonPrintableStyle(DumperOptions.NonPrintableStyle.BINARY);  // 处理非打印字符

        // 写入 YAML 文件
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(outputPath)) {
            yaml.dump(compose, writer);
        }
    }

    public void generateComposeFileFromJson(String jsonPath, String outputPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<InspectContainerResponse> containers = mapper.readValue(new File(jsonPath), new TypeReference<List<InspectContainerResponse>>() {
        });
        generateComposeFile(containers, outputPath, null, null);
    }

    String getServiceName(InspectContainerResponse container) {
        String name = container.getName();
        if (name != null && !name.isEmpty()) {
            return name.startsWith("/") ? name.substring(1) : name;
        }
        return "container_" + container.getId().substring(0, 8) + "_" + System.currentTimeMillis();
    }

    private boolean shouldIncludeField(String fieldName, Set<String> excludeFields) {
        return excludeFields == null || !excludeFields.contains(fieldName);
    }

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
                List<String> networks = new ArrayList<>(networksMap.keySet());
                service.put("networks", networks);
            }
        }

        // restart
        if (shouldIncludeField("restart", excludeFields) && hostConfig != null && hostConfig.getRestartPolicy() != null) {
            RestartPolicy restartPolicy = hostConfig.getRestartPolicy();
            if (restartPolicy.getName() != null) {
                service.put("restart", restartPolicy.getName());
            }
        }

        // labels
        // if (shouldIncludeField("labels", excludeFields) && config.getLabels() != null && !config.getLabels().isEmpty()) {
        //     service.put("labels", config.getLabels());
        // }

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

        // healthcheck
        // if (shouldIncludeField("healthcheck", excludeFields) && config.getHealthcheck() != null) {
        //     HealthCheck hc = config.getHealthcheck();
        //     Map<String, Object> healthMap = new LinkedHashMap<>();
        //     if (hc.getTest() != null) healthMap.put("test", Arrays.asList(hc.getTest()));
        //     if (hc.getInterval() != null) healthMap.put("interval", hc.getInterval().toString());
        //     if (hc.getTimeout() != null) healthMap.put("timeout", hc.getTimeout().toString());
        //     if (hc.getStartPeriod() != null) healthMap.put("start_period", hc.getStartPeriod().toString());
        //     healthMap.put("retries", hc.getRetries());
        //     service.put("healthcheck", healthMap);
        // }

        // depends_on（从 label 中读取）
        // if (shouldIncludeField("depends_on", excludeFields)) {
        //     String depends = config.getLabels() != null ? config.getLabels().get("depends_on") : null;
        //     if (depends != null && !depends.isBlank()) {
        //         service.put("depends_on", Arrays.asList(depends.split(",")));
        //     }
        // }

        return service;
    }


    List<String> arrayToList(String[] arr) {
        return arr == null ? null : Arrays.asList(arr);
    }
} 