<template>
  <div class="app-grid new-app-grid" :class="{ 'animate-in': animateIn }">
    <div
      v-for="(item, index) in appList"
      :key="index"
      class="app-item new-app-item"
      @click="handleAppClick(item)"
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
          <t-icon name="app" />
        </div>
      </div>
      <div class="app-info">
        <div class="app-name new-app-name">{{ item.name }}</div>
        <div class="app-desc">{{ item.desc }}</div>
      </div>
    </div>
    <div class="app-item add-item new-app-item" @click="addApp"
         :style="{ 'animation-delay': `${appList.length * 0.05}s` }">
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
import { ref, defineProps, defineEmits } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';

// 定义应用项接口
interface AppItem {
  name: string;
  icon: string;
  internalUrl: string;
  externalUrl: string;
  desc: string;
}

// 定义组件属性
const props = defineProps({
  appList: {
    type: Array as () => AppItem[],
    required: true
  },
  isInternalNetwork: {
    type: Boolean,
    default: true
  },
  animateIn: {
    type: Boolean,
    default: true
  }
});

// 定义组件事件
const emit = defineEmits(['openApp', 'addApp', 'contextMenu']);

// 使用 Map 来管理图片错误状态
const imageErrorMap = new Map<string, boolean>();

// 处理应用点击
function handleAppClick(item: AppItem) {
  emit('openApp', item);
}

// 处理右键菜单
function handleContextMenu(event: MouseEvent, item: AppItem) {
  emit('contextMenu', { event, item });
}

// 添加应用
function addApp() {
  emit('addApp');
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
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 0.8rem 1rem;
  min-width: 200px;
  min-height: 60px;
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
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  margin-right: 0.8rem;
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
}

.new-app-name {
  color: #fff;
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 0.1rem;
}

.app-desc {
  color: #cbd5e1;
  font-size: 0.85rem;
  font-weight: 400;
  opacity: 0.85;
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