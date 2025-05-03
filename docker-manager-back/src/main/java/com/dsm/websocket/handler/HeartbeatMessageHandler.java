package com.dsm.websocket.handler;

import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

/**
 * 心跳消息处理器
 */
@Slf4j
@Component
public class HeartbeatMessageHandler implements MessageHandler {

    @Override
    public MessageType getType() {
        return MessageType.HEARTBEAT;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            // 发送心跳响应
            Map<String, Object> data = new HashMap<>();
            data.put("timestamp", System.currentTimeMillis());
            
            DockerWebSocketMessage response = new DockerWebSocketMessage(
                MessageType.HEARTBEAT.name(),
                "",
                data
            );
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
        } catch (Exception e) {
            log.error("处理心跳消息时发生错误", e);
        }
    }
} 