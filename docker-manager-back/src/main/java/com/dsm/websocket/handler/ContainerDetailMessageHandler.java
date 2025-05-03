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
import org.springframework.web.socket.TextMessage;
import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

/**
 * 容器详情消息处理器
 */
@Slf4j
@Component
public class ContainerDetailMessageHandler implements MessageHandler {

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

            // 获取容器配置
            ContainerStaticInfoDTO containerConfig = containerService.getContainerConfig(containerId);
            
            // 发送容器详情响应
            DockerWebSocketMessage response = new DockerWebSocketMessage(
                MessageType.CONTAINER_DETAIL.name(),
                "",
                containerConfig
            );
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
        } catch (Exception e) {
            log.error("处理容器详情消息时发生错误", e);
            sendErrorMessage(session, "获取容器详情失败：" + e.getMessage());
        }
    }

    /**
     * 发送错误消息
     *
     * @param session WebSocket 会话
     * @param errorMessage 错误信息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            DockerWebSocketMessage errorResponse = new DockerWebSocketMessage(
                MessageType.ERROR.name(),
                "",
                Map.of("message", errorMessage)
            );
            session.sendMessage(new TextMessage(JSON.toJSONString(errorResponse)));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }
} 