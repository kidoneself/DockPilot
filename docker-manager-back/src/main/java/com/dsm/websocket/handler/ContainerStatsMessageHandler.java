package com.dsm.websocket.handler;

import com.dsm.service.ContainerService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.model.dto.ResourceUsageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 容器资源使用情况消息处理器
 */
@Slf4j
@Component
public class ContainerStatsMessageHandler extends BaseMessageHandler {

    private final ContainerService containerService;

    public ContainerStatsMessageHandler(ContainerService containerService) {
        this.containerService = containerService;
    }

    @Override
    public MessageType getType() {
        return MessageType.CONTAINER_STATS;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
            String containerId = (String) data.get("containerId");

            // 获取容器资源使用情况
            ResourceUsageDTO stats = containerService.getContainerStats(containerId);

            // 发送响应
            sendResponse(session, MessageType.CONTAINER_STATS, wsMessage.getTaskId(), stats);
        } catch (Exception e) {
            log.error("处理容器资源使用情况消息时发生错误", e);
            sendErrorMessage(session, "获取容器资源使用情况失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 