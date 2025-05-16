import type {
  Container,
  ContainerDetail,
  ContainerStats,
  CreateContainerParams,
  ImageDetailResponse,
  ImageListResponse,
  NetworkListResponse,
} from '@/api/model/containerModel';
import type {DockerWebSocketCallbacks, PullImageParams, WebSocketMessage} from '@/api/model/websocketModel';
import {WebSocketMessageType} from '@/api/model/websocketModel';
import {dockerWebSocketService} from './DockerWebSocketService';

/**
 * resolve 返回成功状态，但携带错误信息
 *  reject 返回失败状态，直接抛出错误
 *  处理方式不同：
 *  resolve 方式需要在 .then() 中判断 success 字段
 *  reject 方式必须使用 .catch() 处理
 *  使用场景不同：
 *  resolve 方式适合：
 *  需要统一处理成功和失败的情况
 *  错误是业务逻辑的一部分，不是异常
 *  例如：pullImage 方法，因为拉取失败是正常的业务情况
 *  reject 方式适合：
 *  错误是真正的异常情况
 *  需要中断执行流程
 *  例如：startContainer 方法，因为启动失败是异常情况
 *  代码风格不同：
 *  resolve 方式更符合"返回结果对象"的模式
 *  reject 方式更符合"异常处理"的模式
 *  错误传播不同：
 *  resolve 方式错误不会自动传播，需要手动处理
 *  reject 方式错误会自动传播，直到被 catch 捕获
 */
/**
 *
 * 获取容器列表（通过 WebSocket）
 *
 * 说明：
 * 这是一个通过 WebSocket 获取容器列表的异步方法。
 * 使用 dockerWebSocketService.sendWebSocketMessage 发送请求，类型为 CONTAINER_LIST。
 * 通过回调函数处理不同的响应：
 *   - onComplete：后端返回数据时调用，正常返回容器列表。
 *   - onError：后端返回错误时调用，直接 reject，前端可以统一处理错误。
 *   - onTimeout：超时未响应时调用，也 reject。
 * 这种写法适合一次性请求-响应的场景，错误和超时都通过 Promise 的 reject 抛出，页面 catch 处理。
 *
 * @returns Promise<Container[]> 返回容器列表的 Promise
 */
export const getContainerList = async (): Promise<Container[]> => {
  // 返回一个 Promise，方便调用方用 await 或 then 处理
  return new Promise((resolve, reject) => {
    // 通过 dockerWebSocketService 发送 WebSocket 消息，请求容器列表
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.CONTAINER_LIST, // 指定消息类型为"获取容器列表"
      data: {}, // 这里不需要额外参数，data 为空对象

      // 处理后端返回的"完成"事件
      onComplete: (data) => {
        // data 是后端返回的容器列表数组
        resolve(data || []); // 正常返回数据，Promise 进入 resolved 状态
      },

      // 处理后端返回的"错误"事件
      onError: (err) => {
        // err 是后端返回的错误信息
        // 这种方式：
        // 1. Promise 会进入 rejected 状态（失败状态）
        // 2. 直接抛出错误
        // 3. 调用方必须使用 .catch() 来处理错误
        // 4. 调用示例：
        //       startContainer(containerId)
        // .then(result => {
        //   // 处理成功情况
        // })
        // .catch(error => {
        //   // 处理错误情况
        //   console.error(error.message);
        // });
        reject(new Error(err)); // 直接 reject，Promise 进入 rejected 状态，页面 catch 处理
      },

      // 超时处理（比如 30 秒无响应）
      timeout: 30000, // 设置超时时间为 30 秒
      onTimeout: () => {
        reject(new Error('请求超时')); // 超时也 reject，页面 catch 处理
      },
    });
  });
};

/**
 * 拉取 Docker 镜像
 * 该方法通过 WebSocket 与后端通信，实现 Docker 镜像的拉取功能
 * 支持实时进度显示、日志输出、错误处理等功能
 *
 * @param params - 拉取镜像所需的参数对象，包含镜像名称、标签等信息
 * @param callbacks - 回调函数对象，用于处理拉取过程中的各种事件
 * @returns Promise<{success: boolean; message?: string; data?: any}> - 返回拉取结果，包含成功状态、错误信息和返回数据
 */
export const pullImage = async (
  params: PullImageParams,
  callbacks: DockerWebSocketCallbacks,
): Promise<{ success: boolean; message?: string; data?: any }> => {
  // 创建一个新的 Promise 对象，用于异步处理拉取镜像的结果
  // 使用 resolve 而不是 reject，因为我们需要统一处理成功和失败的情况
  return new Promise((resolve) => {
    // 调用 WebSocket 服务发送拉取镜像的请求
    // 使用链式调用方式处理各种回调
    dockerWebSocketService
      .sendWebSocketMessage({
        // 指定消息类型为拉取镜像，后端根据此类型执行相应的操作
        type: WebSocketMessageType.PULL_IMAGE,

        // 传递拉取参数给后端，包含镜像名称、标签等信息
        data: params,

        // 任务开始时的回调函数
        // 当后端开始处理拉取请求时触发
        // taskId 是后端分配的任务标识符，用于跟踪拉取进度
        onStart: (taskId) => {
          // 在控制台输出任务开始的信息，方便调试
          console.log(`[拉取镜像] 任务开始，taskId: ${taskId}`);
        },

        // 进度更新时的回调函数
        // 当拉取进度发生变化时触发
        // progress 参数表示当前的拉取进度（0-100）
        onProgress: (progress) => {
          // 在控制台输出进度信息
          console.log(`[拉取镜像] 进度更新: ${progress}%`);
          // 调用外部传入的进度回调函数，用于更新UI显示
          // 使用可选链操作符，避免 callbacks 未定义时的错误
          callbacks.onProgress?.(progress);
        },

        // 日志输出时的回调函数
        // 当拉取过程中有新的日志信息时触发
        // data 参数包含具体的日志内容
        onLog: (data) => {
          // 调用外部传入的日志回调函数，用于显示拉取过程的详细信息
          callbacks.onLog?.(data);
        },

        // 任务完成时的回调函数
        // 当镜像拉取成功完成时触发
        // data 参数可能包含额外的完成信息
        onComplete: (data) => {
          // 调用外部传入的完成回调函数，通知调用者任务已完成
          callbacks.onComplete?.();
          // 返回成功结果，包含拉取的数据
          resolve({ success: true, data });
        },

        // 发生错误时的回调函数
        // 当拉取过程中出现错误时触发
        // errMsg 参数包含具体的错误信息
        onError: (errMsg) => {
          // 调用外部传入的错误回调函数，通知调用者发生错误
          callbacks.onError?.(errMsg);
          // 返回失败结果，包含错误信息
          // 这种方式：
          // 1. Promise 会进入 resolved 状态（成功状态）
          // 2. 返回一个包含错误信息的对象
          // 3. 调用方需要使用 .then() 来处理，并在 then 中判断 success 是否为 false
          // 4. 调用示例：
          // pullImage(params, callbacks)
          // .then(result => {
          //   if (!result.success) {
          //     // 处理错误情况
          //     console.error(result.message);
          //   } else {
          //     // 处理成功情况
          //   }
          // });
          resolve({ success: false, message: errMsg });
        },

        // 设置请求超时时间，单位为毫秒
        // 如果 30 秒内没有收到响应，将触发超时处理
        timeout: 30000,

        // 超时时的回调函数
        // 当请求超过指定时间未收到响应时触发
        onTimeout: () => {
          // 调用外部传入的错误回调函数，通知调用者请求超时
          callbacks.onError?.('请求超时');
          // 返回超时结果
          resolve({ success: false, message: '请求超时' });
        },
      })
      // 捕获发送消息过程中可能出现的错误
      // 例如：WebSocket 连接断开、消息发送失败等
      .catch((err) => {
        // 调用外部传入的错误回调函数，通知调用者发送失败
        callbacks.onError?.('发送失败');
        // 返回发送失败的结果
        resolve({ success: false, message: '发送失败' });
      });
  });
};

/**
 * 启动 Docker 容器
 * 该方法通过 WebSocket 与后端通信，实现 Docker 容器的启动功能
 * 支持错误处理和超时处理
 *
 * @param containerId - 要启动的容器 ID，用于标识具体的容器
 * @returns Promise<{success: boolean; message?: string}> - 返回启动结果，包含成功状态和可能的错误信息
 */
export const startContainer = async (containerId: string): Promise<{ success: boolean; message?: string }> => {
  // 创建一个新的 Promise 对象，用于异步处理启动容器的结果
  // 使用 resolve 和 reject 分别处理成功和失败的情况
  return new Promise((resolve, reject) => {
    // 调用 WebSocket 服务发送启动容器的请求
    dockerWebSocketService.sendWebSocketMessage({
      // 指定消息类型为启动容器，后端根据此类型执行相应的操作
      type: WebSocketMessageType.CONTAINER_START,

      // 传递容器 ID 给后端，用于标识要启动的容器
      data: { containerId },

      // 任务完成时的回调函数
      // 当容器成功启动时触发
      onComplete: (data) => {
        // 返回成功结果
        resolve({ success: true, message: data });
      },

      // 发生错误时的回调函数
      // 当启动过程中出现错误时触发
      // errMsg 参数包含具体的错误信息
      onError: (errMsg) => {
        // 创建一个新的 Error 对象并返回，包含错误信息
        reject(new Error(errMsg));
      },

      // 设置请求超时时间，单位为毫秒
      // 如果 30 秒内没有收到响应，将触发超时处理
      timeout: 30000,

      // 超时时的回调函数
      // 当请求超过指定时间未收到响应时触发
      onTimeout: () => {
        // 创建一个新的 Error 对象并返回，表示请求超时
        reject(new Error('请求超时'));
      },
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
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.CONTAINER_STOP,
      data: { containerId },
      onComplete: (data) => {
        resolve({ success: true });
      },
      onError: (err) => {
        reject(new Error(err));
      },
      timeout: 30000,
      onTimeout: () => {
        reject({ success: false, message: '请求超时' });
      },
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
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.CONTAINER_RESTART,
      data: { containerId },
      onComplete: (data) => {
        resolve({ success: true });
      },
      onError: (err) => {
        reject({ success: false, message: err });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({ success: false, message: '请求超时' });
      },
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
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.CONTAINER_DETAIL,
      data: { containerId },
      onComplete: (data) => {
        resolve(data || []);
      },
      onError: (err) => {
        reject(new Error(err));
      },
      timeout: 30000,
      onTimeout: () => {
        reject(new Error('请求超时'));
      },
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
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.CONTAINER_LOGS,
      data: { containerId },
      onComplete: (data) => {
        resolve({ data });
      },
      onError: (err) => {
        reject(new Error(err));
      },
      timeout: 30000,
      onTimeout: () => {
        reject(new Error('请求超时'));
      },
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
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.CONTAINER_STATS,
      data: { containerId },
      onComplete: (data) => {
        resolve(data || []);
      },
      onError: (err) => {
        reject(new Error(err));
      },
      timeout: 30000,
      onTimeout: () => {
        reject(new Error('请求超时'));
      },
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
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.CONTAINER_DELETE,
      data: { containerId },
      onComplete: (data) => {
        resolve({ success: true });
      },
      onError: (err) => {
        reject(new Error(err));
      },
      timeout: 30000,
      onTimeout: () => {
        reject(new Error('请求超时'));
      },
    });
  });
};

/**
 * 更新容器
 * @param containerId 容器ID
 * @param callbacks 回调函数
 * @returns Promise<{success: boolean; message?: string; newContainerId?: string}>
 */
export const updateContainer = async (
  containerId: string,
  callbacks: DockerWebSocketCallbacks,
): Promise<{
  success: boolean;
  message?: string;
  newContainerId?: string;
}> => {
  return new Promise((resolve) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.CONTAINER_UPDATE,
      data: { containerId },
      onStart: (taskId) => {
        console.log(`[更新容器] 任务开始，taskId: ${taskId}`);
        callbacks?.onStart?.(taskId);
      },
      onProgress: (progress) => {
        console.log(`[更新容器] 进度更新: ${progress}%`);
        callbacks?.onProgress?.(progress);
      },
      onLog: (log) => {
        console.log(`[更新容器] 日志: ${log}`);
        callbacks?.onLog?.(log);
      },
      onComplete: (data) => {
        callbacks?.onComplete?.();
        resolve({
          success: true,
          newContainerId: data,
        });
      },
      onError: (errMsg) => {
        callbacks?.onError?.(errMsg);
        resolve({
          success: false,
          message: errMsg,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        // 调用外部传入的错误回调函数，通知调用者请求超时
        callbacks.onError?.('请求超时');
        // 返回超时结果
        resolve({ success: false, message: '请求超时' });
      },
    });
  });
};

/**
 * 获取网络列表
 * @returns Promise<{success: boolean; message?: string; data?: NetworkListResponse}>
 */
export const getNetworkList = (): Promise<{ success: boolean; message?: string; data?: NetworkListResponse }> => {
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.NETWORK_LIST,
      data: {},
      onComplete: (data) => {
        resolve({
          success: true,
          data: data || [],
        });
      },
      onError: (err) => {
        reject({
          success: false,
          message: err,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({
          success: false,
          message: '请求超时',
        });
      },
    });
  });
};

/**
 * 获取镜像列表
 * @returns Promise<{success: boolean; message?: string; data?: ImageListResponse}>
 */
export const getImageList = async (): Promise<{ success: boolean; message?: string; data?: ImageListResponse }> => {
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.IMAGE_LIST,
      data: {},
      onComplete: (data) => {
        resolve({
          success: true,
          data: data || [],
        });
      },
      onError: (err) => {
        reject({
          success: false,
          message: err,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({
          success: false,
          message: '请求超时',
        });
      },
    });
  });
};

/**
 * 获取镜像详情
 * @param imageId 镜像ID
 * @returns Promise<{success: boolean; message?: string; data?: ImageDetailResponse}>
 */
export const getImageDetail = async (
  imageId: string,
): Promise<{
  success: boolean;
  message?: string;
  data?: ImageDetailResponse;
}> => {
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.IMAGE_DETAIL,
      data: { imageId },
      onComplete: (data) => {
        console.log('getImageDetail', data);
        resolve({
          success: true,
          data: data || null,
        });
      },
      onError: (err) => {
        reject({
          success: false,
          message: err,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({
          success: false,
          message: '请求超时',
        });
      },
    });
  });
};

/**
 * 创建容器
 * @param data 容器创建参数
 * @param callbacks 回调函数
 * @returns Promise<string> 返回容器ID
 */
export const createContainer = async (
  data: CreateContainerParams,
  callbacks?: DockerWebSocketCallbacks,
): Promise<string> => {
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.CONTAINER_CREATE,
      data,
      onStart: (taskId) => {
        console.log(`[创建容器] 任务开始，taskId: ${taskId}`);
        callbacks?.onStart?.(taskId);
      },
      onProgress: (progress) => {
        console.log(`[创建容器] 进度更新: ${progress}%`);
        callbacks?.onProgress?.(progress);
      },
      onLog: (log) => {
        console.log(`[创建容器] 日志: ${log}`);
        callbacks?.onLog?.(log);
      },
      onComplete: (data) => {
        callbacks?.onComplete?.();
        resolve(data);
      },
      onError: (errMsg) => {
        callbacks?.onError?.(errMsg);
        reject(new Error(errMsg));
      },
      timeout: 30000,
      onTimeout: () => {
        const errorMsg = '请求超时';
        callbacks?.onError?.(errorMsg);
        reject(new Error(errorMsg));
      },
    });
  });
};

/**
 * 删除应用模板
 * @param templateId 模板ID
 * @returns Promise<{success: boolean; message?: string}>
 */
export const deleteTemplate = async (templateId: string): Promise<{ success: boolean; message?: string }> => {
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.DELETE_TEMPLATE,
      data: { templateId },
      onComplete: () => {
        resolve({
          success: true,
        });
      },
      onError: (err) => {
        reject({
          success: false,
          message: err,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({
          success: false,
          message: '请求超时',
        });
      },
    });
  });
};

export const deleteImage = async (imageId: string): Promise<{ success: boolean; message?: string }> => {
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.IMAGE_DELETE,
      data: { imageId },
      onComplete: () => {
        resolve({
          success: true,
        });
      },
      onError: (err) => {
        reject({
          success: false,
          message: err,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({
          success: false,
          message: '请求超时',
        });
      },
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
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.IMAGE_UPDATE,
      data: params,
      onComplete: (data) => {
        resolve({
          code: 0,
          message: 'success',
          data,
        });
      },
      onError: (err) => {
        reject(new Error(err));
      },
      timeout: 30000,
      onTimeout: () => {
        reject(new Error('请求超时'));
      },
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
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.IMAGE_BATCH_UPDATE,
      data: params,
      onComplete: (data) => {
        if (data.success) {
          resolve({
            code: 0,
            message: 'success',
            data,
          });
        } else {
          reject(new Error(data.message));
        }
      },
      onError: (err) => {
        reject(new Error(err));
      },
      timeout: 30000,
      onTimeout: () => {
        reject(new Error('请求超时'));
      },
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
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.IMAGE_CANCEL_PULL,
      data: { taskId },
      onComplete: (data) => {
        if (data.success) {
          resolve({
            code: 0,
            message: 'success',
            data,
          });
        } else {
          reject(new Error(data.message));
        }
      },
      onError: (err) => {
        reject(new Error(err));
      },
      timeout: 30000,
      onTimeout: () => {
        reject(new Error('请求超时'));
      },
    });
  });
};

/**
 * 检查镜像更新
 * @returns Promise<any>
 */
export const checkImageUpdates = async (): Promise<any> => {
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.IMAGE_CHECK_UPDATES,
      data: {},
      onComplete: (data) => {
        resolve({
          code: 0,
          message: 'success',
          data,
        });
      },
      onError: (err) => {
        reject(new Error(err));
      },
      timeout: 30000,
      onTimeout: () => {
        reject(new Error('请求超时'));
      },
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
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.INSTALL_CHECK_IMAGES,
      data: { images },
      onComplete: (data) => {
        resolve({
          success: true,
          data,
        });
      },
      onError: (err) => {
        reject({
          success: false,
          message: err,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({
          success: false,
          message: '请求超时',
        });
      },
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
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.INSTALL_VALIDATE,
      data: { params },
      onComplete: (data) => {
        resolve({
          success: true,
          data,
        });
      },
      onError: (err) => {
        reject({
          success: false,
          message: err,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({
          success: false,
          message: '请求超时',
        });
      },
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
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.INSTALL_START,
      data: params,
      onStart: (taskId) => {
        console.log(`[开始安装] 任务开始，taskId: ${taskId}`);
      },
      onProgress: (progress) => {
        console.log(`[开始安装] 进度更新: ${progress}%`);
      },
      onLog: (log) => {
        console.log(`[开始安装] 日志: ${log}`);
      },
      onComplete: (data) => {
        if (data?.success) {
          resolve({
            success: true,
            data,
          });
        } else {
          reject({
            success: false,
            message: data?.message,
          });
        }
      },
      onError: (err) => {
        reject({
          success: false,
          message: err,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({
          success: false,
          message: '请求超时',
        });
      },
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
  return new Promise((resolve, reject) => {
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.IMPORT_TEMPLATE,
      data: { content, fileName },
      onComplete: (data) => {
        resolve({
          success: data?.success || false,
          message: data?.message,
        });
      },
      onError: (err) => {
        reject({
          success: false,
          message: err,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({
          success: false,
          message: '请求超时',
        });
      },
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
    dockerWebSocketService.sendWebSocketMessage({
      type: WebSocketMessageType.CONTAINER_JSON_CONFIG,
      data: { containerId },
      onComplete: (data) => {
        resolve({
          success: true,
          data,
        });
      },
      onError: (err) => {
        reject({
          success: false,
          message: err,
        });
      },
      timeout: 30000,
      onTimeout: () => {
        reject({
          success: false,
          message: '请求超时',
        });
      },
    });
  });
};
