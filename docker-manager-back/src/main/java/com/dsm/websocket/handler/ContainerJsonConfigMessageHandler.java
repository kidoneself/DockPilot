package com.dsm.websocket.handler;

import com.dsm.utils.DockerInspectJsonGenerator;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 容器JSON配置消息处理器
 */
@Slf4j
@Component
public class ContainerJsonConfigMessageHandler extends BaseMessageHandler {

    @Override
    public MessageType getType() {
        return MessageType.CONTAINER_JSON_CONFIG;
    }

    @Override
    public void handle(WebSocketSession session, Object message) {
        DockerWebSocketMessage wsMessage = (DockerWebSocketMessage) message;
        Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
        String containerId = (String) data.get("containerId");
        try {
            // 生成容器的JSON配置
            String jsonConfig = DockerInspectJsonGenerator.generateJsonFromContainerId(containerId);
            // 解析为JSONObject以保持字段顺序
            JSONObject jsonObject = JSON.parseObject(jsonConfig);
            // 发送响应
            sendResponse(session, MessageType.CONTAINER_JSON_CONFIG, wsMessage.getTaskId(), jsonObject);
        } catch (Exception e) {
            log.error("生成JSON配置失败", e);
            sendErrorMessage(session, "生成JSON配置失败: " + e.getMessage(), wsMessage.getTaskId());
        }
    }
} 