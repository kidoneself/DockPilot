package com.dsm.websocket.handler;

import com.dsm.mapper.TemplateMapper;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 删除模板消息处理器
 */
@Slf4j
@Component
public class DeleteTemplateMessageHandler extends BaseMessageHandler {

    private final TemplateMapper templateMapper;

    public DeleteTemplateMessageHandler(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Override
    public MessageType getType() {
        return MessageType.DELETE_TEMPLATE;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
        String templateId = (String) data.get("templateId");
        // 删除模板
        templateMapper.deleteById(templateId);
        // 发送操作结果
        sendResponse(session, MessageType.OPERATION_RESULT, wsMessage.getTaskId(), null);
    }
} 