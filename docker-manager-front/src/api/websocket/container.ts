import type {
    Container,
    ContainerDetail,
    ContainerStats,
    CreateContainerParams,
    ImageListResponse,
    NetworkListResponse,
} from '@/api/model/containerModel.ts';
import type {WebSocketMessage} from '@/api/model/websocketModel';
import {dockerWebSocketService} from './DockerWebSocketService';

/**
 * 获取容器列表
 * @returns Promise<Container[]>
 */
// 获取容器列表的异步函数，返回类型为 Container[] 数组
export const getContainerList = async (): Promise<Container[]> => {
  // 返回一个新的 Promise，用于处理异步操作
  return new Promise((resolve, reject) => {
    // 定义消息处理函数，用于处理 WebSocket 返回的消息
    const handler = (message: WebSocketMessage) => {
      // 如果收到容器列表消息
      if (message.type === 'CONTAINER_LIST') {
        // 移除消息监听器，避免重复处理
        dockerWebSocketService.off('CONTAINER_LIST', handler);
        // 解析并返回容器列表数据，如果没有数据则返回空数组
        resolve(message.data || []);
      }
      // 如果收到错误消息
      else if (message.type === 'ERROR') {
        // 移除消息监听器
        dockerWebSocketService.off('CONTAINER_LIST', handler);
        // 抛出错误，包含错误信息
        reject(new Error(message.data.message));
      }
    };

    // 注册消息监听器，监听 'CONTAINER_LIST' 类型的消息
    dockerWebSocketService.on('CONTAINER_LIST', handler);

    // 连接 WebSocket 服务
    dockerWebSocketService
      .connect()
      .then(() => {
        // 连接成功后，发送获取容器列表的请求
        dockerWebSocketService.sendMessage({
          type: 'CONTAINER_LIST', // 消息类型
          taskId: '', // 任务ID（这里为空）
          data: {}, // 请求数据（这里为空对象）
        });
      })
      .catch((error) => {
        // 如果连接失败，直接拒绝 Promise
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
  return new Promise((resolve) => {
    // 生成唯一的任务ID
    const taskId = `start_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        // 检查操作是否成功
        if (message.data && message.data.success) {
          resolve({ success: true });
        } else {
          resolve({
            success: false,
            message: message.data?.message,
          });
        }
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'CONTAINER_START',
          taskId: taskId,
          data: { containerId },
        });
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
 * 停止容器
 * @param containerId 容器ID
 * @returns Promise<{success: boolean; message?: string}>
 */
export const stopContainer = async (containerId: string): Promise<{ success: boolean; message?: string }> => {
  return new Promise((resolve) => {
    // 生成唯一的任务ID
    const taskId = `stop_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        // 检查操作是否成功
        if (message.data && message.data.success) {
          resolve({ success: true });
        } else {
          resolve({
            success: false,
            message: message.data?.message,
          });
        }
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'CONTAINER_STOP',
          taskId: taskId,
          data: { containerId },
        });
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
  return new Promise((resolve) => {
    // 生成唯一的任务ID
    const taskId = `restart_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        // 检查操作是否成功
        if (message.data && message.data.success) {
          resolve({ success: true });
        } else {
          resolve({
            success: false,
            message: message.data?.message,
          });
        }
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'CONTAINER_RESTART',
          taskId: taskId,
          data: { containerId },
        });
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
export const getContainerDetail = async (
  containerId: string,
): Promise<{
  success: boolean;
  message?: string;
  data?: ContainerDetail;
}> => {
  return new Promise((resolve) => {
    // 生成唯一的任务ID
    const taskId = `detail_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === 'CONTAINER_DETAIL') {
        resolve({
          success: true,
          data: message.data,
        });
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'CONTAINER_DETAIL',
          taskId: taskId,
          data: { containerId },
        });
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
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'CONTAINER_LOGS',
          taskId: '',
          data: { containerId },
        });
      })
      .catch((error) => {
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
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'CONTAINER_STATS',
          taskId: '',
          data: { containerId },
        });
      })
      .catch((error) => {
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
  return new Promise((resolve) => {
    // 生成唯一的任务ID
    const taskId = `container_del_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        // 检查操作是否成功
        if (message.data && message.data.success) {
          resolve({ success: true });
        } else {
          resolve({
            success: false,
            message: message.data?.message,
          });
        }
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'CONTAINER_DELETE',
          taskId: taskId,
          data: { containerId },
        });
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
    // 生成唯一的任务ID
    const taskId = `container_update_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);
      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        resolve({
          success: true,
          newContainerId: message.data,
        });
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'CONTAINER_UPDATE',
          taskId: taskId,
          data: { containerId, ...data },
        });
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
    // 生成唯一的任务ID
    const taskId = `network_list_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === 'NETWORK_LIST') {
        resolve({
          success: true,
          data: message.data || [],
        });
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'NETWORK_LIST',
          taskId: taskId,
          data: {},
        });
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
    // 生成唯一的任务ID
    const taskId = `image_list_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === 'IMAGE_LIST') {
        resolve({
          success: true,
          data: message.data || [],
        });
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'IMAGE_LIST',
          taskId: taskId,
          data: {},
        });
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
    // 生成唯一的任务ID
    const taskId = `image_detail_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === 'IMAGE_DETAIL') {
        resolve({
          success: true,
          data: message.data,
        });
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'IMAGE_DETAIL',
          taskId: taskId,
          data: { imageName },
        });
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
    // 生成唯一的任务ID
    const taskId = `container_create_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === 'CONTAINER_OPERATION_RESULT') {
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
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'CONTAINER_CREATE',
          taskId: taskId,
          data,
        });
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
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'NETWORK_OPERATION_RESULT') {
        dockerWebSocketService.off('NETWORK_OPERATION_RESULT', handler);
        if (message.data.success) {
          resolve({
            code: 0,
            message: 'success',
            data: message.data.networkId,
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
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'NETWORK_CREATE',
          taskId: '',
          data,
        });
      })
      .catch((error) => {
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
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'NETWORK_DELETE',
          taskId: '',
          data: { networkId },
        });
      })
      .catch((error) => {
        reject(error);
      });
  });
};

export const deleteImage = async (imageId: string): Promise<{ success: boolean; message?: string }> => {
  return new Promise((resolve) => {
    // 生成唯一的任务ID
    const taskId = `img_del_${Date.now()}`;

    // 创建消息处理函数
    const messageHandler = (message: WebSocketMessage) => {
      // 操作完成后移除处理器
      dockerWebSocketService.removeMessageHandler(taskId);

      if (message.type === 'CONTAINER_OPERATION_RESULT') {
        // 检查操作是否成功
        if (message.data && message.data.success) {
          resolve({ success: true });
        } else {
          resolve({
            success: false,
            message: message.data?.message,
          });
        }
      } else if (message.type === 'ERROR') {
        resolve({
          success: false,
          message: message.data?.message,
        });
      }
    };

    // 注册消息处理器
    dockerWebSocketService.addMessageHandler(taskId, messageHandler);

    // 建立连接并发送消息
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'IMAGE_DELETE',
          taskId: taskId,
          data: { imageId },
        });
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
    const handler = (message: WebSocketMessage) => {
      if (message.type === 'IMAGE_UPDATE') {
        dockerWebSocketService.off('IMAGE_UPDATE', handler);
        resolve({
          code: 0,
          message: 'success',
          data: message.data,
        });
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('IMAGE_UPDATE', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('IMAGE_UPDATE', handler);
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'IMAGE_UPDATE',
          taskId: '',
          data: params,
        });
      })
      .catch((error) => {
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
            data: message.data,
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
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'IMAGE_BATCH_UPDATE',
          taskId: '',
          data: params,
        });
      })
      .catch((error) => {
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
            data: message.data,
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
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'IMAGE_CANCEL_PULL',
          taskId: '',
          data: { taskId },
        });
      })
      .catch((error) => {
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
          data: message.data,
        });
      } else if (message.type === 'ERROR') {
        dockerWebSocketService.off('IMAGE_CHECK_UPDATES', handler);
        reject(new Error(message.data.message));
      }
    };

    dockerWebSocketService.on('IMAGE_CHECK_UPDATES', handler);
    dockerWebSocketService
      .connect()
      .then(() => {
        dockerWebSocketService.sendMessage({
          type: 'IMAGE_CHECK_UPDATES',
          taskId: '',
          data: {},
        });
      })
      .catch((error) => {
        reject(error);
      });
  });
};
