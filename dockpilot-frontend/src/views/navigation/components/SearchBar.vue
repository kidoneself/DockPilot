<template>
  <div class="search-section">
    <div class="search-container">
      <div class="search-engine-wrapper">
        <!-- 搜索引擎选择器 -->
        <n-dropdown
          :options="searchEngineOptions"
          placement="bottom-start"
          size="small"
          @select="handleEngineSelect"
        >
          <div 
            class="search-engine-btn-wrapper"
            :title="`当前搜索引擎: ${currentEngine.label}，点击切换`"
          >
            <n-icon :component="currentEngine.icon" class="engine-icon" />
            <span class="engine-label">{{ currentEngine.label }}</span>
            <n-icon :component="ChevronDownOutline" class="dropdown-icon" />
          </div>
        </n-dropdown>

        <!-- 搜索输入框 -->
        <n-input
          v-model:value="searchQuery"
          size="large"
          :placeholder="currentEngine.placeholder"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
          @update:value="handleSearchUpdate"
        >
          <template #prefix>
            <n-icon :component="SearchOutline" />
          </template>
          <template #suffix>
            <n-button
              v-if="searchQuery && currentEngine.key !== 'local'"
              text
              size="small"
              class="search-btn"
              @click="handleSearch"
            >
              搜索
            </n-button>
          </template>
        </n-input>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, markRaw, onMounted, h } from 'vue'
import { useMessage } from 'naive-ui'
import { SearchOutline, ChevronDownOutline, RocketOutline, FlashOutline, StarOutline, HomeOutline } from '@vicons/ionicons5'

// Props
interface Props {
  modelValue?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: ''
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const message = useMessage()

// 内部搜索值
const searchQuery = ref(props.modelValue)

// 搜索引擎配置
const searchEngines = [
  {
    key: 'google',
    label: '谷歌',
    icon: markRaw(RocketOutline),
    placeholder: '使用谷歌搜索...',
    searchUrl: 'https://www.google.com/search?q='
  },
  {
    key: 'bing',
    label: '必应',
    icon: markRaw(FlashOutline),
    placeholder: '使用必应搜索...',
    searchUrl: 'https://www.bing.com/search?q='
  },
  {
    key: 'baidu',
    label: '百度',
    icon: markRaw(StarOutline),
    placeholder: '使用百度搜索...',
    searchUrl: 'https://www.baidu.com/s?wd='
  },
  {
    key: 'local',
    label: '本地',
    icon: markRaw(HomeOutline),
    placeholder: '搜索本地应用、容器或服务...',
    searchUrl: ''
  }
]

// 当前选中的搜索引擎
const currentEngineKey = ref('local') // 默认使用本地搜索

// 当前搜索引擎
const currentEngine = computed(() => {
  return searchEngines.find(engine => engine.key === currentEngineKey.value) || searchEngines[3]
})

// 搜索引擎选项
const searchEngineOptions = computed(() => {
  return searchEngines.map(engine => ({
    label: engine.label,
    key: engine.key,
    icon: () => h('n-icon', { 
      component: engine.icon,
      style: {
        color: engine.key === 'google' ? '#22d3ee' : 
               engine.key === 'bing' ? '#a78bfa' :
               engine.key === 'baidu' ? '#fbbf24' : 
               '#34d399',
        fontSize: '16px',
        fontWeight: 'bold'
      }
    })
  }))
})

// 处理搜索引擎选择
const handleEngineSelect = (key: string) => {
  currentEngineKey.value = key
  console.log('🔍 切换搜索引擎:', currentEngine.value.label)
  
  // 保存用户偏好到 localStorage
  localStorage.setItem('dockpilot-search-engine', key)
  
  // 如果切换到本地搜索且有内容，立即触发本地搜索
  if (key === 'local' && searchQuery.value) {
    handleSearchUpdate(searchQuery.value)
  }
}

// 处理搜索
const handleSearch = () => {
  if (!searchQuery.value.trim()) {
    message.warning('请输入搜索内容')
    return
  }

  const engine = currentEngine.value
  
  if (engine.key === 'local') {
    // 本地搜索，触发父组件处理
    emit('update:modelValue', searchQuery.value)
    console.log('🔍 执行本地搜索:', searchQuery.value)
  } else {
    // 外部搜索引擎，打开新窗口
    const searchUrl = engine.searchUrl + encodeURIComponent(searchQuery.value)
    window.open(searchUrl, '_blank')
    console.log('🔍 打开外部搜索:', engine.label, searchUrl)
    message.success(`正在使用${engine.label}搜索: ${searchQuery.value}`)
  }
}

// 处理搜索更新（仅用于本地搜索）
const handleSearchUpdate = (value: string) => {
  searchQuery.value = value
  
  // 只有本地搜索才实时更新
  if (currentEngine.value.key === 'local') {
    emit('update:modelValue', value)
  }
}

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  searchQuery.value = newValue
})

// 加载搜索引擎偏好
const loadSearchEnginePreference = () => {
  const savedEngine = localStorage.getItem('dockpilot-search-engine')
  if (savedEngine && searchEngines.some(engine => engine.key === savedEngine)) {
    currentEngineKey.value = savedEngine
  }
  console.log('📖 加载搜索引擎偏好:', currentEngine.value.label)
}

// 组件挂载时加载偏好
onMounted(() => {
  loadSearchEnginePreference()
})
</script>

<style scoped>
.search-section {
  margin-bottom: 20px;
  width: 100%;
  display: flex;
  justify-content: center;
}

.search-container {
  max-width: 600px;
  width: 100%;
}

.search-engine-wrapper {
  display: flex;
  gap: 0;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  overflow: hidden;
  transition: all 0.2s ease;
}

.search-engine-wrapper:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
}

.search-engine-wrapper:focus-within {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.search-engine-btn-wrapper {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: transparent;
  border: none;
  border-radius: 20px 0 0 20px;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 80px;
  color: rgba(255, 255, 255, 0.8);
}

.search-engine-btn-wrapper:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.engine-icon {
  margin-right: 6px;
  font-size: 14px;
}

.engine-label {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  flex: 1;
}

.dropdown-icon {
  margin-left: 4px;
  font-size: 12px;
  opacity: 0.7;
}

.search-input {
  flex: 1;
  background: transparent !important;
  border: none !important;
  border-radius: 0 20px 20px 0 !important;
}

.search-input :deep(.n-input__input-el) {
  background: transparent !important;
  color: #f8fafc !important;
  font-size: 14px !important;
  border: none !important;
  padding-left: 16px !important;
}

.search-input :deep(.n-input__placeholder) {
  color: #64748b !important;
}

.search-input :deep(.n-input__border),
.search-input :deep(.n-input__state-border) {
  display: none !important;
}

.search-btn {
  color: rgba(59, 130, 246, 0.8) !important;
  font-size: 12px !important;
  padding: 4px 8px !important;
  margin-right: 8px !important;
}

.search-btn:hover {
  color: #3b82f6 !important;
  background: rgba(59, 130, 246, 0.1) !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-container {
    max-width: 100%;
  }
  
  .search-engine-btn-wrapper {
    padding: 6px 8px;
    min-width: 60px;
  }
  
  .engine-icon {
    font-size: 13px;
    margin-right: 4px;
  }
  
  .engine-label {
    font-size: 11px;
  }
  
  .dropdown-icon {
    font-size: 11px;
  }
  
  .search-input :deep(.n-input__input-el) {
    font-size: 13px !important;
  }
}

@media (max-width: 480px) {
  .search-engine-wrapper {
    border-radius: 20px;
  }
  
  .search-engine-btn-wrapper {
    padding: 6px 6px;
    min-width: 50px;
  }
  
  .engine-icon {
    font-size: 12px;
    margin-right: 3px;
  }
  
  .engine-label {
    font-size: 10px;
  }
  
  .dropdown-icon {
    font-size: 10px;
    margin-left: 2px;
  }
  
  .search-input {
    border-radius: 0 16px 16px 0 !important;
  }
  
  .search-input :deep(.n-input__input-el) {
    font-size: 12px !important;
    padding-left: 12px !important;
  }
}
</style> 