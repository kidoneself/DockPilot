package com.dsm.websocket.handler;

import com.dsm.model.ContainerStaticInfoDTO;
import com.dsm.service.ContainerService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 容器详情消息处理器
 */
@Slf4j
@Component
public class ContainerDetailMessageHandler extends BaseMessageHandler {

    @Autowired
    private ContainerService containerService;

    @Override
    public MessageType getType() {
        return MessageType.CONTAINER_DETAIL;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
        String containerId = (String) data.get("containerId");
        // 获取容器详情
        ContainerStaticInfoDTO containerDetail = containerService.getContainerConfig(containerId);
        // 发送响应
        sendResponse(session, MessageType.CONTAINER_DETAIL, wsMessage.getTaskId(), containerDetail);

    }
} 