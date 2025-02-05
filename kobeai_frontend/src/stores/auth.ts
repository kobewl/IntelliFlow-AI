import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'
import type { User } from '../types/user'
import { UserRole, isAdmin, isVIP, isSVIP } from '../types/user'
import type { ApiResponse } from '../api/types'

export interface AuthState {
  user: User | null
  token: string | null
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)

  // 计算属性
  const isUserAdmin = computed(() => isAdmin(user.value))
  const isUserVIP = computed(() => isVIP(user.value))
  const isUserSVIP = computed(() => isSVIP(user.value))

  // 初始化用户信息
  function initUser() {
    const storedUser = localStorage.getItem('user')
    const storedToken = localStorage.getItem('token')
    if (storedUser) {
      user.value = JSON.parse(storedUser)
    }
    if (storedToken) {
      token.value = storedToken
    }
  }

  // 设置用户信息
  function setUser(newUser: User | null) {
    user.value = newUser
    if (newUser) {
      localStorage.setItem('user', JSON.stringify(newUser))
    } else {
      localStorage.removeItem('user')
    }
  }

  // 设置 token
  function setToken(newToken: string | null) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('token', newToken)
    } else {
      localStorage.removeItem('token')
    }
  }

  // 登录
  async function login(username: string, password: string) {
    const response = await authApi.login(username, password)
    if (response.code === 200) {
      setUser(response.data.user)
      setToken(response.data.token)
    }
    return response
  }

  // 注册
  async function register(username: string, password: string, email?: string) {
    const response = await authApi.register(username, password, email)
    return response
  }

  // 登出
  async function logout() {
    try {
      await authApi.logout()
    } finally {
      setUser(null)
      setToken(null)
    }
  }

  // 获取用户信息
  async function getProfile() {
    const response = await authApi.getProfile()
    if (response.code === 200) {
      setUser(response.data)
    }
    return response
  }

  // 更新用户信息
  async function updateProfile(data: Partial<User>) {
    try {
      if (!user.value?.id) {
        throw new Error('用户未登录')
      }

      // 确保包含用户ID
      const updateData = {
        ...data,
        id: user.value.id
      }

      const response = await authApi.updateProfile(updateData)
      if (response.code === 200 && response.data) {
        // 更新本地存储的用户信息，保持敏感字段不变
        const updatedUser = {
          ...user.value,
          ...response.data,
          userRole: user.value.userRole, // 保持原有角色
          membershipStartTime: user.value.membershipStartTime,
          membershipEndTime: user.value.membershipEndTime
        }
        setUser(updatedUser)
        return response
      }
      throw new Error(response.message || '更新用户信息失败')
    } catch (error: any) {
      console.error('Update profile error:', error)
      throw new Error(error.message || '更新用户信息失败')
    }
  }

  // 修改密码
  async function changePassword(currentPassword: string, newPassword: string) {
    try {
      const response = await authApi.changePassword({
        currentPassword,
        newPassword
      })
      if (response.code === 200) {
        return response
      }
      throw new Error(response.message || '修改密码失败')
    } catch (error: any) {
      throw new Error(error.message || '修改密码失败')
    }
  }

  return {
    user,
    token,
    isUserAdmin,
    isUserVIP,
    isUserSVIP,
    initUser,
    setUser,
    setToken,
    login,
    register,
    logout,
    getProfile,
    updateProfile,
    changePassword
  }
}) 
