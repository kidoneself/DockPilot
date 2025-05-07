import {ws as wsClient} from '@/utils/websocket';
import type {
    DockerWebSocketCallbacks,
    PullImageParams,
    WebSocketMessage
} from '@/api/model/websocketModel';
import {useNotificationStore} from '@/store/modules/notification';
import {MessagePlugin} from 'tdesign-vue-next';

/**
 * Docker WebSocket 服务
 * 专注于Docker业务逻辑和消息处理，底层连接管理由utils/websocket.ts提供
 */
export class DockerWebSocketService {
    private messageHandlerMap: Map<string, (message: WebSocketMessage) => void> = new Map();
    private typeHandlerMap: Map<string, (message: WebSocketMessage) => void> = new Map();

    constructor() {
        // 设置消息处理器
        wsClient.setMessageHandler(this.handleMessage.bind(this));
    }

    /**
     * 处理消息分发
     */
    private handleMessage(message: WebSocketMessage): void {
        // 处理错误消息
        if (message.type === 'ERROR') {
            this.handleErrorMessage(message);
            return;
        }

        // 处理特定类型的消息
        if (this.typeHandlerMap.has(message.type)) {
            const handler = this.typeHandlerMap.get(message.type);
            handler?.(message);
        }

        // 处理特定任务的消息
        if (message.taskId && this.messageHandlerMap.has(message.taskId)) {
            const handler = this.messageHandlerMap.get(message.taskId);
            handler?.(message);
        }
    }

    /**
     * 处理错误消息
     */
    private handleErrorMessage(message: WebSocketMessage): void {
        const errorMessage = message.data?.message || '操作失败';
        
        if (message.taskId && this.messageHandlerMap.has(message.taskId)) {
            const handler = this.messageHandlerMap.get(message.taskId);
            handler?.(message);
            return;
        }

        MessagePlugin.error(errorMessage);
    }

    /**
     * 发送WebSocket消息
     */
    public async sendMessage(message: WebSocketMessage): Promise<void> {
        await wsClient.connect();
        wsClient.send(message);
    }

    /**
     * 添加消息处理器
     */
    public addMessageHandler(messageId: string, handler: (message: WebSocketMessage) => void): void {
        if (messageId.endsWith('_TYPE')) {
            // 如果是类型处理器，去掉 _TYPE 后缀
            const type = messageId.replace('_TYPE', '');
            this.typeHandlerMap.set(type, handler);
        } else {
            this.messageHandlerMap.set(messageId, handler);
        }
    }

    /**
     * 移除消息处理器
     */
    public removeMessageHandler(messageId: string): void {
        if (messageId.endsWith('_TYPE')) {
            const type = messageId.replace('_TYPE', '');
            this.typeHandlerMap.delete(type);
        } else {
            this.messageHandlerMap.delete(messageId);
        }
    }

    /**
     * 获取当前注册的处理器状态
     */
    public getRegisteredHandlersStatus(): Record<string, number> {
        return {
            'taskIdHandlers': this.messageHandlerMap.size,
            'typeHandlers': this.typeHandlerMap.size
        };
    }
}

// 创建单例实例
export const dockerWebSocketService = new DockerWebSocketService();
