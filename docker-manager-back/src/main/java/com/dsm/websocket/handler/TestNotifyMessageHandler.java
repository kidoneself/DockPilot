package com.dsm.websocket.handler;

import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 测试通知消息处理器
 */
@Slf4j
@Component
public class TestNotifyMessageHandler extends BaseMessageHandler {

    @Override
    public MessageType getType() {
        return MessageType.TEST_NOTIFY;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            
            // 发送测试通知响应
            sendResponse(session, MessageType.TEST_NOTIFY, wsMessage.getTaskId(), "测试通知成功");
        } catch (Exception e) {
            log.error("处理测试通知消息时发生错误", e);
            sendErrorMessage(session, "测试通知失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 