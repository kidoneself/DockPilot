package com.dsm.websocket.handler;

import com.dsm.service.ImageService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 镜像删除消息处理器
 */
@Slf4j
@Component
public class ImageDeleteMessageHandler extends BaseMessageHandler {

    @Autowired
    private ImageService imageService;

    @Override
    public MessageType getType() {
        return MessageType.IMAGE_DELETE;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
        String imageId = (String) data.get("imageId");
        boolean removeStatus = !data.containsKey("removeStatus") || (boolean) data.get("removeStatus");
        // 删除镜像
        imageService.removeImage(imageId, removeStatus);
        // 发送操作结果
        sendResponse(session, MessageType.OPERATION_RESULT, wsMessage.getTaskId(), null);

    }
} 