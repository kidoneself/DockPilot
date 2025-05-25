package com.dsm.service.websocket;

import com.alibaba.fastjson.JSON;
import com.dsm.api.DockerService;
import com.dsm.model.ContainerCreateRequest;
import com.dsm.model.ContainerInfo;
import com.dsm.model.JsonContainerRequest;
import com.dsm.model.MessageType;
import com.dsm.service.http.ContainerInfoService;
import com.dsm.service.http.ContainerService;
import com.dsm.service.http.ContainerSyncService;
import com.dsm.service.http.NetworkService;
import com.dsm.utils.AsyncTaskRunner;
import com.dsm.utils.ErrorMessageExtractor;
import com.dsm.utils.JsonContainerRequestToContainerCreateRequestConverter;
import com.dsm.utils.MessageCallback;
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
import java.util.concurrent.CompletableFuture;

/**
 * 容器服务
 * 处理所有容器相关的消息
 */
@Slf4j
@Service
@Tag(name = "容器 WebSocket 服务", description = "处理容器相关的 WebSocket 消息")
public class ContainerWebSocketService implements BaseService {

    @Autowired
    private ContainerService containerService;

    @Autowired
    private WebSocketMessageSender messageSender;

    @Autowired
    private DockerService dockerService;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private ContainerInfoService containerInfoService;

    @Autowired
    private ContainerSyncService containerSyncService;

    /**
     * 处理WebSocket消息的主入口方法
     *
     * @param session WebSocket会话
     * @param message 接收到的消息
     */
    @Override
    @Operation(
            summary = "处理容器相关的WebSocket消息",
            description = "根据消息类型处理不同的容器操作，包括：\n" +
                    "- CONTAINER_LIST: 获取容器列表\n" +
                    "- CONTAINER_DETAIL: 获取容器详情\n" +
                    "- CONTAINER_START: 启动容器\n" +
                    "- CONTAINER_STOP: 停止容器\n" +
                    "- CONTAINER_RESTART: 重启容器\n" +
                    "- CONTAINER_DELETE: 删除容器\n" +
                    "- CONTAINER_UPDATE: 更新容器\n" +
                    "- CONTAINER_CREATE: 创建容器\n" +
                    "- CONTAINER_LOGS: 获取容器日志\n" +
                    "- CONTAINER_STATS: 获取容器状态\n" +
                    "- CONTAINER_STATE_CHANGE: 容器状态变更\n" +
                    "- CONTAINER_JSON_CONFIG: 获取容器配置\n" +
                    "- NETWORK_DETAIL: 获取网络详情\n" +
                    "- CONTAINER_UPDATE_INFO: 更新容器信息\n" +
                    "- CONTAINER_CLEANUP_DUPLICATES: 清理重复记录"
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
                case CONTAINER_LIST:           // 获取容器列表
                    result = handleContainerList();
                    break;
                case CONTAINER_DETAIL:         // 获取容器详情
                    result = handleContainerDetail(message);
                    break;
                case CONTAINER_START:          // 启动容器
                    AsyncTaskRunner.runWithoutResult(() -> {
                        handleContainerStart(message);
                        return CompletableFuture.completedFuture(null);
                    }, messageSender, session, taskId);
                    return; // 不发送同步响应
                case CONTAINER_STOP:           // 停止容器
                    AsyncTaskRunner.runWithoutResult(() -> {
                        handleContainerStop(message);
                        return CompletableFuture.completedFuture(null);
                    }, messageSender, session, taskId);
                    return; // 不发送同步响应
                case CONTAINER_RESTART:        // 重启容器
                    AsyncTaskRunner.runWithoutResult(() -> {
                        handleContainerRestart(message);
                        return CompletableFuture.completedFuture(null);
                    }, messageSender, session, taskId);
                    return; // 不发送同步响应
                case CONTAINER_DELETE:         // 删除容器
                    AsyncTaskRunner.runWithoutResult(() -> {
                        handleContainerDelete(message);
                        return CompletableFuture.completedFuture(null);
                    }, messageSender, session, taskId);
                    return; // 不发送同步响应
                case CONTAINER_UPDATE:         // 更新容器
                    AsyncTaskRunner.runWithResult(() -> {
                        return handleContainerUpdate(message, new MessageCallback() {
                            @Override
                            public void onProgress(int progress) {
                                System.out.println("进度: " + progress + "%");
                                messageSender.sendProgress(session, taskId, progress);
                            }

                            @Override
                            public void onLog(String log) {
                                System.out.println("日志: " + log);
                                messageSender.sendLog(session, taskId, log);
                            }

                            @Override
                            public void onComplete() {
                                System.out.println("容器更新完成！");
                            }

                            @Override
                            public void onError(String error) {
                                System.err.println("错误: " + error);
                                throw new RuntimeException(error);
                            }
                        });
                    }, messageSender, session, taskId);
                    return; // 不发送同步响应
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
                case NETWORK_DETAIL:           // 获取网络详情
                    result = handleNetworkDetail(message);
                    break;
                case CONTAINER_UPDATE_INFO:    // 更新容器信息
                    result = handleUpdateContainerInfo(message);
                    break;
                case CONTAINER_CLEANUP_DUPLICATES: // 清理重复记录
                    AsyncTaskRunner.runWithoutResult(() -> {
                        handleCleanupDuplicates();
                        return CompletableFuture.completedFuture(null);
                    }, messageSender, session, taskId);
                    return; // 不发送同步响应
                default:
                    log.warn("未知的容器消息类型: {}", type);
            }

            // 发送完成消息
            messageSender.sendComplete(session, taskId, result);
        } catch (Exception e) {
            log.error("处理容器消息时发生错误: {}", type, e);
            String userFriendlyError = ErrorMessageExtractor.extractUserFriendlyError(e);
            messageSender.sendError(session, taskId, userFriendlyError);
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
    private void handleContainerStart(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        containerService.startContainer(containerId);
    }

    /**
     * 处理停止容器的请求
     *
     * @param message WebSocket消息
     * @return 停止操作结果
     */
    private void handleContainerStop(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        containerService.stopContainer(containerId);
    }

    /**
     * 处理重启容器的请求
     *
     * @param message WebSocket消息
     * @return 重启操作结果
     */
    private void handleContainerRestart(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        containerService.restartContainer(containerId);
    }

    /**
     * 处理删除容器的请求
     *
     * @param message WebSocket消息
     * @return 删除操作结果
     */
    private void handleContainerDelete(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        containerService.removeContainer(containerId);
    }

    /**
     * 处理更新容器的请求
     *
     * @param message WebSocket消息
     * @return 更新操作结果
     */
    private CompletableFuture<String> handleContainerUpdate(DockerWebSocketMessage message, MessageCallback callback) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        boolean onlyContainerId = data.containsKey("containerId") && data.size() == 1;
        if (onlyContainerId) {
            return containerService.updateContainerImage(containerId, callback);
        }
        JsonContainerRequest json = JSON.parseObject(JSON.toJSONString(data.get("config")), JsonContainerRequest.class);
        ContainerCreateRequest request = JsonContainerRequestToContainerCreateRequestConverter.convert(json);
        return containerService.updateContainer(containerId, request, callback);
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
     * 处理更新容器信息的请求
     *
     * @param message WebSocket消息
     * @return 更新操作结果
     */
    private Object handleUpdateContainerInfo(DockerWebSocketMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String containerId = (String) data.get("containerId");
        String webUrl = (String) data.get("webUrl");
        String iconUrl = (String) data.get("iconUrl");

        // 获取现有的容器信息
        ContainerInfo containerInfo = containerInfoService.getContainerInfoByContainerId(containerId);
        if (containerInfo == null) {
            throw new RuntimeException("容器信息不存在: " + containerId);
        }

        // 更新字段
        containerInfo.setWebUrl(webUrl);
        containerInfo.setIconUrl(iconUrl);

        // 保存更新
        containerInfoService.updateContainerInfo(containerInfo);

        return "更新成功";
    }

    /**
     * 处理清理容器重复记录的请求
     */
    private void handleCleanupDuplicates() {
        log.info("开始清理容器重复记录...");
        containerSyncService.cleanupDuplicateRecords();
        log.info("容器重复记录清理完成");
    }
} 