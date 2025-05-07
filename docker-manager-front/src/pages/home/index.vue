<template>
  <div class="home-container">
    <!-- 背景图片和遮罩 -->
    <div
      class="cover"
      :style="{
        filter: `blur(${backgroundBlur}px)`,
        background: `url('/src/assets/background.png') no-repeat`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }"
    />
    <div class="mask" :style="{ backgroundColor: `rgba(0,0,0,${backgroundMask})` }" />

    <main class="main-content new-layout">
      <!-- 顶部大标题和时间 -->
      <div class="main-title-row">
        <div class="main-title">D-Tools</div>
        <div class="main-time">
          {{ currentTime }}<span class="main-date"> {{ currentDate }}</span>
        </div>
      </div>
      <!-- 搜索框 -->
      <div class="main-search-box">
        <div class="search-engine-group">
          <t-dropdown
            :options="searchEngines.map((e) => ({ content: e.name, value: e.value }))"
            :min-column-width="120"
            @select="handleEngineChange"
          >
            <t-button shape="circle" variant="text" class="engine-btn">
              <svg class="icon" aria-hidden="true">
                <use :xlink:href="`#${selectedEngine.icon}`"></use>
              </svg>
            </t-button>
          </t-dropdown>
          <t-input
            v-model="searchText"
            placeholder="请输入搜索内容"
            clearable
            class="main-search-input"
            @enter="handleSearch"
          >
            <template #suffix-icon>
              <t-icon name="search" @click="handleSearch" style="cursor: pointer" />
            </template>
          </t-input>
        </div>
      </div>
      <!-- APP 网格 -->
      <div class="app-grid new-app-grid">
        <div
          v-for="(item, index) in appList"
          :key="index"
          class="app-item new-app-item"
          @click="handleAppClick(item)"
        >
          <div class="new-app-icon">
            <img :src="item.icon" alt="应用图标" />
          </div>
          <div class="app-info">
            <div class="app-name new-app-name">{{ item.name }}</div>
            <div class="app-desc">{{ item.desc }}</div>
          </div>
        </div>
        <div class="app-item add-item new-app-item" @click="handleAddApp">
          <div class="new-app-icon">
            <t-icon name="add" />
          </div>
          <div class="app-info">
            <div class="app-name new-app-name">添加应用</div>
            <div class="app-desc">自定义你的应用</div>
          </div>
        </div>
      </div>
    </main>


    <!-- 悬浮按钮组 -->
    <div class="fixed-buttons">
      <div class="button-group">
        <t-tooltip content="添加应用" placement="left">
          <t-button theme="primary" @click="handleAddApp" class="action-btn">
            <template #icon>
              <t-icon name="add" />
            </template>
          </t-button>
        </t-tooltip>
        <t-tooltip content="进入后台" placement="left">
          <t-button theme="primary" @click="openSettings" class="action-btn">
            <template #icon>
              <t-icon name="setting" />
            </template>
          </t-button>
        </t-tooltip>
        <t-tooltip :content="isInternalNetwork ? '当前：内网模式' : '当前：外网模式'" placement="left">
          <t-button 
            theme="primary" 
            @click="toggleNetworkMode" 
            class="action-btn network-btn"
            :class="{ 'is-external': !isInternalNetwork }"
          >
            <template #icon>
              <t-icon :name="isInternalNetwork ? 'cloud-download' : 'cloud-upload'" />
            </template>
          </t-button>
        </t-tooltip>
      </div>
    </div>

    <!-- 内嵌窗口 -->
    <t-dialog v-model:visible="windowVisible" :header="windowTitle" :footer="false" width="80%" height="80%">
      <iframe v-if="windowUrl" :src="windowUrl" class="window-iframe" frameborder="0" />
    </t-dialog>

    <!-- 新增应用对话框 -->
    <t-dialog
      v-model:visible="addAppVisible"
      header="添加新应用"
      :footer="false"
      width="500px"
    >
      <t-form
        ref="form"
        :data="newApp"
        :rules="rules"
        label-width="80px"
        @submit="onSubmit"
      >
        <t-form-item label="应用名称" name="name">
          <t-input v-model="newApp.name" placeholder="请输入应用名称" />
        </t-form-item>
        <t-form-item label="应用图标" name="icon">
          <div class="icon-input-group">
            <t-upload
              :files="uploadFiles"
              :action="uploadUrl"
              :headers="uploadHeaders"
              accept="image/*"
              :show-upload-progress="false"
              :before-upload="beforeUpload"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
            >
              <t-button theme="primary" variant="outline">上传图标</t-button>
            </t-upload>
            <div v-if="newApp.icon" class="icon-preview">
              <img :src="newApp.icon" alt="图标预览" />
            </div>
          </div>
        </t-form-item>
        <t-form-item label="内网地址" name="internalUrl">
          <t-input v-model="newApp.internalUrl" placeholder="请输入内网访问地址" />
        </t-form-item>
        <t-form-item label="外网地址" name="externalUrl">
          <t-input v-model="newApp.externalUrl" placeholder="请输入外网访问地址" />
        </t-form-item>
        <t-form-item label="应用描述" name="desc">
          <t-input v-model="newApp.desc" placeholder="请输入应用描述" />
        </t-form-item>
        <t-form-item>
          <t-space>
            <t-button theme="primary" type="submit">保存</t-button>
            <t-button theme="default" @click="addAppVisible = false">取消</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { MessagePlugin, FormRules, SubmitContext, UploadFile } from 'tdesign-vue-next';
import { useRouter } from 'vue-router';

const router = useRouter();

// 背景配置
const backgroundBlur = ref(0.5);
const backgroundMask = ref(0.3);

// 时钟
const currentTime = ref('');
const currentDate = ref('');
let timer: number;

function updateTime() {
  const now = new Date();
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
  currentDate.value = `${now.getMonth() + 1}-${now.getDate()} 星期${'日一二三四五六'[now.getDay()]}`;
}

// 搜索
const searchText = ref('');

// 内嵌窗口
const windowVisible = ref(false);
const windowTitle = ref('');
const windowUrl = ref('');

// 应用列表
const appList = ref([
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能Web服务器'
  },
  {
    name: 'MySQL',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '流行的关系型数据库'
  },
  {
    name: 'Redis',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能缓存数据库'
  },
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能Web服务器'
  },
  {
    name: 'MySQL',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '流行的关系型数据库'
  },
  {
    name: 'Redis',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能缓存数据库'
  },
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能Web服务器'
  },
  {
    name: 'MySQL',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '流行的关系型数据库'
  },
  {
    name: 'Redis',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能缓存数据库'
  },
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能Web服务器'
  },
  {
    name: 'MySQL',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '流行的关系型数据库'
  },
  {
    name: 'Redis',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能缓存数据库'
  },
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能Web服务器'
  },
  {
    name: 'MySQL',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '流行的关系型数据库'
  },
  {
    name: 'Redis',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能缓存数据库'
  },
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能Web服务器'
  },
  {
    name: 'MySQL',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '流行的关系型数据库'
  },
  {
    name: 'Redis',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能缓存数据库'
  },
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能Web服务器'
  },
  {
    name: 'MySQL',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '流行的关系型数据库'
  },
  {
    name: 'Redis',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能缓存数据库'
  },
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能Web服务器'
  },
  {
    name: 'MySQL',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '流行的关系型数据库'
  },
  {
    name: 'Redis',
    icon: 'https://pan.naspt.vip/d/naspt/emby%E5%9B%BE/MoviePoilt.jpg',
    internalUrl: 'https://www.baidu.com',
    externalUrl: 'https://www.google.com',
    desc: '高性能缓存数据库'
  }
]);

// 搜索引擎
const searchEngines = [
  {
    name: 'Google',
    value: 'google',
    icon: 'icon-google',
    url: 'https://www.google.com/search?q=',
  },
  {
    name: '百度',
    value: 'baidu',
    icon: 'icon-baidu',
    url: 'https://www.baidu.com/s?wd=',
  },
];
const selectedEngine = ref(searchEngines[0]);

function handleEngineChange(val: string) {
  const found = searchEngines.find((e) => e.value === val);
  if (found) selectedEngine.value = found;
}

function handleSearch() {
  if (!searchText.value.trim()) return;
  const url = selectedEngine.value.url + encodeURIComponent(searchText.value);
  window.open(url, '_blank');
}

// 应用点击处理
function handleAppClick(item: any) {
  const url = isInternalNetwork.value ? item.internalUrl : item.externalUrl;
  if (!url) {
    MessagePlugin.error(`${isInternalNetwork.value ? '内网' : '外网'}地址未设置`);
    return;
  }
  windowTitle.value = item.name;
  windowUrl.value = url;
  windowVisible.value = true;
}

// 新增应用相关
const addAppVisible = ref(false);
const newApp = ref({
  name: '',
  icon: '',
  internalUrl: '',
  externalUrl: '',
  desc: ''
});

const rules: FormRules = {
  name: [{ required: true, message: '请输入应用名称', type: 'error' as const }],
  icon: [{ required: true, message: '请上传应用图标', type: 'error' as const }],
  internalUrl: [{ required: true, message: '请输入内网访问地址', type: 'error' as const }],
  externalUrl: [{ required: true, message: '请输入外网访问地址', type: 'error' as const }]
};

// 上传相关
const uploadUrl = 'http://your-upload-api.com/upload'; // 替换为实际的上传接口
const uploadHeaders = {
  // 添加需要的认证头
};
const uploadFiles = ref<UploadFile[]>([]);

function beforeUpload(file: UploadFile) {
  const isImage = file.type?.startsWith('image/');
  if (!isImage) {
    MessagePlugin.error('只能上传图片文件！');
    return false;
  }
  const isLt2M = (file.size || 0) / 1024 / 1024 < 2;
  if (!isLt2M) {
    MessagePlugin.error('图片大小不能超过 2MB！');
    return false;
  }
  return true;
}

function handleUploadSuccess(response: any) {
  if (response.code === 0) {
    newApp.value.icon = response.data.url;
    uploadFiles.value = [];
    MessagePlugin.success('上传成功');
  } else {
    MessagePlugin.error('上传失败：' + response.message);
  }
}

function handleUploadError() {
  MessagePlugin.error('上传失败，请重试');
}

function handleAddApp() {
  addAppVisible.value = true;
  newApp.value = {
    name: '',
    icon: '',
    internalUrl: '',
    externalUrl: '',
    desc: ''
  };
}

function onSubmit(context: SubmitContext) {
  if (context.validateResult === true) {
    MessagePlugin.info('保存功能开发中...');
    addAppVisible.value = false;
  }
}

// 其他功能
function openSettings() {
  router.push('/docker/containers');
}

// 网络模式
const isInternalNetwork = ref(true);

function toggleNetworkMode() {
  isInternalNetwork.value = !isInternalNetwork.value;
  MessagePlugin.info(`已切换到${isInternalNetwork.value ? '内网' : '外网'}模式`);
}

onMounted(() => {
  updateTime();
  timer = window.setInterval(updateTime, 1000);
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
  }
});
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.cover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transform: scale(1.05);
}

.mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.main-content {
  position: relative;
  z-index: 1;
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.new-layout {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  min-height: 100vh;
  padding-top: 5vh;
}

.main-title-row {
  display: flex;
  align-items: flex-end;
  gap: 2rem;
  margin-bottom: 2.5rem;
}

.main-title {
  font-size: 3rem;
  font-weight: 800;
  color: #fff;
  letter-spacing: 2px;
  text-shadow: 0 4px 24px rgba(0, 0, 0, 0.18);
}

.main-time {
  font-size: 1.5rem;
  color: #fff;
  font-weight: 400;
  opacity: 0.85;
}

.main-date {
  font-size: 1rem;
  margin-left: 0.5rem;
  opacity: 0.7;
}

.main-search-box {
  width: 480px;
  margin-bottom: 3rem;
}

.search-engine-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.engine-btn {
  background: rgba(255, 255, 255, 0.1) !important;
  border-radius: 50%;
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  color: #fff !important;
  border: none;
  box-shadow: none;
  transition: background 0.2s;
}

.engine-btn:hover {
  background: rgba(255, 255, 255, 0.18) !important;
}

.main-search-input {
  box-shadow: none;
}

:deep(.main-search-input .t-input) {
  border-radius: 2rem !important;
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1.5px solid rgba(255, 255, 255, 0.12) !important;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
}

:deep(.main-search-input .t-input__inner) {
  border-radius: 2rem !important;
  background: transparent !important;
  color: #fff !important;
  font-size: 1.15rem;
}

:deep(.main-search-input .t-input__prefix-icon) {
  color: #4285f4 !important;
  font-size: 1.3rem;
}

.new-app-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 1.5rem;
  margin-top: 1.5rem;
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
}

.new-app-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.fixed-buttons {
  position: fixed;
  right: 2rem;
  bottom: 2rem;
  z-index: 10;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.action-btn {
  width: 2.8rem !important;
  height: 2.8rem !important;
  border-radius: 12px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease !important;
  margin: 0 !important;
  cursor: pointer !important;
}

.action-btn:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.15) !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-btn :deep(.t-icon) {
  font-size: 1.3rem;
  color: #fff;
}

.network-btn {
  background: rgba(59, 130, 246, 0.2) !important;
  border-color: rgba(59, 130, 246, 0.3) !important;
}

.network-btn.is-external {
  background: rgba(16, 185, 129, 0.2) !important;
  border-color: rgba(16, 185, 129, 0.3) !important;
}

.network-btn:hover {
  background: rgba(59, 130, 246, 0.3) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
}

.network-btn.is-external:hover {
  background: rgba(16, 185, 129, 0.3) !important;
  border-color: rgba(16, 185, 129, 0.4) !important;
}

.window-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

:deep(.t-input) {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
}

:deep(.t-input__inner) {
  color: #fff !important;
}

:deep(.t-input__prefix-icon) {
  color: #94a3b8 !important;
}

:deep(.t-button) {
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
}

:deep(.t-button:hover) {
  background: rgba(255, 255, 255, 0.15) !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
}

.icon-btn-group {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  background: rgba(20, 40, 80, 0.7);
  border-radius: 20px;
  padding: 1rem 1.5rem;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
}

.icon-btn :deep(.t-icon) {
  color: #fff !important;
  font-size: 2rem;
}

.icon-btn {
  background: transparent !important;
  box-shadow: none !important;
}

.soft-glass {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.04);
  padding: 0.5rem 1rem;
  gap: 1rem;
}

.soft-btn {
  background: transparent !important;
  box-shadow: none !important;
  transition: background 0.2s;
}

.soft-btn :deep(.t-icon) {
  color: #fff !important;
  font-size: 1.5rem;
}

.soft-btn:hover {
  background: rgba(255, 255, 255, 0.15) !important;
}

.icon {
  width: 1.5em;
  height: 1.5em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
}

/* 新增应用对话框样式 */
:deep(.t-dialog) {
  background: rgba(20, 20, 40, 0.75) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
}

:deep(.t-dialog__header) {
  color: #fff !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.t-dialog__body) {
  color: #fff !important;
}

/* 统一表单样式 */
:deep(.t-form__item) {
  margin-bottom: 24px;
}

:deep(.t-form__label) {
  color: #e2e8f0 !important;
  font-size: 14px !important;
  line-height: 40px !important;
}

:deep(.t-input),
:deep(.t-select),
:deep(.t-color-picker) {
  width: 100% !important;
  height: 40px !important;
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  border-radius: 8px !important;
}

:deep(.t-input__inner),
:deep(.t-select__value) {
  height: 40px !important;
  line-height: 40px !important;
  font-size: 14px !important;
  color: #fff !important;
}

:deep(.t-input__placeholder),
:deep(.t-select__placeholder) {
  color: rgba(255, 255, 255, 0.5) !important;
}

:deep(.t-form__error) {
  color: #f87171 !important;
  font-size: 12px !important;
  margin-top: 4px !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

:deep(.t-input--error) {
  border-color: #f87171 !important;
}

:deep(.t-input--error:hover) {
  border-color: #f87171 !important;
}

:deep(.t-input--error:focus) {
  border-color: #f87171 !important;
  box-shadow: 0 0 0 2px rgba(248, 113, 113, 0.2) !important;
}

/* 下拉菜单样式 */
:deep(.t-select__dropdown) {
  background: rgba(20, 20, 40, 0.95) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  backdrop-filter: blur(10px);
}

:deep(.t-select__option) {
  color: #fff !important;
}

:deep(.t-select__option:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
}

/* 按钮样式 */
:deep(.t-button) {
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: #fff !important;
}

:deep(.t-button--primary) {
  background: rgba(59, 130, 246, 0.8) !important;
  border: 1px solid rgba(59, 130, 246, 0.3) !important;
}

:deep(.t-button:hover) {
  background: rgba(255, 255, 255, 0.15) !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
}

:deep(.t-button--primary:hover) {
  background: rgba(59, 130, 246, 0.9) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
}

/* 移除颜色选择器相关样式 */
:deep(.t-color-picker__panel),
:deep(.t-color-picker__input) {
  display: none;
}

/* 图标输入组样式 */
.icon-input-group {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 100%;
}

.icon-input-group :deep(.t-input) {
  flex: 1;
}

.icon-preview {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.icon-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

:deep(.t-tooltip) {
  --td-tooltip-bg-color: rgba(0, 0, 0, 0.8);
  --td-tooltip-color: #fff;
  --td-tooltip-padding: 8px 12px;
  --td-tooltip-border-radius: 6px;
  --td-tooltip-font-size: 14px;
}

:deep(.t-tooltip__content) {
  backdrop-filter: blur(4px);
}
</style>
