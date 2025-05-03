package com.dsm.websocket.handler;

import com.dsm.model.dto.ContainerStaticInfoDTO;
import com.dsm.service.ContainerService;
import com.dsm.utils.ContainerStaticInfoConverter;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.github.dockerjava.api.command.InspectContainerResponse;
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
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
            String containerId = (String) data.get("containerId");

            // 获取容器详情
            ContainerStaticInfoDTO containerDetail = containerService.getContainerConfig(containerId);

            // 发送响应
            sendResponse(session, MessageType.CONTAINER_DETAIL, wsMessage.getTaskId(), containerDetail);
        } catch (Exception e) {
            log.error("处理容器详情消息时发生错误", e);
            sendErrorMessage(session, "获取容器详情失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 