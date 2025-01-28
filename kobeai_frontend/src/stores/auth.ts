import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '../api/auth'
import type { User } from '../types/user'
import type { ApiResponse } from '../api/types'

export interface AuthState {
  user: User | null
  token: string | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    token: null
  }),

  actions: {
    // 初始化用户信息
    initUser() {
      const storedUser = localStorage.getItem('user')
      const storedToken = localStorage.getItem('token')
      if (storedUser) {
        this.user = JSON.parse(storedUser)
      }
      if (storedToken) {
        this.token = storedToken
      }
    },

    // 设置用户信息
    setUser(newUser: User | null) {
      this.user = newUser
      if (newUser) {
        localStorage.setItem('user', JSON.stringify(newUser))
      } else {
        localStorage.removeItem('user')
      }
    },

    // 设置 token
    setToken(newToken: string | null) {
      this.token = newToken
      if (newToken) {
        localStorage.setItem('token', newToken)
      } else {
        localStorage.removeItem('token')
      }
    },

    // 登录
    async login(username: string, password: string) {
      const response = await authApi.login(username, password)
      if (response.code === 200) {
        this.setUser(response.data.user)
        this.setToken(response.data.token)
      }
      return response
    },

    // 注册
    async register(username: string, password: string, email?: string) {
      const response = await authApi.register(username, password, email)
      return response
    },

    // 登出
    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.setUser(null)
        this.setToken(null)
      }
    },

    // 获取用户信息
    async getProfile() {
      const response = await authApi.getProfile()
      if (response.code === 200) {
        this.setUser(response.data)
      }
      return response
    },

    // 更新用户信息
    async updateProfile(data: Partial<User>) {
      try {
        const response = await authApi.updateProfile(data)
        if (response.code === 200) {
          // 更新本地存储的用户信息
          if (this.user) {
            const updatedUser = { ...this.user, ...data }
            this.setUser(updatedUser)
          }
        }
        return response
      } catch (error: any) {
        throw new Error(error.message || '更新用户信息失败')
      }
    },

    // 修改密码
    async changePassword(currentPassword: string, newPassword: string) {
      try {
        const response = await authApi.changePassword({
          currentPassword,
          newPassword
        })
        if (response.code === 200) {
          // 密码修改成功后，可以选择是否需要重新登录
          // 这里我们选择不强制重新登录
          return response
        }
        throw new Error(response.message || '修改密码失败')
      } catch (error: any) {
        throw new Error(error.message || '修改密码失败')
      }
    }
  }
}) 
