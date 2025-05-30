package com.dockpilot.websocket.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * WebSocket 会话管理器
 * 负责管理活跃的 WebSocket 连接和任务会话的映射
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * 存储所有活跃的 WebSocket 会话
     * Key: sessionId, Value: WebSocketSession
     */
    private final ConcurrentMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * 存储任务ID到会话ID的映射
     * Key: taskId, Value: sessionId
     */
    private final ConcurrentMap<String, String> taskToSessionMap = new ConcurrentHashMap<>();

    /**
     * 存储最新的会话ID（用于新连接时的任务迁移）
     */
    private volatile String latestSessionId;

    /**
     * 添加新的 WebSocket 会话
     *
     * @param session WebSocket 会话
     */
    public void addSession(WebSocketSession session) {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        latestSessionId = sessionId;
        log.info("WebSocket 会话已添加: {}, 当前活跃会话数: {}", sessionId, activeSessions.size());
    }

    /**
     * 移除 WebSocket 会话
     *
     * @param sessionId 会话ID
     */
    public void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
        
        // 🔧 修复：不要立即清理任务映射，因为任务可能还在进行中
        // 任务映射会在 getSessionForTask 时自动迁移到新会话
        // taskToSessionMap.entrySet().removeIf(entry -> entry.getValue().equals(sessionId));
        
        log.info("WebSocket 会话已移除: {}, 当前活跃会话数: {}, 保留任务映射等待迁移", sessionId, activeSessions.size());
    }

    /**
     * 注册任务到会话的映射
     *
     * @param taskId    任务ID
     * @param sessionId 会话ID
     */
    public void registerTask(String taskId, String sessionId) {
        taskToSessionMap.put(taskId, sessionId);
        log.debug("任务已注册到会话: taskId={}, sessionId={}", taskId, sessionId);
    }

    /**
     * 获取任务对应的 WebSocket 会话
     * 如果原会话已断开，尝试迁移到最新的活跃会话
     *
     * @param taskId 任务ID
     * @return WebSocket 会话，如果没有可用会话则返回 null
     */
    public WebSocketSession getSessionForTask(String taskId) {
        log.debug("🔍 查找任务对应的会话: taskId={}", taskId);
        
        String sessionId = taskToSessionMap.get(taskId);
        
        if (sessionId != null) {
            log.debug("📋 找到任务映射: taskId={} → sessionId={}", taskId, sessionId);
            WebSocketSession session = activeSessions.get(sessionId);
            
            // 如果原会话还活跃，直接返回
            if (session != null && session.isOpen()) {
                log.debug("✅ 原会话仍然活跃: taskId={}, sessionId={}", taskId, sessionId);
                return session;
            }
            
            // 原会话已断开，尝试迁移到最新会话
            log.warn("⚠️ 任务 {} 的原会话 {} 已断开，尝试迁移到最新会话", taskId, sessionId);
            return migrateTaskToLatestSession(taskId);
        }
        
        // 没有注册的会话，使用最新会话
        log.warn("⚠️ 任务 {} 没有注册的会话映射，使用最新活跃会话", taskId);
        return getLatestActiveSession();
    }

    /**
     * 将任务迁移到最新的活跃会话
     *
     * @param taskId 任务ID
     * @return 最新的活跃会话，如果没有则返回 null
     */
    private WebSocketSession migrateTaskToLatestSession(String taskId) {
        WebSocketSession latestSession = getLatestActiveSession();
        
        if (latestSession != null) {
            String newSessionId = latestSession.getId();
            taskToSessionMap.put(taskId, newSessionId);
            log.info("✅ 任务 {} 已迁移到新会话: {}", taskId, newSessionId);
            return latestSession;
        }
        
        log.warn("❌ 没有可用的活跃会话来迁移任务: {}", taskId);
        return null;
    }

    /**
     * 获取最新的活跃会话
     *
     * @return 最新的活跃会话，如果没有则返回 null
     */
    public WebSocketSession getLatestActiveSession() {
        if (latestSessionId != null) {
            WebSocketSession session = activeSessions.get(latestSessionId);
            if (session != null && session.isOpen()) {
                return session;
            }
        }
        
        // 如果最新会话不可用，找一个可用的会话
        for (WebSocketSession session : activeSessions.values()) {
            if (session.isOpen()) {
                latestSessionId = session.getId();
                return session;
            }
        }
        
        return null;
    }

    /**
     * 获取活跃会话数量
     *
     * @return 活跃会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 清理无效的会话
     */
    public void cleanupInactiveSessions() {
        activeSessions.entrySet().removeIf(entry -> {
            WebSocketSession session = entry.getValue();
            if (!session.isOpen()) {
                String sessionId = entry.getKey();
                log.info("清理无效会话: {}", sessionId);
                return true;
            }
            return false;
        });
        
        // 清理指向无效会话的任务映射
        cleanupOrphanedTaskMappings();
    }
    
    /**
     * 清理指向无效会话的任务映射
     */
    private void cleanupOrphanedTaskMappings() {
        taskToSessionMap.entrySet().removeIf(entry -> {
            String sessionId = entry.getValue();
            WebSocketSession session = activeSessions.get(sessionId);
            
            // 如果会话不存在或已关闭，清理任务映射
            if (session == null || !session.isOpen()) {
                log.debug("清理孤立的任务映射: taskId={}, sessionId={}", entry.getKey(), sessionId);
                return true;
            }
            return false;
        });
    }
} 