package com.dsm.websocket.handler;

import com.dsm.websocket.message.MessageType;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 消息处理器接口
 */
public interface MessageHandler {
    /**
     * 获取处理器支持的消息类型
     *
     * @return 消息类型
     */
    MessageType getType();

    /**
     * 处理消息
     *
     * @param session WebSocket 会话
     * @param message 消息内容
     */
    void handle(WebSocketSession session, Object message);
} 