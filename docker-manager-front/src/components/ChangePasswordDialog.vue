<template>
  <t-dialog
    :visible="visible"
    @update:visible="(val) => emit('update:visible', val)"
    header="修改密码"
    :width="500"
    :footer="false"
  >
    <t-form
      ref="form"
      :data="formData"
      :rules="rules"
      @submit="onSubmit"
    >
      <input
        type="text"
        name="username"
        autocomplete="username"
        style="display: none"
      />
      <t-form-item label="原密码" name="oldPassword">
        <t-input
          v-model="formData.oldPassword"
          type="password"
          placeholder="请输入原密码"
          autocomplete="current-password"
        />
      </t-form-item>
      <t-form-item label="新密码" name="newPassword">
        <t-input
          v-model="formData.newPassword"
          type="password"
          placeholder="请输入新密码"
          autocomplete="new-password"
        />
      </t-form-item>
      <t-form-item label="确认新密码" name="confirmPassword">
        <t-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          autocomplete="new-password"
        />
      </t-form-item>
      <t-form-item>
        <t-space>
          <t-button theme="primary" type="submit">确认</t-button>
          <t-button theme="default" @click="onClose">取消</t-button>
        </t-space>
      </t-form-item>
    </t-form>
  </t-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { changePasswordApi } from '@/api/userApi';
import type { SubmitContext } from 'tdesign-vue-next';

interface FormData {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits(['update:visible', 'success']);

const form = ref();
const formData = reactive<FormData>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 6, message: '密码长度不能小于6位' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码' },
    {
      validator: (val: string) => val === formData.newPassword,
      message: '两次输入的密码不一致',
    },
  ],
};

const onSubmit = async (context: SubmitContext) => {
  if (context.validateResult === true) {
    try {
      const response = await changePasswordApi({
        oldPassword: formData.oldPassword,
        newPassword: formData.newPassword,
      });
      if (response.code !== 0) {
        throw new Error(response.message || '密码修改失败');
      }
      MessagePlugin.success('密码修改成功');
      emit('success');
      onClose();
    } catch (error) {
      MessagePlugin.error(error.message || '密码修改失败');
    }
  }
};

const onClose = () => {
  emit('update:visible', false);
  form.value?.reset();
  Object.assign(formData, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
};
</script> 