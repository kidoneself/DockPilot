package com.dockpilot.service.websocket;

import com.dockpilot.model.MessageType;
import com.dockpilot.model.SystemStatusDTO;
import com.dockpilot.service.http.SystemStatusService;
import com.dockpilot.utils.LogUtil;
import com.dockpilot.websocket.model.DockerWebSocketMessage;
import com.dockpilot.websocket.sender.WebSocketMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

/**
 * 系统WebSocket服务
 * 处理系统状态相关的WebSocket消息
 */
@Service
public class SystemWebSocketService implements BaseService {

    @Autowired
    private SystemStatusService systemStatusService;

    @Autowired
    private WebSocketMessageSender messageSender;

    @Override
    public void handle(WebSocketSession session, DockerWebSocketMessage message) {
        MessageType type = MessageType.valueOf(message.getType());
        String taskId = message.getTaskId();

        LogUtil.logSysInfo("处理系统WebSocket消息: " + type);

        try {
            Object result = null;
            switch (type) {
                case SYSTEM_STATUS:
                    result = handleSystemStatus(message);
                    break;
                default:
                    LogUtil.logSysError("未知的系统消息类型: " + type);
                    messageSender.sendError(session, taskId, "未知的系统消息类型: " + type);
                    return;
            }

            // 发送完成消息
            messageSender.sendComplete(session, taskId, result);
        } catch (Exception e) {
            LogUtil.logSysError("处理系统WebSocket消息时发生错误: " + type + ", 错误: " + e.getMessage());
            messageSender.sendError(session, taskId, e.getMessage());
        }
    }

    /**
     * 处理获取系统状态的请求
     *
     * @param message WebSocket消息
     * @return 系统状态信息
     */
    private Object handleSystemStatus(DockerWebSocketMessage message) {
        LogUtil.logSysInfo("获取系统状态信息");
        SystemStatusDTO systemStatus = systemStatusService.getSystemStatus();
        LogUtil.logSysInfo("系统状态信息获取完成");
        return systemStatus;
    }
} 