import { MessagePlugin } from 'tdesign-vue-next';
import type { WebSocketMessage, WebSocketOptions } from './types';

export class WebSocketClient {
  private ws: WebSocket | null = null;
  private connected = false;
  private connectionAttempts = 0;
  private readonly maxReconnectAttempts: number;
  private reconnectInterval: number;
  private readonly connectionTimeout: number;
  private readonly url: string;
  private readonly onMessage?: (message: WebSocketMessage) => void;
  private readonly onError?: (error: any) => void;
  private readonly onClose?: (event: CloseEvent) => void;
  private connectionTimeoutId?: NodeJS.Timeout;

  constructor(options: WebSocketOptions) {
    this.url = options.url;
    this.onMessage = options.onMessage;
    this.onError = options.onError;
    this.onClose = options.onClose;
    this.maxReconnectAttempts = options.maxReconnectAttempts || 5;
    this.reconnectInterval = options.reconnectInterval || 2000;
    this.connectionTimeout = options.connectionTimeout || 5000;
  }

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
      console.log(`WebSocket连接尝试 ${this.connectionAttempts}/${this.maxReconnectAttempts}`);

      try {
        this.ws = new WebSocket(this.url);

        // 设置连接超时
        this.connectionTimeoutId = setTimeout(() => {
          if (this.ws && this.ws.readyState !== WebSocket.OPEN) {
            this.ws.close();
            const error = new Error('WebSocket 连接超时');
            this.handleError(error);
            reject(error);
          }
        }, this.connectionTimeout);

        this.ws.onopen = () => {
          console.log('WebSocket连接成功');
          this.connected = true;
          this.connectionAttempts = 0;
          clearTimeout(this.connectionTimeoutId);
          resolve();
        };

        this.ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data) as WebSocketMessage;
            console.log('接收到WebSocket消息:', message);
            this.onMessage?.(message);
          } catch (e) {
            console.error('解析WebSocket消息失败:', e);
            this.handleError(e);
          }
        };

        this.ws.onerror = (error) => {
          console.error('WebSocket错误:', error);
          this.handleError(error);
          reject(error);
        };

        this.ws.onclose = (event) => {
          console.warn('WebSocket连接关闭:', event);
          clearTimeout(this.connectionTimeoutId);
          if (this.connected) {
            this.connected = false;
            this.onClose?.(event);
          }
        };
      } catch (error) {
        console.error('创建WebSocket连接失败:', error);
        this.handleError(error);
        reject(error);
      }
    });
  }

  public disconnect(): void {
    if (this.ws) {
      try {
        if (this.ws.readyState === WebSocket.OPEN) {
          this.ws.close();
          console.log('WebSocket已断开连接');
        }
      } catch (e) {
        console.error('断开WebSocket失败:', e);
      }
      this.ws = null;
    }
    this.connected = false;
    clearTimeout(this.connectionTimeoutId);
  }

  public send(message: WebSocketMessage): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket 连接未就绪');
    }
    this.ws.send(JSON.stringify(message));
  }

  public isConnected(): boolean {
    return this.connected && this.ws?.readyState === WebSocket.OPEN;
  }

  private handleError(error: any): void {
    this.connected = false;
    this.onError?.(error);

    if (this.connectionAttempts < this.maxReconnectAttempts) {
      console.log(`尝试重连 (${this.connectionAttempts}/${this.maxReconnectAttempts})...`);
      setTimeout(() => {
        this.connect();
      }, this.reconnectInterval);

      // 增加重连延迟
      this.reconnectInterval = Math.min(this.reconnectInterval * 1.5, 10000);
    } else {
      console.warn('WebSocket连接失败');
      MessagePlugin.error('WebSocket连接失败，请稍后重试');
    }
  }
} 