import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { User } from '@/api/http'
import { getUserInfo } from '@/api/http'
import { getToken, removeToken } from '@/utils/auth'
import { useRouter } from 'vue-router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<User | null>(null)
  const isLoggedIn = ref(false)
  const router = useRouter()

  // 设置用户信息
  const setUserInfo = (info: User) => {
    userInfo.value = info
    isLoggedIn.value = true
  }

  // 清除用户信息
  const clearUserInfo = () => {
    userInfo.value = null
    isLoggedIn.value = false
    token.value = null
    removeToken()
  }

  // 获取用户信息
  const fetchUserInfo = async () => {
    try {
      const info = await getUserInfo()
      setUserInfo(info)
      return info
    } catch (error) {
      clearUserInfo()
      throw error
    }
  }

  // 退出登录
  const logout = () => {
    clearUserInfo()
    router.push('/login')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setUserInfo,
    clearUserInfo,
    fetchUserInfo,
    logout
  }
}) 