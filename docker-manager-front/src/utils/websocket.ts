import { MessagePlugin } from 'tdesign-vue-next';

class WebSocketManager {
  private ws: WebSocket | null = null;
  private messageQueue: any[] = [];
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 3000;
  private heartbeatInterval: number | null = null;

  constructor() {
    this.connect();
  }

  private connect() {
    try {
      this.ws = new WebSocket(`ws://${window.location.host}/ws/docker`);
      this.setupEventListeners();
    } catch (error) {
      console.error('WebSocket 连接失败:', error);
      this.handleReconnect();
    }
  }

  private setupEventListeners() {
    if (!this.ws) return;

    this.ws.onopen = () => {
      console.log('WebSocket 连接已建立');
      this.reconnectAttempts = 0;
      this.startHeartbeat();
      this.processMessageQueue();
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket 错误:', error);
      MessagePlugin.error('WebSocket 连接错误');
    };

    this.ws.onclose = () => {
      console.log('WebSocket 连接已关闭');
      MessagePlugin.warning('WebSocket 连接已关闭');
      this.stopHeartbeat();
      this.handleReconnect();
    };
  }

  private handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      setTimeout(() => this.connect(), this.reconnectInterval);
    } else {
      console.error('WebSocket 重连失败，已达到最大重试次数');
      MessagePlugin.error('WebSocket 连接失败，请刷新页面重试');
    }
  }

  private startHeartbeat() {
    this.heartbeatInterval = window.setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.send({
          type: 'HEARTBEAT',
          data: {}
        });
      }
    }, 30000);
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private processMessageQueue() {
    while (this.messageQueue.length > 0) {
      const message = this.messageQueue.shift();
      this.send(message);
    }
  }

  public send(message: any) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      this.messageQueue.push(message);
    }
  }

  public addEventListener(type: 'message', listener: (event: MessageEvent) => void) {
    this.ws?.addEventListener(type, listener as EventListener);
  }

  public removeEventListener(type: 'message', listener: (event: MessageEvent) => void) {
    this.ws?.removeEventListener(type, listener as EventListener);
  }
}

// 导出单例实例
export const ws = new WebSocketManager(); 