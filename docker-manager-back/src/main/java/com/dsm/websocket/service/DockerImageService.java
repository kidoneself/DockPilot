package com.dsm.websocket.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dsm.api.DockerService;
import com.dsm.service.ImageService;
import com.dsm.websocket.callback.PullImageCallback;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.DockerWebSocketMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class DockerImageService {

    @Autowired
    private DockerService dockerService;

    @Autowired
    private ImageService imageService;
    @Autowired
    private DockerWebSocketMessageSender messageSender;

    public void handlePullImage(WebSocketSession session, DockerWebSocketMessage message) {
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) message.getData();
        String imageName = (String) data.get("imageName");
        String taskId = UUID.randomUUID().toString();

        // 发送开始消息
        messageSender.sendMessage(session, new DockerWebSocketMessage("PULL_START", taskId, Map.of("imageName", imageName)));

        // 异步执行拉取操作
        CompletableFuture.runAsync(() -> {
            try {
                // 解析镜像名称和标签
                String[] parts = imageName.split(":");
                String image = parts[0];
                String tag = parts.length > 1 ? parts[1] : "latest";

                // 使用DockerService的pullImage方法
                dockerService.pullImage(image, tag, new PullImageCallback() {
                    @Override
                    public void onProgress(int progress, String status) {
                        // 发送进度消息
                        messageSender.sendMessage(session, new DockerWebSocketMessage("PULL_PROGRESS", taskId, Map.of("progress", progress, "status", status)));
                    }

                    @Override
                    public void onComplete() {
                        // 发送完成消息
                        messageSender.sendMessage(session, new DockerWebSocketMessage("PULL_COMPLETE", taskId, Map.of("status", "success")));
                    }

                    @Override
                    public void onError(String error) {
                        // 发送错误消息
                        messageSender.sendErrorMessage(session, error);
                    }
                });
            } catch (Exception e) {
                messageSender.sendErrorMessage(session, e.getMessage());
            }
        });
    }

    public void handleInstallCheckImages(WebSocketSession session, DockerWebSocketMessage message) {
        try {
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

            // 发送检查结果
            messageSender.sendMessage(session, new DockerWebSocketMessage("INSTALL_CHECK_IMAGES_RESULT", message.getTaskId(), results));
        } catch (Exception e) {
            log.error("检查镜像失败", e);
            messageSender.sendErrorMessage(session, "检查镜像失败: " + e.getMessage());
        }
    }


}