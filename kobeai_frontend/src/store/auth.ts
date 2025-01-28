import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { User } from '../types/user'
import { authApi } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<User | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 计算属性：是否已认证
  const isAuthenticated = computed(() => {
    return !!user.value && !!token.value
  })

  // 登录
  async function login(username: string, password: string) {
    try {
      loading.value = true
      error.value = null
      const response = await authApi.login(username, password)
      
      if (response.code === 200 && response.data) {
        token.value = response.data.token
        user.value = response.data.user as User
        localStorage.setItem('token', response.data.token)
        localStorage.setItem('user', JSON.stringify(response.data.user))
        return true
      }
      
      throw new Error(response.message || '登录失败')
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  // 注册
  async function register(username: string, password: string, email?: string) {
    try {
      loading.value = true
      error.value = null
      const response = await authApi.register(username, password, email)
      
      if (response.code === 200) {
        return true
      }
      
      throw new Error(response.message || '注册失败')
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  // 获取当前用户信息
  async function getCurrentUserInfo() {
    if (!token.value) return
    
    try {
      loading.value = true
      error.value = null
      const response = await authApi.getProfile()
      
      if (response.code === 200) {
        user.value = response.data as User
        localStorage.setItem('user', JSON.stringify(response.data))
      } else {
        throw new Error(response.message || '获取用户信息失败')
      }
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  // 登出
  async function logout() {
    try {
      loading.value = true
      error.value = null
      
      // 先清除本地状态
      token.value = null
      user.value = null
      localStorage.clear()
      sessionStorage.clear()
      
      // 清除所有cookie
      document.cookie.split(';').forEach(cookie => {
        const name = cookie.split('=')[0].trim()
        document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`
      })
      
      // 调用后端接口
      try {
        await authApi.logout()
      } catch (err) {
        console.warn('Backend logout failed:', err)
      }
      
      // 强制重定向到首页
      const baseUrl = window.location.origin
      window.location.href = baseUrl
      
      return true
    } catch (err: any) {
      error.value = err.message
      throw new Error(err.message || '退出登录失败')
    } finally {
      loading.value = false
    }
  }

  // 清除认证状态
  function clearAuthState() {
    // 先清除 store 中的状态
    token.value = null
    user.value = null
    
    // 然后清除存储的数据
    try {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      sessionStorage.clear()
      
      // 清除所有相关的 cookie
      document.cookie.split(';').forEach(cookie => {
        document.cookie = cookie
          .replace(/^ +/, '')
          .replace(/=.*/, '=;expires=' + new Date().toUTCString() + ';path=/')
      })
    } catch (err) {
      console.warn('Failed to clear some storage:', err)
    }
  }

  // 初始化store
  function initializeFromStorage() {
    try {
      const storedUser = localStorage.getItem('user')
      const storedToken = localStorage.getItem('token')
      
      if (storedUser && storedToken) {
        user.value = JSON.parse(storedUser) as User
        token.value = storedToken
      } else {
        clearAuthState()
      }
    } catch (err) {
      console.error('Failed to parse stored user:', err)
      clearAuthState()
    }
  }

  // 初始化store
  initializeFromStorage()

  return {
    token,
    user,
    loading,
    error,
    isAuthenticated,
    login,
    logout,
    register,
    getCurrentUserInfo,
    clearAuthState,
    initializeFromStorage
  }
}) 