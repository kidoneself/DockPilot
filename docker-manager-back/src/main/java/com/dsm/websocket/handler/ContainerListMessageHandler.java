package com.dsm.websocket.handler;

import com.dsm.model.ContainerDTO;
import com.dsm.service.ContainerService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

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
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            List<ContainerDTO> containers = containerService.listContainers();
            sendResponse(session, MessageType.CONTAINER_LIST, wsMessage.getTaskId(), containers);
    }
} 