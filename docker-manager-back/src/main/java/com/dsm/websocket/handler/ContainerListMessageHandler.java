package com.dsm.websocket.handler;

import com.dsm.model.dto.ContainerDTO;
import com.dsm.service.ContainerService;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.message.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

/**
 * 容器列表消息处理器
 */
@Slf4j
@Component
public class ContainerListMessageHandler extends BaseMessageHandler {

    @Autowired
    private ContainerService containerService;

    @Override
    public MessageType getType() {
        return MessageType.CONTAINER_LIST;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            
            // 获取容器列表
            List<ContainerDTO> containers = containerService.listContainers();
            
            // 发送响应
            sendResponse(session, MessageType.CONTAINER_LIST, wsMessage.getTaskId(), containers);
        } catch (Exception e) {
            log.error("处理容器列表消息时发生错误", e);
            sendErrorMessage(session, "获取容器列表失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 