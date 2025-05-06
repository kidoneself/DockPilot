package com.dsm.websocket.handler;

import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.service.DockerInstallService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 安装开始消息处理器
 */
@Slf4j
@Component
public class InstallStartMessageHandler extends BaseMessageHandler {

    @Autowired
    private DockerInstallService dockerInstallService;

    @Override
    public MessageType getType() {
        return MessageType.INSTALL_START;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            dockerInstallService.handleInstallStart(session, wsMessage);
        } catch (Exception e) {
            log.error("处理安装开始消息时发生错误", e);
            sendErrorMessage(session, "开始安装失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 