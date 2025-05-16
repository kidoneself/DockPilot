import {ws as wsClient} from '@/utils/websocket';
import type {
    DockerWebSocketCallbacks,
    PullImageParams,
    WebSocketMessage,
    WebSocketRequestOptions,
    WebSocketResponse
} from '@/api/model/websocketModel';
import {useNotificationStore} from '@/store/modules/notification';
import {MessagePlugin} from 'tdesign-vue-next';
import {generateTaskId} from '@/utils/taskId';
import {WebSocketMessageType} from '@/api/model/websocketModel';

/**
 * Docker WebSocket 服务
 * 专注于Docker业务逻辑和消息处理，底层连接管理由utils/websocket.ts提供
 */
export class DockerWebSocketService {
    private messageHandlerMap: Map<string, (message: WebSocketMessage) => void> = new Map();
    private typeHandlerMap: Map<string, (message: WebSocketMessage) => void> = new Map();
    private timeoutMap: Map<string, number> = new Map();

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
     * @param options 请求选项
     * @returns Promise<any> 返回处理结果
     */
    public async sendWebSocketMessage(options: WebSocketRequestOptions): Promise<void> {
        const taskId = generateTaskId(options.type);
      
        // 设置超时处理（如果配置了 timeout）
        if (options.timeout) {
          const timeoutId = window.setTimeout(() => {
            this.handleTimeout(taskId, options);
          }, options.timeout);
          this.timeoutMap.set(taskId, timeoutId);
        }
      
        const messageHandler = (message: WebSocketMessage) => {
            const ts = message.timestamp ?? Date.now(); // 处理 timestamp 可选性
          
            if (message.taskId !== taskId) return;
          
            if (message.type === WebSocketMessageType.START) {
              options.onStart?.(taskId);
              return;
            }
          
            //这是进度的
            if (message.type === WebSocketMessageType.PROGRESS) {
              options.onProgress?.(message.progress);
            }
          
            //其实这是日志的
            if (message.type === WebSocketMessageType.LOG) {
              options.onLog?.(message.data);
            }
          
            if (message.type === WebSocketMessageType.COMPLETE) {
              this.cleanup(taskId);
              options.onComplete?.(message.data);
            }
          
            if (message.type === WebSocketMessageType.ERROR) {
              this.cleanup(taskId);
              options.onError?.(message.errorMessage || '操作失败');
            }
          };
      
        this.addMessageHandler(taskId, messageHandler );
      
        try {
          // 发送消息
          await wsClient.connect();
          wsClient.send({
            type: options.type,
            taskId,
            data: options.data,
            timestamp: Date.now(),
          });
        } catch (error) {
          this.cleanup(taskId);
          options.onError?.('发送消息失败');
          throw error;
        }
      }

    /**
     * 处理超时
     */
    private handleTimeout(taskId: string, options: WebSocketRequestOptions): void {
        this.cleanup(taskId);
        options.onTimeout?.();
        options.onError?.('操作超时');
    }

    /**
     * 清理资源
     */
    private cleanup(taskId: string): void {
        // 移除消息处理器
        this.removeMessageHandler(taskId);
        
        // 清除超时定时器
        const timeoutId = this.timeoutMap.get(taskId);
        if (timeoutId) {
            window.clearTimeout(timeoutId);
            this.timeoutMap.delete(taskId);
        }
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
