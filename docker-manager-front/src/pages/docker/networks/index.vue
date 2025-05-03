<template>
  <div class="container">
    <t-card title="网络列表" :bordered="false">
      <template #actions>
        <t-button theme="primary" @click="handleCreate">创建网络</t-button>
      </template>
      <t-table
        :data="networks"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @page-change="onPageChange"
      >
        <template #driver="{ row }">
          <t-tag :theme="row.driver === 'bridge' ? 'primary' : 'default'">
            {{ row.driver }}
          </t-tag>
        </template>
        <template #operation="{ row }">
          <t-space>
            <t-button theme="danger" variant="text" @click="handleDelete(row.Id)">删除</t-button>
          </t-space>
        </template>
      </t-table>
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { MessagePlugin } from 'tdesign-vue-next';
import { onMounted, ref } from 'vue';
import { NETWORK_TABLE_COLUMNS } from '@/constants/tableColumns';
import { getNetworkList, createNetwork, deleteNetwork } from '@/api/websocket/container';

const networks = ref([]);
const loading = ref(false);
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
});

const columns = NETWORK_TABLE_COLUMNS;

const fetchNetworks = async () => {
  loading.value = true;
  try {
    const res = await getNetworkList();
    if (res.code === 0) {
      networks.value = res.data;
      pagination.value.total = res.data.length;
    } else {
      MessagePlugin.error(res.message || '获取网络列表失败');
    }
  } catch (error) {
    console.error('获取网络列表失败:', error);
    MessagePlugin.error('获取网络列表失败');
  } finally {
    loading.value = false;
  }
};

const handleCreate = async () => {
  try {
    const res = await createNetwork({
      name: 'my-network',
      driver: 'bridge',
      ipam: {
        driver: 'default',
        config: [
          {
            subnet: '172.20.0.0/16',
            gateway: '172.20.0.1'
          }
        ]
      }
    });
    if (res.code === 0) {
      MessagePlugin.success('创建网络成功');
      fetchNetworks();
    } else {
      MessagePlugin.error(res.message || '创建网络失败');
    }
  } catch (error) {
    console.error('创建网络失败:', error);
    MessagePlugin.error('创建网络失败');
  }
};

const handleDelete = async (row: any) => {
  try {
    await deleteNetwork(row.Id);
    MessagePlugin.success('删除网络成功');
    fetchNetworks();
  } catch (error) {
    console.error('删除网络失败:', error);
    MessagePlugin.error('删除网络失败');
  }
};

const onPageChange = (pageInfo: any) => {
  pagination.value.current = pageInfo.current;
  pagination.value.pageSize = pageInfo.pageSize;
  fetchNetworks();
};

onMounted(() => {
  fetchNetworks();
});
</script>

<style scoped>
.container {
  padding: 20px;
}
</style>
