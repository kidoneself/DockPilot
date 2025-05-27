import { ref, onUnmounted } from 'vue'
import { sendWebSocketMessage } from '@/api/websocket/websocketService'
import type {
  WebSocketCallbacks,
  DockerWebSocketMessage
} from '@/api/websocket/types'

interface UseWebSocketTaskOptions {
  type: string
  data?: any
  timeout?: number
  autoStart?: boolean
  onProgress?: (progress: number, taskId: string) => void
  onLog?: (log: string, taskId: string) => void
  onComplete?: (data: DockerWebSocketMessage, taskId: string) => void
  onError?: (error: string, taskId: string) => void
}

export function useWebSocketTask(options: UseWebSocketTaskOptions) {
  const loading = ref(false)
  const progress = ref(0)
  const logs = ref<string[]>([])
  const error = ref<string | null>(null)
  const taskId = ref<string>('')

  // 业务层可自定义回调，也可直接用响应式数据
  const callbacks: WebSocketCallbacks = {
    onProgress: (p, id) => {
      progress.value = p
      options.onProgress?.(p, id)
    },
    onLog: (log, id) => {
      logs.value.push(log)
      options.onLog?.(log, id)
    },
    onComplete: (data, id) => {
      loading.value = false
      options.onComplete?.(data, id)
    },
    onError: (err, id) => {
      error.value = err
      loading.value = false
      options.onError?.(err, id)
    }
  }

  // 启动任务
  const start = (runtimeData?: any) => {
    loading.value = true
    progress.value = 0
    logs.value = []
    error.value = null
    sendWebSocketMessage({
      type: options.type,
      data: runtimeData || options.data,
      callbacks,
      timeout: options.timeout
    })
  }

  // 自动启动
  if (options.autoStart) start()

  // 自动清理
  onUnmounted(() => {
    // 这里可以做一些任务取消、回调解绑等操作（如有需要）
  })

  return {
    loading,
    progress,
    logs,
    error,
    start,
    taskId
  }
} 