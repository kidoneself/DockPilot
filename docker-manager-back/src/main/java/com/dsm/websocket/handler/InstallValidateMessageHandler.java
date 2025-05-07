package com.dsm.websocket.handler;

import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.service.DockerValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 安装验证消息处理器
 */
@Slf4j
@Component
public class InstallValidateMessageHandler extends BaseMessageHandler {

    @Autowired
    private DockerValidationService dockerValidationService;

    @Override
    public MessageType getType() {
        return MessageType.INSTALL_VALIDATE;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        dockerValidationService.handleInstallValidate(session, wsMessage);

    }
} 