import { MessagePlugin } from 'tdesign-vue-next';

// 容器状态类型
export type ContainerState = 'running' | 'exited' | 'created' | 'paused' | 'restarting';

// 容器操作类型
export type ContainerOperation = 'starting' | 'stopping' | 'restarting';

// 容器操作状态
export interface ContainerOperationState {
  starting: Set<string>;
  stopping: Set<string>;
  restarting: Set<string>;
}

// 获取容器状态主题
export const getStatusTheme = (state: ContainerState): 'success' | 'warning' | 'primary' | 'default' => {
  const themeMap: Record<ContainerState, 'success' | 'warning' | 'primary' | 'default'> = {
    running: 'success',
    exited: 'warning',
    created: 'primary',
    paused: 'default',
    restarting: 'primary'
  };
  return themeMap[state] || 'default';
};

// 获取容器状态图标
export const getStatusIcon = (state: ContainerState): string => {
  const iconMap: Record<ContainerState, string> = {
    running: 'check-circle',
    exited: 'error-circle',
    created: 'time',
    paused: 'pause',
    restarting: 'time'
  };
  return iconMap[state] || 'help-circle';
};

// 获取容器状态文本
export const getStatusText = (state: ContainerState): string => {
  const textMap: Record<ContainerState, string> = {
    running: '运行中',
    exited: '已停止',
    created: '已创建',
    paused: '已暂停',
    restarting: '重启中'
  };
  return textMap[state] || '未知';
};

// 获取容器状态颜色
export const getStatusColor = (state: ContainerState): string => {
  const colorMap: Record<ContainerState, string> = {
    running: 'var(--td-success-color)',
    exited: 'var(--td-warning-color)',
    created: 'var(--td-primary-color)',
    paused: 'var(--td-gray-color-6)',
    restarting: 'var(--td-primary-color)'
  };
  return colorMap[state] || 'var(--td-gray-color-6)';
};

// 获取容器名称首字母
export const getContainerInitial = (name: string | undefined): string => {
  if (!name) return '?';
  const cleanName = name.replace('/', '');
  return cleanName.charAt(0).toUpperCase();
};

// 通用操作处理函数
export const handleContainerOperation = async (
  operation: () => Promise<void>,
  successMsg: string,
  errorMsg: string
): Promise<void> => {
  try {
    await operation();
    MessagePlugin.success(successMsg);
  } catch (error) {
    MessagePlugin.error(errorMsg);
  }
}; 