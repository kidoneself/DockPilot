package com.dsm.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;

public class DockerConfigGenerator1 {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void generateCmdJson(String inspectJsonPath, String outputPath) throws Exception {
        JsonNode inspectArray = mapper.readTree(new File(inspectJsonPath));
        ObjectNode cmdJson = mapper.createObjectNode();

        // 获取第一个容器配置
        JsonNode container = inspectArray.get(0);
        JsonNode config = container.path("Config");
        JsonNode hostConfig = container.path("HostConfig");

        // 基础配置
        cmdJson.put("Image", config.path("Image").asText());
        cmdJson.set("Env", mapper.valueToTree(extractArray(config, "Env")));
        cmdJson.set("ExposedPorts", buildExposedPorts(config.path("ExposedPorts")));
//        cmdJson.set("Entrypoint", mapper.valueToTree(extractArray(config, "Entrypoint")));

        // 主机配置
        ObjectNode hostConfigJson = mapper.createObjectNode();
        hostConfigJson.set("PortBindings", buildPortBindings(hostConfig.path("PortBindings")));
        hostConfigJson.set("Binds", buildBinds(container.path("Mounts")));  // 修正挂载点来源
        hostConfigJson.put("Privileged", hostConfig.path("Privileged").asBoolean());
        hostConfigJson.put("RestartPolicy", hostConfig.path("RestartPolicy").path("Name").asText());
        // 从 inspect2.json 读取网络模式
        hostConfigJson.put("NetworkMode", hostConfig.path("NetworkMode").asText());
        cmdJson.set("HostConfig", hostConfigJson);

        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), cmdJson);
    }

    // 提取数组字段（支持多层路径）
    private static String[] extractArray(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (node.isMissingNode() || !node.isArray()) {
            return new String[0];
        }
        return mapper.convertValue(node, String[].class);
    }

    // 构建端口绑定配置
    private static ObjectNode buildPortBindings(JsonNode portBindings) {
        ObjectNode portsNode = mapper.createObjectNode();
        portBindings.fields().forEachRemaining(entry -> {
            String containerPort = entry.getKey();
            JsonNode binding = entry.getValue().get(0);
            String hostPort = binding.path("HostPort").asText();
            portsNode.put(containerPort, hostPort);
        });
        return portsNode;
    }

    // 构建暴露端口
    private static ObjectNode buildExposedPorts(JsonNode exposedPorts) {
        ObjectNode portsNode = mapper.createObjectNode();
        exposedPorts.fieldNames().forEachRemaining(port -> portsNode.put(port, "{}"));
        return portsNode;
    }

    // 构建卷绑定（从Mounts字段获取）
    private static ArrayNode buildBinds(JsonNode mounts) {
        ArrayNode binds = mapper.createArrayNode();
        mounts.forEach(mount -> {
            String source = mount.path("Source").asText();
            String target = mount.path("Destination").asText();
            binds.add(source + ":" + target);
        });
        return binds;
    }

    public static void main(String[] args) throws Exception {
        DockerConfigGenerator1.generateCmdJson(
                "/Users/lizhiqiang/coding-my/docker/docker-manager-back/src/main/java/com/dsm/test/inspect2.json",
                "/Users/lizhiqiang/coding-my/docker/docker-manager-back/src/main/java/com/dsm/test/cmd.json"
        );
    }
}