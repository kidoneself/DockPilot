package com.dsm.websocket.model;

import com.alibaba.fastjson.JSON;
import com.dsm.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * WebSocket 消息封装体
 * <p>
 * 统一用于前后端消息传输结构。
 * 包含消息类型、任务 ID、消息体、时间戳等字段。
 *
 * @author dsm
 * @version 1.3
 * @since 2024-03-21
 */
@Data
@Schema(description = "WebSocket 消息结构体")
public class DockerWebSocketMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，使用枚举 WebSocketMessageType 定义所有类型
     */
    @Schema(
            description = "消息类型",
            example = "PULL_IMAGE",
            allowableValues = {
                    "PULL_IMAGE", "PUSH_IMAGE", "BUILD_IMAGE",
                    "CREATE_CONTAINER", "START_CONTAINER", "STOP_CONTAINER", "REMOVE_CONTAINER",
                    "COMPLETE", "ERROR", "PROGRESS", "LOG"
            }
    )
    private String type;

    /**
     * 任务 ID（用于标识一个完整流程）
     */
    @Schema(
            description = "任务 ID",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private String taskId;

    /**
     * 消息数据体，类型可由具体业务决定
     */
    @Schema(
            description = "消息数据（任意结构）",
            example = "{\"imageName\":\"nginx:latest\",\"tag\":\"latest\"}"
    )
    private Object data;

    /**
     * 时间戳（毫秒）
     */
    @Schema(
            description = "发送时间戳",
            example = "1647123456789",
            type = "integer",
            format = "int64"
    )
    private long timestamp;

    /**
     * 错误信息
     */
    @Schema(
            description = "错误信息",
            example = "Failed to pull image: nginx:latest"
    )
    private String errorMessage;

    /**
     * 进度信息（0-100）
     */
    @Schema(
            description = "进度信息",
            example = "50",
            minimum = "0",
            maximum = "100",
            type = "integer"
    )
    private Integer progress;

    /**
     * 无参构造函数
     */
    public DockerWebSocketMessage() {
        this.timestamp = System.currentTimeMillis();
        this.progress = 0;
    }

    /**
     * 构造函数
     */
    public DockerWebSocketMessage(String type, String taskId, Object data) {
        this.type = type;
        this.taskId = taskId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.progress = 0;
    }

    /**
     * 静态工厂方法，构造标准消息体
     */
    public static DockerWebSocketMessage of(String type, String taskId, Object data) {
        return new DockerWebSocketMessage(type, taskId, data);
    }

    /**
     * 快速创建成功消息
     */
    public static DockerWebSocketMessage complete(String taskId, Object data) {
        return new DockerWebSocketMessage(MessageType.COMPLETE.name(), taskId, data).success();
    }

    /**
     * 快速创建失败消息
     */
    public static DockerWebSocketMessage fail(String taskId, String errorMessage) {
        return new DockerWebSocketMessage(MessageType.ERROR.name(), taskId, null).error(errorMessage);
    }

    /**
     * 快速创建进度消息
     */
    public static DockerWebSocketMessage progress(String taskId, Integer progress) {
        return new DockerWebSocketMessage(MessageType.PROGRESS.name(), taskId, null).updateProgress(progress);
    }

    /**
     * 快速创建日志消息
     */
    public static DockerWebSocketMessage log(String taskId, String logMessage) {
        return new DockerWebSocketMessage(MessageType.LOG.name(), taskId, logMessage);
    }

    public String toJson() {
        return JSON.toJSONString(this);
    }

    /**
     * 设置成功状态（即进度100）
     */
    public DockerWebSocketMessage success() {
        this.progress = 100;
        return this;
    }

    /**
     * 设置错误状态
     */
    public DockerWebSocketMessage error(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * 更新进度
     */
    public DockerWebSocketMessage updateProgress(Integer progress) {
        this.progress = progress;
        return this;
    }
}