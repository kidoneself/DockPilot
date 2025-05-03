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
import com.alibaba.fastjson.JSON;

import java.util.Map;

/**
 * 容器更新消息处理器
 */
@Slf4j
@Component
public class ContainerUpdateMessageHandler extends BaseMessageHandler {

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
            JsonContainerRequest json = JSON.parseObject(JSON.toJSONString(data), JsonContainerRequest.class);
            ContainerCreateRequest request = JsonContainerRequestToContainerCreateRequestConverter.convert(json);

            // 更新容器
            containerService.updateContainer(containerId, request);

            // 发送操作结果
            sendOperationResult(
                session,
                wsMessage.getTaskId(),
                true,
                containerId,
                "容器更新成功"
            );
        } catch (Exception e) {
            log.error("处理容器更新消息时发生错误", e);
            sendErrorMessage(session, "更新容器失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 