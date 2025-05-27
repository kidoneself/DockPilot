<template>
    <n-card
      class="app-card"
      :style="{ background: bgColor, color: textColor }"
      hoverable
      content-style="padding: 0;"
      @click="onClick"
    >
      <div class="app-card-content">
        <div class="icon-wrap">
          <img v-if="iconUrl" :src="iconUrl" class="app-icon" />
          <n-icon
            v-else
            :size="20"
            :component="icon"
            class="app-icon"
          />
        </div>
        <div class="text-area">
          <div class="app-title">{{ name }}</div>
          <div class="app-desc">{{ description }}</div>
        </div>
      </div>
    </n-card>
  </template>
  
  <script setup lang="ts">
  defineProps({
    name: String,
    description: String,
    icon: [Object, Function], // NaiveUI图标组件
    iconUrl: String,          // 图片icon优先
    bgColor: { type: String, default: '#463c6a' },
    textColor: { type: String, default: '#fff' }
  })
  const emit = defineEmits(['click'])

  function onClick() {
    emit('click')
  }
  </script>
  
  <style scoped>
  .app-card {
  border-radius: 10px;
  height: 60px; /* 固定高度 */
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: box-shadow 0.15s, transform 0.15s, border 0.15s;
  box-shadow: 0 1px 4px 0 rgba(32,128,240,0.04);
  padding: 0;
  background: #463c6a;
  border: 1px solid #5a4e7c;
}
.app-card:hover {
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 2px 8px 0 rgba(32,128,240,0.10);
  border: 1.2px solid #7a6ee6;
}

.app-card-content {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 8px 12px;
  min-width: 0; /* 防止子元素撑破 */
}

.icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #3a335a;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0; /* 不允许被压缩 */
}

.app-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
  background: transparent;
}

.text-area {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 36px;
  flex: 1;
  min-width: 0; /* 防止文字换行撑破 */
  overflow: hidden;
}

.app-title {
  font-size: 0.95rem;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-desc {
  font-size: 0.8rem;
  opacity: 0.7;
  font-weight: 400;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
  </style>