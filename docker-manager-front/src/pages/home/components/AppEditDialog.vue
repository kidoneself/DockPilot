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
          <div v-if="formData.icon" class="icon-preview">
            <img :src="formData.icon" alt="图标预览" />
          </div>
        </div>
      </t-form-item>
      <t-form-item label="内网地址" name="internalUrl">
        <t-input v-model="formData.internalUrl" placeholder="请输入内网访问地址" />
      </t-form-item>
      <t-form-item label="外网地址" name="externalUrl">
        <t-input v-model="formData.externalUrl" placeholder="请输入外网访问地址" />
      </t-form-item>
      <t-form-item label="应用描述" name="desc">
        <t-input v-model="formData.desc" placeholder="请输入应用描述" />
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
const emit = defineEmits(['update:visible', 'submit', 'delete', 'cancel']);

// 表单数据
const formData = ref({
  name: '',
  icon: '',
  internalUrl: '',
  externalUrl: '',
  desc: ''
});

// 上传相关
const uploadUrl = 'http://your-upload-api.com/upload'; // 替换为实际的上传接口
const uploadHeaders = {
  // 添加需要的认证头
};
const uploadFiles = ref<UploadFile[]>([]);

// 表单验证规则
const rules: FormRules = {
  name: [{ required: true, message: '请输入应用名称', type: 'error' as const }],
  icon: [{ required: true, message: '请上传应用图标', type: 'error' as const }],
  internalUrl: [{ required: true, message: '请输入内网访问地址', type: 'error' as const }],
  externalUrl: [{ required: true, message: '请输入外网访问地址', type: 'error' as const }]
};

// 监听appData变化
watch(() => props.appData, (newVal) => {
  if (newVal) {
    formData.value = { ...newVal as AppItem };
  }
}, { immediate: true, deep: true });

// 监听visible变化
watch(() => props.visible, (newVal) => {
  if (!newVal) {
    // 重置表单
    if (!props.isEdit) {
      formData.value = {
        name: '',
        icon: '',
        internalUrl: '',
        externalUrl: '',
        desc: ''
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

// 提交表单
function onSubmit(context: SubmitContext) {
  if (context.validateResult === true) {
    emit('submit', formData.value);
  }
}

// 取消操作
function onCancel() {
  emit('update:visible', false);
  emit('cancel');
}

// 删除操作
function onDeleteClick() {
  emit('delete', formData.value);
}
</script>

<style scoped>
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

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .icon-input-group {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .icon-preview {
    margin-top: 0.5rem;
  }
}
</style> 