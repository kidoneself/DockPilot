package com.dsm.service.websocket;

import com.dsm.model.MessageType;
import com.dsm.service.http.ImageService;
import com.dsm.utils.AsyncTaskRunner;
import com.dsm.utils.MessageCallback;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.WebSocketMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 镜像服务
 * 处理所有镜像相关的消息
 */
@Slf4j
@Service
public class ImageWebSocketService implements BaseService {

    @Resource
    private ImageService imageService;

    @Autowired
    private WebSocketMessageSender messageSender;

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
                case IMAGE_LIST:      // 获取镜像列表
                    result = handleImageList();
                    break;
                case IMAGE_DETAIL:    // 获取镜像详情
                    result = handleImageDetail(message);
                    break;
                case IMAGE_DELETE:    // 删除镜像
                    result = handleImageDelete(message);
                    break;
                case IMAGE_UPDATE:    // 更新镜像
                    result = handleImageUpdate(message);
                    break;
                case IMAGE_BATCH_UPDATE:  // 批量更新镜像
                    result = handleImageBatchUpdate(message);
                    break;
                case PULL_IMAGE:
                    // 假设以下变量已经定义好：
                    AsyncTaskRunner.runWithoutResult(() -> handlePullImage(message, new MessageCallback() {
                        @Override
                        public void onProgress(int progress) {
                            messageSender.sendProgress(session, taskId, progress);
                        }

                        @Override
                        public void onLog(String log) {
                            messageSender.sendLog(session, taskId, log);
                        }

                        @Override
                        public void onComplete() {
                            messageSender.sendComplete(session, taskId, true);
                        }

                        @Override
                        public void onError(String error) {
                            messageSender.sendError(session, taskId, error);
                        }
                    }), messageSender, session, taskId);
                    break;
                case PULL_START:      // 开始拉取镜像
                case PULL_PROGRESS:   // 拉取镜像进度
                case PULL_COMPLETE:   // 拉取镜像完成
                case CANCEL_PULL:     // 取消拉取镜像
                case IMAGE_CANCEL_PULL:   // 取消镜像拉取
                case IMAGE_CHECK_UPDATES: // 检查镜像更新
                    result = handleImageCheckUpdates(message);
                    break;
                default:
                    log.warn("未知的镜像消息类型: {}", type);
            }
            messageSender.sendComplete(session, taskId, result);
        } catch (Exception e) {
            log.error("处理镜像消息时发生错误: {}", type, e);
            messageSender.sendError(session, taskId, e.getMessage());
        }
    }

    /**
     * 处理获取镜像列表的请求
     *
     * @return 镜像列表
     */
    private Object handleImageList() {
        return imageService.listImages();
    }

    /**
     * 处理获取镜像详情的请求
     *
     * @param message WebSocket消息
     * @return 镜像详情信息
     */
    private Object handleImageDetail(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String imageId = (String) data.get("imageId");
        return imageService.getImageDetail(imageId);
    }

    /**
     * 处理删除镜像的请求
     *
     * @param message WebSocket消息
     * @return 删除操作结果
     */
    private Object handleImageDelete(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String imageId = (String) data.get("imageId");
        boolean removeStatus = !data.containsKey("removeStatus") || (boolean) data.get("removeStatus");
        imageService.removeImage(imageId, removeStatus);
        return null;
    }

    /**
     * 处理更新镜像的请求
     *
     * @param message WebSocket消息
     * @return 更新操作结果
     */
    private Object handleImageUpdate(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String image = (String) data.get("image");
        String tag = (String) data.get("tag");
        return imageService.updateImage(image, tag);
    }

    /**
     * 处理批量更新镜像的请求
     *
     * @param message WebSocket消息
     * @return 批量更新操作结果
     */
    private Object handleImageBatchUpdate(DockerWebSocketMessage message) {
        // TODO: 实现批量更新镜像
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
     * 处理检查镜像更新的请求
     *
     * @param message WebSocket消息
     * @return 检查更新结果
     */
    private Object handleImageCheckUpdates(DockerWebSocketMessage message) {
        imageService.checkAllImagesStatus();
        return imageService.listImages();
    }
} 