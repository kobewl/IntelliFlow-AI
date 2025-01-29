import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { User } from '../types/user'
import { authApi, isTokenValid, getAuthToken, clearAuth } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const router = useRouter()

  // 计算属性：是否已认证
  const isAuthenticated = computed(() => {
    return !!user.value && !!token.value && isTokenValid()
  })

  // 初始化store
  async function initializeAuth() {
    try {
      const { token: storedToken } = getAuthToken()
      if (!storedToken || !isTokenValid()) {
        clearAuth()
        return false
      }

      token.value = storedToken
      const storedUser = localStorage.getItem('user')
      if (storedUser) {
        user.value = JSON.parse(storedUser)
      } else {
        // 如果没有用户信息，尝试获取
        await getCurrentUserInfo()
      }
      return true
    } catch (err) {
      console.error('Failed to initialize auth:', err)
      clearAuth()
      return false
    }
  }

  // 登录
  async function login(username: string, password: string) {
    try {
      loading.value = true
      error.value = null
      const response = await authApi.login(username, password)
      
      if (response.code === 200 && response.data) {
        token.value = response.data.token
        user.value = response.data.user
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
        user.value = response.data
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
      
      // 1. 调用后端退出接口
      try {
        await authApi.logout()
      } catch (err) {
        console.warn('Backend logout failed:', err)
      }
      
      // 2. 清理所有状态
      token.value = null
      user.value = null
      
      // 3. 清理所有本地存储
      const preserveKeys = ['theme'] // 保留一些不需要清理的配置
      Object.keys(localStorage).forEach(key => {
        if (!preserveKeys.includes(key)) {
          localStorage.removeItem(key)
        }
      })
      
      // 4. 清理会话存储
      sessionStorage.clear()
      
      // 5. 清理认证状态
      clearAuth()
      
      // 6. 重置 loading 状态
      loading.value = false
      
      // 7. 确保路由状态被重置
      router.replace('/')
      
      return true
    } catch (err: any) {
      console.error('Logout failed:', err)
      error.value = err.message || '退出失败'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  // 清除认证状态
  function clearAuthState() {
    // 1. 清理状态
    token.value = null
    user.value = null
    
    // 2. 清理存储
    const preserveKeys = ['theme']
    Object.keys(localStorage).forEach(key => {
      if (!preserveKeys.includes(key)) {
        localStorage.removeItem(key)
      }
    })
    sessionStorage.clear()
    
    // 3. 清理认证
    clearAuth()
  }

  // 更新用户信息
  async function updateProfile(data: Partial<User>) {
    try {
      loading.value = true
      error.value = null
      const response = await authApi.updateProfile(data)
      
      if (response.code === 200) {
        user.value = response.data
        localStorage.setItem('user', JSON.stringify(response.data))
        return true
      }
      
      throw new Error(response.message || '更新用户信息失败')
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  // 修改密码
  async function changePassword(currentPassword: string, newPassword: string) {
    try {
      loading.value = true
      error.value = null
      const response = await authApi.changePassword({
        currentPassword,
        newPassword
      })
      
      if (response.code === 200) {
        return true
      }
      
      throw new Error(response.message || '修改密码失败')
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

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
    initializeAuth,
    updateProfile,
    changePassword
  }
}) 