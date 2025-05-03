package com.dsm.websocket.handler;

import com.dsm.model.dockerApi.ContainerCreateRequest;
import com.dsm.service.ContainerService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.utils.JsonContainerRequestToContainerCreateRequestConverter;
import com.dsm.model.dto.JsonContainerRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

/**
 * 容器更新消息处理器
 */
@Slf4j
@Component
public class ContainerUpdateMessageHandler implements MessageHandler {

    @Autowired
    private ContainerService containerService;

    @Override
    public MessageType getType() {
        return MessageType.CONTAINER_UPDATE;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
            String containerId = (String) data.get("containerId");
            Map<String, Object> requestData = (Map<String, Object>) data.get("data");

            boolean success = false;
            String errorMessage = null;
            String newContainerId = null;

            try {
                // 直接将 Map 转换为 JsonContainerRequest
                JsonContainerRequest jsonRequest = JSON.parseObject(JSON.toJSONString(data), JsonContainerRequest.class);
                // 使用转换器将 JsonContainerRequest 转换为 ContainerCreateRequest
                ContainerCreateRequest request = JsonContainerRequestToContainerCreateRequestConverter.convert(jsonRequest);
                // 更新容器并获取新的容器 ID
                newContainerId = containerService.updateContainer(containerId, request);
                success = true;
            } catch (Exception e) {
                log.error("更新容器失败", e);
                errorMessage = e.getMessage();
            }

            // 发送操作结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            if (errorMessage != null) {
                result.put("message", errorMessage);
            }
            if (newContainerId != null) {
                result.put("newContainerId", newContainerId);
            }

            DockerWebSocketMessage response = new DockerWebSocketMessage(
                MessageType.CONTAINER_OPERATION_RESULT.name(),
                "",
                result
            );
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
        } catch (Exception e) {
            log.error("处理容器更新消息时发生错误", e);
            sendErrorMessage(session, "处理容器更新失败：" + e.getMessage());
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