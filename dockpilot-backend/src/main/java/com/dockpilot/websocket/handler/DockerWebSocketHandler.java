package com.dockpilot.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.dockpilot.model.MessageType;
import com.dockpilot.websocket.manager.WebSocketSessionManager;
import com.dockpilot.websocket.model.DockerWebSocketMessage;
import com.dockpilot.websocket.router.MessageRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * Docker WebSocket处理器
 *
 * @author dsm
 * @version 1.0
 * @since 2024-03-21
 */
@Slf4j
@Component
@Tag(name = "Docker WebSocket", description = "Docker容器管理系统的WebSocket API")
public class DockerWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private MessageRouter messageRouter;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Operation(
            summary = "WebSocket连接建立",
            description = "当客户端建立WebSocket连接时调用"
    )
    @Override
    public void afterConnectionEstablished(
            @Parameter(description = "WebSocket会话") WebSocketSession session
    ) {
        sessionManager.addSession(session);
        log.info("WebSocket连接已建立: {}", session.getId());
    }

    @Operation(
            summary = "处理WebSocket消息",
            description = "处理客户端发送的WebSocket消息"
    )
    @Override
    protected void handleTextMessage(
            @Parameter(description = "WebSocket会话") WebSocketSession session,
            @Parameter(description = "接收到的消息") TextMessage message
    ) {
        try {
            DockerWebSocketMessage wsMessage = JSON.parseObject(message.getPayload(), DockerWebSocketMessage.class);
            
            // 注册任务到会话的映射
            if (wsMessage.getTaskId() != null) {
                sessionManager.registerTask(wsMessage.getTaskId(), session.getId());
            }
            
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

    @Operation(
            summary = "WebSocket连接关闭",
            description = "当客户端关闭WebSocket连接时调用"
    )
    @Override
    public void afterConnectionClosed(
            @Parameter(description = "WebSocket会话") WebSocketSession session,
            @Parameter(description = "关闭状态") CloseStatus status
    ) {
        sessionManager.removeSession(session.getId());
        log.info("WebSocket连接已关闭: {}", session.getId());
    }
} 