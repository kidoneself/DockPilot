<template>
  <n-modal v-model:show="visible" preset="dialog" title="修改密码">
    <n-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-placement="left"
      label-width="auto"
      require-mark-placement="right-hanging"
      style="max-width: 400px;"
    >
      <n-form-item label="旧密码" path="oldPassword">
        <n-input
          v-model:value="formData.oldPassword"
          type="password"
          placeholder="请输入当前密码"
          show-password-on="click"
          @keydown.enter="handleSubmit"
        />
      </n-form-item>
      
      <n-form-item label="新密码" path="newPassword">
        <n-input
          v-model:value="formData.newPassword"
          type="password"
          placeholder="请输入新密码"
          show-password-on="click"
          @input="handlePasswordInput"
          @keydown.enter="handleSubmit"
        />
      </n-form-item>
      
      <n-form-item label="确认密码" path="confirmPassword">
        <n-input
          v-model:value="formData.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          show-password-on="click"
          @keydown.enter="handleSubmit"
        />
      </n-form-item>
    </n-form>
    
    <template #action>
      <n-space>
        <n-button @click="handleCancel">取消</n-button>
        <n-button type="primary" :loading="loading" @click="handleSubmit">
          确认修改
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { changePassword } from '@/api/http/user'
import type { ChangePasswordRequest } from '@/api/http/user'

interface Props {
  show: boolean
}

interface Emits {
  (e: 'update:show', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const message = useMessage()
const formRef = ref<FormInst | null>(null)
const loading = ref(false)

const visible = ref(props.show)

const formData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 验证新密码
const validateNewPassword = (rule: any, value: string) => {
  if (!value) {
    return new Error('请输入新密码')
  }
  if (value.length < 6) {
    return new Error('密码长度不能少于6位')
  }
  if (value === formData.oldPassword) {
    return new Error('新密码不能与旧密码相同')
  }
  return true
}

// 验证确认密码
const validateConfirmPassword = (rule: any, value: string) => {
  if (!value) {
    return new Error('请确认新密码')
  }
  if (value !== formData.newPassword) {
    return new Error('两次输入的密码不一致')
  }
  return true
}

const rules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: ['input', 'blur'] }
  ],
  newPassword: [
    { required: true, validator: validateNewPassword, trigger: ['input', 'blur'] }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: ['input', 'blur'] }
  ]
}

// 监听props变化
watch(() => props.show, (newValue) => {
  visible.value = newValue
})

// 监听visible变化
watch(visible, (newValue) => {
  emit('update:show', newValue)
  if (!newValue) {
    resetForm()
  }
})

// 处理新密码输入，重新验证确认密码  
const handlePasswordInput = () => {
  // 当新密码变化时，如果确认密码已有值，清除确认密码的验证状态让用户重新输入
  if (formData.confirmPassword) {
    formRef.value?.restoreValidation()
  }
}

// 重置表单
const resetForm = () => {
  formData.oldPassword = ''
  formData.newPassword = ''
  formData.confirmPassword = ''
  formRef.value?.restoreValidation()
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    const requestData: ChangePasswordRequest = {
      oldPassword: formData.oldPassword,
      newPassword: formData.newPassword
    }

    await changePassword(requestData)
    
    message.success('密码修改成功')
    visible.value = false
    emit('success')
  } catch (error: any) {
    if (error?.response?.data?.message) {
      message.error(error.response.data.message)
    } else if (error?.message) {
      message.error(error.message)
    } else {
      message.error('密码修改失败，请重试')
    }
  } finally {
    loading.value = false
  }
}

// 取消操作
const handleCancel = () => {
  visible.value = false
}
</script>

<style scoped>
:deep(.n-form-item-label) {
  font-weight: 500;
}
</style> 