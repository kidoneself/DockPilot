/*
 * 格式化工具函数
 * 生成时间：2024-06-09 22:00:00
 */

// 日期格式化，将日期对象/字符串/数字转为本地字符串
export function formatDate(date: string | number | Date): string {
  if (!date) return '-';
  const d = new Date(date);
  return d.toLocaleString();
}

// 字节数格式化，将字节数转为带单位的字符串
export function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
} 