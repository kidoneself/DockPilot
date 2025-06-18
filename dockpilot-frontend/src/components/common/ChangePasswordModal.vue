<template>
  <n-modal v-model:show="visible" preset="dialog" title="个人设置">
    <n-tabs v-model:value="activeTab" type="line" style="max-width: 400px;">
      <!-- 修改用户名标签页 -->
      <n-tab-pane name="username" tab="修改用户名">
        <n-form
          ref="usernameFormRef"
          :model="usernameFormData"
          :rules="usernameRules"
          label-placement="left"
          label-width="auto"
          require-mark-placement="right-hanging"
        >
          <n-form-item label="当前用户名" path="currentUsername">
            <n-input
              :value="currentUsername"
              readonly
              placeholder="当前用户名"
            />
          </n-form-item>
          
          <n-form-item label="新用户名" path="newUsername">
            <n-input
              v-model:value="usernameFormData.newUsername"
              placeholder="请输入新用户名"
              @keydown.enter="handleUsernameSubmit"
            />
          </n-form-item>
        </n-form>
      </n-tab-pane>
      
      <!-- 修改密码标签页 -->
      <n-tab-pane name="password" tab="修改密码">
        <n-form
          ref="passwordFormRef"
          :model="passwordFormData"
          :rules="passwordRules"
          label-placement="left"
          label-width="auto"
          require-mark-placement="right-hanging"
        >
          <n-form-item label="旧密码" path="oldPassword">
            <n-input
              v-model:value="passwordFormData.oldPassword"
              type="password"
              placeholder="请输入当前密码"
              show-password-on="click"
              @keydown.enter="handlePasswordSubmit"
            />
          </n-form-item>
          
          <n-form-item label="新密码" path="newPassword">
            <n-input
              v-model:value="passwordFormData.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password-on="click"
              @input="handlePasswordInput"
              @keydown.enter="handlePasswordSubmit"
            />
          </n-form-item>
          
          <n-form-item label="确认密码" path="confirmPassword">
            <n-input
              v-model:value="passwordFormData.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password-on="click"
              @keydown.enter="handlePasswordSubmit"
            />
          </n-form-item>
        </n-form>
      </n-tab-pane>
    </n-tabs>
    
    <template #action>
      <n-space>
        <n-button @click="handleCancel">取消</n-button>
        <n-button 
          v-if="activeTab === 'username'" 
          type="primary" 
          :loading="loading" 
          @click="handleUsernameSubmit"
        >
          确认修改用户名
        </n-button>
        <n-button 
          v-if="activeTab === 'password'" 
          type="primary" 
          :loading="loading" 
          @click="handlePasswordSubmit"
        >
          确认修改密码
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { changePassword, changeUsername } from '@/api/http/user'
import type { ChangePasswordRequest, ChangeUsernameRequest } from '@/api/http/user'
import { useUserStore } from '@/store/user'

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
const userStore = useUserStore()
const usernameFormRef = ref<FormInst | null>(null)
const passwordFormRef = ref<FormInst | null>(null)
const loading = ref(false)

const visible = ref(props.show)
const activeTab = ref('username')

// 用户名表单数据
const usernameFormData = reactive({
  newUsername: ''
})

// 密码表单数据
const passwordFormData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 当前用户名
const currentUsername = computed(() => userStore.userInfo?.username || '')

// 验证新用户名
const validateNewUsername = (rule: any, value: string) => {
  if (!value) {
    return new Error('请输入新用户名')
  }
  if (value.length < 3) {
    return new Error('用户名长度不能少于3位')
  }
  if (value === currentUsername.value) {
    return new Error('新用户名不能与当前用户名相同')
  }
  return true
}

// 验证新密码
const validateNewPassword = (rule: any, value: string) => {
  if (!value) {
    return new Error('请输入新密码')
  }
  if (value.length < 6) {
    return new Error('密码长度不能少于6位')
  }
  if (value === passwordFormData.oldPassword) {
    return new Error('新密码不能与旧密码相同')
  }
  return true
}

// 验证确认密码
const validateConfirmPassword = (rule: any, value: string) => {
  if (!value) {
    return new Error('请确认新密码')
  }
  if (value !== passwordFormData.newPassword) {
    return new Error('两次输入的密码不一致')
  }
  return true
}

// 用户名表单验证规则
const usernameRules: FormRules = {
  newUsername: [
    { required: true, validator: validateNewUsername, trigger: ['input', 'blur'] }
  ]
}

// 密码表单验证规则
const passwordRules: FormRules = {
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
  if (passwordFormData.confirmPassword) {
    passwordFormRef.value?.restoreValidation()
  }
}

// 重置表单
const resetForm = () => {
  // 重置用户名表单
  usernameFormData.newUsername = ''
  usernameFormRef.value?.restoreValidation()
  
  // 重置密码表单
  passwordFormData.oldPassword = ''
  passwordFormData.newPassword = ''
  passwordFormData.confirmPassword = ''
  passwordFormRef.value?.restoreValidation()
  
  // 重置到用户名标签页
  activeTab.value = 'username'
}

// 提交用户名修改
const handleUsernameSubmit = async () => {
  if (!usernameFormRef.value) return

  try {
    await usernameFormRef.value.validate()
    loading.value = true

    const requestData: ChangeUsernameRequest = {
      newUsername: usernameFormData.newUsername
    }

    await changeUsername(requestData)
    
    message.success('用户名修改成功，请重新登录')
    visible.value = false
    
    // 清除用户信息并跳转到登录页面
    userStore.logout()
    
    emit('success')
  } catch (error: any) {
    if (error?.response?.data?.message) {
      message.error(error.response.data.message)
    } else if (error?.message) {
      message.error(error.message)
    } else {
      message.error('用户名修改失败，请重试')
    }
  } finally {
    loading.value = false
  }
}

// 提交密码修改
const handlePasswordSubmit = async () => {
  if (!passwordFormRef.value) return

  try {
    await passwordFormRef.value.validate()
    loading.value = true

    const requestData: ChangePasswordRequest = {
      oldPassword: passwordFormData.oldPassword,
      newPassword: passwordFormData.newPassword
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