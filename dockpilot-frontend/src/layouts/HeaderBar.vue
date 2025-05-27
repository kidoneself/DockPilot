<template>
  <div class="header-container">
    <n-breadcrumb>
      <n-breadcrumb-item>Docker</n-breadcrumb-item>
      <n-breadcrumb-item>{{ currentRoute }}</n-breadcrumb-item>
    </n-breadcrumb>
    <div class="header-actions">
      <n-button-group>
        <n-button @click="handleRefresh">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新
        </n-button>
        <n-button @click="handleAdd">
          <template #icon>
            <n-icon><AddOutline /></n-icon>
          </template>
          新建
        </n-button>
      </n-button-group>
      <n-button @click="toggleTheme">
        <template #icon>
          <n-icon>
            <SunnyOutline v-if="isDark" />
            <MoonOutline v-else />
          </n-icon>
        </template>
      </n-button>
      <!-- 更新通知组件 -->
      <UpdateNotification />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/store/theme'
import UpdateNotification from '@/components/UpdateNotification.vue'
import {
  RefreshOutline,
  AddOutline,
  SunnyOutline,
  MoonOutline
} from '@vicons/ionicons5'

const route = useRoute()
const themeStore = useThemeStore()

const currentRoute = computed(() => {
  const path = route.path.split('/')[1]
  return path.charAt(0).toUpperCase() + path.slice(1)
})

const isDark = computed(() => themeStore.theme === 'dark')

function handleRefresh() {
  // TODO: 实现刷新逻辑
}

function handleAdd() {
  // TODO: 实现新建逻辑
}

function toggleTheme() {
  themeStore.toggleTheme()
}
</script>

<style scoped>
.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}
</style> 