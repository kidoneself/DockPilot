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
 * 镜像更新消息处理器
 */
@Slf4j
@Component
public class ImageUpdateMessageHandler extends BaseMessageHandler {

    @Autowired
    private ImageService imageService;

    @Override
    public MessageType getType() {
        return MessageType.IMAGE_UPDATE;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
        String image = (String) data.get("image");
        String tag = (String) data.get("tag");
        // 更新镜像
        Map<String, Object> result = imageService.updateImage(image, tag);
        // 发送响应
        sendResponse(session, MessageType.IMAGE_UPDATE, wsMessage.getTaskId(), result);

    }
} 