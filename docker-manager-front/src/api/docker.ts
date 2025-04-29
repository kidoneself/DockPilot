import { dockerWebSocketService } from './websocket/DockerWebSocketService';
import type { PullImageParams, PullImageProgress, DockerWebSocketCallbacks } from './websocket/types';

export { type PullImageParams, type PullImageProgress, type DockerWebSocketCallbacks };

// 导出WebSocket服务实例
export const dockerWebSocketAPI = dockerWebSocketService;

// 导出镜像相关方法，使用箭头函数包装以保持this上下文
export const checkImages = (...args: Parameters<typeof dockerWebSocketService.checkImages>) => 
  dockerWebSocketService.checkImages(...args);
  
export const pullImage = (...args: Parameters<typeof dockerWebSocketService.pullImage>) => 
  dockerWebSocketService.pullImage(...args); 