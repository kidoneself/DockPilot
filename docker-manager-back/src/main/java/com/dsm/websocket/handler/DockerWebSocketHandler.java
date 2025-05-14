package com.dsm.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.dsm.websocket.router.MessageRouter;
import com.dsm.model.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Docker WebSocket处理器
 *
 * @author dsm
 * @version 1.0
 * @since 2024-03-21
 */
@Slf4j
@Component
@Tag(name = "Docker WebSocket处理器", description = "处理Docker相关的WebSocket消息")
public class DockerWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Autowired
    private MessageRouter messageRouter;

    /**
     * 连接建立时的处理
     *
     * @param session WebSocket会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        SESSIONS.put(sessionId, session);
        log.info("WebSocket连接已建立: {}", sessionId);
    }

    /**
     * 处理接收到的消息
     *
     * @param session WebSocket会话
     * @param message 接收到的消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            DockerWebSocketMessage wsMessage = JSON.parseObject(message.getPayload(), DockerWebSocketMessage.class);
            messageRouter.route(session, wsMessage);
        } catch (Exception e) {
            log.error("处理消息时发生错误", e);
            // 尝试获取 taskId
            String taskId = "unknown";
            try {
                DockerWebSocketMessage wsMessage = JSON.parseObject(message.getPayload(), DockerWebSocketMessage.class);
                if (wsMessage.getTaskId() != null) {
                    taskId = wsMessage.getTaskId();
                }
            } catch (Exception ignore) {
            }
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("message", e.getMessage());
            DockerWebSocketMessage errorResponse = new DockerWebSocketMessage(MessageType.ERROR.name(), taskId, errorData);
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(errorResponse)));
            } catch (Exception sendEx) {
                log.warn("发送错误响应失败", sendEx);
            }
        }
    }

    /**
     * 连接关闭时的处理
     *
     * @param session WebSocket会话
     * @param status  关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        SESSIONS.remove(sessionId);
        log.info("WebSocket连接已关闭: {}", sessionId);
    }
} 