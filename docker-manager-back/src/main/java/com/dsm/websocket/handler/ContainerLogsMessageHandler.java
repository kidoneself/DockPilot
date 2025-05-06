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
 * 容器日志消息处理器
 */
@Slf4j
@Component
public class ContainerLogsMessageHandler extends BaseMessageHandler {

    @Autowired
    private ContainerService containerService;

    @Override
    public MessageType getType() {
        return MessageType.CONTAINER_LOGS;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
        String containerId = (String) data.get("containerId");
        // 获取容器日志
        String logs = containerService.getContainerLogs(containerId, 100, false, false);
        sendResponse(session, MessageType.CONTAINER_LOGS, wsMessage.getTaskId(), logs);
    }

} 