package com.dsm.websocket.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

/**
 * WebSocket消息类
 * 
 * @author dsm
 * @version 1.0
 * @since 2024-03-21
 */
@Data
@Schema(description = "WebSocket消息")
public class DockerWebSocketMessage {
    
    /**
     * 消息类型
     * 现有类型：
     * - PULL_IMAGE: 拉取镜像
     * 
     * 安装功能新增类型：
     * - INSTALL_CHECK_IMAGES: 检查安装所需的镜像
     * - INSTALL_PULL_IMAGE: 拉取安装所需的镜像
     * - INSTALL_VALIDATE: 验证安装参数
     * - INSTALL_START: 开始安装
     * - INSTALL_PROGRESS: 安装进度
     * - INSTALL_LOG: 安装日志
     */
    @Schema(description = "消息类型", example = "PULL_IMAGE")
    private String type;
    
    /**
     * 任务ID，用于标识一个完整的安装流程
     */
    @Schema(description = "任务ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String taskId;
    
    /**
     * 消息数据
     */
    @Schema(description = "消息数据")
    private Object data;
    
    @Schema(description = "时间戳", example = "1647123456789")
    private long timestamp;
    
    /**
     * 无参构造函数
     */
    public DockerWebSocketMessage() {
    }
    
    /**
     * 构造函数
     * 
     * @param type 消息类型
     * @param taskId 任务ID
     * @param data 消息数据
     */
    public DockerWebSocketMessage(String type, String taskId, Object data) {
        this.type = type;
        this.taskId = taskId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
} 