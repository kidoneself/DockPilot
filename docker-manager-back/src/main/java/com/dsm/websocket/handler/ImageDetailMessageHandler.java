package com.dsm.websocket.handler;

import com.dsm.service.ImageService;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.pojo.dto.image.ImageInspectDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 镜像详情消息处理器
 */
@Slf4j
@Component
public class ImageDetailMessageHandler extends BaseMessageHandler {

    @Autowired
    private ImageService imageService;

    @Override
    public MessageType getType() {
        return MessageType.IMAGE_DETAIL;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        try {
            DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
            String imageName = (String) data.get("imageName");

            // 获取镜像详情
            ImageInspectDTO imageDetail = imageService.getImageDetail(imageName);
            
            // 发送响应
            sendResponse(session, MessageType.IMAGE_DETAIL, wsMessage.getTaskId(), imageDetail);
        } catch (Exception e) {
            log.error("处理镜像详情消息时发生错误", e);
            sendErrorMessage(session, "获取镜像详情失败：" + e.getMessage(), ((DockerWebSocketMessage) message).getTaskId());
        }
    }
} 