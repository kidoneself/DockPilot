/**
 * 生成唯一的任务ID
 * @param prefix 任务前缀，用于标识任务类型
 * @returns 格式化的任务ID
 */
export function generateTaskId(prefix: string): string {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 8);
  return `${prefix}_${timestamp}_${random}`;
}

/**
 * 任务ID前缀枚举
 */
export enum TaskIdPrefix {
  CONTAINER_LIST = 'container_list',
  CONTAINER_START = 'container_start',
  CONTAINER_STOP = 'container_stop',
  CONTAINER_RESTART = 'container_restart',
  CONTAINER_DETAIL = 'container_detail',
  CONTAINER_LOGS = 'container_logs',
  CONTAINER_STATS = 'container_stats',
  CONTAINER_DELETE = 'container_delete',
  CONTAINER_UPDATE = 'container_update',
  CONTAINER_CREATE = 'container_create',
  NETWORK_LIST = 'network_list',
  NETWORK_CREATE = 'network_create',
  NETWORK_DELETE = 'network_delete',
  IMAGE_LIST = 'image_list',
  IMAGE_DETAIL = 'image_detail',
  IMAGE_DELETE = 'image_delete',
  IMAGE_PULL = 'image_pull',
  IMAGE_UPDATE = 'image_update',
  IMAGE_BATCH_UPDATE = 'image_batch_update',
  IMAGE_CANCEL_PULL = 'image_cancel_pull',
  IMAGE_CHECK_UPDATES = 'image_check_updates',
  INSTALL_CHECK = 'install_check',
  INSTALL_VALIDATE = 'install_validate',
  INSTALL_START = 'install_start',
  IMPORT_TEMPLATE = 'import_template'
} 