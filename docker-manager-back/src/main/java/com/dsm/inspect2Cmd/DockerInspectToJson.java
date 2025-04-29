package com.dsm.inspect2Cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.Map;

public class DockerInspectToJson {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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

        // 构建简化 JSON
        String simplifiedJson = "{";

        // 容器名称
        simplifiedJson += "\"name\":\"" + nameNode.asText().replaceAll("/", "") + "\", ";

        // 镜像
        simplifiedJson += "\"image\":\"" + imageNode.asText() + "\", ";

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
            Iterator<Map.Entry<String, JsonNode>> portsIter = (Iterator<Map.Entry<String, JsonNode>>) portsNode.fields();
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
        if (volumesNode != null) {
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
     * 主方法：读取 inspect2.json，生成简化版 json 并以容器名命名保存
     * 步骤：
     * 1. 读取 inspect2.json 文件内容为字符串
     * 2. 调用 convertToJson 方法生成简化 json 字符串
     * 3. 解析容器名，去除前缀 / 作为文件名
     * 4. 将简化 json 写入以容器名命名的新文件（如 naspt-mpv2.json）
     * 5. 错误处理：文件不存在、json 解析失败等
     */
    public static void main(String[] args) {
        // 1. 读取 inspect2.json 文件内容为字符串
        String jsonStr = null;
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("/Users/lizhiqiang/coding-my/docker/docker-manager-back/src/main/java/com/dsm/inspect2Cmd/qbinspect.json");
            jsonStr = java.nio.file.Files.readString(path);
            System.out.println("inspect2.json 读取成功，内容长度：" + jsonStr.length()); // 调试输出
        } catch (Exception e) {
            System.err.println("读取 inspect2.json 失败: " + e.getMessage());
            return;
        }

        // 2. 调用 convertToJson 方法生成简化 json 字符串
        String simpleJson = null;
        String containerName = null;
        try {
            simpleJson = convertToJson(jsonStr);
            // 额外解析容器名用于命名输出文件
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonStr);
            JsonNode containerNode = rootNode.get(0);
            containerName = containerNode.get("Name").asText().replaceAll("/", "");
            System.out.println("容器名: " + containerName);
        } catch (Exception e) {
            System.err.println("生成简化 json 失败: " + e.getMessage());
            return;
        }
        if (simpleJson == null || containerName == null) {
            System.err.println("简化 json 或容器名为空，终止输出。");
            return;
        }

        // 3. 将简化 json 写入以容器名命名的新文件（如 naspt-mpv2.json）
        String outFileName = "/Users/lizhiqiang/coding-my/docker/docker-manager-back/src/main/java/com/dsm/inspect2Cmd/" + containerName + ".json";
        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get(outFileName), simpleJson);
            System.out.println("简化 json 已写入文件: " + outFileName);
        } catch (Exception e) {
            System.err.println("写入简化 json 文件失败: " + e.getMessage());
        }
    }
}