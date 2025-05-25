<template>
  <div class="image-list">
    <n-card>
      <template #header>
        <NSpace justify="space-between">
          <NSpace>
            <NButton @click="handleRefresh" :loading="loading">
              <template #icon>
                <n-icon><RefreshOutline /></n-icon>
              </template>
              刷新
            </NButton>
            <NButton @click="showDrawer = true">
              <template #icon>
                <n-icon><DownloadOutline /></n-icon>
              </template>
              拉取镜像
            </NButton>
          </NSpace>
          <SearchBar v-model="searchText" placeholder="搜索镜像" />
        </NSpace>
      </template>

      <NSpace vertical size="large">
        <ImageItem
          v-for="image in filteredImages"
          :key="image.id"
          :image="image"
          @action="handleImageAction"
          @retry-pull="handleRetryPull"
        />
      </NSpace>

      <template #footer>
        <n-empty v-if="filteredImages.length === 0" description="暂无镜像" />
      </template>
    </n-card>

    <!-- 拉取镜像模态框 -->
    <n-modal
      v-model:show="showDrawer"
      preset="card"
      style="width: 500px"
      title="拉取镜像"
      :mask-closable="false"
    >
      <n-form
        ref="formRef"
        :model="formValue"
        :rules="rules"
        label-placement="left"
        label-width="auto"
        require-mark-placement="right-hanging"
      >
        <n-form-item label="镜像名称" path="name">
          <n-input 
            v-model:value="formValue.name" 
            placeholder="例如：nginx:latest 或 ubuntu:20.04"
            :disabled="pulling"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="handleCancel" :disabled="pulling">取消</NButton>
          <NButton 
            type="primary" 
            :loading="pulling" 
            @click="handleSubmit"
            :disabled="!formValue.name || pulling"
          >
            {{ pulling ? '拉取中...' : '开始拉取' }}
          </NButton>
        </NSpace>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, onUnmounted } from 'vue'
import { NButton, NSpace, useMessage, useDialog } from 'naive-ui'
import {
  RefreshOutline,
  DownloadOutline
} from '@vicons/ionicons5'
import ImageItem from '@/components/image/ImageItem.vue'
import SearchBar from '@/components/common/SearchBar.vue'
import { getImageList, pullImage, deleteImage } from '@/api/image'
import type { Image, PullStatus } from '@/api/model/imageModel'
import { useRouter } from 'vue-router'

const message = useMessage()
const router = useRouter()
const dialog = useDialog()

// 基础状态
const images = ref<Image[]>([])
const loading = ref(false)
const showDrawer = ref(false)
const searchText = ref('')
const formRef = ref(null)

// 拉取相关状态
const pulling = ref(false)
const pullComplete = ref(false)
const pullError = ref('')
const progress = ref(0)
const logs = ref<string[]>([])
const currentStatus = ref<'pulling' | 'success' | 'failed' | null>(null)
const statusMessage = ref('')
const pullImageName = ref('')

// 表单数据
const formValue = reactive({
  name: ''
})

// 表单验证规则
const rules = {
  name: {
    required: true,
    message: '请输入镜像名称',
    trigger: 'blur'
  }
}

// 计算属性：过滤后的镜像列表
const filteredImages = computed(() => {
  if (!searchText.value) return images.value
  return images.value.filter(image => 
    image.name.toLowerCase().includes(searchText.value.toLowerCase()) ||
    image.tag.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

// 重置拉取状态
function resetPullState() {
  pulling.value = false
  pullComplete.value = false
  pullError.value = ''
  progress.value = 0
  logs.value = []
  currentStatus.value = null
  statusMessage.value = ''
}

// 取消拉取
function handleCancel() {
  showDrawer.value = false
  resetPullState()
  formValue.name = ''
}

// 刷新镜像列表
async function handleRefresh() {
  loading.value = true
  try {
    await getImageList({
      onComplete: (data) => {
        console.log('获取到的镜像列表数据:', data)
        if (Array.isArray(data)) {
          images.value = data.map((item: any) => {
            // 解析拉取状态
            let pullStatus: PullStatus | undefined
            let isRealImage = item.size > 0 // 真实存在的镜像
            
            if (item.progress) {
              try {
                const progressData = JSON.parse(item.progress)
                pullStatus = {
                  status: progressData.status || 'idle',
                  percentage: progressData.percentage || 0,
                  message: progressData.message || '',
                  error: progressData.error,
                  start_time: progressData.start_time,
                  end_time: progressData.end_time
                }
                
                // 添加调试日志
                if (progressData.status === 'failed') {
                  console.log('🔍 解析到失败镜像:', item.name + ':' + item.tag, progressData)
                }
              } catch (e) {
                console.warn('解析progress失败:', e, 'raw progress:', item.progress)
              }
            }

            // 对于没有progress但pulling为false且size为0的记录，也认为是失败状态
            if (!pullStatus && !isRealImage && item.pulling === false) {
              pullStatus = {
                status: 'failed',
                percentage: 0,
                message: '拉取失败 - 无详细信息',
                error: '镜像拉取失败，请重试'
              }
              console.log('🔍 设置默认失败状态:', item.name + ':' + item.tag)
            }

            // 如果没有progress但镜像真实存在，说明是成功的镜像
            if (isRealImage && !pullStatus) {
              pullStatus = {
                status: 'success',
                percentage: 100,
                message: '镜像已存在'
              }
            }

            // 判断是否可以重试
            let canRetry = false
            if (!isRealImage) {
              // Docker中不存在的记录
              if (pullStatus?.status === 'failed') {
                canRetry = true // 拉取失败可以重试
                console.log('🔍 设置可重试:', item.name + ':' + item.tag, 'pullStatus:', pullStatus)
              } else if (item.pulling === false && !pullStatus) {
                canRetry = true // 没有拉取记录的也可以尝试拉取
              }
            }

            return {
              id: item.id || `image-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
              name: item.name || '未命名镜像',
              tag: item.tag || 'latest',
              size: item.size || 0,
              created: item.created ? new Date(item.created).getTime() : Date.now(),
              needUpdate: item.needUpdate || false,
              statusId: item.statusId || 0,
              pullStatus,
              canRetry,
              // 新增字段用于区分镜像类型
              isRealImage // 是否是Docker中真实存在的镜像
            }
          })
        } else {
          console.error('镜像列表数据格式不正确:', data)
          message.error('获取镜像列表失败：数据格式不正确')
        }
      },
      onError: (error) => {
        console.error('获取镜像列表失败:', error)
        message.error(`获取镜像列表失败: ${error}`)
      }
    })
  } finally {
    loading.value = false
  }
}

// 开始拉取
function startPull() {
  const imageName = formValue.name
  
  pullImage(
    { imageName },
    {
      onProgress: () => {
        // WebSocket会自动更新列表中的进度
      },
      onLog: () => {
        // WebSocket会自动处理日志
      },
      onComplete: () => {
        message.success(`镜像 "${imageName}" 拉取成功`)
        handleRefresh()
      },
      onError: (error: string) => {
        message.error(`镜像 "${imageName}" 拉取失败`)
        handleRefresh()
      }
    }
  )
  
  // 立即关闭模态框
  showDrawer.value = false
  resetPullState()
  formValue.name = ''
  message.info(`镜像 "${imageName}" 开始拉取，请在列表中查看进度`)
  
  // 刷新一次列表显示拉取中状态
  handleRefresh()
}

// 提交表单
async function handleSubmit() {
  if (!formValue.name) {
    message.error('请输入镜像名称')
    return
  }

  pulling.value = true
  
  try {
    startPull()
  } catch (error) {
    console.error('拉取镜像出错:', error)
    message.error('镜像拉取失败')
    pulling.value = false
  }
}

// 处理删除镜像
function handleDeleteImage(image: Image) {
  // 使用镜像名:标签作为删除ID
  const imageId = `${image.name}:${image.tag}`
  const isRealImage = image.isRealImage
  const actionText = isRealImage ? '镜像' : '记录'
  
  deleteImage(imageId, {
    onComplete: () => {
      message.success(`${actionText} "${imageId}" 删除成功`)
      // 刷新镜像列表
      handleRefresh()
    },
    onError: (error: string) => {
      message.error(`删除${actionText}失败: ${error}`)
    }
  })
}

// 处理更新镜像
function handleUpdateImage(image: Image) {
  const imageTag = `${image.name}:${image.tag}`
  console.log('🔄 开始更新镜像:', imageTag)
  
  // 备份原始状态，以便更新失败时恢复
  const originalIsRealImage = image.isRealImage
  const originalPullStatus = image.pullStatus
  
  // 设置更新中状态 - 但保持isRealImage为true，确保原镜像仍可用
  const targetImage = images.value.find(img => img.id === image.id)
  if (!targetImage) {
    console.error('❌ 找不到目标镜像:', image.id)
    message.error('更新失败：找不到目标镜像')
    return
  }
  
  console.log('✅ 找到目标镜像，设置初始状态')
  targetImage.pullStatus = {
    status: 'pulling',
    percentage: 0,
    message: '正在更新镜像...'
  }
  // 关键：保持 isRealImage 为 true，这样用户依然可以创建容器
  targetImage.isRealImage = originalIsRealImage
  
  // 使用 PULL_IMAGE 消息类型，因为更新本质就是重新拉取
  // 这样可以获得完整的进度回调支持
  pullImage(
    { imageName: imageTag },
    {
      onProgress: (progressValue: number) => {
        console.log(`📈 更新进度: ${progressValue}%`)
        if (targetImage) {
          targetImage.pullStatus = {
            status: 'pulling',
            percentage: progressValue,
            message: `更新进度: ${progressValue}%`
          }
          console.log('📊 已更新UI进度:', targetImage.pullStatus)
        }
      },
      onLog: (log: string) => {
        console.log(`📝 更新日志: ${log}`)
        if (targetImage) {
          const currentPercentage = targetImage.pullStatus?.percentage || 0
          targetImage.pullStatus = {
            status: 'pulling',
            percentage: currentPercentage,
            message: log
          }
          console.log('📋 已更新UI日志:', targetImage.pullStatus)
        }
      },
      onComplete: () => {
        console.log('✅ 更新完成:', imageTag)
        if (targetImage) {
          targetImage.pullStatus = {
            status: 'success',
            percentage: 100,
            message: '更新完成'
          }
        }
        message.success(`镜像 "${imageTag}" 更新成功`)
        // 更新成功，刷新列表获取最新信息
        setTimeout(() => {
          console.log('🔄 刷新镜像列表以获取最新信息')
          handleRefresh()
        }, 1000)
      },
      onError: (error: string) => {
        console.error('❌ 更新镜像失败:', error)
        
        // 关键处理：更新失败时恢复原状态，确保原镜像依然可用
        if (targetImage) {
          targetImage.isRealImage = originalIsRealImage // 恢复原来的可用状态
          targetImage.pullStatus = {
            status: 'failed',
            percentage: 0,
            message: '更新失败，但原镜像依然可用',
            error: `更新失败: ${error}`
          }
          targetImage.canRetry = false // 更新失败不显示重试按钮，因为原镜像可用
          console.log('🔄 已恢复原始状态:', targetImage.pullStatus)
        }
        
        message.error(`镜像更新失败，但原镜像依然可以使用`)
      }
    }
  )
}

// 处理镜像操作
function handleImageAction(action: string, image: Image) {
  console.log('镜像操作:', action, image)
  switch (action) {
    case 'create':
      // 跳转到容器创建页面，携带镜像信息
      router.push({
        path: '/containers/create',
        query: {
          image: `${image.name}:${image.tag}`
        }
      })
      break
    case 'detail':
      router.push(`/images/${image.id}`)
      break
    case 'delete':
      // 显示确认删除对话框
      const isRealImage = image.isRealImage
      const actionText = isRealImage ? '删除镜像' : '删除记录'
      const contentText = isRealImage 
        ? `确定要删除镜像 "${image.name}:${image.tag}" 吗？此操作不可撤销。`
        : `确定要删除拉取记录 "${image.name}:${image.tag}" 吗？`
      
      dialog.warning({
        title: `确认${actionText}`,
        content: contentText,
        positiveText: `确认${actionText}`,
        negativeText: '取消',
        onPositiveClick: () => {
          handleDeleteImage(image)
        }
      })
      break
    case 'update':
      // 显示确认更新对话框
      dialog.info({
        title: '确认更新镜像',
        content: `确定要更新镜像 "${image.name}:${image.tag}" 到最新版本吗？`,
        positiveText: '确认更新',
        negativeText: '取消',
        onPositiveClick: () => {
          handleUpdateImage(image)
        }
      })
      break
    default:
      console.warn('未知操作:', action)
  }
}

// 处理重试拉取
function handleRetryPull(image: Image) {
  formValue.name = `${image.name}:${image.tag}`
  showDrawer.value = true
  resetPullState()
}

// 初始化
onMounted(() => {
  console.log('🚀 ImageList 组件初始化')
  handleRefresh()
})

// 组件卸载时清理WebSocket监听
onUnmounted(() => {
  // 无需特殊清理
})
</script>

<style scoped>
.image-list {
  padding: 16px;
}
</style> 