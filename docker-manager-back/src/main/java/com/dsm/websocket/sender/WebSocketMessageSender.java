package com.dsm.websocket.sender;

import com.dsm.model.MessageType;
import com.dsm.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket消息发送器
 */
@Slf4j
@Component
public class WebSocketMessageSender {

    /**
     * 发送普通消息
     *
     * @param session WebSocket会话
     * @param type    消息类型
     * @param taskId  任务ID
     * @param data    消息数据
     */
    public void sendMessage(WebSocketSession session, MessageType type, String taskId, Object data) {
        try {
            DockerWebSocketMessage message = new DockerWebSocketMessage(type.name(), taskId, data);
            session.sendMessage(new TextMessage(message.toJson()));
        } catch (IOException e) {
            log.error("发送消息失败: type={}, taskId={}", type, taskId, e);
        }
    }

    /**
     * 发送进度消息
     *
     * @param session  WebSocket会话
     * @param taskId   任务ID
     * @param progress 进度（0-100）
     */
    public void sendProgress(WebSocketSession session, String taskId, int progress) {
        try {
            DockerWebSocketMessage fail = DockerWebSocketMessage.progress(taskId, progress);
            session.sendMessage(new TextMessage(fail.toJson()));
        } catch (Exception e) {
            log.error("发送进度消息失败: taskId={}", taskId, e);
        }
    }


    /**
     * 发送日志消息
     *
     * @param session WebSocket会话
     * @param taskId  任务ID
     * @param logMessage    进度（0-100）
     */
    public void sendLog(WebSocketSession session, String taskId, String logMessage) {
        try {
            DockerWebSocketMessage log = DockerWebSocketMessage.log(taskId, logMessage);
            session.sendMessage(new TextMessage(log.toJson()));
        } catch (Exception e) {
            log.error("发送进度消息失败: taskId={}", taskId, e);
        }
    }

    /**
     * 发送错误消息
     *
     * @param session      WebSocket会话
     * @param taskId       任务ID
     * @param errorMessage 错误消息
     */
    public void sendError(WebSocketSession session, String taskId, String errorMessage) {
        try {
            DockerWebSocketMessage fail = DockerWebSocketMessage.fail(taskId, errorMessage);
            session.sendMessage(new TextMessage(fail.toJson()));
        } catch (Exception e) {
            log.error("发送错误消息失败: taskId={}", taskId, e);
        }
    }

    /**
     * 发送开始消息
     *
     * @param session WebSocket会话
     * @param taskId  任务ID
     * @param message 开始消息
     */
    public void sendStart(WebSocketSession session, String taskId, String message) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("message", message);
            sendMessage(session, MessageType.START, taskId, data);
        } catch (Exception e) {
            log.error("发送开始消息失败: taskId={}", taskId, e);
        }
    }

    /**
     * 发送完成消息
     *
     * @param session WebSocket会话
     * @param taskId  任务ID
     * @param data    完成数据
     */
    public void sendComplete(WebSocketSession session, String taskId, Object data) {
        try {
            sendMessage(session, MessageType.COMPLETE, taskId, data);
        } catch (Exception e) {
            log.error("发送完成消息失败: taskId={}", taskId, e);
        }
    }
} 