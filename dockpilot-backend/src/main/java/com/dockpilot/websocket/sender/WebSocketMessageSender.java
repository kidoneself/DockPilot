package com.dockpilot.websocket.sender;

import com.dockpilot.model.MessageType;
import com.dockpilot.websocket.manager.WebSocketSessionManager;
import com.dockpilot.websocket.model.DockerWebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private WebSocketSessionManager sessionManager;

    /**
     * 检查WebSocket会话是否可用
     *
     * @param session WebSocket会话
     * @return 是否可用
     */
    private boolean isSessionAvailable(WebSocketSession session) {
        return session != null && session.isOpen();
    }

    /**
     * 获取可用的会话（优先使用指定会话，如果不可用则尝试获取其他可用会话）
     *
     * @param session 指定的会话
     * @param taskId  任务ID
     * @return 可用的会话，如果没有则返回 null
     */
    private WebSocketSession getAvailableSession(WebSocketSession session, String taskId) {
        // 如果指定会话可用，直接使用
        if (isSessionAvailable(session)) {
            log.debug("使用原始会话发送消息: taskId={}, sessionId={}", taskId, session.getId());
            return session;
        }
        
        log.warn("原始会话不可用: taskId={}, sessionId={}", taskId, session != null ? session.getId() : "null");
        
        // 指定会话不可用，尝试从会话管理器获取
        WebSocketSession availableSession = sessionManager.getSessionForTask(taskId);
        if (availableSession != null) {
            log.info("✅ 任务 {} 的会话已迁移到新连接: {} → {}", 
                taskId, 
                session != null ? session.getId() : "null", 
                availableSession.getId());
            return availableSession;
        }
        
        log.error("❌ 没有可用的 WebSocket 会话发送消息: taskId={}", taskId);
        return null;
    }

    /**
     * 安全发送消息
     *
     * @param session WebSocket会话
     * @param message 消息内容
     * @param taskId  任务ID（用于日志）
     */
    private void safeSendMessage(WebSocketSession session, TextMessage message, String taskId) {
        WebSocketSession availableSession = getAvailableSession(session, taskId);
        
        if (availableSession == null) {
            log.warn("WebSocket会话不可用，跳过消息发送: taskId={}", taskId);
            return;
        }
        
        try {
            availableSession.sendMessage(message);
        } catch (IOException e) {
            log.error("发送WebSocket消息失败: taskId={}", taskId, e);
        } catch (IllegalStateException e) {
            log.warn("WebSocket会话状态异常，跳过消息发送: taskId={}, error={}", taskId, e.getMessage());
        }
    }

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
            safeSendMessage(session, new TextMessage(message.toJson()), taskId);
        } catch (Exception e) {
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
            DockerWebSocketMessage progressMessage = DockerWebSocketMessage.progress(taskId, progress);
            safeSendMessage(session, new TextMessage(progressMessage.toJson()), taskId);
        } catch (Exception e) {
            log.error("发送进度消息失败: taskId={}, progress={}%", taskId, progress, e);
        }
    }

    /**
     * 发送带镜像名称的进度消息
     *
     * @param session   WebSocket会话
     * @param taskId    任务ID
     * @param progress  进度（0-100）
     * @param imageName 镜像名称
     */
    public void sendProgressWithImageName(WebSocketSession session, String taskId, int progress, String imageName) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("progress", progress);
            data.put("imageName", imageName);
            
            DockerWebSocketMessage progressMessage = new DockerWebSocketMessage();
            progressMessage.setType(MessageType.PROGRESS.name());
            progressMessage.setTaskId(taskId);
            progressMessage.setData(data);
            progressMessage.setTimestamp(System.currentTimeMillis());
            
            safeSendMessage(session, new TextMessage(progressMessage.toJson()), taskId);
        } catch (Exception e) {
            log.error("发送进度消息失败: taskId={}, progress={}%, imageName={}", taskId, progress, imageName, e);
        }
    }

    /**
     * 发送日志消息
     *
     * @param session    WebSocket会话
     * @param taskId     任务ID
     * @param logMessage 日志消息
     */
    public void sendLog(WebSocketSession session, String taskId, String logMessage) {
        try {
            DockerWebSocketMessage logMsg = DockerWebSocketMessage.log(taskId, logMessage);
            safeSendMessage(session, new TextMessage(logMsg.toJson()), taskId);
        } catch (Exception e) {
            log.error("发送日志消息失败: taskId={}", taskId, e);
        }
    }

    /**
     * 发送带镜像名称的日志消息
     *
     * @param session    WebSocket会话
     * @param taskId     任务ID
     * @param logMessage 日志消息
     * @param imageName  镜像名称
     */
    public void sendLogWithImageName(WebSocketSession session, String taskId, String logMessage, String imageName) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("message", logMessage);
            data.put("imageName", imageName);
            
            DockerWebSocketMessage logMsg = new DockerWebSocketMessage();
            logMsg.setType(MessageType.LOG.name());
            logMsg.setTaskId(taskId);
            logMsg.setData(data);
            logMsg.setTimestamp(System.currentTimeMillis());
            
            safeSendMessage(session, new TextMessage(logMsg.toJson()), taskId);
        } catch (Exception e) {
            log.error("发送日志消息失败: taskId={}, imageName={}", taskId, imageName, e);
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
            DockerWebSocketMessage errorMsg = DockerWebSocketMessage.fail(taskId, errorMessage);
            safeSendMessage(session, new TextMessage(errorMsg.toJson()), taskId);
        } catch (Exception e) {
            log.error("发送错误消息失败: taskId={}, error={}", taskId, errorMessage, e);
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

    /**
     * 广播消息到所有活跃的WebSocket会话
     *
     * @param type 消息类型
     * @param data 消息数据
     */
    public void broadcastToAll(MessageType type, Object data) {
        try {
            DockerWebSocketMessage message = new DockerWebSocketMessage(type.name(), null, data);
            TextMessage textMessage = new TextMessage(message.toJson());
            
            // 获取所有活跃会话
            int broadcastCount = 0;
            for (WebSocketSession session : sessionManager.getAllActiveSessions()) {
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        broadcastCount++;
                    } catch (Exception e) {
                        log.warn("广播消息失败到会话: {}", session.getId(), e);
                    }
                }
            }
            
            log.debug("广播消息完成: type={}, 成功发送到 {} 个会话", type, broadcastCount);
            
        } catch (Exception e) {
            log.error("广播消息失败: type={}", type, e);
        }
    }

    /**
     * 发送Docker事件通知（广播到所有客户端）
     *
     * @param eventType 事件类型
     * @param containerId 容器ID
     * @param containerName 容器名称
     * @param message 通知消息
     */
    public void sendDockerEventNotification(String eventType, String containerId, String containerName, String message) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("eventType", eventType);
            data.put("containerId", containerId);
            data.put("containerName", containerName);
            data.put("message", message);
            data.put("timestamp", System.currentTimeMillis());
            
            broadcastToAll(MessageType.DOCKER_EVENT_NOTIFICATION, data);
            
        } catch (Exception e) {
            log.error("发送Docker事件通知失败: eventType={}, containerName={}", eventType, containerName, e);
        }
    }
} 