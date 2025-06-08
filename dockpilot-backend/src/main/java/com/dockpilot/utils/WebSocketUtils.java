package com.dockpilot.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket工具类
 * 提供WebSocket相关的通用功能
 */
@Slf4j
public class WebSocketUtils {

    /**
     * 🎯 从WebSocket会话获取客户端真实IP
     * 
     * @param session WebSocket会话
     * @return 客户端IP地址，获取失败返回"localhost"
     */
    public static String getClientIp(WebSocketSession session) {
        if (session == null) {
            log.warn("WebSocket会话为空，返回localhost");
            return "localhost";
        }
        
        try {
            // 尝试多种方式获取客户端IP
            String clientIp = extractClientIpFromSession(session);
            
            if (clientIp != null && !clientIp.trim().isEmpty()) {
                log.debug("获取到客户端IP: {}", clientIp);
                return clientIp;
            }
            
            // 如果无法获取，使用备用方案
            log.warn("无法获取客户端真实IP，使用localhost");
            return "localhost";
            
        } catch (Exception e) {
            log.error("获取客户端IP时发生异常: {}", e.getMessage(), e);
            return "localhost";
        }
    }

    /**
     * 🔍 从WebSocket会话中提取客户端IP
     * 按优先级尝试多种方式
     */
    private static String extractClientIpFromSession(WebSocketSession session) {
        // 方法1: 尝试从RemoteAddress获取
        String remoteIp = getRemoteAddressIp(session);
        if (isValidClientIp(remoteIp)) {
            return remoteIp;
        }
        
        // 方法2: 尝试从HandshakeHeaders获取代理转发的真实IP
        String forwardedIp = getForwardedIp(session);
        if (isValidClientIp(forwardedIp)) {
            return forwardedIp;
        }
        
        // 方法3: 尝试从X-Real-IP头获取
        String realIp = getRealIp(session);
        if (isValidClientIp(realIp)) {
            return realIp;
        }
        
        // 方法4: 尝试从Host头获取访问地址
        String hostIp = getHostIp(session);
        if (isValidClientIp(hostIp)) {
            return hostIp;
        }
        
        return null;
    }

    /**
     * 🔹 方法1: 从RemoteAddress获取IP
     */
    private static String getRemoteAddressIp(WebSocketSession session) {
        try {
            if (session.getRemoteAddress() != null) {
                return session.getRemoteAddress().getAddress().getHostAddress();
            }
        } catch (Exception e) {
            log.debug("从RemoteAddress获取IP失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 🔹 方法2: 从X-Forwarded-For头获取IP
     */
    private static String getForwardedIp(WebSocketSession session) {
        try {
            org.springframework.http.HttpHeaders headers = session.getHandshakeHeaders();
            if (headers != null) {
                String xForwardedFor = headers.getFirst("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
                    // X-Forwarded-For可能包含多个IP，取第一个（最原始的客户端IP）
                    return xForwardedFor.split(",")[0].trim();
                }
            }
        } catch (Exception e) {
            log.debug("从X-Forwarded-For获取IP失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 🔹 方法3: 从X-Real-IP头获取IP
     */
    private static String getRealIp(WebSocketSession session) {
        try {
            org.springframework.http.HttpHeaders headers = session.getHandshakeHeaders();
            if (headers != null) {
                return headers.getFirst("X-Real-IP");
            }
        } catch (Exception e) {
            log.debug("从X-Real-IP获取IP失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 🔹 方法4: 从Host头获取IP
     */
    private static String getHostIp(WebSocketSession session) {
        try {
            org.springframework.http.HttpHeaders headers = session.getHandshakeHeaders();
            if (headers != null) {
                String host = headers.getFirst("Host");
                if (host != null && !host.trim().isEmpty()) {
                    // Host可能包含端口，需要提取IP部分
                    return host.split(":")[0].trim();
                }
            }
        } catch (Exception e) {
            log.debug("从Host头获取IP失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 🔍 验证IP是否为有效的客户端IP
     * 排除本地回环地址和空值
     */
    private static boolean isValidClientIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        String trimmedIp = ip.trim();
        
        // 排除本地回环地址
        if ("127.0.0.1".equals(trimmedIp) || 
            "localhost".equals(trimmedIp) || 
            "0:0:0:0:0:0:0:1".equals(trimmedIp) ||
            "::1".equals(trimmedIp)) {
            return false;
        }
        
        // 排除明显无效的IP
        if ("0.0.0.0".equals(trimmedIp) || "unknown".equalsIgnoreCase(trimmedIp)) {
            return false;
        }
        
        return true;
    }

    /**
     * 🎯 获取客户端访问地址的基础URL（用于构建服务访问地址）
     * 
     * @param session WebSocket会话
     * @param port 服务端口
     * @return 完整的访问URL，如: http://192.168.1.100:8080
     */
    public static String buildServiceUrl(WebSocketSession session, String port) {
        String clientIp = getClientIp(session);
        return "http://" + clientIp + ":" + port;
    }

    /**
     * 🎯 获取客户端访问地址的基础URL（用于构建服务访问地址）
     * 
     * @param session WebSocket会话
     * @param port 服务端口
     * @return 完整的访问URL，如: http://192.168.1.100:8080
     */
    public static String buildServiceUrl(WebSocketSession session, int port) {
        return buildServiceUrl(session, String.valueOf(port));
    }
} 