<template>
  <div class="app-center">
    <div class="app-center-header">
      <h2>应用中心</h2>
      <SearchBar v-model="searchText" placeholder="搜索应用" style="width: 240px;" />
    </div>
    <n-grid
      :cols="24"
      :x-gap="24"
      :y-gap="24"
      responsive="screen"
    >
      <n-grid-item
        v-for="app in filteredApps"
        :key="app.id"
        :span="24"
        :xs="24"
        :s="12"
        :m="8"
        :l="6"
        :xl="4"
      >
        <n-card class="app-card" hoverable>
          <div class="app-card-content">
            <img :src="app.icon" class="app-icon" />
            <div class="app-info">
              <div class="app-title">{{ app.name }}</div>
              <div class="app-desc">{{ app.desc }}</div>
            </div>
          </div>
          <template #footer>
            <n-button type="primary" size="small" @click="handleOpen(app)">打开</n-button>
          </template>
        </n-card>
      </n-grid-item>
    </n-grid>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useMessage } from 'naive-ui'
import SearchBar from '@/components/common/SearchBar.vue'

const message = useMessage()
const searchText = ref('')
const apps = ref([
  { 
    id: 1, 
    name: 'Alist', 
    desc: '网盘挂载与文件管理', 
    icon: 'https://img.icons8.com/color/48/000000/cloud.png' 
  },
  { 
    id: 2, 
    name: 'Jellyfin', 
    desc: '媒体服务器', 
    icon: 'https://img.icons8.com/color/48/000000/video.png' 
  },
  { 
    id: 3, 
    name: 'qBittorrent', 
    desc: 'BT下载工具', 
    icon: 'https://img.icons8.com/color/48/000000/bittorrent.png' 
  },
  { 
    id: 4, 
    name: 'Emby', 
    desc: '媒体服务器', 
    icon: 'https://img.icons8.com/color/48/000000/emby.png' 
  },
  { 
    id: 5, 
    name: 'Nextcloud', 
    desc: '私有云盘', 
    icon: 'https://img.icons8.com/color/48/000000/nextcloud.png' 
  },
  { 
    id: 6, 
    name: 'Portainer', 
    desc: 'Docker可视化管理', 
    icon: 'https://img.icons8.com/color/48/000000/docker.png' 
  }
])

const filteredApps = computed(() =>
  !searchText.value
    ? apps.value
    : apps.value.filter(app =>
        app.name.toLowerCase().includes(searchText.value.toLowerCase()) ||
        app.desc.toLowerCase().includes(searchText.value.toLowerCase())
      )
)

function handleOpen(app: any) {
  message.info(`打开应用：${app.name}`)
}
</script>

<style scoped>
.app-center {
  padding: 24px;
}
.app-center-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.app-card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}
.app-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  object-fit: cover;
  background: #f5f5f5;
}
.app-info {
  flex: 1;
}
.app-title {
  font-weight: bold;
  font-size: 1.1rem;
}
.app-desc {
  color: #888;
  font-size: 0.95rem;
  margin-top: 4px;
}
</style> 