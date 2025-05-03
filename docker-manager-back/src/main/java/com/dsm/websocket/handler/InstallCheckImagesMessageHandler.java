package com.dsm.websocket.handler;

import com.dsm.websocket.service.DockerImageService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 安装检查镜像消息处理器
 */
@Slf4j
@Component
public class InstallCheckImagesMessageHandler extends BaseMessageHandler {

    @Autowired
    private DockerImageService dockerImageService;

    @Override
    public MessageType getType() {
        return MessageType.INSTALL_CHECK_IMAGES;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            dockerImageService.handleInstallCheckImages(session, wsMessage);
        } catch (Exception e) {
            log.error("处理安装检查镜像消息时发生错误", e);
            sendErrorMessage(session, "检查镜像失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 