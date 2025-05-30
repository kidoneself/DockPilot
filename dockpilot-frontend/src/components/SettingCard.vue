<template>
  <n-card 
    class="setting-card" 
    :class="{ 
      'card-active': setting.status === 'active',
      'card-developing': setting.status === 'developing',
      'card-disabled': setting.status === 'disabled'
    }"
    hoverable
    @click="handleCardClick"
  >
    <div class="card-content">
      <!-- 左侧图标和信息 -->
      <div class="card-left">
        <div class="setting-icon">
          <span class="icon-emoji">{{ setting.icon }}</span>
        </div>
        <div class="setting-info">
          <div class="setting-header">
            <h4 class="setting-title">{{ setting.title }}</h4>
            <n-tag 
              v-if="setting.status"
              :type="getStatusType(setting.status)"
              size="small"
              class="status-tag"
            >
              {{ getStatusText(setting.status) }}
            </n-tag>
          </div>
          <p class="setting-description">{{ setting.desc }}</p>
        </div>
      </div>
      
      <!-- 右侧操作按钮 -->
      <div class="card-right">
        <n-button
          type="primary"
          size="medium"
          :disabled="setting.status === 'disabled'"
          @click.stop="handleConfig"
          class="config-button"
        >
          <template #icon>
            <n-icon>
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 15.5A3.5 3.5 0 0 1 8.5 12A3.5 3.5 0 0 1 12 8.5a3.5 3.5 0 0 1 3.5 3.5a3.5 3.5 0 0 1-3.5 3.5m7.43-2.53c.04-.32.07-.64.07-.97c0-.33-.03-.66-.07-1l2.11-1.63c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.31-.61-.22l-2.49 1c-.52-.39-1.06-.73-1.69-.98l-.37-2.65A.506.506 0 0 0 14 2h-4c-.25 0-.46.18-.5.42l-.37 2.65c-.63.25-1.17.59-1.69.98l-2.49-1c-.22-.09-.49 0-.61.22l-2 3.46c-.13.22-.07.49.12.64L4.57 11c-.04.34-.07.67-.07 1c0 .33.03.65.07.97l-2.11 1.66c-.19.15-.25.42-.12.64l2 3.46c.12.22.39.3.61.22l2.49-1.01c.52.4 1.06.74 1.69.99l.37 2.65c.04.24.25.42.5.42h4c.25 0 .46-.18.5-.42l.37-2.65c.63-.26 1.17-.59 1.69-.99l2.49 1.01c.22.08.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.66Z"/>
              </svg>
            </n-icon>
          </template>
          {{ getButtonText(setting.status) }}
        </n-button>
      </div>
    </div>
  </n-card>
</template>

<script setup lang="ts">
interface SettingItem {
  key: string
  title: string
  desc: string
  configType: string
  status?: 'active' | 'developing' | 'disabled'
  icon?: string
}

interface Props {
  setting: SettingItem
}

interface Emits {
  (e: 'config'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 获取状态类型
const getStatusType = (status?: string) => {
  switch (status) {
    case 'active':
      return 'success'
    case 'developing':
      return 'warning'
    case 'disabled':
      return 'error'
    default:
      return 'default'
  }
}

// 获取状态文本
const getStatusText = (status?: string) => {
  switch (status) {
    case 'active':
      return '可用'
    case 'developing':
      return '开发中'
    case 'disabled':
      return '已禁用'
    default:
      return '未知'
  }
}

// 获取按钮文本
const getButtonText = (status?: string) => {
  switch (status) {
    case 'active':
      return '配置'
    case 'developing':
      return '预览'
    case 'disabled':
      return '不可用'
    default:
      return '配置'
  }
}

// 处理卡片点击
const handleCardClick = () => {
  if (props.setting.status !== 'disabled') {
    handleConfig()
  }
}

// 处理配置按钮点击
const handleConfig = () => {
  emit('config')
}
</script>

<style scoped>
.setting-card {
  width: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--n-border-color);
  position: relative;
  overflow: hidden;
}

.setting-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* 使用伪元素创建状态边框线，确保显示 */
.setting-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: transparent;
  z-index: 10;
  transition: background-color 0.3s ease;
}

.setting-card.card-active::before {
  background: var(--n-success-color);
}

.setting-card.card-developing::before {
  background: var(--n-warning-color);
}

.setting-card.card-disabled::before {
  background: var(--n-error-color);
}

.setting-card.card-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.setting-card.card-disabled:hover {
  transform: none;
  box-shadow: none;
}

/* 覆盖 Naive UI 卡片的默认样式 */
.setting-card :deep(.n-card) {
  border-left: none !important;
}

.setting-card :deep(.n-card__content) {
  padding: 0 !important;
}

.card-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  position: relative;
  z-index: 1;
}

.card-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.setting-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--n-primary-color-hover) 0%, var(--n-primary-color) 100%);
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.icon-emoji {
  font-size: 24px;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.1));
}

.setting-info {
  flex: 1;
  min-width: 0;
}

.setting-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.setting-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--n-text-color-base);
  line-height: 1.4;
}

.status-tag {
  flex-shrink: 0;
}

.setting-description {
  margin: 0;
  font-size: 14px;
  color: var(--n-text-color-2);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-right {
  flex-shrink: 0;
}

.config-button {
  min-width: 80px;
  font-weight: 500;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .card-content {
    padding: 16px;
    gap: 12px;
  }
  
  .card-left {
    gap: 12px;
  }
  
  .setting-icon {
    width: 40px;
    height: 40px;
  }
  
  .icon-emoji {
    font-size: 20px;
  }
  
  .setting-title {
    font-size: 15px;
  }
  
  .setting-description {
    font-size: 13px;
  }
  
  .config-button {
    min-width: 70px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .card-content {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .card-left {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }
  
  .setting-info {
    text-align: center;
  }
  
  .setting-header {
    justify-content: center;
  }
  
  .card-right {
    align-self: center;
  }
  
  .config-button {
    width: 120px;
  }
}
</style> 