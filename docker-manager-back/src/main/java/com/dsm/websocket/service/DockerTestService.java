package com.dsm.websocket.service;

import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.DockerWebSocketMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class DockerTestService {
    
    @Autowired
    private DockerWebSocketMessageSender messageSender;
    
    public void handleTestNotify(WebSocketSession session, DockerWebSocketMessage message) {
        try {
            log.info("收到测试消息: {}", message);
            
            // 获取消息数据
            @SuppressWarnings("unchecked") 
            Map<String, Object> data = (Map<String, Object>) message.getData();
            String testMessage = data != null && data.containsKey("message") 
                ? (String) data.get("message") 
                : "默认测试消息";
            
            // 构造符合前端通知格式的响应消息
            Map<String, Object> notificationData = Map.of(
                "id", UUID.randomUUID().toString(),
                "content", testMessage,
                "type", "系统通知",
                "status", true,
                "collected", false,
                "date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                "quality", "high"
            );
            
            // 发送通知消息
            messageSender.sendMessage(session, new DockerWebSocketMessage(
                "TEST_NOTIFY_RESPONSE",
                message.getTaskId() != null ? message.getTaskId() : UUID.randomUUID().toString(),
                notificationData
            ));
            
            log.info("测试消息处理完成");
        } catch (Exception e) {
            log.error("处理测试消息时发生错误", e);
            messageSender.sendErrorMessage(session, "处理测试消息失败: " + e.getMessage());
        }
    }
} 