<template>
  <div class="home-container" @contextmenu.prevent="handlePageContextMenu">
    <!-- 背景图片和遮罩 -->
    <div
      class="cover"
      :style="{
        filter: `blur(${backgroundBlur}px)`,
        background: `url(${backgroundImage}) no-repeat`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }"
    />
    <div class="mask" :style="{ backgroundColor: `rgba(0,0,0,${backgroundMask})` }" />

    <main class="main-content new-layout">
      <!-- 顶部大标题和时间 -->
      <div class="main-title-row">
        <div class="main-title">DockPanel</div>
        <div class="main-time">
          {{ currentTime }}<span class="main-date"> {{ currentDate }}</span>
        </div>
      </div>
      <!-- 搜索框 -->
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
      <!-- APP 网格 -->
      <div class="app-grid new-app-grid">
        <div
          v-for="(item, index) in appList"
          :key="index"
          class="app-item new-app-item"
          @click="handleAppClick(item)"
          @contextmenu.prevent="handleContextMenu($event, item)"
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
      :header="isEdit ? '编辑应用' : '添加新应用'"
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
            <t-button 
              v-if="isEdit" 
              theme="danger" 
              variant="outline" 
              @click="handleDeleteApp(newApp)"
            >删除</t-button>
          </t-space>
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 右键菜单 -->
    <div 
      v-show="contextMenuVisible" 
      class="context-menu"
      :style="contextMenuStyle"
    >
      <!-- 应用卡片菜单 -->
      <template v-if="currentContextItem">
        <div class="context-menu-item" @click="handleOpenApp('internal')">
          <t-icon name="cloud-download" />
          <span>打开内网</span>
        </div>
        <div class="context-menu-item" @click="handleOpenApp('external')">
          <t-icon name="cloud-upload" />
          <span>打开外网</span>
        </div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" @click="handleContextMenuClick({ value: 'edit' })">
          <t-icon name="edit" />
          <span>编辑</span>
        </div>
      </template>
      
      <!-- 页面菜单 -->
      <template v-else>
        <div class="context-menu-item" @click="handleRefresh">
          <t-icon name="refresh" />
          <span>刷新</span>
        </div>
      </template>
    </div>

    <!-- 确认对话框 -->
    <div v-if="confirmVisible" class="confirm-dialog-overlay" @click="handleConfirmOverlayClick">
      <div class="confirm-dialog" @click.stop>
        <div class="confirm-dialog-header">
          <t-icon name="warning" class="warning-icon" />
          <span>{{ confirmConfig.title }}</span>
        </div>
        <div class="confirm-dialog-body">
          {{ confirmConfig.content }}
        </div>
        <div class="confirm-dialog-footer">
          <t-button theme="default" @click="handleConfirmCancel">取消</t-button>
          <t-button theme="danger" @click="handleConfirmConfirm">确认</t-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { MessagePlugin, FormRules, SubmitContext, UploadFile, DialogPlugin } from 'tdesign-vue-next';
import { useRouter } from 'vue-router';
// 导入背景图片
import backgroundImage from '/src/assets/background.png';

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

// 使用 Map 来管理图片错误状态
const imageErrorMap = new Map<string, boolean>();

// 应用列表
const appList = ref([
  {
    name: 'Nginx',
    icon: 'https://pan.naspt.vip/d/naspt/11emby%E5%9B%BE/MoviePoilt.jpg',
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
const engineDropdownVisible = ref(false);

function toggleEngineDropdown(e: MouseEvent) {
  e.stopPropagation();
  engineDropdownVisible.value = !engineDropdownVisible.value;
}

function handleEngineChange(val: string) {
  const found = searchEngines.find((e) => e.value === val);
  if (found) {
    selectedEngine.value = found;
    // 保存选择到 localStorage
    localStorage.setItem(STORAGE_KEY, found.value);
    engineDropdownVisible.value = false;
  }
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

// 编辑状态
const isEdit = ref(false);

function handleEditApp(item: any) {
  isEdit.value = true;
  newApp.value = { ...item };
  addAppVisible.value = true;
}

function handleAddApp() {
  isEdit.value = false;
  newApp.value = {
    name: '',
    icon: '',
    internalUrl: '',
    externalUrl: '',
    desc: ''
  };
  addAppVisible.value = true;
}

// 确认对话框相关
const confirmVisible = ref(false);
const confirmConfig = ref({
  title: '',
  content: '',
  onConfirm: () => {}
});

function handleDeleteApp(item: any) {
  confirmConfig.value = {
    title: '确认删除',
    content: `确定要删除应用 "${item.name}" 吗？`,
    onConfirm: () => {
      // TODO: 实现删除逻辑
      MessagePlugin.info('删除功能开发中...');
      addAppVisible.value = false;
      confirmVisible.value = false;
    }
  };
  confirmVisible.value = true;
}

function handleConfirmOverlayClick() {
  confirmVisible.value = false;
}

function handleConfirmCancel() {
  confirmVisible.value = false;
}

function handleConfirmConfirm() {
  confirmConfig.value.onConfirm();
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

// 右键菜单相关
const contextMenuVisible = ref(false);
const contextMenuStyle = ref({
  position: 'fixed' as const,
  left: '0px',
  top: '0px',
  zIndex: 1000
});
const currentContextItem = ref<any>(null);

function handleContextMenu(event: MouseEvent, item: any) {
  event.preventDefault();
  currentContextItem.value = item;
  
  // 获取点击的元素
  const target = event.currentTarget as HTMLElement;
  const rect = target.getBoundingClientRect();
  
  // 获取视口尺寸
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  
  // 计算菜单位置,确保不会超出视口
  const menuWidth = 120; // 预估菜单宽度
  const menuHeight = 160; // 预估菜单高度
  
  let left = event.clientX;
  let top = event.clientY;
  
  // 如果菜单会超出右边界,则向左偏移
  if (left + menuWidth > viewportWidth) {
    left = viewportWidth - menuWidth;
  }
  
  // 如果菜单会超出下边界,则向上偏移
  if (top + menuHeight > viewportHeight) {
    top = viewportHeight - menuHeight;
  }
  
  contextMenuStyle.value = {
    position: 'fixed' as const,
    left: `${left}px`,
    top: `${top}px`,
    zIndex: 1000
  };
  contextMenuVisible.value = true;
}

function handlePageContextMenu(event: MouseEvent) {
  // 如果点击的是应用卡片，不处理
  if ((event.target as HTMLElement).closest('.app-item')) {
    return;
  }
  
  currentContextItem.value = null;
  
  // 获取视口尺寸
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  
  // 计算菜单位置
  const menuWidth = 120;
  const menuHeight = 40;
  
  let left = event.clientX;
  let top = event.clientY;
  
  if (left + menuWidth > viewportWidth) {
    left = viewportWidth - menuWidth;
  }
  
  if (top + menuHeight > viewportHeight) {
    top = viewportHeight - menuHeight;
  }
  
  contextMenuStyle.value = {
    position: 'fixed' as const,
    left: `${left}px`,
    top: `${top}px`,
    zIndex: 1000
  };
  contextMenuVisible.value = true;
}

function handleOpenApp(type: 'internal' | 'external') {
  if (!currentContextItem.value) return;
  
  const url = type === 'internal' 
    ? currentContextItem.value.internalUrl 
    : currentContextItem.value.externalUrl;
    
  if (!url) {
    MessagePlugin.error(`${type === 'internal' ? '内网' : '外网'}地址未设置`);
    return;
  }
  
  windowTitle.value = currentContextItem.value.name;
  windowUrl.value = url;
  windowVisible.value = true;
  contextMenuVisible.value = false;
}

function handleRefresh() {
  window.location.reload();
}

function handleContextMenuClick(dropdownItem: any) {
  if (!currentContextItem.value) return;
  
  switch (dropdownItem.value) {
    case 'edit':
      handleEditApp(currentContextItem.value);
      break;
    case 'delete':
      handleDeleteApp(currentContextItem.value);
      break;
  }
  contextMenuVisible.value = false;
}

// 处理图片加载失败
function handleImageError(event: Event, item: any) {
  const img = event.target as HTMLImageElement;
  imageErrorMap.set(item.icon, true);
  img.style.display = 'none';
}

onMounted(() => {
  updateTime();
  timer = window.setInterval(updateTime, 1000);
  
  // 添加全局点击事件监听
  document.addEventListener('click', () => {
    contextMenuVisible.value = false;
  });

  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement;
    if (!target.closest('.search-engine-selector')) {
      engineDropdownVisible.value = false;
    }
  });
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
  }
  // 移除全局点击事件监听
  document.removeEventListener('click', () => {
    contextMenuVisible.value = false;
  });
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
  width: 680px;
  margin-bottom: 3rem;
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

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  background: rgba(20, 20, 40, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  padding: 4px;
  min-width: 120px;
  z-index: 1000;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  color: #fff;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.context-menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.context-menu-item .t-icon {
  font-size: 16px;
}

.context-menu-item span {
  font-size: 14px;
}

.context-menu-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 4px 0;
}

/* 编辑弹窗样式 */
:deep(.t-dialog__header) {
  color: #fff !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.t-dialog__body) {
  color: #fff !important;
}

:deep(.t-button--danger) {
  background: rgba(239, 68, 68, 0.2) !important;
  border-color: rgba(239, 68, 68, 0.3) !important;
  color: #ef4444 !important;
}

:deep(.t-button--danger:hover) {
  background: rgba(239, 68, 68, 0.3) !important;
  border-color: rgba(239, 68, 68, 0.4) !important;
}

/* 确认对话框样式 */
.confirm-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.confirm-dialog {
  background: rgba(20, 20, 40, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  width: 360px;
  overflow: hidden;
  animation: dialogFadeIn 0.2s ease;
}

.confirm-dialog-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
}

.warning-icon {
  color: #ef4444;
  font-size: 20px;
}

.confirm-dialog-body {
  padding: 20px;
  color: #e2e8f0;
  font-size: 14px;
  line-height: 1.5;
}

.confirm-dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@keyframes dialogFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 修改按钮样式 */
:deep(.t-button--danger) {
  background: rgba(239, 68, 68, 0.2) !important;
  border-color: rgba(239, 68, 68, 0.3) !important;
  color: #ef4444 !important;
}

:deep(.t-button--danger:hover) {
  background: rgba(239, 68, 68, 0.3) !important;
  border-color: rgba(239, 68, 68, 0.4) !important;
}

:deep(.t-button--default) {
  background: rgba(255, 255, 255, 0.1) !important;
  border-color: rgba(255, 255, 255, 0.2) !important;
  color: #fff !important;
}

:deep(.t-button--default:hover) {
  background: rgba(255, 255, 255, 0.15) !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
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
  .new-app-grid {
    grid-template-columns: repeat(4, 1fr);
  }
  
  .main-search-box {
    width: 90%;
    max-width: 680px;
  }
}

@media screen and (max-width: 992px) {
  .new-app-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .main-title {
    font-size: 2.5rem;
  }
  
  .main-time {
    font-size: 1.3rem;
  }
}

@media screen and (max-width: 768px) {
  .new-app-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .main-title-row {
    flex-direction: column;
    align-items: center;
    gap: 1rem;
    text-align: center;
  }
  
  .main-title {
    font-size: 2rem;
  }
  
  .main-time {
    font-size: 1.1rem;
  }
  
  .main-content {
    padding: 1rem;
  }
  
  .fixed-buttons {
    right: 1rem;
    bottom: 1rem;
  }
  
  .action-btn {
    width: 2.5rem !important;
    height: 2.5rem !important;
  }
}

@media screen and (max-width: 480px) {
  .new-app-grid {
    grid-template-columns: 1fr;
  }
  
  .new-app-item {
    min-width: unset;
  }
  
  .main-title {
    font-size: 1.8rem;
  }
  
  .main-time {
    font-size: 1rem;
  }
  
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
  
  .fixed-buttons {
    right: 0.5rem;
    bottom: 0.5rem;
  }
  
  .action-btn {
    width: 2.2rem !important;
    height: 2.2rem !important;
  }
}

/* 优化对话框响应式 */
@media screen and (max-width: 768px) {
  :deep(.t-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }
  
  .confirm-dialog {
    width: 90%;
  }
  
  .icon-input-group {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .icon-preview {
    margin-top: 0.5rem;
  }
}

/* 优化右键菜单响应式 */
@media screen and (max-width: 480px) {
  .context-menu {
    min-width: 160px;
  }
  
  .context-menu-item {
    padding: 10px 12px;
  }
}
</style>
