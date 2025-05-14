import type {
    Container,
    ContainerDetail,
    ContainerStats,
    CreateContainerParams,
    ImageListResponse,
    NetworkListResponse,
} from '@/api/model/containerModel.ts';
import type {DockerWebSocketCallbacks, PullImageParams, WebSocketMessage} from '@/api/model/websocketModel';
import {WebSocketMessageType} from '@/api/model/websocketModel';
import {dockerWebSocketService} from './DockerWebSocketService';
import {generateTaskId, TaskIdPrefix} from '@/utils/taskId';

/**
 * 获取容器列表
 * @returns Promise<Container[]>
 */
export const getContainerList = async (): Promise<Container[]> => {
  return new Promise((resolve, reject) => {
    // 生成任务ID
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_LIST);

    // 创建消息处理器
    const messageHandler = (message: WebSocketMessage) => {
      // 如果消息的taskId不等于任务ID，则返回
      if (message.taskId !== taskId) return;
      // 移除消息处理器
      dockerWebSocketService.removeMessageHandler(taskId);
      // 如果消息的类型是COMPLETE，则返回数据
      if (message.type === WebSocketMessageType.COMPLETE) {
        resolve(message.data || []);
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject(new Error(message.data?.errorMessage));
      }
    };

    // 添加消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);
    // 发送消息
    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_LIST,
        taskId,
        data: {},
      })
      .catch((error: Error) => {
        // 移除消息处理器
        dockerWebSocketService.removeMessageHandler(taskId);
        reject(error);
      });
  });
};

/**
 * 启动容器
 * @param containerId 容器ID
 * @returns Promise<{success: boolean; message?: string}>
 */
export const startContainer = async (containerId: string): Promise<{ success: boolean; message?: string }> => {
  return new Promise((resolve, reject) => {
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_START);

    const messageHandler = (message: WebSocketMessage) => {
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.COMPLETE) {
        resolve({ success: true });
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject({
          success: false,
          message: message.data?.errorMessage,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_START,
        taskId: taskId,
        data: { containerId },
      })
      .catch((error: Error) => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 停止容器
 * @param containerId 容器ID
 * @returns Promise<{success: boolean; message?: string}>
 */
export const stopContainer = async (containerId: string): Promise<{ success: boolean; message?: string }> => {
  return new Promise((resolve, reject) => {
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_STOP);

    const messageHandler = (message: WebSocketMessage) => {
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.COMPLETE) {
        resolve({ success: true });
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject({
          success: false,
          message: message.data?.errorMessage,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_STOP,
        taskId: taskId,
        data: { containerId },
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 重启容器
 * @param containerId 容器ID
 * @returns Promise<{success: boolean; message?: string}>
 */
export const restartContainer = async (containerId: string): Promise<{ success: boolean; message?: string }> => {
  return new Promise((resolve, reject) => {
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_RESTART);

    const messageHandler = (message: WebSocketMessage) => {
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        if (message.data && message.data.success) {
          resolve({ success: true });
        } else {
          resolve({
            success: false,
            message: message.data?.message,
          });
        }
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_RESTART,
        taskId: taskId,
        data: { containerId },
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 获取容器详情
 * @param containerId 容器ID
 * @returns Promise<{success: boolean; message?: string; data?: ContainerDetail}>
 */
export const getContainerDetail = async (containerId: string): Promise<ContainerDetail> => {
  return new Promise((resolve, reject) => {
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_DETAIL);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;
      // 移除消息处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.COMPLETE) {
        resolve(message.data || []);
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject(new Error(message.data.errorMessage));
      }
    };
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);
    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_DETAIL,
        taskId: taskId,
        data: { containerId },
      })
      .catch((error: Error) => {
        // 移除消息处理器
        dockerWebSocketService.removeMessageHandler(taskId);
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
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_LOGS);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.COMPLETE) {
        resolve({ data: message.data });
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_LOGS,
        taskId,
        data: { containerId },
      })
      .catch((error) => {
        dockerWebSocketService.removeMessageHandler(taskId);
        reject(error);
      });
  });
};

/**
 * 获取容器资源使用情况
 * @param containerId 容器ID
 * @returns Promise<ContainerStats>
 */
export async function getContainerStats(containerId: string): Promise<ContainerStats> {
  return new Promise((resolve, reject) => {
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_STATS);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.COMPLETE) {
        resolve(message.data || []);
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject(new Error(message.errorMessage));
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_STATS,
        taskId,
        data: { containerId },
      })
      .catch((error) => {
        dockerWebSocketService.removeMessageHandler(taskId);
        reject(error);
      });
  });
}

/**
 * 删除容器
 * @param containerId 容器ID
 * @returns Promise<{success: boolean; message?: string}>
 */
export const deleteContainer = async (containerId: string): Promise<{ success: boolean; message?: string }> => {
  return new Promise((resolve, reject) => {
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_DELETE);

    const messageHandler = (message: WebSocketMessage) => {
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        if (message.data && message.data.success) {
          resolve({ success: true });
        } else {
          resolve({
            success: false,
            message: message.data?.message,
          });
        }
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_DELETE,
        taskId: taskId,
        data: { containerId },
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 更新容器
 * @param containerId 容器ID
 * @param data 更新数据
 * @returns Promise<{success: boolean; message?: string; newContainerId?: string}>
 */
export const updateContainer = async (
  containerId: string,
  data: any,
): Promise<{
  success: boolean;
  message?: string;
  newContainerId?: string;
}> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_UPDATE);

    const messageHandler = (message: WebSocketMessage) => {
      dockerWebSocketService.removeMessageHandler(taskId);
      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        resolve({
          success: true,
          newContainerId: message.data,
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_UPDATE,
        taskId: taskId,
        data: { containerId, ...data },
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 获取网络列表
 * @returns Promise<{success: boolean; message?: string; data?: NetworkListResponse}>
 */
export const getNetworkList = (): Promise<{ success: boolean; message?: string; data?: NetworkListResponse }> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.NETWORK_LIST);

    const messageHandler = (message: WebSocketMessage) => {
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.NETWORK_LIST) {
        resolve({
          success: true,
          data: message.data || [],
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.NETWORK_LIST,
        taskId: taskId,
        data: {},
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 获取镜像列表
 * @returns Promise<{success: boolean; message?: string; data?: ImageListResponse}>
 */
export const getImageList = async (): Promise<{ success: boolean; message?: string; data?: ImageListResponse }> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.IMAGE_LIST);

    const messageHandler = (message: WebSocketMessage) => {
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.IMAGE_LIST) {
        resolve({
          success: true,
          data: message.data || [],
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.IMAGE_LIST,
        taskId: taskId,
        data: {},
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 获取镜像详情
 * @param imageName 镜像名称
 * @returns Promise<{success: boolean; message?: string; data?: any}>
 */
export const getImageDetail = async (
  imageName: string,
): Promise<{
  success: boolean;
  message?: string;
  data?: any;
}> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.IMAGE_DETAIL);

    const messageHandler = (message: WebSocketMessage) => {
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.IMAGE_DETAIL) {
        resolve({
          success: true,
          data: message.data,
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.IMAGE_DETAIL,
        taskId: taskId,
        data: { imageName },
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 创建容器
 * @param data 容器创建参数
 * @returns Promise<{success: boolean; message?: string; data?: any}>
 */
export const createContainer = async (
  data: CreateContainerParams,
): Promise<{
  success: boolean;
  message?: string;
  data?: any;
}> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_CREATE);

    const messageHandler = (message: WebSocketMessage) => {
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        if (message.data && message.data.success) {
          resolve({
            success: true,
            data: message.data.data,
          });
        } else {
          resolve({
            success: false,
            message: message.data?.message,
          });
        }
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_CREATE,
        taskId: taskId,
        data,
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
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
    const taskId = generateTaskId(TaskIdPrefix.NETWORK_CREATE);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        if (message.data.success) {
          resolve({
            code: 0,
            message: 'success',
            data: message.data.networkId,
          });
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.NETWORK_CREATE,
        taskId,
        data,
      })
      .catch((error) => {
        dockerWebSocketService.removeMessageHandler(taskId);
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
    const taskId = generateTaskId(TaskIdPrefix.NETWORK_DELETE);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        if (message.data.success) {
          resolve();
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.NETWORK_DELETE,
        taskId,
        data: { networkId },
      })
      .catch((error) => {
        dockerWebSocketService.removeMessageHandler(taskId);
        reject(error);
      });
  });
};

export const deleteImage = async (imageId: string): Promise<{ success: boolean; message?: string }> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.IMAGE_DELETE);

    const messageHandler = (message: WebSocketMessage) => {
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        if (message.data && message.data.success) {
          resolve({ success: true });
        } else {
          resolve({
            success: false,
            message: message.data?.message,
          });
        }
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.IMAGE_DELETE,
        taskId: taskId,
        data: { imageId },
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 更新镜像
 * @param params 更新参数
 * @returns Promise<any>
 */
export const updateImage = async (params: { image: string; tag: string; id?: string }): Promise<any> => {
  return new Promise((resolve, reject) => {
    const taskId = generateTaskId(TaskIdPrefix.IMAGE_UPDATE);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        resolve({
          code: 0,
          message: 'success',
          data: message.data,
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.IMAGE_UPDATE,
        taskId,
        data: params,
      })
      .catch((error) => {
        dockerWebSocketService.removeMessageHandler(taskId);
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
    const taskId = generateTaskId(TaskIdPrefix.IMAGE_BATCH_UPDATE);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        if (message.data.success) {
          resolve({
            code: 0,
            message: 'success',
            data: message.data,
          });
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.IMAGE_BATCH_UPDATE,
        taskId,
        data: params,
      })
      .catch((error) => {
        dockerWebSocketService.removeMessageHandler(taskId);
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
    const cancelTaskId = generateTaskId(TaskIdPrefix.IMAGE_CANCEL_PULL);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== cancelTaskId) return;

      dockerWebSocketService.removeMessageHandler(cancelTaskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        if (message.data.success) {
          resolve({
            code: 0,
            message: 'success',
            data: message.data,
          });
        } else {
          reject(new Error(message.data.message));
        }
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.addMessageHandler(cancelTaskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.IMAGE_CANCEL_PULL,
        taskId: cancelTaskId,
        data: { taskId },
      })
      .catch((error) => {
        dockerWebSocketService.removeMessageHandler(cancelTaskId);
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
    const taskId = generateTaskId(TaskIdPrefix.IMAGE_CHECK_UPDATES);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        resolve({
          code: 0,
          message: 'success',
          data: message.data,
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.IMAGE_CHECK_UPDATES,
        taskId,
        data: {},
      })
      .catch((error) => {
        dockerWebSocketService.removeMessageHandler(taskId);
        reject(error);
      });
  });
};

/**
 * 检查镜像可用性
 * @param images 镜像列表
 * @returns Promise<{success: boolean; message?: string; data?: any}>
 */
export const checkImages = async (
  images: { name: string; tag: string }[],
): Promise<{
  success: boolean;
  message?: string;
  data?: any;
}> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.INSTALL_CHECK);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        resolve({
          success: true,
          data: message.data,
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.INSTALL_CHECK_IMAGES,
        taskId,
        data: { images },
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 验证参数
 * @param params 参数
 * @returns Promise<{success: boolean; message?: string; data?: any}>
 */
export const validateParams = async (
  params: any,
): Promise<{
  success: boolean;
  message?: string;
  data?: any;
}> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.INSTALL_VALIDATE);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        resolve({
          success: true,
          data: message.data,
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.INSTALL_VALIDATE,
        taskId,
        data: { params },
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 拉取Docker镜像
 * @param params 拉取参数
 * @param callbacks 回调函数
 * @returns Promise<{success: boolean; message?: string; data?: any}>
 */
export const pullImage = async (
  params: PullImageParams,
  callbacks: DockerWebSocketCallbacks,
): Promise<{
  success: boolean;
  message?: string;
  data?: any;
}> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.IMAGE_PULL);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      if (message.type === WebSocketMessageType.ERROR) {
        callbacks.onError?.(message.data.error);
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: message.data?.message,
        });
        return;
      }

      if (message.type === WebSocketMessageType.PULL_PROGRESS && callbacks.onProgress) {
        const { progress, status } = message.data as { progress: number; status: string };
        callbacks.onProgress({ progress, status });
      }

      if (message.type === WebSocketMessageType.PULL_COMPLETE) {
        callbacks.onComplete?.();
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: true,
          data: message.data,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.PULL_IMAGE,
        taskId,
        data: params,
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        callbacks.onError?.('网络连接失败');
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 开始安装应用
 * @param params 安装参数
 * @returns Promise<{success: boolean; message?: string; data?: any}>
 */
export const startInstall = async (params: {
  appId: string;
  params: Record<string, any>;
}): Promise<{
  success: boolean;
  message?: string;
  data?: any;
}> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.INSTALL_START);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      if (message.type === WebSocketMessageType.INSTALL_LOG) {
        // 日志消息由外部处理器处理
        return;
      }

      if (message.type === WebSocketMessageType.INSTALL_START_RESULT) {
        dockerWebSocketService.removeMessageHandler(taskId);
        if (message.data?.success) {
          resolve({
            success: true,
            data: message.data,
          });
        } else {
          resolve({
            success: false,
            message: message.data?.message,
          });
        }
      } else if (message.type === WebSocketMessageType.ERROR) {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.INSTALL_START,
        taskId,
        data: params,
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 添加安装日志处理器
 * @param handler 日志处理函数
 */
export const addInstallLogHandler = (handler: (message: WebSocketMessage) => void) => {
  const logHandler = (message: WebSocketMessage) => {
    console.log('日志处理器收到消息:', message);
    if (message.type === WebSocketMessageType.INSTALL_LOG) {
      handler(message);
    } else if (message.type === WebSocketMessageType.ERROR) {
      handler(message);
    }
  };
  dockerWebSocketService.addMessageHandler('INSTALL_LOG_TYPE', logHandler);
};

/**
 * 移除安装日志处理器
 */
export const removeInstallLogHandler = () => {
  dockerWebSocketService.removeMessageHandler('INSTALL_LOG_TYPE');
};

/**
 * 添加镜像检查结果处理器
 * @param handler 处理函数
 */
export const addCheckImagesResultHandler = (handler: (message: WebSocketMessage) => void) => {
  dockerWebSocketService.addMessageHandler('INSTALL_CHECK_IMAGES_RESULT', handler);
};

/**
 * 移除镜像检查结果处理器
 */
export const removeCheckImagesResultHandler = () => {
  dockerWebSocketService.removeMessageHandler('INSTALL_CHECK_IMAGES_RESULT');
};

/**
 * 导入应用模板
 * @param content 模板内容
 * @param fileName 文件名
 * @returns Promise<{success: boolean; message?: string}>
 */
export const importTemplate = async (
  content: string,
  fileName: string,
): Promise<{
  success: boolean;
  message?: string;
}> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.IMPORT_TEMPLATE);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.IMPORT_TEMPLATE_RESULT) {
        resolve({
          success: message.data?.success || false,
          message: message.data?.message,
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.IMPORT_TEMPLATE,
        taskId,
        data: { content, fileName },
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};

/**
 * 获取容器JSON配置
 * @param containerId 容器ID
 * @returns Promise<{success: boolean; message?: string; data?: string}>
 */
export const getContainerJsonConfig = (
  containerId: string,
): Promise<{
  success: boolean;
  message?: string;
  data?: string;
}> => {
  return new Promise((resolve, reject) => {
    const taskId = generateTaskId(TaskIdPrefix.CONTAINER_JSON_CONFIG);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;
      dockerWebSocketService.removeMessageHandler(taskId);
      if (message.type === WebSocketMessageType.COMPLETE) {
        resolve({
          success: true,
          data: message.data,
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        reject({
          success: false,
          message: message.data?.errorMessage,
        });
      }
    };
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);
    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.CONTAINER_JSON_CONFIG,
        taskId,
        data: { containerId },
      })
      .catch((error: Error) => {
        // 移除消息处理器
        dockerWebSocketService.removeMessageHandler(taskId);
        reject(error);
      });
  });
};

/**
 * 删除应用模板
 * @param templateId 模板ID
 * @returns Promise<{success: boolean; message?: string}>
 */
export const deleteTemplate = async (templateId: string): Promise<{ success: boolean; message?: string }> => {
  return new Promise((resolve) => {
    const taskId = generateTaskId(TaskIdPrefix.DELETE_TEMPLATE);

    const messageHandler = (message: WebSocketMessage) => {
      if (message.taskId !== taskId) return;

      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === WebSocketMessageType.OPERATION_RESULT) {
        resolve({
          success: true,
        });
      } else if (message.type === WebSocketMessageType.ERROR) {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    dockerWebSocketService
      .sendMessage({
        type: WebSocketMessageType.DELETE_TEMPLATE,
        taskId,
        data: { templateId },
      })
      .catch(() => {
        dockerWebSocketService.removeMessageHandler(taskId);
        resolve({
          success: false,
          message: '网络连接失败',
        });
      });
  });
};
