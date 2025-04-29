<template>
  <div class="dashboard-container">
    <t-card title="仪表盘" :bordered="false">
      <div class="dashboard-content">
        <!-- 空白页面 -->
      </div>
    </t-card>
  </div>
</template>

<script lang="ts">
export default {
  name: 'DashboardBase',
};
</script>

<script setup lang="ts">
import { ref } from 'vue';

const loading = ref(false);
</script>

<style lang="less" scoped>
.dashboard-container {
  padding: 16px;
  height: 100%;
  
  .dashboard-content {
    height: 100%;
  }
}
</style>
