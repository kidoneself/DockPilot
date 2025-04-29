package com.dsm.websocket.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dsm.api.DockerService;
import com.dsm.service.ImageService;
import com.dsm.websocket.callback.PullImageCallback;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.DockerWebSocketMessageSender;
import com.github.dockerjava.api.model.Image;
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

    public void handleInstallPullImage(WebSocketSession session, DockerWebSocketMessage message) {
        // TODO: 实现拉取安装所需镜像的逻辑
    }

    /**
     * 处理更新镜像请求
     *
     * @param session WebSocket会话
     * @param message WebSocket消息
     */
    public void handleUpdateImage(WebSocketSession session, DockerWebSocketMessage message) {
        try {
            @SuppressWarnings("unchecked") Map<String, String> data = (Map<String, String>) message.getData();
            String image = data.get("image");
            String tag = data.getOrDefault("tag", "latest");
            String taskId = UUID.randomUUID().toString();

            // 发送开始消息
            messageSender.sendMessage(session, new DockerWebSocketMessage("UPDATE_START", taskId, Map.of("image", image, "tag", tag)));

            // 异步执行更新操作
            CompletableFuture.runAsync(() -> {
                try {
                    // 使用DockerService的pullImage方法
                    dockerService.pullImage(image, tag, new PullImageCallback() {
                        @Override
                        public void onProgress(int progress, String status) {
                            // 发送进度消息
                            messageSender.sendMessage(session, new DockerWebSocketMessage("UPDATE_PROGRESS", taskId, Map.of("progress", progress, "status", status)));
                        }

                        @Override
                        public void onComplete() {
                            // 发送完成消息
                            messageSender.sendMessage(session, new DockerWebSocketMessage("UPDATE_COMPLETE", taskId, Map.of("status", "success")));
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
        } catch (Exception e) {
            log.error("更新镜像失败", e);
            messageSender.sendErrorMessage(session, "更新镜像失败: " + e.getMessage());
        }
    }

    /**
     * 处理检查镜像更新状态请求
     *
     * @param session WebSocket会话
     * @param message WebSocket消息
     */
    public void handleCheckImageUpdates(WebSocketSession session, DockerWebSocketMessage message) {
        try {
            String taskId = UUID.randomUUID().toString();

            // 发送开始消息
            messageSender.sendMessage(session, new DockerWebSocketMessage("CHECK_UPDATES_START", taskId, null));

            // 异步执行检查操作
            CompletableFuture.runAsync(() -> {
                try {
                    // 直接调用checkAllImagesStatus方法
                    Map<String, Object> result = new HashMap<>();
                    imageService.checkAllImagesStatus();
                    result.put("result", true);
                    // 发送检查结果
                    messageSender.sendMessage(session, new DockerWebSocketMessage("CHECK_UPDATES_COMPLETE", taskId, result));
                } catch (Exception e) {
                    messageSender.sendErrorMessage(session, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("检查镜像更新状态失败", e);
            messageSender.sendErrorMessage(session, "检查镜像更新状态失败: " + e.getMessage());
        }
    }

    /**
     * 检查所有镜像的更新状态
     *
     * @return 包含所有镜像更新状态的Map
     */
    private Map<String, Object> checkAllImagesStatus() {
        Map<String, Object> result = new HashMap<>();
        List<Image> images = dockerService.listImages();

        for (Image image : images) {
            String[] repoTags = image.getRepoTags();
            if (repoTags != null && repoTags.length > 0) {
                for (String repoTag : repoTags) {
                    String[] parts = repoTag.split(":");
                    String imageName = parts[0];
                    String tag = parts.length > 1 ? parts[1] : "latest";

                    try {
                        // 获取本地镜像创建时间
                        String localCreateTime = dockerService.getLocalImageCreateTime(imageName, tag);
                        // 获取远程镜像创建时间
                        String remoteCreateTime = dockerService.getRemoteImageCreateTime(imageName, tag);

                        Map<String, Object> imageStatus = new HashMap<>();
                        imageStatus.put("localCreateTime", localCreateTime);
                        imageStatus.put("remoteCreateTime", remoteCreateTime);
                        imageStatus.put("hasUpdate", !localCreateTime.equals(remoteCreateTime));

                        result.put(repoTag, imageStatus);
                    } catch (Exception e) {
                        log.error("检查镜像 " + repoTag + " 更新状态失败: " + e.getMessage());
                        Map<String, Object> imageStatus = new HashMap<>();
                        imageStatus.put("error", e.getMessage());
                        result.put(repoTag, imageStatus);
                    }
                }
            }
        }

        return result;
    }
} 