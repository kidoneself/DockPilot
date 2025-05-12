package com.dsm.inspect2Cmd;

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
            String currentDir = "/Users/lizhiqiang/coding-my/docker/docker-manager-back/src/main/java/com/dsm/inspect2Cmd";

            // 只使用一个参数，同时作为文件夹名和模板名
            String name = "allinone";
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
            if (cmdNode.size() > 0) {
                simplifiedJson = simplifiedJson.substring(0, simplifiedJson.length() - 2); // 去掉最后的逗号
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
                simplifiedJson += "\"" + env[0] + "\":\"" + env[1] + "\", ";
            }
            simplifiedJson = simplifiedJson.substring(0, simplifiedJson.length() - 2); // 去掉最后的逗号
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
            simplifiedJson = simplifiedJson.substring(0, simplifiedJson.length() - 2); // 去掉最后的逗号
        }
        simplifiedJson += "}, ";

        // 挂载卷
        simplifiedJson += "\"volumes\":{";
        if (volumesNode != null && volumesNode.size() > 0) {
            Iterator<JsonNode> volumeIter = volumesNode.elements();
            while (volumeIter.hasNext()) {
                JsonNode volume = volumeIter.next();
                String hostPath = volume.get("Source").asText();
                String containerPath = volume.get("Destination").asText();
                simplifiedJson += "\"" + hostPath + "\":\"" + containerPath + "\", ";
            }
            simplifiedJson = simplifiedJson.substring(0, simplifiedJson.length() - 2); // 去掉最后的逗号
        }
        simplifiedJson += "}, ";

        // 重启策略
        simplifiedJson += "\"restartPolicy\":\"" + restartPolicyNode.get("Name").asText() + "\"";

        simplifiedJson += "}";

        return simplifiedJson;
    }

    /**
     * 处理指定文件夹下的所有inspect文件并生成模板
     *
     * @param folderPath   包含inspect文件的文件夹路径
     * @param templateName 生成的模板名称
     */
    public static void generateTemplateFromFolder(String folderPath, String templateName) throws Exception {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("指定的路径不是有效的文件夹: " + folderPath);
        }

        // 创建模板的基本结构
        ObjectNode templateNode = objectMapper.createObjectNode();
        templateNode.put("name", templateName);
        templateNode.put("category", "媒体");
        templateNode.put("version", "1.0");
        templateNode.put("description", "一个强大的媒体管理应用");
        templateNode.put("iconUrl", "https://example.com/icon.jpg");
        
        ArrayNode servicesNode = templateNode.putArray("services");

        // 处理文件夹下的所有inspect文件
        File[] files = folder.listFiles((dir, name) -> name.endsWith("Inspect.json"));
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("文件夹中没有找到inspect文件: " + folderPath);
        }

        for (File file : files) {
            String jsonStr = Files.readString(file.toPath());
            String simpleJson = convertToJson(jsonStr);
            JsonNode serviceNode = objectMapper.readTree(simpleJson);
            
            // 创建服务对象
            ObjectNode service = objectMapper.createObjectNode();
            service.put("id", serviceNode.get("name").asText());
            service.put("name", serviceNode.get("name").asText());
            service.set("template", serviceNode);
            
            servicesNode.add(service);
        }

        // 添加parameters
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

        // 添加configs
        ArrayNode configsNode = templateNode.putArray("configs");
        ObjectNode config = objectMapper.createObjectNode();
        config.put("target", "{{DOCKER_PATH}}");
        ArrayNode urlsNode = config.putArray("urls");
        urlsNode.add("https://example.com/config1.tgz");
        urlsNode.add("https://example.com/config2.tgz");
        configsNode.add(config);

        // 保存模板文件
        String outputPath = folderPath + "/" + templateName + ".json";
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), templateNode);
        System.out.println("模板已生成: " + outputPath);
    }

}