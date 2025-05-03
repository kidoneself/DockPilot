package com.dsm.websocket.handler;

import com.dsm.websocket.service.DockerImageService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 拉取镜像消息处理器
 */
@Slf4j
@Component
public class PullImageMessageHandler extends BaseMessageHandler {

    @Autowired
    private DockerImageService dockerImageService;

    @Override
    public MessageType getType() {
        return MessageType.PULL_IMAGE;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
            String imageName = (String) data.get("imageName");

            // 处理拉取镜像请求
            dockerImageService.handlePullImage(session, wsMessage);
            
            // 发送响应
            sendResponse(session, MessageType.PULL_IMAGE, wsMessage.getTaskId(), imageName);
        } catch (Exception e) {
            log.error("处理拉取镜像消息时发生错误", e);
            sendErrorMessage(session, "拉取镜像失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 