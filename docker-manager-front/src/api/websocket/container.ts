import type { Container, ContainerDetail, NetworkListResponse, ImageListResponse, CreateContainerParams } from '@/types/api/container.d.ts';
import type { WebSocketMessageType, WebSocketMessage } from './types';
import { dockerWebSocketService } from './DockerWebSocketService';
import { generateMessageId, registerMessageHandler, sendMessage } from '@/utils/websocket';

/**
 * 容器资源使用情况响应
 */
export interface ContainerStatsResponse {
  code: number;
  message: string;
  data: {
    cpuPercent: number;
    memoryUsage: number;
    memoryLimit: number;
    networkRx: number;
    networkTx: number;
    running: boolean;
  };
}

/**
 * 获取容器列表
 * @returns Promise<Container[]>
 */
export const getContainerList = async (): Promise<Container[]> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'CONTAINER_LIST') {
        dockerWebSocketService.off('CONTAINER_LIST', handler);
        resolve(message.data || []);
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('CONTAINER_LIST', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('CONTAINER_LIST', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'CONTAINER_LIST',
        taskId: '',
        data: {}
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 启动容器
 * @param containerId 容器ID
 * @returns Promise<void>
 */
export const startContainer = async (containerId: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        if (message.data.success) {
          resolve();
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('CONTAINER_OPERATION_RESULT', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'CONTAINER_START',
        taskId: '',
        data: { containerId }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 停止容器
 * @param containerId 容器ID
 * @returns Promise<void>
 */
export const stopContainer = async (containerId: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        if (message.data.success) {
          resolve();
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('CONTAINER_OPERATION_RESULT', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'CONTAINER_STOP',
        taskId: '',
        data: { containerId }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 重启容器
 * @param containerId 容器ID
 * @returns Promise<void>
 */
export const restartContainer = async (containerId: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        if (message.data.success) {
          resolve();
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('CONTAINER_OPERATION_RESULT', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'CONTAINER_RESTART',
        taskId: '',
        data: { containerId }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 获取容器详情
 * @param containerId 容器ID
 * @returns Promise<ContainerDetail>
 */
export const getContainerDetail = async (containerId: string): Promise<ContainerDetail> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'CONTAINER_DETAIL') {
        dockerWebSocketService.off('CONTAINER_DETAIL', handler);
        resolve(message.data);
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('CONTAINER_DETAIL', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('CONTAINER_DETAIL', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'CONTAINER_DETAIL',
        taskId: '',
        data: { containerId }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 获取容器日志
 * @param containerId 容器ID
 * @returns Promise<string>
 */
export const getContainerLogs = async (containerId: string): Promise<{ data: string }> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'CONTAINER_LOGS') {
        dockerWebSocketService.off('CONTAINER_LOGS', handler);
        resolve({ data: message.data });
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('CONTAINER_LOGS', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('CONTAINER_LOGS', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'CONTAINER_LOGS',
        taskId: '',
        data: { containerId }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 获取容器资源使用情况
 * @param containerId 容器ID
 * @returns Promise<ContainerStatsResponse>
 */
export async function getContainerStats(containerId: string): Promise<ContainerStatsResponse> {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'CONTAINER_STATS') {
        dockerWebSocketService.off('CONTAINER_STATS', handler);
        resolve(message.data);
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('CONTAINER_STATS', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('CONTAINER_STATS', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'CONTAINER_STATS',
        taskId: '',
        data: { containerId }
      });
    }).catch(error => {
      reject(error);
    });
  });
}

/**
 * 删除容器
 * @param containerId 容器ID
 * @returns Promise<void>
 */
export const deleteContainer = async (containerId: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        if (message.data.success) {
          resolve();
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('CONTAINER_OPERATION_RESULT', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'CONTAINER_DELETE',
        taskId: '',
        data: { containerId }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 更新容器
 * @param containerId 容器ID
 * @param data 更新数据
 * @returns Promise<string> 返回新容器的ID
 */
export const updateContainer = async (containerId: string, data: any): Promise<string> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        if (message.data.success) {
          // 返回新的容器ID
          resolve(message.data.newContainerId);
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('CONTAINER_OPERATION_RESULT', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'CONTAINER_UPDATE',
        taskId: '',
        data: { containerId, ...data }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 获取网络列表
 * @returns 网络列表
 */
export const getNetworkList = (): Promise<NetworkListResponse> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'NETWORK_LIST') {
        dockerWebSocketService.off('NETWORK_LIST', handler);
        console.log('WebSocket 返回的网络列表数据:', message.data);
        resolve(message.data || []);
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('NETWORK_LIST', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('NETWORK_LIST', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'NETWORK_LIST',
        taskId: '',
        data: {}
      });
    }).catch((error: Error) => {
      reject(error);
    });
  });
};

/**
 * 获取镜像列表
 * @returns Promise<ImageListResponse>
 */
export const getImageList = async (): Promise<ImageListResponse> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'IMAGE_LIST') {
        dockerWebSocketService.off('IMAGE_LIST', handler);
        resolve(message.data || []);
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('IMAGE_LIST', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('IMAGE_LIST', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'IMAGE_LIST',
        taskId: '',
        data: {}
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 获取镜像详情
 * @param imageName 镜像名称
 * @returns Promise<any>
 */
export const getImageDetail = async (imageName: string): Promise<any> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'IMAGE_DETAIL') {
        dockerWebSocketService.off('IMAGE_DETAIL', handler);
        resolve({
          code: 0,
          message: 'success',
          data: message.data
        });
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('IMAGE_DETAIL', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('IMAGE_DETAIL', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'IMAGE_DETAIL',
        taskId: '',
        data: { imageName }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 创建容器
 * @param data 容器创建参数
 * @returns Promise<any>
 */
export const createContainer = async (data: CreateContainerParams): Promise<any> => {
  return new Promise((resolve, reject) => {
    const taskId = Date.now().toString();
    
    // 注册错误处理器
    const errorHandler = (error: any) => {
      dockerWebSocketService.offError(taskId, errorHandler);
      reject(new Error(error.message));
    };
    dockerWebSocketService.onError(taskId, errorHandler);

    const handler = (message: WebSocketMessage) => {
      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        dockerWebSocketService.off('CONTAINER_OPERATION_RESULT', handler);
        dockerWebSocketService.offError(taskId, errorHandler);
        if (message.data.success) {
          resolve({
            code: 0,
            message: 'success',
            data: message.data.data
          });
        } else {
          reject(new Error(message.data.message));
        }
      }
    };

    dockerWebSocketService.on('CONTAINER_OPERATION_RESULT', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'CONTAINER_CREATE',
        taskId,
        data
      });
    }).catch(error => {
      dockerWebSocketService.offError(taskId, errorHandler);
      reject(error);
    });
  });
};

/**
 * 创建网络
 * @param data 网络创建参数
 * @returns Promise<any>
 */
export const createNetwork = async (data: {
  name: string;
  driver: string;
  options?: Record<string, string>;
  ipam?: {
    driver: string;
    config?: Array<{
      subnet?: string;
      gateway?: string;
    }>;
  };
}): Promise<any> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'NETWORK_OPERATION_RESULT') {
        dockerWebSocketService.off('NETWORK_OPERATION_RESULT', handler);
        if (message.data.success) {
          resolve({
            code: 0,
            message: 'success',
            data: message.data.networkId
          });
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('NETWORK_OPERATION_RESULT', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('NETWORK_OPERATION_RESULT', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'NETWORK_CREATE',
        taskId: '',
        data
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 删除网络
 * @param networkId 网络ID
 * @returns Promise<void>
 */
export const deleteNetwork = async (networkId: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'NETWORK_OPERATION_RESULT') {
        dockerWebSocketService.off('NETWORK_OPERATION_RESULT', handler);
        if (message.data.success) {
          resolve();
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('NETWORK_OPERATION_RESULT', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('NETWORK_OPERATION_RESULT', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'NETWORK_DELETE',
        taskId: '',
        data: { networkId }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 删除镜像
 * @param imageId 镜像ID
 * @returns Promise<void>
 */
export const deleteImage = async (imageId: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'IMAGE_DELETE') {
        dockerWebSocketService.off('IMAGE_DELETE', handler);
        resolve();
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('IMAGE_DELETE', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('IMAGE_DELETE', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'IMAGE_DELETE',
        taskId: '',
        data: { imageId }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 更新镜像
 * @param params 更新参数
 * @returns Promise<any>
 */
export const updateImage = async (params: {
  image: string;
  tag: string;
  id?: string;
}): Promise<any> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'IMAGE_UPDATE') {
        dockerWebSocketService.off('IMAGE_UPDATE', handler);
        resolve({
          code: 0,
          message: 'success',
          data: message.data
        });
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('IMAGE_UPDATE', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('IMAGE_UPDATE', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'IMAGE_UPDATE',
        taskId: '',
        data: params
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 批量更新镜像
 * @param params 更新参数
 * @returns Promise<any>
 */
export const batchUpdateImages = async (params: { useProxy: boolean }): Promise<any> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'IMAGE_OPERATION_RESULT') {
        dockerWebSocketService.off('IMAGE_OPERATION_RESULT', handler);
        if (message.data.success) {
          resolve({
            code: 0,
            message: 'success',
            data: message.data
          });
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('IMAGE_OPERATION_RESULT', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('IMAGE_OPERATION_RESULT', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'IMAGE_BATCH_UPDATE',
        taskId: '',
        data: params
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 取消镜像拉取
 * @param taskId 任务ID
 * @returns Promise<any>
 */
export const cancelImagePull = async (taskId: string): Promise<any> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'IMAGE_OPERATION_RESULT') {
        dockerWebSocketService.off('IMAGE_OPERATION_RESULT', handler);
        if (message.data.success) {
          resolve({
            code: 0,
            message: 'success',
            data: message.data
          });
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('IMAGE_OPERATION_RESULT', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('IMAGE_OPERATION_RESULT', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'IMAGE_CANCEL_PULL',
        taskId: '',
        data: { taskId }
      });
    }).catch(error => {
      reject(error);
    });
  });
};

/**
 * 检查镜像更新
 * @returns Promise<any>
 */
export const checkImageUpdates = async (): Promise<any> => {
  return new Promise((resolve, reject) => {
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'IMAGE_CHECK_UPDATES') {
        dockerWebSocketService.off('IMAGE_CHECK_UPDATES', handler);
        resolve({
          code: 0,
          message: 'success',
          data: message.data
        });
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('IMAGE_CHECK_UPDATES', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('IMAGE_CHECK_UPDATES', handler);
    dockerWebSocketService.connect().then(() => {
      dockerWebSocketService.sendMessage({
        type: 'IMAGE_CHECK_UPDATES',
        taskId: '',
        data: {}
      });
    }).catch(error => {
      reject(error);
    });
  });
}; 