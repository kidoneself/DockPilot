<template>
  <div class="network-list">
    <n-card>
      <template #header>
        <NSpace justify="space-between">
          <NSpace>
            <NButton @click="handleRefresh">
              <template #icon>
                <NIcon><RefreshOutline /></NIcon>
              </template>
              刷新
            </NButton>
            <NButton @click="showDrawer = true">
              <template #icon>
                <NIcon><AddOutline /></NIcon>
              </template>
              创建网络
            </NButton>
          </NSpace>
          <SearchBar v-model="searchText" placeholder="搜索网络" />
        </NSpace>
      </template>

      <NSpace vertical size="large">
        <NetworkItem
          v-for="network in filteredNetworks"
          :key="network.id"
          :network="network"
          @action="handleNetworkAction"
        />
      </NSpace>

      <template #footer>
        <n-empty v-if="filteredNetworks.length === 0" description="暂无网络" />
      </template>
    </n-card>

    <n-drawer v-model:show="showDrawer" :width="500" placement="right">
      <n-drawer-content title="创建网络">
        <NSpace vertical align="center" style="padding: 48px 0">
          <NIcon size="48" :depth="3">
            <NSpin>
              <NIcon>
                <svg 
                  xmlns="http://www.w3.org/2000/svg" 
                  xmlns:xlink="http://www.w3.org/1999/xlink" 
                  viewBox="0 0 512 512"
                >
                  <circle 
                    cx="256" 
                    cy="256" 
                    r="200" 
                    fill="currentColor"
                  />
                </svg>
              </NIcon>
            </NSpin>
          </NIcon>
          <NText>网络创建功能正在开发中，敬请期待...</NText>
        </NSpace>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="showDrawer = false">关闭</NButton>
          </NSpace>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { NButton, NSpace, useMessage } from 'naive-ui'
import { RefreshOutline, AddOutline } from '@vicons/ionicons5'
import NetworkItem from '@/components/network/NetworkItem.vue'
import SearchBar from '@/components/common/SearchBar.vue'
import { getNetworkList } from '@/api/network'
import type { Network } from '@/api/model/network'

const message = useMessage()

// 网络列表数据
const networks = ref<Network[]>([])
const loading = ref(false)
const showDrawer = ref(false)
const searchText = ref('')

// 获取网络列表
const fetchNetworkList = async () => {
  loading.value = true
  try {
    await getNetworkList({
      onComplete: (msg) => {
        networks.value = msg.data || []
      },
      onError: (err: string) => {
        message.error(`获取网络列表失败: ${err}`)
      }
    })
  } catch {
    message.error('获取网络列表失败')
  } finally {
    loading.value = false
  }
}

// 页面加载时获取网络列表
onMounted(() => {
  fetchNetworkList()
})

const filteredNetworks = computed(() => {
  if (!searchText.value) return networks.value
  return networks.value.filter(item =>
    (item.name && item.name.includes(searchText.value)) ||
    (item.driver && item.driver.includes(searchText.value))
  )
})

function handleNetworkAction(action: string, network: Network) {
  // 目前只显示信息，因为删除功能未实现
  message.info(`${action}: ${network.name}`)
}

function handleRefresh() {
  fetchNetworkList()
}
</script>

<style scoped>
.network-list {
  height: 100%;
}

:deep(.n-card) {
  background-color: var(--n-card-color);
}

@media (max-width: 768px) {
  .network-list {
    padding: 0;
  }
}
</style> 