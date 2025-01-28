import axios from 'axios'
import type { AxiosResponse } from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应接口
interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 用户接口
export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest extends LoginRequest {
  email: string
}

export interface UserProfile {
  id: number
  username: string
  email: string
  createdAt: string
}

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/auth/login'
    }
    return Promise.reject(error)
  }
)

// API函数
export const userApi = {
  async login(data: LoginRequest): Promise<AxiosResponse<ApiResponse>> {
    return api.post('/user/login', data)
  },

  async register(data: RegisterRequest): Promise<AxiosResponse<ApiResponse>> {
    return api.post('/user/register', data)
  },

  async getProfile(): Promise<AxiosResponse<ApiResponse<UserProfile>>> {
    return api.get('/user/profile')
  },

  async logout(): Promise<AxiosResponse<ApiResponse>> {
    return api.post('/user/logout')
  }
} 