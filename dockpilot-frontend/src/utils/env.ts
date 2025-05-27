// 获取环境变量
export function getEnv(key: string): string {
  return import.meta.env[key] || ''
} 