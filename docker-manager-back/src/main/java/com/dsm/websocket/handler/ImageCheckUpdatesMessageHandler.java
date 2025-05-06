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
 * 镜像更新检查消息处理器
 */
@Slf4j
@Component
public class ImageCheckUpdatesMessageHandler extends BaseMessageHandler {

    @Autowired
    private ImageService imageService;

    @Override
    public MessageType getType() {
        return MessageType.IMAGE_CHECK_UPDATES;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        // 检查所有镜像的更新状态
        imageService.checkAllImagesStatus();
        // 获取更新后的镜像列表
        List<ImageStatusDTO> images = imageService.listImages();
        // 发送响应
        sendResponse(session, MessageType.IMAGE_CHECK_UPDATES, wsMessage.getTaskId(), images);

    }
} 