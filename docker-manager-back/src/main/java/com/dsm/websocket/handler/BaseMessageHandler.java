package com.dsm.websocket.handler;

import com.alibaba.fastjson.JSON;
import com.dsm.websocket.message.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket消息处理器基类
 */
@Slf4j
public abstract class BaseMessageHandler implements MessageHandler {

    /**
     * 获取消息类型
     *
     * @return 消息类型
     */
    public abstract MessageType getType();

    /**
     * 发送响应
     *
     * @param session WebSocket 会话
     * @param type    消息类型
     * @param taskId  任务ID
     * @param data    响应数据
     */
    public void sendResponse(WebSocketSession session, MessageType type, String taskId, Object data) {
        try {
            DockerWebSocketMessage response = new DockerWebSocketMessage(
                    type.name(),
                    taskId,
                    data
            );
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
        } catch (Exception e) {
            log.error("发送响应失败", e);
        }
    }

    /**
     * 发送操作结果响应
     *
     * @param session WebSocket 会话
     * @param taskId  任务ID
     * @param success 是否成功
     * @param data    响应数据
     * @param message 响应消息
     */
    public void sendOperationResult(WebSocketSession session, String taskId, boolean success, Object data, String message) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            if (message != null) {
                result.put("message", message);
            }
            if (data != null) {
                result.put("data", data);
            }
            sendResponse(session, MessageType.CONTAINER_OPERATION_RESULT, taskId, result);
        } catch (Exception e) {
            log.error("发送操作结果响应失败", e);
        }
    }

    /**
     * 发送错误消息
     *
     * @param session      WebSocket 会话
     * @param errorMessage 错误信息
     * @param taskId       任务ID
     */
    public void sendErrorMessage(WebSocketSession session, String errorMessage, String taskId) {
        try {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("message", errorMessage);

            DockerWebSocketMessage errorResponse = new DockerWebSocketMessage(
                    MessageType.ERROR.name(),
                    taskId,
                    errorData
            );
            session.sendMessage(new TextMessage(JSON.toJSONString(errorResponse)));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }
} 