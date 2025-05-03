package com.dsm.websocket.handler;

import com.dsm.pojo.dto.NetworkInfoDTO;
import com.dsm.service.NetworkService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * 网络列表消息处理器
 */
@Slf4j
@Component
public class NetworkListMessageHandler extends BaseMessageHandler {

    @Autowired
    private NetworkService networkService;

    @Override
    public MessageType getType() {
        return MessageType.NETWORK_LIST;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            
            // 获取网络列表
            List<NetworkInfoDTO> networks = networkService.listNetworks();
            
            // 发送响应
            sendResponse(session, MessageType.NETWORK_LIST, wsMessage.getTaskId(), networks);
        } catch (Exception e) {
            log.error("处理网络列表消息时发生错误", e);
            sendErrorMessage(session, "获取网络列表失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 