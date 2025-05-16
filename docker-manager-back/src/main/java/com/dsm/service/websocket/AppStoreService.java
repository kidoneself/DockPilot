package com.dsm.service.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dsm.api.DockerService;
import com.dsm.mapper.TemplateMapper;
import com.dsm.model.MessageType;
import com.dsm.model.Template;
import com.dsm.service.http.ImageService;
import com.dsm.service.http.NetworkService;
import com.dsm.utils.JsonPlaceholderReplacerUtil;
import com.dsm.utils.MessageCallback;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.WebSocketMessageSender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 应用商店服务
 * 处理所有应用商店相关的消息，包括安装和模板管理
 */
@Slf4j
@Service
public class AppStoreService implements BaseService {

    public static final String MNT_HOST = "/mnt/host";

    @Autowired
    private DockerService dockerService;

    @Autowired
    private TemplateMapper templateMapper;

    @Autowired
    private WebSocketMessageSender messageSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NetworkService networkService;
    @Autowired
    private ImageService imageService;

    /**
     * 处理WebSocket消息的主入口方法
     *
     * @param session WebSocket会话
     * @param message 接收到的消息
     */
    @Override
    public void handle(WebSocketSession session, DockerWebSocketMessage message) {
        MessageType type = MessageType.valueOf(message.getType());
        String taskId = message.getTaskId();

        try {

            // 处理消息
            Object result = null;
            switch (type) {
                // 安装相关
                case INSTALL_CHECK_IMAGES:    // 检查安装所需的镜像
                    result = handleInstallCheckImages(message);
                    break;
                case INSTALL_VALIDATE:        // 验证安装参数
                    result = handleInstallValidate(message);
                    break;
                case INSTALL_START:           // 开始安装
                    result = handleInstallStart(session, message);
                    break;
                case INSTALL_LOG:             // 安装日志
                    result = handleInstallLog(session, message);
                    break;
                case PULL_IMAGE:
                    CompletableFuture.runAsync(() -> {
                        CompletableFuture<Void> pullFuture = handlePullImage(message, new MessageCallback() {
                            @Override
                            public void onProgress(int progress) {
                                // 处理进度
                                System.out.println("进度: " + progress + "%");
                                messageSender.sendProgress(session, taskId, progress);
                            }

                            @Override
                            public void onLog(String log) {
                                // 处理日志
                                System.out.println("日志: " + log);
                                messageSender.sendLog(session, taskId, log);
                            }

                            @Override
                            public void onComplete() {
                                // 完成时的操作
                                System.out.println("镜像拉取完成！");
                                messageSender.sendComplete(session, taskId, true);
                            }

                            @Override
                            public void onError(String error) {
                                // 错误时的操作
                                System.err.println("错误: " + error);
                                messageSender.sendError(session, taskId, error);
                            }
                        });

                        pullFuture.thenRun(() -> {
                            messageSender.sendComplete(session, taskId, null); // 拉取完成
                        }).exceptionally(ex -> {
                            messageSender.sendError(session, taskId, ex.getMessage()); // 错误处理
                            return null;
                        });
                    });
                    break;
                // 模板相关
                case NETWORK_LIST:            // 获取网络列表
                    result = handleNetworkList();
                    break;
                case IMPORT_TEMPLATE:         // 导入模板
                    result = handleImportTemplate(message);
                    break;
                case DELETE_TEMPLATE:         // 删除模板
                    result = handleDeleteTemplate(message);
                    break;

                default:
                    log.warn("未知的应用商店消息类型: {}", type);
            }

            // 发送完成消息
            messageSender.sendComplete(session, taskId, result);
        } catch (Exception e) {
            log.error("处理应用商店消息时发生错误: {}", type, e);
            messageSender.sendError(session, taskId, e.getMessage());
        }
    }

    /**
     * 处理检查镜像是否存在的请求
     *
     * @param message WebSocket消息
     */
    private Object handleInstallCheckImages(DockerWebSocketMessage message) {
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) message.getData();
        JSONArray images = (JSONArray) data.get("images");

        List<Map<String, Object>> results = new ArrayList<>();

        for (Object obj : images) {
            JSONObject image = (JSONObject) obj;
            String imageName = image.getString("name");
            String tag = image.getString("tag");
            String fullImageName = tag != null && !tag.isEmpty() ? imageName + ":" + tag : imageName;

            Map<String, Object> result = new HashMap<>();
            result.put("name", imageName);
            result.put("tag", tag);

            try {
                // 尝试获取镜像信息，如果成功则说明镜像存在
                dockerService.getInspectImage(fullImageName);
                result.put("exists", true);
            } catch (Exception e) {
                result.put("exists", false);
                result.put("error", e.getMessage());
            }

            results.add(result);
        }

        return results;
    }

    /**
     * 处理安装参数验证的请求
     *
     * @param message WebSocket消息
     */
    private Object handleInstallValidate(DockerWebSocketMessage message) {
        try {
            @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) message.getData();
            List<Map<String, Object>> results = new ArrayList<>();

            StringBuilder checkCommand = new StringBuilder();

            if (params.containsKey("ports")) {
                @SuppressWarnings("unchecked") List<Map<String, Object>> ports = (List<Map<String, Object>>) params.get("ports");
                for (Map<String, Object> port : ports) {
                    String portStr = port.get("hostPort").toString().trim();
                    int hostPort = Integer.parseInt(portStr);
                    checkCommand.append("nc -z 127.0.0.1 ").append(hostPort).append(" >/dev/null 2>&1; ").append("code=$?; ").append("if [ $code -eq 0 ]; then ").append("echo '::type=port::port=").append(hostPort).append("::status=1::message=Host port is in use'; ").append("else ").append("echo '::type=port::port=").append(hostPort).append("::status=0::message=Host port is available'; ").append("fi; ");
                }
            }

            if (params.containsKey("paths")) {
                @SuppressWarnings("unchecked") List<Map<String, Object>> paths = (List<Map<String, Object>>) params.get("paths");
                for (Map<String, Object> path : paths) {
                    String hostPath = path.get("hostPath").toString().trim();
                    String target = "/host" + hostPath;
                    checkCommand.append("[ -d ").append(target).append(" ] && dir=0 || dir=1; ").append("[ -r ").append(target).append(" ] && read=0 || read=1; ").append("[ -w ").append(target).append(" ] && write=0 || write=1; ").append("if [ $dir -eq 0 ] && [ $read -eq 0 ] && [ $write -eq 0 ]; then ").append("echo '::type=path::path=").append(hostPath).append("::status=0::message=Path is valid and accessible'; ").append("else ").append("msg=\"\"; ").append("if [ $dir -ne 0 ]; then msg=\"$msg Path does not exist;\"; fi; ").append("if [ $read -ne 0 ]; then msg=\"$msg Not readable;\"; fi; ").append("if [ $write -ne 0 ]; then msg=\"$msg Not writable;\"; fi; ").append("echo '::type=path::path=").append(hostPath).append("::status=1::message='$msg''; ").append("fi; ");
                }
            }

            ProcessBuilder pb = new ProcessBuilder("docker", "run", "--rm", "--network", "host", "-v", "/:/host", "docker-manager:latest", "sh", "-c", checkCommand.toString());

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Check output: {}", line);
                    if (!line.startsWith("::")) continue;

                    String[] parts = line.substring(2).split("::");
                    Map<String, String> map = new HashMap<>();
                    for (String part : parts) {
                        int idx = part.indexOf('=');
                        if (idx > 0) {
                            map.put(part.substring(0, idx), part.substring(idx + 1));
                        }
                    }

                    Map<String, Object> result = new HashMap<>();
                    String type = map.get("type");
                    result.put("type", type);
                    result.put("message", map.get("message"));
                    result.put("valid", "0".equals(map.get("status")));

                    if ("port".equals(type)) {
                        result.put("port", Integer.parseInt(map.get("port")));
                    } else if ("path".equals(type)) {
                        result.put("path", map.get("path"));
                    }

                    results.add(result);
                }
            }
            return results;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 处理开始安装应用的请求
     *
     * @param session WebSocket会话
     * @param message WebSocket消息
     */
    private Object handleInstallStart(WebSocketSession session, DockerWebSocketMessage message) {
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) message.getData();
        String appId = (String) data.get("appId");
        @SuppressWarnings("unchecked") Map<String, String> params = (Map<String, String>) data.get("params");

        // 发送开始日志
        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", "开始处理安装请求..."));

        // 从数据库获取应用模板
        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", "正在获取应用模板..."));
        String templateJson = getApplicationTemplate(appId);
        if (templateJson == null) {
            messageSender.sendError(session, message.getTaskId(), "应用模板不存在");
            return null;
        }
        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "success", "message", "成功获取应用模板"));

        // 替换模板中的参数
        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", "正在替换模板参数..."));
        String processedTemplate = JsonPlaceholderReplacerUtil.replacePlaceholders(templateJson, params);
        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "success", "message", "参数替换完成"));

        ObjectMapper mapper = new ObjectMapper();
        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", "开始解析容器配置..."));
        JsonNode jsonNode = null;
        try {
            jsonNode = mapper.readTree(processedTemplate);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "success", "message", "解析容器配置完成"));

        // 获取 services 节点
        JsonNode servicesNode = jsonNode.get("services");
        if (servicesNode == null) {
            messageSender.sendError(session, message.getTaskId(), "模板中未找到 services 配置");
            return Collections.emptyList();
        }

        if (!servicesNode.isArray()) {
            messageSender.sendError(session, message.getTaskId(), "services 配置格式错误，应为对象类型");
            return Collections.emptyList();
        }

        if (servicesNode.isEmpty()) {
            messageSender.sendError(session, message.getTaskId(), "services 配置为空，未定义任何服务");
            return Collections.emptyList();
        }

        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", "开始处理服务配置..."));
        boolean mkdirConfig = true;
        //获取configFiles节点
        JsonNode configsNode = jsonNode.get("configs");
        if (configsNode != null && configsNode.isArray()) {
            mkdirConfig = false;
            messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", "开始处理配置文件..."));

            for (JsonNode config : configsNode) {
                String targetPath = config.get("target").asText();
                targetPath = MNT_HOST + targetPath;
                messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", String.format("正在处理目标路径: %s", targetPath)));

                JsonNode urlsNode = config.get("urls");
                if (urlsNode != null && urlsNode.isArray()) {
                    for (JsonNode urlNode : urlsNode) {
                        String url = urlNode.asText();
                        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", String.format("正在下载文件: %s", url)));

                        try {
                            // 从URL中获取文件名
                            String fileName = url.substring(url.lastIndexOf('/') + 1);
                            // 确保目标路径是完整的文件路径
                            String fullTargetPath = targetPath.endsWith("/") ? targetPath + fileName : targetPath + "/" + fileName;

                            // 下载文件到目标路径
                            downloadFile(url, fullTargetPath, session, message.getTaskId());
                            messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "success", "message", String.format("文件下载成功: %s", url)));
                            // 解压文件
                            messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", String.format("正在解压文件: %s", fullTargetPath)));
                            unzipFile(fullTargetPath);
                            messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "success", "message", String.format("文件解压成功: %s", fullTargetPath)));
                        } catch (Exception e) {
                            messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "error", "message", String.format("文件下载失败: %s, 错误: %s", url, e.getMessage())));
                        }
                    }

                    messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "success", "message", "所有配置文件处理完成"));
                }
            }
        }

        // 使用for循环遍历services节点
        for (JsonNode serviceConfig : servicesNode) {
            String serviceName = serviceConfig.get("name").asText();
            messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", String.format("正在处理服务 [%s] 的配置...", serviceName)));

            JsonNode template = serviceConfig.get("template");
            // 生成容器启动命令
            CreateContainerCmd containerCmd = dockerService.getCmdByTempJson(template);
            if (containerCmd == null) {
                messageSender.sendError(session, message.getTaskId(), String.format("服务 [%s] 的容器启动命令生成失败", serviceName));
                continue;
            }
            if (mkdirConfig) {
                //获取containerCmd中的volumes
                Bind[] binds = Objects.requireNonNull(containerCmd.getHostConfig()).getBinds();
                if (binds != null) {
                    for (Bind bind : binds) {
                        String hostPath = bind.getPath();
                        hostPath = MNT_HOST + hostPath;
                        createHostDirectory(hostPath, session, message.getTaskId());
                    }
                }
            }

            messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "success", "message", String.format("服务 [%s] 的容器启动命令生成成功", serviceName)));
            String containerId = dockerService.startContainerWithCmd(containerCmd);
            messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "success", "message", String.format("服务 [%s] 的容器创建成功", containerId)));
        }

        // 发送处理后的模板
        messageSender.sendMessage(session, MessageType.INSTALL_START_RESULT, message.getTaskId(), Map.of("success", true, "message", "模板处理成功", "template", JSON.parseObject(processedTemplate)));
        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "success", "message", "所有服务配置处理完成"));
        return null;
    }

    /**
     * 处理安装日志的请求
     *
     * @param session WebSocket会话
     * @param message WebSocket消息
     */
    private Object handleInstallLog(WebSocketSession session, DockerWebSocketMessage message) {
        // TODO: 实现安装日志
        return null;
    }

    /**
     * 处理拉取镜像的请求
     *
     * @param message WebSocket消息
     */
    private CompletableFuture<Void> handlePullImage(DockerWebSocketMessage message, MessageCallback callback) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String fullImageName = (String) data.get("imageName");
        String taskId = message.getTaskId();

        // 拆解 imageName 为 repo 和 tag
        String repo, tag;
        if (fullImageName.contains(":")) {
            String[] parts = fullImageName.split(":", 2);
            repo = parts[0];
            tag = parts[1];
        } else {
            repo = fullImageName;
            tag = "latest";
        }

        // 通知开始
        if (callback != null) {
            callback.onProgress(0);  // 初始进度为0
            callback.onLog("开始拉取镜像: " + repo + ":" + tag);
        }

        // 使用 skopeo 拉取镜像
        return imageService.pullImage(repo, tag, callback);
    }

    /**
     * 处理获取网络列表的请求
     */
    private Object handleNetworkList() {
        return networkService.listNetworks();
    }

    /**
     * 处理导入模板的请求
     *
     * @param message WebSocket消息
     */
    private Object handleImportTemplate(DockerWebSocketMessage message) {
        try {
            Map<String, Object> data = (Map<String, Object>) message.getData();
            String templateContent = (String) data.get("content");

            // 解析完整的模板内容
            JsonNode rootNode = objectMapper.readTree(templateContent);

            // 创建新的JSON对象，只包含services和parameters节点
            ObjectNode newTemplate = objectMapper.createObjectNode();
            newTemplate.set("services", rootNode.get("services"));
            newTemplate.set("parameters", rootNode.get("parameters"));
            if (rootNode.get("configs") != null) {
                newTemplate.set("configs", rootNode.get("configs"));
            }

            // 将新的JSON对象转换为字符串
            String processedContent = objectMapper.writeValueAsString(newTemplate);

            // 创建模板实体
            Template template = new Template();
            template.setId(UUID.randomUUID().toString());
            template.setName(rootNode.get("name").asText());
            template.setCategory(rootNode.get("category").asText());
            template.setVersion(rootNode.get("version").asText());
            template.setDescription(rootNode.get("description").asText());
            template.setIconUrl(rootNode.get("iconUrl").asText());
            template.setTemplate(processedContent);
            template.setCreatedAt(LocalDateTime.now());
            template.setUpdatedAt(LocalDateTime.now());
            template.setSortWeight(0);
            return templateMapper.insert(template);
        } catch (Exception e) {
            log.error("导入模板失败", e);
            return null;
        }
    }

    /**
     * 处理删除模板的请求
     *
     * @param message WebSocket消息
     */
    private Object handleDeleteTemplate(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String templateId = (String) data.get("templateId");
        // 删除模板
        return templateMapper.deleteById(templateId);
        // 发送操作结果
    }

    /**
     * 获取应用模板
     *
     * @param appId 应用ID
     * @return 模板内容
     */
    private String getApplicationTemplate(String appId) {
        Template template = templateMapper.selectTemplateById(appId);
        return template.getTemplate();
    }

    /**
     * 下载文件
     *
     * @param url        文件URL
     * @param targetPath 目标路径
     * @param session    WebSocket会话
     * @param taskId     任务ID
     * @throws IOException 下载过程中可能发生的IO异常
     */
    private void downloadFile(String url, String targetPath, WebSocketSession session, String taskId) throws IOException {
        URL fileUrl = new URL(url);
        URLConnection conn = fileUrl.openConnection();
        long contentLength = conn.getContentLengthLong();

        Files.createDirectories(Paths.get(targetPath).getParent());

        try (InputStream in = conn.getInputStream(); OutputStream out = Files.newOutputStream(Paths.get(targetPath))) {
            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int bytesRead;
            long lastPrintedPercent = -1;

            messageSender.sendMessage(session, MessageType.INSTALL_LOG, taskId, Map.of("level", "info", "message", "⬇️ 开始下载: " + url));
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;

                if (contentLength > 0) {
                    long percent = downloaded * 100 / contentLength;
                    if (percent != lastPrintedPercent) {
                        messageSender.sendMessage(session, MessageType.INSTALL_LOG, taskId, Map.of("level", "info", "message", String.format("📦 下载进度: %d%% (%d/%d bytes)", percent, downloaded, contentLength)));
                        lastPrintedPercent = percent;
                    }
                }
            }
            messageSender.sendMessage(session, MessageType.INSTALL_LOG, taskId, Map.of("level", "success", "message", "✅ 下载完成: " + targetPath));
        }
    }

    /**
     * 解压文件
     *
     * @param tgzFilePath tgz文件路径
     * @throws IOException 解压过程中可能发生的IO异常
     */
    private void unzipFile(String tgzFilePath) throws IOException {
        Path targetDir = Paths.get(tgzFilePath).getParent();
        try (InputStream fi = Files.newInputStream(Paths.get(tgzFilePath)); GzipCompressorInputStream gzi = new GzipCompressorInputStream(fi); TarArchiveInputStream ti = new TarArchiveInputStream(gzi)) {

            TarArchiveEntry entry;
            while ((entry = ti.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                Path filePath = targetDir.resolve(entry.getName());
                Files.createDirectories(filePath.getParent());

                try (OutputStream out = Files.newOutputStream(filePath)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = ti.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

    /**
     * 检查路径是否安全
     *
     * @param path 要检查的路径
     * @return 是否安全
     */
    private boolean isPathSafe(String path) {
        // 检查是否包含 .. 或 . 等危险路径
        return !path.contains("..") && !path.contains("./") && !path.contains("/.");
    }

    /**
     * 创建宿主机目录
     *
     * @param hostPath 宿主机路径
     * @param session  WebSocket会话
     * @param taskId   任务ID
     */
    private void createHostDirectory(String hostPath, WebSocketSession session, String taskId) {
        try {
            // 打印原始路径（调试用）
            messageSender.sendMessage(session, MessageType.INSTALL_LOG, taskId, Map.of("level", "info", "message", String.format("原始绑定路径: %s", hostPath)));

            // 安全检查
            if (!isPathSafe(hostPath)) {
                messageSender.sendError(session, taskId, String.format("不安全的路径: %s", hostPath));
                return;
            }

            Path path = Paths.get(hostPath);
            // 检查目录是否已存在
            if (Files.exists(path)) {
                messageSender.sendMessage(session, MessageType.INSTALL_LOG, taskId, Map.of("level", "info", "message", String.format("目录已存在: %s", hostPath)));
                return;
            }

            // 创建目录
            Files.createDirectories(path);
            messageSender.sendMessage(session, MessageType.INSTALL_LOG, taskId, Map.of("level", "success", "message", String.format("创建宿主机目录成功: %s", hostPath)));
        } catch (IOException e) {
            messageSender.sendError(session, taskId, String.format("创建宿主机目录失败: %s, 错误: %s", hostPath, e.getMessage()));
        }
    }
} 