package com.dsm.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.dsm.model.ContainerCreateRequest;
import com.dsm.model.JsonContainerRequest;
import com.dsm.service.ContainerService;
import com.dsm.utils.JsonContainerRequestToContainerCreateRequestConverter;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
        // 将 Map 转换为 ContainerCreateRequest
        JsonContainerRequest json = JSON.parseObject(JSON.toJSONString(data), JsonContainerRequest.class);
        ContainerCreateRequest request = JsonContainerRequestToContainerCreateRequestConverter.convert(json);
        // 创建容器
        String containerId = containerService.createContainer(request);
        // 发送操作结果
        sendResponse(session, MessageType.CONTAINER_OPERATION_RESULT, wsMessage.getTaskId(), containerId);
    }

}