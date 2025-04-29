package com.dsm.websocket.sender;

import com.alibaba.fastjson.JSON;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;
import java.util.Map;

@Slf4j
@Component
public class DockerWebSocketMessageSender {
    
    /**
     * 发送消息
     *
     * @param session WebSocket会话
     * @param message WebSocket消息
     */
    public void sendMessage(WebSocketSession session, DockerWebSocketMessage message) {
        try {
            session.sendMessage(new TextMessage(JSON.toJSONString(message)));
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 发送错误消息
     *
     * @param session      WebSocket会话
     * @param errorMessage 错误信息
     */
    public void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            DockerWebSocketMessage message = new DockerWebSocketMessage();
            message.setType("ERROR");
            message.setTaskId(UUID.randomUUID().toString());
            message.setData(errorMessage);
            session.sendMessage(new TextMessage(JSON.toJSONString(message)));
        } catch (IOException e) {
            log.error("发送错误消息失败", e);
        }
    }
    
    /**
     * 发送日志消息
     *
     * @param session WebSocket会话
     * @param type   日志类型
     * @param message 日志消息
     */
    public void sendLog(WebSocketSession session, String type, String message) {
        try {
            sendMessage(session, new DockerWebSocketMessage(
                "INSTALL_LOG",
                UUID.randomUUID().toString(),
                Map.of(
                    "type", type,
                    "message", message
                )
            ));
        } catch (Exception e) {
            log.error("发送日志消息失败", e);
        }
    }
} 