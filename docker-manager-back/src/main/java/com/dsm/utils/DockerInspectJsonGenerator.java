package com.dsm.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;

import java.util.Iterator;
import java.util.Map;

public class DockerInspectJsonGenerator {
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);

    /**
     * 根据容器ID生成JSON配置
     *
     * @param containerId 容器ID
     * @return 生成的JSON字符串
     */
    public static String generateJsonFromContainerId(String containerId) {
        try {
            DockerClient dockerClient = DockerClientBuilder.getInstance().build();
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            
            // 创建应用配置对象
            JSONObject appConfig = new JSONObject(true); // 使用LinkedHashMap保持顺序
            
            // 设置基本信息
            String containerName = containerInfo.getName().replaceAll("/", "");
            appConfig.put("name", containerName);
            appConfig.put("category", "媒体");
            appConfig.put("version", "1.0");
            appConfig.put("description", "一个强大的媒体管理应用");
            appConfig.put("iconUrl", "https://example.com/icon.jpg");
            
            // 创建服务数组
            JSONObject serviceNode = new JSONObject(true);
            serviceNode.put("id", containerName);
            serviceNode.put("name", containerName);
            
            // 创建模板对象
            JSONObject templateNode = new JSONObject(true);
            templateNode.put("name", containerName);
            templateNode.put("image", containerInfo.getConfig().getImage());
            
            // 设置命令
            JSONArray cmdNode = new JSONArray();
            if (containerInfo.getConfig().getCmd() != null) {
                for (String cmd : containerInfo.getConfig().getCmd()) {
                    cmdNode.add(cmd);
                }
            }
            templateNode.put("cmd", cmdNode);
            
            // 设置环境变量
            JSONObject envNode = new JSONObject(true);
            if (containerInfo.getConfig().getEnv() != null) {
                for (String env : containerInfo.getConfig().getEnv()) {
                    String[] parts = env.split("=", 2);
                    if (parts.length == 2) {
                        envNode.put(parts[0], parts[1]);
                    }
                }
            }
            templateNode.put("env", envNode);
            
            // 设置端口映射
            JSONObject portsNode = new JSONObject(true);
            if (containerInfo.getConfig().getExposedPorts() != null) {
                for (ExposedPort port : containerInfo.getConfig().getExposedPorts()) {
                    String containerPort = port.toString();
                    String hostPort = containerPort.replaceAll("/tcp", "");
                    portsNode.put(containerPort, hostPort);
                }
            }
            templateNode.put("ports", portsNode);
            
            // 设置卷映射
            JSONObject volumesNode = new JSONObject(true);
            if (containerInfo.getMounts() != null) {
                for (InspectContainerResponse.Mount mount : containerInfo.getMounts()) {
                    String source = mount.getSource() != null ? mount.getSource() : "";
                    String destination = mount.getDestination() != null ? mount.getDestination().getPath() : "";
                    if (!source.isEmpty() && !destination.isEmpty()) {
                        volumesNode.put(source, destination);
                    }
                }
            }
            templateNode.put("volumes", volumesNode);
            
            // 设置重启策略
            templateNode.put("restartPolicy", containerInfo.getHostConfig().getRestartPolicy().getName());
            
            // 将模板添加到服务中
            serviceNode.put("template", templateNode);
            
            // 创建服务数组
            JSONArray servicesNode = new JSONArray();
            servicesNode.add(serviceNode);
            appConfig.put("services", servicesNode);
            
            // 添加parameters
            JSONArray parametersNode = new JSONArray();
            JSONObject param1 = new JSONObject(true);
            param1.put("key", "MEDIA_PATH");
            param1.put("value", "/volume2/media");
            param1.put("name", "媒体文件路径");
            parametersNode.add(param1);
            appConfig.put("parameters", parametersNode);

            // 添加configs
            JSONArray configsNode = new JSONArray();
            JSONObject config = new JSONObject(true);
            config.put("target", "{{DOCKER_PATH}}");
            JSONArray urlsNode = new JSONArray();
            urlsNode.add("https://example.com/config1.tgz");
            config.put("urls", urlsNode);
            configsNode.add(config);
            appConfig.put("configs", configsNode);
            
            return appConfig.toJSONString();
        } catch (Exception e) {
            throw new RuntimeException("生成容器JSON配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将Docker inspect的JSON转换为简化版的JSON
     *
     * @param dockerInspectJson Docker inspect的原始JSON
     * @return 简化后的JSON字符串
     */
    private static String convertToJson(String dockerInspectJson) throws Exception {
        JsonNode containerNode = objectMapper.readTree(dockerInspectJson);

        // 创建简化版的 JSON
        JsonNode nameNode = containerNode.get("Name");
        JsonNode imageNode = containerNode.get("Config").get("Image");
        JsonNode envNode = containerNode.get("Config").get("Env");
        JsonNode portsNode = containerNode.get("Config").get("ExposedPorts");
        JsonNode volumesNode = containerNode.get("Mounts");
        JsonNode restartPolicyNode = containerNode.get("HostConfig").get("RestartPolicy");
        JsonNode cmdNode = containerNode.get("Config").get("Cmd");

        // 构建简化 JSON
        String simplifiedJson = "{";

        // 容器名称
        simplifiedJson += "\"name\":\"" + nameNode.asText().replaceAll("/", "") + "\", ";

        // 镜像
        simplifiedJson += "\"image\":\"" + imageNode.asText() + "\", ";

        // 命令
        simplifiedJson += "\"cmd\":[";
        if (cmdNode != null && cmdNode.isArray()) {
            Iterator<JsonNode> cmdIter = cmdNode.elements();
            while (cmdIter.hasNext()) {
                JsonNode cmd = cmdIter.next();
                simplifiedJson += "\"" + cmd.asText() + "\", ";
            }
            if (!cmdNode.isEmpty()) {
                simplifiedJson = simplifiedJson.substring(0, simplifiedJson.length() - 2);
            }
        }
        simplifiedJson += "], ";

        // 环境变量
        simplifiedJson += "\"env\":{";
        if (envNode != null) {
            Iterator<JsonNode> envIter = envNode.elements();
            while (envIter.hasNext()) {
                JsonNode envVar = envIter.next();
                String[] env = envVar.asText().split("=");
                if (env.length == 2) {
                    simplifiedJson += "\"" + env[0] + "\":\"" + env[1] + "\", ";
                }
            }
            if (!envNode.isEmpty()) {
                simplifiedJson = simplifiedJson.substring(0, simplifiedJson.length() - 2);
            }
        }
        simplifiedJson += "}, ";

        // 端口映射
        simplifiedJson += "\"ports\":{";
        if (portsNode != null) {
            Iterator<Map.Entry<String, JsonNode>> portsIter = portsNode.fields();
            while (portsIter.hasNext()) {
                Map.Entry<String, JsonNode> entry = portsIter.next();
                String containerPort = entry.getKey();
                String hostPort = containerPort.replaceAll("/tcp", "");
                simplifiedJson += "\"" + containerPort + "\":\"" + hostPort + "\", ";
            }
            if (!portsNode.isEmpty()) {
                simplifiedJson = simplifiedJson.substring(0, simplifiedJson.length() - 2);
            }
        }
        simplifiedJson += "}, ";

        // 挂载卷
        simplifiedJson += "\"volumes\":{";
        if (volumesNode != null && !volumesNode.isEmpty()) {
            Iterator<JsonNode> volumeIter = volumesNode.elements();
            while (volumeIter.hasNext()) {
                JsonNode volume = volumeIter.next();
                String hostPath = volume.get("Source").asText();
                String containerPath = volume.get("Destination").asText();
                simplifiedJson += "\"" + hostPath + "\":\"" + containerPath + "\", ";
            }
            if (!volumesNode.isEmpty()) {
                simplifiedJson = simplifiedJson.substring(0, simplifiedJson.length() - 2);
            }
        }
        simplifiedJson += "}, ";

        // 重启策略
        simplifiedJson += "\"restartPolicy\":\"" + restartPolicyNode.get("Name").asText() + "\"";

        simplifiedJson += "}";

        return simplifiedJson;
    }
} 