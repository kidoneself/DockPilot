<template>
  <div class="image-list-item">
    <div class="left">
      <span class="status-dot" :class="{ online: !image.needUpdate, update: image.needUpdate }"></span>
    </div>
    <div class="center">
      <div class="main-row">
        <span class="name">{{ image.name }}</span>
        <span class="tag">标签：{{ image.tag }}</span>
        <span v-if="image.needUpdate" class="update-badge">有新版本</span>
      </div>
      <div class="sub-row">
        <span class="created">创建时间：{{ formatDate(image.created) }}</span>
        <span class="size">大小：{{ formatSize(image.size) }}</span>
        <span class="id">ID: {{ image.id.slice(0, 8) }}</span>
      </div>
    </div>
    <div class="actions">
      <button v-if="image.needUpdate" class="icon-btn" @click="$emit('update', image)" title="更新">
        <svg viewBox="0 0 16 16" width="18" height="18"><path d="M8 3v2.5a.5.5 0 0 0 1 0V2.5A.5.5 0 0 0 8.5 2h-5a.5.5 0 0 0 0 1H6.6A6 6 0 1 0 14 8a.5.5 0 0 0-1 0A5 5 0 1 1 8 3z" fill="#1890ff"/></svg>
      </button>
      <ConfirmPopover v-model:visible="showConfirm" @confirm="emitDelete">
        <template #default>
          <button class="icon-btn" @click="showConfirm = true" title="删除">
            <svg viewBox="0 0 16 16" width="18" height="18"><path d="M6.5 6.5l3 3m0-3l-3 3" stroke="#ff4d4f" stroke-width="1.5" stroke-linecap="round"/><circle cx="8" cy="8" r="7" stroke="#ff4d4f" stroke-width="1.2" fill="none"/></svg>
          </button>
        </template>
        <template #content>
          确定要删除镜像 {{ image.name }}:{{ image.tag }} 吗？
        </template>
      </ConfirmPopover>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, ref } from 'vue';
import { formatDate } from '@/utils/format';
import ConfirmPopover from '@/components/ConfirmPopover.vue';

const props = defineProps<{ image: any }>();
const showConfirm = ref(false);
const emitDelete = () => {
  showConfirm.value = false;
  emit('delete', props.image);
};
const emit = defineEmits(['delete', 'update']);

function formatSize(size: number): string {
  if (!size) return '未知';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let index = 0;
  let formattedSize = size;
  while (formattedSize >= 1024 && index < units.length - 1) {
    formattedSize /= 1024;
    index++;
  }
  return `${formattedSize.toFixed(2)} ${units[index]}`;
}
</script>

<style scoped>
.image-list-item {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  margin-bottom: 18px;
  padding: 18px 28px;
  transition: box-shadow 0.2s;
}
.image-list-item:hover {
  box-shadow: 0 4px 16px rgba(24,144,255,0.10);
}
.left {
  margin-right: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
}
.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #d9d9d9;
  display: inline-block;
}
.status-dot.online {
  background: #52c41a;
}
.status-dot.update {
  background: #1890ff;
}
.center {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.main-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
.name {
  font-size: 17px;
  font-weight: 600;
  color: #222;
}
.tag {
  font-size: 13px;
  color: #888;
  background: #f5f5f5;
  border-radius: 4px;
  padding: 2px 8px;
}
.update-badge {
  font-size: 12px;
  color: #1890ff;
  background: #e6f7ff;
  border-radius: 4px;
  padding: 2px 8px;
  margin-left: 4px;
  font-weight: 500;
}
.sub-row {
  display: flex;
  align-items: center;
  gap: 18px;
  color: #999;
  font-size: 13px;
}
.created, .size, .id {
  color: #999;
}
.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
  padding: 0;
}
.icon-btn:hover {
  background: #f0f5ff;
}
</style> 