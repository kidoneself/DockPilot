/*
 * 表格列配置（镜像、网络等）
 * 生成时间：2024-06-09 22:00:00
 */

// 镜像表格列配置
export const IMAGE_TABLE_COLUMNS = [
  { colKey: 'id', title: '镜像ID', width: 120 },
  { colKey: 'name', title: '镜像名称', width: 300 },
  { colKey: 'tag', title: '标签', width: 100 },
  { colKey: 'created', title: '创建时间', width: 180 },
  { colKey: 'operation', title: '操作', width: 200 },
];

// 网络表格列配置
export const NETWORK_TABLE_COLUMNS = [
  { colKey: 'id', title: '网络ID', width: 200 },
  { colKey: 'name', title: '网络名称', width: 200 },
  { colKey: 'driver', title: '驱动', width: 100 },
  { colKey: 'scope', title: '作用域', width: 100 },
  { colKey: 'subnet', title: '子网', width: 200 },
  { colKey: 'operation', title: '操作', width: 200 },
];

// 日志表格列配置
export const LOG_TABLE_COLUMNS = [
  { colKey: 'id', title: 'ID', width: 80 },
  { colKey: 'type', title: '日志类型', width: 120 },
  { colKey: 'level', title: '日志级别', width: 100 },
  { colKey: 'content', title: '日志内容', width: 400 },
  { colKey: 'createTime', title: '创建时间', width: 180 },
]; 