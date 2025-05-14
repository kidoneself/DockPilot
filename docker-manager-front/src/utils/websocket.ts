import { MessagePlugin } from 'tdesign-vue-next';
import type { WebSocketMessage } from '@/api/model/websocketModel';
import { WebSocketMessageType } from '@/api/model/websocketModel';
import { useNotificationStore } from '@/store/modules/notification';

/**
 * WebSocket配置选项
 */
export interface WebSocketOptions {
  /** WebSocket URL */
  url: string;
  /** 收到消息时的回调 */
  onMessage?: (message: WebSocketMessage) => void;
  /** 发生错误时的回调 */
  onError?: (error: any) => void;
  /** 连接关闭时的回调 */
  onClose?: (event: CloseEvent) => void;
  /** 最大重连尝试次数 */
  maxReconnectAttempts?: number;
  /** 重连时间间隔（毫秒） */
  reconnectInterval?: number;
  /** 连接超时时间（毫秒） */
  connectionTimeout?: number;
  /** 心跳间隔时间（毫秒），设为0则禁用心跳 */
  heartbeatInterval?: number;
}

/**
 * WebSocket客户端类
 * 负责底层的WebSocket连接管理和消息处理
 */
export class WebSocketClient {
  private ws: WebSocket | null = null;
  private readonly url: string;
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 10;
  private readonly reconnectDelay = 3000;
  private reconnectTimer: number | null = null;
  private heartbeatInterval: number | null = null;
  private readonly heartbeatIntervalTime = 15000;
  private messageHandler: ((message: WebSocketMessage) => void) | null = null;

  constructor(url: string) {
    this.url = url;
  }

  /**
   * 设置消息处理器
   */
  public setMessageHandler(handler: (message: WebSocketMessage) => void): void {
    this.messageHandler = handler;
  }

  /**
   * 连接WebSocket服务器
   */
  public async connect(): Promise<void> {
    if (this.ws?.readyState === WebSocket.OPEN) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url);

        this.ws.onopen = () => {
          this.reconnectAttempts = 0;
          this.startHeartbeat();
          resolve();
        };

        this.ws.onclose = () => {
          this.stopHeartbeat();
          this.handleDisconnect();
        };

        this.ws.onerror = (error) => {
          this.stopHeartbeat();
          reject(error);
        };

        this.ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data) as WebSocketMessage;
            if (this.messageHandler) {
              this.messageHandler(message);
            }
          } catch (error) {
            console.error('解析WebSocket消息失败:', error);
          }
        };
      } catch (error) {
        reject(error);
      }
    });
  }

  /**
   * 发送消息
   */
  public send(message: WebSocketMessage): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket未连接');
    }
    this.ws.send(JSON.stringify(message));
  }

  /**
   * 断开连接
   */
  public disconnect(): void {
    this.stopHeartbeat();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  /**
   * 检查是否已连接
   */
  public isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  /**
   * 处理断开连接
   */
  private handleDisconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
      this.reconnectTimer = window.setTimeout(() => {
        this.connect().catch(() => {
          // 重连失败，继续尝试
        });
      }, delay);
    } else {
      const notificationStore = useNotificationStore();
      notificationStore.addNotification({
        id: String(Date.now()),
        content: 'WebSocket连接失败，请刷新页面重试',
        type: 'error',
        status: true,
        collected: false,
        date: new Date().toLocaleString(),
        quality: 'high'
      });
    }
  }

  /**
   * 启动心跳
   */
  private startHeartbeat(): void {
    this.stopHeartbeat();
    this.heartbeatInterval = window.setInterval(() => {
      if (this.isConnected()) {
        this.send({
          type: WebSocketMessageType.HEARTBEAT,
          taskId: '',
          data: { timestamp: Date.now() }
        });
      }
    }, this.heartbeatIntervalTime);
  }

  /**
   * 停止心跳
   */
  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }
}

// 创建WebSocket客户端实例
const url = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/docker`;
export const ws = new WebSocketClient(url); 
