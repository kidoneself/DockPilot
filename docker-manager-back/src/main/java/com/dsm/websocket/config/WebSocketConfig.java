package com.dsm.websocket.config;

import com.dsm.websocket.handler.DockerWebSocketHandler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 
 * @author dsm
 * @version 1.0
 * @since 2024-03-21
 */
@Tag(name = "WebSocket配置", description = "WebSocket相关配置")
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private DockerWebSocketHandler dockerWebSocketHandler;
    
    /**
     * 注册WebSocket处理器
     * 
     * @param registry WebSocket处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(dockerWebSocketHandler, "/ws/docker")
               .setAllowedOrigins("*");
    }
} 