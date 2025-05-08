<template>
  <div class="main-search-box">
    <div class="search-engine-group">
      <div class="search-engine-selector" @click="toggleEngineDropdown">
        <img :src="selectedEngine.icon" :alt="selectedEngine.name" class="engine-icon" />
        <div class="dropdown-arrow" :class="{ 'is-active': engineDropdownVisible }">
          <svg viewBox="0 0 1024 1024" width="12" height="12">
            <path d="M512 309.333333c-8.533333 0-17.066667 2.133333-23.466667 8.533334l-341.333333 341.333333c-12.8 12.8-12.8 32 0 44.8 12.8 12.8 32 12.8 44.8 0l320-317.866667 317.866667 320c12.8 12.8 32 12.8 44.8 0 12.8-12.8 12.8-32 0-44.8L533.333333 320c-4.266667-8.533333-12.8-10.666667-21.333333-10.666667z" fill="currentColor"/>
          </svg>
        </div>
        <div class="engine-dropdown" v-show="engineDropdownVisible">
          <div 
            v-for="engine in searchEngines" 
            :key="engine.value"
            class="engine-option"
            @click.stop="handleEngineChange(engine.value)"
          >
            <img :src="engine.icon" :alt="engine.name" class="engine-icon" />
            <span>{{ engine.name }}</span>
          </div>
        </div>
      </div>
      <div class="search-input-wrapper">
        <input
          v-model="searchText"
          type="text"
          class="search-input"
          placeholder="请输入搜索内容"
          @keyup.enter="handleSearch"
        />
        <div class="search-icon" @click="handleSearch">
          <svg viewBox="0 0 1024 1024" width="20" height="20" fill="currentColor">
            <path d="M909.6 854.5L649.9 594.8C690.2 542.7 712 479 712 412c0-80.2-31.3-155.4-87.9-212.1-56.6-56.7-132-87.9-212.1-87.9s-155.5 31.3-212.1 87.9C143.2 256.5 112 331.8 112 412c0 80.1 31.3 155.5 87.9 212.1C256.5 680.8 331.8 712 412 712c67 0 130.6-21.8 182.7-62l259.7 259.6c3.2 3.2 8.4 3.2 11.6 0l43.6-43.5c3.2-3.2 3.2-8.4 0-11.6zM570.4 570.4C528 612.7 471.8 636 412 636s-116-23.3-158.4-65.6C211.3 528 188 471.8 188 412s23.3-116.1 65.6-158.4C296 211.3 352.2 188 412 188s116.1 23.2 158.4 65.6S636 352.2 636 412s-23.3 116.1-65.6 158.4z"/>
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

// 搜索文本
const searchText = ref('');

// 搜索引擎下拉状态
const engineDropdownVisible = ref(false);

// 搜索引擎配置
const searchEngines = [
  {
    name: 'Google',
    value: 'google',
    icon: '/icon/google.svg',
    url: 'https://www.google.com/search?q=',
  },
  {
    name: '百度',
    value: 'baidu',
    icon: '/icon/baidu.svg',
    url: 'https://www.baidu.com/s?wd=',
  },
];

const STORAGE_KEY = 'dockpanel_search_engine';

// 从 localStorage 获取保存的搜索引擎，如果没有则默认使用 Google
const savedEngine = localStorage.getItem(STORAGE_KEY);
const defaultEngine = searchEngines.find(e => e.value === savedEngine) || searchEngines[0];
const selectedEngine = ref(defaultEngine);

// 切换搜索引擎下拉框显示状态
function toggleEngineDropdown(e: MouseEvent) {
  e.stopPropagation();
  engineDropdownVisible.value = !engineDropdownVisible.value;
}

// 选择搜索引擎
function handleEngineChange(val: string) {
  const found = searchEngines.find((e) => e.value === val);
  if (found) {
    selectedEngine.value = found;
    // 保存选择到 localStorage
    localStorage.setItem(STORAGE_KEY, found.value);
    engineDropdownVisible.value = false;
  }
}

// 执行搜索
function handleSearch() {
  if (!searchText.value.trim()) return;
  const url = selectedEngine.value.url + encodeURIComponent(searchText.value);
  window.open(url, '_blank');
}

// 全局点击事件，点击其他区域关闭下拉框
onMounted(() => {
  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement;
    if (!target.closest('.search-engine-selector')) {
      engineDropdownVisible.value = false;
    }
  });
});
</script>

<style scoped>
.main-search-box {
  width: 680px;
  margin-bottom: 3rem;
  margin-left: auto;
  margin-right: auto;
}

.search-engine-group {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.08);
  border: 1.5px solid rgba(255, 255, 255, 0.12);
  border-radius: 1.5rem;
  padding: 0.4rem;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
}

.search-engine-selector {
  position: relative;
  width: 2.2rem;
  height: 2.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  transition: all 0.2s;
}

.search-engine-selector:hover {
  background: rgba(255, 255, 255, 0.18);
  transform: translateY(-1px);
}

.search-engine-selector:active {
  transform: translateY(0);
}

.dropdown-arrow {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 14px;
  height: 14px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.2s;
}

.dropdown-arrow.is-active {
  transform: rotate(180deg);
  background: rgba(255, 255, 255, 0.25);
}

.search-engine-selector:hover .dropdown-arrow {
  background: rgba(255, 255, 255, 0.25);
}

.engine-icon {
  width: 1.3rem;
  height: 1.3rem;
  object-fit: contain;
  display: block;
}

.engine-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  background: rgba(20, 20, 40, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 0.5rem;
  min-width: 120px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  animation: dropdownFadeIn 0.2s ease;
}

@keyframes dropdownFadeIn {
  from {
    opacity: 0;
    transform: translate(-50%, -10px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}

.search-input-wrapper {
  position: relative;
  flex: 1;
  margin-left: 0.5rem;
}

.search-input {
  width: 100%;
  height: 2.2rem;
  background: transparent;
  border: none;
  outline: none;
  color: #fff;
  font-size: 1.1rem;
  padding: 0 2.2rem 0 0.5rem;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.search-icon {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: #4285f4;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
  width: 20px;
  height: 20px;
}

.search-icon:hover {
  color: #5c9eff;
}

.search-icon svg {
  width: 100%;
  height: 100%;
}

.engine-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  color: #fff;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.engine-option:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 响应式布局 */
@media screen and (max-width: 1200px) {
  .main-search-box {
    width: 90%;
    max-width: 680px;
  }
}

@media screen and (max-width: 480px) {
  .search-engine-group {
    flex-direction: column;
    padding: 0.3rem;
  }
  
  .search-engine-selector {
    width: 100%;
    height: 2rem;
    border-radius: 0.5rem;
    margin-bottom: 0.3rem;
  }
  
  .search-input-wrapper {
    margin-left: 0;
  }
  
  .search-input {
    height: 2rem;
    font-size: 1rem;
  }
}
</style> 