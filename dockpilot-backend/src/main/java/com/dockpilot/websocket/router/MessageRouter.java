package com.dockpilot.websocket.router;

import com.alibaba.fastjson.JSON;
import com.dockpilot.model.MessageType;
//import com.dockpilot.service.websocket.AppStoreService;
import com.dockpilot.service.websocket.ContainerWebSocketService;
import com.dockpilot.service.websocket.ImageWebSocketService;
import com.dockpilot.service.websocket.NetworkWebSocketService;
import com.dockpilot.service.websocket.SystemWebSocketService;
import com.dockpilot.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
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
//
//    @Autowired
//    private AppStoreService appStoreService;

    @Autowired
    private NetworkWebSocketService networkService;

    @Autowired
    private SystemWebSocketService systemService;

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
            } else if (type.startsWith("NETWORK_")) {
                networkService.handle(session, message);
            } else if (type.startsWith("SYSTEM_")) {
                systemService.handle(session, message);
            }
             else {
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
        try {
            switch (MessageType.valueOf(message.getType())) {
                case HEARTBEAT:
                    // 处理心跳消息，返回 PONG
                    DockerWebSocketMessage response = new DockerWebSocketMessage(
                            "HEARTBEAT",
                            message.getTaskId(),
                            System.currentTimeMillis()
                    );
                    session.sendMessage(new TextMessage(JSON.toJSONString(response)));
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
        } catch (Exception e) {
            log.error("处理系统消息时发生错误", e);
        }
    }
}