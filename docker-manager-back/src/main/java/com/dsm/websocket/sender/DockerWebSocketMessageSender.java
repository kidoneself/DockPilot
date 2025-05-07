package com.dsm.websocket.sender;

import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.message.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
@Component
public class DockerWebSocketMessageSender {

    /**
     * 发送消息
     *
     * @param session WebSocket会话
     * @param type    消息类型
     * @param taskId  任务ID
     * @param data    消息数据
     */
    public void sendMessage(WebSocketSession session, MessageType type, String taskId, Object data) {
        try {
            DockerWebSocketMessage message = new DockerWebSocketMessage(type.name(), taskId, data);
            session.sendMessage(new TextMessage(message.toJson()));
        } catch (IOException e) {
            log.error("发送消息失败: type={}, taskId={}", type, taskId, e);
        }
    }
} 