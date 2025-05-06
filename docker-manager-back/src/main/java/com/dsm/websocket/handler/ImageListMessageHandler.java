package com.dsm.websocket.handler;

import com.dsm.model.ImageStatusDTO;
import com.dsm.service.ImageService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * 镜像列表消息处理器
 */
@Slf4j
@Component
public class ImageListMessageHandler extends BaseMessageHandler {

    @Autowired
    private ImageService imageService;

    @Override
    public MessageType getType() {
        return MessageType.IMAGE_LIST;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        // 获取镜像列表
        List<ImageStatusDTO> images = imageService.listImages();
        // 发送响应
        sendResponse(session, MessageType.IMAGE_LIST, wsMessage.getTaskId(), images);

    }
} 