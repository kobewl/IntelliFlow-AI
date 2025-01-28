import axios from 'axios'
import type { AxiosResponse } from 'axios'
import type { User } from '../types/user'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface LoginResponse {
  token: string
  user: User
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 获取认证token和过期时间
export function getAuthToken(): { token: string | null, expiresAt: number | null } {
  try {
    const token = localStorage.getItem('token')
    const expiresAt = localStorage.getItem('tokenExpiresAt')
    
    return {
      token: token ? (token.startsWith('Bearer ') ? token : `Bearer ${token}`) : null,
      expiresAt: expiresAt ? parseInt(expiresAt) : null
    }
  } catch (error) {
    console.error('Failed to get auth token:', error)
    return {
      token: null,
      expiresAt: null
    }
  }
}

// 检查token是否有效
export function isTokenValid(): boolean {
  try {
    const { token, expiresAt } = getAuthToken()
    if (!token || !expiresAt) {
      return false
    }
    
    // 如果token存在且未过期，则有效
    return Date.now() < expiresAt
  } catch (error) {
    console.error('Failed to check token validity:', error)
    return false
  }
}

// 设置认证token和过期时间
export function setAuthToken(token: string | null, expiresIn: number = 7 * 24 * 60 * 60) {
  try {
    if (!token) {
      clearAuth()
      return
    }
    
    // 确保token格式一致
    const formattedToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    localStorage.setItem('token', formattedToken)
    const expiresAt = Date.now() + expiresIn * 1000
    localStorage.setItem('tokenExpiresAt', expiresAt.toString())
  } catch (error) {
    console.error('Failed to set auth token:', error)
    clearAuth()
  }
}

// 获取带Bearer前缀的认证头
export function getAuthHeader(token: string): string {
  if (!token) return ''
  return `Bearer ${token}`
}

// 清除认证信息
export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('tokenExpiresAt')
  localStorage.removeItem('user')
}

// 检查token是否需要刷新
export function shouldRefreshToken(): boolean {
  const { expiresAt } = getAuthToken()
  if (!expiresAt) return false
  
  // 如果token将在30分钟内过期，就刷新
  const thirtyMinutes = 30 * 60 * 1000
  return Date.now() + thirtyMinutes > expiresAt
}

// 创建axios实例
export const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  async (config) => {
    const { token } = getAuthToken()
    if (!token) {
      return config
    }

    config.headers = config.headers || {}
    if (typeof token === 'string') {
      config.headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse) => {
    const apiResponse = response.data as ApiResponse<any>
    if (apiResponse.code === 200) {
      return response
    }
    return Promise.reject(new Error(apiResponse.message || '请求失败'))
  },
  async (error) => {
    if (error.response?.status === 401) {
      try {
        // 尝试刷新token
        await authApi.refreshToken()
        // 如果刷新成功，重试原始请求
        const originalRequest = error.config
        return api(originalRequest)
      } catch (refreshError) {
        // 刷新失败，清除认证状态并重定向到登录页
        clearAuth()
        window.location.href = '/auth/login'
        return Promise.reject(new Error('认证已过期，请重新登录'))
      }
    }
    return Promise.reject(error)
  }
)

// API函数
export const authApi = {
  // 登录
  async login(username: string, password: string): Promise<ApiResponse<LoginResponse>> {
    try {
      const response = await api.post<ApiResponse<LoginResponse>>('/auth/login', { username, password })
      const apiResponse = response.data
      if (apiResponse.code === 200 && apiResponse.data.token) {
        setAuthToken(apiResponse.data.token)
        localStorage.setItem('user', JSON.stringify(apiResponse.data.user))
        return apiResponse
      }
      throw new Error(apiResponse.message || '登录失败')
    } catch (error: any) {
      console.error('Login failed:', error)
      throw new Error(error.message || '登录失败')
    }
  },

  // 注册
  async register(username: string, password: string, email?: string): Promise<ApiResponse<void>> {
    try {
      const response = await api.post<ApiResponse<void>>('/auth/register', { username, password, email })
      return response.data
    } catch (error: any) {
      console.error('Registration failed:', error)
      throw new Error(error.message || '注册失败')
    }
  },

  // 获取用户信息
  async getProfile(): Promise<ApiResponse<User>> {
    try {
      const response = await api.get<ApiResponse<User>>('/auth/profile')
      return response.data
    } catch (error: any) {
      console.error('Failed to get profile:', error)
      throw new Error(error.message || '获取用户信息失败')
    }
  },

  // 登出
  async logout(): Promise<ApiResponse<void>> {
    try {
      const { token } = getAuthToken()
      if (!token) {
        clearAuth()
        return { code: 200, message: 'success', data: void 0 }
      }

      const response = await api.post<ApiResponse<void>>('/auth/logout', null, {
        headers: {
          'Authorization': token
        }
      })
      
      // 清除认证状态
      clearAuth()
      
      return response.data
    } catch (error: any) {
      console.error('Logout failed:', error)
      // 清除认证状态
      clearAuth()
      
      if (error.response?.status === 403 || error.response?.status === 401) {
        return { code: 200, message: 'success', data: void 0 }
      }
      throw new Error(error.response?.data?.message || error.message || '退出登录失败')
    }
  },

  // 刷新token
  async refreshToken(): Promise<ApiResponse<{ token: string }>> {
    try {
      const response = await api.post<ApiResponse<{ token: string }>>('/auth/refresh')
      const apiResponse = response.data
      if (apiResponse.code === 200 && apiResponse.data.token) {
        setAuthToken(apiResponse.data.token)
      }
      return apiResponse
    } catch (error: any) {
      console.error('Token refresh failed:', error)
      throw new Error(error.message || '刷新token失败')
    }
  },

  // 更新用户信息
  async updateProfile(data: Partial<User>): Promise<ApiResponse<User>> {
    try {
      const response = await api.put<ApiResponse<User>>('/auth/profile', data)
      return response.data
    } catch (error: any) {
      console.error('Failed to update profile:', error)
      throw new Error(error.message || '更新用户信息失败')
    }
  },

  // 修改密码
  async changePassword(data: ChangePasswordRequest): Promise<ApiResponse<void>> {
    try {
      const response = await api.put<ApiResponse<void>>('/auth/password', data)
      return response.data
    } catch (error: any) {
      console.error('Failed to change password:', error)
      throw new Error(error.message || '修改密码失败')
    }
  }
}

export default api 