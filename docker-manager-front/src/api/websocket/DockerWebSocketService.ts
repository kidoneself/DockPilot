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
  private messageHandlers: Map<string, ((message: WebSocketMessage) => void)[]> = new Map();
  private messageHandlerMap: Map<string, (message: WebSocketMessage) => void> = new Map();
  private errorHandlers: Map<string, ((error: any) => void)[]> = new Map();
  private heartbeatInterval: number | null = null;
  private readonly heartbeatIntervalTime = 30000; // 30秒发送一次心跳
  private boundHandleRawMessage: (event: MessageEvent) => void;

  constructor() {
    // 在初始化时创建绑定的函数引用
    this.boundHandleRawMessage = this.handleRawMessage.bind(this);
    // 在初始化时不添加监听器，而是在connect方法中添加
    this.initHeartbeat();
  }

  private initHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }
    this.heartbeatInterval = window.setInterval(() => {
      if (wsClient.isConnected()) {
        this.sendMessage({
          type: 'HEARTBEAT',
          taskId: '',
          data: { timestamp: Date.now() },
        });
      }
    }, this.heartbeatIntervalTime);
  }

  /**
   * 处理原始WebSocket消息
   */
  private handleRawMessage(event: MessageEvent) {
    try {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    } catch (error) {
      // 解析WebSocket消息失败
    }
  }

  /**
   * 确保WebSocket连接已建立并注册消息处理器
   */
  public async connect(): Promise<void> {
    // 先移除可能存在的消息监听器，防止重复添加
    wsClient.removeEventListener('message', this.boundHandleRawMessage);

    // 如果已连接，则直接添加监听器并返回
    if (wsClient.isConnected()) {
      // 添加消息监听器
      wsClient.addEventListener('message', this.boundHandleRawMessage);
      return Promise.resolve();
    }

    try {
      await wsClient.connect();
      // 连接成功后添加消息监听器
      wsClient.addEventListener('message', this.boundHandleRawMessage);
      return Promise.resolve();
    } catch (error) {
      return Promise.reject(error);
    }
  }

  /**
   * 注册消息处理器
   */
  public on(type: string, handler: (message: WebSocketMessage) => void): void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, []);
    }
    this.messageHandlers.get(type)?.push(handler);
  }

  /**
   * 移除消息处理器
   * @param type 消息类型
   * @param handler 可选的处理函数，如果不提供则移除该类型的所有处理器
   */
  public off(type: string, handler?: (message: WebSocketMessage) => void): void {
    // 如果没有提供处理函数，则移除该类型的所有处理器
    if (!handler) {
      this.messageHandlers.delete(type);
      return;
    }

    // 移除特定的处理函数
    const handlers = this.messageHandlers.get(type);
    if (handlers) {
      const index = handlers.indexOf(handler);
      if (index !== -1) {
        handlers.splice(index, 1);
      }
    }
  }

  /**
   * 发送WebSocket消息
   */
  public async sendMessage(message: WebSocketMessage): Promise<void> {
    await this.connect();
    wsClient.send(message);
  }

  /**
   * 检查镜像可用性
   */
  public async checkImages(images: { name: string; tag: string }[]): Promise<void> {
    await this.sendMessage({
      type: 'INSTALL_CHECK_IMAGES',
      taskId: '',
      data: { images },
    });
  }

  /**
   * 验证参数
   */
  public async validateParams(params: any): Promise<void> {
    await this.sendMessage({
      type: 'INSTALL_VALIDATE',
      taskId: '',
      data: { params },
    });
  }

  /**
   * 拉取Docker镜像
   */
  public async pullImage(params: PullImageParams, callbacks: DockerWebSocketCallbacks): Promise<void> {
    try {
      // 注册消息处理器
      const messageHandler = (message: WebSocketMessage) => {
        switch (message.type) {
          case 'PULL_START':
            callbacks.onStart?.(message.taskId);
            break;
          case 'PULL_PROGRESS':
            callbacks.onProgress?.(message.data);
            break;
          case 'PULL_COMPLETE':
            callbacks.onComplete?.();
            // 清除消息处理器
            this.off('PULL_START', messageHandler);
            this.off('PULL_PROGRESS', messageHandler);
            this.off('PULL_COMPLETE', messageHandler);
            this.off('ERROR', messageHandler);
            break;
          case 'ERROR':
            callbacks.onError?.(message.data.error);
            // 清除消息处理器
            this.off('PULL_START', messageHandler);
            this.off('PULL_PROGRESS', messageHandler);
            this.off('PULL_COMPLETE', messageHandler);
            this.off('ERROR', messageHandler);
            break;
        }
      };

      // 注册处理器
      this.on('PULL_START', messageHandler);
      this.on('PULL_PROGRESS', messageHandler);
      this.on('PULL_COMPLETE', messageHandler);
      this.on('ERROR', messageHandler);

      // 发送拉取请求
      await this.connect();
      await this.sendMessage({
        type: 'PULL_IMAGE',
        taskId: '',
        data: params,
      });
    } catch (error: unknown) {
      callbacks.onError?.(error instanceof Error ? error.message : '拉取镜像失败');
      throw error;
    }
  }



  /**
   * 添加消息处理器
   */
  public addMessageHandler(messageId: string, handler: (message: WebSocketMessage) => void): void {
    this.messageHandlerMap.set(messageId, handler);
  }

  /**
   * 移除消息处理器
   */
  public removeMessageHandler(messageId: string): void {
    this.messageHandlerMap.delete(messageId);
  }


  /**
   * 处理消息分发
   */
  private handleMessage(message: WebSocketMessage): void {
    // 记录消息是否已处理
    let isHandled = false;

    // 优先处理特定消息ID的处理器
    if (message.taskId && this.messageHandlerMap.has(message.taskId)) {
      const handler = this.messageHandlerMap.get(message.taskId);
      if (handler) {
        handler(message);
        isHandled = true; // 标记消息已处理
      }
    }

    // 如果消息已经被taskId处理器处理，则不再执行类型处理器
    if (isHandled) {
      return;
    }

    // 处理心跳响应
    if (message.type === 'HEARTBEAT_RESPONSE') {
      return;
    }

    // 处理错误消息
    if (message.type === 'ERROR') {
      const errorMessage = message.data?.message || '操作失败';

      // 如果有taskId，假定这是一个特定操作的错误，
      // 应该由该操作的处理逻辑自行处理（通过reject Promise），不显示全局错误
      if (message.taskId) {
        return;
      }

      // 调用特定类型的错误处理器
      const errorHandlers = this.errorHandlers.get(message.taskId) || [];
      if (errorHandlers.length > 0) {
        errorHandlers.forEach((handler) => handler(message.data));
      } else {
        // 如果没有特定的错误处理器，使用默认的错误提示
        MessagePlugin.error(errorMessage);
      }
      return;
    }

    // 处理测试通知消息
    if (message.type === 'TEST_NOTIFY_RESPONSE') {
      const notificationStore = useNotificationStore();
      if (message.data) {
        notificationStore.handleWebSocketNotification({
          id: message.data.id || String(Date.now()),
          content: message.data.content || message.data.message || '测试消息',
          type: message.data.type || '系统通知',
          status: true,
          collected: false,
          date: message.data.date || new Date().toLocaleString(),
          quality: message.data.quality || 'high',
        });
      }
      return;
    }

    // 处理其他消息
    const handlers = this.messageHandlers.get(message.type) || [];
    handlers.forEach((handler) => handler(message));
  }

  /**
   * 断开WebSocket连接
   * 注意：这个方法只移除DockerWebSocketService的消息监听器，
   * 不会真正断开底层WebSocket连接，因为连接是全局共享的
   */
  public disconnect(): void {
    try {
      // 移除消息监听器
      wsClient.removeEventListener('message', this.boundHandleRawMessage);
    } catch (error) {
      // 移除消息监听器时出错
    }
  }

  /**
   * 获取当前注册的处理器状态
   * 调试用，可以查看各类型消息的处理器数量
   */
  public getRegisteredHandlersStatus(): Record<string, number> {
    const status: Record<string, number> = {};

    this.messageHandlers.forEach((handlers, type) => {
      status[type] = handlers.length;
    });

    status['__taskIdHandlers'] = this.messageHandlerMap.size;

    return status;
  }
}

// 创建单例实例
export const dockerWebSocketService = new DockerWebSocketService();
