import { ref, computed, onUnmounted } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { startAsyncPackage, getPackageTaskStatus, downloadPackageFile, type PackageTask, type ProjectExportRequest } from '@/api/containerYaml'

/**
 * 打包任务管理组合式函数
 * 包含任务状态持久化、轮询、下载等完整逻辑
 */
export function usePackageTask() {
  const message = useMessage()
  const dialog = useDialog()
  
  // 状态管理
  const packageTask = ref<PackageTask | null>(null)
  const packageTimer = ref<number | null>(null)
  const showPackageProgressModal = ref(false)
  
  // 🔥 任务状态持久化相关
  const PACKAGE_TASK_KEY = 'dockpilot_package_task'
  
  // 🔥 保存任务状态到localStorage
  function saveTaskToStorage(task: PackageTask) {
    try {
      const taskData = {
        taskId: task.taskId,
        projectName: task.projectName,
        containerIds: task.containerIds,
        status: task.status,
        progress: task.progress,
        currentStep: task.currentStep,
        createTime: task.createTime,
        timestamp: Date.now() // 添加时间戳用于过期检查
      }
      localStorage.setItem(PACKAGE_TASK_KEY, JSON.stringify(taskData))
      console.log('💾 任务状态已保存到localStorage:', taskData)
    } catch (error) {
      console.warn('保存任务状态失败:', error)
    }
  }
  
  // 🔥 从localStorage恢复任务状态
  function loadTaskFromStorage(): any | null {
    try {
      const taskData = localStorage.getItem(PACKAGE_TASK_KEY)
      if (!taskData) return null
      
      const parsed = JSON.parse(taskData)
      
      // 检查任务是否过期（超过1小时自动清除）
      const maxAge = 60 * 60 * 1000 // 1小时
      if (Date.now() - parsed.timestamp > maxAge) {
        localStorage.removeItem(PACKAGE_TASK_KEY)
        console.log('🗑️ 过期任务已清除')
        return null
      }
      
      // 只恢复进行中的任务
      if (parsed.status === 'processing' || parsed.status === 'pending') {
        console.log('🔄 发现进行中的任务:', parsed)
        return parsed
      } else {
        // 已完成或失败的任务可以清除
        localStorage.removeItem(PACKAGE_TASK_KEY)
        return null
      }
    } catch (error) {
      console.warn('恢复任务状态失败:', error)
      localStorage.removeItem(PACKAGE_TASK_KEY)
      return null
    }
  }
  
  // 🔥 清除任务状态
  function clearTaskFromStorage() {
    localStorage.removeItem(PACKAGE_TASK_KEY)
    console.log('🗑️ 任务状态已清除')
  }
  
  // 🔥 页面加载时检查并恢复任务
  function checkAndRestoreTask() {
    const savedTask = loadTaskFromStorage()
    if (savedTask) {
      console.log('🔄 正在恢复任务:', savedTask.taskId)
      
      // 显示恢复提示
      message.info(`发现进行中的打包任务，正在恢复...（${savedTask.projectName || '容器项目'}）`)
      
      // 开始静默轮询，获取最新状态
      startSilentPollingTaskStatus(savedTask.taskId, true)
    }
  }
  
  // 处理打包进度的计算属性
  const packageProgress = computed(() => {
    if (!packageTask.value) return 0
    return typeof packageTask.value.progress === 'number' 
      ? packageTask.value.progress 
      : Number(packageTask.value.progress) || 0
  })
  
  // 格式化文件大小
  function formatBytes(bytes: number, decimals = 1) {
    if (!bytes) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(decimals)) + ' ' + sizes[i]
  }
  
  // 🔥 启动异步打包任务
  async function startPackageTask(params: ProjectExportRequest): Promise<{ taskId: string }> {
    try {
      const result = await startAsyncPackage(params)
      console.log('🚀 异步打包任务已启动:', result)
      
      // 🔥 显示简单的成功提示，不显示进度弹窗
      message.success('打包任务已启动，完成后将自动下载')
      
      // 🔥 后台静默轮询任务状态，不显示界面
      startSilentPollingTaskStatus(result.taskId)
      
      return result
    } catch (error: any) {
      message.error('启动打包任务失败: ' + (error.message || error))
      throw error
    }
  }
  
  // 🔥 静默轮询任务状态（不显示进度界面）
  function startSilentPollingTaskStatus(taskId: string, restore?: boolean) {
    if (packageTimer.value) {
      clearInterval(packageTimer.value)
    }
    
    packageTimer.value = window.setInterval(async () => {
      try {
        const task = await getPackageTaskStatus(taskId)
        
        console.log('📊 后台任务状态:', task.status, `${task.progress}%`, task.currentStep)
        
        // 🔥 保存任务状态到localStorage
        if (task.status === 'processing' || task.status === 'pending') {
          saveTaskToStorage(task)
        }
        
        if (task.status === 'completed') {
          // 任务完成，停止轮询
          stopPollingTaskStatus()
          
          // 🔥 清除保存的任务状态
          clearTaskFromStorage()
          
          // 🔥 直接下载文件，无需确认
          downloadCompletedPackage(taskId)
          
          // 🔥 显示完成通知
          message.success(`打包完成！文件正在下载... (${formatBytes(task.fileSize || 0, 1)})`)
          
        } else if (task.status === 'failed') {
          // 任务失败，停止轮询
          stopPollingTaskStatus()
          
          // 🔥 清除保存的任务状态
          clearTaskFromStorage()
          
          // 🔥 显示失败通知，提供重试选项
          message.error('打包失败: ' + (task.errorMessage || '未知错误'))
          
          // 可选：提供重试按钮
          dialog.error({
            title: '打包失败',
            content: `打包过程中发生错误：${task.errorMessage || '未知错误'}`,
            positiveText: '知道了',
            negativeText: '重试',
            onNegativeClick: () => {
              // 重新开始打包流程
              // 这里需要从外部传入重试函数
              console.log('用户选择重试，需要外部处理')
            }
          })
        }
        
      } catch (error: any) {
        console.error('查询任务状态失败:', error)
        // 🔥 网络错误时继续轮询，但减少频率
      }
    }, 3000) // 🔥 调整为3秒轮询一次，减少服务器压力
  }
  
  // 停止轮询任务状态
  function stopPollingTaskStatus() {
    if (packageTimer.value) {
      clearInterval(packageTimer.value)
      packageTimer.value = null
    }
  }
  
  // 下载完成的打包文件
  function downloadCompletedPackage(taskId: string) {
    try {
      const downloadUrl = downloadPackageFile(taskId)
      
      // 创建隐藏的下载链接
      const a = document.createElement('a')
      a.href = downloadUrl
      a.style.display = 'none'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      
      console.log('📥 文件下载已启动:', downloadUrl)
      
    } catch (error: any) {
      message.error('下载文件失败: ' + (error.message || error))
    }
  }
  
  // 关闭打包进度弹窗
  function closePackageProgressModal() {
    showPackageProgressModal.value = false
    stopPollingTaskStatus()
    packageTask.value = null
    // 🔥 清除保存的任务状态
    clearTaskFromStorage()
  }
  
  // 格式化文件大小（兼容性函数）
  function formatFileSize(bytes: number): string {
    return formatBytes(bytes, 1)
  }
  
  // 清理资源
  onUnmounted(() => {
    stopPollingTaskStatus()
  })
  
  return {
    // 状态
    packageTask,
    showPackageProgressModal,
    packageProgress,
    
    // 方法
    startPackageTask,
    checkAndRestoreTask,
    closePackageProgressModal,
    stopPollingTaskStatus,
    formatFileSize,
    formatBytes,
    
    // 内部方法（如果需要外部访问）
    saveTaskToStorage,
    clearTaskFromStorage,
    loadTaskFromStorage
  }
} 