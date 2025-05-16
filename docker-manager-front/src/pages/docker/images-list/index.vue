<template>
  <div class="container">
    <div class="header">
      <div class="title-section">
        <h1 class="title">Docker 镜像管理</h1>
        <span class="subtitle">管理您的 Docker 镜像，包括拉取、更新和删除操作</span>
      </div>
      <div class="actions">
        <button class="action-btn primary" @click="showPullDialog = true">
          <i class="icon">+</i>拉取镜像
        </button>
        <button class="action-btn" @click="handleCheckUpdates">
          <i class="icon">↻</i>检查更新
        </button>
        <button class="action-btn" @click="fetchImages">
          <i class="icon">↻</i>刷新
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>
    
    <div v-else class="images-container">
      <div v-if="images.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <p>暂无镜像</p>
        <button class="action-btn primary" @click="showPullDialog = true">拉取第一个镜像</button>
      </div>
      <ImageListItem
        v-else
        v-for="image in images"
        :key="image.id"
        :image="image"
        @delete="onDelete"
        @update="onUpdate"
      />
    </div>

    <!-- 拉取镜像弹窗 -->
    <div v-if="showPullDialog" class="dialog-mask">
      <div class="dialog-box">
        <h3>拉取新镜像</h3>
        <div class="form-group">
          <label>镜像名称</label>
          <input v-model="pullImageName" placeholder="例如：nginx" />
        </div>
        <div class="form-group">
          <label>标签</label>
          <input v-model="pullImageTag" placeholder="例如：latest" />
        </div>
        <div class="dialog-actions">
          <button class="action-btn" @click="showPullDialog = false">取消</button>
          <button class="action-btn primary" @click="handlePull">确定</button>
        </div>
      </div>
    </div>

    <!-- 删除确认气泡 -->
    <ConfirmPopover v-model:visible="showDeletePopover" @confirm="confirmDelete" @cancel="cancelDelete">
      <template #default>
        <div v-if="deleteImageInfo" style="display:inline-block"></div>
      </template>
      <template #content>
        确定要删除镜像 {{ deleteImageInfo?.name }}:{{ deleteImageInfo?.tag }} 吗？
      </template>
    </ConfirmPopover>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getImageList, checkImageUpdates, pullImage, deleteImage } from '@/api/websocket/container';
import ImageListItem from './ImageListItem.vue';
import ConfirmPopover from '@/components/ConfirmPopover.vue';

const images = ref<any[]>([]);
const loading = ref(false);

const showPullDialog = ref(false);
const pullImageName = ref('');
const pullImageTag = ref('latest');

const showDeletePopover = ref(false);
const deleteImageInfo = ref<any>(null);

const fetchImages = async () => {
  loading.value = true;
  try {
    const res = await getImageList();
    images.value = res.data.map((img: any) => ({
      id: img.id.replace('sha256:', '') || '',
      name: img.name || '未命名镜像',
      tag: img.tag || 'latest',
      created: img.localCreateTime || img.created,
      lastChecked: img.lastChecked,
      needUpdate: img.needUpdate || false,
      size: img.size || 0,
      RepoTags: [`${img.name}:${img.tag}`],
    }));
  } finally {
    loading.value = false;
  }
};

const handleCheckUpdates = async () => {
  await checkImageUpdates();
  fetchImages();
};

const handlePull = async () => {
  if (!pullImageName.value) return;
  showPullDialog.value = false;
  await pullImage({ imageName: `${pullImageName.value}:${pullImageTag.value}` });
  pullImageName.value = '';
  pullImageTag.value = 'latest';
  fetchImages();
};

const onDelete = (image: any) => {
  deleteImageInfo.value = image;
  showDeletePopover.value = true;
};
const confirmDelete = async () => {
  if (!deleteImageInfo.value) return;
  await deleteImage(deleteImageInfo.value.id);
  showDeletePopover.value = false;
  deleteImageInfo.value = null;
  fetchImages();
};
const cancelDelete = () => {
  showDeletePopover.value = false;
  deleteImageInfo.value = null;
};

const onUpdate = async (image: any) => {
  // 这里只做刷新，实际可调用 updateImage
  fetchImages();
};

onMounted(fetchImages);
</script>

<style scoped>
.container {
  background: #f5f6fa;
  min-height: 100vh;
  padding: 48px 0 0 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 40px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
}

.title-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.subtitle {
  font-size: 15px;
  color: #888;
}

.actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border: none;
  border-radius: 8px;
  background: #f5f5f5;
  color: #333;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.action-btn:hover {
  background: #e6e6e6;
}

.action-btn.primary {
  background: #1890ff;
  color: white;
}

.action-btn.primary:hover {
  background: #40a9ff;
}

.icon {
  font-size: 18px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #666;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 18px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.images-container {
  max-width: 900px;
  margin: 0 auto;
  background: none;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #888;
}

.empty-icon {
  font-size: 54px;
  margin-bottom: 18px;
}

.dialog-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.dialog-box {
  background: white;
  border-radius: 14px;
  padding: 36px;
  min-width: 400px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
}

.dialog-box h3 {
  margin: 0 0 24px;
  font-size: 22px;
  color: #1a1a1a;
}

.form-group {
  margin-bottom: 22px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 15px;
}

.form-group input {
  width: 100%;
  padding: 9px 14px;
  border: 1.5px solid #d9d9d9;
  border-radius: 7px;
  font-size: 15px;
  transition: all 0.2s;
}

.form-group input:focus {
  border-color: #40a9ff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(24,144,255,0.13);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 28px;
}

@media screen and (max-width: 900px) {
  .header, .images-container {
    max-width: 100%;
    padding: 0 12px;
  }
}
@media screen and (max-width: 768px) {
  .container {
    padding: 24px 0 0 0;
  }
  .header, .images-container {
    max-width: 100%;
    padding: 0 6px;
  }
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 24px;
    padding-bottom: 16px;
  }
  .title {
    font-size: 20px;
  }
  .subtitle {
    font-size: 13px;
  }
  .action-btn {
    font-size: 13px;
    padding: 7px 12px;
    border-radius: 6px;
  }
  .images-container {
    padding: 0 2px;
  }
  .image-list-item {
    flex-direction: column;
    align-items: flex-start;
    padding: 14px 6px;
    margin-bottom: 10px;
    border-radius: 12px;
  }
}
@media screen and (max-width: 480px) {
  .container {
    padding: 10px 0 0 0;
  }
  .header, .images-container {
    padding: 0 2px;
  }
  .title {
    font-size: 17px;
  }
  .subtitle {
    font-size: 12px;
  }
  .image-list-item {
    padding: 8px 2px;
    border-radius: 8px;
  }
  .action-btn {
    font-size: 12px;
    padding: 6px 8px;
    border-radius: 5px;
  }
}
</style> 