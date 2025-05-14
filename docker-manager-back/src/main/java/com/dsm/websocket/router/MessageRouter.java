package com.dsm.websocket.router;

import com.dsm.model.MessageType;
import com.dsm.service.websocket.AppStoreService;
import com.dsm.service.websocket.ContainerWebSocketService;
import com.dsm.service.websocket.ImageWebSocketService;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 消息路由器
 * 负责消息的路由和分发
 */
@Slf4j
@Component
public class MessageRouter {

    @Autowired
    private ContainerWebSocketService containerService;

    @Autowired
    private ImageWebSocketService imageService;

    @Autowired
    private AppStoreService appStoreService;

    /**
     * 路由消息到对应的服务
     */
    public void route(WebSocketSession session, DockerWebSocketMessage message) {
        String type = message.getType();
        try {
            MessageType messageType = MessageType.valueOf(type);
            
            // 根据消息类型前缀路由到对应的服务
            if (type.startsWith("CONTAINER_")) {
                containerService.handle(session, message);
            } else if (type.startsWith("IMAGE_") || type.startsWith("PULL_")) {
                imageService.handle(session, message);
            } else if (type.startsWith("INSTALL_") || type.startsWith("NETWORK_") || 
                      type.startsWith("IMPORT_") || type.startsWith("DELETE_")) {
                appStoreService.handle(session, message);
            } else {
                // 处理系统消息
                handleSystemMessage(session, message);
            }
        } catch (Exception e) {
            log.error("路由消息时发生错误: {}", type, e);
            throw e;
        }
    }

    /**
     * 处理系统消息
     */
    private void handleSystemMessage(WebSocketSession session, DockerWebSocketMessage message) {
        switch (MessageType.valueOf(message.getType())) {
            case HEARTBEAT:
                // 处理心跳消息
                break;
            case TEST_NOTIFY:
                // 处理测试通知
                break;
            case ERROR:
                // 处理错误消息
                break;
            default:
                log.warn("未知的系统消息类型: {}", message.getType());
        }
    }
}