import { ref } from 'vue';
import { WebSocketClient } from '@/api/websocket/WebSocketClient';
import type { 
  WebSocketMessage, 
  PullImageParams, 
  PullImageProgress, 
  DockerWebSocketCallbacks,
  DockerImage,
  WebSocketMessageType
} from './types';
import { useNotificationStore } from '@/store/modules/notification';

export class DockerWebSocketService {
  private wsClient: WebSocketClient | null = null;
  private readonly wsUrl: string;
  private messageHandlers: Map<string, ((message: WebSocketMessage) => void)[]> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectTimeout = 3000;
  private heartbeatInterval: number | null = null;
  private readonly heartbeatIntervalTime = 30000; // 30秒发送一次心跳

  constructor() {
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    this.wsUrl = `${wsProtocol}//${window.location.host}/ws/docker`;
    this.initHeartbeat();
  }

  private initHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }
    this.heartbeatInterval = window.setInterval(() => {
      if (this.wsClient && this.wsClient.isConnected()) {
        this.sendMessage({
          type: 'HEARTBEAT',
          taskId: '',
          data: { timestamp: Date.now() }
        });
      }
    }, this.heartbeatIntervalTime);
  }

  public async connect(): Promise<void> {
    if (this.wsClient && this.wsClient.isConnected()) {
      return;
    }

    return new Promise<void>(async (resolve, reject) => {
      try {
        this.wsClient = new WebSocketClient({
          url: this.wsUrl,
          onMessage: (message: WebSocketMessage) => {
            console.log('收到WebSocket消息:', message);
            
            // 处理心跳响应
            if (message.type === 'HEARTBEAT_RESPONSE') {
              return;
            }
            
            // 处理测试通知消息
            if (message.type === 'TEST_NOTIFY_RESPONSE') {
              console.log('处理测试通知响应:', message);
              const notificationStore = useNotificationStore();
              notificationStore.handleWebSocketNotification(message.data);
            }
            
            // 处理其他消息
            const handlers = this.messageHandlers.get(message.type) || [];
            handlers.forEach(handler => handler(message));
          },
          onError: (error: Error) => {
            console.error('WebSocket错误:', error);
            this.handleReconnect();
          },
          onClose: () => {
            console.warn('WebSocket连接已关闭');
            this.handleReconnect();
          }
        });
        await this.wsClient.connect();
        this.reconnectAttempts = 0;
        this.initHeartbeat();
        resolve();
      } catch (error: unknown) {
        console.error('创建 WebSocket 连接时出错:', error);
        reject(error);
      }
    });
  }

  public on(type: string, handler: (message: WebSocketMessage) => void): void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, []);
    }
    this.messageHandlers.get(type)?.push(handler);
  }

  public off(type: string, handler: (message: WebSocketMessage) => void): void {
    const handlers = this.messageHandlers.get(type);
    if (handlers) {
      const index = handlers.indexOf(handler);
      if (index !== -1) {
        handlers.splice(index, 1);
      }
    }
  }

  public async send(message: WebSocketMessage): Promise<void> {
    if (!this.wsClient) {
      await this.connect();
    }
    this.wsClient?.send(message);
  }

  public async checkImages(images: { name: string; tag: string }[]): Promise<void> {
    await this.send({
      type: 'INSTALL_CHECK_IMAGES',
      taskId: '',
      data: { images }
    });
  }

  public async validateParams(params: any): Promise<void> {
    await this.send({
      type: 'INSTALL_VALIDATE',
      taskId: '',
      data: { params }
    });
  }

  public async pullImage(params: PullImageParams, callbacks: DockerWebSocketCallbacks): Promise<void> {
    try {
      // 创建 WebSocket 客户端
      this.wsClient = new WebSocketClient({
        url: this.wsUrl,
        onMessage: (message: WebSocketMessage) => {
          switch (message.type) {
            case 'PULL_START':
              callbacks.onStart?.(message.taskId);
              break;
            case 'PULL_PROGRESS':
              callbacks.onProgress?.(message.data);
              break;
            case 'PULL_COMPLETE':
              callbacks.onComplete?.();
              break;
            case 'ERROR':
              callbacks.onError?.(message.data.error);
              break;
            default:
              console.warn('未知的消息类型:', message.type);
          }
        },
        onError: (error: Error) => {
          callbacks.onError?.(error instanceof Error ? error.message : 'WebSocket错误');
        },
        onClose: () => {
          callbacks.onError?.('WebSocket连接已关闭');
        }
      });

      // 连接 WebSocket
      await this.wsClient.connect();

      // 发送拉取请求
      const pullRequest: WebSocketMessage = {
        type: 'PULL_IMAGE',
        taskId: '',
        data: params
      };

      this.wsClient.send(pullRequest);
    } catch (error: unknown) {
      callbacks.onError?.(error instanceof Error ? error.message : '拉取镜像失败');
      throw error;
    }
  }

  public async cancelPull(taskId: string): Promise<void> {
    if (!this.wsClient) {
      throw new Error('WebSocket未连接');
    }

    const cancelRequest: WebSocketMessage = {
      type: 'CANCEL_PULL',
      taskId,
      data: {}
    };

    this.wsClient.send(cancelRequest);
  }

  public async checkImageUpdates(images: DockerImage[]): Promise<void> {
    try {
      // 创建 WebSocket 客户端
      this.wsClient = new WebSocketClient({
        url: this.wsUrl,
        onMessage: (message: WebSocketMessage) => {
          switch (message.type) {
            case 'CHECK_UPDATES_COMPLETE':
              // 更新镜像列表中的更新状态
              if (message.data) {
                const updateInfo = message.data;
                images.forEach(img => {
                  const imageKey = `${img.name}:${img.tag}`;
                  const imageUpdateInfo = updateInfo[imageKey];
                  if (imageUpdateInfo) {
                    img.needUpdate = imageUpdateInfo.hasUpdate;
                    img.lastChecked = new Date().toISOString();
                  }
                });
              }
              break;
            case 'ERROR':
              console.error('检查更新失败:', message.data.error);
              break;
            default:
              console.warn('未知的消息类型:', message.type);
          }
        },
        onError: (error: Error) => {
          console.error('WebSocket错误:', error);
        },
        onClose: () => {
          console.warn('WebSocket连接已关闭');
        }
      });

      // 连接 WebSocket
      await this.wsClient.connect();

      // 发送检查更新请求
      const checkUpdatesRequest: WebSocketMessage = {
        type: 'CHECK_IMAGE_UPDATES',
        taskId: '',
        data: { images }
      };

      this.wsClient.send(checkUpdatesRequest);
    } catch (error: unknown) {
      console.error('检查镜像更新失败:', error);
      throw error;
    }
  }

  public disconnect(): void {
    this.wsClient?.disconnect();
    this.wsClient = null;
  }

  private handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      setTimeout(() => {
        this.connect();
      }, this.reconnectTimeout);
    } else {
      console.error('WebSocket 重连失败，已达到最大重试次数');
    }
  }

  private sendMessage(message: WebSocketMessage) {
    if (this.wsClient && this.wsClient.isConnected()) {
      this.wsClient.send(message);
    } else {
      console.error('WebSocket 未连接，无法发送消息');
    }
  }
}

// 创建单例实例
export const dockerWebSocketService = new DockerWebSocketService(); 