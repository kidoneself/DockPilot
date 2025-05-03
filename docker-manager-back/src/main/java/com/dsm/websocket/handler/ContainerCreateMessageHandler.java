package com.dsm.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.dsm.model.dockerApi.ContainerCreateRequest;
import com.dsm.model.dto.JsonContainerRequest;
import com.dsm.service.ContainerService;
import com.dsm.utils.JsonContainerRequestToContainerCreateRequestConverter;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 容器创建消息处理器
 */
@Slf4j
@Component
public class ContainerCreateMessageHandler extends BaseMessageHandler {

    @Autowired
    private ContainerService containerService;

    @Override
    public MessageType getType() {
        return MessageType.CONTAINER_CREATE;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();

            // 将 Map 转换为 ContainerCreateRequest
            JsonContainerRequest json = JSON.parseObject(JSON.toJSONString(data), JsonContainerRequest.class);

            ContainerCreateRequest request = JsonContainerRequestToContainerCreateRequestConverter.convert(json);

            // 创建容器
            String containerId = containerService.createContainer(request);

            // 发送操作结果
            sendOperationResult(
                session,
                wsMessage.getTaskId(),
                true,
                containerId,
                "容器创建成功"
            );

        } catch (Exception e) {
            log.error("处理容器创建消息时发生错误", e);
            // 发送错误消息
            sendErrorMessage(session, "创建容器失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }

    /**
     * 发送错误消息
     *
     * @param session      WebSocket 会话
     * @param errorMessage 错误信息
     * @param taskId      任务ID
     */
    public void sendErrorMessage(WebSocketSession session, String errorMessage, String taskId) {
        try {
            DockerWebSocketMessage errorResponse = new DockerWebSocketMessage(
                    MessageType.ERROR.name(),
                    taskId,  // 使用传入的 taskId
                    Map.of("message", errorMessage)
            );
            session.sendMessage(new TextMessage(JSON.toJSONString(errorResponse)));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }
} 