package com.dsm.service.websocket;

import com.alibaba.fastjson.JSON;
import com.dsm.api.DockerService;
import com.dsm.model.ContainerCreateRequest;
import com.dsm.model.JsonContainerRequest;
import com.dsm.model.MessageType;
import com.dsm.service.http.ContainerService;
import com.dsm.utils.JsonContainerRequestToContainerCreateRequestConverter;
import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.WebSocketMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 容器服务
 * 处理所有容器相关的消息
 */
@Slf4j
@Service
public class ContainerWebSocketService implements BaseService {

    @Autowired
    private ContainerService containerService;

    @Autowired
    private WebSocketMessageSender messageSender;

    @Autowired
    private DockerService dockerService;

    /**
     * 处理WebSocket消息的主入口方法
     *
     * @param session WebSocket会话
     * @param message 接收到的消息
     */
    @Override
    public void handle(WebSocketSession session, DockerWebSocketMessage message) {
        MessageType type = MessageType.valueOf(message.getType());
        String taskId = message.getTaskId();

        try {
            // 处理消息
            Object result = null;
            switch (type) {
                case CONTAINER_LIST:           // 获取容器列表
                    result = handleContainerList();
                    break;
                case CONTAINER_DETAIL:         // 获取容器详情
                    result = handleContainerDetail(message);
                    break;
                case CONTAINER_START:          // 启动容器
                    result = handleContainerStart(message);
                    break;
                case CONTAINER_STOP:           // 停止容器
                    result = handleContainerStop(message);
                    break;
                case CONTAINER_RESTART:        // 重启容器
                    result = handleContainerRestart(message);
                    break;
                case CONTAINER_DELETE:         // 删除容器
                    result = handleContainerDelete(message);
                    break;
                case CONTAINER_UPDATE:         // 更新容器
                    result = handleContainerUpdate(message);
                    break;
                case CONTAINER_CREATE:         // 创建容器
                    result = handleContainerCreate(message);
                    break;
                case CONTAINER_LOGS:           // 获取容器日志
                    result = handleContainerLogs(message);
                    break;
                case CONTAINER_STATS:          // 获取容器状态
                    result = handleContainerStats(message);
                    break;
                case CONTAINER_STATE_CHANGE:   // 容器状态变更
                    result = handleContainerStateChange(message);
                    break;
                case CONTAINER_JSON_CONFIG:    // 获取容器配置
                    result = handleContainerJsonConfig(message);
                    break;
                default:
                    log.warn("未知的容器消息类型: {}", type);
            }

            // 发送完成消息
            messageSender.sendComplete(session, taskId, result);
        } catch (Exception e) {
            log.error("处理容器消息时发生错误: {}", type, e);
            messageSender.sendError(session, taskId, e.getMessage());
        }
    }

    /**
     * 处理获取容器列表的请求
     *
     * @return 容器列表
     */
    private Object handleContainerList() {
        return containerService.listContainers();
    }

    /**
     * 处理获取容器详情的请求
     *
     * @param message WebSocket消息
     * @return 容器详情信息
     */
    private Object handleContainerDetail(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        return containerService.getContainerConfig(containerId);
    }

    /**
     * 处理启动容器的请求
     *
     * @param message WebSocket消息
     * @return 启动操作结果
     */
    private Object handleContainerStart(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        containerService.startContainer(containerId);
        return null;
    }

    /**
     * 处理停止容器的请求
     *
     * @param message WebSocket消息
     * @return 停止操作结果
     */
    private Object handleContainerStop(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        containerService.stopContainer(containerId);
        return null;
    }

    /**
     * 处理重启容器的请求
     *
     * @param message WebSocket消息
     * @return 重启操作结果
     */
    private Object handleContainerRestart(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        containerService.restartContainer(containerId);
        return null;
    }

    /**
     * 处理删除容器的请求
     *
     * @param message WebSocket消息
     * @return 删除操作结果
     */
    private Object handleContainerDelete(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        containerService.removeContainer(containerId);
        return null;
    }

    /**
     * 处理更新容器的请求
     *
     * @param message WebSocket消息
     * @return 更新操作结果
     */
    private Object handleContainerUpdate(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        boolean onlyContainerId = data.containsKey("containerId") && data.size() == 1;
        if (onlyContainerId) {
            return containerService.updateContainerImage(containerId);
        }
        JsonContainerRequest json = JSON.parseObject(JSON.toJSONString(data), JsonContainerRequest.class);
        ContainerCreateRequest request = JsonContainerRequestToContainerCreateRequestConverter.convert(json);
        return containerService.updateContainer(containerId, request);
    }

    /**
     * 处理创建容器的请求
     *
     * @param message WebSocket消息
     * @return 创建操作结果
     */
    private Object handleContainerCreate(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        JsonContainerRequest json = JSON.parseObject(JSON.toJSONString(data), JsonContainerRequest.class);
        ContainerCreateRequest request = JsonContainerRequestToContainerCreateRequestConverter.convert(json);
        return containerService.createContainer(request);
    }

    /**
     * 处理获取容器日志的请求
     *
     * @param message WebSocket消息
     * @return 容器日志
     */
    private Object handleContainerLogs(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        return containerService.getContainerLogs(containerId, 100, false, false);
    }

    /**
     * 处理获取容器状态的请求
     *
     * @param message WebSocket消息
     * @return 容器状态信息
     */
    private Object handleContainerStats(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        return containerService.getContainerStats(containerId);
    }

    /**
     * 处理容器状态变更的请求
     *
     * @param message WebSocket消息
     * @return 状态变更结果
     */
    private Object handleContainerStateChange(DockerWebSocketMessage message) {
        // TODO: 实现容器状态变更
        return null;
    }

    /**
     * 处理获取容器配置的请求
     *
     * @param message WebSocket消息
     * @return 容器配置信息
     */
    private Object handleContainerJsonConfig(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        String jsonConfig = dockerService.generateJsonFromContainerId(containerId);
        return JSON.parseObject(jsonConfig);

    }
} 