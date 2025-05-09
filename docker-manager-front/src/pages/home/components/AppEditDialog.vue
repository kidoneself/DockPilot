<template>
  <t-dialog
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    :header="isEdit ? '编辑应用' : '添加新应用'"
    :footer="false"
    width="500px"
  >
    <t-form
      ref="form"
      :data="formData"
      :rules="rules"
      label-width="80px"
      @submit="onSubmit"
    >
      <t-form-item label="应用名称" name="name">
        <t-input v-model="formData.name" placeholder="请输入应用名称" />
      </t-form-item>
      <t-form-item label="应用图标" name="icon">
        <div class="icon-container">
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
            <div class="icon-preview">
              <t-loading v-if="iconLoading" size="medium" />
              <template v-else>
                <img v-if="formData.icon" :src="formData.icon" alt="图标预览" />
                <div v-else class="icon-placeholder">
                  <t-icon name="image" />
                  <span class="upload-text">点击上传</span>
                </div>
              </template>
            </div>
          </t-upload>
          <t-button 
            theme="primary" 
            variant="outline" 
            :disabled="!(formData.internalUrl && formData.internalUrl.trim()) && !(formData.externalUrl && formData.externalUrl.trim())"
            @click="handleGetFavicon"
            :loading="iconLoading"
          >获取图标</t-button>
        </div>
      </t-form-item>
      <t-form-item label="内网地址" name="internalUrl">
        <t-input v-model="formData.internalUrl" placeholder="请输入内网访问地址" />
      </t-form-item>
      <t-form-item label="外网地址" name="externalUrl">
        <t-input v-model="formData.externalUrl" placeholder="请输入外网访问地址" />
      </t-form-item>
      <t-form-item label="应用描述" name="description">
        <t-input v-model="formData.description" placeholder="请输入应用描述" />
      </t-form-item>
      <t-form-item label="排序" name="itemSort">
        <t-input-number 
          v-model="formData.itemSort" 
          theme="column"
          align="left"
          :min="0" 
          :max="999" 
          style="width: 80px"
        />
      </t-form-item>
      <t-form-item>
        <t-space>
          <t-button theme="primary" type="submit">保存</t-button>
          <t-button theme="default" @click="onCancel">取消</t-button>
          <t-button 
            v-if="isEdit" 
            theme="danger" 
            variant="outline" 
            @click="onDeleteClick"
          >删除</t-button>
        </t-space>
      </t-form-item>
    </t-form>
  </t-dialog>
</template>

<script setup lang="ts">
import { ref, computed, defineProps, defineEmits, watch } from 'vue';
import { MessagePlugin, FormRules, SubmitContext, UploadFile } from 'tdesign-vue-next';
import { useHomeStore } from '@/store/modules/home';
import type { AppItem } from '@/store/modules/home';

// 定义组件属性
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  isEdit: {
    type: Boolean,
    default: false
  },
  appData: {
    type: Object as () => Partial<AppItem>,
    default: () => ({})
  }
});

// 定义组件事件
const emit = defineEmits(['update:visible']);

// 初始化 store
const homeStore = useHomeStore();

// 表单数据
const formData = ref<Partial<AppItem>>({
  name: '',
  icon: '',
  internalUrl: '',
  externalUrl: '',
  description: '',
  category: '',
  itemSort: 0
});

// 图标加载状态
const iconLoading = ref(false);

// 上传相关
const uploadUrl = 'http://your-upload-api.com/upload'; // 替换为实际的上传接口
const uploadHeaders = {
  // 添加需要的认证头
};
const uploadFiles = ref<UploadFile[]>([]);

// 表单验证规则
const rules: FormRules = {
  name: [{ required: true, message: '请输入应用名称', type: 'error' as const }],
  // 图标为非必填
  // 自定义验证：内网地址和外网地址至少填一个
  internalUrl: [
    { 
      validator: (val: string) => {
        // 如果外网地址有值，则内网地址可以为空
        const externalUrl = formData.value.externalUrl;
        return !!val || !!externalUrl;
      },
      message: '内网地址和外网地址至少填写一个',
      type: 'error' as const
    }
  ],
  externalUrl: [
    {
      validator: (val: string) => {
        // 如果内网地址有值，则外网地址可以为空
        const internalUrl = formData.value.internalUrl;
        return !!val || !!internalUrl;
      },
      message: '内网地址和外网地址至少填写一个',
      type: 'error' as const
    }
  ]
};

// 监听 store.newApp.icon 的变化，只更新图标字段
watch(() => homeStore.newApp.icon, (newIcon) => {
  if (newIcon) {
    formData.value.icon = newIcon;
  }
});

// 只在弹窗打开时同步一次formData
watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      formData.value = { ...(props.appData as AppItem) };
    }
  },
  { immediate: true }
);

// 监听 visible 变化
watch(() => props.visible, (newVal) => {
  if (!newVal) {
    // 重置表单
    if (!props.isEdit) {
      formData.value = {
        name: '',
        icon: '',
        internalUrl: '',
        externalUrl: '',
        description: '',
        category: '',
        itemSort: 0
      };
    }
  }
});

// 上传前验证
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

// 上传成功回调
function handleUploadSuccess(response: any) {
  if (response.code === 0) {
    formData.value.icon = response.data.url;
    uploadFiles.value = [];
    MessagePlugin.success('上传成功');
  } else {
    MessagePlugin.error('上传失败：' + response.message);
  }
}

// 上传失败回调
function handleUploadError() {
  MessagePlugin.error('上传失败，请重试');
}

// 获取图标
function handleGetFavicon() {
  const internalUrl = (formData.value.internalUrl || '').trim();
  const externalUrl = (formData.value.externalUrl || '').trim();

  if (!internalUrl && !externalUrl) {
    MessagePlugin.error('请先填写内网或外网地址');
    return;
  }
  const url = internalUrl || externalUrl;
  
  // 设置加载状态
  iconLoading.value = true;
  
  // 传递当前表单数据，确保其他字段不会丢失
  homeStore.handleGetFavicon(url, { ...formData.value })
    .finally(() => {
      // 无论成功或失败，都结束加载状态
      iconLoading.value = false;
    });
}

// 提交表单
function onSubmit(context: SubmitContext) {
  if (context.validateResult === true) {
    homeStore.onSubmitApp(formData.value as AppItem);
    emit('update:visible', false);
  }
}

// 取消操作
function onCancel() {
  emit('update:visible', false);
}

// 删除操作
function onDeleteClick() {
  homeStore.handleDeleteApp(formData.value as AppItem);
}
</script>

<style scoped>
/* 图标容器样式 */
.icon-container {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 100%;
}

.icon-preview {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  background: #f5f6fa; /* 更明显的浅灰色 */
  border: 1.5px dashed #bdbdbd; /* 虚线边框更醒目 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.icon-preview:hover {
  border-color: var(--td-brand-color);
  background: #e6f7ff;
}

.icon-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.icon-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #bdbdbd; /* 更深的灰色 */
  gap: 0.5rem;
}

.icon-placeholder .t-icon {
  font-size: 2rem;
}

.upload-text {
  font-size: 0.875rem;
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .icon-container {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .icon-preview {
    width: 100%;
    height: 120px;
  }
}
</style> 