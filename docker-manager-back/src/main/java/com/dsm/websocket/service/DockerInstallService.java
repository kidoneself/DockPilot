package com.dsm.websocket.service;

import com.alibaba.fastjson.JSON;
import com.dsm.api.DockerService;
import com.dsm.mapper.TemplateMapper;
import com.dsm.model.Template;
import com.dsm.utils.JsonPlaceholderReplacerUtil;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.DockerWebSocketMessageSender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class DockerInstallService {

    public static final String MNT_HOST = "/mnt/host";
    @Autowired
    private TemplateMapper templateMapper;

    @Autowired
    private DockerWebSocketMessageSender messageSender;

    @Autowired
    private DockerService dockerService;

    public void handleInstallStart(WebSocketSession session, DockerWebSocketMessage message) {
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) message.getData();
        String appId = (String) data.get("appId");
        @SuppressWarnings("unchecked") Map<String, String> params = (Map<String, String>) data.get("params");

        // 发送开始日志
        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", "开始处理安装请求..."));

        // 从数据库获取应用模板
        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", "正在获取应用模板..."));
        String templateJson = getApplicationTemplate(appId);
        if (templateJson == null) {
            messageSender.sendMessage(session, MessageType.ERROR, message.getTaskId(), "应用模板不存在");
            return;
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
            messageSender.sendMessage(session, MessageType.ERROR, message.getTaskId(), "模板中未找到 services 配置");
            return;
        }

        if (!servicesNode.isArray()) {
            messageSender.sendMessage(session, MessageType.ERROR, message.getTaskId(), "services 配置格式错误，应为对象类型");
            return;
        }

        if (servicesNode.isEmpty()) {
            messageSender.sendMessage(session, MessageType.ERROR, message.getTaskId(), "services 配置为空，未定义任何服务");
            return;
        }

        messageSender.sendMessage(session, MessageType.INSTALL_LOG, message.getTaskId(), Map.of("level", "info", "message", "开始处理服务配置..."));
        Boolean isConfig = true;
        //获取configFiles节点
        JsonNode configsNode = jsonNode.get("configs");
        if (configsNode != null && configsNode.isArray()) {
            isConfig = false;
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
            //获取模板里的volumes
            if (isConfig) {

            }
            // 生成容器启动命令
            CreateContainerCmd containerCmd = dockerService.getCmdByTempJson(template);
            if (containerCmd == null) {
                messageSender.sendMessage(session, MessageType.ERROR, message.getTaskId(), String.format("服务 [%s] 的容器启动命令生成失败", serviceName));
                continue;
            }
            if (isConfig) {
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


    }

    /**
     * 从数据库获取应用模板
     *
     * @param appId 应用ID
     * @return 模板JSON字符串
     */
    private String getApplicationTemplate(String appId) {
        Template template = templateMapper.selectTemplateById(appId);
        return template.getTemplate();
    }

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

    private boolean isPathSafe(String path) {
        // 检查是否包含 .. 或 . 等危险路径
        return !path.contains("..") && !path.contains("./") && !path.contains("/.");
    }

    private void createHostDirectory(String hostPath, WebSocketSession session, String taskId) {
        try {
            // 打印原始路径（调试用）
            messageSender.sendMessage(session, MessageType.INSTALL_LOG, taskId, Map.of("level", "info", "message", String.format("原始绑定路径: %s", hostPath)));

            // 安全检查
            if (!isPathSafe(hostPath)) {
                messageSender.sendMessage(session, MessageType.ERROR, taskId, String.format("不安全的路径: %s", hostPath));
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
            messageSender.sendMessage(session, MessageType.ERROR, taskId, String.format("创建宿主机目录失败: %s, 错误: %s", hostPath, e.getMessage()));
        }
    }
} 