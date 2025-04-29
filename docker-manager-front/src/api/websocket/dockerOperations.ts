import { ref } from 'vue';
import { dockerWebSocketService } from './DockerWebSocketService';

// 状态变量
export const progress = ref(0);
export const status = ref('');
export const error = ref('');

/**
 * 处理拉取开始消息
 * @param message 消息对象
 */
const handlePullStart = (message: any) => {
    status.value = '开始拉取镜像';
    progress.value = 0;
    error.value = '';
};

/**
 * 处理拉取进度消息
 * @param message 消息对象
 */
const handlePullProgress = (message: any) => {
    progress.value = message.data.progress;
    status.value = message.data.status;
};

/**
 * 处理拉取完成消息
 * @param message 消息对象
 */
const handlePullComplete = (message: any) => {
    status.value = '拉取完成';
    progress.value = 100;
};

/**
 * 处理错误消息
 * @param message 消息对象
 */
const handlePullError = (message: any) => {
    error.value = message.data.error;
    status.value = '拉取失败';
};

/**
 * 注册消息处理器
 */
export const registerHandlers = () => {
    dockerWebSocketService.on('PULL_START', handlePullStart);
    dockerWebSocketService.on('PULL_PROGRESS', handlePullProgress);
    dockerWebSocketService.on('PULL_COMPLETE', handlePullComplete);
    dockerWebSocketService.on('ERROR', handlePullError);
};

/**
 * 移除消息处理器
 */
export const removeHandlers = () => {
    dockerWebSocketService.off('PULL_START', handlePullStart);
    dockerWebSocketService.off('PULL_PROGRESS', handlePullProgress);
    dockerWebSocketService.off('PULL_COMPLETE', handlePullComplete);
    dockerWebSocketService.off('ERROR', handlePullError);
};

/**
 * 拉取镜像
 * @param imageName 镜像名称
 */
export const pullImage = (imageName: string) => {
    dockerWebSocketService.send({
        type: 'PULL_IMAGE',
        taskId: '',
        data: { imageName }
    });
}; 