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

// 获取认证token
export function getAuthToken(): string | null {
  const token = localStorage.getItem('token')
  if (!token) return null
  return token
}

// 设置认证token
export function setAuthToken(token: string) {
  localStorage.setItem('token', token)
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
  (config) => {
    const token = getAuthToken()
    if (token) {
      config.headers = config.headers || {}
      // 如果 token 已经包含 Bearer 前缀，直接使用；否则添加前缀
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
      return response // 返回完整的 AxiosResponse
    }
    return Promise.reject(new Error(apiResponse.message || '请求失败'))
  },
  async (error) => {
    if (error.response?.status === 401) {
      try {
        // 尝试刷新token
        const currentToken = getAuthToken()
        if (currentToken) {
          const response = await authApi.refreshToken()
          if (response.code === 200 && response.data.token) {
            // 更新token
            setAuthToken(response.data.token)
            // 重试原始请求
            error.config.headers.Authorization = `Bearer ${response.data.token}`
            return api(error.config)
          }
        }
      } catch (refreshError) {
        console.error('Token refresh failed:', refreshError)
      }
      
      // 如果刷新失败或没有token，清除认证状态
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      // 重定向到登录页
      window.location.href = '/auth/login'
      return Promise.reject(new Error('认证已过期，请重新登录'))
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
        // 保存 token 和用户信息
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
      // 获取当前token
      const token = getAuthToken()
      if (!token) {
        // 如果没有token，直接返回成功
        return { code: 200, message: 'success', data: void 0 }
      }

      const response = await api.post<ApiResponse<void>>('/auth/logout', null, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      return response.data
    } catch (error: any) {
      console.error('Logout failed:', error)
      // 如果是403或401错误，视为退出成功
      if (error.response?.status === 403 || error.response?.status === 401) {
        return { code: 200, message: 'success', data: void 0 }
      }
      // 其他错误则抛出
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