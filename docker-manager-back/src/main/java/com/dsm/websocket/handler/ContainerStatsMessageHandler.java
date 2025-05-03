package com.dsm.websocket.handler;

import com.dsm.service.ContainerService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.util.Map;

/**
 * 容器资源使用情况消息处理器
 */
@Slf4j
@Component
public class ContainerStatsMessageHandler implements MessageHandler {

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
            if (!(message instanceof DockerWebSocketMessage)) {
                log.error("无效的消息类型: {}", message.getClass().getName());
                return;
            }

            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
            String containerId = (String) data.get("containerId");

            if (containerId == null || containerId.isEmpty()) {
                log.error("容器ID不能为空");
                sendError(session, "容器ID不能为空");
                return;
            }

            // 获取容器资源使用情况
            var stats = containerService.getContainerStats(containerId);

            // 构建响应消息
            DockerWebSocketMessage response = new DockerWebSocketMessage(
                MessageType.CONTAINER_STATS.name(),
                "",
                stats
            );

            // 发送响应
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
        } catch (Exception e) {
            log.error("处理容器资源使用情况消息时发生错误", e);
            try {
                sendError(session, "获取容器资源使用情况失败: " + e.getMessage());
            } catch (IOException ex) {
                log.error("发送错误消息失败", ex);
            }
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        DockerWebSocketMessage errorMessage = new DockerWebSocketMessage(
            MessageType.ERROR.name(),
            "",
            Map.of("message", message)
        );
        session.sendMessage(new TextMessage(JSON.toJSONString(errorMessage)));
    }
} 