<template>
  <div class="app-grid new-app-grid" :class="{ 'animate-in': animateIn }">
    <div
      v-for="(item, index) in homeStore.webServerList"
      :key="index"
      class="app-item new-app-item"
      @click="homeStore.handleAppClick(item)"
      @contextmenu.prevent="handleContextMenu($event, item)"
      :style="{ 'animation-delay': `${index * 0.05}s` }"
    >
      <div class="new-app-icon">
        <img 
          :src="item.icon" 
          :alt="item.name" 
          @error="handleImageError($event, item)"
          :class="{ 'image-error': imageErrorMap.get(item.icon) }"
        />
        <div v-if="imageErrorMap.get(item.icon)" class="fallback-icon">
          <t-icon name="browser" />
        </div>
      </div>
      <div class="app-info">
        <div class="app-name new-app-name">{{ item.name }}</div>
        <div class="app-desc">{{ item.description }}</div>
      </div>
    </div>
    <div class="app-item add-item new-app-item" @click="homeStore.handleAddApp()"
         :style="{ 'animation-delay': `${homeStore.webServerList.length * 0.05}s` }">
      <div class="new-app-icon">
        <t-icon name="add" />
      </div>
      <div class="app-info">
        <div class="app-name new-app-name">添加应用</div>
        <div class="app-desc">自定义你的应用</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineProps } from 'vue';
import { useHomeStore } from '@/store/modules/home';
import type { AppItem } from '@/store/modules/home';

// 定义组件属性
const props = defineProps({
  animateIn: {
    type: Boolean,
    default: true
  }
});

// 初始化 store
const homeStore = useHomeStore();

// 使用 Map 来管理图片错误状态
const imageErrorMap = new Map<string, boolean>();

// 处理右键菜单
function handleContextMenu(event: MouseEvent, item: AppItem) {
  homeStore.handleContextMenu({ event, item });
}

// 处理图片加载失败
function handleImageError(event: Event, item: AppItem) {
  const img = event.target as HTMLImageElement;
  imageErrorMap.set(item.icon, true);
  img.style.display = 'none';
}
</script>

<style scoped>
.new-app-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 1.5rem;
  margin-top: 1.5rem;
  width: 100%;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.5s ease;
}

.new-app-grid.animate-in {
  opacity: 1;
  transform: translateY(0);
}

.new-app-item {
  display: flex;
  align-items: center;
  background: rgba(20, 20, 40, 0.55);
  border-radius: 10px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 0.6rem 0.6rem;
  min-width: 180px;
  min-height: 50px;
  transition:
    background 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.new-app-item:hover {
  background: rgba(40, 80, 180, 0.18);
  box-shadow: 0 4px 24px 0 rgba(59, 130, 246, 0.1);
  transform: translateY(-4px) scale(1.03);
}

.new-app-icon {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  margin-right: 0.6rem;
  position: relative;
}

.new-app-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.2s;
}

.new-app-icon img.image-error {
  opacity: 0;
}

.fallback-icon {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.5);
}

.fallback-icon :deep(.t-icon) {
  font-size: 1.5rem;
}

.app-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1;
}

.new-app-name {
  color: #fff;
  font-size: 0.9rem;
  font-weight: 700;
  margin-bottom: 0.05rem;
}

.app-desc {
  color: #cbd5e1;
  font-size: 0.7rem;
  font-weight: 400;
  opacity: 0.85;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  line-height: 1.1;
}

/* 应用列表动画 */
.new-app-grid.animate-in .new-app-item {
  animation: fadeInScale 0.5s ease both;
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 响应式布局 */
@media screen and (max-width: 1200px) {
  .new-app-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media screen and (max-width: 992px) {
  .new-app-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media screen and (max-width: 768px) {
  .new-app-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media screen and (max-width: 480px) {
  .new-app-grid {
    grid-template-columns: 1fr;
  }
  
  .new-app-item {
    min-width: unset;
  }
}
</style> 