package com.dsm.websocket.handler;

import com.dsm.service.ContainerService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 容器启动消息处理器
 */
@Slf4j
@Component
public class ContainerStartMessageHandler extends BaseMessageHandler {

    @Autowired
    private ContainerService containerService;

    @Override
    public MessageType getType() {
        return MessageType.CONTAINER_START;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
            String containerId = (String) data.get("containerId");

            // 启动容器
            containerService.startContainer(containerId);

            // 发送操作结果
            sendOperationResult(
                session,
                wsMessage.getTaskId(),
                true,
                containerId,
                "容器启动成功"
            );
        } catch (Exception e) {
            log.error("处理容器启动消息时发生错误", e);
            // 发送错误消息
            sendErrorMessage(session, "启动容器失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 