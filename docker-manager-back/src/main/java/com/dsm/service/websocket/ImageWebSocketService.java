package com.dsm.service.websocket;

import com.dsm.model.MessageType;
import com.dsm.service.http.ImageService;
import com.dsm.utils.ErrorMessageExtractor;
import com.dsm.utils.MessageCallback;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.WebSocketMessageSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "镜像 WebSocket 服务", description = "处理镜像相关的 WebSocket 消息")
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
    @Operation(
            summary = "处理镜像相关的WebSocket消息",
            description = "根据消息类型处理不同的镜像操作，包括：\n" +
                    "- IMAGE_LIST: 获取镜像列表\n" +
                    "- IMAGE_DETAIL: 获取镜像详情\n" +
                    "- IMAGE_DELETE: 删除镜像\n" +
                    "- IMAGE_UPDATE: 更新镜像\n" +
                    "- IMAGE_BATCH_UPDATE: 批量更新镜像\n" +
                    "- PULL_IMAGE: 拉取镜像\n" +
                    "- IMAGE_CHECK_UPDATES: 检查镜像更新"
    )
    public void handle(
            @Parameter(description = "WebSocket会话") WebSocketSession session,
            @Parameter(description = "接收到的消息") DockerWebSocketMessage message
    ) {
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
                    // 创建回调对象
                    MessageCallback callback = new MessageCallback() {
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
                    };

                    // 执行异步任务
                    CompletableFuture<Void> future = handlePullImage(message, callback);

                    // 等待异步任务完成
                    future.whenComplete((voidResult, error) -> {
                        if (error != null) {
                            log.error("拉取镜像失败", error);
                            String userFriendlyError = ErrorMessageExtractor.extractUserFriendlyError(error);
                            messageSender.sendError(session, taskId, userFriendlyError);
                        }
                    });

                    // 关键修复：使用 return 而不是 break，避免执行后面的 sendComplete
                    return;
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
            String userFriendlyError = ErrorMessageExtractor.extractUserFriendlyError(e);
            messageSender.sendError(session, taskId, userFriendlyError);
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

        // 检查是否已经在拉取中
        if (imageService.isPulling(repo, tag)) {
            String error = "镜像 " + repo + ":" + tag + " 已经在拉取中";
            if (callback != null) {
                callback.onError(error);
            }
            return CompletableFuture.failedFuture(new RuntimeException(error));
        }

        // 开始拉取，记录到数据库
        imageService.startPullImage(repo, tag);

        // 创建增强的回调对象，集成数据库状态更新
        MessageCallback enhancedCallback = new MessageCallback() {
            @Override
            public void onProgress(int progress) {
                // 更新数据库进度
                imageService.updatePullProgress(repo, tag, progress, "拉取进度: " + progress + "%");

                // 调用原始回调
                if (callback != null) {
                    callback.onProgress(progress);
                }
            }

            @Override
            public void onLog(String log) {
                // 更新数据库进度，包含日志信息
                imageService.updatePullProgress(repo, tag, -1, log); // -1 表示进度不变，只更新消息

                // 调用原始回调
                if (callback != null) {
                    callback.onLog(log);
                }
            }

            @Override
            public void onComplete() {
                // 拉取成功，更新数据库状态
                // 注意：实际的镜像ID获取可以后续优化，这里先用null
                imageService.completePullImage(repo, tag, null);

                // 调用原始回调
                if (callback != null) {
                    callback.onComplete();
                }
            }

            @Override
            public void onError(String error) {
                // 更新数据库为拉取失败状态
                imageService.failPullImage(repo, tag, error);

                // 调用原始回调
                if (callback != null) {
                    callback.onError(error);
                }
            }
        };

        // 使用增强回调执行拉取
        return imageService.pullImage(repo, tag, enhancedCallback);
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