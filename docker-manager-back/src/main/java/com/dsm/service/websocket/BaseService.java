package com.dsm.service.websocket;

import com.dsm.websocket.model.DockerWebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 基础服务接口
 */
public interface BaseService {

    /**
     * 处理消息
     *
     * @param session WebSocket会话
     * @param message 消息内容
     */
    void handle(WebSocketSession session, DockerWebSocketMessage message);
} 