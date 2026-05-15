import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { User } from '../types/user'
import { UserRole, isAdmin, isVIP, isSVIP } from '../types/user'
import { authApi, isTokenValid, getAuthToken, clearAuth } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const router = useRouter()

  const isAuthenticated = computed(() => {
    return !!user.value && !!token.value && isTokenValid()
  })

  const isUserAdmin = computed(() => isAdmin(user.value))
  const isUserVIP = computed(() => isVIP(user.value))
  const isUserSVIP = computed(() => isSVIP(user.value))

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
        await getCurrentUserInfo()
      }
      return true
    } catch (err) {
      console.error('Failed to initialize auth:', err)
      clearAuth()
      return false
    }
  }

  async function login(username: string, password: string) {
    try {
      loading.value = true
      error.value = null
      const response = await authApi.login(username, password)

      if (response.code === 200 && response.data) {
        token.value = response.data.token
        const userData = response.data.user
        if (!userData.avatar) {
          userData.avatar = '/ai-avatar.png'
        }
        user.value = userData
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

  // getProfile 作为 getCurrentUserInfo 的别名，兼容新旧调用方
  async function getProfile() {
    if (!token.value) return

    try {
      loading.value = true
      error.value = null
      const response = await authApi.getProfile()

      if (response.code === 200 && response.data) {
        const userData = response.data
        if (!userData.avatar) {
          userData.avatar = '/ai-avatar.png'
        }
        user.value = userData
        localStorage.setItem('user', JSON.stringify(userData))
      }
      return response
    } catch (err: any) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      loading.value = true
      error.value = null

      try {
        await authApi.logout()
      } catch (err) {
        console.warn('Backend logout failed:', err)
      }

      token.value = null
      user.value = null

      const preserveKeys = ['theme']
      Object.keys(localStorage).forEach(key => {
        if (!preserveKeys.includes(key)) {
          localStorage.removeItem(key)
        }
      })

      sessionStorage.clear()
      clearAuth()
      loading.value = false
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

  function clearAuthState() {
    token.value = null
    user.value = null

    const preserveKeys = ['theme']
    Object.keys(localStorage).forEach(key => {
      if (!preserveKeys.includes(key)) {
        localStorage.removeItem(key)
      }
    })
    sessionStorage.clear()
    clearAuth()
  }

  async function updateProfile(data: Partial<User>) {
    try {
      loading.value = true
      error.value = null

      if (!user.value?.id) {
        throw new Error('用户未登录')
      }

      const updateData = {
        ...data,
        id: user.value.id
      }

      const response = await authApi.updateProfile(updateData)

      if (response.code === 200 && response.data) {
        const updatedUser = {
          ...user.value,
          ...response.data,
          userRole: user.value.userRole,
          membershipStartTime: user.value.membershipStartTime,
          membershipEndTime: user.value.membershipEndTime
        }
        user.value = updatedUser
        localStorage.setItem('user', JSON.stringify(updatedUser))
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
    isUserAdmin,
    isUserVIP,
    isUserSVIP,
    login,
    logout,
    register,
    getCurrentUserInfo,
    getProfile,
    clearAuthState,
    initializeAuth,
    updateProfile,
    changePassword
  }
})
