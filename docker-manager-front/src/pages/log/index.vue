<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { getLogs, cleanupLogs } from '@/api/log';
import type { Log } from '@/types/api/log';
import { LOG_TABLE_COLUMNS } from '@/constants/tableColumns';

const logs = ref<Log[]>([]);
const loading = ref(false);
const typeFilter = ref('');
const levelFilter = ref('');

// 获取日志列表
const fetchLogs = async () => {
  loading.value = true;
  try {
    const res = await getLogs({
      type: typeFilter.value || undefined,
      level: levelFilter.value || undefined,
    });
    logs.value = res.data;
  } catch (error) {
    MessagePlugin.error('获取日志列表失败');
  } finally {
    loading.value = false;
  }
};

// 清理旧日志
const handleCleanup = async (days: number) => {
  try {
    await cleanupLogs({ days });
    MessagePlugin.success('清理成功');
    fetchLogs();
  } catch (error) {
    MessagePlugin.error('清理失败');
  }
};

// 监听筛选条件变化
watch([typeFilter, levelFilter], () => {
  fetchLogs();
});

onMounted(() => {
  fetchLogs();
});
</script>

<template>
  <div class="log-manager">
    <t-card title="日志管理" class="log-card">
      <template #actions>
        <t-space>
          <t-select v-model="typeFilter" placeholder="日志类型" clearable @change="fetchLogs">
            <t-option value="OPERATION" label="操作日志" />
            <t-option value="SYSTEM" label="系统日志" />
          </t-select>
          <t-select v-model="levelFilter" placeholder="日志级别" clearable @change="fetchLogs">
            <t-option value="INFO" label="INFO" />
            <t-option value="ERROR" label="ERROR" />
            <t-option value="WARN" label="WARN" />
          </t-select>
          <t-button theme="primary" @click="fetchLogs">查询</t-button>
          <t-button theme="danger" @click="handleCleanup(30)">清理30天前日志</t-button>
        </t-space>
      </template>

      <t-table
        :data="logs"
        :columns="LOG_TABLE_COLUMNS"
        :loading="loading"
        row-key="id"
        hover
        stripe
      />
    </t-card>
  </div>
</template>

<style scoped>
.log-manager {
  padding: 20px;
}

.log-card {
  margin-bottom: 20px;
}
</style> 