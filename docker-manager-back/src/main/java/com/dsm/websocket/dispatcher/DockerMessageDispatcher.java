package com.dsm.websocket.dispatcher;

import com.dsm.websocket.handler.MessageHandler;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Docker WebSocket 消息分发器
 */
@Slf4j
@Component
public class DockerMessageDispatcher {

    private final Map<MessageType, MessageHandler> handlers = new ConcurrentHashMap<>();

    @Autowired
    public DockerMessageDispatcher(List<MessageHandler> messageHandlers) {
        // 注册所有消息处理器
        for (MessageHandler handler : messageHandlers) {
            handlers.put(handler.getType(), handler);
        }
    }

    /**
     * 分发消息到对应的处理器
     *
     * @param session WebSocket 会话
     * @param message 消息内容
     */
    public void dispatch(WebSocketSession session, DockerWebSocketMessage message) {
        MessageHandler handler = handlers.get(MessageType.valueOf(message.getType()));
        if (handler != null) {
            handler.handle(session, message);
        } else {
            log.warn("未找到消息类型 {} 的处理器", message.getType());
        }
    }
} 