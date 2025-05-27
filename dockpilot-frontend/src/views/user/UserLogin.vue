<template>
    <!-- 其他结构 -->
  <div :class="['login-bg', { dark: isDark }]">
    <ThemeToggle class="theme-toggle-btn" />

    <div class="login-content">
      <div class="login-logo">
        🚀 DockPilot 容器管理平台
      </div>
      <div :class="['login-box', { dark: isDark }]">
        <h2 class="login-title">登录</h2>
        <n-form
          ref="formRef"
          :model="formValue"
          :rules="rules"
          label-placement="left"
          label-width="auto"
          require-mark-placement="right-hanging"
        >
          <n-form-item path="username">
            <n-input
              v-model:value="formValue.username"
              placeholder="请输入用户名"
              autocomplete="username"
              @keyup.enter="handleLogin"
            />
          </n-form-item>
          <n-form-item path="password">
            <n-input
              v-model:value="formValue.password"
              type="password"
              placeholder="请输入密码"
              show-password-on="click"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </n-form-item>
        </n-form>
        <n-button
          type="primary"
          block
          :loading="loading"
          style="margin-top: 8px;"
          @click="handleLogin"
        >
          登录
        </n-button>
      </div>
    </div>
    <div class="page-footer">
      © 2025 DockPilot 容器管理平台 by Yam
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { useThemeStore } from '@/store/theme'
import { login } from '@/api/http'
import { setToken } from '@/utils/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const message = useMessage()
const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const userStore = useUserStore()

const formValue = reactive({
  username: '',
  password: ''
})

const rules = {
  username: {
    required: true,
    message: '请输入用户名',
    trigger: 'blur'
  },
  password: {
    required: true,
    message: '请输入密码',
    trigger: 'blur'
  }
}

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.theme === 'dark')

async function handleLogin() {
  try {
    await formRef.value?.validate()
    loading.value = true
    const token = await login({
      username: formValue.username,
      password: formValue.password
    })
    setToken(token)
    await userStore.fetchUserInfo()
    message.success(`欢迎回来，${formValue.username}！🎉`)
    router.push('/')
  } catch (error) {
    if (error instanceof Error) {
      message.error(error.message)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  position: fixed;
  left: 0; top: 0;
  width: 100vw; height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
  min-width: 100vw;
  min-height: 100vh;
  box-sizing: border-box;
  overflow: hidden;
}
.login-bg.dark {
  background: #18181c;
}
.theme-toggle-btn {
  position: fixed !important;
  top: 32px !important;
  right: 32px !important;
  left: auto !important;
  z-index: 1000 !important;
  background: red !important;
}
.login-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}
.login-logo {
  margin-bottom: 32px;
  font-size: 28px;
  font-weight: bold;
  color: #222;
  letter-spacing: 2px;
  text-align: center;
}
.dark .login-logo {
  color: #fff;
}
.login-box {
  width: 360px;
  padding: 32px 24px 24px 24px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(0,0,0,0.12);
  display: flex;
  flex-direction: column;
  align-items: stretch;
}
.login-box.dark {
  background: #232324;
  box-shadow: 0 10px 30px rgba(0,0,0,0.32);
}
.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 24px;
  color: #222;
  letter-spacing: 1px;
}
.login-box.dark .login-title {
  color: #fff;
}
.page-footer {
  position: fixed;
  bottom: 20px;
  width: 100%;
  text-align: center;
  font-size: 12px;
  color: #999;
  letter-spacing: 1px;
  z-index: 10;
}
.dark .page-footer {
  color: #666;
}
</style> 