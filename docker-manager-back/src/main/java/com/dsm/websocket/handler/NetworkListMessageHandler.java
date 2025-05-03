package com.dsm.websocket.handler;

import com.dsm.pojo.dto.NetworkInfoDTO;
import com.dsm.service.NetworkService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import com.alibaba.fastjson.JSON;

import java.util.List;
import java.util.Map;

/**
 * 网络列表消息处理器
 */
@Slf4j
@Component
public class NetworkListMessageHandler implements MessageHandler {

    @Autowired
    private NetworkService networkService;

    @Override
    public MessageType getType() {
        return MessageType.NETWORK_LIST;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            // 获取网络列表
            List<NetworkInfoDTO> networks = networkService.listNetworks();
            
            // 创建响应消息
            DockerWebSocketMessage response = new DockerWebSocketMessage(
                MessageType.NETWORK_LIST.name(),
                "",
                networks
            );
            
            // 发送消息
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
        } catch (Exception e) {
            log.error("处理网络列表消息时发生错误", e);
            // 发送错误消息
            sendErrorMessage(session, "获取网络列表失败：" + e.getMessage());
        }
    }

    /**
     * 发送错误消息
     *
     * @param session WebSocket 会话
     * @param errorMessage 错误信息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            DockerWebSocketMessage errorResponse = new DockerWebSocketMessage(
                MessageType.ERROR.name(),
                "",
                Map.of("message", errorMessage)
            );
            session.sendMessage(new TextMessage(JSON.toJSONString(errorResponse)));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }
} 