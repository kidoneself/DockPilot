package com.dsm.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.dsm.model.ContainerCreateRequest;
import com.dsm.model.ContainerStaticInfoDTO;
import com.dsm.model.JsonContainerRequest;
import com.dsm.service.ContainerService;
import com.dsm.utils.ContainerStaticInfoDTOToContainerCreateRequestConverter;
import com.dsm.utils.JsonContainerRequestToContainerCreateRequestConverter;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

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
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
        String containerId = (String) data.get("containerId");
        boolean onlyContainerId = data.containsKey("containerId") && data.size() == 1;
        if (onlyContainerId) {
            // 更新按钮更新容器
            ContainerStaticInfoDTO containerConfig = containerService.getContainerConfig(containerId);
            // 发送操作结果
            ContainerCreateRequest original = ContainerStaticInfoDTOToContainerCreateRequestConverter.convert(containerConfig);
            String newContainerId = containerService.updateContainer(containerId, original);
            sendResponse(session, MessageType.OPERATION_RESULT, wsMessage.getTaskId(), newContainerId);
        }
        JsonContainerRequest json = JSON.parseObject(JSON.toJSONString(data), JsonContainerRequest.class);
        ContainerCreateRequest request = JsonContainerRequestToContainerCreateRequestConverter.convert(json);
        // 更新容器
        String newContainerId = containerService.updateContainer(containerId, request);
        // 发送操作结果
        sendResponse(session, MessageType.OPERATION_RESULT, wsMessage.getTaskId(), newContainerId);
    }
} 