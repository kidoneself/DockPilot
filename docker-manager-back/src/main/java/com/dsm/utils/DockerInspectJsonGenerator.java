package com.dsm.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class DockerInspectJsonGenerator {
    /**
     * 根据容器信息生成JSON配置
     *
     * @param containerInfo 容器信息
     * @return 生成的JSON字符串
     * @throws RuntimeException 当生成配置失败时抛出异常
     */
    public static String generateJsonFromContainerInfo(InspectContainerResponse containerInfo) {
        if (containerInfo == null) {
            throw new IllegalArgumentException("容器信息不能为空");
        }

        try {
            // 创建应用配置对象
            JSONObject appConfig = new JSONObject(true);
            
            // 设置基本信息
            String containerName = Optional.ofNullable(containerInfo.getName())
                    .map(name -> name.replaceAll("/", ""))
                    .orElseThrow(() -> new RuntimeException("容器名称不能为空"));
            
            appConfig.put("name", containerName);
            appConfig.put("category", "媒体");
            appConfig.put("version", "1.0");
            appConfig.put("description", "一个强大的媒体管理应用");
            appConfig.put("iconUrl", "https://example.com/icon.jpg");

            // 创建服务配置
            JSONObject serviceNode = createServiceNode(containerName, containerInfo);
            JSONArray servicesNode = new JSONArray();
            servicesNode.add(serviceNode);
            appConfig.put("services", servicesNode);

            // 添加parameters
            appConfig.put("parameters", createParametersNode());

            // 添加configs
            appConfig.put("configs", createConfigsNode());

            return appConfig.toJSONString();
        } catch (Exception e) {
            log.error("生成容器JSON配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成容器JSON配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建服务节点
     */
    private static JSONObject createServiceNode(String containerName, InspectContainerResponse containerInfo) {
        JSONObject serviceNode = new JSONObject(true);
        serviceNode.put("id", containerName);
        serviceNode.put("name", containerName);
        serviceNode.put("template", createTemplateNode(containerName, containerInfo));
        return serviceNode;
    }

    /**
     * 创建模板节点
     */
    private static JSONObject createTemplateNode(String containerName, InspectContainerResponse containerInfo) {
        JSONObject templateNode = new JSONObject(true);
        templateNode.put("name", containerName);
        templateNode.put("image", Optional.ofNullable(containerInfo.getConfig())
                .map(config -> config.getImage())
                .orElseThrow(() -> new RuntimeException("容器镜像信息不能为空")));

        // 设置命令
        JSONArray cmdNode = new JSONArray();
        Optional.ofNullable(containerInfo.getConfig())
                .map(config -> config.getCmd())
                .ifPresent(cmd -> Arrays.stream(cmd).forEach(cmdNode::add));
        templateNode.put("cmd", cmdNode);

        // 设置环境变量
        templateNode.put("env", createEnvNode(containerInfo));

        // 设置端口映射
        templateNode.put("ports", createPortsNode(containerInfo));

        // 设置卷映射
        templateNode.put("volumes", createVolumesNode(containerInfo));

        // 设置重启策略
        templateNode.put("restartPolicy", Optional.ofNullable(containerInfo.getHostConfig())
                .map(hostConfig -> hostConfig.getRestartPolicy())
                .map(restartPolicy -> restartPolicy.getName())
                .orElse("no"));

        // 设置网络模式
        templateNode.put("networkMode", Optional.ofNullable(containerInfo.getHostConfig())
                .map(hostConfig -> hostConfig.getNetworkMode())
                .orElse("bridge"));

        // 设置特权模式
        templateNode.put("privileged", Optional.ofNullable(containerInfo.getHostConfig())
                .map(hostConfig -> hostConfig.getPrivileged())
                .orElse(false));

        return templateNode;
    }

    /**
     * 创建环境变量节点
     */
    private static JSONObject createEnvNode(InspectContainerResponse containerInfo) {
        JSONObject envNode = new JSONObject(true);
        Optional.ofNullable(containerInfo.getConfig())
                .map(config -> config.getEnv())
                .ifPresent(envs -> Arrays.stream(envs)
                        .map(env -> env.split("=", 2))
                        .filter(parts -> parts.length == 2)
                        .forEach(parts -> envNode.put(parts[0], parts[1])));
        return envNode;
    }

    /**
     * 创建端口映射节点
     */
    private static JSONObject createPortsNode(InspectContainerResponse containerInfo) {
        JSONObject portsNode = new JSONObject(true);
        Optional.ofNullable(containerInfo.getConfig())
                .map(config -> config.getExposedPorts())
                .ifPresent(ports -> Arrays.stream(ports)
                        .map(ExposedPort::toString)
                        .forEach(port -> portsNode.put(port, port)));
        return portsNode;
    }

    /**
     * 创建卷映射节点
     */
    private static JSONObject createVolumesNode(InspectContainerResponse containerInfo) {
        JSONObject volumesNode = new JSONObject(true);
        Optional.ofNullable(containerInfo.getMounts())
                .ifPresent(mounts -> mounts.stream()
                        .filter(mount -> mount.getSource() != null && mount.getDestination() != null)
                        .forEach(mount -> volumesNode.put(
                                mount.getSource(),
                                mount.getDestination().getPath()
                        )));
        return volumesNode;
    }

    /**
     * 创建参数节点
     */
    private static JSONArray createParametersNode() {
        JSONArray parametersNode = new JSONArray();
        JSONObject param1 = new JSONObject(true);
        param1.put("key", "DOCKER_PATH");
        param1.put("name", "Docker配置路径");
        param1.put("value", "/volume1/docker");
        parametersNode.add(param1);

        JSONObject param2 = new JSONObject(true);
        param2.put("key", "MEDIA_PATH");
        param2.put("name", "媒体文件路径");
        param2.put("value", "/volume2/media");
        parametersNode.add(param2);

        return parametersNode;
    }

    /**
     * 创建配置节点
     */
    private static JSONArray createConfigsNode() {
        JSONArray configsNode = new JSONArray();
        JSONObject config = new JSONObject(true);
        config.put("target", "{{DOCKER_PATH}}");
        JSONArray urlsNode = new JSONArray();
        urlsNode.add("https://example.com/config1.tgz");
        urlsNode.add("https://example.com/config2.tgz");
        config.put("urls", urlsNode);
        configsNode.add(config);
        return configsNode;
    }
} 