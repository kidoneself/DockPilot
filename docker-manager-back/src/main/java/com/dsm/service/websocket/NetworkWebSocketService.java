package com.dsm.service.websocket;

import com.dsm.model.MessageType;
import com.dsm.service.http.NetworkService;
import com.dsm.utils.ErrorMessageExtractor;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.WebSocketMessageSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 网络服务
 * 处理所有网络相关的消息
 */
@Slf4j
@Service
@Tag(name = "网络 WebSocket 服务", description = "处理网络相关的 WebSocket 消息")
public class NetworkWebSocketService implements BaseService {

    @Autowired
    private NetworkService networkService;

    @Autowired
    private WebSocketMessageSender messageSender;

    /**
     * 处理WebSocket消息的主入口方法
     *
     * @param session WebSocket会话
     * @param message 接收到的消息
     */
    @Override
    @Operation(
            summary = "处理网络相关的WebSocket消息",
            description = "根据消息类型处理不同的网络操作，包括：\n" +
                    "- NETWORK_LIST: 获取网络列表\n" +
                    "- NETWORK_DETAIL: 获取网络详情\n" +
                    "- NETWORK_CREATE: 创建网络\n" +
                    "- NETWORK_DELETE: 删除网络"
    )
    public void handle(
            @Parameter(description = "WebSocket会话") WebSocketSession session,
            @Parameter(description = "接收到的消息") DockerWebSocketMessage message
    ) {
        MessageType type = MessageType.valueOf(message.getType());
        String taskId = message.getTaskId();

        try {
            // 处理消息
            Object result = null;
            switch (type) {
                case NETWORK_LIST:           // 获取网络列表
                    result = handleNetworkList();
                    break;
                case NETWORK_DETAIL:         // 获取网络详情
                    result = handleNetworkDetail(message);
                    break;
                case NETWORK_CREATE:         // 创建网络
                    result = handleNetworkCreate(message);
                    break;
                case NETWORK_DELETE:         // 删除网络
                    result = handleNetworkDelete(message);
                    break;
                default:
                    log.warn("未知的网络消息类型: {}", type);
            }

            // 发送完成消息
            messageSender.sendComplete(session, taskId, result);
        } catch (Exception e) {
            log.error("处理网络消息时发生错误: {}", type, e);
            String userFriendlyError = ErrorMessageExtractor.extractUserFriendlyError(e);
            messageSender.sendError(session, taskId, userFriendlyError);
        }
    }

    /**
     * 处理获取网络列表的请求
     *
     * @return 网络列表
     */
    private Object handleNetworkList() {
        return networkService.listNetworks();
    }

    /**
     * 处理获取网络详情的请求
     *
     * @param message WebSocket消息
     * @return 网络详情信息
     */
    private Object handleNetworkDetail(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String networkId = (String) data.get("networkId");
        return networkService.getNetworkDetail(networkId);
    }

    /**
     * 处理创建网络的请求
     *
     * @param message WebSocket消息
     * @return 创建结果
     */
    private Object handleNetworkCreate(DockerWebSocketMessage message) {
        // TODO: 实现创建网络
        return null;
    }

    /**
     * 处理删除网络的请求
     *
     * @param message WebSocket消息
     * @return 删除结果
     */
    private Object handleNetworkDelete(DockerWebSocketMessage message) {
        // TODO: 实现删除网络
        return null;
    }
} 