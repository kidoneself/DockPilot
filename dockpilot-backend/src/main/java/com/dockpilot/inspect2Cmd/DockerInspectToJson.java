package com.dockpilot.inspect2Cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;

public class DockerInspectToJson {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            // 获取当前类文件所在的目录
            String currentDir = "/Users/lizhiqiang/coding-my/docker/dockpilot-backend/src/main/java/com/dockpilot/inspect2Cmd";

            // 只使用一个参数，同时作为文件夹名和模板名
            String name = "mp";
            String folderPath = currentDir + File.separator + name;
            System.out.println("正在处理文件夹: " + folderPath);

            generateTemplateFromFolder(folderPath, name);
        } catch (Exception e) {
            System.err.println("生成模板失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String convertToJson(String dockerInspectJson) throws Exception {
        JsonNode rootNode = objectMapper.readTree(dockerInspectJson);
        JsonNode containerNode = rootNode.get(0); // 只处理第一个容器

        JsonNode nameNode = containerNode.get("Name");
        JsonNode imageNode = containerNode.get("Config").get("Image");
        JsonNode envNode = containerNode.get("Config").get("Env");
        JsonNode portsNode = containerNode.get("Config").get("ExposedPorts");
        JsonNode mountsNode = containerNode.get("Mounts");
        JsonNode restartPolicyNode = containerNode.get("HostConfig").get("RestartPolicy");
        JsonNode cmdNode = containerNode.get("Config").get("Cmd");

        // 额外补充字段
        JsonNode portBindingsNode = containerNode.get("HostConfig").get("PortBindings");
        JsonNode networkModeNode = containerNode.get("HostConfig").get("NetworkMode");
        JsonNode privilegedNode = containerNode.get("HostConfig").get("Privileged");

        ObjectNode simplifiedNode = objectMapper.createObjectNode();

        simplifiedNode.put("name", nameNode.asText().replaceAll("/", ""));
        simplifiedNode.put("image", imageNode.asText());

        // CMD
        ArrayNode cmdArray = simplifiedNode.putArray("cmd");
        if (cmdNode != null && cmdNode.isArray()) {
            for (JsonNode cmd : cmdNode) {
                cmdArray.add(cmd.asText());
            }
        }

        // 环境变量
        ObjectNode envObject = simplifiedNode.putObject("env");
        if (envNode != null) {
            for (JsonNode envVar : envNode) {
                String[] parts = envVar.asText().split("=", 2);
                if (parts.length == 2) {
                    envObject.put(parts[0], parts[1]);
                }
            }
        }

        // 端口
        ObjectNode portsObject = simplifiedNode.putObject("ports");
        if (portsNode != null && portBindingsNode != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = portBindingsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String containerPort = entry.getKey();
                JsonNode bindings = entry.getValue();
                if (bindings != null && bindings.isArray() && bindings.size() > 0) {
                    String hostPort = bindings.get(0).get("HostPort").asText();
                    portsObject.put(containerPort, hostPort);
                }
            }
        }

        // 卷挂载
        ObjectNode volumesObject = simplifiedNode.putObject("volumes");
        if (mountsNode != null) {
            for (JsonNode mount : mountsNode) {
                String hostPath = mount.get("Source").asText();
                String containerPath = mount.get("Destination").asText();
                volumesObject.put(hostPath, containerPath);
            }
        }

        // 重启策略
        simplifiedNode.put("restartPolicy", restartPolicyNode.get("Name").asText());

        // 补充字段：网络模式、特权模式
        if (networkModeNode != null) {
            simplifiedNode.put("networkMode", networkModeNode.asText());
        }

        if (privilegedNode != null) {
            simplifiedNode.put("privileged", privilegedNode.asBoolean());
        }

        return objectMapper.writeValueAsString(simplifiedNode);
    }

    public static void generateTemplateFromFolder(String folderPath, String templateName) throws Exception {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("指定的路径不是有效的文件夹: " + folderPath);
        }

        // 模板结构
        ObjectNode templateNode = objectMapper.createObjectNode();
        templateNode.put("name", templateName);
        templateNode.put("category", "媒体");
        templateNode.put("version", "1.0");
        templateNode.put("description", "一个强大的媒体管理应用");
        templateNode.put("iconUrl", "https://example.com/icon.jpg");

        ArrayNode servicesNode = templateNode.putArray("services");

        File[] files = folder.listFiles((dir, name) -> name.endsWith("Inspect.json"));
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("文件夹中没有找到inspect文件: " + folderPath);
        }

        for (File file : files) {
            String jsonStr = Files.readString(file.toPath());
            String simpleJson = convertToJson(jsonStr);
            JsonNode serviceNode = objectMapper.readTree(simpleJson);

            ObjectNode service = objectMapper.createObjectNode();
            service.put("id", serviceNode.get("name").asText());
            service.put("name", serviceNode.get("name").asText());
            service.set("template", serviceNode);

            servicesNode.add(service);
        }

        // parameters
        ArrayNode parametersNode = templateNode.putArray("parameters");
        ObjectNode param1 = objectMapper.createObjectNode();
        param1.put("key", "DOCKER_PATH");
        param1.put("name", "Docker配置路径");
        param1.put("value", "/volume1/docker");
        parametersNode.add(param1);

        ObjectNode param2 = objectMapper.createObjectNode();
        param2.put("key", "MEDIA_PATH");
        param2.put("name", "媒体文件路径");
        param2.put("value", "/volume2/media");
        parametersNode.add(param2);

        // configs
        ArrayNode configsNode = templateNode.putArray("configs");
        ObjectNode config = objectMapper.createObjectNode();
        config.put("target", "{{DOCKER_PATH}}");
        ArrayNode urlsNode = config.putArray("urls");
        urlsNode.add("https://example.com/config1.tgz");
        urlsNode.add("https://example.com/config2.tgz");
        configsNode.add(config);

        // 保存输出
        String outputPath = folderPath + "/" + templateName + ".json";
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), templateNode);
        System.out.println("模板已生成: " + outputPath);
    }
}