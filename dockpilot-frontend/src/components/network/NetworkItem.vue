<template>
  <n-card
    class="network-card"
    hoverable
    bordered
    :style="{ borderLeft: `6px solid ${statusBarColor}` }"
  >
    <div class="network-row" @click="toggleExpand">
      <!-- 左侧：图标、名称 -->
      <div class="network-title-row">
        <div class="network-icon">
          <NIcon :size="40">
            <GitNetworkOutline />
          </NIcon>
        </div>
        <span class="network-name">{{ network.name }}</span>
      </div>
      <!-- 中间：关键信息 -->
      <div class="network-info-row">
        <span class="network-driver">驱动：{{ network.driver }}</span>
        <span class="network-label">子网：</span>
        <span class="network-value">{{ subnet }}</span>
        <span class="network-label">网关：</span>
        <span class="network-value">{{ gateway }}</span>
      </div>
      <!-- 右侧：操作按钮组 -->
      <div class="network-actions" @click.stop>
        <NButtonGroup>
          <NTooltip trigger="hover">
            <template #trigger>
              <NButton
quaternary
circle
size="small"
@click="toggleExpand">
                <template #icon>
                  <NIcon><InformationCircleOutline /></NIcon>
                </template>
              </NButton>
            </template>
            详情
          </NTooltip>
        </NButtonGroup>
      </div>
    </div>
    <transition
      @before-enter="beforeEnter"
      @enter="enter"
      @after-enter="afterEnter"
      @before-leave="beforeLeave"
      @leave="leave"
      @after-leave="afterLeave"
    >
      <div v-if="expanded" ref="detailRef" class="network-detail">
        <div class="detail-inner">
          <div v-if="loading" class="loading-state">
            <n-spin size="small" />
            <span>加载中...</span>
          </div>
          <template v-else>
            <div class="detail-section">
              <div class="detail-row"><span class="detail-label">子网：</span>{{ subnet }}</div>
              <div class="detail-row"><span class="detail-label">网关：</span>{{ gateway }}</div>
              <div class="detail-row"><span class="detail-label">IPv6：</span>{{ ipv6 }}</div>
            </div>
            
            <div 
              v-if="networkDetail?.containers && 
                    Object.keys(networkDetail.containers).length > 0" 
              class="detail-section"
            >
              <div class="section-title">已连接容器</div>
              <div 
                v-for="(container, id) in networkDetail.containers" 
                :key="id" 
                class="container-item"
              >
                <div class="container-name">{{ container.name }}</div>
                <div class="container-info">
                  <span class="info-item">MAC: {{ container.macAddress || '-' }}</span>
                  <span class="info-item">IPv4: {{ container.ipv4Address || '-' }}</span>
                  <span 
                    v-if="container.ipv6Address" 
                    class="info-item"
                  >
                    IPv6: {{ container.ipv6Address }}
                  </span>
                </div>
              </div>
            </div>

            <div 
              v-if="networkDetail?.labels && 
                    Object.keys(networkDetail.labels).length > 0" 
              class="detail-section"
            >
              <div class="section-title">标签</div>
              <div 
                v-for="(value, key) in networkDetail.labels" 
                :key="key" 
                class="detail-row"
              >
                <span class="detail-label">{{ key }}：</span>{{ value }}
              </div>
            </div>
          </template>
        </div>
      </div>
    </transition>
  </n-card>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import {
  GitNetworkOutline,
  InformationCircleOutline
} from '@vicons/ionicons5'
import { NIcon, NButton, NButtonGroup, NTooltip, useMessage } from 'naive-ui'
import type { Network } from '@/api/model/network'
import { getNetworkDetail } from '@/api/network'

const message = useMessage()
const props = defineProps<{
  network: Network
}>()

const expanded = ref(false)
const detailRef = ref<HTMLElement | null>(null)
const networkDetail = ref<Network | null>(null)
const loading = ref(false)

async function toggleExpand() {
  expanded.value = !expanded.value
  if (expanded.value && !networkDetail.value) {
    loading.value = true
    try {
      await getNetworkDetail(props.network.id, {
        onComplete: (msg) => {
          networkDetail.value = msg.data
        },
        onError: (err: string) => {
          message.error(`获取网络详情失败: ${err}`)
        }
      })
    } catch {
      message.error('获取网络详情失败')
    } finally {
      loading.value = false
    }
  }
}

// 彩色边根据Driver类型变化
const statusBarColor = computed(() => {
  switch (props.network.driver) {
    case 'bridge':
      return '#18a058'
    case 'overlay':
      return '#2080f0'
    case 'host':
      return '#f0a020'
    default:
      return '#d3d3d3'
  }
})

const subnet = computed(() => {
  const config = props.network.ipamConfig?.[0]
  return config?.subnet || '未配置'
})
const gateway = computed(() => {
  const config = props.network.ipamConfig?.[0]
  return config?.gateway || '未配置'
})
const ipv6 = computed(() => {
  return props.network.enableIPv6 ? '已启用' : '已禁用'
})

function beforeEnter(el: Element) {
  const h = el as HTMLElement
  h.style.maxHeight = '0'
  h.style.opacity = '0'
}
function enter(el: Element) {
  const h = el as HTMLElement
  nextTick(() => {
    h.style.transition = 'max-height 0.3s linear, opacity 0.3s'
    h.style.maxHeight = h.scrollHeight + 'px'
    h.style.opacity = '1'
  })
}
function afterEnter(el: Element) {
  const h = el as HTMLElement
  h.style.maxHeight = ''
  h.style.transition = ''
}
function beforeLeave(el: Element) {
  const h = el as HTMLElement
  h.style.maxHeight = h.scrollHeight + 'px'
  h.style.opacity = '1'
}
function leave(el: Element, done: () => void) {
  const h = el as HTMLElement
  h.style.transition = 'max-height 0.3s linear, opacity 0.3s'
  h.style.maxHeight = '0'
  h.style.opacity = '0'
  const handler = (e: TransitionEvent) => {
    if (e.propertyName === 'max-height') {
      h.removeEventListener('transitionend', handler)
      done()
    }
  }
  h.addEventListener('transitionend', handler)
}
function afterLeave(el: Element) {
  const h = el as HTMLElement
  h.style.maxHeight = ''
  h.style.transition = ''
}
</script>

<style scoped>
.network-card {
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.03);
  border-radius: 12px;
  background: var(--n-card-color);
  padding: 0;
  margin-bottom: 0;
}
.network-row {
  display: flex;
  align-items: center;
  min-height: 64px;
  padding: 0 24px;
  gap: 24px;
  cursor: pointer;
}
.network-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 180px;
}
.network-icon {
  margin-right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.network-name {
  font-size: 18px;
  font-weight: 500;
  color: var(--n-text-color);
}
.network-info-row {
  display: flex;
  align-items: center;
  gap: 24px;
  font-size: 15px;
  flex: 1;
}
.network-driver {
  color: var(--n-text-color-3);
  font-size: 15px;
  margin-right: 8px;
}
.network-label {
  color: var(--n-text-color-3);
  font-size: 15px;
  margin-left: 8px;
}
.network-value {
  color: var(--n-text-color-2);
  font-size: 15px;
  margin-left: 4px;
}
.network-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}
.n-button {
  min-width: 28px;
  min-height: 28px;
}
.network-detail {
  overflow: hidden;
  background: var(--n-color-table-header);
  border-radius: 0 0 12px 12px;
  font-size: 15px;
  color: var(--n-text-color-2);
}
.detail-inner {
  padding: 16px 32px 16px 48px;
}
.detail-row {
  margin-bottom: 8px;
}
.detail-label {
  color: var(--n-text-color-3);
  margin-right: 8px;
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
/* 新增平滑高度动画 */
.expand-enter-active, .expand-leave-active {
  transition: max-height 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.3s;
  overflow: hidden;
}
.expand-enter-from, .expand-leave-to {
  max-height: 0;
  opacity: 0;
}
.expand-enter-to, .expand-leave-from {
  max-height: 300px;
  opacity: 1;
}
@media (max-width: 1024px) {
  .network-row {
    flex-wrap: wrap;
    gap: 12px;
    padding: 12px;
  }
  .network-title-row {
    min-width: 120px;
  }
  .network-info-row {
    flex-wrap: wrap;
    gap: 12px;
    font-size: 14px;
  }
  .network-driver, .network-label, .network-value {
    font-size: 14px;
  }
}
@media (max-width: 768px) {
  .network-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 8px;
  }
  .network-title-row {
    min-width: 0;
    width: 100%;
    justify-content: flex-start;
  }
  .network-info-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    width: 100%;
    font-size: 13px;
  }
  .network-driver, .network-label, .network-value {
    font-size: 13px;
  }
  .network-actions {
    margin-left: 0;
    align-self: flex-end;
    margin-top: 6px;
  }
  .n-button {
    min-width: 24px;
    min-height: 24px;
  }
}
.detail-section {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--n-border-color);
}

.detail-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--n-text-color-2);
  margin-bottom: 12px;
}

.container-item {
  background: var(--n-color-modal);
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 8px;
}

.container-item:last-child {
  margin-bottom: 0;
}

.container-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.container-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
}

.info-item {
  color: var(--n-text-color-3);
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: var(--n-text-color-3);
}
</style> 