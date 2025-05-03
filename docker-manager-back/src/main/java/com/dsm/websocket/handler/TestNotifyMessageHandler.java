package com.dsm.websocket.handler;

import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.service.DockerTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;
import com.alibaba.fastjson.JSON;
import org.springframework.web.socket.TextMessage;

/**
 * 测试通知消息处理器
 */
@Slf4j
@Component
public class TestNotifyMessageHandler implements MessageHandler {

    @Autowired
    private DockerTestService dockerTestService;

    @Override
    public MessageType getType() {
        return MessageType.TEST_NOTIFY;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            dockerTestService.handleTestNotify(session, wsMessage);
        } catch (Exception e) {
            log.error("处理测试通知消息时发生错误", e);
            sendErrorMessage(session, "处理测试通知失败：" + e.getMessage());
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