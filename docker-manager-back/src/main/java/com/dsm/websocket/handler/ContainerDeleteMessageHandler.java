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
 * 容器删除消息处理器
 */
@Slf4j
@Component
public class ContainerDeleteMessageHandler extends BaseMessageHandler {

    @Autowired
    private ContainerService containerService;

    @Override
    public MessageType getType() {
        return MessageType.CONTAINER_DELETE;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
        String containerId = (String) data.get("containerId");
        // 删除容器
        containerService.removeContainer(containerId);
        // 发送操作结果
        sendResponse(session, MessageType.CONTAINER_OPERATION_RESULT, wsMessage.getTaskId(), null);
    }
} 