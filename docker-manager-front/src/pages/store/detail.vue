<template>
  <div class="detail-container">
    <div class="detail-wrapper">
      <!-- 应用基本信息 -->
      <div class="app-header">
        <div class="app-icon-container">
          <img :src="appDetail?.iconUrl" class="app-icon" :alt="appDetail?.name" />
        </div>
        <div class="app-info">
          <h1 class="app-title">{{ appDetail?.name }}</h1>
          <div class="app-meta">
            <span class="app-category">{{ getCategoryName(appDetail?.category) }}</span>
            <span class="app-version">版本: {{ appDetail?.version }}</span>
          </div>
          <p class="app-description">{{ appDetail?.description }}</p>
        </div>
      </div>

      <!-- 服务列表 -->
      <div class="services-section">
        <h2 class="section-title">包含服务</h2>
        <div class="services-list">
          <t-card v-for="service in appDetail?.services" :key="service.id" class="service-card">
            <template #header>
              <div class="service-header">
                <div class="service-title-group">
                  <h3 class="service-title">{{ service.name }}</h3>
                  <t-tag theme="primary" variant="light" class="image-tag">{{ service.template.image }}</t-tag>
                  <t-tag theme="warning" variant="light" class="restart-tag">
                    <t-icon name="refresh" />
                    {{ service.template.restartPolicy }}
                  </t-tag>
                </div>
              </div>
            </template>
            <div class="service-content">
              <!-- Tab切换 -->
              <t-tabs v-model="activeTabs[service.id]" class="service-tabs">
                <t-tab-panel v-if="service.template.ports" value="ports" label="端口映射">
                  <div class="flow-section">
                    <div class="flow-header">
                      <div class="flow-title">
                        <t-icon name="network" />
                        <span>端口映射</span>
                      </div>
                      <div class="flow-labels">
                        <span class="label container">容器</span>
                        <span class="label host">主机</span>
                      </div>
                    </div>
                    <div class="flow-diagram">
                      <div class="flow-item" v-for="(port, key) in service.template.ports" :key="key">
                        <div class="flow-node container">{{ key }}</div>
                        <div class="flow-arrow">
                          <t-icon name="arrow-right" />
                        </div>
                        <div class="flow-node host">{{ port }}</div>
                      </div>
                    </div>
                  </div>
                </t-tab-panel>
                <t-tab-panel v-if="service.template.volumes" value="volumes" label="卷挂载">
                  <div class="flow-section">
                    <div class="flow-header">
                      <div class="flow-title">
                        <t-icon name="folder" />
                        <span>卷挂载</span>
                      </div>
                      <div class="flow-labels">
                        <span class="label host">主机</span>
                        <span class="label container">容器</span>
                      </div>
                    </div>
                    <div class="flow-diagram">
                      <div class="flow-item" v-for="(volume, key) in service.template.volumes" :key="key">
                        <div class="flow-node host">{{ key }}</div>
                        <div class="flow-arrow">
                          <t-icon name="arrow-right" />
                        </div>
                        <div class="flow-node container">{{ volume }}</div>
                      </div>
                    </div>
                  </div>
                </t-tab-panel>
              </t-tabs>
            </div>
          </t-card>
        </div>
      </div>

      <!-- 安装按钮 -->
      <div class="action-bar">
        <t-button theme="primary" size="large" @click="handleInstall">
          安装应用
        </t-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { getAppDetail } from '@/api/appStore'
import type { AppStoreAppDetail } from '@/api/model/appStoreModel'

const route = useRoute()
const router = useRouter()

// 应用详情数据
const appDetail = ref<AppStoreAppDetail>()
const loading = ref(false)

// 展开/收起状态管理
const expandedSections = ref<Record<string, { ports?: boolean; mounts?: boolean }>>({})

// Tab状态管理
const activeTabs = ref<Record<string, string>>({})

// 获取应用详情
const fetchAppDetail = async () => {
  try {
    loading.value = true
    const id = route.params.id as string
    console.log('获取应用详情，ID:', id)
    
    const res = await getAppDetail(id)
    console.log('应用详情数据:', res)
    
    if (res.code === 0) {
      appDetail.value = res.data
    } else {
      MessagePlugin.error(res.message || '获取应用详情失败')
    }
  } catch (error) {
    console.error('获取应用详情失败:', error)
    MessagePlugin.error('获取应用详情失败')
  } finally {
    loading.value = false
  }
}

// 获取分类名称
const getCategoryName = (category?: string) => {
  const categoryMap: Record<string, string> = {
    media: '媒体服务',
    download: '下载工具',
    dev: '开发工具',
    database: '数据库',
  }
  return category ? categoryMap[category] || category : ''
}

// 获取重启策略名称
const getRestartPolicyName = (policy?: string) => {
  const policyMap: Record<string, string> = {
    'no': '不自动重启',
    'always': '总是重启',
    'unless-stopped': '除非手动停止',
    'on-failure': '失败时重启'
  }
  return policy ? policyMap[policy] || policy : '不自动重启'
}

// 安装应用
const handleInstall = () => {
  if (appDetail.value) {
    router.push(`/store/install/${appDetail.value.id}`)
  }
}

// 初始化Tab状态
onMounted(() => {
  fetchAppDetail()
  // 初始化每个服务的默认Tab
  if (appDetail.value?.services) {
    appDetail.value.services.forEach(service => {
      if (service.template.ports) {
        activeTabs.value[service.id] = 'ports'
      } else if (service.template.volumes) {
        activeTabs.value[service.id] = 'volumes'
      }
    })
  }
})
</script>

<style scoped>
.detail-container {
  padding: 24px;
  height: 100%;
  background-color: var(--td-bg-color-page);
}

.detail-wrapper {
  height: 100%;
  background-color: var(--td-bg-color-container);
  border-radius: 12px;
  box-shadow: var(--td-shadow-1);
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  gap: 24px;
  margin-bottom: 32px;
}

.app-icon-container {
  width: 120px;
  height: 120px;
  flex-shrink: 0;
  border-radius: 12px;
  overflow: hidden;
}

.app-icon {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-info {
  flex: 1;
  min-width: 0;
}

.app-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin: 0 0 12px;
}

.app-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.app-category {
  font-size: 14px;
  color: var(--td-text-color-secondary);
  background: var(--td-bg-color-secondarycontainer);
  padding: 2px 8px;
  border-radius: 4px;
}

.app-version {
  font-size: 14px;
  color: var(--td-text-color-secondary);
}

.app-description {
  font-size: 14px;
  color: var(--td-text-color-secondary);
  line-height: 1.6;
  margin: 0;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin: 0 0 16px;
}

.services-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 32px;
}

.service-card {
  background: var(--td-bg-color-container);
  border: 1px solid var(--td-component-stroke);
  transition: all 0.3s ease;
  margin-bottom: 12px;
}

.service-card:hover {
  box-shadow: var(--td-shadow-2);
  transform: translateY(-2px);
}

.service-header {
  padding: 8px 16px;
  border-bottom: 1px solid var(--td-component-stroke);
  margin-bottom: 0;
}

.service-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 0;
}

.service-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin: 0;
}

.image-tag, .restart-tag {
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.restart-tag .t-icon {
  font-size: 12px;
}

.service-content {
  padding: 0 16px 12px;
}

.flow-section {
  margin-bottom: 12px;
  background: var(--td-bg-color-secondarycontainer);
  border-radius: 6px;
  padding: 8px;
}

.flow-section:last-child {
  margin-bottom: 0;
}

.flow-header {
  margin-bottom: 8px;
}

.flow-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--td-text-color-primary);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.flow-title .t-icon {
  font-size: 14px;
  color: var(--td-brand-color);
}

.flow-labels {
  display: flex;
  justify-content: space-between;
  padding: 0 8px;
  margin-bottom: 4px;
}

.label {
  font-size: 11px;
  color: var(--td-text-color-secondary);
  padding: 1px 6px;
  border-radius: 3px;
}

.label.container {
  background: var(--td-brand-color-1);
  color: var(--td-brand-color);
}

.label.host {
  background: var(--td-warning-color-1);
  color: var(--td-warning-color);
}

.flow-diagram {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.flow-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.flow-node {
  flex: 1;
  padding: 6px;
  border-radius: 4px;
  background: var(--td-bg-color-container);
  text-align: center;
  font-size: 12px;
  color: var(--td-text-color-primary);
  word-break: break-all;
}

.flow-node.container {
  border: 1px solid var(--td-brand-color);
}

.flow-node.host {
  border: 1px solid var(--td-warning-color);
}

.flow-arrow {
  color: var(--td-text-color-secondary);
  display: flex;
  align-items: center;
  padding: 0 2px;
}

.flow-arrow .t-icon {
  font-size: 12px;
}

.action-bar {
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px solid var(--td-component-stroke);
  display: flex;
  justify-content: center;
}

.expand-buttons {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.expand-buttons .t-button {
  padding: 4px 8px;
  font-size: 12px;
}

.expand-buttons .t-icon {
  font-size: 14px;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .detail-container {
    padding: 16px;
  }

  .detail-wrapper {
    padding: 16px;
  }

  .app-header {
    flex-direction: column;
    gap: 16px;
  }

  .app-icon-container {
    width: 100px;
    height: 100px;
  }

  .app-title {
    font-size: 20px;
  }

  .app-meta {
    flex-wrap: wrap;
  }

  .service-title-group {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .service-content {
    padding: 8px 12px;
  }

  .flow-section {
    padding: 6px;
  }

  .flow-item {
    flex-direction: column;
    gap: 2px;
  }

  .flow-arrow {
    transform: rotate(90deg);
    padding: 2px 0;
  }

  .flow-node {
    width: 100%;
    font-size: 11px;
    padding: 4px;
  }
}

.service-tabs {
  margin-top: 0;
}

.service-tabs :deep(.t-tabs__nav) {
  margin-bottom: 0;
}

.service-tabs :deep(.t-tabs__nav-item) {
  padding: 4px 12px;
  font-size: 13px;
}

.service-tabs :deep(.t-tabs__nav-item.t-is-active) {
  font-weight: 500;
}
</style> 