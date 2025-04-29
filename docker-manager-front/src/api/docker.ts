import { dockerWebSocketService } from './websocket/DockerWebSocketService';
import type { PullImageParams, PullImageProgress, DockerWebSocketCallbacks } from './websocket/types';

export { type PullImageParams, type PullImageProgress, type DockerWebSocketCallbacks };

// 导出WebSocket服务实例
export const dockerWebSocketAPI = dockerWebSocketService;

// 导出镜像相关方法
export const checkImages = dockerWebSocketService.checkImages;
export const pullImage = dockerWebSocketService.pullImage; 