<template>
  <div class="store-container">
    <div class="store-wrapper">
      <!-- 应用列表 -->
      <div class="app-list">
        <t-row :gutter="[12, 12]">
          <t-col v-for="app in apps" :key="app.id" :xs="12" :sm="8" :md="5" :lg="4" :xl="4">
            <div class="app-card" @click="handleCardClick(app)">
              <div class="app-icon-container">
                <img :src="app.iconUrl" class="app-icon" :alt="app.name" />
              </div>
              <div class="app-content">
                <div class="app-header">
                  <h3 class="app-title">{{ app.name }}</h3>
                  <span class="app-category">{{ getCategoryName(app.category) }}</span>
                </div>
                <p class="app-description">{{ app.description }}</p>
                <div class="app-footer">
                  <t-button theme="primary" size="small" @click.stop="handleInstall(app)">
                    安装
                  </t-button>
                </div>
              </div>
            </div>
          </t-col>
        </t-row>
      </div>

      <!-- 分页 -->
      <div class="pagination">
        <t-pagination
          v-model="current"
          :total="total"
          :page-size="pageSize"
          @change="handlePageChange"
          :loading="loading"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { getAppList } from '@/api/appStore'
import type { AppStoreApp } from '@/api/model/appStoreModel'

const router = useRouter()

// 分页
const current = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)

// 应用列表数据
const apps = ref<AppStoreApp[]>([])

// 获取应用列表
const fetchAppList = async () => {
  try {
    loading.value = true
    console.log('请求参数:', {
      page: current.value,
      size: pageSize.value
    })
    
    const res = await getAppList({
      page: current.value,
      size: pageSize.value
    })
    
    console.log('接口返回数据:', res)
    
    if (res.code === 0) {
      console.log('应用列表数据:', res.data.records)
      console.log('总数:', res.data.total)
      apps.value = res.data.records
      total.value = res.data.total
    } else {
      console.error('接口返回错误:', res.message)
      MessagePlugin.error(res.message || '获取应用列表失败')
    }
  } catch (error) {
    console.error('请求异常:', error)
    MessagePlugin.error('获取应用列表失败')
  } finally {
    loading.value = false
  }
}

// 事件处理
const handlePageChange = (pageInfo: { current: number }) => {
  console.log('页码变化:', pageInfo)
  current.value = pageInfo.current
  fetchAppList()
}

const handleCardClick = (app: AppStoreApp) => {
  router.push(`/store/install/${app.id}`);
}

// 获取分类名称
const getCategoryName = (category: string) => {
  const categoryMap: Record<string, string> = {
    media: '媒体服务',
    download: '下载工具',
    dev: '开发工具',
    database: '数据库',
  }
  return categoryMap[category] || category
}

// 安装应用
const handleInstall = (app: AppStoreApp) => {
  router.push(`/store/install/${app.id}`);
}

// 初始化
onMounted(() => {
  console.log('组件挂载，开始获取数据')
  fetchAppList()
})
</script>

<style scoped>
.store-container {
  padding: 24px;
  height: 100%;
  background-color: var(--td-bg-color-page);
}

.store-wrapper {
  height: 100%;
  background-color: var(--td-bg-color-container);
  border-radius: 12px;
  box-shadow: var(--td-shadow-1);
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.app-list {
  flex: 1;
  margin-bottom: 24px;
  min-height: 0;
  overflow-y: auto;
}

.app-card {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  overflow: hidden;
  background: var(--td-bg-color-container);
  border: 1px solid var(--td-component-stroke);
  display: flex;
  flex-direction: row;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--td-shadow-3);
  border-color: var(--td-brand-color);
}

.app-icon-container {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  overflow: hidden;
}

.app-icon {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.app-card:hover .app-icon {
  transform: scale(1.05);
}

.app-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 8px;
  min-width: 0;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.app-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-category {
  font-size: 11px;
  color: var(--td-text-color-secondary);
  background: var(--td-bg-color-secondarycontainer);
  padding: 1px 4px;
  border-radius: 3px;
  flex-shrink: 0;
  margin-left: 8px;
}

.app-description {
  flex: 1;
  font-size: 12px;
  color: var(--td-text-color-secondary);
  margin: 4px 0 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.app-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
}

.app-footer :deep(.t-button) {
  width: 60px;
  height: 24px;
  font-size: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  padding-top: 24px;
  border-top: 1px solid var(--td-component-stroke);
  margin-top: auto;
  flex-shrink: 0;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .store-container {
    padding: 16px;
  }

  .store-wrapper {
    padding: 16px;
  }

  .app-icon-container {
    width: 80px;
    height: 80px;
  }

  .app-content {
    padding: 6px;
  }

  .app-title {
    font-size: 13px;
  }

  .app-description {
    font-size: 11px;
    margin: 3px 0 6px;
  }

  .app-footer :deep(.t-button) {
    width: 50px;
    height: 22px;
    font-size: 11px;
  }
}
</style> 