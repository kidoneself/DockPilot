package com.dsm.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.dsm.model.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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
            DockerWebSocketMessage response = new DockerWebSocketMessage(
                    MessageType.HEARTBEAT.name(),
                    "",
                    System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
        } catch (Exception e) {
            log.error("处理心跳消息时发生错误", e);
        }
    }
} 