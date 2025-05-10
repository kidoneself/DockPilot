<template>
  <l-header
    v-if="settingStore.showHeader"
    :show-logo="settingStore.showHeaderLogo"
    :theme="settingStore.displayMode"
    :layout="settingStore.layout"
    :is-fixed="settingStore.isHeaderFixed"
    :menu="headerMenu"
    :is-compact="settingStore.isSidebarCompact"
  />
</template>

<script setup lang="ts">
import { usePermissionStore, useSettingStore } from '@/store';
import { storeToRefs } from 'pinia';
import { computed } from 'vue';
import { useRoute } from 'vue-router';

import LHeader from './Header.vue';

const settingStore = useSettingStore();
const permissionStore = usePermissionStore();
const { routers: menuRouters } = storeToRefs(permissionStore);

const headerMenu = computed(() => {
  if (settingStore.layout === 'mix') {
    if (settingStore.splitMenu) {
      return menuRouters.value.map((menu) => ({
        ...menu,
        children: [],
      }));
    }
    return [];
  }
  return menuRouters.value;
});
</script>
