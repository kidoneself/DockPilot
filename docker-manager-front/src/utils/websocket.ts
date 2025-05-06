import { MessagePlugin } from 'tdesign-vue-next';
import type { WebSocketMessage as BusinessWebSocketMessage, WebSocketMessageType } from '@/api/model/websocketModel';

/**
 * 基础WebSocket消息接口
 * 此处使用通用接口，特定业务消息使用api/model/websocketModel.ts中的定义
 */
export interface BaseWebSocketMessage {
  type: WebSocketMessageType | string;
  taskId?: string;
  data: any;
}

/**
 * WebSocket配置选项
 */
export interface WebSocketOptions {
  /** WebSocket URL */
  url: string;
  /** 收到消息时的回调 */
  onMessage?: (message: BaseWebSocketMessage | BusinessWebSocketMessage) => void;
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
 * 提供WebSocket连接的基础能力，包括：
 * - 连接管理
 * - 自动重连
 * - 心跳检测
 * - 消息队列
 * - 错误处理
 */
export class WebSocketClient {
  private ws: WebSocket | null = null;
  private connected = false;
  private connectionAttempts = 0;
  private messageQueue: any[] = [];
  private readonly maxReconnectAttempts: number;
  private reconnectInterval: number;
  private readonly connectionTimeout: number;
  private readonly url: string;
  private readonly onMessage?: (message: BaseWebSocketMessage | BusinessWebSocketMessage) => void;
  private readonly onError?: (error: any) => void;
  private readonly onClose?: (event: CloseEvent) => void;
  private connectionTimeoutId?: number;
  private heartbeatIntervalId?: number;
  private readonly heartbeatIntervalTime: number;
  private messageEventListeners: ((event: MessageEvent) => void)[] = [];

  /**
   * 创建WebSocket客户端实例
   * @param options WebSocket配置选项
   */
  constructor(options: WebSocketOptions) {
    this.url = options.url;
    this.onMessage = options.onMessage;
    this.onError = options.onError;
    this.onClose = options.onClose;
    this.maxReconnectAttempts = options.maxReconnectAttempts || 5;
    this.reconnectInterval = options.reconnectInterval || 3000;
    this.connectionTimeout = options.connectionTimeout || 5000;
    this.heartbeatIntervalTime = options.heartbeatInterval || 30000;
  }

  /**
   * 连接到WebSocket服务器
   * @returns 连接成功时解析的Promise
   */
  public connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.disconnect();

      if (this.connectionAttempts >= this.maxReconnectAttempts) {
        const error = new Error('达到最大重连次数');
        this.handleError(error);
        reject(error);
        return;
      }

      this.connectionAttempts++;

      try {
        this.ws = new WebSocket(this.url);

        // 设置连接超时
        this.connectionTimeoutId = window.setTimeout(() => {
          if (this.ws && this.ws.readyState !== WebSocket.OPEN) {
            this.ws.close();
            const error = new Error('WebSocket 连接超时');
            this.handleError(error);
            reject(error);
          }
        }, this.connectionTimeout);

        this.ws.onopen = () => {
          this.connected = true;
          this.connectionAttempts = 0;
          clearTimeout(this.connectionTimeoutId);
          this.startHeartbeat();
          this.processMessageQueue();
          resolve();
        };

        this.ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data) as BaseWebSocketMessage;
            this.logMessageListeners();
            this.onMessage?.(message);
            
            // 手动调用所有注册的消息事件监听器
            for (const listener of this.messageEventListeners) {
              try {
                listener(event);
              } catch (err) {
                // 监听器执行出错
              }
            }
          } catch (e) {
            this.handleError(e);
          }
        };

        this.ws.onerror = (error) => {
          this.handleError(error);
          reject(error);
        };

        this.ws.onclose = (event) => {
          clearTimeout(this.connectionTimeoutId);
          this.stopHeartbeat();
          
          if (this.connected) {
            this.connected = false;
            this.onClose?.(event);
            this.handleReconnect();
          }
        };
      } catch (error) {
        this.handleError(error);
        reject(error);
      }
    });
  }

  /**
   * 断开WebSocket连接
   */
  public disconnect(): void {
    // 先移除所有消息事件监听器
    this.removeAllEventListeners('message');
    
    if (this.ws) {
      try {
        if (this.ws.readyState === WebSocket.OPEN) {
          this.ws.close();
        }
      } catch (e) {
        // 断开连接失败
      }
      this.ws = null;
    }
    this.connected = false;
    clearTimeout(this.connectionTimeoutId);
    this.stopHeartbeat();
  }

  /**
   * 发送消息到WebSocket服务器
   * 如果连接尚未建立，消息会被添加到队列等待发送
   * @param message 要发送的消息
   */
  public send(message: any): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      this.messageQueue.push(message);
    }
  }

  /**
   * 检查WebSocket是否已连接
   * @returns 连接状态
   */
  public isConnected(): boolean {
    return this.connected && this.ws?.readyState === WebSocket.OPEN;
  }

  /**
   * 添加自定义事件监听器
   * @param type 事件类型
   * @param listener 事件监听器
   */
  public addEventListener(type: 'message', listener: (event: MessageEvent) => void): void {
    // 防止重复添加相同的监听器
    const index = this.messageEventListeners.findIndex(
      existingListener => existingListener.toString() === listener.toString()
    );
    
    if (index === -1) {
      this.messageEventListeners.push(listener);
      this.ws?.addEventListener(type, listener as EventListener);
    }
  }

  /**
   * 移除自定义事件监听器
   * @param type 事件类型
   * @param listener 事件监听器
   */
  public removeEventListener(type: 'message', listener: (event: MessageEvent) => void): void {
    // 移除指定的监听器
    const index = this.messageEventListeners.findIndex(
      existingListener => existingListener.toString() === listener.toString()
    );
    
    if (index !== -1) {
      this.messageEventListeners.splice(index, 1);
      this.ws?.removeEventListener(type, listener as EventListener);
    }
  }
  
  /**
   * 移除所有消息事件监听器
   * @param type 事件类型
   */
  public removeAllEventListeners(type: 'message'): void {
    // 逐个移除所有监听器
    const listeners = [...this.messageEventListeners];
    for (const listener of listeners) {
      this.ws?.removeEventListener(type, listener as EventListener);
    }
    this.messageEventListeners = [];
  }
  
  /**
   * 记录当前消息监听器的状态
   */
  private logMessageListeners(): void {
    // 记录监听器数量
  }
  
  /**
   * 启动心跳检测
   */
  private startHeartbeat(): void {
    if (this.heartbeatIntervalTime <= 0) return;
    
    this.stopHeartbeat();
    
    this.heartbeatIntervalId = window.setInterval(() => {
      if (this.isConnected()) {
        this.send({
          type: 'HEARTBEAT',
          data: { timestamp: Date.now() }
        });
      } else {
        this.stopHeartbeat();
      }
    }, this.heartbeatIntervalTime);
  }
  
  /**
   * 停止心跳检测
   */
  private stopHeartbeat(): void {
    if (this.heartbeatIntervalId) {
      clearInterval(this.heartbeatIntervalId);
      this.heartbeatIntervalId = undefined;
    }
  }
  
  /**
   * 处理消息队列
   */
  private processMessageQueue(): void {
    while (this.messageQueue.length > 0 && this.isConnected()) {
      const message = this.messageQueue.shift();
      if (message) {
        this.send(message);
      }
    }
  }
  
  /**
   * 处理重连逻辑
   */
  private handleReconnect(): void {
    if (this.connectionAttempts < this.maxReconnectAttempts) {
      setTimeout(() => {
        this.connect().catch(() => {
          // 重连失败，会在connect中处理错误
        });
      }, this.reconnectInterval);
      
      // 每次重连增加等待时间，最大等待时间为30秒
      this.reconnectInterval = Math.min(this.reconnectInterval * 1.5, 30000);
    } else {
      // 达到最大重连次数，通知用户
      MessagePlugin.error('网络连接断开，请刷新页面重试');
    }
  }
  
  /**
   * 处理WebSocket错误
   * @param error 错误对象
   */
  private handleError(error: any): void {
    this.onError?.(error);
  }
}

// 创建WebSocket实例
const url = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/docker`;

// 导出单例
export const ws = new WebSocketClient({
  url,
  maxReconnectAttempts: 5,
  reconnectInterval: 3000,
  connectionTimeout: 5000,
  heartbeatInterval: 30000
}); 